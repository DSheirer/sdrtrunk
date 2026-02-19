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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;

/**
 * Audio playback device manager.
 */
public class AudioPlaybackDeviceManager
{
    /**
     * Always returns the first available device in the system.
     */
    public static AudioPlaybackDeviceDescriptor getDefaultAudioPLaybackDevice()
    {
        List<AudioPlaybackDeviceDescriptor> devices = getAudioPlaybackDevices();

        if(!devices.isEmpty())
        {
            return devices.getFirst();
        }

        return null;
    }

    /**
     * Finds the matching audio playback device
     * @param name of the device
     * @param channels count
     * @return matching entry or the default playback device.
     */
    public static AudioPlaybackDeviceDescriptor getAudioPlaybackDevice(String name, int channels)
    {
        for(AudioPlaybackDeviceDescriptor device: getAudioPlaybackDevices())
        {
            if(device.getMixerInfo().getName().equals(name) && device.getAudioFormat().getChannels() == channels)
            {
                return device;
            }
        }

        return getDefaultAudioPLaybackDevice();
    }

    public static List<AudioPlaybackDeviceDescriptor> getAudioPlaybackDevices()
    {
        List<AudioPlaybackDeviceDescriptor> devices = new ArrayList<>();

        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        for(Mixer.Info mixerInfo : mixerInfos)
        {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);

            if(mixer != null)
            {
                List<AudioFormat> discoveredFormats = new ArrayList<>();

                for(Line.Info sourceLineInfo : mixer.getSourceLineInfo())
                {
                    if(sourceLineInfo instanceof DataLine.Info dataLineInfo)
                    {
                        for(AudioFormat audioFormat : dataLineInfo.getFormats())
                        {
                            if(!audioFormat.isBigEndian() && audioFormat.getSampleSizeInBits() == 16 &&
                               !discoveredFormats.contains(audioFormat))
                            {
                                if(audioFormat.getChannels() == 1)
                                {
                                    if(audioFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED)
                                    {
                                        discoveredFormats.add(audioFormat);
                                        //Modify the format for 8 kHz sample rate
                                        AudioFormat updatedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                8000, 16, audioFormat.getChannels(), audioFormat.getFrameSize(),
                                                8000, false);
                                        devices.add(new AudioPlaybackDeviceDescriptor(mixerInfo, updatedFormat));
                                    }
                                    else if(audioFormat.getSampleRate() == 8000)
                                    {
                                        discoveredFormats.add(audioFormat);
                                        devices.add(new AudioPlaybackDeviceDescriptor(mixerInfo, audioFormat));
                                    }
                                }
                                //Stereo and 64-channel formats
                                else if(audioFormat.getChannels() == 2 || audioFormat.getChannels() > 8)
                                {
                                    if(audioFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED)
                                    {
                                        discoveredFormats.add(audioFormat);
                                        //Modify the format for 8 kHz sample rate, 2 channels, 4 bytes per frame
                                        AudioFormat updatedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                8000, 16, 2, 4,
                                                8000, false);
                                        devices.add(new AudioPlaybackDeviceDescriptor(mixerInfo, updatedFormat));
                                    }
                                    else if(audioFormat.getSampleRate() == 8000)
                                    {
                                        discoveredFormats.add(audioFormat);
                                        //Modify the format for 8 kHz sample rate, 2 channels, 4 bytes per frame
                                        AudioFormat updatedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                                audioFormat.getSampleRate(), 16, 2, 4,
                                                audioFormat.getFrameRate(), false);
                                        devices.add(new AudioPlaybackDeviceDescriptor(mixerInfo, updatedFormat));
                                    }
                                }
                                //TODO: remove the 2 channels max restriction once supported
//                                else if(audioFormat.getChannels() > 2 && audioFormat.getChannels() <= 8)
//                                {
//                                    if(audioFormat.getSampleRate() == AudioSystem.NOT_SPECIFIED)
//                                    {
//                                        discoveredFormats.add(audioFormat);
//                                        //Modify the format for 8 kHz sample rate
//                                        AudioFormat updatedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
//                                                8000, 16, audioFormat.getChannels(), audioFormat.getFrameSize(),
//                                                8000, false);
//                                        devices.add(new AudioPlaybackDeviceDescriptor(mixerInfo, updatedFormat));
//                                    }
//                                    else if(audioFormat.getSampleRate() == 8000)
//                                    {
//                                        discoveredFormats.add(audioFormat);
//                                        devices.add(new AudioPlaybackDeviceDescriptor(mixerInfo, audioFormat));
//                                    }
//                                }
                            }
                        }
                    }
                }
            }
        }

        Collections.sort(devices);
        return devices;
    }


    static void main()
    {
        List<AudioPlaybackDeviceDescriptor> devices = getAudioPlaybackDevices();
        for(AudioPlaybackDeviceDescriptor device : devices)
        {
            System.out.println(device);
        }
    }
}
