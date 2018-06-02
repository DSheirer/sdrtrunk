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
package io.github.dsheirer.sample.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReusableBufferQueue extends AbstractReusableBufferQueue<ReusableBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(ReusableBufferQueue.class);

    public ReusableBufferQueue(String debugName)
    {
        super(debugName);
    }

    /**
     * Returns a reusable buffer from an internal recycling queue, or creates a new buffer if there are currently no
     * buffers available for reuse.
     *
     * @param size of the samples that will be loaded into the buffer
     * @return a reusable buffer
     */
    public ReusableBuffer getBuffer(int size)
    {
        ReusableBuffer buffer = getRecycledBuffer();

        if(buffer == null)
        {
            buffer = new ReusableBuffer(this, new float[size]);
            incrementBufferCount();

            buffer.setDebugName("Owner:" + getDebugName());
            mLog.debug("Buffer Created - Count:" + getBufferCount() + (getDebugName() != null ? " [" + getDebugName() + "]" : ""));
        }

        buffer.resize(size);

        return buffer;
    }
}
