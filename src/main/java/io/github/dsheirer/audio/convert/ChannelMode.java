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

/**
 * MP3 Channel Mode
 *
 * see: http://www.mp3-tech.org/programmer/frame_header.html
 */
public enum ChannelMode
{
    STEREO("STEREO"),
    JOINT_STEREO("JOINT STEREO"),
    DUAL_CHANNEL("DUAL CHANNEL"),
    MONO("MONO");

    private String mLabel;

    ChannelMode(String label)
    {
        mLabel = label;
    }

    public static ChannelMode fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return STEREO;
            case 1:
                return JOINT_STEREO;
            case 2:
                return DUAL_CHANNEL;
            case 3:
            default:
                return MONO;
        }
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
