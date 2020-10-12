/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp.SNDCPPacketHeader;
import java.util.Collections;
import java.util.List;

import java.util.Collections;
import java.util.List;

public class UnknownCompressedHeaderPacket implements IPacket
{
    private SNDCPPacketHeader mSNDCPPacketHeader;
    private BinaryMessage mMessage;
    private int mOffset;

    public UnknownCompressedHeaderPacket(SNDCPPacketHeader SNDCPPacketHeader, BinaryMessage message, int offset)
    {
        mSNDCPPacketHeader = SNDCPPacketHeader;
        mMessage = message;
        mOffset = offset;
    }

    public SNDCPPacketHeader getSNDCPHeader()
    {
        return mSNDCPPacketHeader;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getSNDCPHeader());
        sb.append(" PACKET:");

        if(getMessage().size() > getOffset())
        {
            sb.append(mMessage.getSubMessage(getOffset(), getMessage().size()).toHexString());
        }
        else
        {
            sb.append("(EMPTY)");
        }

        return sb.toString();
    }

    public BinaryMessage getMessage()
    {
        return mMessage;
    }

    public int getOffset()
    {
        return mOffset;
    }

    @Override
    public IHeader getHeader()
    {
        return null;
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

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
