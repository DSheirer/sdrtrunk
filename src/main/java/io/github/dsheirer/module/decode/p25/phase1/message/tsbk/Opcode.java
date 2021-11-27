/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk;

import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.EnumSet;

public enum Opcode
{
    //Vendor: standard, Outbound Service Packet (OSP)
    OSP_GROUP_VOICE_CHANNEL_GRANT(0, "GRP_VCH_GRANT", "GROUP VOICE CHANNEL GRANT"),
    OSP_RESERVED_01(1, "RESERVED_01", "OSP RESERVED 1"),
    OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE(2, "GRP_VCH_GRNT_UPD", "GROUP VOICE CHANNEL GRANT UPDATE"),
    OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT(3, "GRP_VCH_GRNT_UPX", "GROUP VOICE CHANNEL GRANT UPDATE EXPLICIT"),
    OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT(4, "UU_VCH_GRANT", "UNIT-2-UNIT VOICE CHANNEL GRANT"),
    OSP_UNIT_TO_UNIT_ANSWER_REQUEST(5, "UU_ANS_REQ", "UNIT-2-UNIT ANSWER REQUEST"),
    OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE(6, "UU_VCH_GRANT_UPD", "UNIT-2-UNIT VOICE CHANNEL GRANT UPDATE"),
    OSP_RESERVED_07(7, "RESERVED_07", "OSP RESERVED 7"),
    OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT(8, "TEL_INT_VCH_GRNT", "TELEPHONE INTERCONNECT VOICE CHANNEL GRANT"),
    OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE(9, "TEL_INT_VCH_GRNU", "TELEPHONE INTERCONNECT VOICE CHANNEL GRANT UPDATE"),
    OSP_TELEPHONE_INTERCONNECT_ANSWER_REQUEST(10, "TEL_INT_ANS_RQST", "TELEPHONE INTERCONNECT ANSWER REQUEST"),
    OSP_RESERVED_0B(11, "RESERVED_0B", "OSP RESERVED 11"),
    OSP_RESERVED_0C(12, "RESERVED_0C", "OSP RESERVED 12"),
    OSP_RESERVED_0D(13, "RESERVED_0D", "OSP RESERVED 13"),
    OSP_RESERVED_0E(14, "RESERVED_0E", "OSP RESERVED 14"),
    OSP_RESERVED_0F(15, "RESERVED_0F", "OSP RESERVED 15"),
    OSP_INDIVIDUAL_DATA_CHANNEL_GRANT(16, "IND_DCH_GRANT", "INDIVIDUAL CHANNEL GRANT-OBSOLETE"),               //OBSOLETE
    OSP_GROUP_DATA_CHANNEL_GRANT(17, "GRP_DCH_GRANT", "GROUP DATA CHANNEL GRANT-OBSOLETE"),                    //OBSOLETE
    OSP_GROUP_DATA_CHANNEL_ANNOUNCEMENT(18, "GRP_DCH_ANNOUNCE", "GROUP DATA CHANNEL ANNOUNCEMENT-OBSOLETE"),          //OBSOLETE
    OSP_GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT(19, "GRP_DCH_ANNC_EXP", "GROUP DATA CHANNEL ANNOUNCEMENT EXPLICIT-OBSOLETE"), //OBSOLETE
    OSP_SNDCP_DATA_CHANNEL_GRANT(20, "SNDCP_DCH_GRANT", "SNDCP DATA CHANNEL GRANT"),
    OSP_SNDCP_DATA_PAGE_REQUEST(21, "SNDCP_DCH_PAG_RQ", "SNDCP DATA CHANNEL PAGE REQUEST"),
    OSP_SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT(22, "SNDCP_DCH_ANN_EX", "SNDCP DATA CHANNEL ANNOUNCEMENT EXPLICIT"),
    OSP_RESERVED_17(23, "RESERVED_17", "OSP RESERVED 23"),
    OSP_STATUS_UPDATE(24, "STATUS_UPDATE", "STATUS UPDATE"),
    OSP_RESERVED_19(25, "RESERVED_19", "OSP RESERVED 25"),
    OSP_STATUS_QUERY(26, "OSP_STATUS_QUERY", "STATUS QUERY"),
    OSP_RESERVED_1B(27, "RESERVED_1B", "OSP RESERVED 27"),
    OSP_MESSAGE_UPDATE(28, "MESSAGE_UPDATE", "MESSAGE UPDATE"),
    OSP_RADIO_UNIT_MONITOR_COMMAND(29, "RADIO_MONITR_CMD", "RADIO MONITOR COMMAND"),
    OSP_RESERVED_1E(30, "RESERVED_1E", "OSP RESERVED 30"),
    OSP_CALL_ALERT(31, "CALL_ALERT", "CALL ALERT"),
    OSP_ACKNOWLEDGE_RESPONSE(32, "ACK_RESPONSE_FNE", "ACKNOWLEDGE RESPONSE"),
    OSP_QUEUED_RESPONSE(33, "QUEUED_RESPONSE", "QUEUED RESPONSE"),
    OSP_RESERVED_22(34, "RESERVED_22", "OSP RESERVED 34"),
    OSP_RESERVED_23(35, "RESERVED_23", "OSP RESERVED 35"),
    OSP_EXTENDED_FUNCTION_COMMAND(36, "EXTNDED_FUNC_CMD", "EXTENDED FUNCTION COMMAND"),
    OSP_RESERVED_25(37, "RESERVED_25", "OSP RESERVED 37"),
    OSP_RESERVED_26(38, "RESERVED_26", "OSP RESERVED 38"),
    OSP_DENY_RESPONSE(39, "DENY_RESPONSE", "DENY RESPONSE"),
    OSP_GROUP_AFFILIATION_RESPONSE(40, "GRP_AFFIL_RESP", "GROUP AFFILIATION RESPONSE"),
    OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT(41, "SCCB_CCH_BCST_EX", "SECONDARY CONTROL CHANNEL BROADCAST EXPLICIT"),
    OSP_GROUP_AFFILIATION_QUERY(42, "GRP_AFFIL_QUERY", "GROUP AFFILIATION QUERY"),
    OSP_LOCATION_REGISTRATION_RESPONSE(43, "LOCN_REG_RESPONS", "LOCATION REGISTRATION RESPONSE"),
    OSP_UNIT_REGISTRATION_RESPONSE(44, "UNIT_REG_RESPONS", "UNIT REGISTRATION RESPONSE"),
    OSP_UNIT_REGISTRATION_COMMAND(45, "UNIT_REG_COMMAND", "UNIT REGISTRATION COMMAND"),
    OSP_AUTHENTICATION_COMMAND(46, "AUTH_COMMAND", "AUTHENTICATION COMMAND"),
    OSP_UNIT_DEREGISTRATION_ACKNOWLEDGE(47, "DE_REGIST_ACK", "DE-REGISTRATION ACKNOWLEDGE"),
    OSP_TDMA_SYNC_BROADCAST(48, "TDMA_SYNC_BCST", "TDMA SYNCHRONIZATION BROADCAST"),
    OSP_AUTHENTICATION_DEMAND(49, "AUTH_DEMAND", "AUTHENTICATION DEMAND"),
    OSP_AUTHENTICATION_FNE_RESPONSE(50, "AUTH_FNE_RESP", "AUTHENTICATION RESPONSE"),
    OSP_IDENTIFIER_UPDATE_TDMA(51, "IDEN_UPDATE_TDMA", "IDENTIFIER UPDATE TDMA"),
    OSP_IDENTIFIER_UPDATE_VHF_UHF_BANDS(52, "IDEN_UPDATE_VUHF", "IDENTIFIER UPDATE VHF-UHF"),
    OSP_TIME_DATE_ANNOUNCEMENT(53, "TIME_DATE_ANNOUN", "TIME AND DATE ANNOUNCEMENT"),
    OSP_ROAMING_ADDRESS_COMMAND(54, "ROAM_ADDR_CMD", "ROAMING ADDRESS COMMAND"),
    OSP_ROAMING_ADDRESS_UPDATE(55, "ROAM_ADDR_UPDATE", "ROAMING ADDRESS UPDATE"),
    OSP_SYSTEM_SERVICE_BROADCAST(56, "SYS_SVC_BCAST", "SYSTEM SERVICE BROADCAST"),
    OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST(57, "SEC_CCH_BROADCST", "SECONDARY CONTROL CHANNEL BROADCAST"),
    OSP_RFSS_STATUS_BROADCAST(58, "RFSS_STATUS_BCST", "RFSS STATUS BROADCAST"),
    OSP_NETWORK_STATUS_BROADCAST(59, "NET_STATUS_BCAST", "NETWORK STATUS BROADCAST"),
    OSP_ADJACENT_STATUS_BROADCAST(60, "ADJ SITE STATUS", "ADJACENT SITE STATUS"),
    OSP_IDENTIFIER_UPDATE(61, "IDEN_UPDATE", "IDENTIFIER UPDATE"),
    OSP_PROTECTION_PARAMETER_BROADCAST(62, "ENCRYPT_PAR_BCST", "ENCRYPTION PARAMENTERS BROADCAST"),
    OSP_PROTECTION_PARAMETER_UPDATE(63, "ENCRYPT_PAR_UPDT", "ENCRYPTION PARAMETERS UPDATE"),
    OSP_UNKNOWN(-1, "OSP UNKNOWN", "OSP UNKNOWN OPCODE"),

