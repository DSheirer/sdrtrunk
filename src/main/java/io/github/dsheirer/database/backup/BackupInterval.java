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
package io.github.dsheirer.database.backup;

import java.util.concurrent.TimeUnit;

/**
 * How often the call database is backed up automatically
 */
public enum BackupInterval
{
    NEVER("Never (manual only)", 0),
    HOURS_6("Every 6 hours", TimeUnit.HOURS.toMillis(6)),
    HOURS_12("Every 12 hours", TimeUnit.HOURS.toMillis(12)),
    DAY_1("Daily", TimeUnit.DAYS.toMillis(1)),
    DAYS_2("Every 2 days", TimeUnit.DAYS.toMillis(2)),
    WEEK_1("Weekly", TimeUnit.DAYS.toMillis(7)),
    MONTH_1("Monthly (30 days)", TimeUnit.DAYS.toMillis(30));

    public static final BackupInterval DEFAULT = DAY_1;

    private final String mLabel;
    private final long mMilliseconds;

    BackupInterval(String label, long milliseconds)
    {
        mLabel = label;
        mMilliseconds = milliseconds;
    }

    /**
     * Time between automatic backups in milliseconds, or 0 when automatic backups are off
     */
    public long getMilliseconds()
    {
        return mMilliseconds;
    }

    /**
     * Indicates if automatic backups are enabled
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
