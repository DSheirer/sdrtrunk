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

package io.github.dsheirer.source.tuner;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.dsp.fft.ComplexFFT;
import io.github.dsheirer.dsp.fft.ComplexFFTFactory;
import io.github.dsheirer.dsp.window.WindowFactory;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.util.ThreadPool;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Estimates a tuner's noise floor from the tuner's sample buffers, independent of the channelizer that sources the
 * tuner's channels.  A windowed FFT with bins of about 25 kHz measures the tuner bandwidth and each bin's mean power is
 * converted to a noise power density once a second.
 *
 * A channel's noise floor is the median of the 40 usable bins nearest the channel (about +/- 500 kHz), excluding the
 * channel's own bins and their immediate neighbors.  Near a band edge, the search continues toward the band center so
 * that channels anywhere in the tuner's usable bandwidth get an estimate.  Usable bins are the tuner's usable
 * bandwidth, which is where the channel source managers place channels, minus the DC spur.  Most bins hold only noise, so the median is a robust noise estimate unless more than
 * half of the neighborhood is occupied.  The estimate is local because real tuner noise floors aren't flat: a
 * HydraSDR airband recording tilts about 6 dB across 8 MHz, which put a tuner-wide median 2 dB above the noise at one
 * end of the band.
 *
 * The noise density is in full-scale units per Hertz, where a complex tone with a magnitude of 1.0 has a power of 1.0
 * (0 dBFS).  The channelizers preserve the tuner's full scale, so the noise density applies directly to channel
 * samples.
 *
 * Measurement is on demand.  A noise density request turns it on and it turns itself off when there are no requests
 * for a few seconds.  While it's off, the tuner's buffer delivery thread only checks a flag.  While it's on, the
 * delivery thread keeps a reference to the latest buffer and a scheduled task measures a few FFT blocks from it 20
 * times a second.  The estimator never registers as a buffer listener, since that starts and stops tuner streaming.
 */
