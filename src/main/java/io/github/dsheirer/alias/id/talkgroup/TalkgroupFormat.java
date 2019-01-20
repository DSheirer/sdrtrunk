/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.alias.id.talkgroup;

import io.github.dsheirer.protocol.Protocol;

/**
 * Talkgroup protocol identifier entry masks and min/max valid ranges and valid range descriptions
 */
public enum TalkgroupFormat
{
    APCO25("########", 1, 0xFFFFFF, "1 to 16,777,215",
        "<html>APCO25 valid range is 1 to 65,535(talkgroup)<br>or 1 to 16,777,215 (radio ID)"),
    FLEETSYNC("###-####", 1, 0x7FFFFF, "001-0001 to 127-8192",
        "<html>Fleetsync valid ranges are 1-127(prefix)<br>and 1-8192(ident) (ie. 001-0001 to 127-8192)"),
    LTR("##-###", 257, 5375, "01-001 to 20-255",
        "<html>LTR valid ranges are 1-20(repeater) and 1-255(talkgroup) (ie. 01-001 to 20-255)"),
    MDC1200("#####", 1, 0xFFFF, "1 to 65,535",
        "MDC-1200 valid value range is 1-65,535"),
    MPT1327("###-####", 1, 0x7FFFFF, "000-0001 to 127-8192",
        "<html>MPT-1327 valid ranges are 0-127(prefix)<br>and 1-8192(ident) (ie. 000-0001 to 127-8192)"),
    PASSPORT("#####", 1, 0xFFFF, "1 to 65,535",
        "Passport valid value range is 1-65,535"),
    UNKNOWN("########", 1, 0xFFFFFF, "1 to 16,777,215",
        "Unknown protocol valid value range is 1-16,777,215");

    private String mMask;
    private int mMinimumValue;
    private int mMaximumValue;
    private String mValidRangeDescription;
    private String mValidRangeHelpText;

    TalkgroupFormat(String mask, int minimumValue, int maximumValue, String validRangeDescription, String validRangeHelpText)
    {
        mMask = mask;
        mMinimumValue = minimumValue;
        mMaximumValue = maximumValue;
        mValidRangeDescription = validRangeDescription;
        mValidRangeHelpText = validRangeHelpText;
    }

    public String getMask()
    {
        return mMask;
    }

    /**
     * Minimum valid value for the protocol
     */
    public int getMinimumValidValue()
    {
        return mMinimumValue;
    }

    /**
     * Maximum valid value for the protocol
     */
    public int getMaximumValidValue()
    {
        return mMaximumValue;
    }

    /**
     * Short description of the valid value range
     */
    public String getValidRangeDescription()
    {
        return mValidRangeDescription;
    }

    /**
     * Help text for valid value ranges
     */
    public String getValidRangeHelpText()
    {
        return mValidRangeHelpText;
    }

    public static TalkgroupFormat get(Protocol prococol)
    {
        switch(prococol)
        {
            case APCO25:
                return APCO25;
            case FLEETSYNC:
                return FLEETSYNC;
            case LTR:
            case LTR_NET:
            case LTR_STANDARD:
                return LTR;
            case MDC1200:
                return MDC1200;
            case MPT1327:
                return MPT1327;
            case PASSPORT:
                return PASSPORT;
            default:
                return UNKNOWN;
        }
    }
}
