/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.p25.phase2.enumeration;

/**
 * P25 Phase 2 Channel Number enumeration
 */
public enum ChannelNumber
{
    VOICE_CHANNEL_0(0, "VCH0"),
    VOICE_CHANNEL_1(1, "VCH1"),
    RESERVED_2(2, "RESERVED_2"),
    RESERVED_3(3, "RESERVED_3"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    ChannelNumber(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Channel number value
     */
    public int getValue()
    {
        return mValue;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Lookup the Channel Number from an integer value
     */
    public static ChannelNumber fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return VOICE_CHANNEL_0;
            case 1:
                return VOICE_CHANNEL_1;
            case 2:
                return RESERVED_2;
            case 3:
                return RESERVED_3;
            default:
                return UNKNOWN;
        }
    }
}
