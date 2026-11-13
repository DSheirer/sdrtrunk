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
 * LRRP Trigger Distance Token
 *
 * Start Token: 0x78
 * Total Length: 1 byte identifier plus a 7-bit variable length integer value (1-5 bytes)
 *
 * Distance in meters that the radio must move before it sends a triggered location report, e.g. 0x78 0x83 0x74 = 500.
 */
public class TriggerDistance extends VarIntToken
{
    /**
     * Constructs an instance of a trigger distance token.
     *
     * @param message containing the token
     * @param offset to the start of the token
     * @param remainingCharacterCount number of payload bytes remaining, counting from the token identifier
     */
    public TriggerDistance(CorrectedBinaryMessage message, int offset, int remainingCharacterCount)
    {
        super(message, offset, remainingCharacterCount);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.TRIGGER_DISTANCE;
    }

    /**
     * Distance for trigger
     */
    public int getDistance()
    {
        return getValue();
    }

    @Override
    public String toString()
    {
        return "TRIGGER DISTANCE:" + getDistance() + " METERS";
    }
}
