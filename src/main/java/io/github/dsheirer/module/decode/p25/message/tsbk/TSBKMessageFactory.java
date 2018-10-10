/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.message.tsbk;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.trellis.ViterbiDecoder_1_2_P25;
import io.github.dsheirer.module.decode.p25.P25Interleave;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.ControlChannelBaseStationIdentification;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.MotorolaOpcode;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.MotorolaTSBKMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.PatchGroupAdd;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.PatchGroupDelete;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.PatchGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.PatchGroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.PlannedControlChannnelShutdown;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.SystemLoading;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.TrafficChannelBaseStationIdentification;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.AcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.AdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.AuthenticationCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.CallAlert;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.DenyResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.GroupAffiliationQuery;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.GroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.IdentifierUpdateNonVUHF;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.IdentifierUpdateTDMA;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.IdentifierUpdateVUHF;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.LocationRegistrationResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.MessageUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.ProtectionParameterUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.QueuedResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.RadioUnitMonitorCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.RoamingAddressCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.StatusQuery;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.StatusUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SyncBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SystemServiceBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.TimeAndDateAnnouncement;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.UnitDeregistrationAcknowledge;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.UnitRegistrationCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.UnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncement;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.GroupDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.IndividualDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.SNDCPDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.SNDCPDataPageRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.TelephoneInterconnectVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.UnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Opcode;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.BitSet;

public class TSBKMessageFactory
{
    public static final int PDU0_BEGIN = 64;
    public static final int PDU0_END = 260;
    private static final ViterbiDecoder_1_2_P25 VITERBI_HALF_RATE_DECODER = new ViterbiDecoder_1_2_P25();

    public static TSBKMessage create(CorrectedBinaryMessage correctedBinaryMessage, DataUnitID dataUnitID)
    {
        //Get deinterleaved header chunk
        BitSet interleaved = correctedBinaryMessage.get(PDU0_BEGIN, PDU0_END);
        CorrectedBinaryMessage deinterleaved = P25Interleave.deinterleaveChunk(P25Interleave.DATA_DEINTERLEAVE, interleaved);

        //Decode 1/2 rate trellis encoded PDU header
        CorrectedBinaryMessage viterbiDecoded = VITERBI_HALF_RATE_DECODER.decode(deinterleaved);

        if(viterbiDecoded != null)
        {
            //TODO: update all of the TSBK messages to use the 1/2 rate decoded message without preloading the NID
            for(int x = 0; x < viterbiDecoded.size(); x++)
            {
                if(viterbiDecoded.get(x))
                {
                    correctedBinaryMessage.set(x + 64);
                }
                else
                {
                    correctedBinaryMessage.clear((x + 64));
                }
            }

            return getMessage(correctedBinaryMessage, dataUnitID, null);
        }

        return null;

    }

