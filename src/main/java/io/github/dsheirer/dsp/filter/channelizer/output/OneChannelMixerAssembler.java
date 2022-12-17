/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.dsp.filter.channelizer.output;

import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * Sample assembler to extract a single channel from the channel results array of a polyphase channel results array.
 */
public class OneChannelMixerAssembler extends MixerAssembler
{
    private SampleAssembler mSampleAssembler = new SampleAssembler(BUFFER_SIZE);

    /**
     * Constructs an instance
     * @param gain to apply to fully assembled buffers.
     */
    public OneChannelMixerAssembler(float gain)
    {
        super(gain);
    }

    /**
     * Receive a complex I & Q sample pair.
     * @param i inphase
     * @param q quadrature
     */
    public void receive(float i, float q)
    {
        mSampleAssembler.receive(i, q);
    }

    /**
     * Indicates if there is a fully assembled buffer ready.
     */
    @Override
    public boolean hasBuffer()
    {
        return mSampleAssembler.hasBuffer();
    }

    /**
     * Access the assembled buffer.  Frequency offset and gain are applied to the returned buffer.
     * @return assembled buffer.
     */
    @Override
    public ComplexSamples getBuffer(long timestamp)
    {
        ComplexSamples buffer = mSampleAssembler.getBufferAndReset(timestamp);

        if(getMixer().hasFrequency())
        {
            buffer = getMixer().mix(buffer);
        }

        buffer = getGain().apply(buffer);

        return buffer;
    }
}
