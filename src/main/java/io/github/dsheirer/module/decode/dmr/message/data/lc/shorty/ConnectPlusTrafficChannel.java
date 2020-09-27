/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.dmr.message.data.lc.shorty;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRNetwork;
import io.github.dsheirer.module.decode.dmr.identifier.DMRSite;

import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Connect Plus - Control Channel Information
 */
public class ConnectPlusTrafficChannel extends ShortLCMessage
{
    private static final int[] NETWORK = new int[]{4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] SITE = new int[]{16, 17, 18, 19, 20, 21, 22, 23};

    private DMRNetwork mNetwork;
    private DMRSite mSite;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     *
     * @param message containing the short link control message bits
     */
    public ConnectPlusTrafficChannel(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(!isValid())
        {
            sb.append("[CRC ERROR] ");
        }
        sb.append("SLC MOTOROLA CON+ TRAFFIC CHANNEL NETWORK:").append(getNetwork());
        sb.append(" SITE:").append(getSite());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * DMR Network Information
     */
    public DMRNetwork getNetwork()
    {
        if(mNetwork == null)
        {
            mNetwork = DMRNetwork.create(getMessage().getInt(NETWORK));
        }

        return mNetwork;
    }

    /**
     * DMR Site Information
     */
    public DMRSite getSite()
    {
        if(mSite == null)
        {
            mSite = DMRSite.create(getMessage().getInt(SITE));
        }

        return mSite;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getNetwork());
            mIdentifiers.add(getSite());
        }

        return mIdentifiers;
    }
}
