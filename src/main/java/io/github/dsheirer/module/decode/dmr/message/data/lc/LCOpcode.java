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

package io.github.dsheirer.module.decode.dmr.message.data.lc;

import io.github.dsheirer.module.decode.dmr.message.type.Vendor;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * Full and Short Link Control Opcode enumeration
 * ETSI TS 102 361-2 DMR Voice & Generic Services, Annex B.1 & B.2
 * ETSI TS 102 361-4 DMR Trunking Protocol, Annex B.1 & B.2
 */
public enum LCOpcode
{
    FULL_STANDARD_GROUP_VOICE_CHANNEL_USER(Vendor.STANDARD,true, 0, "GROUP VOICE CHANNEL USER"),
    FULL_STANDARD_UNIT_TO_UNIT_VOICE_CHANNEL_USER(Vendor.STANDARD,true, 3, "UNIT-TO-UNIT VOICE CHANNEL USER"),
    FULL_STANDARD_TALKER_ALIAS_HEADER(Vendor.STANDARD,true, 4, "TALKER ALIAS HEADER"),
    FULL_STANDARD_TALKER_ALIAS_BLOCK_1(Vendor.STANDARD,true, 5, "TALKER ALIAS BLOCK 1"),
    FULL_STANDARD_TALKER_ALIAS_BLOCK_2(Vendor.STANDARD,true, 6, "TALKER ALIAS BLOCK 2"),
    FULL_STANDARD_TALKER_ALIAS_BLOCK_3(Vendor.STANDARD,true, 7, "TALKER ALIAS BLOCK 3"),
    FULL_STANDARD_TALKER_ALIAS_COMPLETE(Vendor.STANDARD, true, -1, "TALKER ALIAS COMPLETE"), //Not part of ICD
    FULL_STANDARD_GPS_INFO(Vendor.STANDARD,true, 8, "GPS INFO"),
    FULL_STANDARD_TERMINATOR_DATA(Vendor.STANDARD, true, 48, "TERMINATOR DATA"),
    FULL_STANDARD_UNKNOWN(Vendor.STANDARD,true, -1, "FULL UNKNOWN"),

    FULL_MOTOROLA_GROUP_VOICE_CHANNEL_USER(Vendor.MOTOROLA_CAPACITY_PLUS, true, 0, "GROUP VOICE CHANNEL USER"),
    FULL_CAPACITY_PLUS_WIDE_AREA_VOICE_CHANNEL_USER(Vendor.MOTOROLA_CAPACITY_PLUS, true, 4, "WAN GROUP VOICE CHANNEL USER"),

    FULL_CAPACITY_MAX_GROUP_VOICE_CHANNEL_USER(Vendor.MOTOROLA_CAPACITY_PLUS, true, 16, "CAPMAX GROUP VOICE CHANNEL USER"),
    FULL_CAPACITY_MAX_TALKER_ALIAS(Vendor.MOTOROLA_CAPACITY_PLUS, true, 20, "CAPMAX TALKER ALIAS"),
    FULL_CAPACITY_MAX_TALKER_ALIAS_CONTINUATION(Vendor.MOTOROLA_CAPACITY_PLUS, true, 21, "CAPMAX TALKER ALIAS CONTINUATION"),
    //Observed on Cap+ Multi-Site System during an encrypted voice call
    FULL_CAPACITY_PLUS_ENCRYPTED_VOICE_CHANNEL_USER(Vendor.MOTOROLA_CAPACITY_PLUS, true, 32, "ENCRYPTED VOICE CHANNEL USER"),
    //Observed on Cap+ Multi-Site System during an encrypted voice call
    FULL_ENCRYPTION_PARAMETERS(Vendor.MOTOROLA_CAPACITY_PLUS, true, 33, "ENCRYPTION PARAMETERS"),
    //Cap+ opcodes from https://forums.radioreference.com/threads/understanding-capacity-plus-trunking-some-more.452566/
    //FLCO 0: Group Call Maintenance
    //FLCO 3: Private Call Maintenance (TermLC)
    //FLCO 4: Group Call Grant
    //FLCO 7: Private Call Grant
    //FLCO 35: Private Call Maintenance (EMB)

    FULL_HYTERA_GROUP_VOICE_CHANNEL_USER(Vendor.HYTERA_68, true, 0, "HYTERA GROUP VOICE CHANNEL USER"),
    FULL_HYTERA_UNIT_TO_UNIT_VOICE_CHANNEL_USER(Vendor.HYTERA_68, true, 3, "HYTERA UNIT-TO-UNIT VOICE CHANNEL USER"),
    FULL_HYTERA_TALKER_ALIAS_HEADER(Vendor.HYTERA_68,true, 4, "HYTERA TALKER ALIAS HEADER"),
    FULL_HYTERA_TALKER_ALIAS_BLOCK_1(Vendor.HYTERA_68,true, 5, "HYTERA TALKER ALIAS BLOCK 1"),
    FULL_HYTERA_TALKER_ALIAS_BLOCK_2(Vendor.HYTERA_68,true, 6, "HYTERA TALKER ALIAS BLOCK 2"),
    FULL_HYTERA_TALKER_ALIAS_BLOCK_3(Vendor.HYTERA_68,true, 7, "HYTERA TALKER ALIAS BLOCK 3"),
    FULL_HYTERA_GPS_INFO(Vendor.HYTERA_68,true, 8, "HYTERA GPS INFO"),
    FULL_HYTERA_XPT_CHANNEL_GRANT(Vendor.HYTERA_68, true, 9, "HYTERA XPT CHANNEL GRANT"),
    FULL_HYTERA_TERMINATOR(Vendor.HYTERA_68, true, 48, "HYTERA TERMINATOR"),

