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
import io.github.dsheirer.preference.directory.DirectoryPreference;
import io.github.dsheirer.preference.event.DecodeEventPreference;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.preference.playlist.PlaylistPreference;
import io.github.dsheirer.preference.tuner.TunerPreference;
import io.github.dsheirer.sample.Listener;

/**
 * User Preferences.  A collection of preferences that can be accessed by preference type.
 *
 * Note: user preference updates are broadcast throughout the system using the Google Guava Event Bus.  Each component
 * can register with the event bus and annotate a method to receive updates:
 *
 * To register a component to receive events, add this in the constructor:
 * MyEventBus.getEventBus().register(this);
 *
 * To receive preference update notifications, annotate a method with @Subscribe and use a PreferenceType argument:
 *
 * @Subscribe
 * public void preferenceUpdated(PreferenceType preferenceType)
 * {
 * }
 */
public class UserPreferences implements Listener<PreferenceType>
{
    private DecodeEventPreference mDecodeEventPreference;
    private DirectoryPreference mDirectoryPreference;
    private PlaylistPreference mPlaylistPreference;
    private TalkgroupFormatPreference mTalkgroupFormatPreference;
    private TunerPreference mTunerPreference;

    /**
     * Constructs a new user preferences instance
     */
    public UserPreferences()
    {
        loadPreferenceTypes();
    }

    /**
     * Decode Event preferences
     */
    public DecodeEventPreference getDecodeEventPreference()
    {
        return mDecodeEventPreference;
    }

    /**
     * Directory preferences
     */
    public DirectoryPreference getDirectoryPreference()
    {
        return mDirectoryPreference;
    }

    /**
     * Playlist preferences
     */
    public PlaylistPreference getPlaylistPreference()
    {
        return mPlaylistPreference;
    }

    /**
     * Identifier preferences
     */
    public TalkgroupFormatPreference getTalkgroupFormatPreference()
    {
        return mTalkgroupFormatPreference;
    }

    /**
     * Tuner preferences
     */
    public TunerPreference getTunerPreference()
    {
        return mTunerPreference;
    }

    /**
     * Loads the managed preferences
     */
    private void loadPreferenceTypes()
    {
        mDecodeEventPreference = new DecodeEventPreference(this);
        mDirectoryPreference = new DirectoryPreference(this::receive);
        mPlaylistPreference = new PlaylistPreference(this, mDirectoryPreference);
        mTalkgroupFormatPreference = new TalkgroupFormatPreference(this);
        mTunerPreference = new TunerPreference(this);
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
