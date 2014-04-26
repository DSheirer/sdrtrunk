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
package bits;

import java.util.ArrayList;
import java.util.Iterator;

import sample.Broadcaster;
import sample.Listener;
import dsp.SyncDetectListener;
import dsp.SyncDetectProvider;

/**
 * MessageFramer - processes bitsets looking for a sync pattern within
 * the bits, and then extracts the message, including the sync 
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
	private Broadcaster<BitSetBuffer> mBroadcaster = 
								new Broadcaster<BitSetBuffer>();
	private ArrayList<MessageAssembler> mMessageAssemblers = 
                            new ArrayList<MessageAssembler>();

	private ArrayList<MessageAssembler> mCompletedMessageAssemblers = 
    								new ArrayList<MessageAssembler>();
    
	private BitSetBuffer mPreviousBuffer = null;

	private SyncPatternMatcher mMatcher;
    
    public MessageFramer( boolean[] syncPattern, int messageLength )
    {
        mSyncPattern = syncPattern;
        mMatcher = new SyncPatternMatcher( syncPattern );
        mMessageLength = messageLength;
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
        
        /* Check for sync match and add new message assembler */
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
     * sent (ie flushed) to all registered message listeners, and
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
     * Allow a message listener to register with this framer to receive
     * all framed messages
     */
    public void addMessageListener( Listener<BitSetBuffer> listener )
    {
        mBroadcaster.addListener( listener );
    }
    
    public void removeMessageListener( Listener<BitSetBuffer> listener )
    {
        mBroadcaster.removeListener( listener );
    }

    /*
     * Adds a message assembler to receive bits from the bit stream
     */
    private void addMessageAssembler( MessageAssembler assembler )
    {
        mMessageAssemblers.add( assembler );
    }
    
    private void removeMessageAssembler( MessageAssembler assembler )
    {
        mMessageAssemblers.remove( assembler );
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
        BitSetBuffer mMessage;
        boolean mComplete = false;
        
        MessageAssembler( int messageLength )
        {
            mMessage = new BitSetBuffer( messageLength );
        }
        
        MessageAssembler( int messageLength, boolean[] initialFill )
        {
            this(  messageLength );
            
            /* Pre-load the message with the sync pattern */
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
         * Receives one bit at a time, and assembles them into a message
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
            
            /* Once our message is complete (ie full), send it to all registered
             * message listeners, and set complete flag so for auto-removal */
            if( mMessage.isFull() )
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
            mBroadcaster.receive( mMessage );
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
