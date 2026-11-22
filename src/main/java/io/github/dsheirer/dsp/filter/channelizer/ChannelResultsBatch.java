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
package io.github.dsheirer.dsp.filter.channelizer;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Batch of polyphase channelizer output blocks stored in a single contiguous array, where each block is an interleaved
 * I/Q pair for each channel.  The sample at channel offset C of block B is at index (B * block length) + C.
 *
 * The sample array is returned to the channelizer's pool for reuse when the last reference is released.  This batch
 * starts with one reference that is owned by the creator.  Each consumer that keeps the batch beyond the method that
 * received it must invoke retain() and then invoke release() when finished, and the samples must not be accessed after
 * that.  A reference that is never released only means that the array is garbage collected instead of being reused.
 */
public class ChannelResultsBatch
{
    private final float[] mSamples;
    private final int mBlockLength;
    private final long mTimestamp;
    private final Queue<float[]> mPool;
    private final AtomicInteger mReferenceCount = new AtomicInteger(1);

    /**
     * Constructs an instance
     * @param samples for this batch
     * @param blockLength as the number of array elements per block
     * @param timestamp of the sample buffer that completed this batch
     * @param pool to receive the sample array when the last reference is released
     */
    public ChannelResultsBatch(float[] samples, int blockLength, long timestamp, Queue<float[]> pool)
    {
        mSamples = samples;
        mBlockLength = blockLength;
        mTimestamp = timestamp;
        mPool = pool;
    }

    /**
     * Sample array containing all blocks.
     */
    public float[] getSamples()
    {
        return mSamples;
    }

    /**
     * Number of array elements per block.
     */
    public int getBlockLength()
    {
        return mBlockLength;
    }

    /**
     * Number of blocks in this batch.
     */
    public int getBlockCount()
    {
        return mSamples.length / mBlockLength;
    }

    /**
     * Timestamp of the sample buffer that completed this batch.
     * @return time in milliseconds
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Adds a reference to this batch.
     */
    public void retain()
    {
        mReferenceCount.incrementAndGet();
    }

    /**
     * Releases a reference to this batch.  The sample array is returned to the pool after the last release.
     */
    public void release()
    {
        if(mReferenceCount.decrementAndGet() == 0)
        {
            mPool.offer(mSamples);
        }
    }
}
