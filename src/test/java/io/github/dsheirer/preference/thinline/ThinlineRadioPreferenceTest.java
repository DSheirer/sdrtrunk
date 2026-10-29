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

package io.github.dsheirer.preference.thinline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.Test;

public class ThinlineRadioPreferenceTest
{
    @Test
    void newSetupStartsWithRemoteControlOff()
    {
        Preferences preferences = new MemoryPreferences();
        ThinlineRadioPreference.keepEarlierRemoteDefaults(preferences);

        for(ThinlineRadioPreference.Remote remote: ThinlineRadioPreference.Remote.values())
        {
            assertFalse(ThinlineRadioPreference.isRemoteAllowed(preferences, remote), remote.name());
        }
    }

    @Test
    void updateKeepsRemoteControlForAnExistingSite()
    {
        Preferences preferences = new MemoryPreferences();
        preferences.putBoolean("thinline.health.enabled", true);
        preferences.putBoolean("thinline.remote.desktop", false);

        ThinlineRadioPreference.keepEarlierRemoteDefaults(preferences);

        assertFalse(ThinlineRadioPreference.isRemoteAllowed(preferences, ThinlineRadioPreference.Remote.DESKTOP),
            "a permission the owner turned off stays off");
        assertTrue(ThinlineRadioPreference.isRemoteAllowed(preferences, ThinlineRadioPreference.Remote.TUNERS),
            "a permission that was on by default stays on");
    }

    @Test
    void enablingAfterTheUpdateDoesNotTurnRemoteControlOn()
    {
        Preferences preferences = new MemoryPreferences();
        ThinlineRadioPreference.keepEarlierRemoteDefaults(preferences);

        preferences.putBoolean("thinline.health.enabled", true);
        ThinlineRadioPreference.keepEarlierRemoteDefaults(preferences);

        for(ThinlineRadioPreference.Remote remote: ThinlineRadioPreference.Remote.values())
        {
            assertFalse(ThinlineRadioPreference.isRemoteAllowed(preferences, remote), remote.name());
        }
    }

    private static class MemoryPreferences extends AbstractPreferences
    {
        private final Map<String,String> mValues = new HashMap<>();

        MemoryPreferences()
        {
            super(null, "");
        }

        @Override
        protected void putSpi(String key, String value)
        {
            mValues.put(key, value);
        }

        @Override
        protected String getSpi(String key)
        {
            return mValues.get(key);
        }

        @Override
        protected void removeSpi(String key)
        {
            mValues.remove(key);
        }

        @Override
        protected void removeNodeSpi()
        {
        }

        @Override
        protected String[] keysSpi()
        {
            return mValues.keySet().toArray(new String[0]);
        }

        @Override
        protected String[] childrenNamesSpi()
        {
            return new String[0];
        }

        @Override
        protected AbstractPreferences childSpi(String name)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void syncSpi()
        {
        }

        @Override
        protected void flushSpi()
        {
        }
    }
}
