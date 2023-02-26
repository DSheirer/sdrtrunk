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

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * LRRP Response Code Token
 * <p>
 * Start Token: 0x37
 * Total Length: 2 or 3 bytes
 */
public class Response extends Token
{
    private static final int CODE_LENGTH_FLAG = 8;
    private static final int[] ONE_BYTE_CODE = new int[]{9, 10, 11, 12, 13, 14, 15};
    private static final int[] TWO_BYTE_CODE = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};

    /**
     * Constructs an instance of a response code token.
     *
     * @param message containing the heading
     * @param offset to the start of the token
     */
    public Response(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.RESPONSE;
    }

    /**
     * Indicates if this is an extended (ie 2-byte) code.
     */
    private boolean isExtendedCode()
    {
        return getMessage().get(CODE_LENGTH_FLAG);
    }

    /**
     * Response code.  This value is either one byte or two bytes long.
     */
    public int getCodeValue()
    {
        if(isExtendedCode())
        {
            return getMessage().getInt(TWO_BYTE_CODE, getOffset());
        }
        else
        {
            return getMessage().getInt(ONE_BYTE_CODE, getOffset());
        }
    }

    /**
     * Response code
     */
    public ResponseCode getResponseCode()
    {
        return ResponseCode.fromValue(getCodeValue());
    }

    @Override
    public int getByteLength()
    {
        return isExtendedCode() ? 3 : 2;
    }

    @Override
    public String toString()
    {
        ResponseCode responseCode = getResponseCode();

        if(responseCode == ResponseCode.UNKNOWN)
        {
            return "RESPONSE: UNKNOWN[" + getCodeValue() + "]";
        }

        return "RESPONSE:" + responseCode;
    }
}
