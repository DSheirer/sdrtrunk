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
package module.decode.tait;

import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import alias.AliasList;
import bits.BinaryMessage;

public class Tait1200GPSMessageProcessor implements Listener<BinaryMessage>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( Tait1200GPSMessageProcessor.class );

	private Listener<Message> mMessageListener;
	
	private AliasList mAliasList;
	
	public Tait1200GPSMessageProcessor( AliasList list )
	{
		mAliasList = list;
	}
	
	public void dispose()
	{
		mMessageListener = null;
		mAliasList = null;
	}
	
	@Override
    public void receive( BinaryMessage buffer )
    {
		if( mMessageListener != null )
		{
			mMessageListener.receive( new Tait1200GPSMessage( buffer, mAliasList ) );
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
