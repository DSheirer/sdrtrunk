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
 * P25 Phase 2 Data Unit ID (DUID) enumeration
 */
public enum DataUnitID
{
    VOICE_4(0,"VOICE 4V"),
    RESERVED_1(1, "RESERVED 1"),
    RESERVED_2(2, "RESERVED_2"),
    SCRAMBLED_SACCH(3, "SACCH SCRAMBLED"),
    RESERVED_4(4, "RESERVED 4"),
    RESERVED_5(5, "RESERVED_5"),
    VOICE_2(6,"VOICE 2V"),
    RESERVED_7(7, "RESERVED_7"),
    RESERVED_8(8, "RESERVED 8"),
    SCRAMBLED_FACCH(9, "FACCH SCRAMBLED"),
    RESERVED_10(10, "RESERVED_10"),
    RESERVED_11(11, "RESERVED 11"),
    UNSCRAMBLED_SACCH(12, "SACCH UNSCRAMBLED"),
    RESERVED_13(13, "RESERVED_13"),
    RESERVED_14(14, "RESERVED 14"),
    UNSCRAMBLED_FACCH(15, "FACCH UNSCRAMBLED"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    DataUnitID(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Data Unit ID value
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
     * Lookup the Data Unit ID from an integer value
     */
    public static DataUnitID fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return VOICE_4;
            case 3:
                return SCRAMBLED_SACCH;
            case 6:
                return VOICE_2;
            case 9:
                return SCRAMBLED_FACCH;
            case 12:
                return UNSCRAMBLED_SACCH;
            case 15:
                return UNSCRAMBLED_FACCH;

            //Undefined values
            case 1:
                return RESERVED_1;
            case 2:
                return RESERVED_2;
            case 4:
                return RESERVED_4;
            case 5:
                return RESERVED_5;
            case 7:
                return RESERVED_7;
            case 8:
                return RESERVED_8;
            case 10:
                return RESERVED_10;
            case 11:
                return RESERVED_11;
            case 13:
                return RESERVED_13;
            case 14:
                return RESERVED_14;
            default:
                return UNKNOWN;
        }
    }
}
