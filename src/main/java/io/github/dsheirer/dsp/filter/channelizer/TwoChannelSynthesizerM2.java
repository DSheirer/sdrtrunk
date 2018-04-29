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

import io.github.dsheirer.buffer.RealCircularBuffer;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter;
import io.github.dsheirer.dsp.mixer.IOscillator;
import io.github.dsheirer.dsp.mixer.LowPhaseNoiseOscillator;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class TwoChannelSynthesizerM2
{
    private final static Logger mLog = LoggerFactory.getLogger(TwoChannelSynthesizerM2.class);

    private ComplexFIRFilter mFilter1;
    private ComplexFIRFilter mFilter2;

    private FloatFFT_1D mFFT = new FloatFFT_1D(2);
    private boolean mFlag = false;

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
        int length = (filter.length / 2) + (filter.length % 2);

        float[] filter1 = new float[length];
        float[] filter2 = new float[length];

        for(int x = 0; x < filter.length; x += 2)
        {
            //Load the filter in reverse to facilitate convolution
            int index = length - (x / 2) - 1;

            filter1[index] = filter[x];

            if(x + 1 < filter.length)
            {
                filter2[index] = filter[x + 1];
            }
        }

        mFilter1 = new ComplexFIRFilter(filter1, 1.0f);
        mFilter2 = new ComplexFIRFilter(filter2, 1.0f);
    }

    /**
     * Synthesizes a new channel from the channel 1 and channel 2 sample arrays that are arranged as
     * i0, q0, i1, q1 ... iN-1, qN-1
     *
     * @param channel1 input channel
     * @param channel2 input channel
     * @return synthesized channel results of the same length as both channel 1/2 input arrays.
     */
    public float[] process(float[] channel1, float[] channel2)
    {
        if(channel1.length != channel2.length)
        {
            throw new IllegalArgumentException("Channel 1 and 2 array length must be equal");
        }

        float[] output = new float[channel1.length];

        float[] buffer = new float[4];

        for(int x = 0; x < channel1.length; x += 2)
        {
            buffer[0] = channel1[x];     //i
            buffer[1] = channel1[x + 1]; //q
            buffer[2] = channel2[x];     //i
            buffer[3] = channel2[x + 1]; //q

            mFFT.complexInverse(buffer, true);

            if(mFlag)
            {
                output[x] = mFilter1.filterInphase(buffer[0]) + mFilter2.filterInphase(buffer[2]);
                output[x + 1] = mFilter1.filterQuadrature(buffer[1]) + mFilter2.filterQuadrature(buffer[3]);
            }
            else
            {
                output[x] = mFilter1.filterInphase(buffer[2]) + mFilter2.filterInphase(buffer[0]);
                output[x + 1] = mFilter1.filterQuadrature(buffer[3]) + mFilter2.filterQuadrature(buffer[1]);
            }

            mFlag = !mFlag;
        }

        return output;
    }

    private static float filter(float[] taps, RealCircularBuffer dataBlock)
    {
        float accumulator = 0;

        for(int x = 0; x < taps.length; x++)
        {
            accumulator += (taps[x] * dataBlock.get(x));
        }

        return accumulator;
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting");

        int sampleCount = 10;

        try
        {
            float[] taps = FilterFactory.getSincM2Synthesizer(12500.0, 2, 19, Window.WindowType.BLACKMAN_HARRIS_7, true);

            TwoChannelSynthesizerM2 synthesizer = new TwoChannelSynthesizerM2(taps);

            IOscillator oscillator = new LowPhaseNoiseOscillator(3000.0, 25000.0);

            float[] channel1 = oscillator.generateComplex(sampleCount);
            float[] channel2 = new float[sampleCount * 2];

            float[] synthesized = synthesizer.process(channel1, channel2);

            mLog.debug("1:" + Arrays.toString(channel1));
            mLog.debug("X:" + Arrays.toString(synthesized));
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Filter design error", fde);
        }

        mLog.debug("Finished!!");
    }
}
