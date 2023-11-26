/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.preference;

import io.github.dsheirer.preference.application.ApplicationPreference;
import io.github.dsheirer.preference.calibration.VectorCalibrationPreference;
import io.github.dsheirer.preference.decoder.JmbeLibraryPreference;
import io.github.dsheirer.preference.directory.DirectoryPreference;
import io.github.dsheirer.preference.duplicate.CallManagementPreference;
import io.github.dsheirer.preference.event.DecodeEventPreference;
import io.github.dsheirer.preference.identifier.TalkgroupFormatPreference;
import io.github.dsheirer.preference.javafx.JavaFxPreferences;
import io.github.dsheirer.preference.mp3.MP3Preference;
import io.github.dsheirer.preference.playback.PlaybackPreference;
import io.github.dsheirer.preference.playlist.PlaylistPreference;
import io.github.dsheirer.preference.radioreference.RadioReferencePreference;
import io.github.dsheirer.preference.record.RecordPreference;
import io.github.dsheirer.preference.source.ChannelMultiFrequencyPreference;
import io.github.dsheirer.preference.source.TunerPreference;
import io.github.dsheirer.preference.swing.SwingPreference;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
@Component("userPreferences")
public class UserPreferences implements Listener<PreferenceType>
{
    private Logger mLog = LoggerFactory.getLogger(UserPreferences.class);
    private ApplicationPreference mApplicationPreference;
    private ChannelMultiFrequencyPreference mChannelMultiFrequencyPreference;
    private DecodeEventPreference mDecodeEventPreference;
    private DirectoryPreference mDirectoryPreference;
    private CallManagementPreference mCallManagementPreference;
    private JavaFxPreferences mJavaFxPreferences = new JavaFxPreferences();
    private JmbeLibraryPreference mJmbeLibraryPreference;
    private MP3Preference mMP3Preference;
    private PlaybackPreference mPlaybackPreference;
    private PlaylistPreference mPlaylistPreference;
    private RadioReferencePreference mRadioReferencePreference;
    private RecordPreference mRecordPreference;
    private SwingPreference mSwingPreference = new SwingPreference();
    private TalkgroupFormatPreference mTalkgroupFormatPreference;
    private TunerPreference mTunerPreference;
    private VectorCalibrationPreference mVectorCalibrationPreference;
    private List<IPreferenceUpdateListener> mPreferenceUpdateListeners = new ArrayList<>();

    /**
     * Constructs a new user preferences instance
     */
    public UserPreferences()
    {
        loadPreferenceTypes();
        loadProperties();
    }

    //TODO: get rid of the system properties class and related operations
    /**
     * Loads the application properties file from the user's home directory,
     * creating the properties file for the first-time, if necessary
     */
    private void loadProperties()
    {
        Path propertiesPath = getDirectoryPreference().getDirectoryApplicationRoot().resolve("SDRTrunk.properties");

        if(!Files.exists(propertiesPath))
        {
            try
            {
                mLog.info("SDRTrunk - creating application properties file [" + propertiesPath.toAbsolutePath() + "]");
                Files.createFile(propertiesPath);
            }
            catch(IOException e)
            {
                mLog.error("SDRTrunk - couldn't create application properties file [" + propertiesPath.toAbsolutePath(), e);
            }
        }

        if(Files.exists(propertiesPath))
        {
            SystemProperties.getInstance().load(propertiesPath);
        }
        else
        {
            mLog.error("SDRTrunk - couldn't find or recreate the SDRTrunk application properties file");
        }
    }


    /**
     * Application general/miscellaneous preferences.
     * @return application preferences.
     */
    public ApplicationPreference getApplicationPreference()
    {
        return mApplicationPreference;
    }

    /**
     * Vector/SIMD calibration preferences.
     * @return vector calibration preferences.
     */
    public VectorCalibrationPreference getVectorCalibrationPreference()
    {
        return mVectorCalibrationPreference;
    }

    /**
     * Java FX window management preferences
     */
    public JavaFxPreferences getJavaFxPreferences()
    {
        return mJavaFxPreferences;
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
     * Audio playback preferences
     */
    public PlaybackPreference getPlaybackPreference()
    {
        return mPlaybackPreference;
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
     * Recording preferences
     */
    public RecordPreference getRecordPreference()
    {
        return mRecordPreference;
    }

    /**
     * MP3 preferences
     */
    public MP3Preference getMP3Preference()
    {
        return mMP3Preference;
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
     * Duplicate call detection preferences
     */
    public CallManagementPreference getCallManagementPreference()
    {
        return mCallManagementPreference;
    }

    /**
     * Loads the managed preferences
     */
    private void loadPreferenceTypes()
    {
        mApplicationPreference = new ApplicationPreference(this::receive);
        mChannelMultiFrequencyPreference = new ChannelMultiFrequencyPreference(this::receive);
        mDecodeEventPreference = new DecodeEventPreference(this::receive);
        mDirectoryPreference = new DirectoryPreference(this::receive);
        mCallManagementPreference = new CallManagementPreference(this::receive);
        mJmbeLibraryPreference = new JmbeLibraryPreference(this::receive);
        mMP3Preference = new MP3Preference(this::receive);
        mPlaybackPreference = new PlaybackPreference(this::receive);
        mPlaylistPreference = new PlaylistPreference(this::receive, mDirectoryPreference);
        mRadioReferencePreference = new RadioReferencePreference(this::receive);
        mRecordPreference = new RecordPreference(this::receive);
        mTalkgroupFormatPreference = new TalkgroupFormatPreference(this::receive);
        mTunerPreference = new TunerPreference(this::receive);
        mVectorCalibrationPreference = new VectorCalibrationPreference(this::receive);
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
        for(IPreferenceUpdateListener listener: mPreferenceUpdateListeners)
        {
            listener.preferenceUpdated(preferenceType);
        }
    }

    /**
     * Adds the listener to receive preference update notifications.
     * @param listener to add
     */
    public void addUpdateListener(IPreferenceUpdateListener listener)
    {
        if(listener != null && !mPreferenceUpdateListeners.contains(listener))
        {
            mPreferenceUpdateListeners.add(listener);
        }
    }

    /**
     * Removes the listener from receiving preference update notifications
     * @param listener to remove
     */
    public void removeUpdateListener(IPreferenceUpdateListener listener)
    {
        mPreferenceUpdateListeners.remove(listener);
    }
}
