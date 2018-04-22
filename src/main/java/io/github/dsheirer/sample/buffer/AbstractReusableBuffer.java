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

public abstract class AbstractReusableBuffer
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractReusableBuffer.class);

    protected IReusableBufferDisposedListener mBufferDisposedListener;
    protected long mTimestamp;
    protected AtomicInteger mUserCount = new AtomicInteger();
    private String mDebugName;

    public AbstractReusableBuffer(IReusableBufferDisposedListener bufferDisposedListener, long timestamp)
    {
        mBufferDisposedListener = bufferDisposedListener;
        mTimestamp = timestamp;
    }

    public AbstractReusableBuffer(IReusableBufferDisposedListener bufferDisposedListener)
    {
        mBufferDisposedListener = bufferDisposedListener;
    }

    public void dispose()
    {
        mBufferDisposedListener = null;
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
        mUserCount.set(0);
        recycle();
    }

    /**
     * Decrements the user count for this buffer to indicate that the user/receiver of this buffer is finished using
     * the buffer.
     *
     * User count is established prior to sending this buffer to each listener and each listener invokes this method to
     * indicate that they have finished processing the buffer so that when the user count reaches zero, this buffer
     * can be reused.
     *
     * This method is thread-safe.
     */
    public void decrementUserCount()
    {
        if(mUserCount.decrementAndGet() <= 0)
        {
            recycle();
        }
    }

    /**
     * Sends this buffer back to the owning buffer queue for reuse
     */
    private void recycle()
    {
        prepareForRecycle();

        if(mBufferDisposedListener != null)
        {
            mBufferDisposedListener.disposed(this);
        }

        if(mUserCount.get() < 0)
        {
            throw new IllegalStateException("User count is below zero.  This indicates that this buffer's decrement" +
                " user count was invoked by more than the expected user count");
        }
    }

    /**
     * Invoked just prior to notifying the owner that this buffer is ready for prepareForRecycle.  This method
     * is intended for sub-class implementations to perform any prepareForRecycle cleanup actions.
     */
    protected void prepareForRecycle()
    {
        //No-op in this abstract class
    }

    /**
     * Increments the user count to indicate that this buffer will be sent to another user.
     *
     * Users/receivers of this buffer that wish to forward the buffer to additional users should increment the expected
     * user count prior to invoking the decrementUserCount() to indicate that the original receiver of the buffer is
     * finished to avoid the user count reaching zero and the buffer being flagged as ready for reuse.
     *
     * This method is thread-safe.
     */
    public void incrementUserCount()
    {
        mUserCount.incrementAndGet();
    }

    /**
     * Increments the user count by the specified count to indicate that this buffer will be sent to additional users.
     *
     * Users/receivers of this buffer that wish to forward the buffer to additional users should increment the expected
     * user count prior to invoking the decrementUserCount() to indicate that the original receiver of the buffer is
     * finished to avoid the user count reaching zero and the buffer being flagged as ready for reuse.
     *
     * This method is thread-safe.
     *
     * @param additionalUserCount to add to the current user count
     */
    public void incrementUserCount(int additionalUserCount)
    {
        if(additionalUserCount >= 0)
        {
            mUserCount.addAndGet(additionalUserCount);
        }
        else
        {
            mLog.debug("Request to add negative user count [" + additionalUserCount + "] ignored");
        }
    }

    /**
     * Number of users currently registered for this buffer
     */
    public int getUserCount()
    {
        return mUserCount.get();
    }
}
