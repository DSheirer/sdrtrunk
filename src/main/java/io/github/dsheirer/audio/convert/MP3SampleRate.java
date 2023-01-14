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

public class MP3SampleRate
{
    private final static int[] MPEG_1 = new int[]{44100, 48000, 32000};
    private final static int[] MPEG_2 = new int[]{22050, 24000, 16000};
    private final static int[] MPEG2_5 = new int[]{11025, 12000, 8000};

    public static int getSampleRate(MPEGVersion version, int value)
    {
        switch(version)
        {
            case V_1:
                if(0 <= value && value < MPEG_1.length)
                {
                    return MPEG_1[value];
                }
                break;
            case V_2:
                if(0 <= value && value < MPEG_2.length)
                {
                    return MPEG_2[value];
                }
                break;
            case V_2_5:
                if(0 <= value && value < MPEG2_5.length)
                {
                    return MPEG2_5[value];
                }
                break;
            default:
        }

        return 0;
    }
}
