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
package io.github.dsheirer.module.decode.fleetsync2;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.alias.AliasList;

public class Fleetsync2MessageProcessor implements Listener<BinaryMessage>
{
	private Listener<Message> mMessageListener;
	
	private AliasList mAliasList;
	
	public Fleetsync2MessageProcessor( AliasList list )
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
		FleetsyncMessage message = new FleetsyncMessage( buffer, mAliasList );

		if( mMessageListener != null )
		{
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
