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
package module.decode.ltrstandard;

import message.Message;
import message.MessageDirection;
import sample.Listener;
import alias.AliasList;
import bits.BinaryMessage;

public class LTRStandardMessageProcessor implements Listener<BinaryMessage>
{
	private MessageDirection mDirection;
	private AliasList mAliasList;
	private Listener<Message> mMessageListener;
	
	public LTRStandardMessageProcessor( MessageDirection direction, AliasList list )
	{
		mDirection = direction;
		mAliasList = list;
	}
	
	@Override
    public void receive( BinaryMessage buffer )
    {
		if( mMessageListener != null )
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
}
