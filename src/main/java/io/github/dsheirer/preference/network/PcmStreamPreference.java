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
 * User preferences for the decoded PCM audio stream feature.
 *
 * When enabled, SDRTrunk opens a TCP server on the configured port and streams
 * decoded 16-bit PCM audio from every active voice channel as newline-delimited
 * JSON (NDJSON).  Each line carries Base64-encoded little-endian int16 samples
 * at 8000 Hz mono, the talkgroup, the source radio unit, a unique call ID, and
 * a sequence number so consumers can detect dropped chunks.
 *
 * Unlike the IMBE stream on port 9502, this stream requires no JMBE library on
 * the client — the audio has already been decoded by SDRTrunk.
 */
public class PcmStreamPreference extends Preference
{
    private static final String KEY_ENABLED = "pcm.stream.enabled";
    private static final String KEY_PORT    = "pcm.stream.port";

    private static final boolean DEFAULT_ENABLED = false;
    private static final int     DEFAULT_PORT    = 9503;

    private final Preferences mPreferences = Preferences.userNodeForPackage(PcmStreamPreference.class);

    /**
     * Constructs an instance.
     * @param updateListener notified whenever preferences change
     */
    public PcmStreamPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.SOURCE_PCM_STREAM;
    }

    /** Returns true if the PCM audio stream server should be started at launch. */
    public boolean isEnabled()
    {
        return mPreferences.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }

    public void setEnabled(boolean enabled)
    {
        mPreferences.putBoolean(KEY_ENABLED, enabled);
        notifyPreferenceUpdated();
    }

    /** TCP port the PCM stream server listens on (default 9503). */
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
