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

package io.github.dsheirer.settings.event;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.TimestampFormat;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Settings for the display of channel decode events
 */
public class DecodeEventSettings extends AbstractSettings
{
    public static final TimestampFormat DEFAULT_TIMESTAMP_FORMAT = TimestampFormat.TIMESTAMP_COLONS;
    private final static Logger mLog = LoggerFactory.getLogger(DecodeEventSettings.class);
    private TimestampFormat mTimestampFormat = TimestampFormat.TIMESTAMP_DEFAULT;

    public DecodeEventSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
        loadSettings();
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.DECODE_EVENT;
    }

    private void loadSettings()
    {
        String format = Setting.DECODE_EVENT_TIMESTAMP_FORMAT.get(DEFAULT_TIMESTAMP_FORMAT.name());

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
     * Updates the timestamp format setting
     *
     * @param timestampFormat
     */
    public void setTimestampFormat(TimestampFormat timestampFormat)
    {
        mTimestampFormat = timestampFormat;
        Setting.DECODE_EVENT_TIMESTAMP_FORMAT.put(mTimestampFormat.name());
        notifySettingsUpdated();
    }
}
