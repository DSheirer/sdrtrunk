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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_1_2_P25;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.P25P1Interleave;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.harris.isp.UnknownHarrisISPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.harris.osp.L3HarrisGroupRegroupExplicitEncryptionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.harris.osp.UnknownHarrisOSPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.isp.MotorolaExtendedFunctionResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.isp.MotorolaGroupRegroupVoiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.isp.UnknownMotorolaISPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.ChannelLoading;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaBaseStationId;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaDenyResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaEmergencyAlarmActivation;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaExplicitTDMADataChannelAnnouncement;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaGroupRegroupAddCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaGroupRegroupChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaGroupRegroupChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaGroupRegroupDeleteCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaOpcode15;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaQueuedResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaTrafficChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.PlannedChannelShutdown;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.UnknownMotorolaOSPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.AuthenticationQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.CallAlertRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.CancelServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.EmergencyAlarmRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.ExtendedFunctionResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.FrequencyBandUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.GroupAffiliationQueryResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.GroupAffiliationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.GroupVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.IndividualDataServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.LocationRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.MessageUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.ProtectionParameterRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.RadioUnitMonitorRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.RoamingAddressRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.RoamingAddressResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.SNDCPDataChannelRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.SNDCPDataPageResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.SNDCPReconnectRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.StatusQueryRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.StatusQueryResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.StatusUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.TelephoneInterconnectAnswerResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.TelephoneInterconnectPstnRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitDeRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitToUnitVoiceServiceAnswerResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitToUnitVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnknownISPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.AcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.AdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.AuthenticationCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.CallAlert;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.DenyResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.FrequencyBandUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.FrequencyBandUpdateTDMA;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.FrequencyBandUpdateVUHF;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupAffiliationQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupDataChannelAnnouncement;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.IndividualDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.LocationRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.MessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.ProtectionParameterUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.QueuedResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RadioUnitMonitorCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RoamingAddressCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SNDCPDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SNDCPDataPageRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.StatusQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.StatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SynchronizationBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SystemServiceBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitDeRegistrationAcknowledge;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitRegistrationCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitToUnitVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitToUnitVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnknownOSPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.unknown.isp.UnknownVendorISPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.unknown.osp.UnknownVendorOSPMessage;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating Trunking Signalling Block (TSBK) message parser classes
 */