    //Vendor: standard, Inbound Service Packet (ISP)
    ISP_GROUP_VOICE_SERVICE_REQUEST(0, "GRP_V_REQ", "GROUP VOICE SERVICE REQUEST"),
    ISP_RESERVED_01(1, "RESERVED_01", "ISP RESERVED 1"),
    ISP_RESERVED_02(2, "RESERVED_02", "ISP RESERVED 2"),
    ISP_RESERVED_03(3, "RESERVED_03", "ISP RESERVED 3"),
    ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST(4, "UU_V_REQ", "UNIT-2-UNIT VOICE SERVICE REQUEST"),
    ISP_UNIT_TO_UNIT_ANSWER_RESPONSE(5, "UU_ANS_RSP", "UNIT-2-UNIT ANSWER RESPONSE"),
    ISP_RESERVED_06(6, "RESERVED_06", "ISP RESERVED 6"),
    ISP_RESERVED_07(7, "RESERVED_07", "ISP RESERVED 7"),
    ISP_TELEPHONE_INTERCONNECT_EXPLICIT_DIAL_REQUEST(8, "TELE_INT_DIAL_REQ", "TELEPHONE INTERCONNECT EXPLICIT DIAL REQUEST"),
    ISP_TELEPHONE_INTERCONNECT_PSTN_REQUEST(9, "TELE_INT_PSTN_REQ", "TELEPHONE INTERCONNECT PSTN REQUEST"),
    ISP_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE(10, "TELE_INT_ANS_RSP", "TELEPHONE INTERCONNECT ANSWER RESPONSE"),
    ISP_RESERVED_0B(11, "RESERVED_0B", "ISP RESERVED 11"),
    ISP_RESERVED_0C(12, "RESERVED_0C", "ISP RESERVED 12"),
    ISP_RESERVED_0D(13, "RESERVED_0D", "ISP RESERVED 13"),
    ISP_RESERVED_0E(14, "RESERVED_0E", "ISP RESERVED 14"),
    ISP_RESERVED_0F(15, "RESERVED_0F", "ISP RESERVED 15"),
    ISP_INDIVIDUAL_DATA_SERVICE_REQUEST(16, "IND_DATA_REQ", "INDIVIDUAL DATA SERVICE REQUEST-OBSOLETE"),  //OBSOLETE
    ISP_GROUP_DATA_SERVICE_REQUEST(17, "GRP_DATA_REQ", "GROUP DATA SERVICE REQUEST-OBSOLETE"),       //OBSOLETE
    ISP_SNDCP_DATA_CHANNEL_REQUEST(18, "SN-DATA_CHN_REQ", "SNDCP DATA CHANNEL REQUEST"),
    ISP_SNDCP_DATA_PAGE_RESPONSE(19, "SN-DATA_PAGE_RES", "SNDCP DATA PAGE REQUEST"),
    ISP_SNDCP_RECONNECT_REQUEST(20, "SN-REC_REQ", "SNDCP RECONNECT REQUEST"),
    ISP_RESERVED_15(21, "RESERVED_15", "ISP RESERVED 21"),
    ISP_RESERVED_16(22, "RESERVED_16", "ISP RESERVED 22"),
    ISP_RESERVED_17(23, "RESERVED_17", "ISP RESERVED 23"),
    ISP_STATUS_UPDATE_REQUEST(24, "STS_UPDT_REQ", "STATUS UPDATE REQUEST"),
    ISP_STATUS_QUERY_RESPONSE(25, "STS_Q_RSP", "STATUS QUERY RESPONSE"),
    ISP_STATUS_QUERY_REQUEST(26, "STS_Q_REQ", "STATUS QUERY REQUEST"),
    ISP_RESERVED_1B(27, "RESERVED_1B", "ISP RESERVED 27"),
    ISP_MESSAGE_UPDATE_REQUEST(28, "MSG_UPDT_REQ", "MESSAGE UPDATE REQUEST"),
    ISP_RADIO_UNIT_MONITOR_REQUEST(29, "RAD_MON_REQ", "RADIO UNIT MONITOR REQUEST"),
    ISP_RESERVED_1E(30, "RESERVED_1E", "ISP RESERVED 30"),
    ISP_CALL_ALERT_REQUEST(31, "CALL_ALRT_REQ", "CALL ALERT REQUEST"),
    ISP_UNIT_ACKNOWLEDGE_RESPONSE(32, "ACK_RSP_U", "UNIT ACKNOWLEDGE RESPONSE"),
    ISP_RESERVED_21(33, "RESERVED_21", "ISP RESERVED 33"),
    ISP_RESERVED_22(34, "RESERVED_22", "ISP RESERVED 34"),
    ISP_CANCEL_SERVICE_REQUEST(35, "CAN_SRV_REQ", "CANCEL SERVICE REQUEST"),
    ISP_EXTENDED_FUNCTION_RESPONSE(36, "EXT_FNCT_RSP", "EXTENDED FUNCTION RESPONSE"),
    ISP_RESERVED_25(37, "RESERVED_25", "ISP RESERVED 37"),
    ISP_RESERVED_26(38, "RESERVED_26", "ISP RESERVED 38"),
    ISP_EMERGENCY_ALARM_REQUEST(39, "EMRG_ALRM_REQ", "EMERGENCY ALARM REQUEST"),
    ISP_GROUP_AFFILIATION_REQUEST(40, "GRP_AFF_REQ", "GROUP AFFILIATION REQUEST"),
    ISP_GROUP_AFFILIATION_QUERY_RESPONSE(41, "GRP_AFF_Q_RSP", "GROUP AFFILIATION QUERY RESPONSE"),
    ISP_RESERVED_2A(42, "RESERVED_2A", "ISP RESERVED 42"),
    ISP_UNIT_DE_REGISTRATION_REQUEST(43, "U_DE_REG_REQ", "UNIT DE-REGISTRATION REQUEST"),
    ISP_UNIT_REGISTRATION_REQUEST(44, "U_REG_REQ", "UNIT REGISTRATION REQUEST"),
    ISP_LOCATION_REGISTRATION_REQUEST(45, "LOC_REG_REQ", "LOCATION REGISTRATION REQUEST"),
    ISP_AUTHENTICATION_QUERY_OBSOLETE(46, "AUTH_Q", "AUTHENTICATION QUERY-OBSOLETE"),               //OBSOLETE
    ISP_AUTHENTICATION_RESPONSE_OBSOLETE(47, "AUTH_RSP", "AUTHENTICATION RESPONSE-OBSOLETE"),          //OBSOLETE
    ISP_PROTECTION_PARAMETER_REQUEST(48, "P_PARM_REQ", "ENCRYPTION PARAMETERS REQUEST"),
    ISP_RESERVED_31(49, "RESERVED_31", "ISP RESERVED 49"),
    ISP_IDENTIFIER_UPDATE_REQUEST(50, "IDEN_UP_REQ", "IDENTIFIER UPDATE REQUEST"),
    ISP_RESERVED_33(51, "RESERVED_33", "ISP RESERVED 51"),
    ISP_RESERVED_34(52, "RESERVED_34", "ISP RESERVED 52"),
    ISP_RESERVED_35(53, "RESERVED_35", "ISP RESERVED 53"),
    ISP_ROAMING_ADDRESS_REQUEST(54, "ROAM_ADDR_REQ", "ROAMING ADDRESS REQUEST"),
    ISP_ROAMING_ADDRESS_RESPONSE(55, "ROAM_ADDR_RSP", "ROAMING ADDRESS RESPONSE"),
    ISP_AUTHENTICATION_RESPONSE(56, "AUTH_RESP", "AUTHENTICATION RESPONSE"),
    ISP_AUTHENTICATION_RESPONSE_MUTUAL(57, "AUTH_RESP_M", "AUTHENTICATION RESPONSE MUTUAL"),
    ISP_AUTHENTICATION_FNE_RESULT(58, "AUTH_FNE_RST", "AUTHENTICATION RESULT"),
    ISP_AUTHENTICATION_SU_DEMAND(59, "AUTH_SU_DMD", "AUTHENTICATION SU DEMAND"),
    ISP_RESERVED_3C(60, "RESERVED_3C", "ISP RESERVED 60"),
    ISP_RESERVED_3D(61, "RESERVED_3D", "ISP RESERVED 61"),
    ISP_RESERVED_3E(62, "RESERVED_3E", "ISP RESERVED 62"),
    ISP_RESERVED_3F(63, "RESERVED_3F", "ISP RESERVED 63"),
    ISP_UNKNOWN(-1, "ISP UNKNOWN", "ISP UNKNOWN OPCODE"),

