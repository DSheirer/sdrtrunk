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
package io.github.dsheirer.preference.network;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;

/**
 * User preferences for the TCP network stream feature.
 */
public class NetworkStreamPreference extends Preference
{
    private static final String KEY_ENABLED = "network.stream.enabled";
    private static final String KEY_EVENT_PORT = "network.stream.event.port";
    private static final String KEY_RAW_PORT = "network.stream.raw.port";

    private static final boolean DEFAULT_ENABLED = false;
    private static final int DEFAULT_EVENT_PORT = 9500;
    private static final int DEFAULT_RAW_PORT = 9501;

    private final Preferences mPreferences = Preferences.userNodeForPackage(NetworkStreamPreference.class);

    /**
     * Constructs an instance.
     * @param updateListener notified whenever preferences change
     */
    public NetworkStreamPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.SOURCE_NETWORK_STREAM;
    }

    public boolean isEnabled()
    {
        return mPreferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }

    public void setEnabled(boolean enabled)
    {
        mPreferences.putBoolean(KEY_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    public int getEventPort()
    {
        return mPreferences.getInt(KEY_EVENT_PORT, DEFAULT_EVENT_PORT);
    }

    public void setEventPort(int port)
    {
        mPreferences.putInt(KEY_EVENT_PORT, port);
        notifyPreferenceUpdated();
    }

    public int getRawPort()
    {
        return mPreferences.getInt(KEY_RAW_PORT, DEFAULT_RAW_PORT);
    }

    public void setRawPort(int port)
    {
        mPreferences.putInt(KEY_RAW_PORT, port);
        notifyPreferenceUpdated();
    }
}
