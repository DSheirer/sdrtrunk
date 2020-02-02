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
package io.github.dsheirer.module.decode.dmr.message.data;

/**
 * The Data Unit ID (DUID) is part of the Network ID (NID) field and indicates the type of message.
 */
public enum DataType
{
    PI_HEADER(0, 276, true, "PI  "),
    VOICE_HEADER(1, 276, false, "VH "),
    TLC(2, 276, false, "TLC "),
    CSBK(3, 276, false, "CSBK "),
    MBC_HEADER(4, 276, false, "MBCH "),
    MBC(5, 276, false, "MBC "),
    DATA_HEADER(6, 276, false, "DATA "),
    RATE_1_OF_2_DATA(7, 276, false, "RATE 1/2 "),
    RATE_3_OF_4_DATA(8, 276, false, "RATE 3/4 "),
    SLOT_IDLE(9, 276, false, "IDLE "),
    RATE_1_DATA(10, 276, false, "RATE 1  "),
    PDT_CSBK_ENC_HEADER(11, 276, false, "CSBK ENCRYPTED "),
    PDT_MBC_ENC_HEADER(12, 276, false, "MBC ENCRYPTED"),
    PDT_DATA_ENC_HEADER(13, 276, false, "DATA ENCRYPTED "),
    PDT_CHANNEL_CONTROL_ENC_HEADER(14, 276, false, "MBC ENCRYPTED "),
    RESERVED_15(15, -1, false, "BAD "),

    UNKNOWN(-1, -1, false, "UNKN ");

    private int mValue;
    private int mMessageLength;
    private boolean mTrailingStatusDibit;
    private String mLabel;

    DataType(int value, int length, boolean trailingStatusDibit, String label)
    {
        mValue = value;
        mMessageLength = length;
        mTrailingStatusDibit = trailingStatusDibit;
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
     * Indicates if the message has a trailing status dibit that must be processed
     */
    public boolean hasTrailingStatusDibit()
    {
        return mTrailingStatusDibit;
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
                return MBC;
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
                return PDT_CSBK_ENC_HEADER;
            case 12:
                return PDT_MBC_ENC_HEADER;
            case 13:
                return PDT_DATA_ENC_HEADER;
            case 14:
                return PDT_CHANNEL_CONTROL_ENC_HEADER;
            case 15:
                return RESERVED_15;
            default:
                throw new IllegalArgumentException("Slot Type ID must be in range 0 - 15");
        }
    }
}
