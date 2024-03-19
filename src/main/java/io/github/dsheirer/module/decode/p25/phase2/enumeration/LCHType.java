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
 * LCH timeslot configuration type enumeration used in the I-ISCH.
 */
public enum LCHType
{
    VCH("VCH", "INBOUND SACCH IN USE", "INBOUND SACCH FREE"),
    DCH("DCH", "DATA 0", "DATA 1"),
    RESERVED("RESERVED", "RESERVED 0", "RESERVED 1"),
    LCCH("LCCH", "SINGLE-SLOT CONTROL", "DUAL-SLOT CONTROL"),
    UNKNOWN("UNKNOWN", "UNKNOWN 0", "UNKNOWN 1");

    private String mLabel;
    private String mLchFlagFalse;
    private String mLchFlagTrue;

    /**
     * Constructs an instance
     * @param label to use for the entry
     * @param lchFlagFalse label for when the LCH flag is false value.
     * @param lchFlagTrue label for when the LCH flag is true value.
     */
    LCHType(String label, String lchFlagFalse, String lchFlagTrue)
    {
        mLabel = label;
        mLchFlagFalse = lchFlagFalse;
        mLchFlagTrue = lchFlagTrue;
    }



    /**
     * Utility method to lookup LCH Type from a numeric value.
     * @param value to lookup
     * @return LCH type or UNKNOWN
     */
    public static LCHType fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return VCH;
            case 1:
                return DCH;
            case 2:
                return RESERVED;
            case 3:
                return LCCH;
            default:
                return UNKNOWN;
        }
    }

    /**
     * Gets the LCH flag meaning for this LCH type.
     * @param flag value.
     * @return label for the flag value for this LCH type.
     */
    public String getLCHFlagLabel(boolean flag)
    {
        return flag ? mLchFlagTrue : mLchFlagFalse;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
