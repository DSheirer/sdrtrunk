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
package io.github.dsheirer.module.decode.p25.message.tdu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;

public class CallTermination extends TDULinkControlMessage
{
    public static final int[] SOURCE_ADDRESS = {160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 184, 185,
        186, 187, 188, 189, 190, 191, 192, 193, 194, 195};

    private IIdentifier mSourceAddress;

    public CallTermination(TDULinkControlMessage source)
    {
        super(source);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" BY:" + getSourceAddress());

        return sb.toString();
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }
}
