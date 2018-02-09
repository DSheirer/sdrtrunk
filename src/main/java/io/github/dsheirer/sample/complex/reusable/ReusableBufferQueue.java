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
package io.github.dsheirer.sample.complex.reusable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Queue;
import java.util.concurrent.LinkedTransferQueue;

public class ReusableBufferQueue implements IReusableBufferDisposedListener
{
    private final static Logger mLog = LoggerFactory.getLogger(ReusableBufferQueue.class);
    private Queue<ReusableComplexBuffer> mReusableBufferQueue = new LinkedTransferQueue<>();
    private int mBufferCount = 0;
    private String mDebugName;

    public ReusableBufferQueue(String debugName)
    {
        mDebugName = debugName;
    }

    public ReusableBufferQueue()
    {

    }

    /**
     * Implements buffer disposed listener interface.  Disposed (ie user count = 0) buffers will automatically callback
     * to this method to indicate when they are disposed.
     *
     * @param reusableComplexBuffer that has been disposed
     */
    @Override
    public void disposed(ReusableComplexBuffer reusableComplexBuffer)
    {
        mReusableBufferQueue.offer(reusableComplexBuffer);
    }

    /**
     * Returns a reusable buffer from an internal recycling queue, or creates a new buffer if there are currently no
     * buffers available for reuse.
     *
     * @param size of the samples that will be loaded into the buffer
     * @return a reusable buffer
     */
    public ReusableComplexBuffer getBuffer(int size)
    {
        ReusableComplexBuffer buffer = mReusableBufferQueue.poll();

        //Create a new buffer if we currently don't have any buffers to reuse
        if(buffer == null)
        {
            buffer = new ReusableComplexBuffer(this, new float[size]);
            mBufferCount++;
            mLog.debug("Buffer Created - Count:" + mBufferCount + (mDebugName != null ? " [" + mDebugName + "]" : ""));
        }

        return buffer;
    }
}
