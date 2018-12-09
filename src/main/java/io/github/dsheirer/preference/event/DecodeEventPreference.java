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

package io.github.dsheirer.preference.event;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.TimestampFormat;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * User preferences for the display of channel decode events
 */
public class DecodeEventPreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(DecodeEventPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(DecodeEventPreference.class);
    private TimestampFormat mTimestampFormat = TimestampFormat.TIMESTAMP_DEFAULT;
    private static final String TIMESTAMP_FORMAT_KEY = "timestamp.format";

    public DecodeEventPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
        loadSettings();
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.DECODE_EVENT;
    }

    private void loadSettings()
    {
        String format = mPreferences.get(TIMESTAMP_FORMAT_KEY, TimestampFormat.TIMESTAMP_COLONS.name());

        if(format != null && !format.isEmpty())
        {
            try
            {
                mTimestampFormat = TimestampFormat.valueOf(format);
            }
            catch(Exception e)
            {
                mLog.error("Error loading decode event timestamp format [" + format + "]");
            }
        }
    }

    /**
     * Timestamp format to use for decode event timestamps
     */
    public TimestampFormat getTimestampFormat()
    {
        return mTimestampFormat;
    }

    /**
     * Updates the timestamp format preference
     *
     * @param timestampFormat
     */
    public void setTimestampFormat(TimestampFormat timestampFormat)
    {
        mTimestampFormat = timestampFormat;
        mPreferences.put(TIMESTAMP_FORMAT_KEY, mTimestampFormat.name());
        notifyPreferenceUpdated();
    }
}
