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
import java.text.DecimalFormat;

/**
 * LRRP Heading Token
 *
 * Start Token: 0x56
 * Total Length: 2 bytes
 */
public class Heading extends Token
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("000.0");
    private static final int[] HEADING = new int[]{8, 9, 10, 11, 12, 13, 14, 15};
    private static final float HEADING_MULTIPLIER = 2.0f;

    /**
     * Constructs an instance of a heading token.
     *
     * @param message containing the heading
     * @param offset to the start of the token
     */
    public Heading(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.HEADING;
    }

    /**
     * Heading degrees relative to true North
     */
    public float getHeading()
    {
        return getMessage().getInt(HEADING, getOffset()) * HEADING_MULTIPLIER;
    }

    @Override
    public String toString()
    {
        return "HEADING:" + DECIMAL_FORMAT.format(getHeading());
    }
}
