/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeStamp
{
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    public static SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HHmmss");
    public static SimpleDateFormat TIME_WITH_MILLISECONDS_FORMAT = new SimpleDateFormat("HHmmss.SSS");
    public static SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
    public static SimpleDateFormat DATE_TIME_FORMAT_FILE = new SimpleDateFormat("yyyyMMdd_HHmmss");
    public static SimpleDateFormat DATE_TIME_MILLIS_FORMAT = new SimpleDateFormat("yyyyMMdd HHmmss.SSS");

    /**
     * Returns the current system date formatted as yyyy-MM-dd
     */
    public static synchronized String getFormattedDate()
    {
        return getFormattedDate(System.currentTimeMillis());
    }

    /**
     * Returns the timestamp formatted as a date of yyyy-MM-dd
     */
    public static synchronized String getFormattedDate(long timestamp)
    {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Date time formatted for use in a file.
     * @param timestamp to format
     * @return format string
     */
    public static String getFileFormattedDateTime(long timestamp)
    {
        return DATE_TIME_FORMAT_FILE.format(new Date(timestamp));
    }

    /**
     * Current date and time formatted for use in a file.
     * @return format string
     */
    public static String getFileFormattedDateTime()
    {
        return getFileFormattedDateTime(System.currentTimeMillis());
    }

    /**
     * Creates formatted date and timestamp
     * @param timestamp to format
     * @return formatted date and time
     */
    public static String getFormattedDateTime(long timestamp)
    {
        return DATE_TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Creates formatted date and timestamp using now as the timestamp.
     * @return formatted date and time
     */
    public static String getFormattedDateTime()
    {
        return getFormattedDateTime(System.currentTimeMillis());
    }

    /**
     * Returns the current system time formatted as HH:mm:ss
     */
    public static synchronized String getFormattedTime()
    {
        return getFormattedTime(System.currentTimeMillis());
    }

    /**
     * Returns the timestamp formatted as a time of HH:mm:ss
     */
    public static synchronized String getFormattedTime(long timestamp)
    {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Returns the timestamp formatted as a time of HH:mm:ss
     */
    public static synchronized String getFormattedTimeWithMilliseconds(long timestamp)
    {
        return TIME_WITH_MILLISECONDS_FORMAT.format(new Date(timestamp));
    }

    /**
     * Returns current system time formatted as yyyy-MM-dd*HH:mm:ss
     * with the * representing the separator attribute
     */
    public static synchronized String getTimeStamp(String separator)
    {
        return getTimeStamp(System.currentTimeMillis(), separator);
    }

    /**
     * Returns timestamp formatted as yyyy-MM-dd*HH:mm:ss
     * with the * representing the separator attribute
     */
    public static synchronized String getTimeStamp(long timestamp, String separator)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getFormattedDate(timestamp));
        sb.append(separator);
        sb.append(getFormattedTime(timestamp));

        return sb.toString();
    }

    /**
     * Returns current system time formatted as yyyy-MM-dd*HH:mm:ss.SSS
     * with the * representing the separator attribute
     */
    public static synchronized String getLongTimeStamp(String separator)
    {
        return getLongTimeStamp(System.currentTimeMillis(), separator);
    }

    /**
     * Returns timestamp formatted as yyyy-MM-dd*HH:mm:ss.SSS
     * with the * representing the separator attribute
     */
    public static synchronized String getLongTimeStamp(long timestamp, String separator)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getFormattedDate(timestamp));
        sb.append(separator);
        sb.append(getFormattedTimeWithMilliseconds(timestamp));

        return sb.toString();
    }

}

