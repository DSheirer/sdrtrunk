/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.audio.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.math3.util.FastMath;

public class MP3SilenceGenerator implements ISilenceGenerator
{
    private final static Logger mLog = LoggerFactory.getLogger(MP3SilenceGenerator.class);

    private MP3AudioConverter mMP3AudioConverter;
    private InputAudioFormat mInputAudioFormat;
    private byte[] mSilenceFrame;
    private int mSilenceFrameDuration;

    /**
     * Generates MP3 audio silence frames
     */
    public MP3SilenceGenerator(InputAudioFormat inputAudioFormat, MP3Setting setting)
    {
        mInputAudioFormat = inputAudioFormat;
        mMP3AudioConverter = new MP3AudioConverter(inputAudioFormat, setting, false);
        generate_frame();
    }

    /**
     * Generates a single silence frame
     * @return
     */
    private void generate_frame()
    {
        //We generate silence at 8000 kHz, because it gets resampled by the silence generator to the target rate
        int length = (int)(InputAudioFormat.SR_8000.getSampleRate());

        List<float[]> silenceBuffers = new ArrayList<>();

        int added = 0;
        while(added < length)
        {
            int chunk = Math.min(length - added, 256);
            silenceBuffers.add(new float[chunk]);
            added += chunk;
        }

        List<byte[]> mp3Audio =  mMP3AudioConverter.convert(silenceBuffers);
        byte[] silence = new byte[0];
        for(byte[] mp3Chunk: mp3Audio)
        {
            silence = merge(silence, mp3Chunk);
        }

        MP3AudioFrames mp3Frames = MP3FrameTools.split(silence);
        if(mp3Frames.hasNextFrame())
        {
            mp3Frames.nextFrame();
            mSilenceFrame = mp3Frames.getCurrentFrame();
            mSilenceFrameDuration = mp3Frames.getCurrentFrameDuration();
        }

    }

    /**
     * Generates silence frames
     * @param duration_ms in milliseconds
     * @return
     */
    public MP3AudioFrames generate(long duration_ms)
    {
        List<byte[]> silenceFrames = new ArrayList<>();
        int silenceDuration = 0;

        while(silenceDuration < duration_ms)
        {
            silenceFrames.add(mSilenceFrame);
            silenceDuration += mSilenceFrameDuration;
        }

        return new MP3AudioFrames(silenceDuration, silenceFrames);
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

    /* TODO: Rework for MP3SilenceGenerator returning IAudioFrames
    public static void main(String[] args)
    {
        mLog.debug("Starting ...");

        MP3SilenceGenerator generator = new MP3SilenceGenerator(InputAudioFormat.SR_44100, MP3Setting.VBR_7);

        int x = 100;

//        for(; x < 1000; x += 10)
//        {
        List<byte[]> silenceFrames = generator.generate(x);

        mLog.debug("Generated [" + silenceFrames.size() + "] silence frames");

        for(byte[] silence: silenceFrames)
        {
            mLog.debug("Silence:" + x + " Length:" + silence.length);
        }

        MP3FrameInspector.inspect(silenceFrames);
//        }

        mLog.debug("Finished");
    }
    */
}
