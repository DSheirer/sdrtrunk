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

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractBuffer
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractBuffer.class);

    protected long mTimestamp;
    protected AtomicInteger mUserCount = new AtomicInteger();
    private String mDebugName;

    public AbstractBuffer(long timestamp)
    {
        mTimestamp = timestamp;
    }

    public AbstractBuffer()
    {
    }

    public void dispose()
    {
    }

    public void setDebugName(String debugName)
    {
        mDebugName = debugName;
    }

    public String name()
    {
        return (mDebugName != null ? mDebugName : "(null)") + " User Count:" + getUserCount();
    }

    public String toString()
    {
        return name();
    }

    /**
     * Reference timestamp for this complex buffer.  Timestamp is relative to the first sample in the buffer.
     * @return timestamp as milliseconds since epoch
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp)
    {
        mTimestamp = timestamp;
    }

    /**
     * Sets the user count to 0 and recycles this buffer
     */
    public void clearUserCount()
    {
    }

    /**
     * Number of users currently registered for this buffer
     */
    public int getUserCount()
    {
        return mUserCount.get();
    }
}
