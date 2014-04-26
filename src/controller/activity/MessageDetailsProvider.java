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
package controller.activity;

import alias.Alias;

/**
 * Getter methods to support the Message Activity Table/Model
 * @author denny
 *
 */
public interface MessageDetailsProvider
{
	/**
	 * Indicates if the message is valid and has passed crc/integrity checks
	 */
	public abstract boolean isValid();

	/**
	 * Status of the CRC check of the message
	 */
	public abstract String getErrorStatus();

	/**
	 * Parsed Message
	 * @return
	 */
	public abstract String getMessage();

	/**
	 * Raw ( 0 & 1 ) message bits
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
	 * From identifier alias (from AliasManager)
	 */
	public abstract Alias getFromIDAlias();
	
	/**
	 * Formatted to identifier
	 */
	public abstract String getToID();
	
	/**
	 * To identifier alias (from AliasManager)
	 * @return
	 */
	public abstract Alias getToIDAlias();
	
}
