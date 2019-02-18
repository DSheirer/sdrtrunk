/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.preference;

import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.decoder.JmbeLibraryPreference;
import io.github.dsheirer.preference.directory.DirectoryPreference;
import io.github.dsheirer.preference.event.DecodeEventPreference;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.preference.playlist.PlaylistPreference;
import io.github.dsheirer.preference.radioreference.RadioReferencePreference;
import io.github.dsheirer.preference.source.ChannelMultiFrequencyPreference;
import io.github.dsheirer.preference.source.TunerPreference;
import io.github.dsheirer.preference.swing.SwingPreference;
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
    private JmbeLibraryPreference mJmbeLibraryPreference;
    private DecodeEventPreference mDecodeEventPreference;
    private DirectoryPreference mDirectoryPreference;
    private ChannelMultiFrequencyPreference mChannelMultiFrequencyPreference;
    private PlaylistPreference mPlaylistPreference;
    private RadioReferencePreference mRadioReferencePreference;
    private TalkgroupFormatPreference mTalkgroupFormatPreference;
    private TunerPreference mTunerPreference;
    private SwingPreference mSwingPreference = new SwingPreference();

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
     * Decoder preferences
     */
    public JmbeLibraryPreference getJmbeLibraryPreference()
    {
        return mJmbeLibraryPreference;
    }

    /**
     * Directory preferences
     */
    public DirectoryPreference getDirectoryPreference()
    {
        return mDirectoryPreference;
    }

    /**
     * Multiple frequency channel source preferences
     */
    public ChannelMultiFrequencyPreference getChannelMultiFrequencyPreference()
    {
        return mChannelMultiFrequencyPreference;
    }

    /**
     * Playlist preferences
     */
    public PlaylistPreference getPlaylistPreference()
    {
        return mPlaylistPreference;
    }

    /**
     * Radio reference web services preferences
     */
    public RadioReferencePreference getRadioReferencePreference()
    {
        return mRadioReferencePreference;
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
     * Swing window location/size user preferences
     */
    public SwingPreference getSwingPreference()
    {
        return mSwingPreference;
    }

    /**
     * Loads the managed preferences
     */
    private void loadPreferenceTypes()
    {
        mDecodeEventPreference = new DecodeEventPreference(this::receive);
        mJmbeLibraryPreference = new JmbeLibraryPreference(this::receive);
        mDirectoryPreference = new DirectoryPreference(this::receive);
        mChannelMultiFrequencyPreference = new ChannelMultiFrequencyPreference(this::receive);
        mPlaylistPreference = new PlaylistPreference(this::receive, mDirectoryPreference);
        mRadioReferencePreference = new RadioReferencePreference(this::receive);
        mTalkgroupFormatPreference = new TalkgroupFormatPreference(this::receive);
        mTunerPreference = new TunerPreference(this::receive);
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
