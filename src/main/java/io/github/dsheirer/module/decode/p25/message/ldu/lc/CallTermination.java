/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;

public class CallTermination extends LDU1Message
{
	public static final int[] SOURCE_ADDRESS = { 720,721,722,723,724,725,730,731,732,733,734,735,740,741,742,743,744,745,750,751,752,753,754,755 };
	private IIdentifier mSource;

	public CallTermination( LDU1Message message )
	{
		super( message );
	}
	
	@Override
	public String getMessage()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append( getMessageStub() );
		
		sb.append( " FROM:" + getSourceAddress() );
		
		return sb.toString();
	}

    public IIdentifier getSourceAddress()
    {
        if(mSource == null)
        {
            mSource = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS));
        }

        return mSource;
    }
}
