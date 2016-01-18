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
package module.decode.ltrstandard.message;

import message.MessageDirection;
import message.MessageType;
import alias.Alias;
import alias.AliasList;
import bits.BinaryMessage;
import edac.CRC;

public class CallMessage extends LTRStandardMessage
{
	public CallMessage( BinaryMessage message, MessageDirection direction, 
			AliasList list, CRC crc )
    {
    	super( message, direction, list, crc );
    }

	@Override
	public MessageType getMessageType()
	{
		return MessageType.CA_STRT;
	}

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();

		sb.append( "CALL LCN:" );
		sb.append( getChannelFormatted() );
		sb.append( " TG [ " );
		sb.append( getToID() );
		sb.append( "/" );
		
		Alias alias = getToIDAlias();
		
		if( alias != null )
		{
			sb.append( getToIDAlias().getName() );
		}
		else
		{
			sb.append( "UNKNOWN" );
		}
		
		sb.append( " ] FREE:" );
		sb.append( getFreeFormatted() );

		return sb.toString();
    }
}
