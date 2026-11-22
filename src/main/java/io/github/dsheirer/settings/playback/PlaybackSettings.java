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

package io.github.dsheirer.settings.playback;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.audio.playback.AudioPlaybackDeviceDescriptor;
import io.github.dsheirer.audio.playback.AudioPlaybackDeviceManager;
import io.github.dsheirer.gui.settings.playback.ToneFrequency;
import io.github.dsheirer.gui.settings.playback.ToneUtil;
import io.github.dsheirer.gui.settings.playback.ToneVolume;
import io.github.dsheirer.settings.AbstractSettings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Settings for audio playback
 */
public class PlaybackSettings extends AbstractSettings
{
    public static final boolean DEFAULT_USE_DROP_TONE = true;
    public static final boolean DEFAULT_USE_START_TONE = true;
    public static final ToneFrequency DEFAULT_DROP_TONE_FREQUENCY = ToneFrequency.F500;
    public static final ToneFrequency DEFAULT_START_TONE_FREQUENCY = ToneFrequency.F700;
    public static final ToneVolume DEFAULT_DROP_TONE_VOLUME = ToneVolume.V3;
    public static final ToneVolume DEFAULT_START_TONE_VOLUME = ToneVolume.V3;


    public static final int TONE_LENGTH_SAMPLES = 160;

    private final static Logger mLog = LoggerFactory.getLogger(PlaybackSettings.class);
    private Boolean mUseAudioSegmentStartTone;
    private Boolean mUseAudioSegmentDropTone;
    private ToneFrequency mStartToneFrequency;
    private ToneVolume mStartToneVolume;
    private ToneFrequency mDropToneFrequency;
    private ToneVolume mDropToneVolume;
    private AudioPlaybackDeviceDescriptor mAudioPlaybackDeviceDescriptor;

