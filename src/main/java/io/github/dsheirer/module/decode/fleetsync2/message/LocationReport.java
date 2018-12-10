/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.fleetsync2.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import org.jdesktop.swingx.mapviewer.GeoPosition;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Status message containing a numeric status indicator where the numeric value is pre-defined.
 */
public class LocationReport extends Fleetsync2Message
{
    //Message Block 3
    private static int[] GPS_HOURS = {172, 173, 174, 175, 176};
    private static int[] GPS_MINUTES = {177, 178, 179, 180, 181, 182};
    private static int[] GPS_SECONDS = {183, 184, 185, 186, 187, 188};

    //Message Block 4
    private static int[] GPS_CHECKSUM = {213, 214, 215, 216, 217, 218, 219, 220};
    private static int[] LATITUDE_DEGREES_MINUTES = {221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236};
    private static int[] LATITUDE_FRACTIONAL_MINUTES = {238, 239, 240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251};
    private static int[] SPEED = {252, 253, 254, 255, 256, 257, 258, 259, 260, 261, 262, 263, 264, 265, 266, 267, 268,
        269, 270, 271, 272, 273, 274, 275, 276};

    //Message Block 5
    private static int[] GPS_CENTURY = {277, 278, 279, 280, 281, 282, 283, 284};
    private static int[] GPS_YEAR = {284, 286, 287, 288, 289, 290, 291};
    private static int[] GPS_MONTH = {292, 293, 294, 295};
    private static int[] GPS_DAY = {296, 297, 298, 299, 300};
    private static int[] LONGITUDE_DEGREES_MINUTES = {301, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316};

    //TODO: this overlaps with the CRC block address space ...
    private static int[] LONGITUDE_FRACTIONAL_MINUTES = {318, 319, 320, 321, 322, 323, 324, 325, 326, 327, 328, 329, 330, 331};

    //Message Block 6
    private static int[] GPS_HEADING = {353, 354, 355, 356, 357, 358, 359, 360, 361, 362, 363, 364, 365};

    //Message Block 7

    //Message Block 8
    private static int[] GPS_SPEED = {484, 485, 486, 487, 488, 489, 490, 491};
    private static int[] GPS_SPEED_FRACTIONAL = {492, 493, 494, 495, 496, 497, 498, 499};

    private GeoPosition mGeoPosition;
    private Long mGPSTimestamp;
    private Double mGPSHeading;
    private Double mGPSSpeed;
    private List<Identifier> mIdentifers;

    public LocationReport(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    protected int getBlockCount()
    {
        return 8;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("GPS FROM:").append(getFromIdentifier());
        sb.append(" TO:").append(getToIdentifier());
        sb.append(" ").append(getLocation());
        sb.append(" HEADING:").append(getHeading());
        sb.append(" SPEED:").append(getSpeed());
        sb.append(" TIME:").append(new Date(getGPSTime()));

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifers == null)
        {
            mIdentifers = new ArrayList<>();
            mIdentifers.add(getFromIdentifier());
            mIdentifers.add(getToIdentifier());
        }

        return mIdentifers;
    }

    /**
     * GPS Heading in degrees relative to true North.
     */
    public double getHeading()
    {
        if(mGPSHeading == null)
        {
            if(hasHeading())
            {
                mGPSHeading = (double)getHeadingValue() / 10.0;
            }
            else
            {
                mGPSHeading = 0.0;
            }
        }

        return mGPSHeading;
    }

    private int getHeadingValue()
    {
        return getMessage().getInt(GPS_HEADING);
    }

    private boolean hasHeading()
    {
        return getHeadingValue() != 4095;
    }

    /**
     * Speed in kph
     */
    public double getSpeed()
    {
        if(mGPSSpeed == null)
        {
            mGPSSpeed = (double)getMessage().getInt(GPS_SPEED) + ((double)getMessage().getInt(GPS_SPEED_FRACTIONAL) / 255.0);
        }

        return mGPSSpeed;
    }

    /**
     * Latitude and Longitude location/position.
     */
    public GeoPosition getLocation()
    {
        if(mGeoPosition == null)
        {
            mGeoPosition = new GeoPosition(getLatitude(), getLongitude());
        }

        return mGeoPosition;
    }

    private double getLatitude()
    {
        //TODO: determine the correct hemisphere indicator and replace this
        return convertDDMToDD(0, getMessage().getInt(LATITUDE_DEGREES_MINUTES),
            getMessage().getInt(LATITUDE_FRACTIONAL_MINUTES));
    }

    private double getLongitude()
    {
        //TODO: determine the correct hemisphere indicator and replace this
        return convertDDMToDD(1, getMessage().getInt(LONGITUDE_DEGREES_MINUTES),
            getMessage().getInt(LONGITUDE_FRACTIONAL_MINUTES));
    }

    /**
     * Converts Degrees Decimal Minutes to Decimal Degrees
     *
     * Latitude and Longitude values are represented by:
     *
     * @param hemisphere - 0=North & East, 1=South & West
     * @param degreesMinutes - an integer value with the first 2-3 digits representing
     * the degrees and the last two digits representing the minutes
     * @param decimalDegrees - an integer value representing the fractional
     * minutes
     * @return - decimal degrees formatted value
     */
    public double convertDDMToDD(int hemisphere, int degreesMinutes, int decimalDegrees)
    {
        double retVal = 0.0;

        if(degreesMinutes != 0)
        {
            //Degrees - divide value by 100 and retain the whole number value (ie degrees)
            retVal += (double)(degreesMinutes / 100);

            //Minutes - modulus by 100 to get the whole minutes value
            int wholeMinutes = degreesMinutes % 100;

            if(wholeMinutes != 0)
            {
                retVal += (double)(wholeMinutes / 60.0D);
            }
        }

        if(decimalDegrees != 0)
        {
            //Fractional Minutes - divide by 10,000 to get the decimal place correct
            //then divide by 60 (minutes) to get the decimal value
            //10,000 * 60 = 600,000
            retVal += (double)(decimalDegrees / 600000.0D);
        }

        //Adjust the value +/- for the hemisphere
        if(hemisphere == 1) //South and West values
        {
            retVal = -retVal;
        }

        return retVal;
    }

    /**
     * GPS Timestamp in milliseconds
     */
    public long getGPSTime()
    {
        if(mGPSTimestamp == null)
        {
            Calendar calendar = new GregorianCalendar();
            calendar.add(Calendar.YEAR, 2000 + getMessage().getInt(GPS_YEAR));
            calendar.add(Calendar.MONTH, getMessage().getInt(GPS_MONTH));
            calendar.add(Calendar.DAY_OF_MONTH, getMessage().getInt(GPS_DAY));
            calendar.add(Calendar.HOUR_OF_DAY, getMessage().getInt(GPS_HOURS));
            calendar.add(Calendar.MINUTE, getMessage().getInt(GPS_MINUTES));
            calendar.add(Calendar.SECOND, getMessage().getInt(GPS_SECONDS));

            mGPSTimestamp = calendar.getTimeInMillis();
        }

        return mGPSTimestamp;
    }
}
