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

import io.github.dsheirer.preference.event.DecodeEventPreference;
import io.github.dsheirer.preference.identifier.IdentifierPreference;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;

/**
 * User Preferences.  A collection of preferences that can be accessed by preference type.
 */
public class UserPreferences implements Listener<PreferenceType>
{
    private Broadcaster<PreferenceType> mPreferenceTypeBroadcaster = new Broadcaster<>();
    private DecodeEventPreference mDecodeEventPreference;
    private IdentifierPreference mIdentifierPreference;

    /**
     * Constructs a new user preferences instance
     *
     * @param systemProperties for loading/storing user preferences
     */
    public UserPreferences(SystemProperties systemProperties)
    {
        loadPreferenceTypes(systemProperties);
    }

    /**
     * Identifier preferences
     */
    public IdentifierPreference getIdentifierPreference()
    {
        return mIdentifierPreference;
    }

    /**
     * Decode Event preferences
     */
    public DecodeEventPreference getDecodeEventPreference()
    {
        return mDecodeEventPreference;
    }

    /**
     * Loads the managed preferences
     *
     * @param systemProperties
     */
    private void loadPreferenceTypes(SystemProperties systemProperties)
    {
        mDecodeEventPreference = new DecodeEventPreference(systemProperties, this);
        mIdentifierPreference = new IdentifierPreference(systemProperties, this);
    }

    /**
     * Adds a listener to be notified when a preference has been updated
     *
     * @param listener to receive preference type notifications.
     */
    public void addPreferenceUpdateListener(Listener<PreferenceType> listener)
    {
        mPreferenceTypeBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving preference update notifications
     */
    public void removePreferenceUpdateListener(Listener<PreferenceType> listener)
    {
        mPreferenceTypeBroadcaster.removeListener(listener);
    }

    /**
     * Primary method for receiving notification from a managed preference that a preference type
     * has been changed/updated.
     *
     * @param preferenceType that is updated
     */
    @Override
    public void receive(PreferenceType preferenceType)
    {
        mPreferenceTypeBroadcaster.broadcast(preferenceType);
    }
}
