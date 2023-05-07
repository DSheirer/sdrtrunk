/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.alias.id.talkgroup;

import io.github.dsheirer.protocol.Protocol;

/**
 * Talkgroup protocol identifier entry masks and min/max valid ranges and valid range descriptions
 *
 * Note: the getMask() value is intended for use with a MaskFormatter attached to a JFormattedTextField.  The
 * protocol masks that have a dash (-) use the # (any digit) character and the protocol masks that do not have
 * a formatting dash use an asterisk (*).  Within the editor(s) that use this enumeration, the editor must use
 * a setValidCharacters() on the mask formatter whenever it detects the mask containing one or more asterisks.
 */
public enum TalkgroupFormat
{
    AM("*****", 1, 0xFFFF, "1 to 65,535",
            "AM valid value range is 1-65,535"),
    APCO25("********", 0, 0xFFFF, "0 to 65,535",
        "<html>APCO25 talkgroup valid range is 0 to 65,535"),
    DMR("********", 0, 0xFFFFFF, "0 to 16,777,215",
        "<html>DMR talkgroup valid range is 0 to 16,777,215"),
    FLEETSYNC("###-####", 0, 0x7FFFFF, "001-0001 to 127-8192",
        "<html>Fleetsync valid ranges are 1-127(prefix)<br>and 1-8192(ident) (ie. 001-0001 to 127-8192)"),
    LTR("##-###", 0x101, 0x3FFF, "01-001 to 20-255",
        "<html>LTR valid ranges are 1-20(repeater) and 1-255(talkgroup) (ie. 01-001 to 20-255)"),
    MDC1200("*****", 1, 0xFFFF, "1 to 65,535",
        "MDC-1200 valid value range is 1-65,535"),
    MPT1327("###-####", 1, 0x7FFFFF, "000-0001 to 127-8192",
        "<html>MPT-1327 valid ranges are 0-127(prefix)<br>and 1-8192(ident) (ie. 000-0001 to 127-8192)"),
    NBFM("*****", 1, 0xFFFF, "1 to 65,535",
            "NBFM valid value range is 1-65,535"),
    PASSPORT("*****", 1, 0xFFFF, "1 to 65,535",
        "Passport valid value range is 1-65,535"),
    UNKNOWN("********", 1, 0xFFFFFF, "1 to 16,777,215",
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
     * Indicates if the value is valid for the protocol
     */
    public boolean isValid(int value)
    {
        return getMinimumValidValue() <= value && value <= getMaximumValidValue();
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

    public static TalkgroupFormat get(Protocol protocol)
    {
        if(protocol == null)
        {
            return UNKNOWN;
        }

        switch(protocol)
        {
            case AM:
                return AM;
            case APCO25:
                return APCO25;
            case FLEETSYNC:
                return FLEETSYNC;
            case LTR:
            case LTR_NET:
                return LTR;
            case MDC1200:
                return MDC1200;
            case MPT1327:
                return MPT1327;
            case NBFM:
                return NBFM;
            case PASSPORT:
                return PASSPORT;
            default:
                return UNKNOWN;
        }
    }
}
