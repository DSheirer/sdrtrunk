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

import io.github.dsheirer.audio.AudioFormats;
import io.github.dsheirer.preference.UserPreferences;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;

import javax.sound.sampled.AudioFormat;

/**
 * Single channel (mono) audio provider wraps a single audio channel and provides converted audio.
 */
public class AudioProviderMono extends AudioProvider
{
    private static final ByteBuffer SILENCE = ByteBuffer.allocate(AudioChannel.SAMPLES_PER_INTERVAL * 2);
    private final AudioChannel mAudioChannel;

    /**
     * Constructs an instance
     * @param userPreferences for monitoring preference changes
     */
    public AudioProviderMono(UserPreferences userPreferences)
    {
        mAudioChannel = new AudioChannel(userPreferences, "MONO");
    }

    @Override
    public List<AudioChannel> getAudioChannels()
    {
        return List.of(mAudioChannel);
    }

    @Override
    public AudioFormat getAudioFormat()
    {
        return AudioFormats.PCM_SIGNED_8000_HZ_16_BIT_MONO;
    }

    @Override
    public ByteBuffer getAudio()
    {
        float[] audio = mAudioChannel.getAudio();

        if(audio == null)
        {
            if(!mAudioChannel.hasAudioSegment())
            {
                mAudioChannel.clearMetadata();
            }

            return SILENCE;
        }

        ByteBuffer buffer = ByteBuffer.allocate(AudioChannel.SAMPLES_PER_INTERVAL * 2).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer shortBuffer = buffer.asShortBuffer();

        for(int x = 0; x < AudioChannel.SAMPLES_PER_INTERVAL; x++)
        {
            shortBuffer.put((short) (audio[x] * Short.MAX_VALUE));
        }

        return buffer;
    }
}
