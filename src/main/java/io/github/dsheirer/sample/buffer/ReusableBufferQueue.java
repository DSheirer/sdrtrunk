/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
package io.github.dsheirer.sample.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReusableBufferQueue extends AbstractReusableBufferQueue<ReusableFloatBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(ReusableBufferQueue.class);

    public ReusableBufferQueue(String debugName)
    {
        super(debugName);
    }

    /**
     * Returns a reusable buffer from an internal recycling queue, or creates a new buffer if there are currently no
     * buffers available for reuse.  The user count is set to one and must be managed throughout the lifecycle
     * of the buffer.
     *
     * @param size of the samples that will be loaded into the buffer
     * @return a reusable buffer
     */
    public ReusableFloatBuffer getBuffer(int size)
    {
        ReusableFloatBuffer buffer = getRecycledBuffer();

        if(buffer == null)
        {
            buffer = new ReusableFloatBuffer(this, new float[size]);
            buffer.setDebugName("Owner:" + getDebugName());
            incrementBufferCount();
        }

        buffer.resize(size);
        buffer.incrementUserCount();

        return buffer;
    }

    /**
     * Creates or reuses a buffer and loads it with the samples and timestamp and increments user count to one.
     * @param samples to load
     * @param timestamp to set
     * @return loaded buffer with user count set to one
     */
    public ReusableFloatBuffer getBuffer(float[] samples, long timestamp)
    {
        ReusableFloatBuffer buffer = getRecycledBuffer();

        if(buffer == null)
        {
            buffer = new ReusableFloatBuffer(this, new float[samples.length]);
            buffer.setDebugName("Owner:" + getDebugName());
            incrementBufferCount();
        }

        buffer.reloadFrom(samples, timestamp);
        buffer.incrementUserCount();

        return buffer;
    }
}
