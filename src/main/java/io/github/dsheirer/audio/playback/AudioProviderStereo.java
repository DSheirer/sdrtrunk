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
 * Provides an interleaved stream of left and right audio samples from audio channels.
 *
 * Note: the getAudio() method is optimized to provide buffers of 160 samples (x2) of audio per interval, equivalent to
 * 20 milliseconds of audio at 8,000 Hertz sample rate.
 */
public class AudioProviderStereo extends AudioProvider
{
    private final AudioChannel mAudioChannelLeft;
    private final AudioChannel mAudioChannelRight;
    private static final ByteBuffer SILENCE = ByteBuffer.allocate(AudioChannel.SAMPLES_PER_INTERVAL * 4);

    /**
     * Constructs an instance.
     * @param userPreferences for monitoring preference changes
     */
    public AudioProviderStereo(UserPreferences userPreferences)
    {
        mAudioChannelLeft = new AudioChannel(userPreferences, "LEFT");
        mAudioChannelRight = new AudioChannel(userPreferences, "RIGHT");
    }

    @Override
    public AudioFormat getAudioFormat()
    {
        return AudioFormats.PCM_SIGNED_8000_HZ_16BITS_STEREO;
    }

    @Override
    public List<AudioChannel> getAudioChannels()
    {
        return List.of(mAudioChannelLeft, mAudioChannelRight);
    }

    /**
     * Access the next buffer of audio.
     * @return next buffer or null if there is no audio available
     */
    @Override
    public ByteBuffer getAudio()
    {
        float[] left = mAudioChannelLeft.getAudio();

        if(left == null && !mAudioChannelLeft.hasAudioSegment())
        {
            mAudioChannelLeft.clearMetadata();
        }

        float[] right = mAudioChannelRight.getAudio();

        if(right == null && !mAudioChannelRight.hasAudioSegment())
        {
            mAudioChannelRight.clearMetadata();
        }

        //If we didn't get audio from either channel send silence buffer
        if(left == null && right == null)
        {
            return SILENCE;
        }
        else if(left == null)
        {
            left = new float[AudioChannel.SAMPLES_PER_INTERVAL];
        }
        else if(right == null)
        {
            right = new float[AudioChannel.SAMPLES_PER_INTERVAL];
        }

        ByteBuffer buffer = ByteBuffer.allocate(AudioChannel.SAMPLES_PER_INTERVAL * 4).order(ByteOrder.LITTLE_ENDIAN);
        ShortBuffer shortBuffer = buffer.asShortBuffer();

        for(int x = 0; x < AudioChannel.SAMPLES_PER_INTERVAL; x++)
        {
            shortBuffer.put((short)(left[x] * Short.MAX_VALUE));
            shortBuffer.put((short)(right[x] * Short.MAX_VALUE));
        }

        return buffer;
    }
}
