/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

/**
 * MAC opcode is used with MAC_IDLE, MAC_ACTIVE and MAC_HANGTIME PDU format messages
 */
public enum MacOpcode
{
    TDMA_0_NULL_INFORMATION_MESSAGE("NULL INFORMATION", -1),
    TDMA_1_GROUP_VOICE_CHANNEL_USER_ABBREVIATED("GROUP VOICE CHANNEL USER ABBREVIATED", 7),
    TDMA_2_UNIT_TO_UNIT_VOICE_CHANNEL_USER("UNIT-TO-UNIT VOICE CHANNEL USER", 8),
    TDMA_3_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER("TELEPHONE INTERCONNECT VOICE CHANNEL USER", 7),
    TDMA_5_GROUP_VOICE_CHANNEL_GRANT_UPDATE("TDMA GROUP VOICE CHANNEL GRANT UPDATE", 16),
    TDMA_17_INDIRECT_GROUP_PAGING("INDIRECT GROUP PAGING", Integer.MIN_VALUE),
    TDMA_18_INDIVIDUAL_PAGING_MESSAGE_WITH_PRIORITY("INDIVIDUAL PAGING MESSAGE WITH PRIORITY", Integer.MIN_VALUE),
    TDMA_33_GROUP_VOICE_CHANNEL_USER_EXTENDED("GROUP VOICE CHANNEL USER EXTENDED", 14),
    TDMA_34_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED("UNIT-TO-UNIT VOICE CHANNEL USER EXTENDED", 15),
    TDMA_37_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT("TDMA GROUP VOICE CHANNEL GRANT UPDATE EXPLICIT", 15),
    TDMA_48_POWER_CONTROL_SIGNAL_QUALITY("POWER CONTROL SIGNAL QUALITY", 5),
    TDMA_49_MAC_RELEASE("MAC RELEASE", 7),
    TDMA_PARTITION_0_UNKNOWN_OPCODE("UNKNOWN TDMA OPCODE", -1),

    PHASE1_64_GROUP_VOICE_CHANNEL_GRANT_ABBREVIATED("GROUP VOICE CHANNEL GRANT ABBREVIATED", 9),
    PHASE1_65_GROUP_VOICE_SERVICE_REQUEST("GROUP VOICE SERVICE REQUEST", 7),
    PHASE1_66_GROUP_VOICE_CHANNEL_GRANT_UPDATE("GROUP VOICE CHANNEL GRANT UPDATE", 9),
    PHASE1_68_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_ABBREVIATED("UNIT-TO-UNIT VOICE CHANNEL GRANT ABBREVIATED", 9),
    PHASE1_69_UNIT_TO_UNIT_ANSWER_REQUEST_ABBREVIATED("UNIT-TO-UNIT ANSWER REQUEST ABBREVIATED", 8),
    PHASE1_72_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED("UNIT-TO-UNIT VOICE CHANNEL GRANT UPDATE ABBREVIATED", 9),
    PHASE1_76_TELEPHONE_INTERCONNECT_ANSWER_REQUEST("TELEPHONE INTERCONNECT_ANSWER REQUEST", 9),
    PHASE1_84_SNDCP_DATA_CHANNEL_GRANT("SNDCP DATA CHANNEL GRANT", 9),
    PHASE1_85_SNDCP_DATA_PAGE_REQUEST("SNDCP DATA PAGE REQUEST", 7),
    PHASE1_86_SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT("SNDCP DATA CHANNEL ANNOUNCEMENT EXPLICIT", 9),
    PHASE1_90_STATUS_QUERY_ABBREVIATED("STATUS QUERY ABBREVIATED", 7),
    PHASE1_93_RADIO_UNIT_MONITOR_COMMAND("RADIO UNIT MONITOR COMMAND", 8),
    PHASE1_94_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_ABBREVIATED("RADIO UNIT MONITOR ENHANCED COMMAND ABBREVIATED", 14),
    PHASE1_95_CALL_ALERT_ABBREVIATED("CALL ALERT ABBREVIATED", 7),
    PHASE1_96_ACK_RESPONSE("ACK RESPONSE", 9),
    PHASE1_97_QUEUED_RESPONSE("QUEUED RESPONSE", 9),
    PHASE1_100_EXTENDED_FUNCTION_COMMAND("EXTENDED FUNCTION COMMAND", 9),
    PHASE1_103_DENY_RESPONSE("DENY RESPONSE", 9),
    PHASE1_106_GROUP_AFFILIATION_QUERY_ABBREVIATED("GROUP AFFILIATION QUERY ABBREVIATED", 7),
    PHASE1_109_UNIT_REGISTRATION_COMMAND_ABBREVIATED("UNIT REGISTRATION COMMAND ABBREVIATED", 7),
    PHASE1_115_IDENTIFIER_UPDATE_TDMA("IDENTIFIER UPDATE TDMA", 9),
    PHASE1_116_IDENTIFIER_UPDATE_V_UHF("IDENTIFIER UPDATE V/UHF", 9),
    PHASE1_117_TIME_AND_DATE_ANNOUNCEMENT("TIME AND DATE ANNOUNCEMENT", 9),
    PHASE1_120_SYSTEM_SERVICE_BROADCAST("SYSTEM SERVICE BROADCAST", 9),
    PHASE1_121_SECONDARY_CONTROL_CHANNEL_BROADCAST("SECONDARY CONTROL CHANNEL BROADCAST", 9),
    PHASE1_122_RFSS_STATUS_BROADCAST_ABBREVIATED("RFSS STATUS BROADCAST ABBREVIATED", 9),
    PHASE1_123_NETWORK_STATUS_BROADCAST_ABBREVIATED("NETWORK STATUS BROADCAST ABBREVIATED", 11),
    PHASE1_124_ADJACENT_STATUS_BROADCAST_ABBREVIATED("ADJACENT STATUS BROADCAST ABBREVIATED", 9),
    PHASE1_125_IDENTIFIER_UPDATE("IDENTIFIER UPDATE", 9),
    PHASE1_PARTITION_1_UNKNOWN_OPCODE("UNKNOWN PHASE 1 OPCODE", -1),

