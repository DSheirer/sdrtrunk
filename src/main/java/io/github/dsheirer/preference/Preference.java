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

package io.github.dsheirer.preference;

import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;

/**
 * Preference base class with support for broadcasting preference changes
 */
public abstract class Preference
{
    public static final String PROPERTY_PREFIX = "user.preference.";
    private Listener<PreferenceType> mPreferenceUpdateListener;
    private SystemProperties mSystemProperties;

    public Preference(SystemProperties systemProperties, Listener<PreferenceType> updateListener)
    {
        mSystemProperties = systemProperties;
        mPreferenceUpdateListener = updateListener;
    }

    /**
     * System properties for storing/retrieving preference values
     */
    protected SystemProperties getSystemProperties()
    {
        return mSystemProperties;
    }

    /**
     * Indicates which type of preference for this class
     */
    public abstract PreferenceType getPreferenceType();

    /**
     * Notifies any registered listeners that a preference has been updated
     */
    public void notifyPreferenceUpdated()
    {
        if(mPreferenceUpdateListener != null)
        {
            mPreferenceUpdateListener.receive(getPreferenceType());
        }
    }
}
