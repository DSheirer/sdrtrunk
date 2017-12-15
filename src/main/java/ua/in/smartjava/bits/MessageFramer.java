/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package ua.in.smartjava.bits;

import java.util.ArrayList;
import java.util.Iterator;

import ua.in.smartjava.sample.Broadcaster;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.dsp.symbol.SyncDetectListener;
import ua.in.smartjava.dsp.symbol.SyncDetectProvider;

/**
 * MessageFramer - processes bitsets looking for a sync pattern within
 * the ua.in.smartjava.bits, and then extracts the ua.in.smartjava.message, including the sync
 * pattern, for a total bit length of messageLength.
 * 
 * Will extract multiple messages simultaneously, for each sync pattern that is
 * encountered within the bitset bit stream.
 */
public class MessageFramer implements Listener<Boolean>, 
									  SyncDetectProvider
{
	private boolean[] mSyncPattern;
	private int mMessageLength;
	private SyncDetectListener mSyncDetectListener;
	private Broadcaster<BinaryMessage> mBroadcaster = 
								new Broadcaster<BinaryMessage>();
	private ArrayList<MessageAssembler> mMessageAssemblers = 
                            new ArrayList<MessageAssembler>();

	private ArrayList<MessageAssembler> mCompletedMessageAssemblers = 
    								new ArrayList<MessageAssembler>();
    
	private BinaryMessage mPreviousBuffer = null;

	private SyncPatternMatcher mMatcher;
    
    public MessageFramer( boolean[] syncPattern, int messageLength )
    {
        mSyncPattern = syncPattern;
        mMatcher = new SyncPatternMatcher( syncPattern );
        mMessageLength = messageLength;
    }
    
    public void reset()
    {
    	for( MessageAssembler assembler: mMessageAssemblers )
    	{
    		assembler.dispose();
    	}
        	
        mMessageAssemblers.clear();
    }    
    
    public void dispose()
    {
    	mBroadcaster.dispose();
    	mCompletedMessageAssemblers.clear();
    	mMessageAssemblers.clear();
    }

    @Override
    public void receive( Boolean bit )
    {
    	mMatcher.receive( bit );
    	
        Iterator<MessageAssembler> it = mMessageAssemblers.iterator();
        
        MessageAssembler assembler;
        
        while( it.hasNext() )
        {
            assembler = it.next();

            /* Dispose and remove any completed assemblers */
            if( assembler.complete() )
            {
            	assembler.dispose();
            	it.remove();
            }
            /* Otherwise, send them the bit */
            else
            {
                assembler.receive( bit );
            }
        }
        
        /* Check for sync match and add new ua.in.smartjava.message assembler */
    	if( mMatcher.matches() )
    	{
            addMessageAssembler( new MessageAssembler( mMessageLength, mSyncPattern ) );
            
            /* Notify any sync detect listener(s) */
            if( mSyncDetectListener != null )
            {
            	mSyncDetectListener.syncDetected();
            }
    	}
    }
    
    /**
     * Causes all messages currently under assembly to be forcibly
     * sent (ie flushed) to all registered ua.in.smartjava.message listeners, and
     * subsequently, all assemblers to be deleted
     */
    public void flush()
    {
        for( MessageAssembler assembler: mMessageAssemblers )
        {
            assembler.flush();
        }
    }
    
	@Override
    public void setSyncDetectListener( SyncDetectListener listener )
    {
		mSyncDetectListener = listener;
    }
    
    /**
     * Allow a ua.in.smartjava.message listener to register with this framer to receive
     * all framed messages
     */
    public void addMessageListener( Listener<BinaryMessage> listener )
    {
        mBroadcaster.addListener( listener );
    }
    
    public void removeMessageListener( Listener<BinaryMessage> listener )
    {
        mBroadcaster.removeListener( listener );
    }

    /*
     * Adds a ua.in.smartjava.message assembler to receive ua.in.smartjava.bits from the bit stream
     */
    private void addMessageAssembler( MessageAssembler assembler )
    {
        mMessageAssemblers.add( assembler );
    }

    @SuppressWarnings( "unused" )
	private void removeMessageAssembler( MessageAssembler assembler )
    {
        mMessageAssemblers.remove( assembler );
    }

    /**
     * Assembles a binary ua.in.smartjava.message, starting with the initial fill of the
     * identified sync pattern, and every bit thereafter.  Once the accumulated
     * ua.in.smartjava.bits equal the ua.in.smartjava.message length, the ua.in.smartjava.message is sent and the assembler
     * flags itself as complete.
     * 
     * By design, multiple ua.in.smartjava.message assemblers can exist at the same time, each
     * assembling different, overlapping potential messages
     */
    private class MessageAssembler implements Listener<Boolean>
    {
        BinaryMessage mMessage;
        boolean mComplete = false;
        
        MessageAssembler( int messageLength )
        {
            mMessage = new BinaryMessage( messageLength );
        }
        
        MessageAssembler( int messageLength, boolean[] initialFill )
        {
            this(  messageLength );
            
            /* Pre-load the ua.in.smartjava.message with the sync pattern */
            for( int x = 0; x < initialFill.length; x++ )
            {
                receive( initialFill[ x ] );
            }
        }
        
        public void dispose()
        {
        	mMessage = null;
        }

        @Override
        /**
         * Receives one bit at a time, and assembles them into a ua.in.smartjava.message
         */
        public void receive( Boolean bit )
        {
            try
            {
                mMessage.add( bit );
            }
            catch( BitSetFullException e )
            {
                e.printStackTrace();
            }
            
            /* Once our ua.in.smartjava.message is complete (ie full), send it to all registered
             * ua.in.smartjava.message listeners, and set complete flag so for auto-removal */
            if( mMessage.isFull() )
            {
                mComplete = true;
                flush();
            }
        }

        /**
         * Flushes/Sends the current ua.in.smartjava.message, or partial ua.in.smartjava.message, and sets
         * complete flag to true, so that we can be auto-removed
         */
        public void flush()
        {
            mBroadcaster.receive( mMessage );
            mComplete = true;
        }

        /**
         * Flag to indicate when this assembler has received all of the ua.in.smartjava.bits it
         * is looking for (ie ua.in.smartjava.message length), and should then be removed from
         * receiving any more ua.in.smartjava.bits
         */
        public boolean complete()
        {
            return mComplete;
        }
    }
}
