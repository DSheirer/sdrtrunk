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

/**
 * Implements a two-channel synthesizer.  This class is intended to be used with an M2 polyphase channelizer in order
 * to recover a signal that overlaps the boundary between two adjacent channels.
 *
 * Each M2 channel is sampled at twice the channel bandwidth, so the composite of two adjacent channels can be
 * represented at the same sample rate as a single channel: channel 1 remains at baseband and channel 2 is translated
 * to FS/2, placing the boundary between the two channels at +FS/4.  The caller is responsible for translating channel 2
 * by FS/2 (multiplying each sample by (-1)^n, where n is the absolute channelizer output index) since only the caller
 * knows the absolute sample index parity.  With a perfect reconstruction (-6 dB band edge) channelizer filter, the sum
 * of the two channels recovers the original spectrum across the channel boundary.
 *
 * The composite is then filtered with the supplied low-pass prototype, modulated up by +FS/4 so that it is centered
 * on the boundary between the two channels, to suppress the out-of-band channelizer transition regions that alias
 * into the outer portions of the composite spectrum.  The output remains centered at +FS/4 and the caller is
 * responsible for translating it to baseband.
 */
public class TwoChannelSynthesizerM2
{
    private float[] mFilterI;
    private float[] mFilterQ;
    private float[] mBufferI;
    private float[] mBufferQ;
    private int mBufferPointer;

    /**
     * Synthesizer for combining two M2 oversampled channels into a composite channel.  Output sample rate is the same
     * as the input sample rate of one of the channels.
     *
     * @param filter low-pass prototype filter designed at the channel sample rate.
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
        sb.append("\n\t\t Filter I: " + Arrays.toString(mFilterI));
        sb.append("\n\t\t Filter Q: " + Arrays.toString(mFilterQ));
        return sb.toString();
    }

    /**
     * Initializes the synthesizer filter and data buffers for operation.
     *
     * The prototype low-pass filter is modulated by +FS/4 (multiplied by j^n) to create a complex band-pass filter
     * centered on the boundary between the two channels.  The filter is stored in time-reversed order so that it
     * can be applied directly against the oldest-to-newest ordered sample history.
     *
     * @param filter low-pass prototype filter.
     */
    private void init(float[] filter)
    {
        int length = filter.length;
        mFilterI = new float[length];
        mFilterQ = new float[length];

        for(int x = 0; x < length; x++)
        {
            int reversed = length - 1 - x;

            switch(x % 4)
            {
                case 0:
                    mFilterI[reversed] = filter[x];
                    break;
                case 1:
                    mFilterQ[reversed] = filter[x];
                    break;
                case 2:
                    mFilterI[reversed] = -filter[x];
                    break;
                case 3:
                    mFilterQ[reversed] = -filter[x];
                    break;
            }
        }

        //Double-length buffers so that the newest filter-length span of samples is always contiguous
        mBufferI = new float[length * 2];
        mBufferQ = new float[length * 2];
        mBufferPointer = 0;
    }

    /**
     * Synthesizes a new channel from the channel 1 and channel 2 sample buffers.  Channel 2 must already be
     * translated to FS/2.
     *
     * @param channelBuffer1 input channel at baseband
     * @param channelBuffer2 input channel translated to FS/2
     * @return synthesized channel results, centered at +FS/4, of the same length as the input buffers.
     */
    public ComplexSamples process(ComplexSamples channelBuffer1, ComplexSamples channelBuffer2)
    {
        if(channelBuffer1.i().length != channelBuffer2.i().length)
        {
            throw new IllegalArgumentException("Channel 1 and 2 array length must be equal");
        }

        float[] iCh1 = channelBuffer1.i();
        float[] qCh1 = channelBuffer1.q();
        float[] iCh2 = channelBuffer2.i();
        float[] qCh2 = channelBuffer2.q();

        float[] i = new float[iCh1.length];
        float[] q = new float[qCh1.length];

        int length = mFilterI.length;
        float iAccumulator, qAccumulator;

        for(int x = 0; x < iCh1.length; x++)
        {
            //Combine the channels and load the composite sample into both halves of the double-length buffer
            float iSample = iCh1[x] + iCh2[x];
            float qSample = qCh1[x] + qCh2[x];
            mBufferI[mBufferPointer] = iSample;
            mBufferQ[mBufferPointer] = qSample;
            mBufferI[mBufferPointer + length] = iSample;
            mBufferQ[mBufferPointer + length] = qSample;
            mBufferPointer++;

            if(mBufferPointer >= length)
            {
                mBufferPointer = 0;
            }

            //Oldest sample is at mBufferPointer and newest sample is at mBufferPointer + length - 1
            iAccumulator = 0.0f;
            qAccumulator = 0.0f;

            for(int y = 0; y < length; y++)
            {
                float iData = mBufferI[mBufferPointer + y];
                float qData = mBufferQ[mBufferPointer + y];
                iAccumulator += (iData * mFilterI[y]) - (qData * mFilterQ[y]);
                qAccumulator += (qData * mFilterI[y]) + (iData * mFilterQ[y]);
            }

            i[x] = iAccumulator;
            q[x] = qAccumulator;
        }

        return new ComplexSamples(i, q, channelBuffer1.timestamp());
    }
}
