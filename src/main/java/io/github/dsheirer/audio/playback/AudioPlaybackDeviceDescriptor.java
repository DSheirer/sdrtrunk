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

package io.github.dsheirer.audio.playback;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;

/**
 * Audio playback device descriptor for a mixer and a supported audio output format (ie number of channels).
 */
public class AudioPlaybackDeviceDescriptor implements Comparable<AudioPlaybackDeviceDescriptor>
{
    private final Mixer.Info mMixerInfo;
    private final AudioFormat mAudioFormat;

    /**
     * Constructs an instance
     *
     * @param mixerInfo for the mixer
     * @param audioFormat supported by the mixer
     */
    public AudioPlaybackDeviceDescriptor(Mixer.Info mixerInfo, AudioFormat audioFormat)
    {
        mMixerInfo = mixerInfo;
        mAudioFormat = audioFormat;
    }

    @Override
    public String toString()
    {
        return getCleanDescription(getMixerInfo()) + "(" + getMixerInfo().getName() + ") - " +
                getChannelDescription(getAudioFormat());
    }

    private static String getCleanDescription(Mixer.Info mixerInfo)
    {
        return mixerInfo.getDescription().replace("Direct Audio Device: ", "");
    }

    /**
     * Provides a description of the channels for the audio format
     * @param audioFormat with channels
     * @return pretty string
     */
    private static String getChannelDescription(AudioFormat audioFormat)
    {
        return switch(audioFormat.getChannels())
        {
            case 1 -> "Mono";
            case 2 -> "Stereo";
            case AudioSystem.NOT_SPECIFIED -> "Unknown number of channels";
            default -> audioFormat.getChannels() + " Channels";
        };
    }

    /**
     * Audio format supported by the mixer
     *
     * @return audio format
     */
    public AudioFormat getAudioFormat()
    {
        return mAudioFormat;
    }

    /**
     * Mixer information for the available system mixer.
     */
    public Mixer.Info getMixerInfo()
    {
        return mMixerInfo;
    }

    @Override
    public int compareTo(AudioPlaybackDeviceDescriptor other)
    {
        if(other == null)
        {
            return 1;
        }

        if(other.getMixerInfo().getDescription().equals(getMixerInfo().getDescription()))
        {
            return Integer.compare(getAudioFormat().getChannels(), other.getAudioFormat().getChannels());
        }

        return getMixerInfo().getDescription().compareTo(other.getMixerInfo().getDescription());
    }
}
