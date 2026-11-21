/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.database.ageoff;

import java.util.concurrent.TimeUnit;

/**
 * How long duplicate and Do Not Monitor calls (and their recordings) are kept before they are deleted
 */
public enum RetentionPeriod
{
    MINUTES_30("30 minutes", TimeUnit.MINUTES.toMillis(30)),
    HOUR_1("1 hour", TimeUnit.HOURS.toMillis(1)),
    HOURS_2("2 hours", TimeUnit.HOURS.toMillis(2)),
    HOURS_4("4 hours", TimeUnit.HOURS.toMillis(4)),
    HOURS_8("8 hours", TimeUnit.HOURS.toMillis(8)),
    HOURS_12("12 hours", TimeUnit.HOURS.toMillis(12)),
    DAY_1("1 day", TimeUnit.DAYS.toMillis(1)),
    DAYS_2("2 days", TimeUnit.DAYS.toMillis(2)),
    DAYS_7("7 days", TimeUnit.DAYS.toMillis(7)),
    NEVER("Never delete", 0);

    public static final RetentionPeriod DEFAULT = HOUR_1;

    private final String mLabel;
    private final long mMilliseconds;

    RetentionPeriod(String label, long milliseconds)
    {
        mLabel = label;
        mMilliseconds = milliseconds;
    }

    /**
     * Retention time in milliseconds, or 0 for never
     */
    public long getMilliseconds()
    {
        return mMilliseconds;
    }

    /**
     * Indicates if calls are deleted after this period
     */
    public boolean isEnabled()
    {
        return this != NEVER;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