    public static TSBKMessage getMessage(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        Vendor vendor = Vendor.fromValue(message.getInt(TSBKMessage.VENDOR_ID));

        switch(vendor)
        {
            case STANDARD:
                Opcode opcode =
                    Opcode.fromValue(message.getInt(TSBKMessage.OPCODE));

                switch(opcode)
                {
                    case ACKNOWLEDGE_RESPONSE:
                        return new AcknowledgeResponse(message, duid, aliasList);
                    case ADJACENT_STATUS_BROADCAST:
                        return new AdjacentStatusBroadcast(message, duid, aliasList);
                    case AUTHENTICATION_COMMAND:
                        return new AuthenticationCommand(message, duid, aliasList);
                    case CALL_ALERT:
                        return new CallAlert(message, duid, aliasList);
                    case DENY_RESPONSE:
                        return new DenyResponse(message, duid, aliasList);
                    case EXTENDED_FUNCTION_COMMAND:
                        return new ExtendedFunctionCommand(message, duid, aliasList);
                    case GROUP_AFFILIATION_QUERY:
                        return new GroupAffiliationQuery(message, duid, aliasList);
                    case GROUP_AFFILIATION_RESPONSE:
                        return new GroupAffiliationResponse(message, duid, aliasList);
                    case GROUP_DATA_CHANNEL_ANNOUNCEMENT:
                        return new GroupDataChannelAnnouncement(message, duid, aliasList);
                    case GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                        return new GroupDataChannelAnnouncementExplicit(message, duid, aliasList);
                    case GROUP_DATA_CHANNEL_GRANT:
                        return new GroupDataChannelGrant(message, duid, aliasList);
                    case GROUP_VOICE_CHANNEL_GRANT:
                        return new GroupVoiceChannelGrant(message, duid, aliasList);
                    case GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                        return new GroupVoiceChannelGrantUpdate(message, duid, aliasList);
                    case GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                        return new GroupVoiceChannelGrantUpdateExplicit(message, duid, aliasList);
                    case IDENTIFIER_UPDATE_NON_VUHF:
                        return new IdentifierUpdateNonVUHF(message, duid, aliasList);
                    case IDENTIFIER_UPDATE_TDMA:
                        return new IdentifierUpdateTDMA(message, duid, aliasList);
                    case IDENTIFIER_UPDATE_VHF_UHF_BANDS:
                        return new IdentifierUpdateVUHF(message, duid, aliasList);
                    case INDIVIDUAL_DATA_CHANNEL_GRANT:
                        return new IndividualDataChannelGrant(message, duid, aliasList);
                    case LOCATION_REGISTRATION_RESPONSE:
                        return new LocationRegistrationResponse(message, duid, aliasList);
                    case MESSAGE_UPDATE:
                        return new MessageUpdate(message, duid, aliasList);
                    case NETWORK_STATUS_BROADCAST:
                        return new NetworkStatusBroadcast(message, duid, aliasList);
                    case QUEUED_RESPONSE:
                        return new QueuedResponse(message, duid, aliasList);
                    case PROTECTION_PARAMETER_UPDATE:
                        return new ProtectionParameterUpdate(message, duid, aliasList);
                    case RADIO_UNIT_MONITOR_COMMAND:
                        return new RadioUnitMonitorCommand(message, duid, aliasList);
                    case RFSS_STATUS_BROADCAST:
                        return new RFSSStatusBroadcast(message, duid, aliasList);
                    case ROAMING_ADDRESS_COMMAND:
                        return new RoamingAddressCommand(message, duid, aliasList);
                    case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                        return new SecondaryControlChannelBroadcast(message, duid, aliasList);
                    case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                        return new SecondaryControlChannelBroadcastExplicit(message, duid, aliasList);
                    case SNDCP_DATA_CHANNEL_GRANT:
                        return new SNDCPDataChannelGrant(message, duid, aliasList);
                    case SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                        return new SNDCPDataChannelAnnouncementExplicit(message, duid, aliasList);
                    case SNDCP_DATA_PAGE_REQUEST:
                        return new SNDCPDataPageRequest(message, duid, aliasList);
                    case STATUS_QUERY:
                        return new StatusQuery(message, duid, aliasList);
                    case STATUS_UPDATE:
                        return new StatusUpdate(message, duid, aliasList);
                    case TDMA_SYNC_BROADCAST:
                        return new SyncBroadcast(message, duid, aliasList);
                    case SYSTEM_SERVICE_BROADCAST:
                        return new SystemServiceBroadcast(message, duid, aliasList);
                    case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                        return new TelephoneInterconnectAnswerRequest(message, duid, aliasList);
                    case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                        return new TelephoneInterconnectVoiceChannelGrant(message, duid, aliasList);
                    case TIME_DATE_ANNOUNCEMENT:
                        return new TimeAndDateAnnouncement(message, duid, aliasList);
                    case UNIT_DEREGISTRATION_ACKNOWLEDGE:
                        return new UnitDeregistrationAcknowledge(message, duid, aliasList);
                    case UNIT_REGISTRATION_COMMAND:
                        return new UnitRegistrationCommand(message, duid, aliasList);
                    case UNIT_REGISTRATION_RESPONSE:
                        return new UnitRegistrationResponse(message, duid, aliasList);
                    case UNIT_TO_UNIT_ANSWER_REQUEST:
                        return new UnitToUnitAnswerRequest(message, duid, aliasList);
                    case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                        return new UnitToUnitVoiceChannelGrant(message, duid, aliasList);
                    case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                        return new UnitToUnitVoiceChannelGrantUpdate(message, duid, aliasList);
                    default:
                        return new TSBKMessage(message, duid, aliasList);
                }

            case MOTOROLA:
                MotorolaOpcode motorolaOpcode = MotorolaOpcode.
                    fromValue(message.getInt(TSBKMessage.OPCODE));

                switch(motorolaOpcode)
                {
                    case CCH_PLANNED_SHUTDOWN:
                        return new PlannedControlChannnelShutdown(message, duid, aliasList);
                    case CONTROL_CHANNEL_ID:
                        return new ControlChannelBaseStationIdentification(message, duid, aliasList);
                    case PATCH_GROUP_ADD:
                        return new PatchGroupAdd(message, duid, aliasList);
                    case PATCH_GROUP_DELETE:
                        return new PatchGroupDelete(message, duid, aliasList);
                    case PATCH_GROUP_CHANNEL_GRANT:
                        return new PatchGroupVoiceChannelGrant(message, duid, aliasList);
                    case PATCH_GROUP_CHANNEL_GRANT_UPDATE:
                        return new PatchGroupVoiceChannelGrantUpdate(message, duid, aliasList);
                    case SYSTEM_LOAD:
                        return new SystemLoading(message, duid, aliasList);
                    case TRAFFIC_CHANNEL_ID:
                        return new TrafficChannelBaseStationIdentification(message, duid, aliasList);
                    default:
                }

                return new MotorolaTSBKMessage(message, duid, aliasList);
            default:
                return new TSBKMessage(message, duid, aliasList);
        }
    }
}