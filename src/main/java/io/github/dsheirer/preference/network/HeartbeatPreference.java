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
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * User preferences for heartbeat monitor entries.
 */
public class HeartbeatPreference extends Preference
{
    private static final String KEY_COUNT = "heartbeat.count";
    private static final String KEY_PREFIX = "heartbeat.";

    private final Preferences mPreferences = Preferences.userNodeForPackage(HeartbeatPreference.class);
    private List<HeartbeatEntry> mEntries;

    /**
     * Constructs an instance.
     * @param updateListener notified whenever preferences change
     */
    public HeartbeatPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
        mEntries = load();
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.SOURCE_HEARTBEAT;
    }

    /**
     * Returns the current list of heartbeat entries (defensive copy).
     */
    public List<HeartbeatEntry> getEntries()
    {
        return new ArrayList<>(mEntries);
    }

    /**
     * Adds a new entry.
     */
    public void addEntry(HeartbeatEntry entry)
    {
        mEntries.add(new HeartbeatEntry(entry));
        persist();
        notifyPreferenceUpdated();
    }

    /**
     * Updates the entry at the given index.
     */
    public void updateEntry(int index, HeartbeatEntry entry)
    {
        if(index >= 0 && index < mEntries.size())
        {
            mEntries.set(index, new HeartbeatEntry(entry));
            persist();
            notifyPreferenceUpdated();
        }
    }

    /**
     * Removes the entry at the given index.
     */
    public void removeEntry(int index)
    {
        if(index >= 0 && index < mEntries.size())
        {
            mEntries.remove(index);
            persist();
            notifyPreferenceUpdated();
        }
    }

    private List<HeartbeatEntry> load()
    {
        List<HeartbeatEntry> entries = new ArrayList<>();
        int count = mPreferences.getInt(KEY_COUNT, 0);

        for(int i = 0; i < count; i++)
        {
            HeartbeatEntry entry = new HeartbeatEntry();
            entry.setEnabled(mPreferences.getBoolean(KEY_PREFIX + i + ".enabled", true));
            entry.setChannelName(mPreferences.get(KEY_PREFIX + i + ".channelName", ""));
            entry.setSystemId(mPreferences.getInt(KEY_PREFIX + i + ".systemId", 0));
            entry.setSiteId(mPreferences.getInt(KEY_PREFIX + i + ".siteId", 0));
            entry.setKumaUrl(mPreferences.get(KEY_PREFIX + i + ".kumaUrl", ""));
            entry.setPushUrl2(mPreferences.get(KEY_PREFIX + i + ".pushUrl2", ""));
            entry.setIntervalSeconds(mPreferences.getInt(KEY_PREFIX + i + ".intervalSeconds", 30));
            entries.add(entry);
        }

        return entries;
    }

    private void persist()
    {
        int oldCount = mPreferences.getInt(KEY_COUNT, 0);
        mPreferences.putInt(KEY_COUNT, mEntries.size());

        for(int i = 0; i < mEntries.size(); i++)
        {
            HeartbeatEntry entry = mEntries.get(i);
            mPreferences.putBoolean(KEY_PREFIX + i + ".enabled", entry.isEnabled());
            mPreferences.put(KEY_PREFIX + i + ".channelName", entry.getChannelName());
            mPreferences.putInt(KEY_PREFIX + i + ".systemId", entry.getSystemId());
            mPreferences.putInt(KEY_PREFIX + i + ".siteId", entry.getSiteId());
            mPreferences.put(KEY_PREFIX + i + ".kumaUrl", entry.getKumaUrl());
            mPreferences.put(KEY_PREFIX + i + ".pushUrl2", entry.getPushUrl2());
            mPreferences.putInt(KEY_PREFIX + i + ".intervalSeconds", entry.getIntervalSeconds());
        }

        // Clean up stale keys beyond current list size
        for(int i = mEntries.size(); i < oldCount; i++)
        {
            mPreferences.remove(KEY_PREFIX + i + ".enabled");
            mPreferences.remove(KEY_PREFIX + i + ".channelName");
            mPreferences.remove(KEY_PREFIX + i + ".systemId");
            mPreferences.remove(KEY_PREFIX + i + ".siteId");
            mPreferences.remove(KEY_PREFIX + i + ".kumaUrl");
            mPreferences.remove(KEY_PREFIX + i + ".pushUrl2");
            mPreferences.remove(KEY_PREFIX + i + ".intervalSeconds");
        }
    }
}
