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
package decode.ltrstandard;

import message.Message;
import message.MessageDirection;
import sample.Broadcaster;
import sample.Listener;
import alias.AliasList;
import bits.BitSetBuffer;

public class LTRStandardMessageProcessor implements Listener<BitSetBuffer>
{
	private MessageDirection mDirection;
	private Broadcaster<Message> mBroadcaster = new Broadcaster<Message>();
	private AliasList mAliasList;
	
	public LTRStandardMessageProcessor( MessageDirection direction, AliasList list )
	{
		mDirection = direction;
		mAliasList = list;
	}
	
	@Override
    public void receive( BitSetBuffer buffer )
    {
		LTRStandardMessage message;
		
		if( mDirection == MessageDirection.OSW )
		{
			message = new LTRStandardOSWMessage( buffer, mAliasList );
		}
		else
		{
			buffer.flip( 0, 40 );

			message = new LTRStandardISWMessage( buffer, mAliasList );
		}
		
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
}
