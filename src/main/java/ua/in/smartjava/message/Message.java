/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
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
package ua.in.smartjava.message;

import java.util.Collections;
import java.util.List;

import ua.in.smartjava.map.Plottable;
import ua.in.smartjava.alias.Alias;

public abstract class Message
{
	protected long mTimeReceived;
	
	public Message()
	{
		mTimeReceived = System.currentTimeMillis();
	}

	public long getTimeReceived()
	{
		return mTimeReceived;
	}

	/**
	 * Map plottable
	 */
	public Plottable getPlottable()
	{
//TODO: move this to an IPlottable interface that only gets implemented as needed
		return null;
	}

	/**
	 * Decoded textual representation of the ua.in.smartjava.message
	 */
	public abstract String toString();
	
	/**
	 * Indicates if the ua.in.smartjava.message is valid and has passed crc/integrity checks
	 */
	public abstract boolean isValid();

	/**
	 * Status of the CRC check of the ua.in.smartjava.message
	 */
	public abstract String getErrorStatus();

	/**
	 * Parsed Message
	 * @return
	 */
	public abstract String getMessage();

	/**
	 * Raw ( 0 & 1 ) ua.in.smartjava.message ua.in.smartjava.bits
	 */
	public abstract String getBinaryMessage();
	

	/**
	 * Decoded protocol
	 */
	public abstract String getProtocol();
	
	/**
	 * Event - call, data, idle, etc.
	 */
	public abstract String getEventType();
	
	/**
	 * Formatted from identifier
	 */
	public abstract String getFromID();

	/**
	 * From identifier ua.in.smartjava.alias (from AliasManager)
	 */
	public abstract Alias getFromIDAlias();
	
	/**
	 * Formatted to identifier
	 */
	public abstract String getToID();
	
	/**
	 * To identifier ua.in.smartjava.alias (from AliasManager)
	 * @return
	 */
	public abstract Alias getToIDAlias();
	
	
	/**
	 * Provides a listing of aliases contained in the ua.in.smartjava.message.
	 */
	public List<Alias> getAliases()
	{
		return Collections.EMPTY_LIST;
	}
}