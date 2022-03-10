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
 * MPEG Layer
 *
 * see: http://www.mp3-tech.org/programmer/frame_header.html
 */
public enum MPEGLayer
{
    RESERVED("LAYER-RESERVED"),
    LAYER3("LAYER-3"),
    LAYER2("LAYER-2"),
    LAYER1("LAYER-1");

    private String mLabel;

    MPEGLayer(String label)
    {
        mLabel = label;
    }

    public static MPEGLayer fromValue(int value)
    {
        switch(value)
        {
            case 1:
                return LAYER3;
            case 2:
                return LAYER2;
            case 3:
                return LAYER1;
            default:
            case 0:
                return RESERVED;
        }
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
