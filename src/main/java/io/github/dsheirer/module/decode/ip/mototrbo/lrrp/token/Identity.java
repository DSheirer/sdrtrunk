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

package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * LRRP Identity Token
 *
 * Start Token: 0x22
 * Total Length: 2 bytes plus a 1-4 byte request identifier
 */
public class Identity extends Token
{
    private static final int IDENTIFIER_START = 16;
    private static final int[] BYTE_VALUE = new int[]{0, 1, 2, 3, 4, 5, 6, 7};

    /**
     * Constructs an instance of an identity token.
     *
     * @param message containing the identity
     * @param offset to the start of the token
     */
    public Identity(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.IDENTITY;
    }

    /**
     * Request identifier bytes, as transmitted.  The read is clamped to the end of the message so that a corrupted
     * length field cannot produce bytes that were never received.
     */
    private BinaryMessage getIdentifierBytes()
    {
        int start = getOffset() + IDENTIFIER_START;
        int available = Math.max(0, getMessage().size() - start);
        int length = Math.min(getVariableLengthPayloadSize() * 8, available);
        return getMessage().getSubMessage(start, start + length);
    }

    /**
     * Request identifier assembled big-endian from the 1-4 payload bytes.  Moto.Net (LRRPPacket.cs) and node-dmr-lib
     * (LRRP.js) both read the identifier this way.
     *
     * @return identifier, or zero when the payload length is not 1-4 bytes.
     */
    public int getID()
    {
        int length = getVariableLengthPayloadSize();

        if(length < 1 || length > 4)
        {
            return 0;
        }

        int value = 0;

        for(int x = 0; x < length; x++)
        {
            value = (value << 8) | getMessage().getInt(BYTE_VALUE, getOffset() + IDENTIFIER_START + (x * 8));
        }

        return value;
    }

    @Override
    public String toString()
    {
        //Hexadecimal, as transmitted (e.g. ID:FFFFEE)
        return "ID:" + getIdentifierBytes().toHexString();
    }
}
