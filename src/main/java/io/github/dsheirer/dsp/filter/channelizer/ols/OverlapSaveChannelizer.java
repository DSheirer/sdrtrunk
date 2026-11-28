/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.dsp.filter.channelizer.ols;

import io.github.dsheirer.dsp.fft.ComplexFFT;
import io.github.dsheirer.dsp.fft.ComplexFFTFactory;
import java.util.Arrays;

/**
 * Overlap-save (fast convolution) channelizer.  Each hop of new complex samples is appended to the overlap kept from
 * the previous block, the whole block is transformed with one forward FFT, and every channel slices its own bins from
 * the shared spectrum (see OverlapSaveChannel).  The forward FFT cost does not depend on the channel count.
 *
 * Block parameters: FFT size N, hop L and overlap N - L.  The overlap bounds the impulse response a channel filter can
 * have.  With the default 40 Hz bins and a 4/5 hop (overlap factor 5), 10 MS/s uses N = 250,000 and a 20 ms hop.
 *
 * Not thread-safe: invoke receive() from a single thread.  Block listeners are invoked on that thread with the
 * spectrum, which is only valid during the call.  Channels can be added and removed from any thread.
 *
 * Block timestamps come from a sample clock: each hop's timestamp is one hop's duration after the previous one (20 ms
 * at the default parameters), and the clock follows the buffer timestamps only across a break in the sample stream
 * (see MAXIMUM_CLOCK_ERROR_MS).
 *
 * See the package documentation (package-info.java) for the theory of operation and references.
 */
public class OverlapSaveChannelizer
{
    public static final double DEFAULT_BIN_WIDTH = 40.0;
    public static final int DEFAULT_OVERLAP_FACTOR = 5;
    /**
     * Largest difference between a received buffer's timestamp and the sample clock that is treated as buffer timing
     * jitter rather than a break in the sample stream.
     */
    public static final long MAXIMUM_CLOCK_ERROR_MS = 100;
    private static final BlockListener[] NO_LISTENERS = new BlockListener[0];

    private final double mSampleRate;
    private final int mFFTSize;
    private final int mHop;
    private final ComplexFFT mFFT;
    private final float[] mI;
    private final float[] mQ;
    private final float[] mTailI;
    private final float[] mTailQ;
    private volatile BlockListener[] mListeners = NO_LISTENERS;
    private volatile Runnable mBlockBoundaryTask;
    private int mPointer;
    private long mBlockCount;
    private long mHopTimestamp;
    private long mSampleCount;
    private boolean mClockSet;
    private long mClockOrigin;
    private long mClockOriginSample;

    /**
     * Receives each transformed block.
     */
    public interface BlockListener
    {
        /**
         * Processes one block's spectrum.
         * @param spectrumI inphase bins of the forward FFT, valid only during the call
         * @param spectrumQ quadrature bins, valid only during the call
         * @param block index of this block since the channelizer started
         * @param timestamp of the block's first new (hop) sample, which is the first output sample of each channel
         */
        void processBlock(float[] spectrumI, float[] spectrumQ, long block, long timestamp);
    }

    /**
     * Constructs an instance.
     * @param sampleRate of the complex input
     * @param fftSize N
     * @param hop L, the new samples per block
     * @param fft forward FFT of size N
     */
    public OverlapSaveChannelizer(double sampleRate, int fftSize, int hop, ComplexFFT fft)
    {
        if(fft.getSize() != fftSize || hop <= 0 || hop > fftSize)
        {
            throw new IllegalArgumentException("Invalid FFT size [" + fftSize + "] or hop [" + hop + "]");
        }

        mSampleRate = sampleRate;
        mFFTSize = fftSize;
        mHop = hop;
        mFFT = fft;
        mI = new float[fftSize];
        mQ = new float[fftSize];
        mTailI = new float[fftSize - hop];
        mTailQ = new float[fftSize - hop];
        mPointer = fftSize - hop;
    }

