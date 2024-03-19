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
 * P25 Phase 2 ISCH Location within a Superframe
 */
public enum ISCHSequence
{
    ISCH_1(0, "FRAGMENT 1/3", 0),
    ISCH_2(1, "FRAGMENT 2/3", 4),
    ISCH_3(2, "FRAGMENT 3/3", 8),
    RESERVED_4(3, "RSVD4", 0),
    UNKNOWN(-1, "UNKNO", 0);

    private int mValue;
    private String mLabel;
    private int mTimeslotOffset;

    ISCHSequence(int value, String label, int timeslotOffset)
    {
        mValue = value;
        mLabel = label;
        mTimeslotOffset = timeslotOffset;
    }

    /**
     * ISCH sequence number value
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Offset to apply to each of the timeslots in a fragment.
     * @return timeslot offset
     */
    public int getTimeslotOffset()
    {
        return mTimeslotOffset;
    }

    /**
     * Indicates if this sequence is a final fragment sequence
     */
    public boolean isFinalFragment()
    {
        return this == ISCH_3;
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
