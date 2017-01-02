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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import record.mp3.MP3Recorder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MP3SilenceGenerator implements ISilenceGenerator
{
    private final static Logger mLog = LoggerFactory.getLogger(MP3SilenceGenerator.class);

    private MP3AudioConverter mGenerator = new MP3AudioConverter(MP3Recorder.MP3_BIT_RATE, MP3Recorder.CONSTANT_BIT_RATE);
    private byte[] mPreviousPartialFrameData;

    /**
     * Generates MP3 audio silence frames
     */
    public MP3SilenceGenerator()
    {
    }

    public byte[] generate(long duration)
    {
        int length = (int)(duration * 8);   //8000 Hz sample rate
        float[] silence = new float[length];
        AudioPacket silencePacket = new AudioPacket(silence, null);
        List<AudioPacket> silencePackets = new ArrayList<>();
        silencePackets.add(silencePacket);

        byte[] frameData = mGenerator.convert(silencePackets);

        frameData = merge(mPreviousPartialFrameData, frameData);

        if(frameData != null && frameData.length > 0)
        {
            if(frameData.length < 144)
            {
                mPreviousPartialFrameData = frameData;
                return null;
            }
            else if((frameData.length % 144) == 0)
            {
                mPreviousPartialFrameData = null;
                return frameData;
            }
            else
            {
                int integralFrameLength = (int)(frameData.length / 144) * 144;
                mPreviousPartialFrameData = Arrays.copyOfRange(frameData, integralFrameLength, frameData.length);
                return Arrays.copyOf(frameData, integralFrameLength);
            }
        }

        return null;
    }

    private static byte[] merge(byte[] a, byte[] b)
    {
        if(a == null && b == null)
        {
            return null;
        }
        else if(a == null)
        {
            return b;
        }
        else if(b == null)
        {
            return a;
        }

        byte[] c = new byte[a.length + b.length];
        System.arraycopy(a,0,c,0,a.length);
        System.arraycopy(b,0,c,a.length,b.length);

        return c;
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        for(long x = 243; x < 500; x ++)
        {
            MP3SilenceGenerator generator = new MP3SilenceGenerator();

            byte[] silence = generator.generate(x);

            if(silence != null && silence.length > 0)
            {
                mLog.debug("Silence:" + x + " Length:" + silence.length);
                MP3FrameInspector.inspect(silence);
            }
        }

        mLog.debug("Finished");
    }
}
