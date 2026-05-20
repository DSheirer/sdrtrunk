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
 * User preferences for the raw IMBE audio stream feature.
 *
 * When enabled, SDRTrunk opens a TCP server on the configured port and streams
 * every raw IMBE voice frame from every active P25 Phase 1 voice channel as
 * newline-delimited JSON (NDJSON).  Each line carries the 18-byte IMBE frame
 * (Base64-encoded), the talkgroup, the source radio unit, a unique call ID,
 * and a sequence number so consumers can detect dropped frames.
 */
public class ImbeStreamPreference extends Preference
{
    private static final String KEY_ENABLED = "imbe.stream.enabled";
    private static final String KEY_PORT    = "imbe.stream.port";

    private static final boolean DEFAULT_ENABLED = false;
    private static final int     DEFAULT_PORT    = 9502;

    private final Preferences mPreferences = Preferences.userNodeForPackage(ImbeStreamPreference.class);

    /**
     * Constructs an instance.
     * @param updateListener notified whenever preferences change
     */
    public ImbeStreamPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.SOURCE_IMBE_STREAM;
    }

    /** Returns true if the IMBE audio stream server should be started at launch. */
    public boolean isEnabled()
    {
        return mPreferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }

    public void setEnabled(boolean enabled)
    {
        mPreferences.putBoolean(KEY_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    /** TCP port the IMBE stream server listens on (default 9502). */
    public int getPort()
    {
        return mPreferences.getInt(KEY_PORT, DEFAULT_PORT);
    }

    public void setPort(int port)
    {
        mPreferences.putInt(KEY_PORT, port);
        notifyPreferenceUpdated();
    }
}
