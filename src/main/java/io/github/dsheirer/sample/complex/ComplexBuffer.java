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
package io.github.dsheirer.sample.complex;

import io.github.dsheirer.sample.Buffer;
import io.github.dsheirer.source.SourceEvent;

public class ComplexBuffer extends Buffer
{
    //Default timestamp - we use a static variable here so that there is only ever 1 instance in memory
    private static final long DEFAULT_TIMESTAMP = 0;

    /**
     * Wrapper around float array containing interleaved I/Q samples and an optional source event that should be
     * processed prior to processing the sample data.
     */
    public ComplexBuffer(float[] samples, SourceEvent sourceEvent)
    {
        super(samples, sourceEvent);
    }
    /**
     * Wrapper around float array containing interleaved I/Q samples
     */
    public ComplexBuffer(float[] samples)
    {
        super(samples);
    }


    /**
     * Reference timestamp for the first sample in this buffer.  This functionality is intended to be implemented by a
     * subclass implementation.  The default value is 0.
     *
     * @return reference timestamp or a value of 0 if hasTimestamp() indicates false.
     */
    public long getTimestamp()
    {
        return DEFAULT_TIMESTAMP;
    }

    /**
     * Indicates if this buffer has a timestamp that references the first sample in the buffer.
     */
    public boolean hasTimestamp()
    {
        return false;
    }
}
