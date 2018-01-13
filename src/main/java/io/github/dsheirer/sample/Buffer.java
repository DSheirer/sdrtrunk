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
package io.github.dsheirer.sample;

import io.github.dsheirer.source.SourceEvent;

public class Buffer
{
    private float[] mSamples;
    private SourceEvent mSourceEvent;

    /**
     * Buffer containing float sample data and a source event that should be processed prior to processing the
     * sample data.
     *
     * @param samples data
     * @param sourceEvent indicating a change to the sample data that should be processed first
     */
    public Buffer(float[] samples, SourceEvent sourceEvent)
    {
        mSamples = samples;
        mSourceEvent = sourceEvent;
    }

    /**
     * Buffer containing float sample data
     * @param samples
     */
    public Buffer(float[] samples)
    {
        mSamples = samples;
    }

    /**
     * Float sample array
     */
    public float[] getSamples()
    {
        return mSamples;
    }

    /**
     * Optional source event that should be processed before processing the included samples.  Use the hasSourceEvent()
     * method to check for the existence of an event before accessing.
     *
     * @return optional source event or null
     */
    public SourceEvent getSourceEvent()
    {
        return mSourceEvent;
    }

    /**
     * Indicates if this buffer contains an optional source event that should be processed prior to processing the
     * included float samples array.
     */
    public boolean hasSourceEvent()
    {
        return mSourceEvent != null;
    }

    /**
     * Cleanup method to nullify all data and references
     */
    public void dispose()
    {
        mSamples = null;
        mSourceEvent = null;
    }
}