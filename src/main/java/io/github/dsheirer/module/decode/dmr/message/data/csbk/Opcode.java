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

package io.github.dsheirer.module.decode.dmr.message.data.csbk;

import io.github.dsheirer.module.decode.dmr.message.type.Vendor;
import java.util.EnumMap;
import java.util.EnumSet;
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
    STANDARD_FEATURE_NOT_SUPPORTED(Vendor.STANDARD, 3, "FEATURE NOT SUPPORTED"),
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
    STANDARD_NEGATIVE_ACKNOWLEDGE_RESPONSE(Vendor.STANDARD, 38, "NEGATIVE ACKNOWLEDGE RESPONSE"),
    STANDARD_ANNOUNCEMENT(Vendor.STANDARD, 40, "ANNOUNCEMENT"),
    STANDARD_MAINTENANCE(Vendor.STANDARD, 42, "MAINTENANCE"),
    STANDARD_CLEAR(Vendor.STANDARD, 46, "CLEAR"),
    STANDARD_PROTECT(Vendor.STANDARD, 47, "PROTECT"),
    STANDARD_PRIVATE_VOICE_CHANNEL_GRANT(Vendor.STANDARD, 48, "PRIVATE VOICE CHANNEL GRANT"),
    STANDARD_TALKGROUP_VOICE_CHANNEL_GRANT(Vendor.STANDARD, 49, "TALKGROUP VOICE CHANNEL GRANT"),
    STANDARD_BROADCAST_TALKGROUP_VOICE_CHANNEL_GRANT(Vendor.STANDARD, 50, "BROADCAST TALKGROUP VOICE CHANNEL GRANT"),
    STANDARD_PRIVATE_DATA_CHANNEL_GRANT_SINGLE_ITEM(Vendor.STANDARD, 51, "PRIVATE DATA CHANNEL GRANT SINGLE ITEM"),
    STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_SINGLE_ITEM(Vendor.STANDARD, 52, "TALKGROUP DATA CHANNEL GRANT SINGLE ITEM"),
    STANDARD_DUPLEX_PRIVATE_VOICE_CHANNEL_GRANT(Vendor.STANDARD, 53, "DUPLEX PRIVATE VOICE CHANNEL GRANT"),
    STANDARD_DUPLEX_PRIVATE_DATA_CHANNEL_GRANT(Vendor.STANDARD, 54, "DUPLEX PRIVATE DATA CHANNEL GRANT"),
    STANDARD_PRIVATE_DATA_CHANNEL_GRANT_MULTI_ITEM(Vendor.STANDARD, 55, "PRIVATE DATA CHANNEL GRANT MULTI ITEM"),
    STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_MULTI_ITEM(Vendor.STANDARD, 56, "TALKGROUP DATA CHANNEL GRANT MULTI ITEM"),
    STANDARD_MOVE_TSCC(Vendor.STANDARD, 57, "MOVE TSCC"),
    STANDARD_PREAMBLE(Vendor.STANDARD, 61, "PREAMBLE"),

    MOTOROLA_CONPLUS_NEIGHBOR_REPORT(Vendor.MOTOROLA_CONNECT_PLUS, 1, "NEIGHBOR REPORT"),
    MOTOROLA_CONPLUS_VOICE_CHANNEL_USER(Vendor.MOTOROLA_CONNECT_PLUS, 3, "VOICE CHANNEL USER"),
    MOTOROLA_CONPLUS_DATA_CHANNEL_GRANT(Vendor.MOTOROLA_CONNECT_PLUS, 6, "DATA CHANNEL GRANT"),
    MOTOROLA_CONPLUS_CSBKO_10(Vendor.MOTOROLA_CONNECT_PLUS, 10, "CSBKO 10"),
    MOTOROLA_CONPLUS_TERMINATE_CHANNEL_GRANT(Vendor.MOTOROLA_CONNECT_PLUS, 12, "TERMINATE CHANNEL GRANT"),
    MOTOROLA_CONPLUS_CSBKO_16(Vendor.MOTOROLA_CONNECT_PLUS, 16, "CSBKO 16"),
    MOTOROLA_CONPLUS_REGISTRATION_REQUEST(Vendor.MOTOROLA_CONNECT_PLUS, 17, "REGISTRATION REQUEST"),
    MOTOROLA_CONPLUS_REGISTRATION_RESPONSE(Vendor.MOTOROLA_CONNECT_PLUS, 18, "REGISTRATION RESPONSE"),
    MOTOROLA_CONPLUS_TALKGROUP_AFFILIATION(Vendor.MOTOROLA_CONNECT_PLUS, 24, "TALKGROUP AFFILIATION"),
    MOTOROLA_CONPLUS_DATA_WINDOW_ANNOUNCEMENT(Vendor.MOTOROLA_CONNECT_PLUS, 28, "ENHANCED DATA REVERT WINDOW ANNOUNCEMENT"),
    MOTOROLA_CONPLUS_DATA_WINDOW_GRANT(Vendor.MOTOROLA_CONNECT_PLUS, 29, "ENHANCED DATA REVERT WINDOW GRANT"),

    MOTOROLA_CAPMAX_ALOHA(Vendor.MOTOROLA_CAPACITY_PLUS, 25, "ALOHA"),

    MOTOROLA_CAPPLUS_CALL_ALERT(Vendor.MOTOROLA_CAPACITY_PLUS, 31, "CALL ALERT"),
    MOTOROLA_CAPPLUS_CALL_ALERT_ACK(Vendor.MOTOROLA_CAPACITY_PLUS, 32, "CALL ALERT ACK"),
    MOTOROLA_CAPPLUS_DATA_WINDOW_ANNOUNCEMENT(Vendor.MOTOROLA_CAPACITY_PLUS, 41, "ENHANCED DATA REVERT WINDOW ANNOUNCEMENT"),
    MOTOROLA_CAPPLUS_DATA_WINDOW_GRANT(Vendor.MOTOROLA_CAPACITY_PLUS, 42, "ENHANCED DATA REVERT WINDOW GRANT"),
    MOTOROLA_CAPPLUS_NEIGHBOR_REPORT(Vendor.MOTOROLA_CAPACITY_PLUS, 59, "NEIGHBOR REPORT"),
    MOTOROLA_CAPPLUS_CSBKO_60(Vendor.MOTOROLA_CAPACITY_PLUS, 60, "CSBKO 60"),
    MOTOROLA_CAPPLUS_PREAMBLE(Vendor.MOTOROLA_CAPACITY_PLUS, 61, "PREAMBLE"),
    MOTOROLA_CAPPLUS_SYSTEM_STATUS(Vendor.MOTOROLA_CAPACITY_PLUS, 62, "SYSTEM STATUS"),

    HYTERA_08_ACKNOWLEDGE(Vendor.HYTERA_8, 32, "HYTERA 08 ACKNOWLEDGE"),
    HYTERA_08_ANNOUNCEMENT(Vendor.HYTERA_8, 40, "HYTERA 08 ANNOUNCEMENT"),
    HYTERA_08_CSBKO_44(Vendor.HYTERA_8, 44, "HYTERA 08 CSBKO 44"),
    HYTERA_08_CSBKO_47(Vendor.HYTERA_8, 47, "HYTERA 08 CSBKO 47"),

    HYTERA_68_XPT_SITE_STATE(Vendor.HYTERA_68, 10, "HYTERA 68 XPT SITE STATE"),
    HYTERA_68_ALOHA(Vendor.HYTERA_68, 25, "HYTERA 68 ALOHA"),
    HYTERA_68_ACKNOWLEDGE(Vendor.HYTERA_68, 32, "HYTERA 68 ACKNOWLEDGE"),
    HYTERA_68_ANNOUNCEMENT(Vendor.HYTERA_68, 40, "HYTERA 68 ANNOUNCEMENT"),
    //Opcode 54 and/or 55 - See patent on Hytera call forwarding
    HYTERA_68_XPT_PREAMBLE(Vendor.HYTERA_68, 61, "HYTERA 68 XPT PREAMBLE"),
    HYTERA_68_CSBKO_62(Vendor.HYTERA_68, 62, "HYTERA 68 CSBKO 62"),


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
     * Indicates if this opcode is a data channel grant opcode
     */
    public boolean isDataChannelGrantOpcode()
    {
        return DATA_CHANNEL_GRANT_OPCODES.contains(this);
    }

    /**
     * Data Channel Grant opcodes
     */
    public static final EnumSet<Opcode> DATA_CHANNEL_GRANT_OPCODES = EnumSet.of(
        STANDARD_PRIVATE_DATA_CHANNEL_GRANT_MULTI_ITEM,
        STANDARD_PRIVATE_DATA_CHANNEL_GRANT_SINGLE_ITEM,
        STANDARD_DUPLEX_PRIVATE_DATA_CHANNEL_GRANT,
        STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_MULTI_ITEM,
        STANDARD_TALKGROUP_DATA_CHANNEL_GRANT_SINGLE_ITEM);


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
