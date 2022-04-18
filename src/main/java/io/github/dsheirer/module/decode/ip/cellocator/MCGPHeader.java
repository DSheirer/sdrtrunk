/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.ip.Header;

/**
 * Cellocator MCGP Header
 */
public class MCGPHeader extends Header
{
    private static final int[] M_CHARACTER = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] C_CHARACTER = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] G_CHARACTER = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] P_CHARACTER = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int M_VALUE = 0x4D;
    private static final int C_VALUE = 0x43;
    private static final int G_VALUE = 0x47;
    private static final int P_VALUE = 0x50;

    private static final int[] MESSAGE_TYPE = {32, 33, 34, 35, 36, 37, 38, 39};
    public static final int HEADER_LENGTH = 40;

    private MCGPMessageType mMessageType;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the header
     * @param offset to the header within the message
     */
    public MCGPHeader(BinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Performs simple check of the first 32 bits of the message looking for the
     * signature 'MCGP' header for cellocator messages.
     * @param message to inspect
     * @param offset to the cellocator payload
     * @return true if the payload starts with MCGP
     */
    public static boolean isCellocatorMessage(BinaryMessage message, int offset)
    {
        return message.getInt(M_CHARACTER, offset) == M_VALUE &&
               message.getInt(C_CHARACTER, offset) == C_VALUE &&
               message.getInt(G_CHARACTER, offset) == G_VALUE &&
               message.getInt(P_CHARACTER, offset) == P_VALUE;
    }

    @Override
    public int getLength()
    {
        return HEADER_LENGTH;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CELLOCATOR ").append(getMessageType());

        if(getMessageType() == MCGPMessageType.UNKNOWN)
        {
            sb.append(" PACKET:").append(getMessage().toHexString());
        }

        return sb.toString();
    }

    /**
     * Indicates the type of message
     */
    public MCGPMessageType getMessageType()
    {
        if(mMessageType == null)
        {
            mMessageType = getMessageType(getMessage(), getOffset());
        }

        return mMessageType;
    }

    /**
     * Sets/Overrides the message type
     */
    void setMessageType(MCGPMessageType type)
    {
        mMessageType = type;
    }

    /**
     * Indicates the type of MCGP message contained in the message starting at the offset index.
     */
    public static MCGPMessageType getMessageType(BinaryMessage message, int offset)
    {
        int payloadLengthBytes = (message.size() - offset) / 8;
        return MCGPMessageType.fromValue(message.getInt(MESSAGE_TYPE, offset), payloadLengthBytes);
    }

    /**
     * Numeric value for the message type for this header
     */
    public int getMessageTypeValue()
    {
        return getMessage().getInt(MESSAGE_TYPE, getOffset());
    }
}
