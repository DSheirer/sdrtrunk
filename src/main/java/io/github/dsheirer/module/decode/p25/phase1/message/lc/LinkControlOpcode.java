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

package io.github.dsheirer.module.decode.p25.phase1.message.lc;

import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.EnumSet;

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
    RESERVED_09("RESERVED-09", 9),
    RESERVED_0A("RESERVED-0A", 10),
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
    CHANNEL_IDENTIFIER_UPDATE_EXPLICIT("CHANNEL IDENTIFIER UPDATE EXPLICIT", 25),
    RESERVED_1A("RESERVED-1A", 26),
    RESERVED_1B("RESERVED-1B", 27),
    RESERVED_1C("RESERVED-1C", 28),
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
    RESERVED_2A("RESERVED-2A", 42),
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

    MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_USER("PATCH GROUP VOICE CHANNEL USER", 0),
    MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_UPDATE("PATCH GROUP VOICE CHANNEL UPDATE", 1),
    MOTOROLA_PATCH_GROUP_ADD("PATCH GROUP ADD", 3),
    MOTOROLA_PATCH_GROUP_DELETE("PATCH GROUP DELETE", 4),
    MOTOROLA_UNIT_GPS("UNIT GPS", 6),
    MOTOROLA_TALK_COMPLETE("TALK_COMPLETE", 15),
    MOTOROLA_UNKNOWN("UNKNOWN", -1),

    UNKNOWN("UNKNOWN", -1);

    private String mLabel;
    private int mCode;

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
            EnumSet.range(MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_USER, MOTOROLA_UNKNOWN);


    /**
     * Network/channel related opcodes
     */
    public static final EnumSet<LinkControlOpcode> NETWORK_OPCODES = EnumSet.of(CHANNEL_IDENTIFIER_UPDATE,
            CHANNEL_IDENTIFIER_UPDATE_EXPLICIT, SYSTEM_SERVICE_BROADCAST, SECONDARY_CONTROL_CHANNEL_BROADCAST,
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
            case MOTOROLA:
                switch(value)
                {
                    case 0:
                        return MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_USER;
                    case 1:
                        return MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_UPDATE;
                    case 3:
                        return MOTOROLA_PATCH_GROUP_ADD;
                    case 4:
                        return MOTOROLA_PATCH_GROUP_DELETE;
                    case 6:
                        return MOTOROLA_UNIT_GPS;
                    case 15:
                        return MOTOROLA_TALK_COMPLETE;
                    default:
                        return MOTOROLA_UNKNOWN;
                }
            default:
                if(0 <= value && value <= 63)
                {
                    return values()[value];
                }
        }

        return UNKNOWN;
    }
}