    //Vendor: motorola, Inbound Service Packet (ISP)
    MOTOROLA_ISP_UNKNOWN(-1, "MOTOROLA ISP UNKNOWN OPCODE", "MOTOROLA ISP UNKNOWN OPCODE"),

    //Vendor: motorola, Outbound Service Packet (OSP)
    MOTOROLA_OSP_PATCH_GROUP_ADD(0, "PATCH GROUP ADD", "MOTOROLA PATCH GROUP ADD"),
    MOTOROLA_OSP_PATCH_GROUP_DELETE(1, "PATCH GROUP DELE", "MOTOROLA PATCH GROUP DELETE"),
    MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT(2, "PTCH GRP VCHN GR", "MOTOROLA PATCH GROUP CHANNEL GRANT"),
    MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT_UPDATE(3, "PTCH GRP VCH UPD", "MOTOROLA PATCH GROUP CHANNEL GRANT UPDATE"),
    MOTOROLA_OSP_TRAFFIC_CHANNEL_ID(5, "TRAFFIC CHANNEL", "MOTOROLA TRAFFIC CHANNEL"),
    MOTOROLA_OSP_DENY_RESPONSE(7, "DENY RESPONSE", "MOTOROLA DENY RESPONSE"),
    MOTOROLA_OSP_SYSTEM_LOADING(9, "SYSTEM LOADING", "SYSTEM LOADING"),
    MOTOROLA_OSP_BASE_STATION_ID(11, "CCH BASE STAT ID", "CONTROL CHANNEL BASE STATION ID"),
    MOTOROLA_OSP_CONTROL_CHANNEL_PLANNED_SHUTDOWN(14, "CCH PLND SHUTDWN", "CONTROL CHANNEL PLANNED SHUTDOWN"),
    MOTOROLA_OSP_UNKNOWN(-1, "MOTOROLA OSP UNKNOWN OPCODE", "MOTOROLA OSP UNKNOWN OPCODE"),

