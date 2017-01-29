/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package sample;

import java.util.Collection;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class OverflowableTransferQueue<E>
{
    public enum State {NORMAL, OVERFLOW};
    private Listener<State> mStateListener;

    private LinkedTransferQueue<E> mQueue = new LinkedTransferQueue<E>();
    private AtomicInteger mCounter = new AtomicInteger();
    private int mMaximumSize;
    private int mResetThreshold;
    private boolean mOverflow = false;

    /**
     * Concurrent transfer queue that couples a higher-throughput linked transfer queue with an atomic integer for
     * monitoring queue size.  When the queue size exceeds maximum size (overflow), all inbound elements are ignored
     * until the queue size is reduced to or below the reset threshold.
     *
     * @param maximumSize of the queue.  Overflow state will occur once queue size exceeds this value.
     * @param resetThreshold for resetting overflow state to normal, once queue size is at or below this value.
     */
    public OverflowableTransferQueue(int maximumSize, int resetThreshold)
    {
        mMaximumSize = maximumSize;
        mResetThreshold = resetThreshold;
    }

    /**
     * Adds the element to the queue if able to do so without exceeding maximum queue size.  Otherwise, ignores
     * the element.
     */
    public void offer(E e)
    {
        if(!mOverflow)
        {
            mQueue.offer(e);

            int size = mCounter.incrementAndGet();

            if(size > mMaximumSize)
            {
                setOverflow(true);
            }
        }
    }

    /**
     * Retrieves elements from the queue into the collection up to the maximum number of elements specified
     */
    public int drainTo(Collection<? super E> collection, int maxElements)
    {
        int drainCount = mQueue.drainTo(collection, maxElements);

        if(drainCount > 0)
        {
            int size = mCounter.addAndGet(-drainCount);

            if(mOverflow && size <= mResetThreshold)
            {
                setOverflow(false);
            }
        }

        return drainCount;
    }

    /**
     * Sets a listener to receive state change events
     */
    public void setStateListener(Listener<State> listener)
    {
        mStateListener = listener;
    }


    /**
     * Current state of the queue, normal or overflow
     */
    public State getState()
    {
        return mOverflow ? State.OVERFLOW : State.NORMAL;
    }

    /**
     * Toggles the overflow state
     */
    private void setOverflow(boolean overflow)
    {
        mOverflow = overflow;

        if(mStateListener != null)
        {
            mStateListener.receive(getState());
        }
    }

    /**
     * Clears all elements from the queue and resets the internal counter to 0
     */
    public void clear()
    {
        synchronized(mQueue)
        {
            mQueue.clear();
            mCounter.set(0);
            mOverflow = false;
        }
    }
}
