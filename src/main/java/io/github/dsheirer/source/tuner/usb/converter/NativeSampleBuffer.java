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

package io.github.dsheirer.source.tuner.usb.converter;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;

import java.util.List;

/**
 * Base class for native samples byte buffer.  This class holds a copy of the byte array of samples
 * that was copied from a native byte buffer into the JVM heap space.
 *
 * Consumers of this sample buffer must make a copy of the contents in either interleaved or non-interleaved
 * sample array buffers.
 */
public abstract class NativeSampleBuffer
{
    private byte[] mSamples;

    public NativeSampleBuffer(byte[] samples)
    {
        mSamples = samples;
    }

    protected byte[] getSamples()
    {
        return mSamples;
    }

    public abstract List<ComplexSamples> convert();

    public abstract List<InterleavedComplexSamples> convertInterleaved();
}