public class TunerNoiseFloorEstimator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TunerNoiseFloorEstimator.class);
    private static final double BIN_WIDTH = 25000.0;
    //Minimum size so that low sample rate tuners have enough bins for a channel neighborhood, with narrower bins
    private static final int MINIMUM_FFT_SIZE = 64;
    //Window with low sidelobes so that strong signals don't leak into the noise bins
    private static final WindowType WINDOW_TYPE = WindowType.BLACKMAN_HARRIS_7;
    private static final long PROCESSING_INTERVAL_MS = 50;
    private static final int BLOCKS_PER_INTERVAL = 8;
    private static final long ESTIMATE_INTERVAL_MS = 1000;
    private static final long REQUEST_TIMEOUT_MS = 3000;
    //Fraction of the sample rate, centered on DC, that is used when the tuner's usable bandwidth isn't known.  The
    //tuner's anti-alias filter rolls off the edges.
    private static final double DEFAULT_USABLE_BANDWIDTH_FRACTION = 0.8;
    //Bins on each side of DC that are skipped to avoid the tuner's DC spur
    private static final int DC_EXCLUSION_BINS = 1;
    //Usable bins nearest the channel that form its noise neighborhood (+/- 500 kHz at 25 kHz bins)
    private static final int NEIGHBORHOOD_BINS = 40;
    //Bins on each side of a channel's own bins that are excluded since they can hold the channel's signal
    private static final int GUARD_BINS = 1;
    //Fewest neighborhood bins for an estimate
    private static final int MINIMUM_NEIGHBORHOOD_BINS = 8;

    /**
     * Noise density of each bin, with the tuner frequency and sample rate that it was measured at.  Bins are in FFT
     * order: bin 0 is DC and bins above size / 2 are negative frequencies.
     */
    private record Estimate(long frequency, double sampleRate, double[] density, boolean[] usable) {}

    private final LongSupplier mFrequency;
    private final DoubleSupplier mSampleRate;
    private final IntSupplier mUsableBandwidth;
    private final boolean mScheduled;
    private final AtomicReference<INativeBuffer> mLatestBuffer = new AtomicReference<>();
    private final Object mLock = new Object();
    private volatile boolean mActive;
    private volatile long mLastRequestTimestamp;
    private volatile Estimate mEstimate;
    private ScheduledFuture<?> mProcessingTask;

    //Measurement state, accessed only by the processing task
    private long mMeasurementFrequency;
    private double mMeasurementSampleRate;
    private ComplexFFT mFFT;
    private float[] mWindow;
    private double mWindowPower;
    private float[] mBlock;
    private int mBlockPointer;
    private double[] mBinPowerAccumulator;
    private int mAccumulatedBlocks;
    private long mMeasurementStart;

    /**
     * Constructs an instance
     * @param frequency supplier for the tuner's center frequency
     * @param sampleRate supplier for the tuner's sample rate
     * @param usableBandwidth supplier for the tuner's usable bandwidth in Hertz, or 0 or less if unknown
     */
    public TunerNoiseFloorEstimator(LongSupplier frequency, DoubleSupplier sampleRate, IntSupplier usableBandwidth)
    {
        this(frequency, sampleRate, usableBandwidth, true);
    }

    /**
     * Constructs an instance
     * @param frequency supplier for the tuner's center frequency
     * @param sampleRate supplier for the tuner's sample rate
     * @param usableBandwidth supplier for the tuner's usable bandwidth in Hertz, or 0 or less if unknown
     * @param scheduled true to measure on a scheduled task, or false for tests that call process() directly
     */
    TunerNoiseFloorEstimator(LongSupplier frequency, DoubleSupplier sampleRate, IntSupplier usableBandwidth,
                             boolean scheduled)
    {
        mFrequency = frequency;
        mSampleRate = sampleRate;
        mUsableBandwidth = usableBandwidth;
        mScheduled = scheduled;
    }

    /**
     * Receives each tuner buffer on the tuner's buffer delivery thread.  Keeps the buffer only while measuring.
     */
    public void receive(INativeBuffer buffer)
    {
        if(mActive)
        {
            mLatestBuffer.set(buffer);
        }
    }

    /**
     * Noise power density near a channel, in full-scale units per Hertz.  Turns on the measurement.  Thread safe.
     * @param frequency of the channel
     * @param bandwidth of the channel in Hertz
     * @return noise density, or NaN when there isn't an estimate yet, the tuner frequency or sample rate changed since
     * the estimate, or the channel isn't within the usable tuner bandwidth.
     */
    public double getNoiseDensity(long frequency, int bandwidth)
    {
        mLastRequestTimestamp = System.currentTimeMillis();

        if(!mActive)
        {
            start();
        }

        Estimate estimate = mEstimate;

        if(estimate == null || estimate.frequency() != mFrequency.getAsLong() ||
            estimate.sampleRate() != mSampleRate.getAsDouble())
        {
            return Double.NaN;
        }

        int size = estimate.density().length;
        double binWidth = estimate.sampleRate() / size;
        //Signed bin offsets from DC of the bins that the channel overlaps
        int low = (int)Math.round((frequency - bandwidth / 2.0 - estimate.frequency()) / binWidth);
        int high = (int)Math.round((frequency + bandwidth / 2.0 - estimate.frequency()) / binWidth);
        int center = (int)Math.round((double)(frequency - estimate.frequency()) / binWidth);

        if(Math.abs(center) >= size / 2)
        {
            return Double.NaN;
        }

        double[] values = new double[NEIGHBORHOOD_BINS];
        int count = 0;
        int below = low - GUARD_BINS - 1;
        int above = high + GUARD_BINS + 1;

        //Alternate outward on each side of the channel, so that a channel near a band edge continues toward the center
        while(count < NEIGHBORHOOD_BINS && (below > -size / 2 || above < size / 2))
        {
            if(below > -size / 2)
            {
                int bin = below < 0 ? below + size : below;

                if(estimate.usable()[bin])
                {
                    values[count++] = estimate.density()[bin];
                }

                below--;
            }

            if(count < NEIGHBORHOOD_BINS && above < size / 2)
            {
                int bin = above < 0 ? above + size : above;

                if(estimate.usable()[bin])
                {
                    values[count++] = estimate.density()[bin];
                }

                above++;
            }
        }

        return count >= MINIMUM_NEIGHBORHOOD_BINS ? median(values, count) : Double.NaN;
    }

    /**
     * Turns on the measurement.
     */
    private void start()
    {
        synchronized(mLock)
        {
            if(!mActive)
            {
                mActive = true;

                if(mScheduled)
                {
                    mProcessingTask = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::processScheduled,
                        PROCESSING_INTERVAL_MS, PROCESSING_INTERVAL_MS, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /**
     * Turns off the measurement and discards the estimate, so that a stale estimate isn't used when it turns back on.
     */
    private void stop()
    {
        synchronized(mLock)
        {
            //A request can arrive between the timeout check and here
            if(System.currentTimeMillis() - mLastRequestTimestamp <= REQUEST_TIMEOUT_MS)
            {
                return;
            }

            mActive = false;

            if(mProcessingTask != null)
            {
                mProcessingTask.cancel(false);
                mProcessingTask = null;
            }

            mLatestBuffer.set(null);
            mEstimate = null;
            mBinPowerAccumulator = null;
        }
    }

    private void processScheduled()
    {
        try
        {
            if(System.currentTimeMillis() - mLastRequestTimestamp > REQUEST_TIMEOUT_MS)
            {
                stop();
                return;
            }

            process(System.currentTimeMillis());
        }
        catch(Throwable t)
        {
            LOGGER.error("Error estimating tuner noise floor", t);
        }
    }

    /**
     * Measures a few FFT blocks from the latest buffer and updates the estimate once a second.  Package private for
     * tests, which call it directly instead of waiting for the scheduled task.
     * @param now timestamp in milliseconds
     */
    void process(long now)
    {
        INativeBuffer buffer = mLatestBuffer.getAndSet(null);

        if(buffer == null)
        {
            return;
        }

        long frequency = mFrequency.getAsLong();
        double sampleRate = mSampleRate.getAsDouble();

        if(sampleRate <= 0)
        {
            return;
        }

        if(mBinPowerAccumulator == null || frequency != mMeasurementFrequency || sampleRate != mMeasurementSampleRate)
        {
            configure(frequency, sampleRate, now);
        }

        int blocks = 0;
        int size = mWindow.length;
        mBlockPointer = 0;
        Iterator<InterleavedComplexSamples> iterator = buffer.iteratorInterleaved();

        while(blocks < BLOCKS_PER_INTERVAL && iterator.hasNext())
        {
            float[] samples = iterator.next().samples();
            int pointer = 0;

            while(blocks < BLOCKS_PER_INTERVAL && pointer < samples.length)
            {
                int count = Math.min(samples.length - pointer, 2 * (size - mBlockPointer));
                System.arraycopy(samples, pointer, mBlock, 2 * mBlockPointer, count);
                pointer += count;
                mBlockPointer += count / 2;

                if(mBlockPointer == size)
                {
                    measureBlock();
                    mBlockPointer = 0;
                    blocks++;
                }
            }
        }

        if(now - mMeasurementStart >= ESTIMATE_INTERVAL_MS && mAccumulatedBlocks > 0)
        {
            publish();
            mMeasurementStart = now;
        }
    }

    /**
     * Sets up the FFT for the tuner's sample rate and restarts the measurement.
     */
    private void configure(long frequency, double sampleRate, long now)
    {
        mMeasurementFrequency = frequency;
        mMeasurementSampleRate = sampleRate;
        mMeasurementStart = now;
        mEstimate = null;

        int size = getFftSize(sampleRate);

        if(mWindow == null || mWindow.length != size)
        {
            mFFT = ComplexFFTFactory.getComplexFFT(size);
            mWindow = WindowFactory.getWindow(WINDOW_TYPE, size);
            mBlock = new float[size * 2];
            double sumSquares = 0.0;

            for(float coefficient: mWindow)
            {
                sumSquares += coefficient * coefficient;
            }

            mWindowPower = sumSquares;
        }

        mBinPowerAccumulator = new double[size];
        mAccumulatedBlocks = 0;
    }

    /**
     * FFT size that gives bins of about 25 kHz, or narrower bins at low sample rates, using a size that the mixed
     * radix FFT supports.
     */
    static int getFftSize(double sampleRate)
    {
        int size = Math.max(MINIMUM_FFT_SIZE, (int)Math.round(sampleRate / BIN_WIDTH));

        while(size > MINIMUM_FFT_SIZE && !ComplexFFT.isSupported(size))
        {
            size--;
        }

        return size;
    }

    /**
     * Windows the block, runs the FFT and accumulates each bin's power.
     */
    private void measureBlock()
    {
        for(int x = 0; x < mWindow.length; x++)
        {
            mBlock[2 * x] *= mWindow[x];
            mBlock[2 * x + 1] *= mWindow[x];
        }

        mFFT.forward(mBlock, 0);

        for(int bin = 0; bin < mWindow.length; bin++)
        {
            float i = mBlock[2 * bin];
            float q = mBlock[2 * bin + 1];
            mBinPowerAccumulator[bin] += i * i + q * q;
        }

        mAccumulatedBlocks++;
    }

    /**
     * Converts the accumulated bin powers to noise densities and publishes the estimate.  For white noise with variance
     * s^2 per complex sample, a windowed FFT bin has a mean power of s^2 * sum(w^2), and the noise density is s^2 / fs.
     */
    private void publish()
    {
        int size = mBinPowerAccumulator.length;
        double[] density = new double[size];
        boolean[] usable = new boolean[size];
        double scale = 1.0 / (mAccumulatedBlocks * mWindowPower * mMeasurementSampleRate);
        int usableBandwidth = mUsableBandwidth != null ? mUsableBandwidth.getAsInt() : 0;
        double usableFraction = usableBandwidth > 0 ? Math.min(1.0, usableBandwidth / mMeasurementSampleRate) :
            DEFAULT_USABLE_BANDWIDTH_FRACTION;
        double maximumOffset = size * usableFraction / 2.0;

        for(int bin = 0; bin < size; bin++)
        {
            density[bin] = mBinPowerAccumulator[bin] * scale;
            int offset = bin < (size + 1) / 2 ? bin : bin - size;
            usable[bin] = Math.abs(offset) > DC_EXCLUSION_BINS && Math.abs(offset) <= maximumOffset;
        }

        mEstimate = new Estimate(mMeasurementFrequency, mMeasurementSampleRate, density, usable);
        Arrays.fill(mBinPowerAccumulator, 0.0);
        mAccumulatedBlocks = 0;
    }

    /**
     * Median of the first count values.  Sorts the values.
     */
    private static double median(double[] values, int count)
    {
        Arrays.sort(values, 0, count);
        int middle = count / 2;
        return (count % 2 == 1) ? values[middle] : (values[middle - 1] + values[middle]) / 2.0;
    }

    /**
     * Indicates if the measurement is turned on.
     */
    public boolean isActive()
    {
        return mActive;
    }
}
