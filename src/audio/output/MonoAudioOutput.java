/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio.output;

import audio.AudioFormats;
import audio.AudioPacket;
import source.mixer.MixerChannel;

import javax.sound.sampled.Mixer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Mono Audio output implementation
 */
public class MonoAudioOutput extends AudioOutput
{
    private final static int BUFFER_SIZE = 8000;

    public MonoAudioOutput(Mixer mixer)
    {
        super(mixer, MixerChannel.MONO, AudioFormats.PCM_SIGNED_8KHZ_16BITS_MONO,
            AudioFormats.MONO_SOURCE_DATALINE_INFO, BUFFER_SIZE);
    }

    /**
     * Converts the audio packet data into mono audio frames.
     */
    protected ByteBuffer convert(AudioPacket packet)
    {
        if(packet.hasAudioBuffer())
        {
            float[] samples = packet.getAudioBuffer().getSamples();

			/* Little-endian byte buffer */
            ByteBuffer buffer = ByteBuffer.allocate(samples.length * 2).order(ByteOrder.LITTLE_ENDIAN);

            ShortBuffer shortBuffer = buffer.asShortBuffer();

            for(float sample : samples)
            {
                shortBuffer.put((short) (sample * Short.MAX_VALUE));
            }

            return buffer;
        }

        return null;
    }
}