    SHORT_STANDARD_NULL_MESSAGE(Vendor.STANDARD,false,0, "NULL MESSAGE"),
    SHORT_STANDARD_ACTIVITY_UPDATE(Vendor.STANDARD,false,1, "ACTIVITY UPDATE"),
    SHORT_STANDARD_CONTROL_CHANNEL_SYSTEM_PARAMETERS(Vendor.STANDARD,false,2, "CONTROL CHANNEL SYSTEM PARAMETERS"),
    SHORT_STANDARD_TRAFFIC_CHANNEL_SYSTEM_PARAMETERS(Vendor.STANDARD,false,3, "TRAFFIC CHANNEL SYSTEM PARAMETERS"),

    //CAP+ SLCO 15 = Rest Channel https://forums.radioreference.com/threads/understanding-capacity-plus-trunking.209318/page-7
    SHORT_CAPACITY_PLUS_REST_CHANNEL_NOTIFICATION(Vendor.MOTOROLA_CAPACITY_PLUS, false,15, "REST CHANNEL NOTIFICATION"),

    SHORT_STANDARD_XPT_CHANNEL(Vendor.STANDARD, false, 8, "STANDARD XPT CHANNEL"),
    SHORT_HYTERA_XPT_CHANNEL(Vendor.HYTERA_68, false, 8, "HYTERA XPT CHANNEL"),

    SHORT_CONNECT_PLUS_TRAFFIC_CHANNEL(Vendor.STANDARD, false, 9, "TRAFFIC CHANNEL INFO"),
    SHORT_CONNECT_PLUS_CONTROL_CHANNEL(Vendor.STANDARD, false, 10, "CONTROL CHANNEL INFO"),


    SHORT_STANDARD_UNKNOWN(Vendor.STANDARD,false,-1, "UNKNOWN");

    private static final EnumSet<LCOpcode> TALKER_ALIAS_OPCODES = EnumSet.of(FULL_STANDARD_TALKER_ALIAS_HEADER,
            FULL_STANDARD_TALKER_ALIAS_BLOCK_1, FULL_STANDARD_TALKER_ALIAS_BLOCK_2, FULL_STANDARD_TALKER_ALIAS_BLOCK_3,
            FULL_HYTERA_TALKER_ALIAS_HEADER, FULL_HYTERA_TALKER_ALIAS_BLOCK_1, FULL_HYTERA_TALKER_ALIAS_BLOCK_2,
            FULL_HYTERA_TALKER_ALIAS_BLOCK_3);

    private final Vendor mVendor;
    private final boolean mFull;
    private final int mValue;
    private final String mLabel;

    /**
     * Constructs an instance
     * @param vendor of the message
     * @param full to indicate if this is a full or short link control opcode
     * @param value of the opcode
     * @param label for the opcode
     */
    LCOpcode(Vendor vendor, boolean full, int value, String label)
    {
        mVendor = vendor;
        mFull = full;
        mValue = value;
        mLabel = label;
    }

    /**
     * Standard vendor, or the vendor of a custom opcode
     */
    public Vendor getVendor()
    {
        return mVendor;
    }

    /**
     * Indicates if this entry is a full link control opcode
     */
    public boolean isFull()
    {
        return mFull;
    }

    /**
     * Indicates if this entry is a short link control opcode
     */
    public boolean isShort()
    {
        return !mFull;
    }

    /**
     * Indicates if this entry is one of the four talker alias opcodes.
     */
    public boolean isTalkerAliasOpcode()
    {
        return TALKER_ALIAS_OPCODES.contains(this);
    }

    public int getValue()
    {
        return mValue;
    }

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
     * Lookup map of opcode values
     */
    private static final Map<Vendor,Map<Integer,LCOpcode>> FULL_LOOKUP_MAP = new EnumMap<>(Vendor.class);
    private static final Map<Vendor,Map<Integer,LCOpcode>> SHORT_LOOKUP_MAP = new EnumMap<>(Vendor.class);

    static
    {
        for(LCOpcode opcode: LCOpcode.values())
        {
            if(opcode.isFull())
            {
                if(FULL_LOOKUP_MAP.containsKey(opcode.getVendor()))
                {
                    FULL_LOOKUP_MAP.get(opcode.getVendor()).put(opcode.getValue(), opcode);
                }
                else
                {
                    Map<Integer,LCOpcode> map = new TreeMap<>();
                    map.put(opcode.getValue(), opcode);
                    FULL_LOOKUP_MAP.put(opcode.getVendor(), map);
                }
            }
            else
            {
                if(SHORT_LOOKUP_MAP.containsKey(opcode.getVendor()))
                {
                    SHORT_LOOKUP_MAP.get(opcode.getVendor()).put(opcode.getValue(), opcode);
                }
                else
                {
                    Map<Integer,LCOpcode> map = new TreeMap<>();
                    map.put(opcode.getValue(), opcode);
                    SHORT_LOOKUP_MAP.put(opcode.getVendor(), map);
                }
            }
        }
    }

    /**
     * Lookup the opcode from the value
     */
    public static LCOpcode fromValue(boolean full, int value, Vendor vendor)
    {
        if(full)
        {
            LCOpcode fullOpcode = FULL_STANDARD_UNKNOWN;
            Map<Integer,LCOpcode> map = FULL_LOOKUP_MAP.get(vendor);
            if(map != null && map.containsKey(value))
            {
                fullOpcode = map.get(value);
            }
            return fullOpcode;
        }
        else
        {
            LCOpcode shortOpcode = SHORT_STANDARD_UNKNOWN;
            Map<Integer,LCOpcode> map = SHORT_LOOKUP_MAP.get(vendor);
            if(map != null && map.containsKey(value))
            {
                shortOpcode = map.get(value);
            }
            return shortOpcode;
        }
    }
}
