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
 * Total Length: 6 bytes
 */
public class Identity extends Token
{
    private static final int IDENTIFIER_START = 16;
    private static final int[] BYTE_VALUE = new int[]{0, 1, 2, 3, 4, 5, 6, 7};

    /**
     * Constructs an instance of a heading token.
     *
     * @param message containing the heading
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

    private BinaryMessage getTokenContents()
    {
        return getMessage().getSubMessage(getOffset(), getOffset() + 48);
    }

    /**
     * Extracts the payload contents.
     *
     * Note: assumes that the values are 1-4 bytes in big-endian format?  Not sure if this is correct because the
     * values that I observed only had value in the LSB and the 3 MSBs were all zeros.
     */
    public int getID()
    {
        int length = getVariableLengthPayloadSize();
        int payloadOffset = 16;
        int value = 0;

        while(0 < length && length <= 4)
        {
            int byteValue = getMessage().getInt(BYTE_VALUE, getOffset() + payloadOffset);

            byteValue = Integer.rotateLeft(byteValue, payloadOffset - 16);
            value += byteValue;
            payloadOffset += 8;
            length--;
        }

        return value;
    }


    @Override
    public String toString()
    {
        int id = getID();

        if(id != 0)
        {
            return "ID:" + id;
        }

        return "ID:" + getTokenContents().toHexString();
    }
}
