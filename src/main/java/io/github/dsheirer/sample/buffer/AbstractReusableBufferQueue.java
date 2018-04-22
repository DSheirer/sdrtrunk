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

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

public abstract class AbstractReusableBufferQueue<T extends AbstractReusableBuffer>
        implements IReusableBufferDisposedListener<T>
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractReusableBufferQueue.class);

    private Queue<T> mReusableBufferQueue = new LinkedTransferQueue<>();
    private int mBufferCount = 0;
    private String mDebugName;

    /**
     * Base queue for managing reusable buffers.
     *
     * @param debugName to associate a debug string with this buffer instance
     */
    public AbstractReusableBufferQueue(String debugName)
    {
        mDebugName = debugName;
    }

    /**
     * Base queue for managing reusable buffers.
     */
    public AbstractReusableBufferQueue()
    {
    }

    /**
     * Disposes of any reclaimed buffers to prepare this queue for disposal.
     */
    public void dispose()
    {
        T buffer = mReusableBufferQueue.poll();

        while(buffer != null)
        {
            buffer.dispose();
            buffer = mReusableBufferQueue.poll();
        }

        mBufferCount = 0;
    }

    /**
     * Implements buffer disposed listener interface.  Disposed (ie user count = 0) buffers will automatically callback
     * to this method to indicate when they are disposed.
     *
     * @param reusableBuffer that has been disposed
     */
    @Override
    public void disposed(T reusableBuffer)
    {
        mReusableBufferQueue.offer(reusableBuffer);
    }

    /**
     * Get a recycled buffer from the queue
     */
    protected T getRecycledBuffer()
    {
        return mReusableBufferQueue.poll();
    }

    /**
     * Increments the count of buffers managed by this queue.  Note: this method is NOT thread safe
     */
    protected void incrementBufferCount()
    {
        mBufferCount++;
    }

    /**
     * Current count of buffers created by this queue
     */
    protected int getBufferCount()
    {
        return mBufferCount;
    }

    /**
     * Debug name for this queue instance
     */
    protected String getDebugName()
    {
        return mDebugName;
    }
}
