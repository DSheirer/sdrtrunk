/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.hytera.rrs;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.hytera.sds.DestinationId;
import io.github.dsheirer.module.decode.ip.hytera.sds.HyteraToken;
import io.github.dsheirer.module.decode.ip.hytera.sds.HyteraTokenHeader;
import io.github.dsheirer.module.decode.ip.hytera.sds.HyteraTokenType;
import io.github.dsheirer.module.decode.p25.identifier.ipv4.DMRIpAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Hytera SDS Long Data Message with SMS encoded payload
 */
public class HyteraRrsPacket implements IPacket
{
    private HyteraTokenHeader mHeader;
    private Identifier mDestination;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param header to the packet within the message
     */
    public HyteraRrsPacket(HyteraTokenHeader header)
    {
        mHeader = header;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("HYTERA RRS REGISTER");
        if(hasDestination())
        {
            sb.append(" RADIO:").append(getDestination());
        }

        return sb.toString();
    }

    @Override
    public HyteraTokenHeader getHeader()
    {
        return mHeader;
    }

    /**
     * Destination radio identifier for the SMS message
     */
    public Identifier getDestination()
    {
        if(mDestination == null)
        {
            HyteraToken destinationToken = getHeader().getTokenByType(HyteraTokenType.ID_DESTINATION);

            if(destinationToken instanceof DestinationId destinationId)
            {
                if(((DestinationId) destinationToken).isIp())
                {
                    mDestination = DMRIpAddress.createFrom(((DestinationId) destinationToken).getId());
                }
                else
                {
                    mDestination = DMRRadio.createTo(destinationId.getId());
                }
            }
        }

        return mDestination;
    }

    /**
     * Indicates if the SMS message has a destination identifier
     */
    public boolean hasDestination()
    {
        return getDestination() != null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(hasDestination())
            {
                mIdentifiers.add(getDestination());
            }
        }

        return mIdentifiers;
    }

    @Override
    public IPacket getPayload()
    {
        return null;
    }

    @Override
    public boolean hasPayload()
    {
        return false;
    }
}