    //Vendor: motorola, Inbound Service Packet (ISP)
    HARRIS_ISP_UNKNOWN(-1, "HARRIS ISP UNKNOWN OPCODE", "HARRIS ISP UNKNOWN OPCODE"),

    //Vendor: harris, Outbound Service Packet (OSP)
    HARRIS_OSP_TDMA_SYNC(48, "HARRIS TDMA SYNC", "HARRIS TDMA SYNC BROADCAST"),
    HARRIS_OSP_UNKNOWN(-1, "HARRIS OSP UNKNOWN OPCODE", "HARRIS OSP UNKNOWN OPCODE"),

    //Vendor: unknown, Inbound Service Packet (ISP)
    UNKNOWN_VENDOR_ISP(-1, "UNKNOWN VENDOR/OPCODE ISP", "UNKNOWN VENDOR ISP OPCODE"),

    //Vendor: unknown, Outbound Service Packet (OSP)
    UNKNOWN_VENDOR_OSP(-1, "UNKNOWN VENDOR/OPCODE OSP", "UNKNOWN VENDOR OSP OPCODE");

    private int mCode;
    private String mLabel;
    private String mDescription;

    public static final EnumSet<Opcode> STANDARD_OUTBOUND_OPCODES = EnumSet.range(OSP_GROUP_VOICE_CHANNEL_GRANT,
        OSP_PROTECTION_PARAMETER_UPDATE);
    public static final EnumSet<Opcode> STANDARD_INBOUND_OPCODES = EnumSet.range(ISP_GROUP_VOICE_SERVICE_REQUEST,
        ISP_RESERVED_3F);
    public static final EnumSet<Opcode> DATA_CHANNEL_GRANT_OPCODES = EnumSet.of(OSP_SNDCP_DATA_CHANNEL_GRANT,
        OSP_INDIVIDUAL_DATA_CHANNEL_GRANT, OSP_GROUP_DATA_CHANNEL_GRANT);

