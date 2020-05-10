/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.csbk;

import io.github.dsheirer.module.decode.dmr.message.type.Vendor;

import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Control Signalling Block (CSBK) Opcode enumeration
 *
 * ETSI TS 102 361-2 DMR Voice & Generic Services, Annex B
 * ETSI TS 102 361-4 DMR Trunking Protocol, Annex B
 */
public enum Opcode
{
    STANDARD_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST(Vendor.STANDARD, 4, "UNIT TO UNIT VOICE SERVICE REQUEST"),
    STANDARD_UNIT_TO_UNIT_VOICE_SERVICE_RESPONSE(Vendor.STANDARD, 5, "UNIT TO UNIT VOICE SERVICE RESPONSE"),
    STANDARD_CHANNEL_TIMING(Vendor.STANDARD, 7, "CHANNEL TIMING"),
    STANDARD_ALOHA(Vendor.STANDARD, 25, "ALOHA"),
    STANDARD_UNIFIED_DATA_TRANSPORT_OUTBOUND_HEADER(Vendor.STANDARD, 26, "UNIFIED DATA TRANSPORT OUTBOUND HEADER"),
    STANDARD_UNIFIED_DATA_TRANSPORT_INBOUND_HEADER(Vendor.STANDARD, 27, "UNIFIED DATA TRANSPORT OUTBOUND HEADER"),
    STANDARD_AHOY(Vendor.STANDARD, 28, "AHOY"),
    STANDARD_ACTIVATION(Vendor.STANDARD, 30, "ACTIVATION"),
    STANDARD_RANDOM_ACCESS_SERVICE_REQUEST(Vendor.STANDARD, 31, "RANDOM ACCESS SERVICE REQUEST"),
    STANDARD_ACKNOWLEDGE_RESPONSE_OUTBOUND_TSCC(Vendor.STANDARD, 32, "ACKNOWLEDGE RESPONSE OUTBOUND TSCC"),
    STANDARD_ACKNOWLEDGE_RESPONSE_INBOUND_TSCC(Vendor.STANDARD, 33, "ACKNOWLEDGE RESPONSE INBOUND TSCC"),
    STANDARD_ACKNOWLEDGE_RESPONSE_OUTBOUND_PAYLOAD(Vendor.STANDARD, 34, "ACKNOWLEDGE RESPONSE OUTBOUND PAYLOAD"),
    STANDARD_ACKNOWLEDGE_RESPONSE_INBOUND_PAYLOAD(Vendor.STANDARD, 35, "ACKNOWLEDGE RESPONSE INBOUND PAYLOAD"),
    STANDARD_UNIFIED_DATA_TRANSPORT_FOR_DGNA_OUTBOUND_HEADER(Vendor.STANDARD, 36, "UNIFIED DATA TRANSPORT OUTBOUND HEADER"),
    STANDARD_UNIFIED_DATA_TRANSPORT_FOR_DGNA_INBOUND_HEADER(Vendor.STANDARD, 37, "UNIFIED DATA TRANSPORT OUTBOUND HEADER"),
    STANDARD_ANNOUNCEMENT(Vendor.STANDARD, 40, "ANNOUNCEMENT"),
    STANDARD_NEGATIVE_ACKNOWLEDGE_RESPONSE(Vendor.STANDARD, 38, "NEGATIVE ACKNOWLEDGE RESPONSE"),
    STANDARD_MAINTENANCE(Vendor.STANDARD, 42, "MAINTENANCE"),
    STANDARD_CLEAR(Vendor.STANDARD, 46, "CLEAR"),
    STANDARD_PROTECT(Vendor.STANDARD, 47, "PROTECT"),
    STANDARD_PRIVATE_VOICE_CHANNEL_GRANT(Vendor.STANDARD, 48, "PRIVATE VOICE CHANNEL GRANT"),
    STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT(Vendor.STANDARD, 49, "TALKGROUP VOICE CHANNEL GRANT"),
    STANDARD_PRIVATE_BROADCAST_VOICE_CHANNEL_GRANT(Vendor.STANDARD, 50, "PRIVATE BROADCAST VOICE CHANNEL GRANT"),
    STANDARD_PRIVATE_DATA_CHANNEL_GRANT_SINGLE_ITEM(Vendor.STANDARD, 51, "PRIVATE DATA CHANNEL GRANT SINGLE ITEM"),
    STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_SINGLE_ITEM(Vendor.STANDARD, 52, "TALKGROUP DATA CHANNEL GRANT SINGLE ITEM"),
    STANDARD_DUPLEX_PRIVATE_VOICE_CHANNEL_GRANT(Vendor.STANDARD, 53, "DUPLEX PRIVATE VOICE CHANNEL GRANT"),
    STANDARD_DUPLEX_PRIVATE_DATA_CHANNEL_GRANT(Vendor.STANDARD, 54, "DUPLEX PRIVATE DATA CHANNEL GRANT"),
    STANDARD_PRIVATE_DATA_CHANNEL_GRANT_MULTI_ITEM(Vendor.STANDARD, 55, "PRIVATE DATA CHANNEL GRANT SINGLE ITEM"),
    STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_MULTI_ITEM(Vendor.STANDARD, 56, "TALKGROUP DATA CHANNEL GRANT SINGLE ITEM"),
    STANDARD_MOVE_PDUS(Vendor.STANDARD, 57, "MOVE PDUS"),
    STANDARD_PREAMBLE(Vendor.STANDARD, 61, "PREAMBLE"),

