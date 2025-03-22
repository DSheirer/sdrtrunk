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

package io.github.dsheirer.module.decode.p25.phase1.message.lc;

import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.EnumSet;
import java.util.logging.Logger;

/**
 * Opcodes used for Link Control Words.
 */
public enum LinkControlOpcode
{
    GROUP_VOICE_CHANNEL_USER("GROUP VOICE CHANNEL USER", 0),
    RESERVED_01("RESERVED-01", 1),
    GROUP_VOICE_CHANNEL_UPDATE("GROUP VOICE CHANNEL UPDATE", 2),
    UNIT_TO_UNIT_VOICE_CHANNEL_USER("UNIT-TO-UNIT VOICE CHANNEL USER", 3),
    GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT("GROUP VOICE CHANNEL UPDATE EXPLICIT", 4),
    UNIT_TO_UNIT_ANSWER_REQUEST("UNIT-TO-UNIT ANSWER REQUEST", 5),
    TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER("TELEPHONE INTERCONNECT VOICE CHANNEL USER", 6),
    TELEPHONE_INTERCONNECT_ANSWER_REQUEST("TELEPHONE INTERCONNECT ANSWER REQUEST", 7),
    RESERVED_08("RESERVED-08", 8),
    SOURCE_ID_EXTENSION("SOURCE ID EXTENSION", 9),
    UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED("UNIT-TO-UNIT VOICE CHANNEL USER EXTENDED", 10),
    RESERVED_0B("RESERVED-0B", 11),
    RESERVED_0C("RESERVED-0C", 12),
    RESERVED_0D("RESERVED-0D", 13),
    RESERVED_0E("RESERVED-0E", 14),
    CALL_TERMINATION_OR_CANCELLATION("CALL TERMINATION", 15),
    GROUP_AFFILIATION_QUERY("GROUP AFFILIATION QUERY", 16),
    UNIT_REGISTRATION_COMMAND("UNIT REGISTRATION COMMAND", 17),
    UNIT_AUTHENTICATION_COMMAND("UNIT AUTHENTICATION COMMAND", 18),
    STATUS_QUERY("STATUS QUERY", 19),
    STATUS_UPDATE("STATUS_UPDATE", 20),
    MESSAGE_UPDATE("MESSAGE UPDATE", 21),
    CALL_ALERT("CALL ALERT", 22),
    EXTENDED_FUNCTION_COMMAND("EXTENDED FUNCTION COMMAND", 23),
    CHANNEL_IDENTIFIER_UPDATE("CHANNEL IDENTIFIER UPDATE", 24),
    CHANNEL_IDENTIFIER_UPDATE_VU("CHANNEL IDENTIFIER UPDATE EXPLICIT", 25),
    STATUS_UPDATE_EXTENDED("STATUS UPDATE-SOURCE ID REQUIRED", 26),
    MESSAGE_UPDATE_EXTENDED("MESSAGE UPDATE-SOURCE ID REQUIRED", 27),
    EXTENDED_FUNCTION_COMMAND_EXTENDED("EXTENDED FUNCTION COMMAND-SOURCE ID REQUIRED", 28),
    RESERVED_1D("RESERVED-1D", 29),
    RESERVED_1E("RESERVED-1E", 30),
    RESERVED_1F("RESERVED-1F", 31),
    SYSTEM_SERVICE_BROADCAST("SYSTEM SERVICE BROADCAST", 32),
    SECONDARY_CONTROL_CHANNEL_BROADCAST("SECONDARY CONTROL CHANNEL BROADCAST", 33),
    ADJACENT_SITE_STATUS_BROADCAST("ADJACENT SITE STATUS", 34),
    RFSS_STATUS_BROADCAST("RFSS STATUS BROADCAST", 35),
    NETWORK_STATUS_BROADCAST("NET STATUS BROADCAST", 36),
    PROTECTION_PARAMETER_BROADCAST("ENCRYPTION PARAMETERS BROADCAST", 37),
    SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT("SECONDARY CONTROL CHANNEL BROADCAST EXPLICIT", 38),
    ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT("ADJACENT SITE STATUS EXPLICIT", 39),
    RFSS_STATUS_BROADCAST_EXPLICIT("RFSS STATUS BROADCAST EXPLICIT", 40),
    NETWORK_STATUS_BROADCAST_EXPLICIT("NETWORK STATUS BROADCAST EXPLICIT", 41),
    CONVENTIONAL_FALLBACK_INDICATION("CONVENTIONAL FALLBACK INDICATION", 42),
    RESERVED_2B("RESERVED-2B", 43),
    RESERVED_2C("RESERVED-2C", 44),
    RESERVED_2D("RESERVED-2D", 45),
    RESERVED_2E("RESERVED-2E", 46),
    RESERVED_2F("RESERVED-2F", 47),
    RESERVED_30("RESERVED-30", 48),
    RESERVED_31("RESERVED-31", 49),
    RESERVED_32("RESERVED-32", 50),
    RESERVED_33("RESERVED-33", 51),
    RESERVED_34("RESERVED-34", 52),
    RESERVED_35("RESERVED-35", 53),
    RESERVED_36("RESERVED-36", 54),
    RESERVED_37("RESERVED-37", 55),
    RESERVED_38("RESERVED-38", 56),
    RESERVED_39("RESERVED-39", 57),
    RESERVED_3A("RESERVED-3A", 58),
    RESERVED_3B("RESERVED-3B", 59),
    RESERVED_3C("RESERVED-3C", 60),
    RESERVED_3D("RESERVED-3D", 61),
    RESERVED_3E("RESERVED-3E", 62),
    RESERVED_3F("RESERVED-3F", 63),

    MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_USER("MOTOROLA GROUP REGROUP VOICE CHANNEL USER", 0),
    MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_UPDATE("MOTOROLA GROUP REGROUP VOICE CHANNEL UPDATE", 1),
    MOTOROLA_FAILSOFT("MOTOROLA FAILSOFT", 2),
    MOTOROLA_GROUP_REGROUP_ADD("MOTOROLA GROUP REGROUP ADD", 3),
    MOTOROLA_GROUP_REGROUP_DELETE("MOTOROLA GROUP REGROUP DELETE", 4),
    MOTOROLA_UNIT_GPS("MOTOROLA UNIT GPS", 6),
    MOTOROLA_EMERGENCY_ALARM_ACTIVATION("MOTOROLA EMERGENCY ALARM ACTIVATION", 10),
    MOTOROLA_TALK_COMPLETE("MOTOROLA TALK_COMPLETE", 15),
    MOTOROLA_TALKER_ALIAS_HEADER("MOTOROLA TALKER ALIAS HEADER", 21),
    MOTOROLA_TALKER_ALIAS_DATA_BLOCK("MOTOROLA TALKER ALIAS DATA BLOCK", 23),
    MOTOROLA_UNKNOWN("MOTOROLA UNKNOWN", -1),

    L3HARRIS_RETURN_TO_CONTROL_CHANNEL("UNKNOWN OPCODE 10", 10),
    L3HARRIS_TALKER_GPS_BLOCK1("TALKER GPS 1/2", 42),
    L3HARRIS_TALKER_GPS_BLOCK2("TALKER GPS 2/2", 43),
    L3HARRIS_TALKER_GPS_COMPLETE("TALKER GPS", -2),
    L3HARRIS_TALKER_ALIAS_BLOCK_1("TALKER ALIAS BLOCK 1", 50),
    L3HARRIS_TALKER_ALIAS_BLOCK_2("TALKER ALIAS BLOCK 2", 51),
    L3HARRIS_TALKER_ALIAS_BLOCK_3("TALKER ALIAS BLOCK 3", 52),
    L3HARRIS_TALKER_ALIAS_BLOCK_4("TALKER ALIAS BLOCK 4", 53),
    L3HARRIS_UNKNOWN("L3HARRIS UNKNOWN", -1),

    //This is used for reassembled talker aliases and is not an actual opcode
    TALKER_ALIAS_COMPLETE("TALKER ALIAS COMPLETE", -1),

    UNKNOWN("UNKNOWN", -1);

    private String mLabel;
    private int mCode;
    private static Logger LOGGER = Logger.getLogger(LinkControlOpcode.class.getName());

    /**
     * Constructor
     * @param label to display for the opcode
     * @param code value.
     */
    LinkControlOpcode(String label, int code)
    {
        mLabel = label;
        mCode = code;
    }

    /**
     * Command, response and status opcodes
     */
    public static final EnumSet<LinkControlOpcode> COMMAND_STATUS_OPCODES = EnumSet.of(UNIT_TO_UNIT_ANSWER_REQUEST,
            TELEPHONE_INTERCONNECT_ANSWER_REQUEST, CALL_TERMINATION_OR_CANCELLATION, GROUP_AFFILIATION_QUERY,
            UNIT_AUTHENTICATION_COMMAND, UNIT_REGISTRATION_COMMAND, STATUS_QUERY, STATUS_UPDATE, MESSAGE_UPDATE,
            EXTENDED_FUNCTION_COMMAND, CALL_ALERT);

    /**
     * Motorola Opcodes
     */
    public static final EnumSet<LinkControlOpcode> MOTOROLA_OPCODES =
            EnumSet.range(MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_USER, MOTOROLA_UNKNOWN);