    VENDOR_PARTITION_2_UNKNOWN_OPCODE("UNKNOWN VENDOR OPCODE", -1),

    PHASE1_192_GROUP_VOICE_CHANNEL_GRANT_EXTENDED("GROUP VOICE CHANNEL GRANT EXTENDED", 11),
    PHASE1_195_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT("GROUP VOICE CHANNEL GRANT UPDATE EXPLICIT", 8),
    PHASE1_196_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_ABBREVIATED("UNIT-TO-UNIT VOICE CHANNEL GRANT ABBREVIATED", 15),
    PHASE1_197_UNIT_TO_UNIT_ANSWER_REQUEST_EXTENDED("UNIT-TO-UNIT ANSWER REQUEST EXTENDED", 12),
    PHASE1_198_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED("UNIT-TO-UNIT VOICE CHANNEL GRANT EXTENDED", 15),
    PHASE1_218_STATUS_QUERY_EXTENDED("STATUS QUERY EXTENDED", 11),
    PHASE1_223_CALL_ALERT_EXTENDED("CALL ALERT EXTENDED", 11),
    PHASE1_233_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT("SECONDARY CONTROL CHANNEL BROADCAST EXPLICIT", 8),
    PHASE1_234_GROUP_AFFILIATION_QUERY_EXTENDED("GROUP AFFILIATION QUERY EXTENDED", 11),
    PHASE1_250_RFSS_STATUS_BROADCAST_EXTENDED("RFSS STATUS BROADCAST EXTENDED", 11),
    PHASE1_251_NETWORK_STATUS_BROADCAST_EXTENDED("NETWORK STATUS BROADCAST EXTENDED", 13),
    PHASE1_252_ADJACENT_STATUS_BROADCAST_EXTENDED("ADJACENT STATUS BROADCAST EXTENDED", 11),
    PHASE1_EXTENDED_PARTITION_3_UNKNOWN_OPCODE("UNKNOWN EXTENDED PHASE 1 OPCODE", 1),

    UNKNOWN("UNKNOWN", -1);

    private String mLabel;
    private int mLength;

