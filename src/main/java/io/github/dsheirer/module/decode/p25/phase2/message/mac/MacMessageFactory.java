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

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.CallAlertAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.CallAlertExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.DateAndTimeAnnouncement;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.DenyResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.EndPushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateTDMA;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateVUHF;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupPagingMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateMultiple;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateMultipleExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.IndividualPagingMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacRelease;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NullInformationMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PowerControlSignalQuality;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.QueuedResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandEnhanced;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataPageRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SecondaryControlChannelBroadcastAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SystemServiceBroadcast;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitRegistrationCommandAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnknownStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnknownVendorMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisRegroupCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisTalkerAlias;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message factory for creating Encoded MAC Information (EMI) message parser instances.
 */
public class MacMessageFactory
{
    public static final int DEFAULT_MAC_STRUCTURE_INDEX = 8;

    private final static Logger mLog = LoggerFactory.getLogger(MacMessageFactory.class);

    public static List<MacMessage> create(int timeslot, DataUnitID dataUnitID,
                                          CorrectedBinaryMessage message, long timestamp)
    {
        List<MacMessage> messages = new ArrayList<>();

        MacPduType macPduType = MacMessage.getMacPduTypeFromMessage(message);

        switch(macPduType)
        {
            case MAC_0_RESERVED:
                break;
            case MAC_1_PTT:
                messages.add(new MacMessage(timeslot, dataUnitID, message, timestamp, new PushToTalk(message)));
                break;
            case MAC_2_END_PTT:
                messages.add(new MacMessage(timeslot, dataUnitID, message, timestamp, new EndPushToTalk(message)));
                break;
            case MAC_3_IDLE:
            case MAC_4_ACTIVE:
            case MAC_6_HANGTIME:
                List<Integer> indices = getMacStructureIndices(message);

                for(Integer index : indices)
                {
                    MacStructure macStructure = createMacStructure(message, index);
                    messages.add(new MacMessage(timeslot, dataUnitID, message, timestamp, macStructure));
                }
                break;
            case MAC_5_RESERVED:
                break;
            case MAC_7_RESERVED:
                break;
            case MAC_UNKNOWN:
                break;
            default:
                messages.add(new UnknownMacMessage(timeslot, dataUnitID, message, timestamp));
                break;
        }

        return messages;
    }

    /**
     * Identifies the MAC structure start indices for the message
     *
     * @param message containing one or more MAC structures
     * @return structure start indices
     */
    private static List<Integer> getMacStructureIndices(CorrectedBinaryMessage message)
    {
        List<Integer> indices = new ArrayList<>();

        //There should always be a MAC structure at index 8
        indices.add(DEFAULT_MAC_STRUCTURE_INDEX);

        MacOpcode opcode = MacStructure.getOpcode(message, DEFAULT_MAC_STRUCTURE_INDEX);

        int opcodeLength = opcode.getLength();

        if(opcodeLength > 0 && opcode != MacOpcode.TDMA_0_NULL_INFORMATION_MESSAGE)
        {
            int secondStructureIndex = DEFAULT_MAC_STRUCTURE_INDEX + (opcode.getLength() * 8);

            if(secondStructureIndex < message.size())
            {
                MacOpcode secondOpcode = MacStructure.getOpcode(message, secondStructureIndex);

                if(secondOpcode != MacOpcode.TDMA_0_NULL_INFORMATION_MESSAGE)
                {
                    indices.add(secondStructureIndex);

                    if(secondOpcode.getLength() > 0)
                    {
                        int thirdStructureIndex = secondStructureIndex + (secondOpcode.getLength() * 8);

                        if(thirdStructureIndex < message.size())
                        {
                            MacOpcode thirdOpcode = MacStructure.getOpcode(message, thirdStructureIndex);

                            if(thirdOpcode != MacOpcode.TDMA_0_NULL_INFORMATION_MESSAGE)
                            {
                                indices.add(thirdStructureIndex);
                            }
                        }
                    }
                }
            }
        }

        return indices;
    }

