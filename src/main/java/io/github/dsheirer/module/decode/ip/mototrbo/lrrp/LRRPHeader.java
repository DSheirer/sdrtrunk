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

package io.github.dsheirer.module.decode.ip.mototrbo.lrrp;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.Header;
import java.util.Collections;
import java.util.List;

/**
 * Location Request/Response Protocol (LRRP) Header
 */
public class LRRPHeader extends Header
{
    private static final int[] TYPE = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] LENGTH = {8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public LRRPHeader(BinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Checks that the underlying message is long enough to contain the PDU
     */
    private void checkValid()
    {
        setValid(getMessage().size() >= (getOffset() + getLength() + getPayloadLength()));
    }

    /**
     * Integer value of the LRRP Packet type
     */
    public int getLrrpPacketTypeValue()
    {
        return getMessage().getInt(TYPE, getOffset());
    }

    /**
     * LRRP Packet Type
     */
    public LRRPPacketType getLRRPPacketType()
    {
        return LRRPPacketType.fromValue(getLrrpPacketTypeValue());
    }

    /**
     * Length of the payload in 8-bit characters
     *
     * @return
     */
    public int getPayloadLength()
    {
        return getMessage().getInt(LENGTH, getOffset());
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
        return 16;
    }

    /**
     * Identifiers for this packet
     */
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