    MacOpcode(String label, int length)
    {
        mLabel = label;
        mLength = length;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Length of the message in octets/bytes for the opcode
     */
    public int getLength()
    {
        return mLength;
    }

    /**
     * Indicates if the opcode is for a variable-length message
     */
    public boolean isVariableLength()
    {
        return getLength() == Integer.MIN_VALUE;
    }

    /**
     * Lookup the MAC opcode from an integer value
     */
    public static MacOpcode fromValue(int value)
    {
        if(0 <= value && value <= 63)
        {
            switch(value)
            {
                case 0:
                    return TDMA_0_NULL_INFORMATION_MESSAGE;
                case 1:
                    return TDMA_1_GROUP_VOICE_CHANNEL_USER_ABBREVIATED;
                case 2:
                    return TDMA_2_UNIT_TO_UNIT_VOICE_CHANNEL_USER;
                case 3:
                    return TDMA_3_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER;
                case 5:
                    return TDMA_5_GROUP_VOICE_CHANNEL_GRANT_UPDATE;
                case 17:
                    return TDMA_17_INDIRECT_GROUP_PAGING;
                case 18:
                    return TDMA_18_INDIVIDUAL_PAGING_MESSAGE_WITH_PRIORITY;
                case 33:
                    return TDMA_33_GROUP_VOICE_CHANNEL_USER_EXTENDED;
                case 34:
                    return TDMA_34_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED;
                case 37:
                    return TDMA_37_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT;
                case 48:
                    return TDMA_48_POWER_CONTROL_SIGNAL_QUALITY;
                case 49:
                    return TDMA_49_MAC_RELEASE;
            }

            return TDMA_PARTITION_0_UNKNOWN_OPCODE;
        }
        else if(64 <= value && value <= 127)
        {
            switch(value)
            {
                case 64:
                    return PHASE1_64_GROUP_VOICE_CHANNEL_GRANT_ABBREVIATED;
                case 65:
                    return PHASE1_65_GROUP_VOICE_SERVICE_REQUEST;
                case 66:
                    return PHASE1_66_GROUP_VOICE_CHANNEL_GRANT_UPDATE;
                case 68:
                    return PHASE1_68_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_ABBREVIATED;
                case 69:
                    return PHASE1_69_UNIT_TO_UNIT_ANSWER_REQUEST_ABBREVIATED;
                case 72:
                    return PHASE1_72_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED;
                case 76:
                    return PHASE1_76_TELEPHONE_INTERCONNECT_ANSWER_REQUEST;
                case 84:
                    return PHASE1_84_SNDCP_DATA_CHANNEL_GRANT;
                case 85:
                    return PHASE1_85_SNDCP_DATA_PAGE_REQUEST;
                case 86:
                    return PHASE1_86_SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT;
                case 90:
                    return PHASE1_90_STATUS_QUERY_ABBREVIATED;
                case 93:
                    return PHASE1_93_RADIO_UNIT_MONITOR_COMMAND;
                case 94:
                    return PHASE1_94_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_ABBREVIATED;
                case 95:
                    return PHASE1_95_CALL_ALERT_ABBREVIATED;
                case 96:
                    return PHASE1_96_ACK_RESPONSE;
                case 97:
                    return PHASE1_97_QUEUED_RESPONSE;
                case 100:
                    return PHASE1_100_EXTENDED_FUNCTION_COMMAND;
                case 103:
                    return PHASE1_103_DENY_RESPONSE;
                case 106:
                    return PHASE1_106_GROUP_AFFILIATION_QUERY_ABBREVIATED;
                case 109:
                    return PHASE1_109_UNIT_REGISTRATION_COMMAND_ABBREVIATED;
                case 115:
                    return PHASE1_115_IDENTIFIER_UPDATE_TDMA;
                case 116:
                    return PHASE1_116_IDENTIFIER_UPDATE_V_UHF;
                case 117:
                    return PHASE1_117_TIME_AND_DATE_ANNOUNCEMENT;
                case 120:
                    return PHASE1_120_SYSTEM_SERVICE_BROADCAST;
                case 121:
                    return PHASE1_121_SECONDARY_CONTROL_CHANNEL_BROADCAST;
                case 122:
                    return PHASE1_122_RFSS_STATUS_BROADCAST_ABBREVIATED;
                case 123:
                    return PHASE1_123_NETWORK_STATUS_BROADCAST_ABBREVIATED;
                case 124:
                    return PHASE1_124_ADJACENT_STATUS_BROADCAST_ABBREVIATED;
                case 125:
                    return PHASE1_125_IDENTIFIER_UPDATE;
            }

            return PHASE1_PARTITION_1_UNKNOWN_OPCODE;
        }
        else if(128 <= value && value <= 191)
        {
            return VENDOR_PARTITION_2_UNKNOWN_OPCODE;
        }
        else if(192 <= value && value <= 255)
        {
            switch(value)
            {
                case 192:
                    return PHASE1_192_GROUP_VOICE_CHANNEL_GRANT_EXTENDED;
                case 195:
                    return PHASE1_195_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT;
                case 196:
                    return PHASE1_196_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_ABBREVIATED;
                case 197:
                    return PHASE1_197_UNIT_TO_UNIT_ANSWER_REQUEST_EXTENDED;
                case 198:
                    return PHASE1_198_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED;
                case 218:
                    return PHASE1_218_STATUS_QUERY_EXTENDED;
                case 233:
                    return PHASE1_233_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT;
                case 234:
                    return PHASE1_234_GROUP_AFFILIATION_QUERY_EXTENDED;
                case 250:
                    return PHASE1_250_RFSS_STATUS_BROADCAST_EXTENDED;
                case 251:
                    return PHASE1_251_NETWORK_STATUS_BROADCAST_EXTENDED;
                case 252:
                    return PHASE1_252_ADJACENT_STATUS_BROADCAST_EXTENDED;
            }

            return PHASE1_EXTENDED_PARTITION_3_UNKNOWN_OPCODE;
        }

        return UNKNOWN;
    }
}
