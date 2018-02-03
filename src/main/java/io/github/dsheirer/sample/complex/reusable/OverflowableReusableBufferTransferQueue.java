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

import io.github.dsheirer.sample.OverflowableTransferQueue;

public class OverflowableReusableBufferTransferQueue extends OverflowableTransferQueue<ReusableComplexBuffer>
{
    /**
     * Concurrent transfer queue that couples a higher-throughput linked transfer queue with an atomic integer for
     * monitoring queue size.  When the queue size exceeds maximum size (overflow), all inbound elements are ignored
     * until the queue size is reduced to or below the reset threshold.
     *
     * This implementation includes special handling for reusable complex buffers.
     *
     * @param maximumSize of the queue.  Overflow state will occur once queue size exceeds this value.
     * @param resetThreshold for resetting overflow state to normal, once queue size is at or below this value.
     */
    public OverflowableReusableBufferTransferQueue(int maximumSize, int resetThreshold)
    {
        super(maximumSize, resetThreshold);
    }

    /**
     * Overrides the overflow method to decrement the user count on any buffers that are being discarded when the queue
     * is in an overflow state.
     *
     * @param reusableComplexBuffer that will be discarded
     */
    @Override
    protected void overflow(ReusableComplexBuffer reusableComplexBuffer)
    {
        reusableComplexBuffer.decrementUserCount();
    }

    /**
     * Overrides the buffer clear method to decrement the user count on each buffer that is being cleared from the queue.
     */
    @Override
    public void clear()
    {
        synchronized(mQueue)
        {
            ReusableComplexBuffer buffer = mQueue.poll();

            while(buffer != null)
            {
                buffer.decrementUserCount();
                buffer = mQueue.poll();
            }

            mCounter.set(0);
            mOverflow.set(false);
        }
    }
}
