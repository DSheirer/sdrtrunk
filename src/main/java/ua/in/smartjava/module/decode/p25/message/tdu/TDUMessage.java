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

package ua.in.smartjava.module.decode.p25.message.tdu;

import ua.in.smartjava.module.decode.p25.message.P25Message;
import ua.in.smartjava.module.decode.p25.reference.DataUnitID;
import ua.in.smartjava.alias.AliasList;
import ua.in.smartjava.bits.BinaryMessage;
import ua.in.smartjava.edac.CRC;

public class TDUMessage extends P25Message
{
	public TDUMessage( BinaryMessage message, DataUnitID duid,
            AliasList aliasList )
    {
	    super( message, duid, aliasList );

	    /* NID CRC is checked in the ua.in.smartjava.message framer, thus a constructed ua.in.smartjava.message
	     * means it passed the CRC */
	    mCRC = new CRC[ 1 ];
	    mCRC[ 0 ] = CRC.PASSED;
    }

	@Override
    public String getMessage()
    {
		StringBuilder sb = new StringBuilder();
		
		sb.append( "NAC:" );
		sb.append( getNAC() );
		sb.append( " " );
		sb.append( getDUID().getLabel() );
		
		sb.append( " TERMINATOR DATA UNIT" );
		
	    return sb.toString();
    }
}
