/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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
 * LRRP Trigger Periodic Token
 *
 * Total Length: 2 bytes
 */
public class TriggerPeriodic extends Token
{
    private static final int[] SECONDS = new int[]{8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs an instance of a heading token.
     *
     * @param message containing the heading
     * @param offset to the start of the token
     */
    public TriggerPeriodic(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.TRIGGER_DISTANCE;
    }

    /**
     * Seconds for trigger
     */
    public int getSeconds()
    {
        return getMessage().getInt(SECONDS, getOffset());
    }

    @Override
    public String toString()
    {
        return "TRIGGER EVERY:" + getSeconds() + " SECS";
    }
}
