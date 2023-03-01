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
 * LRRP Approximate Position with latitude, longitude and error
 */
public class Circle2d extends Point2d
{
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private static final int[] RADIUS = new int[]{72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87};

    /**
     * Constructs an instance of an approximate position token.
     *
     * @param message containing the heading
     * @param offset to the start of the token
     */
    public Circle2d(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.CIRCLE_2D;
    }

    /**
     * Radius of position
     *
     * @return radius in meters
     */
    public float getRadius()
    {
        return getMessage().getInt(RADIUS, getOffset()) * HUNDREDTHS_MULTIPLIER;
    }

    @Override
    public String toString()
    {
        CorrectedBinaryMessage sub = getMessage().getSubMessage(getOffset(), getOffset() + 87);
        return "CIRCLE 2D POSITION:" + getPosition() + " RADIUS:" + DECIMAL_FORMAT.format(getRadius()) + " MTRS";
    }
}
