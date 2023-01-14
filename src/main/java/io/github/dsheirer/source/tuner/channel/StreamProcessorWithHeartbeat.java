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

package io.github.dsheirer.source.tuner.channel;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import io.github.dsheirer.util.ThreadPool;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe implementation that coordinates the flow of data/buffer stream processing with a parallel, periodic
 * heartbeat using an internal locking mechanism to coordinate the two activities.  This class is intended to be used
 * by TunerChannelSource implementations to coordinate the flow of sample buffers produced on a dispatch thread with a
 * scheduled timer driven heartbeat mechanism, to ensure the downstream listener receives either samples or a
 * heartbeat, but not both at the same time.
 *
 * Samples are processed as they arrive and are immediately followed by a heartbeat.  A separate scheduled timer runs
 * on a periodic schedule to optionally fire a heartbeat, independent of the sample stream rate.  The scheduled
 * heartbeat is optional and will skip a heartbeat interval if this processor is currently dispatching a sample buffer.
 * Sample stream processing will block until a currently running scheduled heartbeat timer event completes, when these
 * two events occur at the same time.
 */
public class StreamProcessorWithHeartbeat<T> implements Listener<T>
{
    private ReentrantLock mLock = new ReentrantLock();
    private HeartbeatManager mHeartbeatManager;
    private long mHeartbeatInterval;
    private Listener<T> mListener;
    private ScheduledFuture<?> mHeartbeatTimerFuture;

    /**
     * Constructs an instance
     * @param heartbeatManager that will be invoked by an internal scheduled timer
     * @param heartbeatInterval in milliseconds
     */
    public StreamProcessorWithHeartbeat(HeartbeatManager heartbeatManager, long heartbeatInterval)
    {
        mHeartbeatManager = heartbeatManager;
        mHeartbeatInterval = heartbeatInterval;
    }

    /**
     * Starts the heartbeat timer
     */
    public void start()
    {
        if(mHeartbeatTimerFuture == null)
        {
            mHeartbeatTimerFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(() -> fireOptionalHeartbeat(),
                    mHeartbeatInterval, mHeartbeatInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops the heartbeat timer
     */
    public void stop()
    {
        if(mHeartbeatTimerFuture != null)
        {
            mHeartbeatTimerFuture.cancel(true);
            mHeartbeatTimerFuture = null;
        }
    }

    /**
     * Optionally fires a heartbeat if we're not currently processing a native buffer
     */
    private void fireOptionalHeartbeat()
    {
        //Acquire lock only if it's not already locked, otherwise ignore heartbeat for this timer interval.
        if(mLock.tryLock())
        {
            try
            {
                mHeartbeatManager.broadcast();
            }
            finally
            {
                mLock.unlock();
            }
        }
    }

    @Override
    public void receive(T data)
    {
        mLock.lock();

        try
        {
            mHeartbeatManager.broadcast();

            Listener<T> listener = mListener;

            if(listener != null)
            {
                listener.receive(data);
            }
        }
        finally
        {
            mLock.unlock();
        }
    }

    /**
     * Registers the listener to receive data/buffers for the defined generic type that are distributed to this processor.
     * @param listener to register or null to deregister an existing listener
     */
    public void setListener(Listener<T> listener)
    {
        mListener = listener;
    }
}
