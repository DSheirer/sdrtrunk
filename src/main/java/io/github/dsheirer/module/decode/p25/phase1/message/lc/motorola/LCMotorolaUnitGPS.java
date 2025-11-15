/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.identifier.P25Location;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.ArrayList;
import java.util.List;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * Motorola Unit Self-Reported GPS Location
 */
public class LCMotorolaUnitGPS extends LinkControlWord
{
    private static final double LATITUDE_MULTIPLIER = 90.0 / 0x7FFFFF;
    private static final double LONGITUDE_MULTIPLIER = 180.0 / 0x7FFFFF;
    private static final FragmentedIntField LATITUDE_TWOS_COMPLEMENT = FragmentedIntField.of(24, 25, 26, 27, 28,
            29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47);
    private static final FragmentedIntField LONGITUDE_TWOS_COMPLEMENT = FragmentedIntField.of(48, 49, 50, 51,
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71);
    private P25Location mLocation;
    private GeoPosition mGeoPosition;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     * @param message binary
     */
    public LCMotorolaUnitGPS(CorrectedBinaryMessage message)
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
     * GPS Location
     * @return location in decimal degrees
     */
    public P25Location getLocation()
    {
        if(mLocation == null)
        {
            mLocation = P25Location.createFrom(getLatitude(), getLongitude());
        }

        return mLocation;
    }

    /**
     * Geo position
     * @return position
     */
    public GeoPosition getGeoPosition()
    {
        if(mGeoPosition == null)
        {
            mGeoPosition = new GeoPosition(getLatitude(), getLongitude());
        }

        return mGeoPosition;
    }

    /**
     * GPS Latitude value.
     * @return value in decimal degrees
     */
    public double getLatitude()
    {
        return parse24BitInteger(getInt(LATITUDE_TWOS_COMPLEMENT)) * LATITUDE_MULTIPLIER;
    }

    /**
     * GPS Longitude value.
     * @return value in decimal degrees
     */
    public double getLongitude()
    {
        return parse24BitInteger(getInt(LONGITUDE_TWOS_COMPLEMENT)) * LONGITUDE_MULTIPLIER;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getLocation());
        }

        return mIdentifiers;
    }

    /**
     * Parses a 24-bit two-complement number to a signed integer.
     * @param value with 24 bits
     * @return signed integer value.
     */
    public static int parse24BitInteger(int value)
    {
        //If high-order bit is set, perform sign extension setting the 8x high order bits as a signed 32-bit integer
        if((value & 0x800000) == 0x800000)
        {
            value |= 0xFF000000;
        }

        return value;
    }
}
