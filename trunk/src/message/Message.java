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
package message;


import java.util.Collections;
import java.util.Date;
import java.util.List;

import map.Plottable;
import alias.Alias;
import controller.activity.MessageDetailsProvider;

public abstract class Message implements MessageDetailsProvider
{
	protected long mTimeReceived;
	protected MessageType mType;
	
	public Message()
	{
		this( MessageType.UN_KNWN );
	}

	public Message( MessageType type )
	{
		mTimeReceived = System.currentTimeMillis();
		mType = type;
	}
	
	public abstract Plottable getPlottable();

	public abstract String toString();
	
	public abstract boolean isValid();
	
	public long getTimeReceived()
	{
		return mTimeReceived;
	}
	
	public Date getDateReceived()
	{
		return new Date( mTimeReceived );
	}
	
	public MessageType getType()
	{
		return mType;
	}

	/**
	 * Provides a listing of aliases contained in the message.  
	 */
	public List<Alias> getAliases()
	{
		return Collections.EMPTY_LIST;
	}
}
