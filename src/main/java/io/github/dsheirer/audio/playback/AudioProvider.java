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

import io.github.dsheirer.audio.AudioEvent;
import java.nio.ByteBuffer;
import java.util.List;

import javax.sound.sampled.AudioFormat;

/**
 * Abstract audio provider implementation
 */
public abstract class AudioProvider
{
    /**
     * Audio format for this provider
     */
    public abstract AudioFormat getAudioFormat();

    /**
     * Provide a non-null, 160-sample/20-millisecond buffer of audio at 8 kHz
     * @return audio buffer
     */
    public abstract ByteBuffer getAudio();

    /**
     * Provides access to the audio channels for this provider
     * @return list of audio channels
     */
    public abstract List<AudioChannel> getAudioChannels();

    /**
     * Notifies audio event listener(s) of the event
     * @param eventType to broadcast
     */
    public void notify(AudioEvent.Type eventType)
    {
        for(AudioChannel audioChannel : getAudioChannels())
        {
            audioChannel.notify(eventType);
        }
    }

    /**
     * Disposes the audio channel to prepare for audio output changes or shutdown.
     */
    public void dispose()
    {
        for(AudioChannel audioChannel: getAudioChannels())
        {
            audioChannel.dispose();
        }
    }
}
