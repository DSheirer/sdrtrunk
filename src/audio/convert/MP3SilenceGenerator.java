/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.convert;

import audio.AudioPacket;
import audio.broadcast.AudioBroadcaster;
import audio.convert.MP3AudioConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import record.mp3.MP3Recorder;

import java.util.ArrayList;
import java.util.List;

public class MP3SilenceGenerator implements ISilenceGenerator
{
    private final static Logger mLog = LoggerFactory.getLogger(MP3SilenceGenerator.class);

    private MP3AudioConverter mGenerator = new MP3AudioConverter(MP3Recorder.MP3_BIT_RATE, MP3Recorder.CONSTANT_BIT_RATE);

    /**
     * Generates MP3 audio silence frames
     */
    public MP3SilenceGenerator()
    {
        //Prime the conversion buffer with 172 ms of audio to produce (and throw away) the first 20 byte buffer
        generate(172);
    }

    public byte[] generate(long duration)
    {
        int length = (int)(duration * 8);   //8000 Hz sample rate
        float[] silence = new float[length];
        AudioPacket silencePacket = new AudioPacket(silence, null);
        List<AudioPacket> silencePackets = new ArrayList<>();
        silencePackets.add(silencePacket);
        return mGenerator.convert(silencePackets);
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        MP3SilenceGenerator generator = new MP3SilenceGenerator();

        int sampleCount = 0;

        int block = 1;

        for(long x = 0; x < 500; x ++)
        {
            byte[] silence = generator.generate(block);

            sampleCount += block;
            mLog.debug("Silence:" + x + " Count:" + sampleCount + " Length:" + silence.length);
        }

        mLog.debug("Finished");
    }
}
