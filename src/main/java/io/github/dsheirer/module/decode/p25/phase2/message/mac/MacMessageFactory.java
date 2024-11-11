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

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCP25;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AcknowledgeResponseFNEAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AcknowledgeResponseFNEExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastExtendedExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AdjacentStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AuthenticationDemand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AuthenticationFNEResponseAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AuthenticationFNEResponseExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.CallAlertAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.CallAlertExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.CallAlertExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.DenyResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.EndPushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateTDMAAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateTDMAExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.FrequencyBandUpdateVUHF;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationResponseAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationResponseExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupRegroupVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateMultipleExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateMultipleImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.IndirectGroupPagingWithoutPriority;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.IndividualPagingWithPriority;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.LocationRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacRelease;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureFailedPDUCRC;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVariableLength;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MultiFragmentContinuationMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NullAvoidZeroBiasInformation;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NullInformation;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PowerControlSignalQuality;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.QueuedResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorEnhancedCommandAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorEnhancedCommandExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RoamingAddressCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RoamingAddressUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataChannelAnnouncement;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataPageRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SecondaryControlChannelBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SynchronizationBroadcast;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SystemServiceBroadcast;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelGrantExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelGrantImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelGrantUpdateImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TimeAndDateAnnouncement;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitDeRegistrationAcknowledge;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitRegistrationCommandAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitRegistrationResponseAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitRegistrationResponseExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceServiceChannelGrantAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceServiceChannelGrantExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceServiceChannelGrantExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnknownMacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnknownVendorMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisGroupRegroupExplicitEncryptionCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisPrivateDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisTalkerAlias;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisTalkerGpsLocation;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisUnitToUnitDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisUnknownOpcode129;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisUnknownOpcode143;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.UnknownOpcode136;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaActiveGroupRadiosOpcode130_x82;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaActiveGroupRadiosOpcode143_x8F;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaActiveGroupRadiosOpcode191_xBF;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaDenyResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupAddCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupChannelGrantExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupChannelGrantImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupDeleteCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaQueuedResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaTDMADataChannel;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaTalkerAliasDataBlock;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaTalkerAliasHeader;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaUnknownOpcode135;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
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

    /**
     * Creates a set of MAC messages
     * @param timeslot for the messages
     * @param dataUnitID indicates the type of message
     * @param message to process
     * @param timestamp for the message
     * @param maxIndex is the maximum bit index that contains octet data in this message
     * @return list of one or more mac messages
     */
    public static List<MacMessage> create(int timeslot, DataUnitID dataUnitID, CorrectedBinaryMessage message,
                                          long timestamp, int maxIndex)
    {
        List<MacMessage> messages = new ArrayList<>();

        boolean passesCRC = true;

        int nac = 0;

        if(dataUnitID.isFACCH())
        {
            passesCRC = CRCP25.crc12_FACCH(message);
        }
        else if(dataUnitID.isLCCH())
        {
            passesCRC = CRCP25.crc16_LCCH(message);

            if(passesCRC)
            {
                nac = MacStructure.getLcchNac(message);
            }
        }
        else if(dataUnitID.isSACCH())
        {
            passesCRC = CRCP25.crc12_SACCH(message);
        }

        if(!passesCRC)
        {
            MacMessage mac = new MacMessage(timeslot, dataUnitID, message, timestamp, new MacStructureFailedPDUCRC(message, 0));
            mac.setValid(false);
            messages.add(mac);
            return messages;
        }

        MacPduType macPduType = MacMessage.getMacPduTypeFromMessage(message);

        switch(macPduType)
        {
            case MAC_1_PTT:
                messages.add(new MacMessage(timeslot, dataUnitID, message, timestamp, new PushToTalk(message)));
                break;
            case MAC_2_END_PTT:
                messages.add(new MacMessage(timeslot, dataUnitID, message, timestamp, new EndPushToTalk(message)));
                break;
            case MAC_0_SIGNAL:
            case MAC_3_IDLE:
            case MAC_4_ACTIVE:
            case MAC_6_HANGTIME:
                List<Integer> indices = getMacStructureIndices(message, maxIndex);

                for(Integer index : indices)
                {
                    MacStructure macStructure = createMacStructure(message, index);
                    MacMessage macMessage = new MacMessage(timeslot, dataUnitID, message, timestamp, macStructure);
                    messages.add(macMessage);
                }
                break;
            default:
                messages.add(new UnknownMacMessage(timeslot, dataUnitID, message, timestamp));
                break;
        }

        //Assign the nac value parsed from the LCCH MAC PDU content paylaod when it is non-zero
        if(nac > 0)
        {
            for(MacMessage macMessage: messages)
            {
                macMessage.setNAC(nac);
            }
        }


        return messages;
    }

    /**
     * Identifies the MAC structure start indices for the message
     *
     * @param message containing one or more MAC structures
     * @param maxIndex is the maximum bit index that is parseable in this structure
     * @return structure start indices
     */
    private static List<Integer> getMacStructureIndices(CorrectedBinaryMessage message, int maxIndex)
    {
        List<Integer> indices = new ArrayList<>();

        //There should always be a MAC structure at index 8
        indices.add(DEFAULT_MAC_STRUCTURE_INDEX);

        MacOpcode opcode = MacStructure.getOpcode(message, DEFAULT_MAC_STRUCTURE_INDEX);
        opcode = checkVendorOpcode(opcode, message, DEFAULT_MAC_STRUCTURE_INDEX);

        int opcodeLength = getLength(opcode, message, DEFAULT_MAC_STRUCTURE_INDEX, maxIndex);

        if(opcodeLength > 0 && opcode != MacOpcode.TDMA_00_NULL_INFORMATION_MESSAGE)
        {
            int secondStructureIndex = DEFAULT_MAC_STRUCTURE_INDEX + (opcodeLength * 8);

            if(secondStructureIndex < maxIndex)
            {
                MacOpcode secondOpcode = MacStructure.getOpcode(message, secondStructureIndex);
                secondOpcode = checkVendorOpcode(secondOpcode, message, secondStructureIndex);

                if(secondOpcode != MacOpcode.TDMA_00_NULL_INFORMATION_MESSAGE)
                {
                    indices.add(secondStructureIndex);

                    opcodeLength = getLength(secondOpcode, message, secondStructureIndex, maxIndex);

                    if(opcodeLength > 0)
                    {
                        int thirdStructureIndex = secondStructureIndex + (opcodeLength * 8);

                        if(thirdStructureIndex < maxIndex)
                        {
                            MacOpcode thirdOpcode = MacStructure.getOpcode(message, thirdStructureIndex);

                            if(thirdOpcode != MacOpcode.TDMA_00_NULL_INFORMATION_MESSAGE)
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
     * Calculates the structure length for the opcode.
     * @param opcode for the structure
     * @param message containing bits
     * @param offset to the start of the structure being inspected
     * @param maxIndex for this PDU content field.
     * @return length of the structure in octets.
     */
    private static int getLength(MacOpcode opcode, CorrectedBinaryMessage message, int offset, int maxIndex)
    {
        //How many octets are available in the message - total length minus offset minus 12 bits of CRC
        int availableOctets = Math.ceilDiv(maxIndex - offset, 8);

        if(opcode.isVariableLength())
        {
            if(opcode.isVendorPartition())
            {
                return MacStructureVendor.getLength(message, offset);
            }
            else
            {
                return MacStructureVariableLength.getLength(message, offset);
            }
        }
        else if(opcode.isUnknownLength())
        {
            return availableOctets;
        }
        else
        {
            return opcode.getLength();
        }
    }

    /**
     * Checks to see if the opcode is a vendor opcode and returns the correct vendor version of the opcode.
     * @param opcode to check
     * @param message that possibly contains a vendor identifier
     * @param index to the start of the mac structure in this message
     * @return original opcode or the vendor opcode when appropriate.
     */
    private static MacOpcode checkVendorOpcode(MacOpcode opcode, CorrectedBinaryMessage message, int index)
    {
        if(opcode == MacOpcode.VENDOR_PARTITION_2_UNKNOWN_OPCODE)
        {
            Vendor vendor = MacStructureVendor.getVendor(message, index);
            int opcodeNumber = MacStructure.getOpcodeNumber(message, index);
            return MacOpcode.fromValue(opcodeNumber, vendor);
        }

        return opcode;
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
        return createMacStructure(message, offset, opcode);
    }

    /**
     * Creates a MAC structure parser for the message with the specified structure start offset and opcode
     *
     * @param message containing a MAC structure
     * @param offset to the start of the structure
     * @return MAC structure parser
     */
    public static MacStructure createMacStructure(CorrectedBinaryMessage message, int offset, MacOpcode opcode)
    {
        switch(opcode)
        {
            case TDMA_00_NULL_INFORMATION_MESSAGE:
                return new NullInformation(message, offset);
            case TDMA_01_GROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                return new GroupVoiceChannelUserAbbreviated(message, offset);
            case TDMA_02_UNIT_TO_UNIT_VOICE_CHANNEL_USER_ABBREVIATED:
                return new UnitToUnitVoiceChannelUserAbbreviated(message, offset);
            case TDMA_03_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                return new TelephoneInterconnectVoiceChannelUser(message, offset);
            case TDMA_05_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_IMPLICIT:
                return new GroupVoiceChannelGrantUpdateMultipleImplicit(message, offset);
            case TDMA_08_NULL_AVOID_ZERO_BIAS:
                return new NullAvoidZeroBiasInformation(message, offset);
            case TDMA_10_MULTI_FRAGMENT_CONTINUATION_MESSAGE:
                return new MultiFragmentContinuationMessage(message, offset);
            case TDMA_11_INDIRECT_GROUP_PAGING_WITHOUT_PRIORITY:
                return new IndirectGroupPagingWithoutPriority(message, offset);
            case TDMA_12_INDIVIDUAL_PAGING_WITH_PRIORITY:
                return new IndividualPagingWithPriority(message, offset);
            case TDMA_21_GROUP_VOICE_CHANNEL_USER_EXTENDED:
                return new GroupVoiceChannelUserExtended(message, offset);
            case TDMA_22_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
                return new UnitToUnitVoiceChannelUserExtended(message, offset);
            case TDMA_25_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_EXPLICIT:
                return new GroupVoiceChannelGrantUpdateMultipleExplicit(message, offset);
            case TDMA_30_POWER_CONTROL_SIGNAL_QUALITY:
                return new PowerControlSignalQuality(message, offset);
            case TDMA_31_MAC_RELEASE:
                return new MacRelease(message, offset);
            case PHASE1_40_GROUP_VOICE_CHANNEL_GRANT_IMPLICIT:
                return new GroupVoiceChannelGrantImplicit(message, offset);
            case PHASE1_41_GROUP_VOICE_SERVICE_REQUEST:
                return new GroupVoiceServiceRequest(message, offset);
            case PHASE1_42_GROUP_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
                return new GroupVoiceChannelGrantUpdateImplicit(message, offset);
            case PHASE1_44_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_ABBREVIATED:
                return new UnitToUnitVoiceServiceChannelGrantAbbreviated(message, offset);
            case PHASE1_45_UNIT_TO_UNIT_ANSWER_REQUEST_ABBREVIATED:
                return new UnitToUnitAnswerRequestAbbreviated(message, offset);
            case PHASE1_46_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED:
                return new UnitToUnitVoiceChannelGrantUpdateAbbreviated(message, offset);
            case PHASE1_48_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_IMPLICIT:
                return new TelephoneInterconnectVoiceChannelGrantImplicit(message, offset);
            case PHASE1_49_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
                return new TelephoneInterconnectVoiceChannelGrantUpdateImplicit(message, offset);
            case PHASE1_4A_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
                return new TelephoneInterconnectAnswerRequest(message, offset);
            case PHASE1_4C_RADIO_UNIT_MONITOR_COMMAND_ABBREVIATED:
                return new RadioUnitMonitorCommandAbbreviated(message, offset);
            case PHASE1_54_SNDCP_DATA_CHANNEL_GRANT:
                return new SNDCPDataChannelGrant(message, offset);
            case PHASE1_55_SNDCP_DATA_PAGE_REQUEST:
                return new SNDCPDataPageRequest(message, offset);
            case PHASE1_58_STATUS_UPDATE_ABBREVIATED:
                return new StatusUpdateAbbreviated(message, offset);
            case PHASE1_5A_STATUS_QUERY_ABBREVIATED:
                return new StatusQueryAbbreviated(message, offset);
            case PHASE1_5D_RADIO_UNIT_MONITOR_COMMAND_OBSOLETE:
                return new UnknownMacStructure(message, offset); //Message is obsolete -- return unknown
            case PHASE1_5C_MESSAGE_UPDATE_ABBREVIATED:
                return new MessageUpdateAbbreviated(message, offset);
            case PHASE1_5E_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_ABBREVIATED:
                return new RadioUnitMonitorEnhancedCommandAbbreviated(message, offset);
            case PHASE1_5F_CALL_ALERT_ABBREVIATED:
                return new CallAlertAbbreviated(message, offset);
            case PHASE1_60_ACKNOWLEDGE_RESPONSE_FNE_ABBREVIATED:
                return new AcknowledgeResponseFNEAbbreviated(message, offset);
            case PHASE1_61_QUEUED_RESPONSE:
                return new QueuedResponse(message, offset);
            case PHASE1_64_EXTENDED_FUNCTION_COMMAND_ABBREVIATED:
                return new ExtendedFunctionCommandAbbreviated(message, offset);
            case PHASE1_67_DENY_RESPONSE:
                return new DenyResponse(message, offset);
            case PHASE1_68_GROUP_AFFILIATION_RESPONSE_ABBREVIATED:
                return new GroupAffiliationResponseAbbreviated(message, offset);
            case PHASE1_6A_GROUP_AFFILIATION_QUERY_ABBREVIATED:
                return new GroupAffiliationQueryAbbreviated(message, offset);
            case PHASE1_6B_LOCATION_REGISTRATION_RESPONSE:
                return new LocationRegistrationResponse(message, offset);
            case PHASE1_6C_UNIT_REGISTRATION_RESPONSE_ABBREVIATED:
                return new UnitRegistrationResponseAbbreviated(message, offset);
            case PHASE1_6D_UNIT_REGISTRATION_COMMAND_ABBREVIATED:
                return new UnitRegistrationCommandAbbreviated(message, offset);
            case PHASE1_6F_DEREGISTRATION_ACKNOWLEDGE:
                return new UnitDeRegistrationAcknowledge(message, offset);
            case PHASE1_70_SYNCHRONIZATION_BROADCAST:
                return new SynchronizationBroadcast(message, offset);
            case PHASE1_71_AUTHENTICATION_DEMAND:
                return new AuthenticationDemand(message, offset);
            case PHASE1_72_AUTHENTICATION_FNE_RESPONSE_ABBREVIATED:
                return new AuthenticationFNEResponseAbbreviated(message, offset);
            case PHASE1_73_IDENTIFIER_UPDATE_TDMA_ABBREVIATED:
                return new FrequencyBandUpdateTDMAAbbreviated(message, offset);
            case PHASE1_74_IDENTIFIER_UPDATE_V_UHF:
                return new FrequencyBandUpdateVUHF(message, offset);
            case PHASE1_75_TIME_AND_DATE_ANNOUNCEMENT:
                return new TimeAndDateAnnouncement(message, offset);
            case PHASE1_76_ROAMING_ADDRESS_COMMAND:
                return new RoamingAddressCommand(message, offset);
            case PHASE1_77_ROAMING_ADDRESS_UPDATE:
                return new RoamingAddressUpdate(message, offset);
            case PHASE1_78_SYSTEM_SERVICE_BROADCAST:
                return new SystemServiceBroadcast(message, offset);
            case PHASE1_79_SECONDARY_CONTROL_CHANNEL_BROADCAST_IMPLICIT:
                return new SecondaryControlChannelBroadcastImplicit(message, offset);
            case PHASE1_7A_RFSS_STATUS_BROADCAST_IMPLICIT:
                return new RfssStatusBroadcastImplicit(message, offset);
            case PHASE1_7B_NETWORK_STATUS_BROADCAST_IMPLICIT:
                return new NetworkStatusBroadcastImplicit(message, offset);
            case PHASE1_7C_ADJACENT_STATUS_BROADCAST_IMPLICIT:
                return new AdjacentStatusBroadcastImplicit(message, offset);
            case PHASE1_7D_IDENTIFIER_UPDATE:
                return new FrequencyBandUpdate(message, offset);
            case PHASE1_88_UNKNOWN_LCCH_OPCODE:
                return new UnknownOpcode136(message, offset);
            case PHASE1_90_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                return new GroupRegroupVoiceChannelUserAbbreviated(message, offset);
            case PHASE1_C0_GROUP_VOICE_CHANNEL_GRANT_EXPLICIT:
                return new GroupVoiceChannelGrantExplicit(message, offset);
            case PHASE1_C3_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                return new GroupVoiceChannelGrantUpdateExplicit(message, offset);
            case PHASE1_C4_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_EXTENDED_VCH:
                return new UnitToUnitVoiceServiceChannelGrantExtendedVCH(message, offset);
            case PHASE1_C5_UNIT_TO_UNIT_ANSWER_REQUEST_EXTENDED:
                return new UnitToUnitAnswerRequestExtended(message, offset);
            case PHASE1_C6_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_VCH:
                return new UnitToUnitVoiceChannelGrantUpdateExtendedVCH(message, offset);
            case PHASE1_C7_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_LCCH:
                return new UnitToUnitVoiceChannelGrantUpdateExtendedLCCH(message, offset);
            case PHASE1_C8_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_EXPLICIT:
                return new TelephoneInterconnectVoiceChannelGrantExplicit(message, offset);
            case PHASE1_C9_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                return new TelephoneInterconnectVoiceChannelGrantUpdateExplicit(message, offset);
            case PHASE1_CB_CALL_ALERT_EXTENDED_LCCH:
                return new CallAlertExtendedLCCH(message, offset);
            case PHASE1_CC_RADIO_UNIT_MONITOR_COMMAND_EXTENDED_VCH:
                return new RadioUnitMonitorCommandExtendedVCH(message, offset);
            case PHASE1_CD_RADIO_UNIT_MONITOR_COMMAND_EXTENDED_LCCH:
                return new RadioUnitMonitorCommandExtendedLCCH(message, offset);
            case PHASE1_CE_MESSAGE_UPDATE_EXTENDED_LCCH:
                return new MessageUpdateExtendedLCCH(message, offset);
            case PHASE1_CF_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_EXTENDED_LCCH:
                return new UnitToUnitVoiceServiceChannelGrantExtendedLCCH(message, offset);
            case PHASE1_D6_SNDCP_DATA_CHANNEL_ANNOUNCEMENT:
                return new SNDCPDataChannelAnnouncement(message, offset);
            case PHASE1_D8_STATUS_UPDATE_EXTENDED_VCH:
                return new StatusUpdateExtendedVCH(message, offset);
            case PHASE1_D9_STATUS_UPDATE_EXTENDED_LCCH:
                return new StatusUpdateExtendedLCCH(message, offset);
            case PHASE1_DA_STATUS_QUERY_EXTENDED_VCH:
                return new StatusQueryExtendedVCH(message, offset);
            case PHASE1_DB_STATUS_QUERY_EXTENDED_LCCH:
                return new StatusQueryExtendedLCCH(message, offset);
            case PHASE1_DC_MESSAGE_UPDATE_EXTENDED_VCH:
                return new MessageUpdateExtendedVCH(message, offset);
            case PHASE1_DE_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_EXTENDED:
                return new RadioUnitMonitorEnhancedCommandExtended(message, offset);
            case PHASE1_DF_CALL_ALERT_EXTENDED_VCH:
                return new CallAlertExtendedVCH(message, offset);
            case PHASE1_E0_ACKNOWLEDGE_RESPONSE_FNE_EXTENDED:
                return new AcknowledgeResponseFNEExtended(message, offset);
            case PHASE1_E4_EXTENDED_FUNCTION_COMMAND_EXTENDED_VCH:
                return new ExtendedFunctionCommandExtendedVCH(message, offset);
            case PHASE1_E5_EXTENDED_FUNCTION_COMMAND_EXTENDED_LCCH:
                return new ExtendedFunctionCommandExtendedLCCH(message, offset);
            case PHASE1_E8_GROUP_AFFILIATION_RESPONSE_EXTENDED:
                return new GroupAffiliationResponseExtended(message, offset);
            case PHASE1_E9_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                return new SecondaryControlChannelBroadcastExplicit(message, offset);
            case PHASE1_EA_GROUP_AFFILIATION_QUERY_EXTENDED:
                return new GroupAffiliationQueryExtended(message, offset);
            case PHASE1_EC_UNIT_REGISTRATION_RESPONSE_EXTENDED:
                return new UnitRegistrationResponseExtended(message, offset);
            case PHASE1_F2_AUTHENTICATION_FNE_RESPONSE_EXTENDED:
                return new AuthenticationFNEResponseExtended(message, offset);
            case PHASE1_F3_IDENTIFIER_UPDATE_TDMA_EXTENDED:
                return new FrequencyBandUpdateTDMAExtended(message, offset);
            case PHASE1_FA_RFSS_STATUS_BROADCAST_EXPLICIT:
                return new RfssStatusBroadcastExplicit(message, offset);
            case PHASE1_FB_NETWORK_STATUS_BROADCAST_EXPLICIT:
                return new NetworkStatusBroadcastExplicit(message, offset);
            case PHASE1_FC_ADJACENT_STATUS_BROADCAST_EXPLICIT:
                return new AdjacentStatusBroadcastExplicit(message, offset);
            case PHASE1_FE_ADJACENT_STATUS_BROADCAST_EXTENDED_EXPLICIT:
                return new AdjacentStatusBroadcastExtendedExplicit(message, offset);

            case L3HARRIS_81_UNKNOWN_OPCODE_129:
                return new L3HarrisUnknownOpcode129(message, offset);
            case L3HARRIS_8F_UNKNOWN_OPCODE_143:
                return new L3HarrisUnknownOpcode143(message, offset);
            case L3HARRIS_A0_PRIVATE_DATA_CHANNEL_GRANT:
                return new L3HarrisPrivateDataChannelGrant(message, offset);
            case L3HARRIS_AA_GPS_LOCATION:
                return new L3HarrisTalkerGpsLocation(message, offset);
            case L3HARRIS_A8_TALKER_ALIAS:
                return new L3HarrisTalkerAlias(message, offset);
            case L3HARRIS_AC_UNIT_TO_UNIT_DATA_CHANNEL_GRANT:
                return new L3HarrisUnitToUnitDataChannelGrant(message, offset);
            case L3HARRIS_B0_GROUP_REGROUP_EXPLICIT_ENCRYPTION_COMMAND:
                return new L3HarrisGroupRegroupExplicitEncryptionCommand(message, offset);

            case MOTOROLA_80_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                return new MotorolaGroupRegroupVoiceChannelUserAbbreviated(message, offset);
            case MOTOROLA_81_GROUP_REGROUP_ADD:
                return new MotorolaGroupRegroupAddCommand(message, offset);
            case MOTOROLA_82_ACTIVE_GROUP_RADIOS_130:
                return new MotorolaActiveGroupRadiosOpcode130_x82(message, offset);
            case MOTOROLA_83_GROUP_REGROUP_VOICE_CHANNEL_UPDATE:
                return new MotorolaGroupRegroupVoiceChannelUpdate(message, offset);
            case MOTOROLA_84_GROUP_REGROUP_EXTENDED_FUNCTION_COMMAND:
                return new MotorolaGroupRegroupExtendedFunctionCommand(message, offset);
            case MOTOROLA_87_UNKNOWN_OPCODE_135:
                return new MotorolaUnknownOpcode135(message, offset);
            case MOTOROLA_89_GROUP_REGROUP_DELETE:
                return new MotorolaGroupRegroupDeleteCommand(message, offset);
            case MOTOROLA_8B_TDMA_DATA_CHANNEL:
                return new MotorolaTDMADataChannel(message, offset);
            case MOTOROLA_8F_ACTIVE_GROUP_RADIOS_143:
                return new MotorolaActiveGroupRadiosOpcode143_x8F(message, offset);
            case MOTOROLA_BF_ACTIVE_GROUP_RADIOS_191:
                return new MotorolaActiveGroupRadiosOpcode191_xBF(message, offset);
            case MOTOROLA_91_TALKER_ALIAS_HEADER:
                return new MotorolaTalkerAliasHeader(message, offset);
            case MOTOROLA_95_TALKER_ALIAS_DATA_BLOCK:
                return new MotorolaTalkerAliasDataBlock(message, offset);
            case MOTOROLA_A0_GROUP_REGROUP_VOICE_CHANNEL_USER_EXTENDED:
                return new MotorolaGroupRegroupVoiceChannelUserExtended(message, offset);
            case MOTOROLA_A3_GROUP_REGROUP_CHANNEL_GRANT_IMPLICIT:
                return new MotorolaGroupRegroupChannelGrantImplicit(message, offset);
            case MOTOROLA_A4_GROUP_REGROUP_CHANNEL_GRANT_EXPLICIT:
                return new MotorolaGroupRegroupChannelGrantExplicit(message, offset);
            case MOTOROLA_A5_GROUP_REGROUP_CHANNEL_GRANT_UPDATE:
                return new MotorolaGroupRegroupChannelGrantUpdate(message, offset);
            case MOTOROLA_A6_QUEUED_RESPONSE:
                return new MotorolaQueuedResponse(message, offset);
            case MOTOROLA_A7_DENY_RESPONSE:
                return new MotorolaDenyResponse(message, offset);
            case MOTOROLA_A8_ACKNOWLEDGE_RESPONSE:
                return new MotorolaAcknowledgeResponse(message, offset);

            case VENDOR_PARTITION_2_UNKNOWN_OPCODE:
                Vendor vendor = MacStructureVendor.getVendor(message, offset);
                int opcodeNumber = MacStructure.getOpcodeNumber(message, offset);

                //L3Harris GPS seems to have an extra octet (0x80) where the opcode should be and the true opcode 0xAA is
                //possibly one byte to the right... adjust for this.
                if(vendor == Vendor.V170 && opcodeNumber == 0x80)
                {
                    Vendor candidateVendor = MacStructureVendor.getVendor(message, offset + 8);

                    if(candidateVendor == Vendor.HARRIS)
                    {
                        return createMacStructure(message, offset + 8, MacOpcode.L3HARRIS_AA_GPS_LOCATION);
                    }
                }

                MacOpcode vendorOpcode = MacOpcode.fromValue(opcodeNumber, vendor);

                if(vendorOpcode != MacOpcode.VENDOR_PARTITION_2_UNKNOWN_OPCODE)
                {
                    return createMacStructure(message, offset, vendorOpcode);
                }

                return new UnknownVendorMessage(message, offset);
        }

        return new UnknownMacStructure(message, offset);
    }
}
