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
package ua.in.smartjava.audio.output;

import ua.in.smartjava.audio.AudioFormats;
import ua.in.smartjava.audio.AudioPacket;
import ua.in.smartjava.source.mixer.MixerChannel;

import javax.sound.sampled.Mixer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

/**
 * Stereo ua.in.smartjava.audio output implementation.
 */
public class StereoAudioOutput extends AudioOutput
{
    private final static int BUFFER_SIZE = 16000;

    public StereoAudioOutput(Mixer mixer, MixerChannel channel)
    {
        super(mixer, channel, AudioFormats.PCM_SIGNED_8KHZ_16BITS_STEREO, AudioFormats.STEREO_SOURCE_DATALINE_INFO,
            BUFFER_SIZE);
    }

    /**
     * Converts the ua.in.smartjava.audio packet data into stereo ua.in.smartjava.audio frames with the mixer
     * ua.in.smartjava.channel containing the ua.in.smartjava.audio and the other ua.in.smartjava.channel containing zero
     * valued (silent) samples.
     */
    protected ByteBuffer convert(AudioPacket packet)
    {
        if(packet.hasAudioBuffer())
        {
            float[] samples = packet.getAudioBuffer().getSamples();

			/* Little-endian byte ua.in.smartjava.buffer */
            ByteBuffer buffer = ByteBuffer.allocate(samples.length * 4)
                .order(ByteOrder.LITTLE_ENDIAN);

            ShortBuffer shortBuffer = buffer.asShortBuffer();

            if(getMixerChannel() == MixerChannel.LEFT)
            {
                for(float sample : samples)
                {
                    shortBuffer.put((short) (sample * Short.MAX_VALUE));
                    shortBuffer.put((short) 0);
                }
            }
            else
            {
                for(float sample : samples)
                {
                    shortBuffer.put((short) 0);
                    shortBuffer.put((short) (sample * Short.MAX_VALUE));
                }
            }

            return buffer;
        }

        return null;
    }
}
