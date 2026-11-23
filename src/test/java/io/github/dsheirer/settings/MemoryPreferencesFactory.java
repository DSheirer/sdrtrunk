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

package io.github.dsheirer.settings;

import java.util.Map;
import java.util.TreeMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Preferences store that keeps everything in memory, selected for the tests with the java.util.prefs.PreferencesFactory
 * system property in the Gradle test task.  Tests then start from empty preferences in each test JVM and can't read
 * or change the user's settings, such as the vector calibrations that select DSP implementations, on any operating
 * system.
 */
public class MemoryPreferencesFactory implements PreferencesFactory
{
    private static final Preferences USER_ROOT = new MemoryPreferences(null, "");
    private static final Preferences SYSTEM_ROOT = new MemoryPreferences(null, "");

    @Override
    public Preferences userRoot()
    {
        return USER_ROOT;
    }

    @Override
    public Preferences systemRoot()
    {
        return SYSTEM_ROOT;
    }

    /**
     * Preferences node held in memory.  AbstractPreferences provides the locking, caching of child nodes, and events.
     */
    private static class MemoryPreferences extends AbstractPreferences
    {
        private final Map<String,String> mValues = new TreeMap<>();

        MemoryPreferences(AbstractPreferences parent, String name)
        {
            super(parent, name);
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
            mValues.clear();
        }

        @Override
        protected String[] keysSpi()
        {
            return mValues.keySet().toArray(new String[0]);
        }

        @Override
        protected String[] childrenNamesSpi()
        {
            //AbstractPreferences tracks the child nodes that it created, which are the only children in memory
            return new String[0];
        }

        @Override
        protected AbstractPreferences childSpi(String name)
        {
            return new MemoryPreferences(this, name);
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
