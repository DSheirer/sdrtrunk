/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Implements a two-channel polyphase filer synthesizer.  This class is intended to be used with an M2 polyphase
 * channelizer in order to recover a signal that overlaps the boundary between two channels.  This synthesizer will
 * rejoin the two M2 neighboring channels, mix the signal of interest to baseband, and filter the the output stream to
 * half of the sample rate, producing an overall M2 output sample rate.
 *
 * The synthesizer concept is modeled on the structure described by Fred Harris et al, in 'Interleaving Different
 * Bandwidth Narrowband Channels in Perfect Reconstruction Cascade Polyphase Filter Banks for Efficient Flexible
 * Variable Bandwidth Filters in Wideband Digital Transceivers'.
 *
 * The data buffers and sub-filter kernels bear no resemblance to the structures defined in the paper.  The data and
 * filter structures in this class are organized to take advantage of Java's ability to leverage SIMD processor
 * intrinsics for optimal efficiency in processing.  The paper specifies splitting the filter kernel into N polyphase
 * partitions and creating N data buffers.  This class organizes the data buffer as a contiguous sample
 * array and creates two copies of the filter kernel, one arranged for normal filtering and the second arranged for
 * phase-shifted filtering.  Instead of calculating the dot-product for each sub-filter, we calculate the product of the
 * full data array against the filter and then accumulate each sub-filter.  This allows java to use SIMD for the array
 * product and then normal processing for the accumulation.  Since this is a two-channel processor and the results of
 * each filter accumulation are added, we use a single accumulator across both filters.
 *
 */
public class TwoChannelSynthesizerM2
{
    private final static Logger mLog = LoggerFactory.getLogger(TwoChannelSynthesizerM2.class);

    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("Two Channel Synthesizer M2");
    private float[] mDataBuffer;
    private float[] mNormalFilter;
    private float[] mPhaseShiftedFilter;
    private float[] mFilterProduct;
    private float[] mIFFTBuffer = new float[4];
    private float mIAccumulator;
    private float mQAccumulator;
    private FloatFFT_1D mFFT = new FloatFFT_1D(2);
    private boolean mFlag = true;

    /**
     * Polyphase synthesizer for mixing two M2 oversampled channels into a composite channel using
     * perfect reconstruction filters (-6db at band edge).  Output sample rate is the same as the
     * input sample rate of one of the channels.
     */
    public TwoChannelSynthesizerM2(float[] filter)
    {
        init(filter);
    }

    /**
     * Partitions the filter into 2 polyphase partitions and uses only the first filter.  The second
     * filter partition and all corresponding samples represent the oversampling excess that will be
     * thrown away.
     *
     * We create two instances of filter 1 to simplify buffer management where we have to swap the data
     * buffer at each sample to correct for phase shift introduced by buffer swapping when down sampling
     * from the M2 sample rates of each channel.
     *
     * @param filter to partition into 2 polyphase filters.
     */
    private void init(float[] filter)
    {
        int tapsPerChannel = (int)Math.ceil(filter.length / 2);

        mNormalFilter = getNormalFilter(filter, tapsPerChannel);
        mPhaseShiftedFilter = getPhaseShiftedFilter(filter, tapsPerChannel);
        mDataBuffer = new float[mNormalFilter.length];
        mFilterProduct = new float[mNormalFilter.length];
    }

