/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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
package module.decode.passport;

import java.util.HashMap;

import message.Message;
import message.MessageType;
import sample.Listener;
import alias.AliasList;
import bits.BinaryMessage;

public class PassportMessageProcessor implements Listener<BinaryMessage>
{
	private Listener<Message> mMessageListener;
	private IdleMessageFinder mIdleFinder = new IdleMessageFinder();
	private AliasList mAliasList;
	private PassportMessage mIdleMessage;
	
	public PassportMessageProcessor( AliasList list )
	{
		mAliasList = list;
	}
	
	@Override
    public void receive( BinaryMessage buffer )
    {
		if( mMessageListener != null )
		{
			PassportMessage message;
			
			if( mIdleMessage != null )
			{
				message = new PassportMessage( buffer, mIdleMessage, mAliasList );
			}
			else
			{
				message = new PassportMessage( buffer, mAliasList );
				mIdleFinder.receive( message );
			}

			mMessageListener.receive( message );
		}
    }
	
    public void setMessageListener( Listener<Message> listener )
    {
		mMessageListener = listener;
    }

    public void removeMessageListener()
    {
		mMessageListener = null;
    }
	
	public class IdleMessageFinder
	{
		public HashMap<String,Integer> mMessageCounts =	
					new HashMap<String,Integer>();
		
		public boolean mIdleMessageFound = false;
		
		public IdleMessageFinder()
		{
		}
		
		public void receive( PassportMessage message )
		{
			if( !mIdleMessageFound &&
				message.isValid() && 
				message.getMessageType() == MessageType.SY_IDLE )
			{
				if( mMessageCounts.containsKey( message.getBinaryMessage() ) )
				{
					int count = mMessageCounts.get( message.getBinaryMessage() );
					
					if( count >= 3 )
					{
						mIdleMessageFound = true;
						mIdleMessage = message;
						mMessageCounts = null;
					}
					else
					{
						count++;
						mMessageCounts.put( message.getBinaryMessage(), count );
					}
				}
				else
				{
					mMessageCounts.put( message.getBinaryMessage(), 1 );
				}
			}
		}
	}
}