    /**
     * Constructs this setting with an update listener
     * @param updateListener to receive notifications whenever these settings change
     */
    public PlaybackSettings(Listener<SettingsType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public SettingsType getSettingsType()
    {
        return SettingsType.PLAYBACK;
    }

    /**
     * Indicates if an audio segment drop tone should be used.
     */
    public boolean getUseAudioSegmentDropTone()
    {
        if(mUseAudioSegmentDropTone == null)
        {
            mUseAudioSegmentDropTone = Setting.PLAYBACK_USE_DROP_TONE.getBoolean(DEFAULT_USE_DROP_TONE);
        }

        return mUseAudioSegmentDropTone;
    }

    /**
     * Sets the setting for using an audio segment drop tone
     */
    public void setUseAudioSegmentDropTone(boolean use)
    {
        mUseAudioSegmentDropTone = use;
        Setting.PLAYBACK_USE_DROP_TONE.putBoolean(use);
        notifySettingsUpdated();
    }

    /**
     * Indicates if an audio segment start tone should be used.
     */
    public boolean getUseAudioSegmentStartTone()
    {
        if(mUseAudioSegmentStartTone == null)
        {
            mUseAudioSegmentStartTone = Setting.PLAYBACK_USE_START_TONE.getBoolean(DEFAULT_USE_START_TONE);
        }

        return mUseAudioSegmentStartTone;
    }

    /**
     * Sets the setting for using an audio segment start tone
     */
    public void setUseAudioSegmentStartTone(boolean use)
    {
        mUseAudioSegmentStartTone = use;
        Setting.PLAYBACK_USE_START_TONE.putBoolean(use);
        notifySettingsUpdated();
    }

    /**
     * Frequency for the drop tone
     */
    public ToneFrequency getDropToneFrequency()
    {
        if(mDropToneFrequency == null)
        {
            int frequency = Setting.PLAYBACK_DROP_TONE_FREQUENCY.getInt(DEFAULT_DROP_TONE_FREQUENCY.getValue());
            mDropToneFrequency = ToneFrequency.fromValue(frequency);
        }

        return mDropToneFrequency;
    }

    /**
     * Sets the frequency for the drop tone
     */
    public void setDropToneFrequency(ToneFrequency toneFrequency)
    {
        mDropToneFrequency = toneFrequency;
        Setting.PLAYBACK_DROP_TONE_FREQUENCY.putInt(toneFrequency.getValue());
        notifySettingsUpdated();
    }

    /**
     * Frequency for the start tone
     */
    public ToneFrequency getStartToneFrequency()
    {
        if(mStartToneFrequency == null)
        {
            int frequency = Setting.PLAYBACK_START_TONE_FREQUENCY.getInt(DEFAULT_START_TONE_FREQUENCY.getValue());
            mStartToneFrequency = ToneFrequency.fromValue(frequency);
        }

        return mStartToneFrequency;
    }

    /**
     * Sets the frequency for the start tone
     */
    public void setStartToneFrequency(ToneFrequency toneFrequency)
    {
        mStartToneFrequency = toneFrequency;
        Setting.PLAYBACK_START_TONE_FREQUENCY.putInt(toneFrequency.getValue());
        notifySettingsUpdated();
    }

    /**
     * Drop tone volume
     */
    public ToneVolume getDropToneVolume()
    {
        if(mDropToneVolume == null)
        {
            int volume = Setting.PLAYBACK_DROP_TONE_VOLUME.getInt(DEFAULT_DROP_TONE_VOLUME.getValue());
            mDropToneVolume = ToneVolume.fromValue(volume);
        }

        return mDropToneVolume;
    }

    /**
     * Sets the drop tone volume
     */
    public void setDropToneVolume(ToneVolume toneVolume)
    {
        mDropToneVolume = toneVolume;
        Setting.PLAYBACK_DROP_TONE_VOLUME.putInt(toneVolume.getValue());
        notifySettingsUpdated();
    }

    /**
     * Start tone volume
     */
    public ToneVolume getStartToneVolume()
    {
        if(mStartToneVolume == null)
        {
            int volume = Setting.PLAYBACK_START_TONE_VOLUME.getInt(DEFAULT_START_TONE_VOLUME.getValue());
            mStartToneVolume = ToneVolume.fromValue(volume);
        }

        return mStartToneVolume;
    }

    /**
     * Sets the start tone volume
     */
    public void setStartToneVolume(ToneVolume toneVolume)
    {
        mStartToneVolume = toneVolume;
        Setting.PLAYBACK_START_TONE_VOLUME.putInt(toneVolume.getValue());
        notifySettingsUpdated();
    }

    /**
     * Buffer with samples for the audio segment start tone
     */
    public float[] getStartTone()
    {
        if(getUseAudioSegmentStartTone())
        {
            return getStartTone(TONE_LENGTH_SAMPLES);
        }

        return null;
    }

    public float[] getStartTone(int length)
    {
        return ToneUtil.getTone(getStartToneFrequency(), getStartToneVolume(), length);
    }

    /**
     * Buffer with samples for the audio segment drop tone
     */
    public float[] getDropTone()
    {
        if(getUseAudioSegmentDropTone())
        {
            return getDropTone(TONE_LENGTH_SAMPLES);
        }

        return null;
    }

    public float[] getDropTone(int length)
    {
        return ToneUtil.getTone(getDropToneFrequency(), getDropToneVolume(), length);
    }

    /**
     * Test tone to use for testing the currently selected mixer output
     */
    public float[] getAudioPlaybackTestTone()
    {
        return ToneUtil.getTone(ToneFrequency.F1200, ToneVolume.V10, TONE_LENGTH_SAMPLES * 4);
    }

    /**
     * Preferred audio playback device
     */
    public AudioPlaybackDeviceDescriptor getAudioPlaybackDevice()
    {
        if(mAudioPlaybackDeviceDescriptor == null)
        {
            AudioPlaybackDeviceDescriptor descriptor = AudioPlaybackDeviceManager.getDefaultAudioPLaybackDevice();

            if(descriptor != null)
            {
                String name = Setting.PLAYBACK_AUDIO_DEVICE_NAME.get(descriptor.getMixerInfo().getName());
                int channelCount = Setting.PLAYBACK_AUDIO_CHANNEL_COUNT.getInt(descriptor.getAudioFormat().getChannels());
                mAudioPlaybackDeviceDescriptor = AudioPlaybackDeviceManager.getAudioPlaybackDevice(name, channelCount);
            }
            else
            {
                mLog.error("Error - no audio playback devices available");
            }
        }

        return mAudioPlaybackDeviceDescriptor;
    }

    /**
     * Sets the preferred audio playback device
     */
    public void setAudioPlaybackDevice(AudioPlaybackDeviceDescriptor descriptor)
    {
        if(descriptor != null)
        {
            mAudioPlaybackDeviceDescriptor = descriptor;
            Setting.PLAYBACK_AUDIO_DEVICE_NAME.put(descriptor.getMixerInfo().getName());
            Setting.PLAYBACK_AUDIO_CHANNEL_COUNT.putInt(descriptor.getAudioFormat().getChannels());
            notifySettingsUpdated();
        }
    }
}