    /**
     * Creates an instance with the default 40 Hz bins and overlap factor 5, using the calibrated FFT
     * (ComplexFFTFactory).
     * @param sampleRate that is supported (see isSupported())
     */
    public static OverlapSaveChannelizer create(double sampleRate)
    {
        int size = getDefaultFFTSize(sampleRate);
        return create(sampleRate, ComplexFFTFactory.getComplexFFT(size));
    }

    /**
     * Creates an instance with the default 40 Hz bins and overlap factor 5.
     * @param sampleRate that is supported (see isSupported())
     * @param fft of size getDefaultFFTSize(sampleRate)
     */
    public static OverlapSaveChannelizer create(double sampleRate, ComplexFFT fft)
    {
        int size = getDefaultFFTSize(sampleRate);
        return new OverlapSaveChannelizer(sampleRate, size, getDefaultHop(size), fft);
    }

    public static int getDefaultFFTSize(double sampleRate)
    {
        return (int)Math.round(sampleRate / DEFAULT_BIN_WIDTH);
    }

    public static int getDefaultHop(int fftSize)
    {
        return fftSize / DEFAULT_OVERLAP_FACTOR * (DEFAULT_OVERLAP_FACTOR - 1);
    }

    /**
     * Indicates if the sample rate can use the default block parameters: an integral number of 40 Hz bins, divisible
     * by the overlap factor, with no prime factors other than 2, 3 and 5.
     * @param sampleRate of the tuner
     * @return true if supported
     */
    public static boolean isSupported(double sampleRate)
    {
        double bins = sampleRate / DEFAULT_BIN_WIDTH;
        long size = Math.round(bins);

        if(size < 1000 || size > Integer.MAX_VALUE / 2 || Math.abs(bins - size) > 1E-6 ||
            size % DEFAULT_OVERLAP_FACTOR != 0)
        {
            return false;
        }

        for(int factor : new int[]{2, 3, 5})
        {
            while(size % factor == 0)
            {
                size /= factor;
            }
        }

        return size == 1;
    }

    public double getSampleRate()
    {
        return mSampleRate;
    }

    public int getFFTSize()
    {
        return mFFTSize;
    }

    public int getHop()
    {
        return mHop;
    }

    /**
     * Forward FFT used by this channelizer.
     */
    public ComplexFFT getFFT()
    {
        return mFFT;
    }

    public double getBinWidth()
    {
        return mSampleRate / mFFTSize;
    }

    /**
     * Number of blocks transformed so far.
     */
    public long getBlockCount()
    {
        return mBlockCount;
    }

    /**
     * Adds a channel.  The listener list is copied on change, so that a block doesn't iterate with an allocation.
     */
    public synchronized void addChannel(BlockListener listener)
    {
        BlockListener[] listeners = Arrays.copyOf(mListeners, mListeners.length + 1);
        listeners[listeners.length - 1] = listener;
        mListeners = listeners;
    }

    public synchronized void removeChannel(BlockListener listener)
    {
        BlockListener[] listeners = mListeners;

        for(int x = 0; x < listeners.length; x++)
        {
            if(listeners[x] == listener)
            {
                BlockListener[] remaining = new BlockListener[listeners.length - 1];
                System.arraycopy(listeners, 0, remaining, 0, x);
                System.arraycopy(listeners, x + 1, remaining, x, listeners.length - x - 1);
                mListeners = remaining;
                return;
            }
        }
    }

    public int getChannelCount()
    {
        return mListeners.length;
    }

    /**
     * Sets a task to run on the receive() thread after each forward FFT and before the spectrum goes to the block
     * listeners, for example to apply tuning changes at a block boundary.
     */
    public void setBlockBoundaryTask(Runnable task)
    {
        mBlockBoundaryTask = task;
    }

