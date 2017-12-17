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
package io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox;

public enum UltravoxMessageClass
{
    OPERATIONS(0x0),
    BROADCASTER(0x1),
    LISTENER(0x2),
    CACHEABLE_METADATA_1(0x3),
    CACHEABLE_METADATA_2(0x4),
    PASS_THROUGH_METADATA_1(0x5),
    PASS_THROUGH_METADATA_2(0x6),
    ENCODED_DATA(0x7),
    ADVANCED_ENCODED_DATA(0x8),
    FRAMED_DATA(0x9),
    CACHEABLE_BINARY_METADATA(0xA),
    UNKNOWN(0xF);

    private int mValue;

    private UltravoxMessageClass(int value)
    {
        mValue = value;
    }

    public int getValue()
    {
        return mValue;
    }

    public static UltravoxMessageClass fromValue(int value)
    {
        if(0x0 <= value && value <= 0xA)
        {
            return values()[value];
        }

        return UNKNOWN;
    }
}
