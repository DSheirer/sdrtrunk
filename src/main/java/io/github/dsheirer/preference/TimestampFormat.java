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

package io.github.dsheirer.preference;

import java.text.SimpleDateFormat;

/**
 * Timestamp formats
 */
public enum TimestampFormat
{
    TIMESTAMP_DEFAULT("yyyy-MM-dd HH:mm:ss", "Timestamp: yyyy-MM-dd HH:mm:ss"),
    TIMESTAMP_COLONS("yyyy:MM:dd:HH:mm:ss", "Timestamp Colons: yyyy:MM:dd:HH:mm:ss"),
    TIMESTAMP_MILLIS("yyyy:MM:dd:HH:mm:ss.SSS", "Timestamp Milliseconds: yyyy:MM:dd:HH:mm:ss.SSS"),
    TIMESTAMP_COMPACT("yyyyMMdd_HHmmss", "Timestamp Compact: yyyyMMdd_HHmmss"),
    DATE_DASHES("yyyy-MM-dd", "Date Dashes: YYYY-MM-DD"),
    DATE_COLONS("yyyy:MM:dd", "Date Colons: YYYY:MM:DD"),
    DATE_COMPACT("yyyyMMdd", "Date Compact: YYYYMMDD"),
    TIME_COMPACT("HHmmss", "Time Compact: HHmmss"),
    TIME_COLONS("HH:mm:ss", "Time Colons: HH:mm:ss"),
    TIME_MILLIS("HH:mm:ss.SSS", "Time Milliseconds: HH:mm:ss.SSS"),
    TIME_SECONDS("ss.SSS", "Time Seconds: ss.SSS");

    private String mFormat;
    private String mLabel;

    /**
     * Constructs a timestamp format
     * @param format for the timestamp
     * @param label to describe the format
     */
    TimestampFormat(String format, String label)
    {
        mFormat = format;
        mLabel = label;
    }

    /**
     * Format string for the timestamp format
     */
    public String getFormat()
    {
        return mFormat;
    }

    /**
     * Creates a new Simple Date Formatter to use format string.
     */
    public SimpleDateFormat getFormatter()
    {
        return new SimpleDateFormat(mFormat);
    }

    /**
     * Overrides the toString() with a descriptive label
     */
    @Override
    public String toString()
    {
        return mLabel;
    }
}
