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
    HEADER_DATA_UNIT(0, 648, 11, 10, "HDU  "),
    TERMINATOR_DATA_UNIT(3, 0, 2,  28,"TDU  "),
    LOGICAL_LINK_DATA_UNIT_1(5, 1568, 24, 0, "LDU1 "),
    TRUNKING_SIGNALING_BLOCK_1(7, 196, 5, 42, "TSBK1"),
    LOGICAL_LINK_DATA_UNIT_2(10, 1568, 24, 0,  "LDU2 "),
    PACKET_DATA_UNIT(12, 196, 5, 42, "PDU"),
    TERMINATOR_DATA_UNIT_LINK_CONTROL(15, 288, 6, 20, "TDULC"),

    //Set to 2x dibits longer than longest (ie LDU) message type so we can force a detect if it's followed by a sync.
    PLACE_HOLDER(-1, 1572, 24, 0, "PLACEHOLDER"), //Originally: 1686

    //The following are not true data unit identifiers, rather they are used as identifiers
    ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL(-1, -1, 0, 0, "AMBTC"),
    IP_PACKET_DATA(-1, 0, 0, 0, "IPPKT"),
    PACKET_DATA_UNIT_BLOCK_1(-1, 196 * 2, 8, 56, "PDU"),
    PACKET_DATA_UNIT_BLOCK_2(-1, 196 * 3, 10, 0, "PDU"),
    PACKET_DATA_UNIT_BLOCK_3(-1, 196 * 4, 13, 14, "PDU"),
    PACKET_DATA_UNIT_BLOCK_4(-1, 196 * 5, 16, 28, "PDU"),
    PACKET_DATA_UNIT_BLOCK_5(-1, 196 * 6, 19, 42, "PDU"),
    SUBNETWORK_DEPENDENT_CONVERGENCE_PROTOCOL(-1, 0, 0, 0, "SNDCP"),
    TRUNKING_SIGNALING_BLOCK_2(7, 196 * 2, 8, 56, "TSBK2"),
    TRUNKING_SIGNALING_BLOCK_3(7, 196 * 3, 10, 0,"TSBK3"),
    UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL(-1, -1, 0, 0, "UMBTC"),

    UNKNOWN(-1, -1, 0, 0, "UNKNOWN");

    private final int mValue;
    private final int mMessageLength;
    private final int mElapsedDibitLength;
    private final String mLabel;

    /**
     * Constructs a Data Unit ID (DUID) entry
     * @param value transmitted in the NID
     * @param messageLength in bits
     * @param statusDibits count
     * @param nullBits trailing null bits used to pad the message length to the final status dibit
     * @param label for the DUID
     */
    P25P1DataUnitID(int value, int messageLength, int statusDibits, int nullBits, String label)
    {
        mValue = value;
        mMessageLength = messageLength + nullBits;
        //Elapsed dibit length starts with: sync(24) + nid(32) = 56 dibits
        mElapsedDibitLength = 56 + (messageLength / 2) + statusDibits + (nullBits / 2);
        mLabel = label;
    }

    public static final EnumSet<P25P1DataUnitID> VALID_PRIMARY_DUIDS = EnumSet.of(HEADER_DATA_UNIT, TERMINATOR_DATA_UNIT,
            LOGICAL_LINK_DATA_UNIT_1, LOGICAL_LINK_DATA_UNIT_2, PACKET_DATA_UNIT, PACKET_DATA_UNIT_BLOCK_1,
            TRUNKING_SIGNALING_BLOCK_1, TERMINATOR_DATA_UNIT_LINK_CONTROL);

    public static final EnumSet<P25P1DataUnitID> TSBK_DATA_UNITS = EnumSet.of(TRUNKING_SIGNALING_BLOCK_1,
            TRUNKING_SIGNALING_BLOCK_2, TRUNKING_SIGNALING_BLOCK_3);

    /**
     * Indicates if this DUID is a primary DUID carried by the NID.
     * @return true if a primary DUID.
     */
    public boolean isValidPrimaryDUID()
    {
        return VALID_PRIMARY_DUIDS.contains(this);
    }

    /**
     * Indicates if this DUID is a TSBK 1, 2, or 3
     */
    public boolean isTSBK()
    {
        return TSBK_DATA_UNITS.contains(this);
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
     * Elapsed dibit length from sync detection to next sync detection for this DUID
     * @return elapsed dibit length.
     */
    public int getElapsedDibitLength()
    {
        return mElapsedDibitLength;
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
        return false;
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
                return PACKET_DATA_UNIT;
            case 15:
                return TERMINATOR_DATA_UNIT_LINK_CONTROL;
            default:
                return UNKNOWN;
        }
    }
}
