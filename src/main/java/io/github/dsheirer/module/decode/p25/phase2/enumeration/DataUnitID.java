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

import java.util.EnumSet;

/**
 * P25 Phase 2 Data Unit ID (DUID) enumeration
 */
public enum DataUnitID
{

    VOICE_4(0, 0x00,"VOICE-4"),
    SCRAMBLED_SACCH(3, 0x39, "SACCH-S"),
    VOICE_2(6, 0x65, "VOICE-2"),
    SCRAMBLED_FACCH(9, 0x9A,  "FACCH-S"),
    UNSCRAMBLED_SACCH(12, 0xC6,  "SACCH-U"),
    UNSCRAMBLED_FACCH(15, 0xFF,  "FACCH-U"),

    UNKNOWN(-1, 0x00, "UNKNOWN-");

    private int mValue;
    private int mValueWithParity;
    private String mLabel;

    DataUnitID(int value, int valueWithParity, String label)
    {
        mValue = value;
        mValueWithParity = valueWithParity;
        mLabel = label;
    }

    /**
     * Data Unit ID value
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Data Unit ID value with parity bits
     */
    public int getValueWithParity()
    {
        return mValueWithParity;
    }

    /**
     * Indicates if this DUID is a SACCH, either scrambled or unscrambled
     */
    public boolean isSACCH()
    {
        return this == SCRAMBLED_SACCH || this == UNSCRAMBLED_SACCH;
    }

    /**
     * Indicates if this DUID is a FACCH, either scrambled or unscrambled
     */
    public boolean isFACCH()
    {
        return this == SCRAMBLED_FACCH || this == UNSCRAMBLED_FACCH;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static EnumSet<DataUnitID> VALID_VALUES = EnumSet.range(VOICE_4, UNSCRAMBLED_FACCH);

    /**
     * Lookup the Data Unit ID from an encoded 8-bit integer value that contains the 4-bit duid value and an appended
     * 4-bit parity value.
     */
    public static DataUnitID fromEncodedValue(int value)
    {
        int masked = 0xF & (value >> 4);

        switch(masked)
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
        }

        DataUnitID closest = DataUnitID.UNKNOWN;
        int errorCount = 4;

        for(DataUnitID duid: VALID_VALUES)
        {
            int mask = value ^ duid.getValueWithParity();
            int maskErrorCount = Integer.bitCount(mask);

            if(maskErrorCount < errorCount)
            {
                errorCount = maskErrorCount;
                closest = duid;
            }
        }

        return closest;
    }
}
