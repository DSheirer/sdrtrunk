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

public abstract class AbstractReusableBufferQueue<T extends ReusableBuffer> implements IReusableBufferDisposedListener<T>
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractReusableBufferQueue.class);

    private Queue<T> mReusableBufferQueue = new LinkedTransferQueue<>();
    private int mBufferCount = 0;
    private String mDebugName;
    private int mBufferUserCounter;

    public AbstractReusableBufferQueue(String debugName)
    {
        mDebugName = debugName;
    }

    public AbstractReusableBufferQueue()
    {
    }

    public void dispose()
    {
        mReusableBufferQueue.clear();
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

//        if(mDebugName != null && mDebugName.contentEquals("NativeBufferConverter"))
//        {
//            mLog.debug("Reclaimed: " + reusableBuffer.name());
//        }
    }

    /**
     * Returns a reusable buffer from an internal recycling queue, or creates a new buffer if there are currently no
     * buffers available for reuse.
     *
     * @param size of the samples that will be loaded into the buffer
     * @return a reusable buffer
     */
    public T getBuffer(int size)
    {
        T buffer = mReusableBufferQueue.poll();

        if(buffer == null)
        {
            buffer = createBuffer(size);
            mBufferCount++;
            buffer.setDebugName("Owner:" + mDebugName + " Number:" + mBufferUserCounter++ + " NEW BUFFER");

            mLog.debug("Buffer Created - Count:" + mBufferCount + (mDebugName != null ? " [" + mDebugName + "]" : ""));
        }
        else
        {
            String previous = buffer.name();
            buffer.setDebugName("Owner:" + mDebugName + " Number:" + mBufferUserCounter++);

//            if(mDebugName != null && mDebugName.contentEquals("NativeBufferConverter"))
//            {
//                mLog.debug("Buffer Reused: " + previous + " New Name:" + buffer.name());
//            }
        }

        return buffer;
    }

    /**
     * Create a new buffer of the specified size
     */
    protected abstract T createBuffer(int size);
}
