/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import java.util.EnumSet;

/**
 * The Data Unit ID (DUID) is part of the Network ID (NID) field and indicates the type of message.
 */
public enum P25P1DataUnitID
{
    HEADER_DATA_UNIT(0, 678, true, "HDU  "),
    TERMINATOR_DATA_UNIT(3, 30, true, "TDU  "),
    LOGICAL_LINK_DATA_UNIT_1(5, 1568, true, "LDU1 "),
    TRUNKING_SIGNALING_BLOCK_1(7, 248, false, "TSBK1"),
    LOGICAL_LINK_DATA_UNIT_2(10, 1568, true, "LDU2 "),
    PACKET_DATA_UNIT_1(12, 1200, false, "PDU"),
    TERMINATOR_DATA_UNIT_LINK_CONTROL(15, 432, true, "TDULC"),

    //Set to length of LDU plus sync/nid so that next sync detect can force it to a best guess
    PLACEHOLDER(-1, 1800, false, "PLACEHOLDER"), //Originally: 1686

    //The following are not true data unit identifiers, rather they are used as identifiers
    ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL(-1, -1, false, "AMBTC"),
    IP_PACKET_DATA(-1, 0, false, "IPPKT"),
    PACKET_DATA_UNIT(-1, 196, false, "PDU  "),
    SUBNETWORK_DEPENDENT_CONVERGENCE_PROTOCOL(-1, 0, false, "SNDCP"),
    TRUNKING_SIGNALING_BLOCK_2(7, 464, false, "TSBK2"),
    TRUNKING_SIGNALING_BLOCK_3(7, 720, false, "TSBK3"),
    UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL(-1, -1, false, "UMBTC"),

    UNKNOWN(-1, -1, false, "UNKNOWN");

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

    public static final EnumSet<P25P1DataUnitID> VALID_PRIMARY_DUIDS = EnumSet.of(HEADER_DATA_UNIT, TERMINATOR_DATA_UNIT,
            LOGICAL_LINK_DATA_UNIT_1, LOGICAL_LINK_DATA_UNIT_2, PACKET_DATA_UNIT_1, PACKET_DATA_UNIT,
            TRUNKING_SIGNALING_BLOCK_1, TERMINATOR_DATA_UNIT_LINK_CONTROL);

    /**
     * Indicates if this DUID is a primary DUID carried by the NID.
     * @return true if a primary DUID.
     */
    public boolean isValidPrimaryDUID()
    {
        return VALID_PRIMARY_DUIDS.contains(this);
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
            case 3:
                return TERMINATOR_DATA_UNIT;
            case 5:
                return LOGICAL_LINK_DATA_UNIT_1;
            case 7:
                return TRUNKING_SIGNALING_BLOCK_1;
            case 10:
                return LOGICAL_LINK_DATA_UNIT_2;
            case 12:
                return PACKET_DATA_UNIT_1;
            case 15:
                return TERMINATOR_DATA_UNIT_LINK_CONTROL;
            default:
                return UNKNOWN;
        }
    }
}
