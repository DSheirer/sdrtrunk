/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_1_2_P25;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.P25P1Interleave;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.harris.isp.UnknownHarrisISPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.harris.osp.HarrisTDMASyncBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.harris.osp.UnknownHarrisOSPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.isp.UnknownMotorolaISPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.ChannelLoading;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaBaseStationId;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaDenyResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaTrafficChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.PatchGroupAdd;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.PatchGroupDelete;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.PatchGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.PatchGroupVoiceChannelGrantUpdate;
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
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SyncBroadcast;
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

        Vendor vendor = TSBKMessage.getVendor(message);
        Opcode opcode = TSBKMessage.getOpcode(message, direction, vendor);

        switch(opcode)
        {
            case ISP_AUTHENTICATION_QUERY_OBSOLETE:
                return new AuthenticationQuery(dataUnitID, message, nac, timestamp);
            case ISP_CALL_ALERT_REQUEST:
                return new CallAlertRequest(dataUnitID, message, nac, timestamp);
            case ISP_CANCEL_SERVICE_REQUEST:
                return new CancelServiceRequest(dataUnitID, message, nac, timestamp);
            case ISP_EMERGENCY_ALARM_REQUEST:
                return new EmergencyAlarmRequest(dataUnitID, message, nac, timestamp);
            case ISP_EXTENDED_FUNCTION_RESPONSE:
                return new ExtendedFunctionResponse(dataUnitID, message, nac, timestamp);
            case ISP_GROUP_AFFILIATION_QUERY_RESPONSE:
                return new GroupAffiliationQueryResponse(dataUnitID, message, nac, timestamp);
            case ISP_GROUP_AFFILIATION_REQUEST:
                return new GroupAffiliationRequest(dataUnitID, message, nac, timestamp);
            case ISP_GROUP_VOICE_SERVICE_REQUEST:
                return new GroupVoiceServiceRequest(dataUnitID, message, nac, timestamp);
            case ISP_IDENTIFIER_UPDATE_REQUEST:
                return new FrequencyBandUpdateRequest(dataUnitID, message, nac, timestamp);
            case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                return new IndividualDataServiceRequest(dataUnitID, message, nac, timestamp);
            case ISP_LOCATION_REGISTRATION_REQUEST:
                return new LocationRegistrationRequest(dataUnitID, message, nac, timestamp);
            case ISP_MESSAGE_UPDATE_REQUEST:
                return new MessageUpdateRequest(dataUnitID, message, nac, timestamp);
            case ISP_PROTECTION_PARAMETER_REQUEST:
                return new ProtectionParameterRequest(dataUnitID, message, nac, timestamp);
            case ISP_RADIO_UNIT_MONITOR_REQUEST:
                return new RadioUnitMonitorRequest(dataUnitID, message, nac, timestamp);
            case ISP_ROAMING_ADDRESS_REQUEST:
                return new RoamingAddressRequest(dataUnitID, message, nac, timestamp);
            case ISP_ROAMING_ADDRESS_RESPONSE:
                return new RoamingAddressResponse(dataUnitID, message, nac, timestamp);
            case ISP_SNDCP_DATA_CHANNEL_REQUEST:
                return new SNDCPDataChannelRequest(dataUnitID, message, nac, timestamp);
            case ISP_SNDCP_DATA_PAGE_RESPONSE:
                return new SNDCPDataPageResponse(dataUnitID, message, nac, timestamp);
            case ISP_SNDCP_RECONNECT_REQUEST:
                return new SNDCPReconnectRequest(dataUnitID, message, nac, timestamp);
            case ISP_STATUS_QUERY_REQUEST:
                return new StatusQueryRequest(dataUnitID, message, nac, timestamp);
            case ISP_STATUS_QUERY_RESPONSE:
                return new StatusQueryResponse(dataUnitID, message, nac, timestamp);
            case ISP_STATUS_UPDATE_REQUEST:
                return new StatusUpdateRequest(dataUnitID, message, nac, timestamp);
            case ISP_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
                return new TelephoneInterconnectAnswerResponse(dataUnitID, message, nac, timestamp);
            case ISP_TELEPHONE_INTERCONNECT_PSTN_REQUEST:
                return new TelephoneInterconnectPstnRequest(dataUnitID, message, nac, timestamp);
            case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                return new UnitAcknowledgeResponse(dataUnitID, message, nac, timestamp);
            case ISP_UNIT_REGISTRATION_REQUEST:
                return new UnitRegistrationRequest(dataUnitID, message, nac, timestamp);
            case ISP_UNIT_DE_REGISTRATION_REQUEST:
                return new UnitDeRegistrationRequest(dataUnitID, message, nac, timestamp);
            case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                return new UnitToUnitVoiceServiceAnswerResponse(dataUnitID, message, nac, timestamp);
            case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                return new UnitToUnitVoiceServiceRequest(dataUnitID, message, nac, timestamp);
            case OSP_ACKNOWLEDGE_RESPONSE:
                return new AcknowledgeResponse(dataUnitID, message, nac, timestamp);
            case OSP_ADJACENT_STATUS_BROADCAST:
                return new AdjacentStatusBroadcast(dataUnitID, message, nac, timestamp);
            case OSP_AUTHENTICATION_COMMAND:
                return new AuthenticationCommand(dataUnitID, message, nac, timestamp);
            case OSP_CALL_ALERT:
                return new CallAlert(dataUnitID, message, nac, timestamp);
            case OSP_DENY_RESPONSE:
                return new DenyResponse(dataUnitID, message, nac, timestamp);
            case OSP_EXTENDED_FUNCTION_COMMAND:
                return new ExtendedFunctionCommand(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_AFFILIATION_QUERY:
                return new GroupAffiliationQuery(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_AFFILIATION_RESPONSE:
                return new GroupAffiliationResponse(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_DATA_CHANNEL_ANNOUNCEMENT:
                return new GroupDataChannelAnnouncement(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                return new GroupDataChannelAnnouncementExplicit(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_DATA_CHANNEL_GRANT:
                return new GroupDataChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_VOICE_CHANNEL_GRANT:
                return new GroupVoiceChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                return new GroupVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                return new GroupVoiceChannelGrantUpdateExplicit(dataUnitID, message, nac, timestamp);
            case OSP_IDENTIFIER_UPDATE:
                return new FrequencyBandUpdate(dataUnitID, message, nac, timestamp);
            case OSP_IDENTIFIER_UPDATE_TDMA:
                return new FrequencyBandUpdateTDMA(dataUnitID, message, nac, timestamp);
            case OSP_IDENTIFIER_UPDATE_VHF_UHF_BANDS:
                return new FrequencyBandUpdateVUHF(dataUnitID, message, nac, timestamp);
            case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
                return new IndividualDataChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_LOCATION_REGISTRATION_RESPONSE:
                return new LocationRegistrationResponse(dataUnitID, message, nac, timestamp);
            case OSP_MESSAGE_UPDATE:
                return new MessageUpdate(dataUnitID, message, nac, timestamp);
            case OSP_NETWORK_STATUS_BROADCAST:
                return new NetworkStatusBroadcast(dataUnitID, message, nac, timestamp);
            case OSP_PROTECTION_PARAMETER_UPDATE:
                return new ProtectionParameterUpdate(dataUnitID, message, nac, timestamp);
            case OSP_RADIO_UNIT_MONITOR_COMMAND:
                return new RadioUnitMonitorCommand(dataUnitID, message, nac, timestamp);
            case OSP_QUEUED_RESPONSE:
                return new QueuedResponse(dataUnitID, message, nac, timestamp);
            case OSP_ROAMING_ADDRESS_COMMAND:
                return new RoamingAddressCommand(dataUnitID, message, nac, timestamp);
            case OSP_RFSS_STATUS_BROADCAST:
                return new RFSSStatusBroadcast(dataUnitID, message, nac, timestamp);
            case OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST:
                return new SecondaryControlChannelBroadcast(dataUnitID, message, nac, timestamp);
            case OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                return new SecondaryControlChannelBroadcastExplicit(dataUnitID, message, nac, timestamp);
            case OSP_SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                return new SNDCPDataChannelAnnouncementExplicit(dataUnitID, message, nac, timestamp);
            case OSP_SNDCP_DATA_CHANNEL_GRANT:
                return new SNDCPDataChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_SNDCP_DATA_PAGE_REQUEST:
                return new SNDCPDataPageRequest(dataUnitID, message, nac, timestamp);
            case OSP_STATUS_QUERY:
                return new StatusQuery(dataUnitID, message, nac, timestamp);
            case OSP_STATUS_UPDATE:
                return new StatusUpdate(dataUnitID, message, nac, timestamp);
            case OSP_TDMA_SYNC_BROADCAST:
                return new SyncBroadcast(dataUnitID, message, nac, timestamp);
            case OSP_SYSTEM_SERVICE_BROADCAST:
                return new SystemServiceBroadcast(dataUnitID, message, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                return new TelephoneInterconnectAnswerRequest(dataUnitID, message, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                return new TelephoneInterconnectVoiceChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                return new TelephoneInterconnectVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);
            case OSP_UNIT_DEREGISTRATION_ACKNOWLEDGE:
                return new UnitDeRegistrationAcknowledge(dataUnitID, message, nac, timestamp);
            case OSP_UNIT_REGISTRATION_COMMAND:
                return new UnitRegistrationCommand(dataUnitID, message, nac, timestamp);
            case OSP_UNIT_REGISTRATION_RESPONSE:
                return new UnitRegistrationResponse(dataUnitID, message, nac, timestamp);
            case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                return new UnitToUnitAnswerRequest(dataUnitID, message, nac, timestamp);
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                return new UnitToUnitVoiceChannelGrant(dataUnitID, message, nac, timestamp);
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                return new UnitToUnitVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);

            case HARRIS_ISP_UNKNOWN:
                return new UnknownHarrisISPMessage(dataUnitID, message, nac, timestamp);
            case HARRIS_OSP_TDMA_SYNC:
                return new HarrisTDMASyncBroadcast(dataUnitID, message, nac, timestamp);
            case HARRIS_OSP_UNKNOWN:
                return new UnknownHarrisOSPMessage(dataUnitID, message, nac, timestamp);

            case MOTOROLA_ISP_UNKNOWN:
                return new UnknownMotorolaISPMessage(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_CONTROL_CHANNEL_ID:
                return new MotorolaBaseStationId(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_CONTROL_CHANNEL_PLANNED_SHUTDOWN:
                return new PlannedChannelShutdown(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_DENY_RESPONSE:
                return new MotorolaDenyResponse(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_TRAFFIC_CHANNEL_ID:
                return new MotorolaTrafficChannel(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_PATCH_GROUP_ADD:
                return new PatchGroupAdd(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_PATCH_GROUP_DELETE:
                return new PatchGroupDelete(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT:
                return new PatchGroupVoiceChannelGrant(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT_UPDATE:
                return new PatchGroupVoiceChannelGrantUpdate(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_SYSTEM_LOADING:
                return new ChannelLoading(dataUnitID, message, nac, timestamp);
            case MOTOROLA_OSP_UNKNOWN:
                return new UnknownMotorolaOSPMessage(dataUnitID, message, nac, timestamp);

            case UNKNOWN_VENDOR_ISP:
                return new UnknownVendorISPMessage(dataUnitID, message, nac, timestamp);

            case UNKNOWN_VENDOR_OSP:
                return new UnknownVendorOSPMessage(dataUnitID, message, nac, timestamp);

            default:
                if(direction == Direction.INBOUND)
                {
                    return new UnknownISPMessage(dataUnitID, message, nac, timestamp);
                }
                else
                {
                    return new UnknownOSPMessage(dataUnitID, message, nac, timestamp);
                }
        }
    }
}
