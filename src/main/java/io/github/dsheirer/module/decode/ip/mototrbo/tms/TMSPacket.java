/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.mototrbo.tms;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.Packet;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Text Messaging Service (TMS) message packet.
 */
public class TMSPacket extends Packet
{
    private TMSHeader mHeader;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public TMSPacket(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TEXT MESSAGE: ").append(getTextMessage());
        return sb.toString();
    }

    /**
     * Extracts the text message payload.
     */
    public String getTextMessage()
    {
        //Payload starts at packet offset, after 32-bit header
        int byteCount = (getMessage().length() - getOffset() - 32) / 8;

        if(byteCount > 0)
        {
            byte[] message = new byte[byteCount];

            for(int x = 0; x < byteCount; x++)
            {
                message[x] = getMessage().getByte(getOffset() + 32 + (x * 8));
            }

            //Characters are 16-bit, UTF-16 Little Endian
            return new String(message, StandardCharsets.UTF_16LE);
        }

        return "(ERROR-INSUFFICIENT BYTES)";
    }


    @Override
    public TMSHeader getHeader()
    {
        if(mHeader == null)
        {
            mHeader = new TMSHeader(getMessage(), getOffset());
        }

        return mHeader;
    }

    @Override
    public IPacket getPayload()
    {
        //There is no child payload.
        return null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        //Massage and Header don't have any additional identifiers.
        return Collections.emptyList();
    }
}
