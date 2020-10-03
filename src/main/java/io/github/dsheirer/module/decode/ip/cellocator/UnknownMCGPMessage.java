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

package io.github.dsheirer.module.decode.ip.cellocator;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.UnknownPacket;

/**
 * MCGP Packet - Unknown Message Type
 */
public class UnknownMCGPMessage extends MCGPPacket
{
    private IPacket mPayload;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param header for this message
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public UnknownMCGPMessage(MCGPHeader header, CorrectedBinaryMessage message, int offset)
    {
        super(header, message, offset);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CELLOCATOR UNKNOWN MESSAGE TYPE [");
        sb.append(getHeader().getMessageTypeValue()).append("] ").append(getPayload());
        return sb.toString();
    }

    @Override
    public IPacket getPayload()
    {
        if(mPayload == null)
        {
            mPayload = new UnknownPacket(getMessage(), getOffset());
        }

        return mPayload;
    }

    @Override
    protected CellocatorRadioIdentifier getRadioId()
    {
        return null;
    }
}
