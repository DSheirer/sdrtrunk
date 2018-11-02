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

package io.github.dsheirer.module.decode.ip.ipv4;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.Packet;
import io.github.dsheirer.module.decode.ip.PacketMessageFactory;
import io.github.dsheirer.module.decode.ip.UnknownPacket;

/**
 * IPV4 packet
 */
public class IPV4Packet extends Packet
{
    private IPV4Header mHeader;
    private IPacket mPayload;

    /**
     * Constructs an IPV4 packet parser
     *
     * @param message containing an IPV4 packet
     * @param offset to the start of the packet within the binary message
     */
    public IPV4Packet(BinaryMessage message, int offset)
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
            sb.append("IPV4 - INVALID HEADER");
        }

        if(hasPayload())
        {
            if(getPayload() instanceof UnknownPacket)
            {
                sb.append(" PROTOCOL:").append(getHeader().getProtocol());
            }

            sb.append(" ").append(getPayload().toString());
        }
        else
        {
            sb.append(" NO PAYLOAD!");
        }

        return sb.toString();
    }

    @Override
    public IPV4Header getHeader()
    {
        if(mHeader == null)
        {
            mHeader = new IPV4Header(getMessage(), getOffset());
        }

        return mHeader;
    }

    /**
     * Payload packet for this IP packet.
     *
     * @return payload packet or null
     */
    @Override
    public IPacket getPayload()
    {
        if(mPayload == null && getHeader().isValid())
        {
            if(getHeader().isValid())
            {
                mPayload = PacketMessageFactory.create(getHeader().getProtocol(), getMessage(),
                    getOffset() + getHeader().getLength());
            }
            else
            {
                mPayload = new UnknownPacket(getMessage(), getOffset());
            }
        }

        return mPayload;
    }
}
