/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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

package io.github.dsheirer.preference.source;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * Preferences for multiple frequency tuner channel sources.
 */
public class ChannelMultiFrequencyPreference extends Preference
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelMultiFrequencyPreference.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(ChannelMultiFrequencyPreference.class);
    private static final String PREFERENCE_KEY_ROTATION_DELAY = "rotation.delay";
    private static final long DEFAULT_ROTATION_DELAY = 5000; //5 seconds
    private Long mRotationDelay;

    /**
     * Constructs a tuner preference with the update listener
     *
     * @param updateListener
     */
    public ChannelMultiFrequencyPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.MULTI_FREQUENCY;
    }

    /**
     * Rotation delay value in milliseconds.  This value is used for channel configurations where there source
     * defines multiple frequencies for a system that employs rotating control channels.
     */
    public long getRotationDelay()
    {
        if(mRotationDelay == null)
        {
            mRotationDelay = mPreferences.getLong(PREFERENCE_KEY_ROTATION_DELAY, DEFAULT_ROTATION_DELAY);
        }

        return mRotationDelay;
    }

    /**
     * Sets the rotation delay value in milliseconds.
     * @param rotationDelay in milliseconds (1,000 - 60,000)
     */
    public void setRotationDelay(long rotationDelay)
    {
        if(1000 <= rotationDelay && rotationDelay <= 60000)
        {
            mRotationDelay = rotationDelay;
            mPreferences.putLong(PREFERENCE_KEY_ROTATION_DELAY, mRotationDelay);
            notifyPreferenceUpdated();
        }
    }

    /**
     * Resets the rotation delay and removes it from the preferences store, so that it can be recreated with
     * a default value.
     */
    public void resetRotationDelay()
    {
        mPreferences.remove(PREFERENCE_KEY_ROTATION_DELAY);
        mRotationDelay = null;
        notifyPreferenceUpdated();
    }
}
