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

public class TimestampedComplexBuffer extends ComplexBuffer
{
    private long mTimestamp;

    /**
     * Constructs a timestamped complex buffer using the specified time in milliseconds.
     *
     * @param samples of data
     */
    public TimestampedComplexBuffer(float[] samples, long timestamp)
    {
        super(samples);

        mTimestamp = timestamp;
    }

    /**
     * Constructs a timestamped complex buffer using the current system time in milliseconds.
     *
     * @param samples of data
     */
    public TimestampedComplexBuffer(float[] samples)
    {
        this(samples, System.currentTimeMillis());
    }

    /**
     * Reference timestamp for this complex buffer.  Timestamp is relative to the first sample in the buffer.
     * @return timestamp as milliseconds since epoch
     */
    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Indicates that this complex buffer has a reference timestamp
     * @return true
     */
    @Override
    public boolean hasTimestamp()
    {
        return true;
    }
}
