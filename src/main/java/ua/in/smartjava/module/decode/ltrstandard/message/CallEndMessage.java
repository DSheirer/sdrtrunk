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
package ua.in.smartjava.module.decode.ltrstandard.message;

import ua.in.smartjava.message.MessageDirection;
import ua.in.smartjava.message.MessageType;
import ua.in.smartjava.alias.Alias;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;
import ua.in.smartjava.edac.CRC;

public class CallEndMessage extends LTRStandardMessage
{
	public CallEndMessage( BinaryMessage message, MessageDirection direction, 
			AliasList list, CRC crc )
    {
    	super( message, direction, list, crc );
    }

	@Override
	public MessageType getMessageType()
	{
		return MessageType.CA_ENDD;
	}

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "END* AREA:" );
		sb.append( getArea() );
		sb.append( " LCN:" );
		sb.append( getChannelFormatted() );
		sb.append( " TG [" );
		sb.append( getToID() );
		sb.append( "/" );
		Alias endAlias = getToIDAlias();
		if( endAlias != null )
		{
			sb.append( getToIDAlias().getName() );
		}
		else
		{
			sb.append( "UNKNOWN" );
		}
		sb.append( "] FREE:" );
		sb.append( getFreeFormatted() );

		return sb.toString();
    }
}