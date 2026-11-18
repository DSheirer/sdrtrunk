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
package io.github.dsheirer.dsp.filter.channelizer.output;

import io.github.dsheirer.dsp.filter.channelizer.ChannelResultsBatch;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import io.github.dsheirer.util.Dispatcher;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ChannelOutputProcessor implements IPolyphaseChannelOutputProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelOutputProcessor.class);

    /**
     * Maximum queued channel sample batches, about one second of samples.  A full batch is dropped when this channel
     * falls behind, which preserves the even-sized batch block parity that two-channel synthesis relies on.
     */
    private static final int MAXIMUM_QUEUED_BATCHES = 50;

    /**
     * Maximum channel sample arrays held for reuse: the queued batches plus the batch being processed and the batch
     * being copied.
     */
    private static final int MAXIMUM_POOLED_ARRAYS = MAXIMUM_QUEUED_BATCHES + 2;

    private Dispatcher<QueuedChannelSamples> mChannelSamplesDispatcher;
    private final Queue<float[]> mSamplePool = new ArrayBlockingQueue<>(MAXIMUM_POOLED_ARRAYS);
    private volatile int[] mChannelOffsets = new int[0];
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
        mChannelSamplesDispatcher = new Dispatcher<>(threadName,50, mHeartbeatManager, MAXIMUM_QUEUED_BATCHES);
        mChannelSamplesDispatcher.setListener(queued -> {
            try
            {
                applyPendingUpdates(queued.sequence());
                mCurrentSampleTimestamp = queued.timestamp();
                process(queued.samples(), queued.blockCount());
            }
            catch(Throwable t)
            {
                mLog.error("Error processing channel samples", t);
            }
            finally
            {
                mSamplePool.offer(queued.samples());
            }
        });
    }

    /**
     * Sets the I/Q offsets within each channel results block for the polyphase channel(s) that this processor copies.
     * The offsets take effect at the first channel results batch received after this method is invoked, the same as a
     * change queued with enqueueUpdate(), so a change of offsets and of the mixer frequency apply to the same batch.
     * This should only be invoked on the thread that delivers channel results, or before results are delivered.
     * @param offsets for each polyphase channel, which is twice the channel index.
     */
    protected void setChannelOffsets(int... offsets)
    {
        mChannelOffsets = offsets.clone();
    }

    /**
     * Current I/Q offsets within each channel results block for the polyphase channel(s) that this processor copies.
     */
    protected int[] getChannelOffsets()
    {
        return mChannelOffsets.clone();
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
        mChannelSamplesDispatcher.start();
    }

    @Override
    public void stop()
    {
        mChannelSamplesDispatcher.stop();
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

    /**
     * Copies this processor's polyphase channel(s) from the channel results batch and queues the copy for processing on
     * the dispatcher thread.  The copy is small (about 8 kB per polyphase channel for a 1024 block batch), so the batch
     * isn't retained beyond this method and its large shared sample array returns to the channelizer's pool as soon as
     * the channelizer has dispatched it to every channel.  A slow channel then queues only its own samples.
     *
     * @param channelResults batch to copy from
     */
    @Override
    public void receiveChannelResults(ChannelResultsBatch channelResults)
    {
        long sequence = mBatchSequence.incrementAndGet();

        if(!mChannelSamplesDispatcher.isRunning())
        {
            return;
        }

        int[] offsets = mChannelOffsets;
        float[] source = channelResults.getSamples();
        int blockLength = channelResults.getBlockLength();
        int blockCount = channelResults.getBlockCount();
        float[] samples = getPooledArray(blockCount * offsets.length * 2);
        int pointer = 0;

        for(int block = 0; block < blockCount; block++)
        {
            int blockOffset = block * blockLength;

            for(int offset : offsets)
            {
                samples[pointer++] = source[blockOffset + offset];
                samples[pointer++] = source[blockOffset + offset + 1];
            }
        }

        if(!mChannelSamplesDispatcher.offer(new QueuedChannelSamples(samples, blockCount, channelResults.getTimestamp(),
            sequence)))
        {
            mSamplePool.offer(samples);
        }
    }

    /**
     * Provides a reused sample array of the requested length when one is available, or a new array.
     */
    private float[] getPooledArray(int length)
    {
        float[] samples = mSamplePool.poll();
        return samples != null && samples.length == length ? samples : new float[length];
    }

    /**
     * Sub-class implementation to process one batch of copied channel samples.
     * @param samples for each block in order, with the I/Q pair of each polyphase channel in the order of the channel
     * offsets, for example I1,Q1,I2,Q2 per block for two channels.
     * @param blockCount number of blocks.  Batches start on an even block index, so the index of a block within this
     * batch has the same parity as its absolute block index.
     */
    public abstract void process(float[] samples, int blockCount);

    @Override
    public int getInputChannelCount()
    {
        return mInputChannelCount;
    }

    /**
     * Copied channel samples with the timestamp of the batch they were copied from and the batch's position in the
     * sequence of received batches.
     */
    private record QueuedChannelSamples(float[] samples, int blockCount, long timestamp, long sequence) {}

    /**
     * Configuration change and the sequence number of the last batch received before the change was requested.
     */
    private record PendingUpdate(Runnable update, long lastSequenceBefore) {}
}
