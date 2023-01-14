/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase1;

/**
 * The Data Unit ID (DUID) is part of the Network ID (NID) field and indicates the type of message.
 */
public enum P25P1DataUnitID
{
    HEADER_DATA_UNIT(0, 648 + 10, true, "HDU  "),
    UNKNOWN_1(1, -1, false, "UNK01"),
    UNKNOWN_2(2, -1, false, "UNK02"),
    TERMINATOR_DATA_UNIT(3, 28, true, "TDU  "),
    UNKNOWN_4(4, -1, false, "UNK04"),
    LOGICAL_LINK_DATA_UNIT_1(5, 1568, true, "LDU1 "),
    VSELP1(6, 64 + 1616, false, "VSEL1"),
    UNKNOWN_8(8, -1, false, "UNK08"),
    VSELP2(9, 1616 + 64, false, "VSEL2"),
    LOGICAL_LINK_DATA_UNIT_2(10, 1568, true, "LDU2 "),
    UNKNOWN_11(11, -1, false, "UNK11"),
    PACKET_HEADER_DATA_UNIT(12, 196, false, "PDUH "),
    PACKET_DATA_UNIT(-1, 196, false, "PDU  "),
    UNKNOWN_13(13, -1, false, "UNK13"),
    UNKNOWN_14(14, -1, false, "UNK14"),
    TERMINATOR_DATA_UNIT_LINK_CONTROL(15, 308, true, "TDULC"),

    //The following are not true data unit identifiers, rather they are used as identifiers
    ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL(-1, -1, false, "AMBTC"),
    IP_PACKET_DATA(-1, 0, false, "IPPKT"),
    SUBNETWORK_DEPENDENT_CONVERGENCE_PROTOCOL(-1, 0, false, "SNDCP"),
    TRUNKING_SIGNALING_BLOCK_1(7, 196, false, "TSBK1"),
    TRUNKING_SIGNALING_BLOCK_2(7, 196, false, "TSBK2"),
    TRUNKING_SIGNALING_BLOCK_3(7, 196, false, "TSBK3"),
    UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL(-1, -1, false, "UMBTC"),

    UNKNOWN(-1, -1, false, "UNKN ");

    private int mValue;
    private int mMessageLength;
    private boolean mTrailingStatusDibit;
    private String mLabel;

    P25P1DataUnitID(int value, int length, boolean trailingStatusDibit, String label)
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
    public static P25P1DataUnitID fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return HEADER_DATA_UNIT;
            case 1:
                return UNKNOWN_1;
            case 2:
                return UNKNOWN_2;
            case 3:
                return TERMINATOR_DATA_UNIT;
            case 4:
                return UNKNOWN_4;
            case 5:
                return LOGICAL_LINK_DATA_UNIT_1;
            case 6:
                return VSELP1;
            case 7:
                return TRUNKING_SIGNALING_BLOCK_1;
            case 8:
                return UNKNOWN_8;
            case 9:
                return VSELP2;
            case 10:
                return LOGICAL_LINK_DATA_UNIT_2;
            case 11:
                return UNKNOWN_11;
            case 12:
                return PACKET_HEADER_DATA_UNIT;
            case 13:
                return UNKNOWN_13;
            case 14:
                return UNKNOWN_14;
            case 15:
                return TERMINATOR_DATA_UNIT_LINK_CONTROL;
            default:
                throw new IllegalArgumentException("Data Unit ID must be in range 0 - 15");
        }
    }
}
