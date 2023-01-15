/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.Arrays;
import org.apache.commons.math3.util.FastMath;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a two-channel polyphase filer synthesizer.  This class is intended to be used with an M2 polyphase
 * channelizer in order to recover a signal that overlaps the boundary between two channels.  This synthesizer will
 * rejoin the two M2 neighboring channels, mix the signal of interest to baseband, and filter the output stream to
 * half of the sample rate, producing an overall M2 output sample rate.
 *
 * The synthesizer concept is modeled on the structure described by Fred Harris et al, in 'Interleaving Different
 * Bandwidth Narrowband Channels in Perfect Reconstruction Cascade Polyphase Filter Banks for Efficient Flexible
 * Variable Bandwidth Filters in Wideband Digital Transceivers'.
 *
 * The data buffers and sub-filter kernels bear some resemblance to the structures defined in the paper.  The data and
 * filter structures in this class are organized to take advantage of Java's ability to leverage SIMD processor
 * intrinsics for optimal efficiency in processing.  The paper specifies splitting the filter kernel into N polyphase
 * partitions and creating N data buffers.  This class organizes the data buffer as a contiguous sample
 * array and creates an I/Q interleaved filter kernel.  Instead of calculating the dot-product for each sub-filter,
 * we calculate the product of the full data array against the filter and then accumulate each sub-filter.  This allows
 * java to use SIMD for the array product and then normal processing for the accumulation.  Since this is a two-channel
 * processor and the results of each filter accumulation are added, we use a single accumulator across both filters.
 *
 */
public class TwoChannelSynthesizerM2
{
    private final static Logger mLog = LoggerFactory.getLogger(TwoChannelSynthesizerM2.class);
    private float[] mSerpentineDataBuffer;
    private float[] mIQInterleavedFilter;
    private float[] mFilterVectorProduct;
    private float mIAccumulator;
    private float mQAccumulator;
    private FloatFFT_1D mFFT = new FloatFFT_1D(2);
    private boolean mTopBlockFlag = true;

    /**
     * Polyphase synthesizer for mixing two M2 oversampled channels into a composite channel using perfect
     * reconstruction filters (-6db at band edge).  Output sample rate is the same as the input sample rate of one of
     * the channels.
     */
    public TwoChannelSynthesizerM2(float[] filter)
    {
        init(filter);
    }

    /**
     * Description of the state/configuration of this synthesizer to support debugging.
     * @return state description.
     */
    public String getStateDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Two-Channel Synthesizer");
        sb.append("\n\t\t Interleaved Filter: " + Arrays.toString(mIQInterleavedFilter));
        sb.append("\n\t\t Serpentine Buffer Length: ").append(mSerpentineDataBuffer.length);
        sb.append("\n\t\t Filter Vector Product Length: ").append(mFilterVectorProduct.length);
        return sb.toString();
    }

    /**
     * Initializes the synthesizer filter and data buffers for operation.
     *
     * @param filter to use for polyphase synthesis of two channels.
     */
    private void init(float[] filter)
    {
        int tapsPerChannel = (int) FastMath.ceil(filter.length / 2);
        mIQInterleavedFilter = getInterleavedFilter(filter, tapsPerChannel);
        mSerpentineDataBuffer = new float[mIQInterleavedFilter.length];
        mFilterVectorProduct = new float[mIQInterleavedFilter.length];
    }

    /**
     * Synthesizes a new channel from the channel 1 and channel 2 sample arrays that are arranged as
     * i0, q0, i1, q1 ... iN-1, qN-1
     *
     * @param channelBuffer1 input channel
     * @param channelBuffer2 input channel
     * @return synthesized channel results of the same length as both channel 1/2 input arrays.
     */
    public ComplexSamples process(ComplexSamples channelBuffer1, ComplexSamples channelBuffer2)
    {
        if(channelBuffer1.i().length != channelBuffer1.i().length)
        {
            throw new IllegalArgumentException("Channel 1 and 2 array length must be equal");
        }

        float[] iCh1 = channelBuffer1.i();
        float[] qCh1 = channelBuffer1.q();
        float[] iCh2 = channelBuffer2.i();
        float[] qCh2 = channelBuffer2.q();

        float[] i = new float[iCh1.length];
        float[] q = new float[qCh1.length];

        float[] IFFTBuffer = new float[4];

        for(int x = 0; x < iCh1.length; x++)
        {
            //Load samples from each channel into buffer for IFFT
            IFFTBuffer[0] = iCh1[x];     //i
            IFFTBuffer[1] = qCh1[x]; //q
            IFFTBuffer[2] = iCh2[x];     //i
            IFFTBuffer[3] = qCh2[x]; //q

            //Perform Inverse FFT (IFFT)
            mFFT.complexInverse(IFFTBuffer, true);

            //Perform serpentine shift of data blocks in the data buffer - make room for 4 new samples
            System.arraycopy(mSerpentineDataBuffer, 0, mSerpentineDataBuffer, 4, mSerpentineDataBuffer.length - 4);

            //Top Block - load samples into data buffer in normal order
            if(mTopBlockFlag)
            {
                System.arraycopy(IFFTBuffer, 0, mSerpentineDataBuffer, 0, IFFTBuffer.length);
            }
            //Bottom Block - swap samples via data loading to account for phase shift
            else
            {
                System.arraycopy(IFFTBuffer, 0, mSerpentineDataBuffer, 2, 2);
                System.arraycopy(IFFTBuffer, 2, mSerpentineDataBuffer, 0, 2);
            }

            //Note: in order to use Java's ability to leverage SIMD intrinsics, we perform filtering in two steps
            //(multiply then accumulate) since our filter and data are structured with I and Q vectors interleaved.
            //This approach allows the Hotspot compiler to more easily recognize the scalor operations.

            //Multiply data samples by the I/Q interleaved filter to form the vector product
            for(int y = 0; y < mSerpentineDataBuffer.length; y++)
            {
                mFilterVectorProduct[y] = mSerpentineDataBuffer[y] * mIQInterleavedFilter[y];
            }

            //Accumulate output I/Q samples from vector product
            mIAccumulator = 0.0f;
            mQAccumulator = 0.0f;

            for(int y = 0; y < mFilterVectorProduct.length; y += 2)
            {
                mIAccumulator += mFilterVectorProduct[y];
                mQAccumulator += mFilterVectorProduct[y + 1];
            }

            i[x] = mIAccumulator;
            q[x] = mQAccumulator;

            mTopBlockFlag = !mTopBlockFlag;
        }

        return new ComplexSamples(i, q, channelBuffer1.timestamp());
    }

    /**
     * Creates an interleaved I/Q filter where each coefficient from the filter argument is duplicated and the returned
     * filter is twice the length of the original filter.
     *
     * Note: the returned filter array is sized to:  2 * channel count * taps per channel, which may be slightly more
     * than twice the length of the original filter.  Any Added filter array elements will contain zero values.
     *
     * @param coefficients to create an interleaved filter
     * @param tapsPerChannel count
     * @return
     */
    private static float[] getInterleavedFilter(float[] coefficients, int tapsPerChannel)
    {
        int channelCount = 2;

        float[] filter = new float[channelCount * tapsPerChannel * 2];

        int coefficientPointer = 0;
        int filterPointer = 0;

        //Create a new filter that duplicates each tap to produce an interleaved I/Q filter
        while(coefficientPointer < coefficients.length)
        {
            filter[filterPointer++] = coefficients[coefficientPointer];
            filter[filterPointer++] = coefficients[coefficientPointer++];
        }

        return filter;
    }
}
