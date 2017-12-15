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
package audio.convert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MP3FrameInspector
{
    private final static Logger mLog = LoggerFactory.getLogger(MP3FrameInspector.class);

    public static void inspect(byte[] frames)
    {
        if(frames == null || frames.length == 0)
        {
            return;
        }

        if(frames.length % 144 != 0)
        {
            log("Frame byte array not multiple of 144 byte frame size - length:" + frames.length, frames);
            return;
        }

        //Check for erroneous frame sync
        for(int x = 0; x < frames.length - 1; x++)
        {
            if(frames[x] == (byte)0xFF && (frames[x + 1] & 0xE0) == 0xE0 && (x % 144) != 0)
            {
                log("Bad frame sync detected at byte " + x + " in frame " + (x / 144), frames);
                return;
            }
        }

        //Check MP3 headers

//        log("Frames ...", frames);
    }

    private static void log(String message, byte[] frames)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(message).append("\n");

        int frameCounter = 0;
        sb.append(frameCounter++).append(" ");

        for(int x = 0; x < frames.length; x++)
        {
            if(x > 0 && x % 144 == 0)
            {
                sb.append("\n");
                sb.append(frameCounter++).append(" ");
            }

            sb.append(String.format("%02X ", frames[x]));
        }

        mLog.info(sb.toString());
    }

    public static void main(String[] args)
    {
        mLog.info("Starting ...");
        MP3SilenceGenerator gen = new MP3SilenceGenerator();

        byte[] audio = gen.generate(173);

        inspect(audio);

        mLog.info("Finished");
    }
}
