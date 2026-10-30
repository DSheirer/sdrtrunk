/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.dsp.filter.channelizer.output;

import io.github.dsheirer.dsp.filter.channelizer.ChannelResultsBatch;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import io.github.dsheirer.util.Dispatcher;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ChannelOutputProcessor implements IPolyphaseChannelOutputProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelOutputProcessor.class);

    /**
     * Maximum queued channel results batches, about one second of samples.  A full batch is dropped when this channel
     * falls behind, which preserves the even-sized batch block parity that two-channel synthesis relies on.
     */
    private static final int MAXIMUM_QUEUED_BATCHES = 50;

    private Dispatcher<QueuedChannelResults> mChannelResultsDispatcher;
    private final Queue<PendingUpdate> mPendingUpdates = new ConcurrentLinkedQueue<>();
    private final AtomicLong mBatchSequence = new AtomicLong();
    private HeartbeatManager mHeartbeatManager;
    protected volatile Listener<ComplexSamples> mComplexSamplesListener;
    private int mInputChannelCount;
    private long mCurrentSampleTimestamp = System.currentTimeMillis();

    /**
     * Base class for polyphase channelizer output channel processing.  Provides built-in frequency translation
     * oscillator support to apply frequency correction to the channel sample stream as requested by sample consumer.
     *
     * @param inputChannelCount is the number of input channels for this output processor
     * @param heartbeatManager to receive pings on the dispatcher thread
     * @param threadName for the dispatcher
     */
    public ChannelOutputProcessor(int inputChannelCount, HeartbeatManager heartbeatManager, String threadName)
    {
        mInputChannelCount = inputChannelCount;
        //Process 1/10th of the sample rate per second at a rate of 20 times a second (200% of anticipated rate)
        mHeartbeatManager = heartbeatManager;
        mChannelResultsDispatcher = new Dispatcher(threadName,50, mHeartbeatManager, MAXIMUM_QUEUED_BATCHES);
        mChannelResultsDispatcher.setListener(queued -> {
            try
            {
                applyPendingUpdates(queued.sequence());
                mCurrentSampleTimestamp = queued.channelResults().getTimestamp();
                process(queued.channelResults());
            }
            catch(Throwable t)
            {
                mLog.error("Error processing channel results", t);
            }
            finally
            {
                queued.channelResults().release();
            }
        });
    }

    /**
     * Queues a configuration change to apply on the dispatcher thread, so that configuration changes don't overlap with
     * sample processing.  The change takes effect at the first channel results batch received after this method is
     * invoked and batches that are already queued continue to use the previous configuration.  A change is not lost if
     * that batch is dropped, since it then takes effect at the following batch.
     * @param update to apply
     */
    protected void enqueueUpdate(Runnable update)
    {
        mPendingUpdates.add(new PendingUpdate(update, mBatchSequence.get()));
    }

    /**
     * Applies queued configuration changes that take effect at or before the batch sequence number.  This should only
     * be invoked on the dispatcher thread.
     * @param sequence of the batch about to be processed
     */
    private void applyPendingUpdates(long sequence)
    {
        PendingUpdate update;

        while((update = mPendingUpdates.peek()) != null && update.lastSequenceBefore() < sequence)
        {
            mPendingUpdates.poll();
            update.update().run();
        }
    }

    /**
     * Timestamp for the current series of samples.
     * @return time in milliseconds to use with assembled complex sample buffers.
     */
    protected long getCurrentSampleTimestamp()
    {
        return mCurrentSampleTimestamp;
    }

    @Override
    public void start()
    {
        mChannelResultsDispatcher.start();
    }

    @Override
    public void stop()
    {
        mChannelResultsDispatcher.stop();
    }

    /**
     * Registers the listener to receive the assembled complex sample buffers from this processor.
     */
    @Override
    public void setListener(Listener<ComplexSamples> listener)
    {
        mComplexSamplesListener = listener;
    }

    @Override
    public int getPolyphaseChannelIndexCount()
    {
        return mInputChannelCount;
    }

    public void dispose()
    {
    }

    @Override
    public void receiveChannelResults(ChannelResultsBatch channelResults)
    {
        channelResults.retain();

        if(!mChannelResultsDispatcher.offer(new QueuedChannelResults(channelResults, mBatchSequence.incrementAndGet())))
        {
            channelResults.release();
        }
    }

    /**
     * Sub-class implementation to process one polyphase channelizer results batch.
     * @param channelResults to process
     */
    public abstract void process(ChannelResultsBatch channelResults);

    @Override
    public int getInputChannelCount()
    {
        return mInputChannelCount;
    }

    /**
     * Channel results batch and its position in the sequence of received batches.
     */
    private record QueuedChannelResults(ChannelResultsBatch channelResults, long sequence) {}

    /**
     * Configuration change and the sequence number of the last batch received before the change was requested.
     */
    private record PendingUpdate(Runnable update, long lastSequenceBefore) {}
}
