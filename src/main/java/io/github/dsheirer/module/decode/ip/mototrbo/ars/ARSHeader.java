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

package io.github.dsheirer.module.decode.ip.mototrbo.ars;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.Header;
import java.util.List;

public abstract class ARSHeader extends Header
{
    private static final int[] LENGTH = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int HEADER_EXTENSION_FLAG = 16;
    private static final int ACKNOWLEDGEMENT_FLAG = 17;
    private static final int PRIORITY_FLAG = 18;
    private static final int CONTROL_USER_FLAG = 19;
    private static final int[] PDU_TYPE = {20, 21, 22, 23};

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public ARSHeader(BinaryMessage message, int offset)
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
     * Indicates the type of ARS PDU
     */
    public ARSPDUType getPDUType()
    {
        return getPDUType(getMessage(), getOffset());
    }

    /**
     * Indicates the type of ARS PDU contained in the message starting at the offset index.
     */
    public static ARSPDUType getPDUType(BinaryMessage message, int offset)
    {
        return ARSPDUType.fromValue(message.getInt(PDU_TYPE, offset));
    }

    /**
     * Indicates if this message has a header extension byte
     */
    public boolean hasHeaderExtension()
    {
        return getMessage().get(HEADER_EXTENSION_FLAG + getOffset());
    }

    /**
     * Indicates if this is an acknowledgement type PDU
     */
    public boolean isAcknowledge()
    {
        return getMessage().get(ACKNOWLEDGEMENT_FLAG + getOffset());
    }

    /**
     * Indicates if this PDU was sent with the priority flag
     */
    public boolean isPriority()
    {
        return getMessage().get(PRIORITY_FLAG + getOffset());
    }

    /**
     * Indicates if this packet is a control packet (true) or a user packet (false).
     */
    public boolean isControl()
    {
        return getMessage().get(CONTROL_USER_FLAG + getOffset());
    }

    /**
     * Length of the payload
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
        if(hasHeaderExtension())
        {
            return 16 + 16;
        }
        else
        {
            return 16 + 8;
        }
    }

    /**
     * Identifiers for this packet
     */
    public abstract List<Identifier> getIdentifiers();
}
