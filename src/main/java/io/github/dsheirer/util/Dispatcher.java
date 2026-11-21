/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.ToLongFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Threaded scheduled processor for receiving elements from a separate producer thread and forwarding those buffers to a
 * registered listener on this consumer/dispatcher thread.  Internally uses a single-thread thread pool to effect a
 * timer-based interval for processing to avoid excessive context switching inherent in a blocking queue.  Sizes the
 * thread pool to a single thread to ensure Garbage Collector can efficiently clean objects created on the thread.
 */
public class Dispatcher<E> implements Listener<E>
{
    private final static Logger mLog = LoggerFactory.getLogger(Dispatcher.class);
    private static final long OVERFLOW_LOG_INTERVAL_MILLISECONDS = 10000;
    private final BlockingQueue<E> mQueue;
    private Listener<E> mListener;
    private final AtomicBoolean mRunning = new AtomicBoolean();
    private String mThreadName;
    private ScheduledExecutorService mExecutorService;
    private ScheduledFuture<?> mScheduledFuture;
    private final long mInterval;
    private HeartbeatManager mHeartbeatManager;
    private long mDroppedCount = 0;
    private long mLastOverflowLogTimestamp = 0;
    private final ToLongFunction<E> mWeigher;
    private volatile long mMaximumQueuedWeight = Long.MAX_VALUE;
    private final AtomicLong mQueuedWeight = new AtomicLong();

    /**
     * Constructs an instance of a Dispatcher with integrated heartbeat support.
     * @param threadName to name the dispatcher thread
     * @param interval for processing each batch in milliseconds.
     * @param heartbeatManager to receive a heartbeat command at each processing interval.
     */
    public Dispatcher(String threadName, long interval, HeartbeatManager heartbeatManager)
    {
        this(threadName, interval);
        mHeartbeatManager = heartbeatManager;
    }

    /**
     * Constructs an instance of a bounded Dispatcher with integrated heartbeat support.
     * @param threadName to name the dispatcher thread
     * @param interval for processing each batch in milliseconds.
     * @param heartbeatManager to receive a heartbeat command at each processing interval.
     * @param maximumQueueSize for queued elements.  Elements received while the queue is full are dropped.
     */
    public Dispatcher(String threadName, long interval, HeartbeatManager heartbeatManager, int maximumQueueSize)
    {
        this(threadName, interval, maximumQueueSize);
        mHeartbeatManager = heartbeatManager;
    }

    /**
     * Constructs an instance with an unbounded queue.
     * @param threadName to name the dispatcher thread
     * @param interval for processing each batch in milliseconds.
     */
    public Dispatcher(String threadName, long interval)
    {
        mThreadName = threadName;
        mInterval = interval;
        mQueue = new LinkedTransferQueue<>();
        mWeigher = null;
    }

    /**
     * Constructs a bounded instance.
     * @param threadName to name the dispatcher thread
     * @param interval for processing each batch in milliseconds.
     * @param maximumQueueSize for queued elements.  Elements received while the queue is full are dropped.
     */
    public Dispatcher(String threadName, long interval, int maximumQueueSize)
    {
        mThreadName = threadName;
        mInterval = interval;
        mQueue = new LinkedBlockingQueue<>(maximumQueueSize);
        mWeigher = null;
    }

    /**
     * Constructs an instance that is bounded by the total weight of the queued elements instead of the element count,
     * for when element sizes vary widely, for example sample buffers from different tuners.  An element is dropped
     * when adding it would exceed the maximum weight, except when nothing is queued, so that an element heavier than
     * the maximum is still delivered.  Elements count toward the weight until the listener has processed them.
     *
     * @param threadName to name the dispatcher thread
     * @param interval for processing each batch in milliseconds.
     * @param weigher to calculate the weight of each element, for example a sample count.
     * @param maximumQueuedWeight for the queued elements.
     */
    public Dispatcher(String threadName, long interval, ToLongFunction<E> weigher, long maximumQueuedWeight)
    {
        if(weigher == null)
        {
            throw new IllegalArgumentException("Weigher cannot be null");
        }

        mThreadName = threadName;
        mInterval = interval;
        mQueue = new LinkedTransferQueue<>();
        mWeigher = weigher;
        setMaximumQueuedWeight(maximumQueuedWeight);
    }

    /**
     * Sets the maximum total weight of the queued elements, for a dispatcher that was constructed with a weigher.
     * @param maximumQueuedWeight for the queued elements.
     */
    public void setMaximumQueuedWeight(long maximumQueuedWeight)
    {
        if(maximumQueuedWeight <= 0)
        {
            throw new IllegalArgumentException("Maximum queued weight must be positive: " + maximumQueuedWeight);
        }

        mMaximumQueuedWeight = maximumQueuedWeight;
    }