    Opcode(int code, String label, String description)
    {
        mCode = code;
        mLabel = label;
        mDescription = description;
    }

    /**
     * Formatted, fixed-length label
     */
    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Full description text
     */
    public String getDescription()
    {
        return mDescription;
    }

    /**
     * Numeric value for the opcode
     */
    public int getCode()
    {
        return mCode;
    }

    /**
     * Indicates if this opcode is an SNDCP data channel grant opcode
     */
    public boolean isDataChannelGrant()
    {
        return DATA_CHANNEL_GRANT_OPCODES.contains(this);
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    @Deprecated //use the ISP/OSP version of this method instead
    public static Opcode fromValue(int value)
    {
        if(0 <= value && value <= 63)
        {
            return values()[value];
        }

        return OSP_UNKNOWN;
    }

    public static Opcode fromValue(int value, Direction direction, Vendor vendor)
    {
        switch(vendor)
        {
            case STANDARD:
                if(direction == Direction.OUTBOUND)
                {
                    if(0 <= value && value <= 63)
                    {
                        for(Opcode outboundOpcode : STANDARD_OUTBOUND_OPCODES)
                        {
                            if(outboundOpcode.getCode() == value)
                            {
                                return outboundOpcode;
                            }
                        }
                    }

                    return OSP_UNKNOWN;
                }
                else
                {
                    if(0 <= value && value <= 63)
                    {
                        for(Opcode inboundOpcode : STANDARD_INBOUND_OPCODES)
                        {
                            if(inboundOpcode.getCode() == value)
                            {
                                return inboundOpcode;
                            }
                        }
                    }

                    return ISP_UNKNOWN;
                }
            case HARRIS:
                if(direction == Direction.INBOUND)
                {
                    return HARRIS_ISP_UNKNOWN;
                }
                else
                {
                    switch(value)
                    {
                        case 48:
                            return HARRIS_OSP_TDMA_SYNC;
                        default:
                            return HARRIS_OSP_UNKNOWN;
                    }
                }
            case MOTOROLA:
                if(direction == Direction.INBOUND)
                {
                    return MOTOROLA_ISP_UNKNOWN;
                }
                else
                {
                    switch(value)
                    {
                        case 0x00:
                            return MOTOROLA_OSP_PATCH_GROUP_ADD;
                        case 0x01:
                            return MOTOROLA_OSP_PATCH_GROUP_DELETE;
                        case 0x02:
                            return MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT;
                        case 0x03:
                            return MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT_UPDATE;
                        case 0x05:
                            return MOTOROLA_OSP_TRAFFIC_CHANNEL_ID;
                        case 0x07:
                            return MOTOROLA_OSP_DENY_RESPONSE;
                        case 0x09:
                            return MOTOROLA_OSP_SYSTEM_LOADING;
                        case 0x0B:
                            return MOTOROLA_OSP_BASE_STATION_ID;
                        case 0x0E:
                            return MOTOROLA_OSP_CONTROL_CHANNEL_PLANNED_SHUTDOWN;
                        default:
                            return MOTOROLA_OSP_UNKNOWN;
                    }
                }
            default:
                if(direction == Direction.INBOUND)
                {
                    return UNKNOWN_VENDOR_ISP;
                }
                else
                {
                    return UNKNOWN_VENDOR_OSP;
                }
        }
    }
}
