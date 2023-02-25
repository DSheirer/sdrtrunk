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
 * LRRP Unknown Request Type 61
 * <p>
 * Total Length: 2 bytes
 */
public class Request61 extends Token
{
    private static final int[] VALUE = new int[]{8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs an instance of a heading token.
     *
     * @param message containing the heading
     * @param offset to the start of the token
     */
    public Request61(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.REQUEST_61;
    }

    public int getValue()
    {
        return getMessage().getInt(VALUE, getOffset());
    }

    @Override
    public String toString()
    {
        return "REQUEST-61:" + getValue();
    }
}