    MOTOROLA_CONPLUS_NEIGHBOR_REPORT(Vendor.MOTOROLA_CONNECT_PLUS, 1, "NEIGHBOR REPORT"),
    MOTOROLA_CONPLUS_VOICE_CHANNEL_USER(Vendor.MOTOROLA_CONNECT_PLUS, 3, "CHANNEL GRANT"),
    //Opcode 6: Data Channel Grant, like opcode 3
    //Opcode 6: https://forums.radioreference.com/threads/understanding-connect-plus-trunking.213131/page-7
    //Opcode 12: https://forums.radioreference.com/threads/understanding-connect-plus-trunking.213131/page-6
    //Opcode 17: https://forums.radioreference.com/threads/understanding-connect-plus-trunking.213131/page-6
    //Opcode 18: https://forums.radioreference.com/threads/understanding-connect-plus-trunking.213131/page-6
    //Opcode 24: https://forums.radioreference.com/threads/understanding-connect-plus-trunking.213131/page-6
    MOTOROLA_CONPLUS_UNKNOWN_28(Vendor.MOTOROLA_CONNECT_PLUS, 28, "UNKNOWN 28"),

    MOTOROLA_CAPPLUS_SYSTEM_STATUS(Vendor.MOTOROLA_CAPACITY_PLUS, 62, "SYSTEM STATUS"),

    UNKNOWN(Vendor.UNKNOWN, -1, "UNKNOWN");

    private Vendor mVendor;
    private int mValue;
    private String mLabel;

    Opcode(Vendor vendor, int value, String label)
    {
        mVendor = vendor;
        mValue = value;
        mLabel = label;
    }

    /**
     * Vendor for this opcode
     */
    public Vendor getVendor()
    {
        return mVendor;
    }

    /**
     * Numeric value for the opcode
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Pretty label for the opcode
     */
    public String getLabel()
    {
        return mLabel;
    }

    @Override
    public String toString()
    {
        return getLabel();
    }

    /**
     * Lookup map of vendors and opcode value to opcodes
     */
    private static final Map<Vendor,Map<Integer,Opcode>> LOOKUP_MAP = new EnumMap<>(Vendor.class);

    static
    {
        for(Opcode opcode : Opcode.values())
        {
            if(LOOKUP_MAP.containsKey(opcode.getVendor()))
            {
                LOOKUP_MAP.get(opcode.getVendor()).put(opcode.getValue(), opcode);
            }
            else
            {
                Map<Integer,Opcode> map = new TreeMap<>();
                map.put(opcode.getValue(), opcode);
                LOOKUP_MAP.put(opcode.getVendor(), map);
            }
        }
    }

    /**
     * Lookup the opcode from the value for the specified vendor
     */
    public static Opcode fromValue(int value, Vendor vendor)
    {
        if(LOOKUP_MAP.containsKey(vendor))
        {
            Map<Integer,Opcode> opcodeMap = LOOKUP_MAP.get(vendor);

            if(opcodeMap.containsKey(value))
            {
                return opcodeMap.get(value);
            }
        }

        return UNKNOWN;
    }
}
