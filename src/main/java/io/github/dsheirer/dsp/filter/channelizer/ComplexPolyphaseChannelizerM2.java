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
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.util.Dispatcher;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.commons.math3.util.FastMath;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Non-Maximally Decimated Polyphase Filter Bank (NMDPFB) channelizer that divides the input baseband complex sample
 * stream into equal bandwidth channels that are each oversampled by 2x for output.
 *
 * This polyphase channelizer is based off of the channelizer described by Fred Harris in Multirate Signal
 * Processing for Communications Systems, p230-233.
 *
 * Samples are loaded into this filter one block at a time (1/2 channel count) and a filtered output is calculated
 * to produce an overall 2x oversampled channel sample rate.  Instead of shifting the existing sample blocks for each
 * block load, the window of filtered samples moves toward the start of an oversized sample buffer, and the window is
 * only copied back to the end of the buffer when it reaches the start.
 *
 * The prototype filter for the channelizer is rearranged to align with the structure of the sample buffer.
 *
 * Instead of using an array of channel filters as described in the Harris text, this filter and the sample buffer
 * are arranged as a contiguous array to maximize Java's ability to leverage native processor Single Instruction
 * Multiple Data (SIMD) intrinsics (since Java 8).  The filter process is broken into three steps:
 *
 *   -Fused multiply and accumulate of the inline sample/filter arrays into each I/Q sub-channel accumulator
 *   -Rearrange the sub-channel results to correctly order the sub-channels
 *   -Perform IFFT
 *
 * Note: design the prototype filter as a Nyquist windowed filter with a -6.02 db attenuation at the channel edge
 * frequency if you need Perfect Reconstruction where you'll later re-join two or more channels to form a wider
 * bandwidth channel or to isolate a signal that located between two channels.
 */
