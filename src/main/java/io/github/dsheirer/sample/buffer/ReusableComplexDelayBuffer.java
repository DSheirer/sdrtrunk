/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
package io.github.dsheirer.sample.buffer;

import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.LinkedTransferQueue;

/**
 * Delay buffer/queue for reusable complex buffers with support for pre-loading new buffer listeners (ie channels)
 * with delayed sample buffers and broadcasting of incoming sample buffers to a list of listeners.
 *
 * This class is designed to compensate/account for any delays in processing of a control channel and subsequent
 * channel grants so that when a new channel is allocated, that channel can be pre-loaded with time delayed sample
 * buffers that start as close as possible to the channel grant timestamp, in order to avoid cutting off the
 * beginning of a channel grant transmission.
 *
 * New channel listeners are enqueued with a request timestamp and as new buffers arrive, the new channel listeners
 * are preloaded with delayed buffers from the queue and then added to an internal broadcaster so that they
 * receive all subsequent buffers as normal.
 *
 * This class is designed to be controlled by the sample producer thread for adding new listeners and pre-loading
 * delayed buffers.  Any listeners that are added to this class are expected to implement a non-blocking receive method
 * so as not to delay the stream of sample buffers.  Channel listeners are expected to implement buffer queue processing
 * on another thread.
 */
public class ReusableComplexDelayBuffer implements Listener<ReusableComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(ReusableComplexDelayBuffer.class);

    private ReusableBufferBroadcaster<ReusableComplexBuffer> mBroadcaster = new ReusableBufferBroadcaster<>();
    private LinkedTransferQueue<ActionRequest> mActionRequestQueue = new LinkedTransferQueue<>();
    private ActionRequest mActionRequest;

    private ReusableComplexBuffer[] mDelayBuffer;
    private int mDelayBufferPointer = 0;
    private long mBufferDuration;

    /**
     * Creates a new delay buffer with the specified delay length and the buffer sample rate.
     *
     * @param size of the delay queue
     * @param bufferDuration in milliseconds for each complex buffer processed by this instance
     */
    public ReusableComplexDelayBuffer(int size, long bufferDuration)
    {
        mDelayBuffer = new ReusableComplexBuffer[size];
        mBufferDuration = bufferDuration;
    }

    /**
     * Prepares this instance for disposal by releasing all stored sample buffers.
     */
    public void dispose()
    {
        clearBuffer();
    }

    public void clear()
    {
        //Submit a clear buffer request to be processed upon the next buffer that arrives
        mActionRequestQueue.offer(new ActionRequest());
    }

    /**
     * Clears any delayed/enqueued sample buffers.
     */
    private void clearBuffer()
    {
        for(int x = 0; x < mDelayBuffer.length; x++)
        {
            if(mDelayBuffer[x] != null)
            {
                mDelayBuffer[x].decrementUserCount();
                mDelayBuffer[x] = null;
            }
        }

        mDelayBufferPointer = 0;
    }

    @Override
    public synchronized void receive(ReusableComplexBuffer reusableComplexBuffer)
    {
        mActionRequest = mActionRequestQueue.poll();

        while(mActionRequest != null)
        {
            switch(mActionRequest.getAction())
            {
                case ADD_USER:
                    processNewListener(mActionRequest);
                    break;
                case REMOVE_USER:
                    mBroadcaster.removeListener(mActionRequest.getListener());
                    break;
                case CLEAR_BUFFER:
                    clearBuffer();
                    break;
            }

            mActionRequest = mActionRequestQueue.poll();
        }

        //Increment user count before we hand it to the broadcaster so that we can keep it as a copy for the
        //delay buffer
        reusableComplexBuffer.incrementUserCount();

        mBroadcaster.receive(reusableComplexBuffer);

        //Decrement the user count on the oldest buffer before we replace it in the delay queue
        if(mDelayBuffer[mDelayBufferPointer] != null)
        {
            mDelayBuffer[mDelayBufferPointer].decrementUserCount();
        }

        //Store the new buffer in the delay queue and increment the pointer
        mDelayBuffer[mDelayBufferPointer++] = reusableComplexBuffer;

        //Wrap the delay buffer pointer as needed
        if(mDelayBufferPointer >= mDelayBuffer.length)
        {
            mDelayBufferPointer = 0;
        }
    }

    /**
     * Indicates if any listeners are registered with this delay buffer
     */
    public boolean hasListeners()
    {
        return mBroadcaster.hasListeners();
    }

    /**
     * Processes any newly added listeners by checking all buffers in the delay queue and pre-loading the
     * listeners with any buffers that occur on or after the listener's requested start timestamp.
     */
    private void processNewListener(ActionRequest listenerToAdd)
    {
        ReusableComplexBuffer bufferToEvaluate;

        int pointer = mDelayBufferPointer;

        int preloadedBufferCount = 0;

        for(int x = 0; x < mDelayBuffer.length; x++)
        {
            bufferToEvaluate = mDelayBuffer[pointer];

            if(bufferToEvaluate != null &&
                (bufferToEvaluate.getTimestamp() >= listenerToAdd.getTimestamp() ||
                    bufferToEvaluate.getTimestamp() + mBufferDuration >= listenerToAdd.getTimestamp()))
            {
                bufferToEvaluate.incrementUserCount();
                listenerToAdd.getListener().receive(bufferToEvaluate);
                preloadedBufferCount++;
            }

            pointer++;

            if(pointer >= mDelayBuffer.length)
            {
                pointer = 0;
            }
        }

        mBroadcaster.addListener(listenerToAdd.getListener());
    }

    /**
     * Adds the listener to receive a copy of each buffer.  The listener will be preloaded with delayed buffers
     * that are at or after the specified timestamp.
     *
     * @param listener to add
     * @param timestamp of the oldest sample buffers to preload to the listener
     */
    public void addListener(Listener<ReusableComplexBuffer> listener, long timestamp)
    {
        mActionRequestQueue.add(new ActionRequest(listener, timestamp));
    }

    /**
     * Removes the listener from receiving sample buffers
     */
    public void removeListener(Listener<ReusableComplexBuffer> listener)
    {
        mActionRequestQueue.add(new ActionRequest(listener));
    }

    private enum Action{ADD_USER, REMOVE_USER, CLEAR_BUFFER};

    /**
     * Actions that must be completed on the incoming sample stream thread.
     */
    public class ActionRequest
    {
        private Action mAction;
        private Listener<ReusableComplexBuffer> mListener;
        private long mTimestamp;

        /**
         * Creates an add listener request
         * @param listener to add
         * @param timestamp for the preloading buffers to the listener from the delay queue
         */
        public ActionRequest(Listener<ReusableComplexBuffer> listener, long timestamp)
        {
            mAction = Action.ADD_USER;
            mListener = listener;
            mTimestamp = timestamp;
        }

        /**
         * Creates a remove listener request
         * @param listener
         */
        public ActionRequest(Listener<ReusableComplexBuffer> listener)
        {
            mAction = Action.REMOVE_USER;
            mListener = listener;
        }

        /**
         * Creates a clearBuffer buffer request
         */
        public ActionRequest()
        {
            mAction = Action.CLEAR_BUFFER;
        }

        public Action getAction()
        {
            return mAction;
        }

        /**
         * Listener to be added/removed
         */
        public Listener<ReusableComplexBuffer> getListener()
        {
            return mListener;
        }

        /**
         * Requested timestamp for buffers to be preloaded.
         */
        public long getTimestamp()
        {
            return mTimestamp;
        }
    }
}
