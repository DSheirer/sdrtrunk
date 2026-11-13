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
 * LRRP Periodic Trigger Token (request packets)
 *
 * Start Token: 0x34 (in a response packet 0x34 is the Timestamp token)
 * Total Length: 1 byte
 *
 * Flags a triggered location request as periodic.  The reporting interval, when present, follows in a separate
 * Trigger Interval (0x31) token, and a Trigger Condition (0x4A) or Trigger Distance (0x78) token can follow instead.
 */
public class TriggerPeriodic extends Token
{
    /**
     * Constructs an instance of a periodic trigger token.
     *
     * @param message containing the token
     * @param offset to the start of the token
     */
    public TriggerPeriodic(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.TRIGGER_PERIODIC;
    }

    @Override
    public String toString()
    {
        return "PERIODIC";
    }
}