public class ComplexPolyphaseChannelizerM2 extends AbstractComplexPolyphaseChannelizer
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexPolyphaseChannelizerM2.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final int DEFAULT_MINIMUM_CHANNEL_BANDWIDTH = 25000;

    /**
     * Determines how many processed channel results to dispatch for threaded IFFT processing per batch.
     *
     * Note: this value must be even.  Each dispatched batch then starts on an even block index, which lets downstream
     * channel output processors (e.g. TwoChannelOutputProcessor) derive the absolute block index parity from each
     * result's position within the batch.
     */
    private static final int PROCESSED_CHANNEL_RESULTS_THRESHOLD = 1024;

    /**
     * Maximum queued channel results batches awaiting IFFT, about one second of samples.  A full batch is dropped when
     * the IFFT processor falls behind, which preserves the even-sized batch block parity.
     */
    private static final int MAXIMUM_QUEUED_BATCHES = 50;

    /**
     * Maximum batch sample arrays held for reuse: the queued IFFT batches plus the batches queued for channel output.
     */
    private static final int MAXIMUM_POOLED_BATCHES = 2 * MAXIMUM_QUEUED_BATCHES;

    //Sized to process 40 times per second
    private IFFTProcessorDispatcher mIFFTProcessorDispatcher = new IFFTProcessorDispatcher(25);
    private FloatFFT_1D mFFT;
    private ChannelDFT mChannelDFT;
    private float[] mInlineSamples;
    private float[] mInlineFilter;

    /**
     * Number of additional blocks of space in the sample buffer.  The window of filtered samples moves through this
     * space one block at a time, so the window is copied once per this many blocks, instead of being shifted per block.
     */
    private static final int SAMPLE_BUFFER_SPARE_BLOCKS = 32;

    /**
     * Reusable sub-channel accumulator.  This is scratch space that is fully overwritten on each process() invocation
     * and never escapes the method, so it is allocated once here (sized to the sub-channel count) and reused rather
     * than being reallocated on every block.  process() is only ever invoked from the single-threaded polyphase buffer
     * dispatcher (see PolyphaseChannelManager), so reusing this buffer across calls is thread-safe.  It is (re)sized in
     * init() alongside the other rate-dependent buffers.
     */
    private float[] mFilterAccumulator;
    private boolean mTopBlockIndicator = true;
    private int[] mTopBlockMap;
    private int[] mMiddleBlockMap;
    private int mSampleBufferPointer;
    private int mWindowOffset;
    private int mSamplesPerBlock;
    private int mTapsPerChannel;
    private Queue<float[]> mBatchPool;
    private float[] mBatchSamples;
    private int mBatchBlockCount;
    private long mLatestSamplesTimestamp;

    /**
     * Creates a NMDPFB channelizer instance.
     *
     * @param taps of a low-pass filter designed for the inbound sample rate with a cutoff frequency
     * equal to the channel bandwidth (sample rate / filters).  If you need to synthesize (combine two or more
     * channel outputs) a new bandwidth signal from the outputs of this filter, then the filter should be designed
     * as a nyquist filter with -6 dB attenuation at the channel bandwidth cutoff frequency
     * @param sampleRate of the incoming sample stream
     * @param channelCount - number of filters/channels to output.  Since this filter bank performs 2x oversampling for
     * each channel output, this number must be even (divisible by 2).
     */
    public ComplexPolyphaseChannelizerM2(float[] taps, int sampleRate, int channelCount)
    {
        super(sampleRate, channelCount);

        if(channelCount % 2 != 0)
        {
            throw new IllegalArgumentException("Channel count must be an even multiple of the over-sample rate (2x)");
        }

        mTapsPerChannel = (int) FastMath.ceil((double)taps.length / (double)channelCount);

        init(taps);
    }

    /**
     * Creates a NMDPFB channelizer instance and designs a Perfect Reconstruction prototype filter appropriate for
     * the baseband sample rate and quantity of filter taps per polyphase sub-channel.
     *
     * @param sampleRate to be channelized.
     * @param tapsPerChannel to use when designing the filter
     */
    public ComplexPolyphaseChannelizerM2(double sampleRate, int tapsPerChannel) throws FilterDesignException
    {
        super(sampleRate, getChannelCount(sampleRate));

        float[] filterTaps = FilterFactory.getSincM2Channelizer(getChannelSampleRate(), getChannelCount(),
            tapsPerChannel, false);

        //The filter designer can increase the taps per channel in order to meet the band edge objective
        mTapsPerChannel = filterTaps.length / getChannelCount();

        init(filterTaps);
    }

    /**
     * Starts sample processing
     */
    public void start()
    {
        mIFFTProcessorDispatcher.start();
    }

    /**
     * Stops sample processing.
     */
    public void stop()
    {
        mIFFTProcessorDispatcher.stop();
        mBatchPool.clear();
    }

    /**
     * Calculates the multiple of two number of channels that can be channelized from the specified sample rate so that
     * each channel has a minimum bandwidth of the default channel bandwidth (25 kHz).  The channel count is the IFFT
     * size, so it is reduced as needed to have only factors of 2, 3 and 5, which keeps the IFFT efficient.  Other prime
     * factors are many times slower, for example 244 channels (2*2*61) takes 10x as long as 240 channels.
     * @param sampleRate to channelize
     * @return number of multiple of two channels that can be channelized.
     */
    public static int getChannelCount(double sampleRate)
    {
        int channels = (int)(sampleRate / DEFAULT_MINIMUM_CHANNEL_BANDWIDTH);

        if(channels % 2 != 0)
        {
            channels--;
        }

        while(channels > 2 && !hasOnlyFactorsOf2And3And5(channels))
        {
            channels -= 2;
        }

        mLog.info("Sample Rate [" + DECIMAL_FORMAT.format(sampleRate) + "] providing [" + channels +
            "] channels at [" + DECIMAL_FORMAT.format(sampleRate / (double)channels) + "] Hz each");

        return channels;
    }

    /**
     * Receives the complex sample buffer and processes the results through the channelizer.
     */
    @Override
    public void receive(InterleavedComplexSamples complexSamples)
    {
        mLatestSamplesTimestamp = complexSamples.timestamp();

        float[] samples = complexSamples.samples();

        int samplesPointer = 0;
        int samplesToCopy;

        while(samplesPointer < samples.length)
        {
            if(mSampleBufferPointer < mSamplesPerBlock)
            {
                samplesToCopy = mSamplesPerBlock - mSampleBufferPointer;

                int samplesDiff = samples.length - samplesPointer;
                if(samplesDiff < samplesToCopy)
                {
                    samplesToCopy = samplesDiff;
                }

                System.arraycopy(samples, samplesPointer, mInlineSamples, mWindowOffset + mSampleBufferPointer, samplesToCopy);

                mSampleBufferPointer += samplesToCopy;
                samplesPointer += samplesToCopy;
            }

            if(mSampleBufferPointer >= mSamplesPerBlock)
            {
                //Filter buffered samples and produce a single sample across each of the polyphase channels
                process();

                if(++mBatchBlockCount >= PROCESSED_CHANNEL_RESULTS_THRESHOLD)
                {
                    ChannelResultsBatch batch = new ChannelResultsBatch(mBatchSamples, getSubChannelCount(),
                        mLatestSamplesTimestamp, mBatchPool);

                    if(!mIFFTProcessorDispatcher.offer(batch))
                    {
                        batch.release();
                    }

                    mBatchSamples = getBatchSamples();
                    mBatchBlockCount = 0;
                }

                //Move the window back one block to make room for a new block of samples, or when the window is at the
                //start of the buffer, copy the samples that remain in the window to the end of the buffer
                mWindowOffset -= mSamplesPerBlock;

                if(mWindowOffset < 0)
                {
                    mWindowOffset = mSamplesPerBlock * SAMPLE_BUFFER_SPARE_BLOCKS;
                    System.arraycopy(mInlineSamples, 0, mInlineSamples, mWindowOffset + mSamplesPerBlock,
                        mInlineFilter.length - mSamplesPerBlock);
                }

                mSampleBufferPointer = 0;
            }
        }
    }

    /**
     * Indicates if the value has no prime factors other than 2, 3 and 5.
     */
    private static boolean hasOnlyFactorsOf2And3And5(int value)
    {
        for(int factor : new int[]{2, 3, 5})
        {
            while(value % factor == 0)
            {
                value /= factor;
            }
        }

        return value == 1;
    }

    /**
     * Creates a top-block processing accumulator map that maps each interim filter and sample index product
     * to the corresponding final output index for the array that will feed the IFFT.
     *
     * @param channelCount - number of channels
     * @return output index to filter accumulator index mapping
     */
    private static int[] getTopBlockMap(int channelCount)
    {
        int[] newMap = new int[channelCount * 2];

        //Reorder the subchannel arrays to the structure needed for top-block processing
        int blockSize = channelCount / 2;

        for(int channel = 0; channel < blockSize; channel++)
        {
            int newIndex = 2 * channel;
            int originalIndex = 2 * (blockSize - channel - 1);
            int offset = 2 * blockSize;

            newMap[originalIndex] = newIndex;
            newMap[originalIndex + 1] = newIndex + 1;
            newMap[offset + originalIndex] = offset + newIndex;
            newMap[offset + originalIndex + 1] = offset + newIndex + 1;
        }

        return newMap;
    }

    /**
     * Creates a top-block processing accumulator map that maps each interim filter and sample index product
     * to the corresponding final output index for the array that will feed the IFFT.
     *
     * @param channelCount - number of channels
     * @return output index to filter accumulator index mapping
     */
    private static int[] getMiddleBlockMap(int channelCount)
    {
        int[] newMap = new int[channelCount * 2];

        //Reorder the subchannel arrays to the structure needed for top-block processing
        int blockSize = channelCount / 2;

        for(int channel = 0; channel < blockSize; channel++)
        {
            int newIndex = 2 * channel;
            int originalIndex = 2 * (blockSize - channel - 1);
            int offset = 2 * blockSize;

            newMap[offset + originalIndex] = newIndex;
            newMap[offset + originalIndex + 1] = newIndex + 1;
            newMap[originalIndex] = offset + newIndex;
            newMap[originalIndex + 1] = offset + newIndex + 1;
        }

        return newMap;
    }

    /**
     * Rearranges the filter coefficients to align with a contiguous sample buffer for processing efficiency.
     * @param coefficients of the polyphase filter
     * @param channelCount number of channels where each channel is an I/Q pair
     * @return filter rearranged for inline sample buffer processing
     */
    private static float[] getAlignedFilter(float[] coefficients, int channelCount, int tapsPerChannel)
    {
        float[] filter = new float[channelCount * tapsPerChannel * 2];
        int blockSize = channelCount;

        int coefficientPointer = 0;
        int filterPointer = 0;

        //Create a new filter that duplicates each tap to produce an interleaved I/Q filter
        while(coefficientPointer < coefficients.length)
        {
            filter[filterPointer++] = coefficients[coefficientPointer];
            filter[filterPointer++] = coefficients[coefficientPointer++];
        }

        //Swap each of the coefficients on block size boundaries
        for(int x = 0; x < filter.length; x += blockSize)
        {
            for(int y = 0; y < blockSize / 2; y++)
            {
                int index1 = x + y;
                int index2 = x + (blockSize - y - 1);
                float temp = filter[index2];
                filter[index2] = filter[index1];
                filter[index1] = temp;
            }
        }

        return filter;
    }


    /**
     * Provides a reused batch sample array when one is available, or a new array.  Reused arrays hold old samples, which
     * doesn't matter since each batch is fully overwritten.
     */
    private float[] getBatchSamples()
    {
        float[] samples = mBatchPool.poll();
        return samples != null ? samples : new float[PROCESSED_CHANNEL_RESULTS_THRESHOLD * getSubChannelCount()];
    }

    /**
     * Processes the sample buffer for each new block of sample data that is loaded and writes the results to the next
     * block of the current batch.
     *
     * Note: mInlineFilter and the window of mInlineSamples are both exactly (subChannelCount * tapsPerChannel) in length
     * (see init() and getAlignedFilter()), so the fused multiply-accumulate loop below indexes every element of each
     * exactly once.  The multiply and per-sub-channel accumulation are fused into a single pass, which removes the previous
     * per-call interim product array entirely and preserves bit-for-bit identical output: for each sub-channel the
     * tap products are summed in the same ascending tap order as before, and the JVM does not contract the separate
     * float multiply/add into an FMA without an explicit Math.fma() call.
     */
    private void process()
    {
        final int subChannelCount = getSubChannelCount();
        final int tapsPerChannel = mTapsPerChannel;
        final float[] inlineSamples = mInlineSamples;
        final float[] inlineFilter = mInlineFilter;
        final float[] filterAccumulator = mFilterAccumulator;
        final int windowOffset = mWindowOffset;

        //Reset the reused accumulator to zero before summing this block's tap products
        Arrays.fill(filterAccumulator, 0.0f);

        //Fused multiply-accumulate: multiply each sample by its corresponding filter tap and accumulate the product
        //directly into the appropriate I/Q sub-channel accumulator.
        for(int tap = 0; tap < tapsPerChannel; tap++)
        {
            final int tapOffset = tap * subChannelCount;

            for(int channel = 0; channel < subChannelCount; channel++)
            {
                final int index = tapOffset + channel;
                filterAccumulator[channel] += inlineSamples[windowOffset + index] * inlineFilter[index];
            }
        }

        final float[] batchSamples = mBatchSamples;
        final int batchOffset = mBatchBlockCount * subChannelCount;

        //Rearrange the accumulated sub-channels into the order required by the IFFT, alternating between the
        //top-block and middle-block orderings on successive blocks.
        final int[] blockMap = mTopBlockIndicator ? mTopBlockMap : mMiddleBlockMap;

        for(int x = 0; x < subChannelCount; x++)
        {
            batchSamples[batchOffset + x] = filterAccumulator[blockMap[x]];
        }

        mTopBlockIndicator = !mTopBlockIndicator;
    }

    /**
     * Initializes the channelizer filter structures.
     *
     * @param coefficients of the prototype filter for this channelizer
     */
    private void init(float[] coefficients)
    {
        int channelCount = getChannelCount();
        mFFT = new FloatFFT_1D(channelCount);
        mChannelDFT = ChannelDFTFactory.getChannelDFT(channelCount);
        int bufferLength = getSubChannelCount() * mTapsPerChannel;
        mSamplesPerBlock = channelCount; //Same as subChannelCount / 2
        mTopBlockMap = getTopBlockMap(channelCount);
        //The prototype filter group delay (taps per channel * channel count / 2 samples) rotates each channel k by
        //PI * k * tapsPerChannel, so with an odd taps per channel count adjacent channel outputs are 180 degrees out of
        //phase.  Starting with the middle block map applies an M/2 circular shift ahead of the IFFT, which multiplies
        //channel k by (-1)^k and makes adjacent channels phase coherent for synthesis.
        mTopBlockIndicator = (mTapsPerChannel % 2 == 0);
        mMiddleBlockMap = getMiddleBlockMap(channelCount);
        mInlineFilter = getAlignedFilter(coefficients, channelCount, mTapsPerChannel);
        mWindowOffset = mSamplesPerBlock * SAMPLE_BUFFER_SPARE_BLOCKS;
        mInlineSamples = new float[mWindowOffset + bufferLength];
        mFilterAccumulator = new float[getSubChannelCount()];
        mBatchPool = new ArrayBlockingQueue<>(MAXIMUM_POOLED_BATCHES);
        mBatchSamples = getBatchSamples();
    }

    /**
     * Separate threaded processor to receive and enqueue filtered channel results batches, transform each block as
     * required to align the phase of each polyphase channel, and then dispatch the results to any registered sample
     * consumer channels.  When few channels are in use, only those channels are transformed, and the results for all
     * other channels are not valid.
     */
    public class IFFTProcessorDispatcher extends Dispatcher<ChannelResultsBatch>
    {
        public IFFTProcessorDispatcher(long interval)
        {
            super("sdrtrunk polyphase ifft processor", interval, MAXIMUM_QUEUED_BATCHES);

            //We create a listener interface to receive the batched channel results from the scheduled thread pool
            //dispatcher thread that is part of this continuous buffer processor.  We perform an IFFT on each
            //block in the batch and then dispatch the batch so that it can be distributed to each channel listener.
            setListener(batch -> {
                try
                {
                    float[] samples = batch.getSamples();
                    int[] channels = prepareChannels();

                    if(channels.length > 0 && channels.length <= mChannelDFT.getMaximumChannels())
                    {
                        //Rotate only the channels that are in use to the correct phase
                        mChannelDFT.process(samples, channels);
                    }
                    else
                    {
                        int blockLength = batch.getBlockLength();

                        for(int offset = 0; offset < samples.length; offset += blockLength)
                        {
                            //Rotate each of the channels to the correct phase using the IFFT
                            mFFT.complexInverse(samples, offset, true);
                        }
                    }

                    dispatch(batch);
                }
                catch(Throwable t)
                {
                    mLog.error("Error during IFFT and dispatch of processed channel results", t);
                }
                finally
                {
                    batch.release();
                }
            });
        }
    }
}
