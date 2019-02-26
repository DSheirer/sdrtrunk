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
 * P25 Phase 2 ISCH Location within a Superframe
 */
public enum ISCHSequence
{
    ISCH_1(0, "ISCH 1"),
    ISCH_2(1, "ISCH 2"),
    ISCH_3(2, "ISCH 3"),
    RESERVED_4(3, "RESERVED_4"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    ISCHSequence(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * ISCH sequence number value
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
     * Lookup the ISCH sequence from an integer value
     */
    public static ISCHSequence fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return ISCH_1;
            case 1:
                return ISCH_2;
            case 2:
                return ISCH_3;
            case 3:
                return RESERVED_4;
            default:
                return UNKNOWN;
        }
    }
}