    /**
     * Discards the partial block and the saved overlap and restarts the sample clock, for a restart of the sample
     * stream, so that the first block after the restart doesn't join old samples to new ones.  Call it on the thread
     * that calls receive(), or while no thread is in receive().
     */
    public void reset()
    {
        int overlap = mFFTSize - mHop;
        Arrays.fill(mI, 0, overlap, 0.0f);
        Arrays.fill(mQ, 0, overlap, 0.0f);
        mPointer = overlap;
        mClockSet = false;
    }

    /**
     * Receives interleaved complex samples.
     * @param samples interleaved I/Q
     * @param timestamp of the first sample, in milliseconds
     */
    public void receive(float[] samples, long timestamp)
    {
        receive(samples, 0, samples.length / 2, timestamp);
    }

    /**
     * Receives interleaved complex samples from part of an array.  The samples are copied into the block before this
     * returns and the array isn't retained, so the caller can pass a shared, read-only array.
     * @param interleaved I/Q samples
     * @param offset of the first complex sample in the array
     * @param length in complex samples
     * @param timestamp of the first sample, in milliseconds
     */
    public void receive(float[] interleaved, int offset, int length, long timestamp)
    {
        if(offset < 0 || length < 0 || 2L * offset + 2L * length > interleaved.length)
        {
            throw new IndexOutOfBoundsException("Offset [" + offset + "] and length [" + length + "] exceed the " +
                interleaved.length / 2 + " samples in the array");
        }

        synchronizeClock(timestamp);

        int pointer = 0;
        final float[] i = mI;
        final float[] q = mQ;
        final int overlap = mFFTSize - mHop;

        while(pointer < length)
        {
            if(mPointer == overlap)
            {
                mHopTimestamp = getTimestamp(mSampleCount + pointer);
            }

            int count = Math.min(length - pointer, mFFTSize - mPointer);
            int source = 2 * (offset + pointer);
            int destination = mPointer;

            for(int x = 0; x < count; x++)
            {
                i[destination + x] = interleaved[source + 2 * x];
                q[destination + x] = interleaved[source + 2 * x + 1];
            }

            mPointer += count;
            pointer += count;

            if(mPointer == mFFTSize)
            {
                process();
            }
        }

        mSampleCount += length;
    }

    /**
     * Checks the sample clock against the timestamp of a received buffer's first sample, and moves the clock's origin
     * to the timestamp on the first buffer or when the clock is off by more than MAXIMUM_CLOCK_ERROR_MS, for example
     * after dropped buffers or a restart of the sample stream.  Smaller differences are tuner buffer timing jitter, or
     * a caller that stamps every chunk of a buffer with the buffer's timestamp, and are ignored.
     */
    private void synchronizeClock(long timestamp)
    {
        if(!mClockSet || Math.abs(timestamp - getTimestamp(mSampleCount)) > MAXIMUM_CLOCK_ERROR_MS)
        {
            mClockOrigin = timestamp;
            mClockOriginSample = mSampleCount;
            mClockSet = true;
        }
    }

    /**
     * Timestamp of a sample from the sample clock.
     * @param sample index since the channelizer started
     * @return timestamp in milliseconds
     */
    private long getTimestamp(long sample)
    {
        return mClockOrigin + Math.round((sample - mClockOriginSample) * 1000.0 / mSampleRate);
    }

    /**
     * Transforms the block and hands the spectrum to each channel, then restores the overlap for the next block.
     */
    private void process()
    {
        int overlap = mFFTSize - mHop;
        System.arraycopy(mI, mHop, mTailI, 0, overlap);
        System.arraycopy(mQ, mHop, mTailQ, 0, overlap);

        mFFT.forward(mI, mQ);

        Runnable task = mBlockBoundaryTask;

        if(task != null)
        {
            task.run();
        }

        BlockListener[] listeners = mListeners;

        for(BlockListener listener : listeners)
        {
            listener.processBlock(mI, mQ, mBlockCount, mHopTimestamp);
        }

        mBlockCount++;
        System.arraycopy(mTailI, 0, mI, 0, overlap);
        System.arraycopy(mTailQ, 0, mQ, 0, overlap);
        mPointer = overlap;
    }
}
