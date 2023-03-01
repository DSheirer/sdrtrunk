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

package io.github.dsheirer.module.decode.ip.mototrbo.xcmp;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.Header;
import java.util.Collections;
import java.util.List;

/**
 * EXtensible Command Message Protocol (XCMP) Header
 */
public class XCMPHeader extends Header
{
    private static final int[] MESSAGE_TYPE = {0, 1, 2, 3, 4, 5, 6, 7};

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public XCMPHeader(BinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Identify the XCMP message type from the message
     */
    public static XCMPMessageType getMessageType(BinaryMessage message, int offset)
    {
        return XCMPMessageType.fromValue(message.getInt(MESSAGE_TYPE, offset));
    }

    /**
     * Identify the XCMP message type for this message
     */
    public XCMPMessageType getMessageType()
    {
        return getMessageType(getMessage(), getOffset());
    }

    /**
     * Message Type Value
     */
    public int getMessageTypeValue()
    {
        return getMessage().getInt(MESSAGE_TYPE, getOffset());
    }

    /**
     * Length of this header in bits
     *
     * Note this length value includes the first 2 bytes of the message that indicates the overall
     * message length and the length of the PDU header byte(s).
     *
     * This method may need to be overridden if there is more than 1 extension header in a PDU.
     */
    @Override
    public int getLength()
    {
        return 8;
    }

    /**
     * Identifiers for this packet
     */
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("XCMP");

        if(getMessageType() != XCMPMessageType.UNKNOWN)
        {
            sb.append(" ").append(getMessageType());
        }
        else
        {
            sb.append(" UNKNOWN FILE TYPE:").append(String.format("%02X", getMessageTypeValue()));
        }

        return sb.toString();
    }
}
