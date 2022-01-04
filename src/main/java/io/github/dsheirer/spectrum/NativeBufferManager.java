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
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;

/**
 * Thread-safe native buffer manager.  Receives incoming native buffers in a non-blocking manner.  Received buffers are
 * only enqueued if they are needed to meet the next requested sample array size.  Once the incoming producer quota
 * is reached, incoming buffers are ignored.
 *
 * Transferred native buffers are enqueued on the consumer side until enough samples are amassed to satisfy a get
 * operation.
 *
 * The queuing on both the producer and consumer side ensures the thread-safe transfer queue never overfills.
 *
 * The estimated request size argument sets the initial quota of samples to enqueue on the producer size.  This value
 * is updated on the consumer side with each request.  This class is designed for the get() operation to consistently
 * request the same size buffer, with occasional changes.
 *
 * @param <T> native buffer
 */
public class NativeBufferManager<T extends INativeBuffer>
{
    private LinkedTransferQueue<T> mTransferQueue = new LinkedTransferQueue<>();
    private List<T> mProducerQueue = new ArrayList<>();
    private List<T> mConsumerQueue = new ArrayList<>();
    private int mRequestSize;
    private int mProducerAvailable;

    /**
     * Constructs an instance
     * @param estimatedRequestSize initially, for each complex sample retrieval.
     */
    public NativeBufferManager(int estimatedRequestSize)
    {
        mRequestSize = estimatedRequestSize;
    }
    /**
     * Adds the buffer to this manager if it's needed.
     * @param nativeBuffer to add
     */
    public void add(T nativeBuffer)
    {
        //If we have enough produced buffers and the transfer queue is empty ... move them to the queue
        if(mProducerAvailable >= mRequestSize && mTransferQueue.isEmpty())
        {
            mTransferQueue.addAll(mProducerQueue);
            mProducerQueue.clear();
            mProducerAvailable = 0;
        }

        //Add this buffer to the producer queue if it's needed to meet the anticipated request size
        if(mProducerAvailable < mRequestSize)
        {
            mProducerQueue.add(nativeBuffer);
            mProducerAvailable += nativeBuffer.sampleCount();
        }
    }

    /**
     * Clears/removes all queued native buffers
     */
    public void clear()
    {
        mTransferQueue.clear();
        mProducerQueue.clear();
        mConsumerQueue.clear();
    }

    /**
     * Returns an array of complex samples of the length requested.  If there are not sufficient samples in the queue
     * to fully satisfy the request, an IOException is thrown.
     * @param requestedSamples number of complex sample pairs.
     * @return a float array with twice as many (2 samples for each I & Q pair) values as requested.
     * @throws IOException if there are (temporarily) insufficient samples available.
     */
    public float[] get(int requestedSamples) throws IOException
    {
        //Update the producer's sample quota if it changes
        if(requestedSamples != mRequestSize)
        {
            mRequestSize = requestedSamples;
        }

        List<T> drained = new ArrayList<>();
        mTransferQueue.drainTo(drained);
        mConsumerQueue.addAll(drained);

        int count = 0;

        for(T buffer: mConsumerQueue)
        {
            count += buffer.sampleCount();
        }

        if(count < requestedSamples)
        {
            throw new IOException("Insufficient samples.  Please try again later");
        }

        float[] samples = new float[requestedSamples * 2];
        int samplesPointer = 0;

        for(T buffer: mConsumerQueue)
        {
            Iterator<InterleavedComplexSamples> iterator = buffer.iteratorInterleaved();

            while(iterator.hasNext() && samplesPointer < samples.length)
            {
                InterleavedComplexSamples complexSamples = iterator.next();
                int toCopy = Math.min(samples.length - samplesPointer, complexSamples.samples().length);
                System.arraycopy(complexSamples.samples(), 0, samples, samplesPointer, toCopy);
                samplesPointer += toCopy;
            }

            if(samplesPointer >= samples.length)
            {
                break;
            }
        }

        mConsumerQueue.clear();
        return samples;
    }
}
