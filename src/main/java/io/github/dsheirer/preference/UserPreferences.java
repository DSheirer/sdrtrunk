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

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.event.DecodeEventPreference;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.sample.Listener;

/**
 * User Preferences.  A collection of preferences that can be accessed by preference type.
 */
public class UserPreferences implements Listener<PreferenceType>
{
    private DecodeEventPreference mDecodeEventPreference;
    private TalkgroupFormatPreference mTalkgroupFormatPreference;

    /**
     * Constructs a new user preferences instance
     */
    public UserPreferences()
    {
        loadPreferenceTypes();
    }

    /**
     * Identifier preferences
     */
    public TalkgroupFormatPreference getTalkgroupFormatPreference()
    {
        return mTalkgroupFormatPreference;
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
     */
    private void loadPreferenceTypes()
    {
        mDecodeEventPreference = new DecodeEventPreference(this);
        mTalkgroupFormatPreference = new TalkgroupFormatPreference(this);
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
        MyEventBus.getEventBus().post(preferenceType);
    }
}