    /**
     * Creates a MAC structure parser for the message with the specified structure start offset.
     *
     * @param message containing a MAC structure
     * @param offset to the start of the structure
     * @return MAC structure parser
     */
    public static MacStructure createMacStructure(CorrectedBinaryMessage message, int offset)
    {
        MacOpcode opcode = MacStructure.getOpcode(message, offset);

        switch(opcode)
        {
            case TDMA_0_NULL_INFORMATION_MESSAGE:
                return new NullInformationMessage(message, offset);
            case TDMA_1_GROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                return new GroupVoiceChannelUserAbbreviated(message, offset);
            case TDMA_2_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                return new UnitToUnitVoiceChannelUserAbbreviated(message, offset);
            case TDMA_3_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                return new TelephoneInterconnectVoiceChannelUser(message, offset);
            case TDMA_5_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE:
                return new GroupVoiceChannelGrantUpdateMultiple(message, offset);
            case TDMA_17_INDIRECT_GROUP_PAGING:
                return new GroupPagingMessage(message, offset);
            case TDMA_18_INDIVIDUAL_PAGING_MESSAGE_WITH_PRIORITY:
                return new IndividualPagingMessage(message, offset);
            case TDMA_33_GROUP_VOICE_CHANNEL_USER_EXTENDED:
                return new GroupVoiceChannelUserExtended(message, offset);
            case TDMA_34_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
                return new UnitToUnitVoiceChannelUserExtended(message, offset);
            case TDMA_37_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_EXPLICIT:
                return new GroupVoiceChannelGrantUpdateMultipleExplicit(message, offset);
            case TDMA_48_POWER_CONTROL_SIGNAL_QUALITY:
                return new PowerControlSignalQuality(message, offset);
            case TDMA_49_MAC_RELEASE:
                return new MacRelease(message, offset);
            case PHASE1_64_GROUP_VOICE_CHANNEL_GRANT_ABBREVIATED:
                return new GroupVoiceChannelGrantAbbreviated(message, offset);
            case PHASE1_65_GROUP_VOICE_SERVICE_REQUEST:
                return new GroupVoiceServiceRequest(message, offset);
            case PHASE1_66_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                return new GroupVoiceChannelGrantUpdate(message, offset);
            case PHASE1_68_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_ABBREVIATED:
                return new UnitToUnitVoiceChannelGrantAbbreviated(message, offset);
            case PHASE1_69_UNIT_TO_UNIT_ANSWER_REQUEST_ABBREVIATED:
                return new UnitToUnitAnswerRequestAbbreviated(message, offset);
            case PHASE1_70_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED:
                return new UnitToUnitVoiceChannelGrantUpdateAbbreviated(message, offset);
            case PHASE1_74_TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                return new TelephoneInterconnectAnswerRequest(message, offset);
            case PHASE1_76_RADIO_UNIT_MONITOR_COMMAND_ABBREVIATED:
                return new RadioUnitMonitorCommand(message, offset);
            case PHASE1_84_SNDCP_DATA_CHANNEL_GRANT:
                return new SNDCPDataChannelGrant(message, offset);
            case PHASE1_85_SNDCP_DATA_PAGE_REQUEST:
                return new SNDCPDataPageRequest(message, offset);
            case PHASE1_88_STATUS_UPDATE_ABBREVIATED:
                return new StatusUpdateAbbreviated(message, offset);
            case PHASE1_90_STATUS_QUERY_ABBREVIATED:
                return new StatusQueryAbbreviated(message, offset);
            case OBSOLETE_PHASE1_93_RADIO_UNIT_MONITOR_COMMAND:
                return new UnknownStructure(message, offset); //Message is obsolete -- return unknown
            case PHASE1_92_MESSAGE_UPDATE_ABBREVIATED:
                return new MessageUpdateAbbreviated(message, offset);
            case PHASE1_94_RADIO_UNIT_MONITOR_COMMAND_ENHANCED:
                return new RadioUnitMonitorCommandEnhanced(message, offset);
            case PHASE1_95_CALL_ALERT_ABBREVIATED:
                return new CallAlertAbbreviated(message, offset);
            case PHASE1_96_ACK_RESPONSE:
                return new AcknowledgeResponse(message, offset);
            case PHASE1_97_QUEUED_RESPONSE:
                return new QueuedResponse(message, offset);
            case PHASE1_100_EXTENDED_FUNCTION_COMMAND_ABBREVIATED:
                return new ExtendedFunctionCommand(message, offset);
            case PHASE1_103_DENY_RESPONSE:
                return new DenyResponse(message, offset);
            case PHASE1_106_GROUP_AFFILIATION_QUERY_ABBREVIATED:
                return new GroupAffiliationQueryAbbreviated(message, offset);
            case PHASE1_109_UNIT_REGISTRATION_COMMAND_ABBREVIATED:
                return new UnitRegistrationCommandAbbreviated(message, offset);
            case PHASE1_115_IDENTIFIER_UPDATE_TDMA:
                return new FrequencyBandUpdateTDMA(message, offset);
            case PHASE1_116_IDENTIFIER_UPDATE_V_UHF:
                return new FrequencyBandUpdateVUHF(message, offset);
            case PHASE1_117_TIME_AND_DATE_ANNOUNCEMENT:
                return new DateAndTimeAnnouncement(message, offset);
            case PHASE1_120_SYSTEM_SERVICE_BROADCAST:
                return new SystemServiceBroadcast(message, offset);
            case PHASE1_121_SECONDARY_CONTROL_CHANNEL_BROADCAST_ABBREVIATED:
                return new SecondaryControlChannelBroadcastAbbreviated(message, offset);
            case PHASE1_122_RFSS_STATUS_BROADCAST_ABBREVIATED:
                return new RfssStatusBroadcastAbbreviated(message, offset);
            case PHASE1_123_NETWORK_STATUS_BROADCAST_ABBREVIATED:
                return new NetworkStatusBroadcastAbbreviated(message, offset);
            case PHASE1_124_ADJACENT_STATUS_BROADCAST_ABBREVIATED:
                return new AdjacentStatusBroadcastAbbreviated(message, offset);
            case PHASE1_125_IDENTIFIER_UPDATE:
                return new FrequencyBandUpdate(message, offset);
            case PHASE1_168_L3HARRIS_TALKER_ALIAS:
                return new L3HarrisTalkerAlias(message, offset);
            case PHASE1_176_L3HARRIS_GROUP_REGROUP:
                return new L3HarrisRegroupCommand(message, offset);
            case PHASE1_192_GROUP_VOICE_CHANNEL_GRANT_EXTENDED:
                return new GroupVoiceChannelGrantExtended(message, offset);
            case PHASE1_195_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                return new GroupVoiceChannelGrantUpdateExplicit(message, offset);
            case PHASE1_196_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_EXTENDED:
                return new UnitToUnitVoiceChannelGrantExtended(message, offset);
            case PHASE1_197_UNIT_TO_UNIT_ANSWER_REQUEST_EXTENDED:
                return new UnitToUnitAnswerRequestExtended(message, offset);
            case PHASE1_198_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED:
                return new UnitToUnitVoiceChannelGrantUpdateExtended(message, offset);
            case PHASE1_204_RADIO_UNIT_MONITOR_COMMAND_EXTENDED:
                return new RadioUnitMonitorCommandExtended(message, offset);
            case PHASE1_214_SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                return new SNDCPDataChannelAnnouncementExplicit(message, offset);
            case PHASE1_216_STATUS_UPDATE_EXTENDED:
                return new StatusUpdateExtended(message, offset);
            case PHASE1_218_STATUS_QUERY_EXTENDED:
                return new StatusQueryExtended(message, offset);
            case PHASE1_220_MESSAGE_UPDATE_EXTENDED:
                return new MessageUpdateExtended(message, offset);
            case PHASE1_223_CALL_ALERT_EXTENDED:
                return new CallAlertExtended(message, offset);
            case PHASE1_228_EXTENDED_FUNCTION_COMMAND_EXTENDED:
                return new ExtendedFunctionCommandExtended(message, offset);
            case PHASE1_233_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                return new SecondaryControlChannelBroadcastExplicit(message, offset);
            case PHASE1_234_GROUP_AFFILIATION_QUERY_EXTENDED:
                return new GroupAffiliationQueryExtended(message, offset);
            case PHASE1_250_RFSS_STATUS_BROADCAST_EXTENDED:
                return new RfssStatusBroadcastExtended(message, offset);
            case PHASE1_251_NETWORK_STATUS_BROADCAST_EXTENDED:
                return new NetworkStatusBroadcastExtended(message, offset);
            case PHASE1_252_ADJACENT_STATUS_BROADCAST_EXTENDED:
                return new AdjacentStatusBroadcastExtended(message, offset);

            case VENDOR_PARTITION_2_UNKNOWN_OPCODE:
                return new UnknownVendorMessage(message, offset);
        }

        return new UnknownStructure(message, offset);
    }
}
