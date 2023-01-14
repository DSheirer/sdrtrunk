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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MP3FrameInspector
{
    private final static Logger mLog = LoggerFactory.getLogger(MP3FrameInspector.class);

    public static void inspect(List<byte[]> frames)
    {
        if(frames.isEmpty())
        {
            mLog.info("No Frame Data!");
            return;
        }

        for(byte[] frame: frames)
        {
            StringBuilder sb = new StringBuilder();
            int frameCounter = 0;

            for(int x = 0; x < frame.length; x++)
            {
                if(MP3Header.isValid(frame, x))
                {
                    sb.append("\n").append(++frameCounter).append(": ");
                    sb.append(MP3Header.inspect(frame, x)).append(" - ");
                }

                sb.append(String.format("%02X ", frame[x]));
            }

            mLog.info(sb.toString());
        }
    }

    /* TODO: Rework for MP3SilenceGenerator returning IAudioFrames
    public static void main(String[] args)
    {
        mLog.info("Starting ...");
        MP3SilenceGenerator gen = new MP3SilenceGenerator(InputAudioFormat.SR_8000, MP3Setting.getDefault());

        List<byte[]> audio = gen.generate(173);

        inspect(audio);

        mLog.info("Finished");
    }
    */
}
