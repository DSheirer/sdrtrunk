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

public class MP3BitRate
{
    private final static int[] MPEG_1_L3 = new int[]{0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320};
    private final static int[] MPEG_2_L3 = new int[]{0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160};
    private final static int[] MPEG_2_5_L3 = new int[]{0, 8, 16, 24, 32, 40, 48, 56, 64};

    public static int getLayer3BitRate(MPEGVersion version, int value)
    {
        switch(version)
        {
            case V_1:
                if(0 <= value && value < MPEG_1_L3.length)
                {
                    return MPEG_1_L3[value];
                }
                break;
            case V_2:
                if(0 <= value && value < MPEG_2_L3.length)
                {
                    return MPEG_2_L3[value];
                }
                break;
            case V_2_5:
                if(0 <= value && value < MPEG_2_5_L3.length)
                {
                    return MPEG_2_5_L3[value];
                }
                break;
            default:
        }

        return 0;
    }
}