public class TSBKMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(TSBKMessageFactory.class);
    private static final ViterbiDecoder_1_2_P25 VITERBI_HALF_RATE_DECODER = new ViterbiDecoder_1_2_P25();

    public static TSBKMessage create(Direction direction, P25P1DataUnitID dataUnitID,
                                     CorrectedBinaryMessage correctedBinaryMessage, int nac, long timestamp)
    {
        //Get deinterleaved header chunk
        CorrectedBinaryMessage deinterleaved = P25P1Interleave.deinterleaveChunk(P25P1Interleave.DATA_DEINTERLEAVE, correctedBinaryMessage);

        //Decode 1/2 rate trellis encoded PDU header
        CorrectedBinaryMessage message = VITERBI_HALF_RATE_DECODER.decode(deinterleaved);

        if(message == null)
        {
            return null;
        }

        //The CRC-CCITT can correct up to 1 bit error or detect 2 or more errors.  We mark the message as
        //invalid if the algorithm detects more than 1 correctable error.
        int errors = CRCP25.correctCCITT80(message, 0, 80);

        Vendor vendor = TSBKMessage.getVendor(message);
        Opcode opcode = TSBKMessage.getOpcode(message, direction, vendor);

        TSBKMessage tsbk = null;

        switch(opcode)
        {
            case ISP_AUTHENTICATION_QUERY_OBSOLETE:
                tsbk = new AuthenticationQuery(dataUnitID, message, nac, timestamp);
                break;
            case ISP_CALL_ALERT_REQUEST:
                tsbk = new CallAlertRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_CANCEL_SERVICE_REQUEST:
                tsbk = new CancelServiceRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_EMERGENCY_ALARM_REQUEST:
                tsbk = new EmergencyAlarmRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_EXTENDED_FUNCTION_RESPONSE:
                tsbk = new ExtendedFunctionResponse(dataUnitID, message, nac, timestamp);
                break;
            case ISP_GROUP_AFFILIATION_QUERY_RESPONSE:
                tsbk = new GroupAffiliationQueryResponse(dataUnitID, message, nac, timestamp);
                break;
            case ISP_GROUP_AFFILIATION_REQUEST:
                tsbk = new GroupAffiliationRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_GROUP_VOICE_SERVICE_REQUEST:
                tsbk = new GroupVoiceServiceRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_IDENTIFIER_UPDATE_REQUEST:
                tsbk = new FrequencyBandUpdateRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                tsbk = new IndividualDataServiceRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_LOCATION_REGISTRATION_REQUEST:
                tsbk = new LocationRegistrationRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_MESSAGE_UPDATE_REQUEST:
                tsbk = new MessageUpdateRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_PROTECTION_PARAMETER_REQUEST:
                tsbk = new ProtectionParameterRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_RADIO_UNIT_MONITOR_REQUEST:
                tsbk = new RadioUnitMonitorRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_ROAMING_ADDRESS_REQUEST:
                tsbk = new RoamingAddressRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_ROAMING_ADDRESS_RESPONSE:
                tsbk = new RoamingAddressResponse(dataUnitID, message, nac, timestamp);
                break;
            case ISP_SNDCP_DATA_CHANNEL_REQUEST:
                tsbk = new SNDCPDataChannelRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_SNDCP_DATA_PAGE_RESPONSE:
                tsbk = new SNDCPDataPageResponse(dataUnitID, message, nac, timestamp);
                break;
            case ISP_SNDCP_RECONNECT_REQUEST:
                tsbk = new SNDCPReconnectRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_STATUS_QUERY_REQUEST:
                tsbk = new StatusQueryRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_STATUS_QUERY_RESPONSE:
                tsbk = new StatusQueryResponse(dataUnitID, message, nac, timestamp);
                break;
            case ISP_STATUS_UPDATE_REQUEST:
                tsbk = new StatusUpdateRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
                tsbk = new TelephoneInterconnectAnswerResponse(dataUnitID, message, nac, timestamp);
                break;
            case ISP_TELEPHONE_INTERCONNECT_PSTN_REQUEST:
                tsbk = new TelephoneInterconnectPstnRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                tsbk = new UnitAcknowledgeResponse(dataUnitID, message, nac, timestamp);
                break;
            case ISP_UNIT_REGISTRATION_REQUEST:
                tsbk = new UnitRegistrationRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_UNIT_DE_REGISTRATION_REQUEST:
                tsbk = new UnitDeRegistrationRequest(dataUnitID, message, nac, timestamp);
                break;
            case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                tsbk = new UnitToUnitVoiceServiceAnswerResponse(dataUnitID, message, nac, timestamp);
                break;
            case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                tsbk = new UnitToUnitVoiceServiceRequest(dataUnitID, message, nac, timestamp);
                break;
            case OSP_ACKNOWLEDGE_RESPONSE:
                tsbk = new AcknowledgeResponse(dataUnitID, message, nac, timestamp);
                break;
            case OSP_ADJACENT_STATUS_BROADCAST:
                tsbk = new AdjacentStatusBroadcast(dataUnitID, message, nac, timestamp);
                break;
            case OSP_AUTHENTICATION_COMMAND:
                tsbk = new AuthenticationCommand(dataUnitID, message, nac, timestamp);
                break;
            case OSP_CALL_ALERT:
                tsbk = new CallAlert(dataUnitID, message, nac, timestamp);
                break;
            case OSP_DENY_RESPONSE:
                tsbk = new DenyResponse(dataUnitID, message, nac, timestamp);
                break;
            case OSP_EXTENDED_FUNCTION_COMMAND:
                tsbk = new ExtendedFunctionCommand(dataUnitID, message, nac, timestamp);
                break;
            case OSP_GROUP_AFFILIATION_QUERY:
                tsbk = new GroupAffiliationQuery(dataUnitID, message, nac, timestamp);
                break;
            case OSP_GROUP_AFFILIATION_RESPONSE:
                tsbk = new GroupAffiliationResponse(dataUnitID, message, nac, timestamp);
                break;
            case OSP_GROUP_DATA_CHANNEL_ANNOUNCEMENT:
                tsbk = new GroupDataChannelAnnouncement(dataUnitID, message, nac, timestamp);
                break;
            case OSP_GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                tsbk = new GroupDataChannelAnnouncementExplicit(dataUnitID, message, nac, timestamp);
                break;
            case OSP_GROUP_DATA_CHANNEL_GRANT:
                tsbk = new GroupDataChannelGrant(dataUnitID, message, nac, timestamp);
                break;
            case OSP_GROUP_VOICE_CHANNEL_GRANT:
                tsbk = new GroupVoiceChannelGrant(dataUnitID, message, nac, timestamp);
                break;
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                tsbk = new GroupVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);
                break;
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                tsbk = new GroupVoiceChannelGrantUpdateExplicit(dataUnitID, message, nac, timestamp);
                break;
            case OSP_IDENTIFIER_UPDATE:
                tsbk = new FrequencyBandUpdate(dataUnitID, message, nac, timestamp);
                break;
            case OSP_IDENTIFIER_UPDATE_TDMA:
                tsbk = new FrequencyBandUpdateTDMA(dataUnitID, message, nac, timestamp);
                break;
            case OSP_IDENTIFIER_UPDATE_VHF_UHF_BANDS:
                tsbk = new FrequencyBandUpdateVUHF(dataUnitID, message, nac, timestamp);
                break;
            case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
                tsbk = new IndividualDataChannelGrant(dataUnitID, message, nac, timestamp);
                break;
            case OSP_LOCATION_REGISTRATION_RESPONSE:
                tsbk = new LocationRegistrationResponse(dataUnitID, message, nac, timestamp);
                break;
            case OSP_MESSAGE_UPDATE:
                tsbk = new MessageUpdate(dataUnitID, message, nac, timestamp);
                break;
            case OSP_NETWORK_STATUS_BROADCAST:
                tsbk = new NetworkStatusBroadcast(dataUnitID, message, nac, timestamp);
                break;
            case OSP_RESERVED_3F:
                tsbk = new ProtectionParameterUpdate(dataUnitID, message, nac, timestamp);
                break;
            case OSP_RADIO_UNIT_MONITOR_COMMAND:
                tsbk = new RadioUnitMonitorCommand(dataUnitID, message, nac, timestamp);
                break;
            case OSP_QUEUED_RESPONSE:
                tsbk = new QueuedResponse(dataUnitID, message, nac, timestamp);
                break;
            case OSP_ROAMING_ADDRESS_COMMAND:
                tsbk = new RoamingAddressCommand(dataUnitID, message, nac, timestamp);
                break;
            case OSP_RFSS_STATUS_BROADCAST:
                tsbk = new RFSSStatusBroadcast(dataUnitID, message, nac, timestamp);
                break;
            case OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST:
                tsbk = new SecondaryControlChannelBroadcast(dataUnitID, message, nac, timestamp);
                break;
            case OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                tsbk = new SecondaryControlChannelBroadcastExplicit(dataUnitID, message, nac, timestamp);
                break;
            case OSP_SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                tsbk = new SNDCPDataChannelAnnouncementExplicit(dataUnitID, message, nac, timestamp);
                break;
            case OSP_SNDCP_DATA_CHANNEL_GRANT:
                tsbk = new SNDCPDataChannelGrant(dataUnitID, message, nac, timestamp);
                break;
            case OSP_SNDCP_DATA_PAGE_REQUEST:
                tsbk = new SNDCPDataPageRequest(dataUnitID, message, nac, timestamp);
                break;
            case OSP_STATUS_QUERY:
                tsbk = new StatusQuery(dataUnitID, message, nac, timestamp);
                break;
            case OSP_STATUS_UPDATE:
                tsbk = new StatusUpdate(dataUnitID, message, nac, timestamp);
                break;
            case OSP_TDMA_SYNC_BROADCAST:
                tsbk = new SynchronizationBroadcast(dataUnitID, message, nac, timestamp);
                break;
            case OSP_SYSTEM_SERVICE_BROADCAST:
                tsbk = new SystemServiceBroadcast(dataUnitID, message, nac, timestamp);
                break;
            case OSP_TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                tsbk = new TelephoneInterconnectAnswerRequest(dataUnitID, message, nac, timestamp);
                break;
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                tsbk = new TelephoneInterconnectVoiceChannelGrant(dataUnitID, message, nac, timestamp);
                break;
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                tsbk = new TelephoneInterconnectVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);
                break;
            case OSP_UNIT_DEREGISTRATION_ACKNOWLEDGE:
                tsbk = new UnitDeRegistrationAcknowledge(dataUnitID, message, nac, timestamp);
                break;
            case OSP_UNIT_REGISTRATION_COMMAND:
                tsbk = new UnitRegistrationCommand(dataUnitID, message, nac, timestamp);
                break;
            case OSP_UNIT_REGISTRATION_RESPONSE:
                tsbk = new UnitRegistrationResponse(dataUnitID, message, nac, timestamp);
                break;
            case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                tsbk = new UnitToUnitAnswerRequest(dataUnitID, message, nac, timestamp);
                break;
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                tsbk = new UnitToUnitVoiceChannelGrant(dataUnitID, message, nac, timestamp);
                break;
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                tsbk = new UnitToUnitVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);
                break;

            case HARRIS_ISP_UNKNOWN:
                tsbk = new UnknownHarrisISPMessage(dataUnitID, message, nac, timestamp);
                break;
            case HARRIS_OSP_GRG_EXENC_CMD:
                tsbk = new L3HarrisGroupRegroupExplicitEncryptionCommand(dataUnitID, message, nac, timestamp);
                break;
            case HARRIS_OSP_UNKNOWN:
                tsbk = new UnknownHarrisOSPMessage(dataUnitID, message, nac, timestamp);
                break;

            case MOTOROLA_ISP_UNKNOWN:
                tsbk = new UnknownMotorolaISPMessage(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_ISP_EXTENDED_FUNCTION_RESPONSE:
                tsbk = new MotorolaExtendedFunctionResponse(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_ISP_GROUP_REGROUP_VOICE_REQUEST:
                tsbk = new MotorolaGroupRegroupVoiceRequest(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_BASE_STATION_ID:
                tsbk = new MotorolaBaseStationId(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_CONTROL_CHANNEL_PLANNED_SHUTDOWN:
                tsbk = new PlannedChannelShutdown(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_DENY_RESPONSE:
                tsbk = new MotorolaDenyResponse(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_QUEUED_RESPONSE:
                tsbk = new MotorolaQueuedResponse(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_ACKNOWLEDGE_RESPONSE:
                tsbk = new MotorolaAcknowledgeResponse(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_TRAFFIC_CHANNEL_ID:
                tsbk = new MotorolaTrafficChannel(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_GROUP_REGROUP_ADD:
                tsbk = new MotorolaGroupRegroupAddCommand(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_GROUP_REGROUP_DELETE:
                tsbk = new MotorolaGroupRegroupDeleteCommand(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_GRANT:
                tsbk = new MotorolaGroupRegroupChannelGrant(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_UPDATE:
                tsbk = new MotorolaGroupRegroupChannelUpdate(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_SYSTEM_LOADING:
                tsbk = new ChannelLoading(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_EXTENDED_FUNCTION_COMMAND:
                tsbk = new MotorolaExtendedFunctionCommand(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_EMERGENCY_ALARM_ACTIVATION:
                tsbk = new MotorolaEmergencyAlarmActivation(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_OPCODE_15:
                tsbk = new MotorolaOpcode15(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_TDMA_DATA_CHANNEL:
                tsbk = new MotorolaExplicitTDMADataChannelAnnouncement(dataUnitID, message, nac, timestamp);
                break;
            case MOTOROLA_OSP_UNKNOWN:
                tsbk = new UnknownMotorolaOSPMessage(dataUnitID, message, nac, timestamp);
                break;

            case UNKNOWN_VENDOR_ISP:
                tsbk = new UnknownVendorISPMessage(dataUnitID, message, nac, timestamp);
                break;

            case UNKNOWN_VENDOR_OSP:
                tsbk = new UnknownVendorOSPMessage(dataUnitID, message, nac, timestamp);
                break;

            default:
                if(direction == Direction.INBOUND)
                {
                    tsbk = new UnknownISPMessage(dataUnitID, message, nac, timestamp);
                }
                else
                {
                    tsbk = new UnknownOSPMessage(dataUnitID, message, nac, timestamp);
                }
                break;
        }

        if(tsbk != null && errors > 1)
        {
            tsbk.setValid(false);
        }

        return tsbk;
    }

}
