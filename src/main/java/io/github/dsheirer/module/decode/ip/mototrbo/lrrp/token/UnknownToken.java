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
 * Unknown or Unrecognized token.  This class is also used to capture the remaining character content of the token
 * string when parsing tokens from a LRRP packet, to account for the remaining token content.
 */
public class UnknownToken extends Token
{
    private int mCharacterLength;

    /**
     * Constructs an instance
     *
     * @param message containing the token and value
     * @param offset to the start of the token identifier
     */
    public UnknownToken(CorrectedBinaryMessage message, int offset, int characterLength)
    {
        super(message, offset);
        mCharacterLength = characterLength;
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.UNKNOWN;
    }

    private BinaryMessage getTokenContents()
    {
        return getMessage().getSubMessage(getOffset(), getOffset() + (mCharacterLength * 8));
    }

    @Override
    public String toString()
    {
        return "UNK:" + getTokenContents().toHexString();
    }
}