    /**
     * Sets the thread name.  If this dispatcher is already started, this has no effect.
     * @param threadName to use for this dispatcher.
     */
    public void setThreadName(String threadName)
    {
        mThreadName = threadName;
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
     * processing.  If this processor has a bounded queue and the queue is full, the incoming buffer is dropped.
     *
     * @param e to enqueue for distribution to a registered listener
     */
    public void receive(E e)
    {
        offer(e);
    }

    /**
     * Adds the element to this processor's queue, for when the caller has to take action on an element that isn't
     * accepted.
     *
     * @param e to enqueue for distribution to a registered listener
     * @return true if the element was queued, or false if this processor is stopped or a bounded queue is full.
     */
    public boolean offer(E e)
    {
        if(!mRunning.get())
        {
            return false;
        }

        boolean queued;

        if(mWeigher != null)
        {
            long weight = mWeigher.applyAsLong(e);
            long queuedWeight = mQueuedWeight.addAndGet(weight);

            if(queuedWeight > mMaximumQueuedWeight && queuedWeight > weight)
            {
                mQueuedWeight.addAndGet(-weight);
                queued = false;
            }
            else
            {
                queued = mQueue.offer(e);
            }
        }
        else
        {
            queued = mQueue.offer(e);
        }

        if(!queued)
        {
            mDroppedCount++;
            long now = System.currentTimeMillis();

            if(now - mLastOverflowLogTimestamp >= OVERFLOW_LOG_INTERVAL_MILLISECONDS)
            {
                mLog.warn("Dispatcher [" + mThreadName + "] queue is full, consumer is not keeping up - dropped [" +
                    mDroppedCount + "] elements");
                mLastOverflowLogTimestamp = now;
                mDroppedCount = 0;
            }
        }

        return queued;
    }

    /**
     * Starts this buffer processor and allows queuing of incoming buffers.
     */
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            if(mScheduledFuture != null)
            {
                //Note: this has to be false because downstream implementations may have acquired locks and they must
                //be able to release those locks or we'll get a deadlock situation.
                mScheduledFuture.cancel(false);
            }

            if(mExecutorService != null)
            {
                mExecutorService.shutdown();
                mExecutorService = null;
            }

            mQueue.clear();
            mQueuedWeight.set(0);
            mExecutorService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory(mThreadName));

            Runnable r = (mHeartbeatManager != null ? new ProcessorWithHeartbeat() : new Processor());
            mScheduledFuture = mExecutorService.scheduleAtFixedRate(r, 0, mInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops this buffer processor and waits up to two seconds for the processing thread to terminate.
     */
    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mScheduledFuture != null)
            {
                //Note: this has to be false because downstream implementations may have acquired locks and they must
                //be able to release those locks or we'll get a deadlock situation.
                mScheduledFuture.cancel(false);
                mScheduledFuture = null;
                mQueue.clear();
                mQueuedWeight.set(0);
            }

            if(mExecutorService != null)
            {
                mExecutorService.shutdown();
                mExecutorService = null;
            }
        }
    }

    /**
     * Stops this buffer processor and flushes the queue to the listener
     */
    public void flushAndStop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mScheduledFuture != null)
            {
                //Note: this has to be false because downstream implementations may have acquired locks and they must
                //be able to release those locks or we'll get a deadlock situation.
                mScheduledFuture.cancel(false);
                mScheduledFuture = null;
            }

            if(mExecutorService != null)
            {
                mExecutorService.shutdown();
                mExecutorService = null;
            }

            List<E> elements = new ArrayList<>();

            mQueue.drainTo(elements);

            for(E element: elements)
            {
                if(mListener != null)
                {
                    try
                    {
                        mListener.receive(element);
                    }
                    catch(Throwable t)
                    {
                        mLog.error("Error while flusing and dispatching element [" + element.getClass() + "] to listener [" +
                                mListener.getClass() + "]", t);
                    }
                }

                release(element);
            }
        }
    }

    /**
     * Indicates if this processor is currently running
     */
    public boolean isRunning()
    {
        return mRunning.get();
    }

    /**
     * Processes elements from the queue.  Note: this should only be invoked on the Processor thread.
     */
    private void process()
    {
        List<E> elements = new ArrayList<>();

        mQueue.drainTo(elements);

        for(E element: elements)
        {
            if(mRunning.get() && mListener != null)
            {
                try
                {
                    mListener.receive(element);
                }
                catch(Throwable t)
                {
                    mLog.error("Error while dispatching element [" + element.getClass() + "] to listener [" +
                            mListener.getClass() + "]", t);
                }
            }

            release(element);
        }
    }

    /**
     * Removes the weight of a dequeued element from the queued weight, once the element is processed.
     */
    private void release(E element)
    {
        if(mWeigher != null)
        {
            mQueuedWeight.addAndGet(-mWeigher.applyAsLong(element));
        }
    }

    /**
     * Processor to service the buffer queue and distribute the buffers to the registered listener
     */
    class Processor implements Runnable
    {
        private final AtomicBoolean mRunning = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mRunning.compareAndSet(false, true))
            {
                process();
                mRunning.set(false);
            }
        }
    }

    /**
     * Processor to service the buffer queue and distribute the buffers to the registered listener.  Includes a
     * support for commanding a heart beat with each processing interval.
     */
    class ProcessorWithHeartbeat implements Runnable
    {
        private final AtomicBoolean mRunning = new AtomicBoolean();

        @Override
        public void run()
        {
            if(mRunning.compareAndSet(false, true))
            {
                process();

                try
                {
                    mHeartbeatManager.broadcast();
                }
                catch(Throwable t)
                {
                    mLog.error("Error broadcasting heartbeat during Dispatcher processing interval", t);
                }

                mRunning.set(false);
            }
        }
    }
}
