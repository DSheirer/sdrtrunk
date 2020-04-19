/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.bits;

import io.github.dsheirer.dsp.symbol.ISyncDetectListener;
import io.github.dsheirer.dsp.symbol.SyncDetectProvider;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MessageFramer - processes bitsets looking for a sync pattern within
 * the bits, and then extracts the message, including the sync
 * pattern, for a total bit length of messageLength.
 *
 * Will extract multiple messages simultaneously, for each sync pattern that is
 * encountered within the bitset bit stream.
 */
public class MessageFramer implements IBinarySymbolProcessor, Listener<Boolean>, SyncDetectProvider
{
    private boolean[] mSyncPattern;
    private int mMessageLength;
    private ISyncDetectListener mSyncDetectListener;
    private Broadcaster<CorrectedBinaryMessage> mBroadcaster = new Broadcaster<>();
    private List<MessageAssembler> mMessageAssemblers = new ArrayList<>();
    private List<MessageAssembler> mCompletedMessageAssemblers = new ArrayList<>();
    private SyncPatternMatcher mMatcher;

    public MessageFramer(boolean[] syncPattern, int messageLength)
    {
        mSyncPattern = syncPattern;
        mMatcher = new SyncPatternMatcher(syncPattern);
        mMessageLength = messageLength;
    }

    public void reset()
    {
        for(MessageAssembler assembler : mMessageAssemblers)
        {
            assembler.dispose();
        }

        mMessageAssemblers.clear();
    }

    public void dispose()
    {
        mBroadcaster.dispose();
        mCompletedMessageAssemblers.clear();
    }

    public void process(boolean bit)
    {
        mMatcher.receive(bit);

        Iterator<MessageAssembler> it = mMessageAssemblers.iterator();

        MessageAssembler assembler;

        while(it.hasNext())
        {
            assembler = it.next();

            /* Dispose and remove any completed assemblers */
            if(assembler.complete())
            {
                assembler.dispose();
                it.remove();
            }
            /* Otherwise, send them the bit */
            else
            {
                assembler.receive(bit);
            }
        }

        /* Check for sync match and add new message assembler */
        if(mMatcher.matches())
        {
            addMessageAssembler(new MessageAssembler(mMessageLength, mSyncPattern));

            /* Notify any sync detect listener(s) */
            if(mSyncDetectListener != null)
            {
                mSyncDetectListener.syncDetected(0);
            }
        }
    }

    @Deprecated //Legacy support ... remove once all producers are converted to use receive(boolean bit) method
    @Override
    public void receive(Boolean bit)
    {
        process(bit);
    }

    @Override
    public void setSyncDetectListener(ISyncDetectListener listener)
    {
        mSyncDetectListener = listener;
    }

    /**
     * Allow a message listener to register with this framer to receive
     * all framed messages
     */
    public void addMessageListener(Listener<CorrectedBinaryMessage> listener)
    {
        mBroadcaster.addListener(listener);
    }

    public void removeMessageListener(Listener<CorrectedBinaryMessage> listener)
    {
        mBroadcaster.removeListener(listener);
    }

    /*
     * Adds a message assembler to receive bits from the bit stream
     */
    private void addMessageAssembler(MessageAssembler assembler)
    {
        mMessageAssemblers.add(assembler);
    }

    private void removeMessageAssembler(MessageAssembler assembler)
    {
        mMessageAssemblers.remove(assembler);
    }

    /**
     * Assembles a binary message, starting with the initial fill of the
     * identified sync pattern, and every bit thereafter.  Once the accumulated
     * bits equal the message length, the message is sent and the assembler
     * flags itself as complete.
     *
     * By design, multiple message assemblers can exist at the same time, each
     * assembling different, overlapping potential messages
     */
    private class MessageAssembler implements Listener<Boolean>
    {
        CorrectedBinaryMessage mMessage;
        boolean mComplete = false;

        MessageAssembler(int messageLength)
        {
            mMessage = new CorrectedBinaryMessage(messageLength);
        }

        MessageAssembler(int messageLength, boolean[] initialFill)
        {
            this(messageLength);

            /* Pre-load the message with the sync pattern */
            for (boolean b : initialFill) {
                receive(b);
            }
        }

        public void dispose()
        {
            mMessage = null;
        }

        @Override
        /**
         * Receives one bit at a time, and assembles them into a message
         */
        public void receive(Boolean bit)
        {
            try
            {
                mMessage.add(bit);
            }
            catch(BitSetFullException e)
            {
                e.printStackTrace();
            }

            /* Once our message is complete (ie full), send it to all registered
             * message listeners, and set complete flag so for auto-removal */
            if(mMessage.isFull())
            {
                mComplete = true;
                flush();
            }
        }

        /**
         * Flushes/Sends the current message, or partial message, and sets
         * complete flag to true, so that we can be auto-removed
         */
        public void flush()
        {
            mBroadcaster.receive(mMessage);
            mComplete = true;
        }

        /**
         * Flag to indicate when this assembler has received all of the bits it
         * is looking for (ie message length), and should then be removed from
         * receiving any more bits
         */
        public boolean complete()
        {
            return mComplete;
        }
    }
}
