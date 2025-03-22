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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Utility class to parse L3Harris GPS data from P25 Phase 1 and Phase 2 messages.
 */
public class L3HarrisGPS
{
    public static final SimpleDateFormat SDF = new SimpleDateFormat("HH:mm:ss");
    static {
        SDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    private static final DecimalFormat GPS_FORMAT = new DecimalFormat("0.000000");

    //Bits 0 & 1 not set in sample data - seems unused for a 1/10000th of a minute number system
    private static final IntField LAT_MINUTES_FRACTIONAL = IntField.range(0, 15);
    private static final int LAT_HEMISPHERE = 16;
    //Bit 39 & 41 not set in sample data
    private static final IntField LAT_MINUTES = IntField.range(17, 23);
    private static final IntField LAT_DEGREES = IntField.range(24, 31);

    //Bits 56 & 57 not set in sample data - seems unused for a 1/5000th of a minute number system
    private static final IntField LONG_MINUTES_FRACTIONAL = IntField.range(32, 47);
    private static final int LONG_HEMISPHERE = 48;
    //Bit 71 & 73 not set in sample data
    private static final IntField LONG_MINUTES = IntField.range(49, 55);
    private static final IntField LONG_DEGREES = IntField.range(56, 63);

    //There's a leading bit missing from GPS Time to get from (2^16) to (2^17) needed address space (86,400 total seconds)
    private static final IntField GPS_TIME = IntField.range(64, 79);
    private static final int GPS_TIME_MSB = 80;

    //This may not be accurate.
    private static final IntField HEADING = IntField.range(95, 103);

    /**
     * Parses the latitude value from the message
     * @param message containing latitude value.
     * @param offset into the message to the start of the formatted GPS data.
     * @return latitude in degrees decimal.
     */
    public static double parseLatitude(CorrectedBinaryMessage message, int offset)
    {
        return parseCoordinate(message, offset, LAT_DEGREES, LAT_MINUTES, LAT_MINUTES_FRACTIONAL, LAT_HEMISPHERE);
    }

    /**
     * Parses the longitude value from the message
     * @param message containing longitude value.
     * @param offset into the message to the start of the formatted GPS data.
     * @return longitude in degrees decimal.
     */
    public static double parseLongitude(CorrectedBinaryMessage message, int offset)
    {
        return parseCoordinate(message, offset, LONG_DEGREES, LONG_MINUTES, LONG_MINUTES_FRACTIONAL, LONG_HEMISPHERE);
    }

    /**
     * Parses the latitude or longitude coordinate value from the message
     * @param message containing the coordinate
     * @param offset into the message to the start of the GPS data
     * @param degrees field definition
     * @param minutes field definition
     * @param fractional field definition
     * @param hemisphere field definition
     * @return value in degrees decimal.
     */
    private static double parseCoordinate(CorrectedBinaryMessage message, int offset, IntField degrees,
                                          IntField minutes, IntField fractional, int hemisphere)
    {
        double longitude = message.getInt(degrees, offset);
        longitude += ((message.getInt(minutes, offset) + (message.getInt(fractional, offset) / 10000.0)) / 60.0);
        longitude *= (message.get(hemisphere + offset) ? -1 : 1);
        return longitude;
    }

    /**
     * Parse the timestamp
     * @param message containing the timestamp
     * @param offset into the message to the start of the GPS data.
     * @return timestamp in milliseconds
     */
    public static long parseTimestamp(CorrectedBinaryMessage message, int offset)
    {
        long seconds = message.getInt(GPS_TIME, offset);

        if(message.get(offset + GPS_TIME_MSB))
        {
            seconds *= 2;
        }

        return seconds * 1000; //Convert seconds to milliseconds.
    }

    /**
     * Heading
     *
     * @return heading, 0-359 degrees.
     */
    public static int parseHeading(CorrectedBinaryMessage message, int offset)
    {
        return message.getInt(HEADING, offset);
    }
}
