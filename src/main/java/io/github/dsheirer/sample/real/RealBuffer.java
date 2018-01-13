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
package io.github.dsheirer.sample.real;

import io.github.dsheirer.sample.Buffer;
import io.github.dsheirer.source.SourceEvent;

import java.util.Arrays;

public class RealBuffer extends Buffer
{
    /**
     * Wrapper around float array containing real float samples and an optional source event that should be processed
     * prior to processing the sample array data.
     */
    public RealBuffer(float[] samples, SourceEvent sourceEvent)
    {
        super(samples, sourceEvent);
    }

    /**
     * Wrapper around float array containing real float samples
     */
    public RealBuffer(float[] samples)
    {
        super(samples);
    }

    /**
     * Creates a deep copy of the buffer and a shallow (reference) copy of the source event
     */
    public RealBuffer copyOf()
    {
        float[] copy = Arrays.copyOf(getSamples(), getSamples().length);

        return new RealBuffer(copy, getSourceEvent());
    }
}
