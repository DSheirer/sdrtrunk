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

import io.github.dsheirer.preference.UserPreferences;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.sampled.AudioFormat;

/**
 * Multi-Channel audio provider implementation.  Provides an interleaved stream of audio from multiple channels.
 *
 * Note: this implementation has not been tested for use.
 */
public class AudioProviderMultiChannel extends AudioProvider
{
    private static ByteBuffer SILENCE;
    private final List<AudioChannel> mAudioChannels = new ArrayList<>();
    private final AudioFormat mAudioFormat;

    /**
     * Constructs a multi-channel audio provider
     * @param userPreferences for this provider
     * @param audioFormat for this provider
     */
    public AudioProviderMultiChannel(UserPreferences userPreferences, AudioFormat audioFormat)
    {
        SILENCE = ByteBuffer.allocate(AudioChannel.SAMPLES_PER_INTERVAL * audioFormat.getChannels());

        for(int channel = 0; channel < audioFormat.getChannels(); channel++)
        {
            mAudioChannels.add(new AudioChannel(userPreferences, String.valueOf(channel)));
        }

        mAudioFormat = audioFormat;
    }

    @Override
    public AudioFormat getAudioFormat()
    {
        return mAudioFormat;
    }

    /**
     * Access the next buffer of audio.
     * @return next buffer or null if there is no audio available
     */
    @Override
    public ByteBuffer getAudio()
    {
        float[][] samples = new float[mAudioChannels.size()][];
        boolean hasAudio = false;

        AudioChannel channel = null;

        for(int x = 0; x < mAudioChannels.size(); x++)
        {
            channel = mAudioChannels.get(x);
            samples[x] = channel.getAudio();

            if(samples[x] == null)
            {
                if(!channel.hasAudioSegment())
                {
                    channel.clearMetadata();
                }

                samples[x] = new float[AudioChannel.SAMPLES_PER_INTERVAL];
            }
            else
            {
                hasAudio = true;
            }
        }

        if(!hasAudio)
        {
            return SILENCE;
        }

        ByteBuffer buffer = ByteBuffer.allocate(AudioChannel.SAMPLES_PER_INTERVAL * 2 *
                mAudioChannels.size()).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer shortBuffer = buffer.asShortBuffer();

        for(int sampleCounter = 0; sampleCounter < AudioChannel.SAMPLES_PER_INTERVAL; sampleCounter++)
        {
            for(int channelNumber = 0; channelNumber < mAudioChannels.size(); channelNumber++)
            {
                shortBuffer.put((short)(samples[channelNumber][sampleCounter] * Short.MAX_VALUE));
            }
        }

        return buffer;
    }

    @Override
    public List<AudioChannel> getAudioChannels()
    {
        return Collections.unmodifiableList(mAudioChannels);
    }
}
