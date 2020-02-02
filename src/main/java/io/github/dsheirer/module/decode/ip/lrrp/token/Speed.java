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
 * LRRP Speed Token
 *
 * Start Token: 0x6C
 * Total Length: 3 bytes
 */
public class Speed extends Token
{
    private static final int[] UNKNOWN = new int[]{8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] SPEED = new int[]{16, 17, 18, 19, 20, 21, 22, 23};

    /**
     * Constructs an instance of a heading token.
     *
     * @param message containing the heading
     * @param offset to the start of the token
     */
    public Speed(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.SPEED;
    }

    /**
     * Speed in kilometers per hour (kph)
     */
    public long getSpeed()
    {
        return getMessage().getInt(SPEED, getOffset());
    }

    @Override
    public String toString()
    {
        return "SPEED:" + getSpeed() + " KPH";
    }
}
