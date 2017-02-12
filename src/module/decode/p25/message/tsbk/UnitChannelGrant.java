/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package module.decode.p25.message.tsbk;

import alias.Alias;
import alias.AliasList;
import bits.BinaryMessage;
import module.decode.p25.reference.DataUnitID;

public abstract class UnitChannelGrant extends ChannelGrant
{
    public static final int[] TARGET_ADDRESS = {96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110,
        111, 112, 113, 114, 115, 116, 117, 118, 119};
    public static final int[] SOURCE_ADDRESS = {120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133,
        134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    public UnitChannelGrant(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        if(isEmergency())
        {
            sb.append(" EMERGENCY");
        }

        sb.append(" FROM:");
        sb.append(getSourceAddress());

        sb.append(" TO:");
        sb.append(getTargetAddress());

        return sb.toString();
    }

    public String getTargetAddress()
    {
        return mMessage.getHex(TARGET_ADDRESS, 6);
    }

    public String getSourceAddress()
    {
        return mMessage.getHex(SOURCE_ADDRESS, 6);
    }

    @Override
    public String getFromID()
    {
        return getSourceAddress();
    }

    @Override
    public Alias getFromIDAlias()
    {
        if(mAliasList != null)
        {
            return mAliasList.getTalkgroupAlias(getFromID());
        }

        return null;
    }

    @Override
    public String getToID()
    {
        return getTargetAddress();
    }
}