    /**
     * L3Harris Opcodes
     */
    public static final EnumSet<LinkControlOpcode> L3HARRIS_OPCODES = EnumSet.of(L3HARRIS_RETURN_TO_CONTROL_CHANNEL, L3HARRIS_TALKER_GPS_BLOCK1, L3HARRIS_TALKER_GPS_BLOCK2, L3HARRIS_TALKER_ALIAS_BLOCK_1, L3HARRIS_TALKER_ALIAS_BLOCK_2,
            L3HARRIS_TALKER_ALIAS_BLOCK_3, L3HARRIS_TALKER_ALIAS_BLOCK_4, L3HARRIS_UNKNOWN);

    /**
     * Network/channel related opcodes
     */
    public static final EnumSet<LinkControlOpcode> NETWORK_OPCODES = EnumSet.of(CHANNEL_IDENTIFIER_UPDATE,
            CHANNEL_IDENTIFIER_UPDATE_VU, SYSTEM_SERVICE_BROADCAST, SECONDARY_CONTROL_CHANNEL_BROADCAST,
            SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT, ADJACENT_SITE_STATUS_BROADCAST,
            ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT, RFSS_STATUS_BROADCAST, RFSS_STATUS_BROADCAST_EXPLICIT,
            NETWORK_STATUS_BROADCAST, NETWORK_STATUS_BROADCAST_EXPLICIT, PROTECTION_PARAMETER_BROADCAST);

    /**
     * Voice/call opcodes
     */
    public static final EnumSet<LinkControlOpcode> VOICE_OPCODES = EnumSet.of(GROUP_VOICE_CHANNEL_USER,
            GROUP_VOICE_CHANNEL_UPDATE, GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT, UNIT_TO_UNIT_VOICE_CHANNEL_USER,
            TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER);

    /**
     * Indicates if the enumeration element is contained in one of the enumset groupings above.
     * @return true if the element is grouped.
     */
    public boolean isGrouped()
    {
        return COMMAND_STATUS_OPCODES.contains(this) || MOTOROLA_OPCODES.contains(this) ||
                NETWORK_OPCODES.contains(this) || VOICE_OPCODES.contains(this);
    }

    /**
     * Pretty label/string for the element
     * @return label
     */
    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Numeric value of the opcode.
     * @return code value.
     */
    public int getCode()
    {
        return mCode;
    }

    /**
     * Lookup method for finding an opcode for a given vendor and value combination.
     * @param value of the opcode
     * @param vendor for the opcode
     * @return matching element or UNKNOWN.
     */
    public static LinkControlOpcode fromValue(int value, Vendor vendor)
    {
        switch(vendor)
        {
            case STANDARD:
                if(0 <= value && value <= 63)
                {
                    return values()[value];
                }
                break;
            case HARRIS:
                switch(value)
                {
                    case 10:
                        return L3HARRIS_RETURN_TO_CONTROL_CHANNEL;
                    case 42:
                        return L3HARRIS_TALKER_GPS_BLOCK1;
                    case 43:
                        return L3HARRIS_TALKER_GPS_BLOCK2;
                    case 50:
                        return L3HARRIS_TALKER_ALIAS_BLOCK_1;
                    case 51:
                        return L3HARRIS_TALKER_ALIAS_BLOCK_2;
                    case 52:
                        return L3HARRIS_TALKER_ALIAS_BLOCK_3;
                    case 53:
                        return L3HARRIS_TALKER_ALIAS_BLOCK_4;
                    default:
                        return L3HARRIS_UNKNOWN;
                }
            case MOTOROLA:
                switch(value)
                {
                    case 0:
                        return MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_USER;
                    case 1:
                        return MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_UPDATE;
                    case 2:
                        return MOTOROLA_FAILSOFT;
                    case 3:
                        return MOTOROLA_GROUP_REGROUP_ADD;
                    case 4:
                        return MOTOROLA_GROUP_REGROUP_DELETE;
                    case 6:
                        return MOTOROLA_UNIT_GPS;
                    case 10:
                        return MOTOROLA_EMERGENCY_ALARM_ACTIVATION;
                    case 15:
                        return MOTOROLA_TALK_COMPLETE;
                    case 21:
                        return MOTOROLA_TALKER_ALIAS_HEADER;
                    case 23:
                        return MOTOROLA_TALKER_ALIAS_DATA_BLOCK;
                    default:
                        return MOTOROLA_UNKNOWN;
                }
        }

        if(0 <= value && value <= 63)
        {
            return values()[value];
        }

        return UNKNOWN;
    }
}
