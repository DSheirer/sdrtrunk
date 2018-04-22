/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableChannelResultsBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import io.github.dsheirer.sample.real.IOverflowListener;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ComplexPolyphaseChannelizerM2 extends AbstractComplexPolyphaseChannelizer
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexPolyphaseChannelizerM2.class);

    private static final int DEFAULT_MINIMUM_CHANNEL_BANDWIDTH = 12500;

    //Sized at 152 buffers a second where max = 5 seconds and reset = 2 seconds worth of buffers
    private IFFTProcessor mIFFTProcessor = new IFFTProcessor((5 * 152), (2 * 152));
    private FloatFFT_1D mFFT;
    private float[] mInlineSamples;
    private float[] mInlineFilter;
    private float[] mInlineInterimOutput;
    private float[] mFilterAccumulator;
    private boolean mTopBlockIndicator = true;
    private int[] mTopBlockMap;
    private int[] mMiddleBlockMap;
    private int mSampleBufferPointer;
    private int mSamplesPerBlock;
    private int mTapsPerChannel;

    /**
     * Non-Maximally Decimated Polyphase Filter Bank (NMDPFB) channelizer that divides the input frequency band into
     * equal bandwidth channels that are each oversampled by 2x for output.
     *
     * This polyphase channelizer is based off of the channelizer described by Fred Harris in Multirate Signal
     * Processing for Communications Systems, p230-233.
     *
     * Samples are loaded into this filter one block at a time (1/2 channel count) and a filtered output is calculated
     * to produce an overall 2x oversampled channel sample rate.  Each sample block load is preceded by a serpentine
     * shift of the existing sample blocks.  We use the java System.arrayCopy() method which is able to leverage
     * native processor intrinsics for efficiency.
     *
     * The prototype filter for the channelizer is rearranged to align with the structure of the sample buffer.
     *
     * Instead of using an array of channel filters as described in the Harris text, this filter and the sample buffer
     * are arranged as a contiguous array to maximize Java's ability to leverage native processor Single Instruction
     * Multiple Data (SIMD) intrinsics (since Java 8).  The filter process is broken into three steps:
     *   -Multiply the inline array of samples and filter coefficients
     *   -Accumulate the results for each sub-channel
     *   -Rearrange the sub-channel results to correctly order the sub-channels
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

        mTapsPerChannel = (int)Math.ceil((double)taps.length / (double)channelCount);

        init(taps);
    }

    /**
     * Non-Maximally Decimated Polyphase Filter Bank (NMDPFB) channelizer that divides the input frequency band into
     * equal bandwidth channels that are each oversampled by 2x for output.
     *
     * This polyphase channelizer is based off of the channelizer described by Fred Harris in Multirate Signal
     * Processing for Communications Systems, p230-233.
     *
     * Samples are loaded into this filter one block at a time (1/2 channel count) and a filtered output is calculated
     * to produce an overall 2x oversampled channel sample rate.  Each sample block load is preceded by a serpentine
     * shift of the existing sample blocks.  We use the java System.arrayCopy() method which is able to leverage
     * native processor intrinsics for efficiency.
     *
     * The prototype filter for the channelizer is rearranged to align with the structure of the sample buffer.
     *
     * Instead of using an array of channel filters as described in the Harris text, this filter and the sample buffer
     * are arranged as a contiguous array to maximize Java's ability to leverage native processor Single Instruction
     * Multiple Data (SIMD) intrinsics (since Java 8).  The filter process is broken into three steps:
     *   -Multiply the inline array of samples and filter coefficients
     *   -Accumulate the results for each sub-channel
     *   -Rearrange the sub-channel results to correctly order the sub-channels
     *
     * @param sampleRate to be channelized.
     * @param tapsPerChannel to use when designing the filter
     */
    public ComplexPolyphaseChannelizerM2(double sampleRate, int tapsPerChannel) throws FilterDesignException
    {
        super(sampleRate, getChannelCount(sampleRate));

        mTapsPerChannel = tapsPerChannel;

        float[] filterTaps = FilterFactory.getSincM2Channelizer(getChannelSampleRate(), getChannelCount(),
            mTapsPerChannel, WindowType.BLACKMAN_HARRIS_7, true);

        init(filterTaps);
    }

    /**
     * Starts sample processing
     */
    public void start()
    {
        mIFFTProcessor.start();
    }

    /**
     * Stops sample processing.
     */
    public void stop()
    {
        mIFFTProcessor.stop();
    }

    /**
     * Calculates the multiple of two number of channels that can be channelized from the specified sample rate so that
     * each channel has a minimum bandwidth of the default channel bandwidth (12.5 kHz).
     * @param sampleRate to channelize
     * @return number of multiple of two channels that can be channelized.
     */
    private static int getChannelCount(double sampleRate)
    {
        int channels = (int)(sampleRate / DEFAULT_MINIMUM_CHANNEL_BANDWIDTH);

        if(channels % 2 != 0)
        {
            channels--;
        }

        mLog.debug("Sample Rate [" + sampleRate + "] will produce [" + channels + "] channels at [" + (sampleRate / (double)channels) + "] each");
        return channels;
    }

    /**
     * Updates this channelizer to use the new sample rate.  This method creates a new filter suitable for the
     * sample rate and reinitializes all internal data structures to prepare for processing the new sample rate.
     * @param sampleRate in hertz
     */
    @Override
    public void setSampleRate(double sampleRate)
    {
        try
        {
            super.setSampleRate(sampleRate);

            float[] filterTaps = FilterFactory.getSincM2Channelizer(getChannelSampleRate(), getChannelCount(),
                mTapsPerChannel, WindowType.BLACKMAN_HARRIS_7, true);

            init(filterTaps);
        }
        catch(FilterDesignException fde)
        {
            throw new IllegalArgumentException("Cannot create a channelizer filter for the specified sample rate [" +
                sampleRate + "]");
        }
    }

    /**
     * Receives the complex sample buffer and processes the results through the channelizer.
     */
    @Override
    public void receive(ReusableComplexBuffer reusableComplexBuffer)
    {
        long start = System.nanoTime();
        long processing = 0;

        ReusableChannelResultsBuffer channelResultsBuffer = getChannelResultsBuffer();
        channelResultsBuffer.setTimestamp(reusableComplexBuffer.getTimestamp());

        float[] samples = reusableComplexBuffer.getSamples();

        int samplesPointer = 0;
        int samplesToCopy;

        while(samplesPointer < samples.length)
        {
            if(mSampleBufferPointer < mSamplesPerBlock)
            {
                samplesToCopy = mSamplesPerBlock - mSampleBufferPointer;

                if((samples.length - samplesPointer) < samplesToCopy)
                {
                    samplesToCopy = (samples.length - samplesPointer);
                }

                System.arraycopy(samples, samplesPointer, mInlineSamples, mSampleBufferPointer, samplesToCopy);

                mSampleBufferPointer += samplesToCopy;
                samplesPointer += samplesToCopy;
            }

            if(mSampleBufferPointer >= mSamplesPerBlock)
            {
                //Filter buffered samples and produce a single sample across each of the polyphase channels
                processing += process(channelResultsBuffer);

                //Right-shift the samples in the buffer over to make room for a new block of samples
                //Note: since JDK 8, hotspot JIT compiler uses native processor intrinsics for efficiency
                System.arraycopy(mInlineSamples, 0, mInlineSamples, mSamplesPerBlock, (mInlineSamples.length - mSamplesPerBlock));
                mSampleBufferPointer = 0;
            }
        }

        channelResultsBuffer.incrementUserCount();

        //Enqueue the channel results buffer for IFFT processing and distribution on a different thread
        mIFFTProcessor.receive(channelResultsBuffer);

//        mLog.debug("Duration: " + (System.nanoTime() - start) + " Processing:" + processing);
        //Decrement the user count to let the originator know we're done with their buffer
        reusableComplexBuffer.decrementUserCount();
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
     * Processes the sample buffer for each new block of sample data that is loaded and distributes the results to any
     * registered channel listeners.
     */
    private long process(ReusableChannelResultsBuffer channelResultsBuffer)
    {
        //Multiply each of the samples by the corresponding filter tap
        for(int x = 0; x < mInlineSamples.length; x++)
        {
            mInlineInterimOutput[x] = mInlineSamples[x] * mInlineFilter[x];
        }

        long start = System.nanoTime();

        Arrays.fill(mFilterAccumulator, 0.0f);

        int tapOffset = 0;

        //Accumulate the sample/filter product results into each of the I/Q sub-channels
        for(int tap = 0; tap < mTapsPerChannel; tap++)
        {
            tapOffset = tap * getSubChannelCount();

            for(int channel = 0; channel < getSubChannelCount(); channel++)
            {
                mFilterAccumulator[channel] += mInlineInterimOutput[tapOffset + channel];
            }
        }

        float[] processed = channelResultsBuffer.getEmptyBuffer(getSubChannelCount());

        if(mTopBlockIndicator)
        {
            for(int x = 0; x < getSubChannelCount(); x++)
            {
                processed[x] = mFilterAccumulator[mTopBlockMap[x]];
            }
        }
        else
        {
            for(int x = 0; x < getSubChannelCount(); x++)
            {
                processed[x] = mFilterAccumulator[mMiddleBlockMap[x]];
            }
        }

        channelResultsBuffer.addChannelResults(processed);

        mTopBlockIndicator = !mTopBlockIndicator;

        long elapsed = System.nanoTime() - start;

        return elapsed;
    }

//    /**
//     * Buffers the channel results and dispatches the channel results buffer to the channel output processors when full.
//     * @param buffer containing filtered and IFFT'd channelResults with a corresponding timestamp
//     */
//    private void processChannelResults(ReusableChannelResultsBuffer buffer)
//    {
//        if(mChannelResultsBuffer == null)
//        {
//            mChannelResultsBuffer = new PolyphaseChannelResultsBuffer(buffer.getTimestamp(),
//                mChannelResultsBufferSize);
//        }
//
//        try
//        {
//            mChannelResultsBuffer.add(buffer.getSamplesCopy());
//        }
//        catch(IllegalArgumentException iae)
//        {
//            //If the buffer is full (unlikely) or the channel results array length has changed (possible), flush the
//            //current buffer, create a new one, and store the current results
//            flushChannelResultsBuffer();
//
//            mChannelResultsBuffer = new PolyphaseChannelResultsBuffer(buffer.getTimestamp(), mChannelResultsBufferSize);
//
//            mChannelResultsBuffer.add(buffer.getSamplesCopy());
//        }
//
//        if(mChannelResultsBuffer.isFull())
//        {
//            flushChannelResultsBuffer();
//        }
//
//        buffer.decrementUserCount();
//    }

//    /**
//     * Dispatches the non-empty channel results buffer to the channel output processors and nullifies the reference to
//     * the buffer so that a new buffer can be created upon receiving the next channel results.
//     */
//    private void flushChannelResultsBuffer()
//    {
//        if(mChannelResultsBuffer != null && !mChannelResultsBuffer.isEmpty())
//        {
//            dispatch(mChannelResultsBuffer);
//        }
//
//        mChannelResultsBuffer = null;
//    }

    /**
     * Initializes the channelizer filter structures.
     *
     * @param coefficients of the prototype filter for this channelizer
     */
    private void init(float[] coefficients)
    {
        //Setup the FFT
        mFFT = new FloatFFT_1D(getChannelCount());

        int channelCount = getChannelCount();
        int bufferLength = getSubChannelCount() * mTapsPerChannel;

        mSamplesPerBlock = getChannelCount(); //Same as subChannelCount / 2
        mTopBlockMap = getTopBlockMap(channelCount);
        mMiddleBlockMap = getMiddleBlockMap(channelCount);
        mInlineFilter = getAlignedFilter(coefficients, channelCount, mTapsPerChannel);
        mInlineSamples = new float[bufferLength];
        mInlineInterimOutput = new float[bufferLength];
        mFilterAccumulator = new float[getSubChannelCount()];
    }

    /**
     * Separate threaded processor to receive and enqueue filtered channel results buffers, perform IFFT on each array
     * as required to align the phase of each polyphase channel, and then dispatch the results to any registered
     * sample consumer channels.
     */
    public class IFFTProcessor extends ContinuousBufferProcessor<ReusableChannelResultsBuffer>
    {
        public IFFTProcessor(int maximumSize, int resetThreshold)
        {
            super(maximumSize, resetThreshold);

            //We create a listener interface to receive the buffers from the scheduled thread pool
            //dispatcher thread that is part of this continuous buffer processor.  We perform an IFFT on each
            //channel results array contained in each results buffer and then dispatch the buffer
            //so that it can be distributed to each channel listener.
            setListener(new Listener<List<ReusableChannelResultsBuffer>>()
            {
                @Override
                public void receive(List<ReusableChannelResultsBuffer> buffers)
                {
                    for(ReusableChannelResultsBuffer buffer: buffers)
                    {
                        for(float[] channelResults: buffer.getChannelResults())
                        {
                            //Rotate each of the channels to the correct phase using the IFFT
                            mFFT.complexInverse(channelResults, true);
                        }

                        dispatch(buffer);
                    }
                }
            });

            setOverflowListener(new IOverflowListener()
            {
                @Override
                public void sourceOverflow(boolean overflow)
                {
                    mLog.debug("IFFTProcessor overflow changed - overflow:" + overflow);
                }
            });
        }

        /**
         * Clears any buffers from the dispatch/processing queue.  Overrides the parent method so that we can set
         * the user count to 0 to allow the buffer to be reclaimed.
         */
        protected Collection<ReusableChannelResultsBuffer> clearQueue()
        {
            Collection<ReusableChannelResultsBuffer> buffersToDispose = super.clearQueue();

            for(ReusableChannelResultsBuffer buffer: buffersToDispose)
            {
                buffer.clearUserCount();
            }

            return buffersToDispose;
        }
    }

    public static void main(String[] args)
    {
        int channelCount = 4;
        int tapsPerChannel = 8;
        float[] taps = new float[channelCount * tapsPerChannel];

        int sampleRate = 50000;

        for(int x = 0; x < taps.length; x++)
        {
            taps[x] = x;
        }

        ComplexPolyphaseChannelizerM2 channelizer = new ComplexPolyphaseChannelizerM2(taps, sampleRate, channelCount);

        ReusableComplexBufferQueue queue = new ReusableComplexBufferQueue();

        int sampleCount = channelCount * 2 * tapsPerChannel;
        ReusableComplexBuffer buffer = queue.getBuffer(sampleCount);
        buffer.incrementUserCount();

        for(int x = 0; x < sampleCount; x++)
        {
            buffer.getSamples()[x] = x;
        }

        channelizer.receive(buffer);
    }
}
