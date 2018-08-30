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
package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

public class SecondaryControlChannelBroadcastExplicit extends SecondaryControlChannelBroadcast
{
    public SecondaryControlChannelBroadcastExplicit(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" SITE:" + getRFSS() + "-" + getSiteID());

        sb.append(" DOWNLINK:" + getChannel1());
        sb.append(" SVC1:" + SystemService.toString(getSystemServiceClass1()));

        if(hasChannel2())
        {
            sb.append(" UPLINK:" + getChannel2());
            sb.append(" SVC2:" + SystemService.toString(getSystemServiceClass2()));
        }

        return sb.toString();
    }
}