    /**
     * Synthesizes a new channel from the channel 1 and channel 2 sample arrays that are arranged as
     * i0, q0, i1, q1 ... iN-1, qN-1
     *
     * @param channelBuffer1 input channel
     * @param channelBuffer2 input channel
     * @return synthesized channel results of the same length as both channel 1/2 input arrays.
     */
    public ReusableComplexBuffer process(ReusableComplexBuffer channelBuffer1, ReusableComplexBuffer channelBuffer2)
    {
        if(channelBuffer1.getSamples().length != channelBuffer1.getSamples().length)
        {
            throw new IllegalArgumentException("Channel 1 and 2 array length must be equal");
        }

        float[] channel1 = channelBuffer1.getSamples();
        float[] channel2 = channelBuffer2.getSamples();

        ReusableComplexBuffer synthesizedComplexBuffer = mReusableComplexBufferQueue.getBuffer(channel1.length);

        float[] output = synthesizedComplexBuffer.getSamples();

        for(int x = 0; x < channel1.length; x += 2)
        {
            mIFFTBuffer[0] = channel1[x];     //i
            mIFFTBuffer[1] = channel1[x + 1]; //q
            mIFFTBuffer[2] = channel2[x];     //i
            mIFFTBuffer[3] = channel2[x + 1]; //q

            mFFT.complexInverse(mIFFTBuffer, true);

            System.arraycopy(mDataBuffer, 0, mDataBuffer, 4, mDataBuffer.length - 4);
            System.arraycopy(mIFFTBuffer, 0, mDataBuffer, 0, mIFFTBuffer.length);

            if(mFlag)
            {
                for(int y = 0; y < mDataBuffer.length; y++)
                {
                    mFilterProduct[y] = mDataBuffer[y] * mNormalFilter[y];
                }
            }
            else
            {
                for(int y = 0; y < mDataBuffer.length; y++)
                {
                    mFilterProduct[y] = mDataBuffer[y] * mPhaseShiftedFilter[y];
                }
            }

            mIAccumulator = 0.0f;
            mQAccumulator = 0.0f;

            for(int y = 0; y < mFilterProduct.length; y += 2)
            {
                mIAccumulator += mFilterProduct[y];
                mQAccumulator += mFilterProduct[y + 1];
            }

            output[x] = mIAccumulator;
            output[x + 1] = mQAccumulator;

            mFlag = !mFlag;
        }

        channelBuffer1.decrementUserCount();
        channelBuffer2.decrementUserCount();

        synthesizedComplexBuffer.incrementUserCount();

        return synthesizedComplexBuffer;
    }

    private static float[] getNormalFilter(float[] coefficients, int tapsPerChannel)
    {
        float[] filter = new float[4 * tapsPerChannel];

        int idealLength = 2 * tapsPerChannel;

        int offset = idealLength - 2;

        int index;

        for(int x = 0; x < idealLength; x += 2)
        {
            index = offset - x;

            if(index < coefficients.length)
            {
                //Filter 1
                filter[2 * x + 0] = coefficients[index];
                filter[2 * x + 1] = coefficients[index];
            }

            index = offset - x + 1;

            if(index < coefficients.length)
            {
                //Filter 2
                filter[2 * x + 2] = coefficients[index];
                filter[2 * x + 3] = coefficients[index];
            }
        }

        return filter;
    }

    private static float[] getPhaseShiftedFilter(float[] coefficients, int tapsPerChannel)
    {
        float[] filter = new float[4 * tapsPerChannel];

        int idealLength = 2 * tapsPerChannel;

        int offset = idealLength - 2;

        int index;

        for(int x = 0; x < idealLength; x += 2)
        {
            index = offset - x + 1;

            if(index < coefficients.length)
            {
                //Filter 2
                filter[2 * x + 0] = coefficients[index];
                filter[2 * x + 1] = coefficients[index];
            }

            index = offset - x;

            if(index < coefficients.length)
            {
                //Filter 1
                filter[2 * x + 2] = coefficients[index];
                filter[2 * x + 3] = coefficients[index];
            }
        }

        return filter;
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting");

        int sampleCount = 30;

        try
        {
//            float[] taps = FilterFactory.getSincM2Synthesizer(12500.0, 2, 20, Window.WindowType.BLACKMAN_HARRIS_7, true);

            float[] taps = new float[15];
            for(int x = 0; x < taps.length; x++)
            {
                taps[x] = x;
            }

            float[] normalFilter = getPhaseShiftedFilter(taps, 8);

            mLog.debug("Filter: " + Arrays.toString(normalFilter));

//            TwoChannelSynthesizerM2 synthesizer = new TwoChannelSynthesizerM2(taps);
//
//            IOscillator oscillator = new LowPhaseNoiseOscillator(3000.0, 25000.0);
//
//            float[] channel1 = oscillator.generateComplex(sampleCount);
//            float[] channel2 = new float[sampleCount * 2];
//
//            float[] channel1 = new float[16];
//            float[] channel2 = new float[16];
//            for(int x = 1; x <= 16; x++)
//            {
//                channel1[x - 1] = x;
//                channel2[x - 1] = x + 16;
//            }
//
//
//            float[] synthesized = synthesizer.process(channel1, channel2);
//
//            mLog.debug("1:" + Arrays.toString(channel1));
//            mLog.debug("X:" + Arrays.toString(synthesized));
        }
        catch(Exception fde)
        {
            mLog.error("Filter design error", fde);
        }

        mLog.debug("Finished!!");
    }
}
