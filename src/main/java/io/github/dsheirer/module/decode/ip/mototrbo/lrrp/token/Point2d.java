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
 * LRRP Position Token
 *
 * Start Token: 0x66
 * Total Length: 9 bytes
 */
public class Point2d extends Token
{
    private static final int LATITUDE_HEMISPHERE_FLAG = 8;
    private static final int[] LATITUDE = new int[]{9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
        25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] LONGITUDE = new int[]{40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
        57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    private static final double LATITUDE_MULTIPLIER = 180.0 / 4294967295.0d;
    private static final double LONGITUDE_MULTIPLIER = 360.0 / 4294967295.0d;
    protected static final float HUNDREDTHS_MULTIPLIER = 0.01f;

    private LRRPPosition mPosition;

    /**
     * Constructs an instance of a position token.
     *
     * @param message containing the heading
     * @param offset to the start of the token
     */
    public Point2d(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public TokenType getTokenType()
    {
        return TokenType.POINT_2D;
    }

    /**
     * Position value
     */
    public LRRPPosition getPosition()
    {
        if(mPosition == null)
        {
            mPosition = LRRPPosition.createFrom(getLatitude(), getLongitude());
        }

        return mPosition;
    }

    /**
     * Latitude in degrees decimal
     *
     * Note: latitude field is parsed as a leading hemisphere indicator followed by a positive 31-bit integer.
     */
    public double getLatitude()
    {
        return getMessage().getInt(LATITUDE, getOffset()) * LATITUDE_MULTIPLIER *
                (getMessage().get(getOffset() + LATITUDE_HEMISPHERE_FLAG) ? -1 : 1);
    }

    /**
     * Longitude in degrees decimal
     *
     * Note: longitude field is parsed as a two's complement 32-bit integer.
     */
    public double getLongitude()
    {
        //Note: in testing against US data (positive lat, negative long), parsing the lat and long values as
        // two's complement values produced correct results.  However, testing against AUS data (negative lat,
        // positive long), the latitude hemisphere is incorrect.  So, I updated the code to treat latitude as a 31-bit
        // positive integer with a leading hemisphere flag bit and left the longitude parsing as two's complement.
        //
        // This same formula can't be applied against the longitude values, or they will be incorrect for US locations.
        // This may be a radio system configuration item that we have to expose in the future as a switch.  Will wait
        // until users report incorrect latitude parsing before we make any more changes.

        return getMessage().getInt(LONGITUDE, getOffset()) * LONGITUDE_MULTIPLIER;
    }

    @Override
    public String toString()
    {
        return "POINT:" + getPosition();
    }
}
