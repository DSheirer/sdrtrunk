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

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;

public class MP3FrameTools {

    private MP3FrameTools() {}

    /**
     * Split MP3 audio at frame boundaries
     * @param input byte array of audio
     * @return list of byte arrays containing audio frames
     */
    public static MP3AudioFrames split(byte[] input)
    {
        List<byte[]> mp3Frames = new ArrayList<>();
        int audioDuration = 0;

        int offset = 0;
        while(offset < input.length)
        {
            if(MP3Header.hasSync(input, offset))
            {
                int framelen = MP3Header.getFrameLength(input, offset);
                mp3Frames.add(Arrays.copyOfRange(input, offset, offset + FastMath.min(framelen, input.length - offset)));
                audioDuration += MP3Header.getFrameDuration(input, offset);
                offset += framelen;
            }
            else
            {
                offset += 1;
            }
        }

        return new MP3AudioFrames(audioDuration, mp3Frames);
    }

}
