/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.sample.IOverflowListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.OverflowableTransferQueue;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ContinuousBufferProcessor<E> implements Listener<E>
{
    private final static Logger mLog = LoggerFactory.getLogger(ContinuousBufferProcessor.class);

    protected OverflowableTransferQueue<E> mQueue;
    private Listener<List<E>> mListener;
    private ScheduledFuture<?> mScheduledFuture;
    private AtomicBoolean mRunning = new AtomicBoolean();
    private long mProcessingPeriod = 5; //milliseconds

    /**
     * Scheduled Buffer Processor combines an internal overflowable buffer with a scheduled runnable processing task
     * for periodically distributing internally queued elements to the registered listener.  This processor provides
     * a convenient way to create a thread-safe buffer for receiving elements from one thread/runnable and then
     * distributing those elements to a registered listener where distribution occurs on a separate scheduled thread
     * pool runnable thread.  This allows the calling input thread to quickly return without incurring any subsequent
     * processing workload.
     *
     * The internal queue is an overflowable queue implementation that allows a listener to be registered to receive
     * notifications of overflow and reset state.  Queue sizing parameters are specified in the constructor.
     *
     * @param maximumSize of the internal queue (overflow happens when this is exceeded)
     * @param resetThreshold of the internal queue (overflow reset happens once queue size falls below this threshold
     */
    public ContinuousBufferProcessor(int maximumSize, int resetThreshold)
    {
        this(new OverflowableTransferQueue<>(maximumSize, resetThreshold));
    }

    /**
     * Listener to receive the queued buffers each time this processor runs.
     */
    protected Listener<List<E>> getListener()
    {
        return mListener;
    }

    /**
     * Sets the processing periodicity or run interval for how often the buffer queue is processed.
     *
     * @param milliseconds (default = 5 milliseconds)
     */
    public void setProcessingPeriod(long milliseconds)
    {
        mProcessingPeriod = milliseconds;
    }

    /**
     * Scheduled Buffer Processor combines an internal overflowable buffer with a scheduled runnable processing task
     * for periodically distributing internally queued elements to the registered listener.  This processor provides
     * a convenient way to create a thread-safe buffer for receiving elements from one thread/runnable and then
     * distributing those elements to a registered listener where distribution occurs on a separate scheduled thread
     * pool runnable thread.  This allows the calling input thread to quickly return without incurring any subsequent
     * processing workload.
     *
     * The internal queue is an overflowable queue implementation that allows a listener to be registered to receive
     * notifications of overflow and reset state.  Queue sizing parameters are specified in the constructor.
     *
     * @param queue implmentation of an overflowable transfer queue
     */
    public ContinuousBufferProcessor(OverflowableTransferQueue<E> queue)
    {
        mQueue = queue;
    }

    /**
     * Sets the listener to receive notifications of buffer overflow and/or reset.  Note: this method can also be
     * invoked with a null argument to clear the previously registered overflow listener.
     *
     * @param listener to receive overflow/reset notifications.
     */
    public void setOverflowListener(IOverflowListener listener)
    {
        mQueue.setOverflowListener(listener);
    }

    /**
     * Sets or changes the listener to receive buffers from this processor.
     * @param listener to receive buffers
     */
    public void setListener(Listener<List<E>> listener)
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
    @Override
    public void receive(E e)
    {
        mQueue.offer(e);
    }

    /**
     * Starts this buffer processor and allows queuing of incoming buffers.
     */
    public void start()
    {
        if(mRunning.compareAndSet(false, true))
        {
            mScheduledFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new Processor(), 0, mProcessingPeriod, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Stops this buffer processor, stops queuing of incoming buffers and clears the internal buffer queue.
     */
    public void stop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mScheduledFuture != null)
            {
                mScheduledFuture.cancel(true);
                mScheduledFuture = null;
            }

            clearQueue();
        }
    }

    /**
     * Stops this buffer processor,stops queuing of incoming buffers and flushes the internal buffer queue
     * contents to the listener.
     */
    public void flushAndStop()
    {
        if(mRunning.compareAndSet(true, false))
        {
            if(mScheduledFuture != null)
            {
                mScheduledFuture.cancel(true);
                mScheduledFuture = null;
            }

            process();
        }
    }

    /**
     * Distributes queued buffers to the listener
     */
    protected void process()
    {
        List<E> buffers = new ArrayList<>();

        try
        {
            mQueue.drainTo(buffers);

            if(mListener != null)
            {
                mListener.receive(buffers);
            }
        }
        catch(Throwable throwable)
        {
            mLog.error("Error while dispatching buffers to listener", throwable);
        }
    }

    /**
     * Clears any buffers from the queue
     */
    protected Collection<E> clearQueue()
    {
        Collection<E> clearedElements = new ArrayList<>();
        mQueue.drainTo(clearedElements);
        return clearedElements;
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
            process();
        }
    }
}
