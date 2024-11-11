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

import java.util.EnumSet;

/**
 * P25 Phase 2 Data Unit ID (DUID) enumeration
 */
public enum DataUnitID
{
    VOICE_4(0, 0x00,"VOICE-4"),
    RESERVED_1(1, 0x17, "RESERVED 1"),
    RESERVED_2(2, 0x2E, "RESERVED 2"),
    SCRAMBLED_SACCH(3, 0x39, "SACCH-S"),
    RESERVED_4(4, 0x4B, "RESERVED 4"),
    RESERVED_5(5, 0x5C, "RESERVED 5"),
    VOICE_2(6, 0x65, "VOICE-2"),
    RESERVED_7(7, 0x72, "RESERVED 7"),
    RESERVED_8(8, 0x8D, "RESERVED 8"),
    SCRAMBLED_FACCH(9, 0x9A,  "FACCH-S"),
    SCRAMBLED_DATCH(10, 0xA3, "DATCH-S"), //Motorola APX-Next TDMA Data Channel
    RESERVED_B(11, 0xB4, "RESERVED 11"),
    UNSCRAMBLED_SACCH(12, 0xC6,  "SACCH-U"),
    UNSCRAMBLED_LCCH(13, 0xD1, "LOCCH-U"),
    RESERVED_E(14, 0xE8, "RESERVED 14"),
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

    /**
     * Indicates if this DUID is a LCCH
     * @return
     */
    public boolean isLCCH()
    {
        return this == UNSCRAMBLED_LCCH;
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
        switch(value)
        {
            case 0x00:
                return VOICE_4;
            case 0x17:
                return RESERVED_1;
            case 0x2E:
                return RESERVED_2;
            case 0x39:
                return SCRAMBLED_SACCH;
            case 0x4B:
                return RESERVED_4;
            case 0x5C:
                return RESERVED_5;
            case 0x65:
                return VOICE_2;
            case 0x72:
                return RESERVED_7;
            case 0x8D:
                return RESERVED_8;
            case 0x9A:
                return SCRAMBLED_FACCH;
            case 0xA3:
                return SCRAMBLED_DATCH;
            case 0xB4:
                return RESERVED_B;
            case 0xC6:
                return UNSCRAMBLED_SACCH;
            case 0xD1:
                return UNSCRAMBLED_LCCH;
            case 0xE8:
                return RESERVED_E;
            case 0xFF:
                return UNSCRAMBLED_FACCH;
        }

        DataUnitID closest = DataUnitID.UNKNOWN;
        int errorCount = 8;

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

        //The encoding (8,4,4) should be able to detect up to 2 bit errors.  Anything more is ambiguous.
        if(errorCount <= 2)
        {
            return closest;
        }

        return UNKNOWN;
    }
}
