/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.util;

import io.github.dsheirer.sample.Listener;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Threaded processor for receiving elements from a separate producer thread and forwarding those buffers to a
 * registered listener on this consumer/dispatcher thread.
 */
public class Dispatcher<E> implements Listener<E>
{
    private final static Logger mLog = LoggerFactory.getLogger(Dispatcher.class);
    private static final long OVERFLOW_LOG_EVENT_WAIT_PERIOD = TimeUnit.SECONDS.toMillis(10);
    private LinkedBlockingQueue<E> mQueue;
    private Listener<E> mListener;
    private AtomicBoolean mRunning = new AtomicBoolean();
    private String mThreadName;
    private Thread mThread;
    private E mPoisonPill;
    private long mLastOverflowLogEvent;

    /**
     * Constructs an instance
     * @param maxSize of the internal queue
     * @param threadName to name the dispatcher thread
     * @param poisonPill of type E, used to kill the thread
     */
    public Dispatcher(int maxSize, String threadName, E poisonPill)
    {
        if(poisonPill == null)
        {
            throw new IllegalArgumentException("Poison pill must be non-null");
        }
        mQueue = new LinkedBlockingQueue<>(maxSize);
        mThreadName = threadName;
        mPoisonPill = poisonPill;
    }

    /**
     * Listener to receive the queued buffers each time this processor runs.
     */
    protected Listener<E> getListener()
    {
        return mListener;
    }

    /**
     * Sets or changes the listener to receive buffers from this processor.
     * @param listener to receive buffers
     */
    public void setListener(Listener<E> listener)
    {
        mListener = listener;
    }

    /**
     * Primary input method for adding buffers to this processor.  Note: incoming buffers will be ignored if this
     * processor is in a stopped state.  You must invoke start() to allow incoming buffers and initiate buffer
     * processing.
     *
     * @param e to enqueue for distribution to a registered listener
     */
    public void receive(E e)
    {
        if(mRunning.get())
        {
            if(!mQueue.offer(e))
            {
                if(System.currentTimeMillis() > (mLastOverflowLogEvent + OVERFLOW_LOG_EVENT_WAIT_PERIOD))
                {
                    mLastOverflowLogEvent = System.currentTimeMillis();
                    mLog.warn("Dispatcher - temporary buffer overflow for thread [" + mThreadName + "] - throwing away samples - " +
                            " processor flag:" + (mRunning.get() ? "running" : "stopped") +
                            " thread:" + (mThread != null ? (mThread.isAlive() ? mThread.getState() : "dead") : "null" ));
                }
            }
        }
    }

    /**
     * Starts this buffer processor and allows queuing of incoming buffers.
     */
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            mThread = new Thread(new Processor());
            mThread.setName(mThreadName);
            mThread.setPriority(Thread.MAX_PRIORITY);
            mThread.start();
        }
    }

    /**
     * Stops this buffer processor and waits up to two seconds for the processing thread to terminate.
     */
    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            mQueue.offer(mPoisonPill);

            try
            {
                mThread.interrupt();
                mThread.join();
                mThread = null;
            }
            catch(Exception e)
            {
                mLog.error("Timeout while waiting to join terminating buffer processor thread");
            }
        }
    }

    /**
     * Finishes processing any queued buffers and then stops the processing thread.
     */
    public void flushAndStop()
    {
        mQueue.offer(mPoisonPill);
    }

    /**
     * Indicates if this processor is currently running
     */
    public boolean isRunning()
    {
        return mRunning.get();
    }

    /**
     * Processor to service the buffer queue and distribute the buffers to the registered listener
     */
    class Processor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                mQueue.clear();

                E element;

                while(mRunning.get())
                {
                    try
                    {
                        element = mQueue.take();

                        if(mPoisonPill == element)
                        {
                            mRunning.set(false);
                        }
                        else if(element != null)
                        {
                            if(mListener == null)
                            {
                                throw new IllegalStateException("Listener for [" + mThreadName + "] is null");
                            }
                            mListener.receive(element);
                        }
                    }
                    catch(InterruptedException e)
                    {
                        //Normal shutdown is by interrupt
                        mRunning.set(false);
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error while processing element", e);
                    }
                }

                //Shutting down - clear the queue
                mQueue.clear();
            }
            catch(Throwable t)
            {
                mLog.error("Unexpected error thrown from the Dispatcher thread", t);
            }
        }
    }
}
