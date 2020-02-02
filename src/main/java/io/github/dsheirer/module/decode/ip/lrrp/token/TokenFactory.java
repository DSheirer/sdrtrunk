/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.ip.lrrp.token;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * LRRP Token Factory
 */
public class TokenFactory
{
    public static Token createToken(String id, CorrectedBinaryMessage message, int offset, int remainingCharacterCount)
    {
        TokenType tokenType = TokenType.fromValue(id);

        switch(tokenType)
        {
            case HEADING:
                return new Heading(message, offset);
            case POSITION:
                return new Position(message, offset);
            case POSITION_3D:
                return new Position3D(message, offset);
            case SPEED:
                return new Speed(message, offset);
            case TIMESTAMP:
                return new Timestamp(message, offset);
            case IDENTITY:
                return new Identity(message, offset);
            case UNKNOWN_23:
                return new Unknown23(message, offset);
            case UNKNOWN:
            default:
                return new UnknownToken(message, offset, remainingCharacterCount);
        }
    }
}
