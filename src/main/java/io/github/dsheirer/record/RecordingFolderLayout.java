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
package io.github.dsheirer.record;

/**
 * How call audio recordings are organized into subfolders of the recordings folder.  The date folder is the date the
 * call started (local time), named yyyy-MM-dd so that the folders sort by date.
 */
public enum RecordingFolderLayout
{
    NONE("No subfolders"),
    DATE("Date"),
    DATE_SYSTEM("Date / System"),
    SYSTEM_DATE("System / Date"),
    SYSTEM_SITE_DATE("System / Site / Date"),
    SYSTEM_CHANNEL_DATE("System / Channel / Date"),
    SYSTEM_TALKGROUP_DATE("System / Talkgroup or Radio / Date"),
    SYSTEM_DATE_TALKGROUP("System / Date / Talkgroup or Radio"),
    TALKGROUP_DATE("Talkgroup or Radio / Date");

    public static final RecordingFolderLayout DEFAULT = SYSTEM_DATE;

    private final String mLabel;

    RecordingFolderLayout(String label)
    {
        mLabel = label;
    }

    /**
     * Indicates if the layout has a date folder (that an hour folder can be added below)
     */
    public boolean hasDate()
    {
        return this != NONE;
    }

    /**
     * Indicates if the layout has a system folder, so the recording file name leaves out the system name
     */
    public boolean hasSystemFolder()
    {
        return this == DATE_SYSTEM || this == SYSTEM_DATE || this == SYSTEM_SITE_DATE || this == SYSTEM_CHANNEL_DATE ||
            this == SYSTEM_TALKGROUP_DATE || this == SYSTEM_DATE_TALKGROUP;
    }

    /**
     * Indicates if the layout has a site folder, so the recording file name leaves out the site name
     */
    public boolean hasSiteFolder()
    {
        return this == SYSTEM_SITE_DATE;
    }

    /**
     * Indicates if the layout has a channel folder, so the recording file name leaves out the channel name
     */
    public boolean hasChannelFolder()
    {
        return this == SYSTEM_CHANNEL_DATE;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
