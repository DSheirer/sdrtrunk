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
package io.github.dsheirer.preference.decoder;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.prefs.Preferences;

/**
 * Preferences for allocating traffic channels on trunked systems.
 */
public class TrafficChannelPreference extends Preference
{
    private static final String PREFERENCE_KEY_IGNORE_UNALIASED_TALKGROUPS = "traffic.ignore.unaliased.talkgroups";
    private final Preferences mPreferences = Preferences.userNodeForPackage(TrafficChannelPreference.class);
    private Boolean mIgnoreUnaliasedTalkgroups;

    public TrafficChannelPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.TRAFFIC_CHANNEL;
    }

    /**
     * Indicates if talkgroup calls should be left untuned when the talkgroup has no alias in the channel's alias list.
     */
    public boolean isIgnoreUnaliasedTalkgroups()
    {
        if(mIgnoreUnaliasedTalkgroups == null)
        {
            mIgnoreUnaliasedTalkgroups = mPreferences.getBoolean(PREFERENCE_KEY_IGNORE_UNALIASED_TALKGROUPS, false);
        }

        return mIgnoreUnaliasedTalkgroups;
    }

    public void setIgnoreUnaliasedTalkgroups(boolean ignore)
    {
        mIgnoreUnaliasedTalkgroups = ignore;
        mPreferences.putBoolean(PREFERENCE_KEY_IGNORE_UNALIASED_TALKGROUPS, ignore);
        notifyPreferenceUpdated();
    }
}
