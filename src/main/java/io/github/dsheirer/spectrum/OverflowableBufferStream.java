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
package io.github.dsheirer.spectrum;


import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.OverflowableTransferQueue;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Iterator;
import java.util.LinkedList;

public class OverflowableBufferStream<T extends INativeBuffer> extends OverflowableTransferQueue<T>
{
    private static final Logger mLog = LoggerFactory.getLogger(OverflowableBufferStream.class);

    private int mFlushCount = 0;
    private Iterator<InterleavedComplexSamples> mCurrentNativeBufferIterator;
    private InterleavedComplexSamples mCurrentBuffer;
    private int mCurrentBufferPointer = 0;
    private FloatBuffer mFloatBuffer;
    private LinkedList<T> mBufferList = new LinkedList<>();
    private int mBufferFetchLimit;

    /**
     * Stream for receiving reusable buffers from a producer thread and providing access to a consumer thread to read
     * the sample data.  Extends the overflowable transfer queue to define maximum and reset thresholds to ensure the
     * queue doesn't grow too large.
     *
     * This stream is designed for consistent sample size requests each time the get() samples method is invoked, such
     * as might be needed for an FFT processor.  It uses an internal float buffer that must be resized whenever the
     * get(sample size) argument changes, so consider using another option if you anticipate that your sample size
     * argument during get() operations will vary.
     *
     * @param maxSize of the internal buffer queue - defines the overflow threshold
     * @param resetThreshold of the internal buffer queue - defines when the overflow state will be cleared once the
     * queue size has reduced below this threshold
     * @param arraySize of the anticipated samples to fetch using the get(x,y) method.
     */
    public OverflowableBufferStream(int maxSize, int resetThreshold, int arraySize)
    {
        super(maxSize, resetThreshold);
        mFloatBuffer = FloatBuffer.allocate(arraySize);
        mBufferFetchLimit = maxSize - resetThreshold;
    }

    /**
     * Retrieves the specified number of float samples from the stream.  When a non-zero overlap is specified, a
     * quantity of samples from the previous buffer are preloaded in the buffer and new samples are added to make a
     * full array count.
     *
     * @param sampleCount of float samples to pull from the stream
     * @param overlap to reuse part of the previous buffer's samples in this fetch - must be less than sample count
     * @return an array of float samples from the stream
     * @throws IOException if the buffer queue is/becomes empty and the samples cannot be provided
     * @throws IllegalArgumentException if the overlap argument is not less than the sample count argument
     */
    public float[] get(int sampleCount, int overlap) throws IOException
    {
        if(overlap >= sampleCount)
        {
            throw new IllegalArgumentException("Overlap must be less than the requested sample count");
        }

        if(mCurrentBuffer == null)
        {
            //This will throw an IO exception if the queue is empty
            getNextBuffer();
        }

        //Resize the float buffer if the request size changes
        if(mFloatBuffer.capacity() != sampleCount)
        {
            mFloatBuffer = FloatBuffer.allocate(sampleCount);
        }

        while(mFloatBuffer.hasRemaining())
        {
            int available = mCurrentBuffer.samples().length - mCurrentBufferPointer;

            if(available <= 0)
            {
                getNextBuffer();
            }
            else
            {
                //Fill the buffer to capacity
                int toCopy = mFloatBuffer.remaining();

                if(available < toCopy)
                {
                    toCopy = available;
                }

                if(toCopy > 0)
                {
                    mFloatBuffer.put(mCurrentBuffer.samples(), mCurrentBufferPointer, toCopy);
                    mCurrentBufferPointer += toCopy;
                }
            }
        }

        //If we get to here, the float buffer is full.  Get the samples and then refill with overlap if non-zero
        mFloatBuffer.rewind();
        float[] samples = new float[sampleCount];
        mFloatBuffer.get(samples);
        mFloatBuffer.clear();

        if(overlap > 0)
        {
            mFloatBuffer.put(samples, samples.length - overlap, overlap);
        }

        try
        {
            performFlush();
        }
        catch(IOException ioe)
        {
            //Do nothing if we get an IO exception ... the buffer is empty
        }

        //Reset flush count to zero, even if we ran out of buffers
        mFlushCount = 0;

        return samples;
    }

    /**
     * Throws away incoming sample data until mFlushCount is zeroized or the queue is empty.
     *
     * @throws IOException if the queue is emptied while performing flush.
     */
    private void performFlush() throws IOException
    {
        //Flush sample data as requested
        while(mFlushCount > 0)
        {
            int available = mCurrentBuffer.samples().length - mCurrentBufferPointer;

            if(available <= mFlushCount)
            {
                mFlushCount -= available;
                getNextBuffer();
            }
            else
            {
                mCurrentBufferPointer += mFlushCount;
                mFlushCount = 0;
            }
        }
    }

    /**
     * Flushes the specified number of samples from the incoming stream.
     * @param sampleCount to throw away
     */
    public void flush(int sampleCount)
    {
        mFlushCount += sampleCount;
    }

    /**
     * Fetches the next buffer from the queue.
     *
     * @throws IOException if the queue is currently empty
     */
    private void getNextBuffer() throws IOException
    {
        if(mCurrentNativeBufferIterator == null)
        {
            if(mBufferList.isEmpty())
            {
                drainTo(mBufferList, mBufferFetchLimit);
            }

            if(!mBufferList.isEmpty())
            {
                T buffer = mBufferList.poll();

                if(buffer != null)
                {
                    mCurrentNativeBufferIterator = buffer.iteratorInterleaved();
                }
            }
        }

        if(mCurrentNativeBufferIterator != null && !mCurrentNativeBufferIterator.hasNext())
        {
            mCurrentNativeBufferIterator = null;
            getNextBuffer();
            return;
        }

        if(mCurrentNativeBufferIterator != null && mCurrentNativeBufferIterator.hasNext())
        {
            mCurrentBuffer = mCurrentNativeBufferIterator.next();
            mCurrentBufferPointer = 0;
        }
        else
        {
            mCurrentBuffer = null;
            throw new IOException("Buffer queue is empty");
        }
    }

    /**
     * Clears all elements from the queue and resets the internal counter to 0
     */
    @Override
    public void clear()
    {
        synchronized(mQueue)
        {
            mQueue.clear();
            mCounter.set(0);
            mOverflow.set(false);
        }
    }
}
