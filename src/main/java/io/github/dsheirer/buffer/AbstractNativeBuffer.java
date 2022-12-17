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

package io.github.dsheirer.buffer;

/**
 * Base native buffer class.
 */
public abstract class AbstractNativeBuffer implements INativeBuffer
{
    private long mTimestamp;
    private float mSamplesPerMillisecond;

    /**
     * Constructs an instance
     * @param timestamp for the start of this buffer
     * @param samplesPerMillisecond to calculate the time offset of sub-buffers extracted from this native buffer.
     */
    public AbstractNativeBuffer(long timestamp, float samplesPerMillisecond)
    {
        mTimestamp = timestamp;
        mSamplesPerMillisecond = samplesPerMillisecond;
    }

    /**
     * Timestamp for the start of this buffer
     * @return timestamp in milliseconds
     */
    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Quantity of samples representing one millisecond of sample data, used for calculating fragment timestamp offsets.
     * @return samples per millisecond count.
     */
    public float getSamplesPerMillisecond()
    {
        return mSamplesPerMillisecond;
    }

    /**
     * Calculates the timestamp for a samples buffer fragment based on where it is extracted from relative to the
     * native buffer starting timestamp.
     * @param samplesPointer for the start of the fragment.  Note: this value will be divided by 2 to account for I/Q sample pairs.
     * @return timestamp adjusted to the fragment or sub-buffer start sample.
     */
    protected long getFragmentTimestamp(int samplesPointer)
    {
        return getTimestamp() + (long)(samplesPointer / 2 / getSamplesPerMillisecond());
    }
}
