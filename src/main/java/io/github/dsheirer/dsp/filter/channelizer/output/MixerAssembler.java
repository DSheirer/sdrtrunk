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

import io.github.dsheirer.dsp.gain.complex.ComplexGain;
import io.github.dsheirer.dsp.gain.complex.ComplexGainFactory;
import io.github.dsheirer.dsp.mixer.ComplexMixer;
import io.github.dsheirer.dsp.mixer.ComplexMixerFactory;
import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * Sample assembler for a channel output processor that incorporates a mixer to perform final frequency
 * translation on an extracted channel.
 */
public abstract class MixerAssembler
{
    protected static final int BUFFER_SIZE = 2048;

    private ComplexMixer mMixer = ComplexMixerFactory.getMixer(0, 0);
    private ComplexGain mGain;

    /**
     * Constructs an instance.
     * @param gain to apply to fully assembled buffers.
     */
    protected MixerAssembler(float gain)
    {
        mGain = ComplexGainFactory.getComplexGain(gain);
    }

    /**
     * Complex samples mixer for this assembler
     */
    protected ComplexMixer getMixer()
    {
        return mMixer;
    }

    protected ComplexGain getGain()
    {
        return mGain;
    }

    /**
     * Indicates if this assembler has a buffer ready to dispatch to consumers.
     */
    public abstract boolean hasBuffer();

    /**
     * Access the fully assembled buffer when hasBuffer() indicates that a buffer is ready.
     * @param timestamp to use for the assembled complex samples buffer
     * @return true if a full buffer is assembled and ready.
     */
    public abstract ComplexSamples getBuffer(long timestamp);
}
