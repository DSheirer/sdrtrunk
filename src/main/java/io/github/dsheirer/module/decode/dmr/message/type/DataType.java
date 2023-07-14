/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * DMR Data Type enumeration
 */
public enum DataType
{
    PI_HEADER(0, 288,  "ENCRYPTION HEADER"),
    VOICE_HEADER(1, 288, "VOICE HEADER"),
    TLC(2, 288,  "TERMINATOR"),
    CSBK(3, 288, "CSBK"),
    MBC_HEADER(4, 288, "MULTI-BLOCK CONTROL HEADER"),
    MBC_BLOCK(5, 276,  "MULTI-BLOCK CONTROL BLOCK"),
    DATA_HEADER(6, 288,  "DATA HEADER"),
    RATE_1_OF_2_DATA(7, 276, "RATE 1/2 PACKET"),
    RATE_3_OF_4_DATA(8, 276,  "RATE 3/4 PACKET"),
    SLOT_IDLE(9, 276, "IDLE"),
    RATE_1_DATA(10, 276,  "RATE 1/1 PACKET"),
    USB_DATA(11, 276,  "UNIFIED SINGLE BLOCK DATA"),
    MBC_ENC_HEADER(12, 276,  "MULTI-BLOCK CONTROL ENCRYPTED HEADER"),
    DATA_ENC_HEADER(13, 276, "DATA ENCRYPTED HEADER"),
    CHANNEL_CONTROL_ENC_HEADER(14, 276,  "CONTROL CHANNEL ENCRYPTED HEADER"),
    RESERVED_15(15, -1,  "RESERVED 15"),
    UNKNOWN(-1, -1,  "UNKNOWN");

    private int mValue;
    private int mMessageLength;
    private String mLabel;

    DataType(int value, int length, String label)
    {
        mValue = value;
        mMessageLength = length;
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
     * Length of the message in bits
     */
    public int getMessageLength()
    {
        return mMessageLength;
    }

    /**
     * Short display label
     */
    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Lookup the Data Unit ID from an integer value
     */
    public static DataType fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return PI_HEADER;
            case 1:
                return VOICE_HEADER;
            case 2:
                return TLC;
            case 3:
                return CSBK;
            case 4:
                return MBC_HEADER;
            case 5:
                return MBC_BLOCK;
            case 6:
                return DATA_HEADER;
            case 7:
                return RATE_1_OF_2_DATA;
            case 8:
                return RATE_3_OF_4_DATA;
            case 9:
                return SLOT_IDLE;
            case 10:
                return RATE_1_DATA;
            case 11:
                return USB_DATA;
            case 12:
                return MBC_ENC_HEADER;
            case 13:
                return DATA_ENC_HEADER;
            case 14:
                return CHANNEL_CONTROL_ENC_HEADER;
            case 15:
                return RESERVED_15;
            default:
                throw new IllegalArgumentException("Slot Type ID must be in range 0 - 15");
        }
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
