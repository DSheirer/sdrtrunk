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
package decode.mdc1200;

import message.Message;
import sample.Broadcaster;
import sample.Listener;
import alias.AliasList;
import bits.BitSetBuffer;

public class MDCMessageProcessor implements Listener<BitSetBuffer>
{
	private static int sMESSAGE_LENGTH = 112;
	
	private Broadcaster<Message> mBroadcaster = new Broadcaster<Message>();
	private AliasList mAliasList;
	
	public MDCMessageProcessor( AliasList aliasList )
	{
		mAliasList = aliasList;
	}
	
	public void dispose()
	{
		mBroadcaster.dispose();
	}
	
	@Override
    public void receive( BitSetBuffer buffer )
    {
		/**
		 * De-interleave the 112 bits of message one, starting after the sync
		 * pattern
		 */
		deinterleave( buffer, 40 );

		/**
		 * For simplicity, we also deinterleave message 2, which may or may not
		 * be there, starting after the initial sync ( 0 - 39 ) and the initial
		 * message ( 40 - 151 ) and the second sync ( 152 - 191 ).
		 */
		deinterleave( buffer, 192 );

		/**
		 * Wrap the buffer in a message along with the designated alias list
		 * and send it on its merry way
		 */
		MDCMessage message = new MDCMessage( buffer, mAliasList );
		
    	mBroadcaster.receive( message );
    }
	
    public void addMessageListener( Listener<Message> listener )
    {
		mBroadcaster.addListener( listener );
    }

    public void removeMessageListener( Listener<Message> listener )
    {
    	mBroadcaster.removeListener( listener );
    }
	
	/**
	 * Removes NRZ-I encoding from the entire buffer.  Assumes that the initial
	 * bit will be a 0 after decode, despite what is currently in the buffer,
	 * in order to maintain correct polarity of the decoded message.
	 */
	private void removeNRZIEncoding( BitSetBuffer buffer )
	{
		/**
		 * Clear bit position 0, the first bit of the sync pattern, to start
		 * the decode with the correct polarity
		 */
		buffer.clear( 0 );

		/**
		 * Set each bit according to the state of the previous bit using XOR
		 * as follows:
		 * 
		 * 	0 = same as previous state
		 * 	1 = inverse of previous state
		 */
		for( int x = 1; x < buffer.size(); x++ )
		{
			if( buffer.get( x - 1 ) ^ buffer.get( x ) )
			{
				buffer.set( x );
			}
			else
			{
				buffer.clear( x );
			}
		}
	}
	
	/**
	 * Deinterleaves a 112-bit packet, starting at the offset into the buffer
	 */
	private void deinterleave( BitSetBuffer buffer, int offset )
	{
		if( buffer.size() < sMESSAGE_LENGTH + offset )
		{
			throw new IllegalArgumentException( "MDCMessageProcessor - "
				+ "cannot deinterleave message - message buffer too short" );
		}
		
		BitSetBuffer deinterleaved = new BitSetBuffer( 112 );

		int deinterleavedPointer = 0;
		
		for( int column = 0; column < 16; column++ )
		{
			for( int row = 0; row < 7; row++ )
			{
				if( buffer.get( ( row * 16 ) + column + offset ) )
				{
					deinterleaved.set( deinterleavedPointer );
				}
				
				deinterleavedPointer++;
			}
		}

		//Overlay the deinterleaved bits back onto the source message and
		//return it
		buffer.clear( offset, offset + sMESSAGE_LENGTH );
		
		for( int x = 0; x < sMESSAGE_LENGTH; x++ )
		{
			if( deinterleaved.get( x ) )
			{
				buffer.set( x + offset );
			}
			else
			{
				buffer.clear( x + offset );
			}
		}
	}
}
