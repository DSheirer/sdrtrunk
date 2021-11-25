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
 * LRRP Geo-Fence Position with latitude, longitude and radius
 */
public class GeoFencePosition extends Position
{
    private static final int[] RADIUS_WHOLE = new int[]{72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] RADIUS_FRACTIONAL = new int[]{80, 81, 82, 83, 84, 85, 86, 87};

    /**
     * Constructs an instance of an approximate position token.
     *
     * @param message containing the heading
     * @param offset to the start of the token
     */
    public GeoFencePosition(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.POSITION_GEO_FENCE;
    }

    /**
     * Radius of position
     *
     * @return radius in meters
     */
    public float getRadius()
    {
        return getFloat(RADIUS_WHOLE, RADIUS_FRACTIONAL);
    }

    @Override
    public String toString()
    {
        CorrectedBinaryMessage sub = getMessage().getSubMessage(getOffset(), getOffset() + 87);
        return "GEO-FENCE POSITION:" + getPosition() + " RADIUS:" + getRadius() + " KM";
    }
}
