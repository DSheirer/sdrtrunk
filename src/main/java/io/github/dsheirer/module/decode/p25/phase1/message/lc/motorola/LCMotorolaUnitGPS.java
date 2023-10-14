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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import java.util.Collections;
import java.util.List;

/**
 * Motorola Unit Self-Reported GPS Location
 */
public class LCMotorolaUnitGPS extends MotorolaLinkControlWord
{
    private static final double LATITUDE_MULTIPLIER = 90.0 / 0x7FFFFF;
    private static final double LONGITUDE_MULTIPLIER = 180.0 / 0x7FFFFF;

    private static final int[] UNKNOWN = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int LATITUDE_SIGN = 24;
    private static final int[] LATITUDE = {25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int LONGITUDE_SIGN = 48;
    private static final int[] LONGITUDE = {49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    /**
     * Constructs an instance
     * @param message binary
     */
    public LCMotorolaUnitGPS(BinaryMessage message)
    {
        super(message);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA UNIT GPS LOCATION: ").append(getLatitude()).append(" ").append(getLongitude());
        return sb.toString();
    }

    /**
     * GPS Latitude value.
     * @return value in decimal degrees
     */
    public double getLatitude()
    {
        return getMessage().getInt(LATITUDE) * LATITUDE_MULTIPLIER * (getMessage().get(LATITUDE_SIGN) ? -1 : 1);
    }

    /**
     * GPS Longitude value.
     * @return value in decimal degrees
     */
    public double getLongitude()
    {
        return getMessage().getInt(LONGITUDE) * LONGITUDE_MULTIPLIER * (getMessage().get(LONGITUDE_SIGN) ? -1 : 1);
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
