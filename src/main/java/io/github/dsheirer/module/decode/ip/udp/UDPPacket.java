/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.ip.udp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.Packet;
import io.github.dsheirer.module.decode.ip.PacketMessageFactory;
import io.github.dsheirer.module.decode.ip.UnknownPacket;

import java.util.ArrayList;
import java.util.List;

public class UDPPacket extends Packet
{
    private UDPHeader mHeader;
    private IPacket mPayload;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public UDPPacket(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(getHeader().isValid())
        {
            sb.append(getHeader());
        }
        else
        {
            sb.append("INVALID HEADER");
        }

        sb.append(" ").append(getPayload());

        return sb.toString();
    }

    @Override
    public UDPHeader getHeader()
    {
        if(mHeader == null)
        {
            mHeader = new UDPHeader(getMessage(), getOffset());
        }

        return mHeader;
    }

    @Override
    public IPacket getPayload()
    {
        if(mPayload == null && getHeader().isValid())
        {
            if(getHeader().isValid())
            {
                mPayload = PacketMessageFactory.createUDPPayload(getHeader().getSourcePort(),
                    getHeader().getDestinationPort(), getMessage(), getOffset() + getHeader().getLength());
            }
            else
            {
                //Set the offset to the message offset plus 64 bits for UDP header
                mPayload = new UnknownPacket(getMessage(), getOffset() + 64);
            }
        }

        return mPayload;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getHeader().getSourcePort());
            mIdentifiers.add(getHeader().getDestinationPort());

            //Roll up the identifiers from the payload packet
            if(hasPayload())
            {
                mIdentifiers.addAll(getPayload().getIdentifiers());
            }
        }

        return mIdentifiers;
    }
}
