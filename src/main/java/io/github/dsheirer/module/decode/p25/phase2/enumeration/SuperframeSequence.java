/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase2.enumeration;

/**
 * P25 Phase 2 Superframe location within an Ultraframe
 */
public enum SuperframeSequence
{
    SUPERFRAME_1(0, "SUPERFRAME 1/4"),
    SUPERFRAME_2(1, "SUPERFRAME 2/4"),
    SUPERFRAME_3(2, "SUPERFRAME 3/4"),
    SUPERFRAME_4(3, "SUPERFRAME 4/4"),
    UNKNOWN(-1, "UNK");

    private int mValue;
    private String mLabel;

    SuperframeSequence(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Superframe sequence number value
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
     * Lookup the superframe sequence from an integer value
     */
    public static SuperframeSequence fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return SUPERFRAME_1;
            case 1:
                return SUPERFRAME_2;
            case 2:
                return SUPERFRAME_3;
            case 3:
                return SUPERFRAME_4;
            default:
                return UNKNOWN;
        }
    }
}
