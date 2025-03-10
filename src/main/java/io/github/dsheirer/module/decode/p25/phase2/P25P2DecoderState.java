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
package io.github.dsheirer.module.decode.p25.phase2;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.channel.state.ChangeChannelTimeoutEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.channel.state.TimeslotDecoderState;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierUpdateListener;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.alias.P25TalkerAliasIdentifier;
import io.github.dsheirer.identifier.encryption.EncryptionKey;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.identifier.patch.PatchGroupPreLoadDataContent;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.P25DecodeEvent;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.MotorolaTalkerAliasComplete;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.IP25ChannelGrantDetailProvider;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacPduType;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AcknowledgeResponseFNEAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AcknowledgeResponseFNEExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AuthenticationDemand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AuthenticationFNEResponseAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AuthenticationFNEResponseExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.DenyResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.EndPushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationResponseAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationResponseExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupRegroupVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateMultipleExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantUpdateMultipleImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.IndirectGroupPagingWithoutPriority;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.IndividualPagingWithPriority;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.LocationRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacRelease;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.NetworkStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PowerControlSignalQuality;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.QueuedResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorEnhancedCommandAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorEnhancedCommandExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RfssStatusBroadcastImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RoamingAddressCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RoamingAddressUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataPageRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelGrantUpdateImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisGroupRegroupExplicitEncryptionCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisTalkerAlias;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisTalkerGpsLocation;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaAcknowledgeResponse;
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
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.DatchTimeslot;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder state for an APCO-25 Phase II channel.  Maintains the call/control/data/idle state of the channel and
 * produces events by monitoring the decoded message stream.
 */
public class P25P2DecoderState extends TimeslotDecoderState implements IdentifierUpdateListener
{
    private static final Logger LOGGER = LoggerFactory.getLogger(P25P2DecoderState.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(LOGGER);
    private Channel mChannel;
    private PatchGroupManager mPatchGroupManager;
    private P25P2NetworkConfigurationMonitor mNetworkConfigurationMonitor = new P25P2NetworkConfigurationMonitor();
    private P25TrafficChannelManager mTrafficChannelManager;
    private int mEndPttOnFacchCounter = 0;

    /**
     * Constructs an APCO-25 decoder state instance for a traffic or control channel.
     *
     * @param channel with configuration details
     * @param timeslot for this decoder state
     * @param trafficChannelManager to coordinate traffic channel activity
     * @param patchGroupManager instance shared across both timeslots
     */
    public P25P2DecoderState(Channel channel, int timeslot, P25TrafficChannelManager trafficChannelManager,
                             PatchGroupManager patchGroupManager)
    {
        super(timeslot);
        mChannel = channel;
        mTrafficChannelManager = trafficChannelManager;
        mPatchGroupManager = patchGroupManager;
    }

    /**
     * Identifies the decoder type
     */
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE2;
    }

    /**
     * Performs a full reset to prepare this object for reuse on a new channel
     */
    @Override
    public void reset()
    {
        super.reset();
        resetState();
    }

    /**
     * Resets any temporal state details
     */
    protected void resetState()
    {
        super.resetState();
        mTrafficChannelManager.processP2TrafficCallEnd(getCurrentFrequency(), getTimeslot(), System.currentTimeMillis(), "RESET STATE INVOKED");
        mEndPttOnFacchCounter = 0;
    }

    /**
     * Processes an identifier collection to harvest Patch Groups to preload when this channel is first starting up.
     *
     * @param preLoadDataContent containing an identifier collection with optional patch group identifier(s).
     */
    @Subscribe
    public void process(PatchGroupPreLoadDataContent preLoadDataContent)
    {
        //Only do this on timeslot 1 since both timeslots are sharing the same patch group manager.
        if(getTimeslot() == P25P1Message.TIMESLOT_1)
        {
            for(Identifier identifier : preLoadDataContent.getData().getIdentifiers(Role.TO))
            {
                if(identifier instanceof PatchGroupIdentifier patchGroupIdentifier)
                {
                    mPatchGroupManager.addPatchGroup(patchGroupIdentifier, preLoadDataContent.getTimestamp());
                }
            }
        }
    }

    /**
     * Primary message processing method.
     */
    @Override
    public void receive(IMessage message)
    {
        if(message.isValid() && message.getTimeslot() == getTimeslot())
        {
            if(message instanceof MacMessage macMessage)
            {
                processMacMessage(macMessage);
            }
            else if(message instanceof AbstractVoiceTimeslot)
            {
                if(isEncrypted())
                {
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ENCRYPTED, getTimeslot()));
                }
                else
                {
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL, getTimeslot()));
                }

                //If we're tracking the call event, update the duration on it
                mTrafficChannelManager.processP2TrafficVoice(getCurrentFrequency(), getTimeslot(), message.getTimestamp());
            }
            //Motorola TDMA data channel.
            else if(message instanceof DatchTimeslot)
            {
                broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA, getTimeslot()));
            }
            else if(message instanceof EncryptionSynchronizationSequence ess)
            {
                //We don't send any state events for this message since it can only occur in conjunction with
                //an audio frame that already sends the call state event
                getIdentifierCollection().update(message.getIdentifiers());
                mTrafficChannelManager.processP2TrafficCurrentUser(getCurrentFrequency(), getTimeslot(), ess.getEncryptionKey(), ess.getTimestamp());

                if(ess.isEncrypted())
                {
                    continueState(State.ENCRYPTED);
                }
                else
                {
                    continueState(State.CALL);
                }
            }
            else if(message instanceof MotorolaTalkerAliasComplete tac && tac.isValid())
            {
                mTrafficChannelManager.getTalkerAliasManager().update(tac.getRadio(), tac.getAlias());
            }
        }
    }

    /**
     * Process MAC message structures
     */
    private void processMacMessage(MacMessage message)
    {
        MacPduType macPduType = message.getMacPduType();

        switch(macPduType)
        {
            case MAC_0_SIGNAL:
                continueState(State.CONTROL);
                break;
            case MAC_6_HANGTIME:
                //During hangtime, the from talker is no longer involved in the call ... remove it because sometimes
                //we don't get the end PTT to signal the end.
                getIdentifierCollection().remove(Role.FROM);
                break;
        }

        MacStructure mac = message.getMacStructure();

        switch((mac.getOpcode()))
        {
            /**
             * Partition 0 Opcodes
             */
            case TDMA_PARTITION_0_UNKNOWN_OPCODE:
                break;
            case PUSH_TO_TALK:
                processPushToTalk(message, mac);
                break;
            case END_PUSH_TO_TALK:
                processEndPushToTalk(message, mac);
                break;
            case TDMA_00_NULL_INFORMATION_MESSAGE:
                processNullInformation(message, mac);
                break;
            case TDMA_01_GROUP_VOICE_CHANNEL_USER_ABBREVIATED:
            case TDMA_02_UNIT_TO_UNIT_VOICE_CHANNEL_USER_ABBREVIATED:
            case TDMA_03_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
            case TDMA_21_GROUP_VOICE_CHANNEL_USER_EXTENDED:
            case TDMA_22_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
                processChannelUser(message, mac);
                break;
            case TDMA_05_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_IMPLICIT:
            case TDMA_25_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_EXPLICIT:
            case PHASE1_42_GROUP_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
            case PHASE1_46_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED:
            case PHASE1_49_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
            case PHASE1_C3_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
            case PHASE1_C6_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_VCH:
            case PHASE1_C7_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_LCCH:
            case PHASE1_C9_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                processChannelGrantUpdate(message, mac);
                break;
            case TDMA_08_NULL_AVOID_ZERO_BIAS:
                //This is a filler message - ignore
                break;
            case TDMA_10_MULTI_FRAGMENT_CONTINUATION_MESSAGE:
                //Ignore - this is a continuation message that doesn't mean anything by itself
                break;
            case TDMA_11_INDIRECT_GROUP_PAGING_WITHOUT_PRIORITY:
            case TDMA_12_INDIVIDUAL_PAGING_WITH_PRIORITY:
                processPaging(message, mac);
                break;
            case TDMA_30_POWER_CONTROL_SIGNAL_QUALITY:
                processPowerControl(message, mac);
                break;
            case TDMA_31_MAC_RELEASE:
                processMacRelease(message, mac);
                break;

            /**
             * Partition 1 Opcodes
             */
            case PHASE1_PARTITION_1_UNKNOWN_OPCODE:
                break;
            case PHASE1_40_GROUP_VOICE_CHANNEL_GRANT_IMPLICIT:
            case PHASE1_44_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_ABBREVIATED:
            case PHASE1_48_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_IMPLICIT:
            case PHASE1_54_SNDCP_DATA_CHANNEL_GRANT:
            case PHASE1_C8_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_EXPLICIT:
            case PHASE1_C0_GROUP_VOICE_CHANNEL_GRANT_EXPLICIT:
            case PHASE1_C4_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_EXTENDED_VCH:
            case PHASE1_CF_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_EXTENDED_LCCH:
                processChannelGrant(message, mac);
                break;
            case PHASE1_41_GROUP_VOICE_SERVICE_REQUEST:
                //Inbound only.
                break;
            case PHASE1_45_UNIT_TO_UNIT_ANSWER_REQUEST_ABBREVIATED:
            case PHASE1_4A_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
            case PHASE1_C5_UNIT_TO_UNIT_ANSWER_REQUEST_EXTENDED:
                processAnswer(message, mac);
                break;
            case PHASE1_4C_RADIO_UNIT_MONITOR_COMMAND_ABBREVIATED:
            case PHASE1_5D_RADIO_UNIT_MONITOR_COMMAND_OBSOLETE:
            case PHASE1_5E_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_ABBREVIATED:
            case PHASE1_CC_RADIO_UNIT_MONITOR_COMMAND_EXTENDED_VCH:
            case PHASE1_CD_RADIO_UNIT_MONITOR_COMMAND_EXTENDED_LCCH:
            case PHASE1_DE_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_EXTENDED:
                processRadioUnitMonitor(message, mac);
                break;
            case PHASE1_52_SNDCP_DATA_CHANNEL_REQUEST:
                //Ignore - this is an inbound request by the SU
                break;
            case PHASE1_53_SNDCP_DATA_PAGE_RESPONSE:
                //Ignore - this is an inbound request by the SU
                break;
            case PHASE1_55_SNDCP_DATA_PAGE_REQUEST:
                processDataPageRequest(message, mac);
                break;
            case PHASE1_58_STATUS_UPDATE_ABBREVIATED:
            case PHASE1_5A_STATUS_QUERY_ABBREVIATED:
            case PHASE1_D8_STATUS_UPDATE_EXTENDED_VCH:
            case PHASE1_D9_STATUS_UPDATE_EXTENDED_LCCH:
            case PHASE1_DA_STATUS_QUERY_EXTENDED_VCH:
            case PHASE1_DB_STATUS_QUERY_EXTENDED_LCCH:
                processStatus(message, mac);
                break;
            case PHASE1_5C_MESSAGE_UPDATE_ABBREVIATED:
            case PHASE1_CE_MESSAGE_UPDATE_EXTENDED_LCCH:
            case PHASE1_DC_MESSAGE_UPDATE_EXTENDED_VCH:
                processMessageUpdate(message, mac);
                break;
            case PHASE1_5F_CALL_ALERT_ABBREVIATED:
            case PHASE1_CB_CALL_ALERT_EXTENDED_LCCH:
            case PHASE1_DF_CALL_ALERT_EXTENDED_VCH:
                processCallAlert(message, mac);
                break;
            case PHASE1_60_ACKNOWLEDGE_RESPONSE_FNE_ABBREVIATED:
            case PHASE1_E0_ACKNOWLEDGE_RESPONSE_FNE_EXTENDED:
                processAcknowledge(message, mac);
                break;
            case PHASE1_61_QUEUED_RESPONSE:
                processQueued(message, mac);
                break;
            case PHASE1_64_EXTENDED_FUNCTION_COMMAND_ABBREVIATED:
            case PHASE1_E4_EXTENDED_FUNCTION_COMMAND_EXTENDED_VCH:
            case PHASE1_E5_EXTENDED_FUNCTION_COMMAND_EXTENDED_LCCH:
                processExtendedFunctionCommand(message, mac);
                break;
            case PHASE1_67_DENY_RESPONSE:
                processDeny(message, mac);
                break;
            case PHASE1_68_GROUP_AFFILIATION_RESPONSE_ABBREVIATED:
            case PHASE1_6A_GROUP_AFFILIATION_QUERY_ABBREVIATED:
            case PHASE1_E8_GROUP_AFFILIATION_RESPONSE_EXTENDED:
            case PHASE1_EA_GROUP_AFFILIATION_QUERY_EXTENDED:
                processAffiliation(message, mac);
                break;
            case PHASE1_6B_LOCATION_REGISTRATION_RESPONSE:
                processLocationRegistration(message, mac);
                break;
            case PHASE1_6C_UNIT_REGISTRATION_RESPONSE_ABBREVIATED:
            case PHASE1_6D_UNIT_REGISTRATION_COMMAND_ABBREVIATED:
            case PHASE1_6F_DEREGISTRATION_ACKNOWLEDGE:
            case PHASE1_EC_UNIT_REGISTRATION_RESPONSE_EXTENDED:
                processUnitRegistration(message, mac);
                break;
            case PHASE1_70_SYNCHRONIZATION_BROADCAST:
                //Ignore - channel timing information
                break;
            case PHASE1_71_AUTHENTICATION_DEMAND:
            case PHASE1_72_AUTHENTICATION_FNE_RESPONSE_ABBREVIATED:
            case PHASE1_F2_AUTHENTICATION_FNE_RESPONSE_EXTENDED:
                processAuthentication(message, mac);
                break;
            case PHASE1_76_ROAMING_ADDRESS_COMMAND:
            case PHASE1_77_ROAMING_ADDRESS_UPDATE:
                processRoamingAddress(message, mac);
                break;
            case PHASE1_73_IDENTIFIER_UPDATE_TDMA_ABBREVIATED:
            case PHASE1_74_IDENTIFIER_UPDATE_V_UHF:
            case PHASE1_78_SYSTEM_SERVICE_BROADCAST:
            case PHASE1_79_SECONDARY_CONTROL_CHANNEL_BROADCAST_IMPLICIT:
            case PHASE1_7A_RFSS_STATUS_BROADCAST_IMPLICIT:
            case PHASE1_7B_NETWORK_STATUS_BROADCAST_IMPLICIT:
            case PHASE1_7C_ADJACENT_STATUS_BROADCAST_IMPLICIT:
            case PHASE1_7D_IDENTIFIER_UPDATE:
            case PHASE1_D6_SNDCP_DATA_CHANNEL_ANNOUNCEMENT:
            case PHASE1_E9_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
            case PHASE1_F3_IDENTIFIER_UPDATE_TDMA_EXTENDED:
            case PHASE1_FA_RFSS_STATUS_BROADCAST_EXPLICIT:
            case PHASE1_FB_NETWORK_STATUS_BROADCAST_EXPLICIT:
            case PHASE1_FC_ADJACENT_STATUS_BROADCAST_EXPLICIT:
            case PHASE1_FE_ADJACENT_STATUS_BROADCAST_EXTENDED_EXPLICIT:
                processNetwork(message, mac);
                break;
            case PHASE1_75_TIME_AND_DATE_ANNOUNCEMENT:
                //Ignore
                break;

            /**
             * Partition 3 Opcodes
             */
            case PHASE1_EXTENDED_PARTITION_3_UNKNOWN_OPCODE:
                //Ignore
                break;
            case PHASE1_90_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                if(mac instanceof GroupRegroupVoiceChannelUserAbbreviated gr)
                {
                    if(gr.hasPatchgroup())
                    {
                        mPatchGroupManager.addPatchGroup(gr.getPatchgroup(), message.getTimestamp());
                    }

                    if(gr.hasPatchgroup() || gr.hasRadio())
                    {
                        processChannelUser(message, mac);
                    }
                }
                break;

            /**
             * Partition 2 Opcodes
             */
            case VENDOR_PARTITION_2_UNKNOWN_OPCODE:
                //Ignore
                break;

            //Vendor: L3Harris
            case L3HARRIS_A0_PRIVATE_DATA_CHANNEL_GRANT:
            case L3HARRIS_AC_UNIT_TO_UNIT_DATA_CHANNEL_GRANT:
                processChannelGrant(message, mac);
                break;
            case L3HARRIS_A8_TALKER_ALIAS:
                processTalkerAlias(message, mac);
                break;
            case L3HARRIS_AA_GPS_LOCATION:
                processGPS(message, mac);
                break;
            case L3HARRIS_B0_GROUP_REGROUP_EXPLICIT_ENCRYPTION_COMMAND:
                processDynamicRegrouping(message, mac);
                break;

            //Vendor: Motorola
            case MOTOROLA_80_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                if(mac instanceof MotorolaGroupRegroupVoiceChannelUserAbbreviated gr)
                {
                    mPatchGroupManager.addPatchGroup(gr.getPatchGroup(), message.getTimestamp());
                }
                processChannelUser(message, mac);
                break;
            case MOTOROLA_81_GROUP_REGROUP_ADD:
                processDynamicRegrouping(message, mac);
                break;
            case MOTOROLA_83_GROUP_REGROUP_VOICE_CHANNEL_UPDATE:
                if(mac instanceof MotorolaGroupRegroupVoiceChannelUpdate gr)
                {
                    mPatchGroupManager.addPatchGroup(gr.getPatchgroup(), message.getTimestamp());
                }
                processChannelGrantUpdate(message, mac);
                break;
            case MOTOROLA_84_GROUP_REGROUP_EXTENDED_FUNCTION_COMMAND:
                processExtendedFunctionCommand(message, mac);
                break;
            case MOTOROLA_89_GROUP_REGROUP_DELETE:
                processDynamicRegrouping(message, mac);
                break;
            case MOTOROLA_8B_TDMA_DATA_CHANNEL:
                break;
            case MOTOROLA_91_TALKER_ALIAS_HEADER:
                //Unknown
                break;
            case MOTOROLA_95_TALKER_ALIAS_DATA_BLOCK:
                //Unknown
                break;
            case MOTOROLA_A0_GROUP_REGROUP_VOICE_CHANNEL_USER_EXTENDED:
                if(mac instanceof MotorolaGroupRegroupVoiceChannelUserExtended gr)
                {
                    mPatchGroupManager.addPatchGroup(gr.getPatchgroup(), message.getTimestamp());
                }
                processChannelUser(message, mac);
                break;
            case MOTOROLA_A3_GROUP_REGROUP_CHANNEL_GRANT_IMPLICIT:
                if(mac instanceof MotorolaGroupRegroupChannelGrantImplicit gr)
                {
                    mPatchGroupManager.addPatchGroup(gr.getTargetAddress(), message.getTimestamp());
                }
                processChannelGrant(message, mac);
                break;
            case MOTOROLA_A4_GROUP_REGROUP_CHANNEL_GRANT_EXPLICIT:
                if(mac instanceof MotorolaGroupRegroupChannelGrantExplicit gr)
                {
                    mPatchGroupManager.addPatchGroup(gr.getTargetAddress(), message.getTimestamp());
                }
                processChannelGrant(message, mac);
                break;
            case MOTOROLA_A5_GROUP_REGROUP_CHANNEL_GRANT_UPDATE:
                if(mac instanceof MotorolaGroupRegroupChannelGrantUpdate gr)
                {
                    mPatchGroupManager.addPatchGroup(gr.getPatchgroupA(), message.getTimestamp());

                    if(gr.hasPatchgroupB())
                    {
                        mPatchGroupManager.addPatchGroup(gr.getPatchgroupB(), message.getTimestamp());
                    }
                }
                processChannelGrantUpdate(message, mac);
                break;
            case MOTOROLA_A6_QUEUED_RESPONSE:
                processQueued(message, mac);
                break;
            case MOTOROLA_A7_DENY_RESPONSE:
                processDeny(message, mac);
                break;
            case MOTOROLA_A8_ACKNOWLEDGE_RESPONSE:
                processAcknowledge(message, mac);
                break;
            case L3HARRIS_81_UNKNOWN_OPCODE_129:
                //Unknown
                break;
            case PHASE1_88_UNKNOWN_LCCH_OPCODE:
                //Unknown
                break;
            case L3HARRIS_8F_UNKNOWN_OPCODE_143:
                //Unknown
                break;
            case UNKNOWN:
            default:
                break;
        }
    }

    /**
     * Creates a copy of the current identifier collection with all USER and CHANNEL identifiers removed and adds the
     * user identifier argument added to the collection.
     * @param userIdentifierToAdd to add to the collection
     * @param timestamp to check for freshness of patch groups.
     */
    private MutableIdentifierCollection getIdentifierCollectionForUser(Identifier userIdentifierToAdd, long timestamp)
    {
        return getIdentifierCollectionForUsers(Collections.singletonList(userIdentifierToAdd), timestamp);
    }

    /**
     * Creates a copy of the current identifier collection with all USER and CHANNEL identifiers removed and adds the
     * user identifiers argument added to the collection.
     * @param identifiersToAdd to add to the collection
     * @param timestamp to check for freshness of patch groups.
     */
    private MutableIdentifierCollection getIdentifierCollectionForUsers(List<Identifier> identifiersToAdd, long timestamp)
    {
        MutableIdentifierCollection mic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        mic.remove(IdentifierClass.USER);
        mic.remove(Form.CHANNEL);
        for(Identifier identifier : identifiersToAdd)
        {
            //Filter the identifiers through the patch group manager
            mic.update(mPatchGroupManager.update(identifier, timestamp));
        }
        mTrafficChannelManager.getTalkerAliasManager().enrichMutable(mic);
        return mic;
    }

    /**
     * Acknowledgements.
     */
    private void processAcknowledge(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_60_ACKNOWLEDGE_RESPONSE_FNE_ABBREVIATED:
                if(mac instanceof AcknowledgeResponseFNEAbbreviated ar)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.ACKNOWLEDGE, "ACKNOWLEDGE: " + ar.getServiceType());
                }
                break;
            case PHASE1_E0_ACKNOWLEDGE_RESPONSE_FNE_EXTENDED:
                if(mac instanceof AcknowledgeResponseFNEExtended ar)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.ACKNOWLEDGE, "ACKNOWLEDGE: " + ar.getServiceType());
                }
                break;
            case MOTOROLA_A8_ACKNOWLEDGE_RESPONSE:
                if(mac instanceof MotorolaAcknowledgeResponse ar)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.ACKNOWLEDGE, "ACKNOWLEDGE: " + ar.getServiceType());
                }
                break;

        }
    }

    /**
     * Affiliation request and response (for talkgroups).
     */
    private void processAffiliation(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_68_GROUP_AFFILIATION_RESPONSE_ABBREVIATED:
            case PHASE1_E8_GROUP_AFFILIATION_RESPONSE_EXTENDED:
                if(mac instanceof GroupAffiliationResponseAbbreviated || mac instanceof GroupAffiliationResponseExtended)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.RESPONSE, "GROUP AFFILIATION");
                }
                break;
            case PHASE1_6A_GROUP_AFFILIATION_QUERY_ABBREVIATED:
            case PHASE1_EA_GROUP_AFFILIATION_QUERY_EXTENDED:
                if(mac instanceof GroupAffiliationQueryAbbreviated || mac instanceof GroupAffiliationQueryExtended)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.QUERY, "GROUP AFFILIATION");
                }
                break;
        }
    }

    /**
     * Answer Request & Response
     */
    private void processAnswer(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_45_UNIT_TO_UNIT_ANSWER_REQUEST_ABBREVIATED:
                if(mac instanceof UnitToUnitAnswerRequestAbbreviated uuara)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST,
                            "UNIT-TO-UNIT ANSWER REQUEST - " + uuara.getServiceOptions());
                }
                break;
            case PHASE1_4A_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
                if(mac instanceof TelephoneInterconnectAnswerRequest tiar)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST,
                            "TELEPHONE INTERCONNECT ANSWER REQUEST");
                }
                break;
            case PHASE1_C5_UNIT_TO_UNIT_ANSWER_REQUEST_EXTENDED:
                if(mac instanceof UnitToUnitAnswerRequestExtended uuare)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST, "UNIT-TO-UNIT ANSWER REQUEST "
                            + uuare.getServiceOptions());
                }
                break;
        }
    }

    /**
     * Authentication
     */
    private void processAuthentication(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_71_AUTHENTICATION_DEMAND:
                if(mac instanceof AuthenticationDemand ad)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.COMMAND,
                            "AUTHENTICATE - SEED:" + ad.getRandomSeed() + " CHALLENGE:" + ad.getChallenge());
                }
                break;
            case PHASE1_72_AUTHENTICATION_FNE_RESPONSE_ABBREVIATED:
                if(mac instanceof AuthenticationFNEResponseAbbreviated ar)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.RESPONSE,
                            "AUTHENTICATION " + ar.getResponse());
                }
                break;
            case PHASE1_F2_AUTHENTICATION_FNE_RESPONSE_EXTENDED:
                if(mac instanceof AuthenticationFNEResponseExtended ar)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.RESPONSE,
                            "AUTHENTICATION " + ar.getResponse());
                }
                break;
        }
    }

    /**
     * Call Alert for the target radio to call the source radio
     */
    private void processCallAlert(MacMessage message, MacStructure mac)
    {
        broadcast(message, mac, DecodeEventType.CALL_ALERT, "CALL THE FROM RADIO");
    }

    /**
     * Channel grants
     */
    private void processChannelGrant(MacMessage message, MacStructure mac)
    {
        //TODO: will we ever see a channel grant on a non-control channel?
        if(message.getMacPduType() == MacPduType.MAC_3_IDLE || message.getMacPduType() == MacPduType.MAC_6_HANGTIME)
        {
            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier, message.getTimestamp()));
            }

            mTrafficChannelManager.getTalkerAliasManager().enrichMutable(getIdentifierCollection());

            continueState(State.ACTIVE);
        }

        //All channel grant messages implement this interface
        if(mac instanceof IP25ChannelGrantDetailProvider cgdp)
        {
            updateCurrentChannel(cgdp.getChannel());

            //Only dispatch channel grant if we're a control channel.
            if(mChannel.isStandardChannel())
            {
                MutableIdentifierCollection ic = getIdentifierCollectionForUsers(mac.getIdentifiers(), message.getTimestamp());
                //Add the traffic channel to the IC
                ic.update(cgdp.getChannel());
                mTrafficChannelManager.processP2ChannelGrant(cgdp.getChannel(), cgdp.getServiceOptions(), ic, mac.getOpcode(),
                        message.getTimestamp(), mac.toString());
            }
        }
    }

    /**
     * Channel Grant Update.  Indicates to the current channel users that there is activity on other channels involving
     * talkgroups that one or more of the current call radios may want to join and the message contains enough info
     * for the radio to move directly to the channel to take part in the call.
     */
    private void processChannelGrantUpdate(MacMessage message, MacStructure mac)
    {
        if(message.getMacPduType() == MacPduType.MAC_3_IDLE || message.getMacPduType() == MacPduType.MAC_6_HANGTIME)
        {
            continueState(State.ACTIVE);
        }

        switch(mac.getOpcode())
        {
            case TDMA_05_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_IMPLICIT:
                if(mac instanceof GroupVoiceChannelGrantUpdateMultipleImplicit cgu)
                {
                    updateCurrentChannel(cgu.getChannel1());

                    if(mChannel.isStandardChannel())
                    {
                        MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getGroupAddress1(), message.getTimestamp());
                        mic.update(cgu.getChannel1());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions1(), mic,
                                mac.getOpcode(), message.getTimestamp(), mac.toString());
                    }

                    if(cgu.hasGroup2())
                    {
                        updateCurrentChannel(cgu.getChannel2());

                        if(mChannel.isStandardChannel())
                        {
                            MutableIdentifierCollection mic2 = getIdentifierCollectionForUser(cgu.getGroupAddress2(), message.getTimestamp());
                            mic2.update(cgu.getChannel1());
                            mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions2(), mic2,
                                    mac.getOpcode(), message.getTimestamp(), mac.toString());
                        }
                    }

                    if(cgu.hasGroup3())
                    {
                        updateCurrentChannel(cgu.getChannel3());

                        if(mChannel.isStandardChannel())
                        {
                            MutableIdentifierCollection mic3 = getIdentifierCollectionForUser(cgu.getGroupAddress3(), message.getTimestamp());
                            mic3.update(cgu.getChannel1());
                            mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions3(), mic3,
                                    mac.getOpcode(), message.getTimestamp(), mac.toString());
                        }
                    }
                }
                break;
            case TDMA_25_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_EXPLICIT:
                if(mac instanceof GroupVoiceChannelGrantUpdateMultipleExplicit cgu)
                {
                    updateCurrentChannel(cgu.getChannel1());

                    if(mChannel.isStandardChannel())
                    {
                        MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getGroupAddress1(), message.getTimestamp());
                        mic.update(cgu.getChannel1());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions1(), mic,
                                mac.getOpcode(), message.getTimestamp(), mac.toString());
                    }

                    if(cgu.hasGroup2())
                    {
                        updateCurrentChannel(cgu.getChannel2());

                        if(mChannel.isStandardChannel())
                        {
                            MutableIdentifierCollection mic2 = getIdentifierCollectionForUser(cgu.getGroupAddress2(), message.getTimestamp());
                            mic2.update(cgu.getChannel1());
                            mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions2(), mic2,
                                    mac.getOpcode(), message.getTimestamp(), mac.toString());
                        }
                    }
                }
                break;
            case PHASE1_42_GROUP_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
                if(mac instanceof GroupVoiceChannelGrantUpdateImplicit cgu)
                {
                    updateCurrentChannel(cgu.getChannel1());

                    //Create an empty service options
                    VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);

                    if(mChannel.isStandardChannel())
                    {
                        MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getGroupAddress1(), message.getTimestamp());
                        mic.update(cgu.getChannel1());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel1(), serviceOptions, mic, mac.getOpcode(),
                                message.getTimestamp(), mac.toString());
                    }

                    if(cgu.hasGroup2())
                    {
                        updateCurrentChannel(cgu.getChannel2());

                        if(mChannel.isStandardChannel())
                        {
                            MutableIdentifierCollection mic2 = getIdentifierCollectionForUser(cgu.getGroupAddress2(), message.getTimestamp());
                            mic2.update(cgu.getChannel1());
                            mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel1(), serviceOptions, mic2,
                                    mac.getOpcode(), message.getTimestamp(), mac.toString());
                        }
                    }
                }
                break;
            case PHASE1_46_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED:
                if(mac instanceof UnitToUnitVoiceChannelGrantUpdateAbbreviated cgu)
                {
                    updateCurrentChannel(cgu.getChannel());

                    if(mChannel.isStandardChannel())
                    {
                        //Create an empty service options
                        VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);
                        MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers(), message.getTimestamp());
                        mic.update(cgu.getChannel());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel(), serviceOptions, mic, mac.getOpcode(),
                                message.getTimestamp(), mac.toString());
                    }
                }
                break;
            case PHASE1_49_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
                if(mac instanceof TelephoneInterconnectVoiceChannelGrantUpdateImplicit cgu)
                {
                    updateCurrentChannel(cgu.getChannel());

                    if(mChannel.isStandardChannel())
                    {
                        //Create an empty service options
                        VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);
                        MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers(), message.getTimestamp());
                        mic.update(cgu.getChannel());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel(), serviceOptions, mic, mac.getOpcode(),
                                message.getTimestamp(), mac.toString());
                    }
                }
                break;
            case PHASE1_C3_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                if(mac instanceof GroupVoiceChannelGrantUpdateExplicit cgu)
                {
                    updateCurrentChannel(cgu.getChannel());

                    if(mChannel.isStandardChannel())
                    {
                        MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getGroupAddress(), message.getTimestamp());
                        mic.update(cgu.getChannel());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel(), cgu.getServiceOptions(), mic,
                                mac.getOpcode(), message.getTimestamp(), mac.toString());
                    }
                }
                break;
            case PHASE1_C6_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_VCH:
                if(mac instanceof UnitToUnitVoiceChannelGrantUpdateExtendedVCH cgu)
                {
                    updateCurrentChannel(cgu.getChannel());

                    if(mChannel.isStandardChannel())
                    {
                        //Create an empty service options
                        VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);
                        MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers(), message.getTimestamp());
                        mic.update(cgu.getChannel());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel(), serviceOptions, mic, mac.getOpcode(),
                                message.getTimestamp(), mac.toString());
                    }
                }
                break;
            case PHASE1_C7_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_LCCH:
                if(mac instanceof UnitToUnitVoiceChannelGrantUpdateExtendedLCCH cgu)
                {
                    updateCurrentChannel(cgu.getChannel());

                    if(mChannel.isStandardChannel())
                    {
                        MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers(), message.getTimestamp());
                        mic.update(cgu.getChannel());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel(), cgu.getServiceOptions(), mic,
                                mac.getOpcode(), message.getTimestamp(), mac.toString());
                    }
                }
                break;
            case PHASE1_C9_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                if(mac instanceof TelephoneInterconnectVoiceChannelGrantUpdateExplicit cgu)
                {
                    updateCurrentChannel(cgu.getChannel());

                    if(mChannel.isStandardChannel())
                    {
                        MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers(), message.getTimestamp());
                        mic.update(cgu.getChannel());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel(), cgu.getServiceOptions(), mic,
                                mac.getOpcode(), message.getTimestamp(), mac.toString());
                    }
                }
                break;
            case MOTOROLA_83_GROUP_REGROUP_VOICE_CHANNEL_UPDATE:
                if(mac instanceof MotorolaGroupRegroupVoiceChannelUpdate cgu)
                {
                    mPatchGroupManager.addPatchGroup(cgu.getPatchgroup(), message.getTimestamp());
                    updateCurrentChannel(cgu.getChannel());

                    if(mChannel.isStandardChannel())
                    {
                        MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getPatchgroup(), message.getTimestamp());
                        mic.update(cgu.getChannel());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannel(), cgu.getServiceOptions(), mic,
                                mac.getOpcode(), message.getTimestamp(), mac.toString());
                    }
                }
                break;
            case MOTOROLA_A5_GROUP_REGROUP_CHANNEL_GRANT_UPDATE:
                if(mac instanceof MotorolaGroupRegroupChannelGrantUpdate cgu)
                {
                    mPatchGroupManager.addPatchGroup(cgu.getPatchgroupA(), message.getTimestamp());
                    updateCurrentChannel(cgu.getChannelA());

                    //Create an empty service options
                    VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);

                    if(mChannel.isStandardChannel())
                    {
                        MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getPatchgroupA(), message.getTimestamp());
                        mic.update(cgu.getChannelA());
                        mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannelA(), serviceOptions, mic, mac.getOpcode(),
                                message.getTimestamp(), mac.toString());
                    }

                    if(cgu.hasPatchgroupB())
                    {
                        mPatchGroupManager.addPatchGroup(cgu.getPatchgroupB(), message.getTimestamp());
                        updateCurrentChannel(cgu.getChannelB());

                        if(mChannel.isStandardChannel())
                        {
                            MutableIdentifierCollection mic2 = getIdentifierCollectionForUser(cgu.getPatchgroupB(), message.getTimestamp());
                            mic2.update(cgu.getChannelB());
                            mTrafficChannelManager.processP2ChannelUpdate(cgu.getChannelB(), serviceOptions, mic2,
                                    mac.getOpcode(), message.getTimestamp(), mac.toString());
                        }
                    }
                }
            default:
                //Log unhandled opcodes at least once to detect and add support.
                LOGGING_SUPPRESSOR.error(mac.getOpcode().name(), 1,
                        "Unrecognized Channel Grant Update Opcode: " + mac.getOpcode().name());
                break;
        }
    }

    /**
     * Updates the current channel from channel grant and update messaging.
     * @param channelDescriptor to compare to the current frequency.
     */
    private void updateCurrentChannel(IChannelDescriptor channelDescriptor)
    {
        if(getCurrentChannel() == null &&
                getCurrentFrequency() > 0 &&
                channelDescriptor.getDownlinkFrequency() == getCurrentFrequency())
        {
            if(channelDescriptor instanceof APCO25Channel p25)
            {
                if(p25.getTimeslot() == getTimeslot())
                {
                    setCurrentChannel(channelDescriptor);
                }
                else if(getTimeslot() == P25P1Message.TIMESLOT_1)
                {
                    APCO25Channel timeslot1 = APCO25Channel.create(p25.getValue().getDownlinkBandIdentifier(),
                            p25.getValue().getDownlinkChannelNumber() - 1);
                    timeslot1.setFrequencyBand(p25.getValue().getFrequencyBand());
                    setCurrentChannel(timeslot1);
                }
                else
                {
                    APCO25Channel timeslot2 = APCO25Channel.create(p25.getValue().getDownlinkBandIdentifier(),
                            p25.getValue().getDownlinkChannelNumber() + 1);
                    timeslot2.setFrequencyBand(p25.getValue().getFrequencyBand());
                    setCurrentChannel(timeslot2);
                }
            }
        }
    }

    @Override
    public void setCurrentChannel(IChannelDescriptor channel)
    {
        if(channel == null)
        {
            return;
        }

        if(channel instanceof APCO25Channel apco25Channel && apco25Channel.getTimeslot() != getTimeslot())
        {
            channel = apco25Channel.decorateAs(getTimeslot());
        }

        super.setCurrentChannel(channel);
    }

    /**
     * Channel user (ie current user on this channel).
     *
     * Note: calling code should ensure that the mac structure argument implements the IServiceOptionsProvider interface.
     */
    private void processChannelUser(MacMessage message, MacStructure mac)
    {
        for(Identifier identifier : mac.getIdentifiers())
        {
            //Add to the identifier collection after filtering through the patch group manager
            getIdentifierCollection().update(mPatchGroupManager.update(identifier, message.getTimestamp()));
        }

        mTrafficChannelManager.getTalkerAliasManager().enrichMutable(getIdentifierCollection());

        if(message.getMacPduType() == MacPduType.MAC_3_IDLE || message.getMacPduType() == MacPduType.MAC_6_HANGTIME)
        {
            continueState(State.ACTIVE);
        }
        else
        {
            if(mac instanceof IServiceOptionsProvider sop)
            {
                IChannelDescriptor currentChannel = mTrafficChannelManager.processP2TrafficCurrentUser(getCurrentFrequency(),
                        getTimeslot(), getCurrentChannel(), sop.getServiceOptions(), mac.getOpcode(),
                        getIdentifierCollection().copyOf(), message.getTimestamp(), null, message.toString());

                if(getCurrentChannel() == null)
                {
                    setCurrentChannel(currentChannel);
                }

                if(sop.getServiceOptions().isEncrypted())
                {
                    continueState(State.ENCRYPTED);
                }
                else
                {
                    continueState(State.CALL);
                }
            }
            else
            {
                LOGGING_SUPPRESSOR.error("Unrecognized Service Options Not Implemented:" + mac.getClass().getName(),
                        1, "Unrecognized voice channel user MAC message.  Please notify the developer " +
                                "that this class should implement the IServiceOptionsProvider interface.  Class: " +
                                mac.getClass());
                continueState(State.ACTIVE);
            }
        }
    }

    /**
     * Push-To-Talk (ie start of audio on this channel)
     */
    private void processPushToTalk(MacMessage message, MacStructure mac)
    {
        for(Identifier identifier : mac.getIdentifiers())
        {
            //Add to the identifier collection after filtering through the patch group manager
            getIdentifierCollection().update(mPatchGroupManager.update(identifier, message.getTimestamp()));
        }

        mTrafficChannelManager.getTalkerAliasManager().enrichMutable(getIdentifierCollection());

        if(mac instanceof PushToTalk ptt)
        {
            VoiceServiceOptions vso = ptt.isEncrypted() ? VoiceServiceOptions.createEncrypted() : VoiceServiceOptions.createUnencrypted();

            //First TCM call creates the tracked event and second call starts the call and updates the duration
            mTrafficChannelManager.processP2TrafficCurrentUser(getCurrentFrequency(), getTimeslot(), getCurrentChannel(), vso,
                    mac.getOpcode(), getIdentifierCollection().copyOf(), message.getTimestamp(),
                    ptt.isEncrypted() ? ptt.getEncryptionKey().toString() : null, message.toString());

            mTrafficChannelManager.processP2TrafficVoice(getCurrentFrequency(), getTimeslot(), message.getTimestamp());

            broadcast(new DecoderStateEvent(this, Event.START, ptt.isEncrypted() ? State.ENCRYPTED : State.CALL, getTimeslot()));
        }
    }

    /**
     * Dynamic Regrouping
     */
    private void processDynamicRegrouping(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case L3HARRIS_B0_GROUP_REGROUP_EXPLICIT_ENCRYPTION_COMMAND:
                if(mac instanceof L3HarrisGroupRegroupExplicitEncryptionCommand regroup)
                {
                    if(regroup.getRegroupOptions().isActivate())
                    {
                        if(mPatchGroupManager.addPatchGroup(regroup.getPatchGroup(), message.getTimestamp()))
                        {
                            broadcast(message, mac, DecodeEventType.DYNAMIC_REGROUP, "ACTIVATE " + regroup.getPatchGroup());
                        }
                    }
                    else
                    {
                        if(mPatchGroupManager.removePatchGroup(regroup.getPatchGroup()))
                        {
                            broadcast(message, mac, DecodeEventType.DYNAMIC_REGROUP, "DEACTIVATE " + regroup.getPatchGroup());
                        }
                    }
                }
                break;
            case MOTOROLA_81_GROUP_REGROUP_ADD:
                if(mac instanceof MotorolaGroupRegroupAddCommand mgrac)
                {
                    if(mPatchGroupManager.addPatchGroup(mgrac.getPatchGroup(), message.getTimestamp()))
                    {
                        broadcast(message, mac, DecodeEventType.DYNAMIC_REGROUP, "ACTIVATE " + mgrac.getPatchGroup());
                    }
                }
                break;
            case MOTOROLA_89_GROUP_REGROUP_DELETE:
                if(mac instanceof MotorolaGroupRegroupDeleteCommand mgrdc)
                {
                    if(mPatchGroupManager.removePatchGroup(mgrdc.getPatchGroup()))
                    {
                        broadcast(message, mac, DecodeEventType.DYNAMIC_REGROUP, "DEACTIVATE " + mgrdc.getPatchGroup());
                    }
                }
                break;
        }
    }

    /**
     * End Push-To-Talk (ie end of current audio segment on this channel).
     */
    private void processEndPushToTalk(MacMessage message, MacStructure mac)
    {
        if(mac instanceof EndPushToTalk)
        {
            //No matter what, remove the FROM identifier on end PTT.
            getIdentifierCollection().remove(Role.FROM);

            //Only reset the identifiers if the call event is closed out, otherwise we might have a timing issue
            //between the control channel and the traffic channel.
            if(mTrafficChannelManager.processP2TrafficEndPushToTalk(getCurrentFrequency(), getTimeslot(),
                    message.getTimestamp(), "END PUSH TO TALK - " + message))
            {
                getIdentifierCollection().remove(IdentifierClass.USER);
            }

            if(message.getDataUnitID().isFACCH())
            {
                mEndPttOnFacchCounter++;

                //FNE sending 2 or more End PTT in FACCH timeslots indicates a traffic channel teardown event.
                if(mEndPttOnFacchCounter > 1 && mChannel.isTrafficChannel())
                {
                    broadcast(new DecoderStateEvent(this, Event.END, State.TEARDOWN, getTimeslot()));
                    return; //don't issue a reset state after this.
                }
            }
        }
    }

    /**
     * Data Page Request.  Indicates that trunked data service has been requested for the target radio.
     */
    private void processDataPageRequest(MacMessage message, MacStructure mac)
    {
        if(mac instanceof SNDCPDataPageRequest sdpr)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.PAGE, "SNDCP DATA PAGE " + sdpr.getServiceOptions());
        }
    }


    /**
     * Deny Response
     */
    private void processDeny(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_67_DENY_RESPONSE:
                if(mac instanceof DenyResponse dr)
                {
                    broadcast(message, mac, DecodeEventType.RESPONSE, "DENY: " + dr.getDeniedServiceType() + " REASON:" +
                            dr.getDenyReason() + " ADDL:" + dr.getAdditionalInfo());
                }
                break;
            case MOTOROLA_A7_DENY_RESPONSE:
                if(mac instanceof MotorolaDenyResponse dr)
                {
                    broadcast(message, mac, DecodeEventType.RESPONSE, "DENY: " + dr.getDeniedServiceType() + " REASON:" +
                            dr.getDenyReason() + (dr.hasAdditionalInformation() ? " ADDL:" + dr.getAdditionalInfo() : ""));
                }
                break;
        }
    }

    /**
     * Processes the Extended Function Command
     */
    private void processExtendedFunctionCommand(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_64_EXTENDED_FUNCTION_COMMAND_ABBREVIATED:
                if(mac instanceof ExtendedFunctionCommandAbbreviated efc)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, "EXTENDED FUNCTION: " +
                            efc.getExtendedFunction() + " ARGUMENTS:" + efc.getArguments());
                }
                break;
            case PHASE1_E4_EXTENDED_FUNCTION_COMMAND_EXTENDED_VCH:
                if(mac instanceof ExtendedFunctionCommandExtendedVCH efce)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, "EXTENDED FUNCTION: " +
                            efce.getExtendedFunction() + " ARGUMENTS:" + efce.getArguments());
                }
                break;
            case PHASE1_E5_EXTENDED_FUNCTION_COMMAND_EXTENDED_LCCH:
                if(mac instanceof ExtendedFunctionCommandExtendedLCCH efce)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, "EXTENDED FUNCTION: " +
                            efce.getExtendedFunction() + " ARGUMENTS:" + efce.getArguments());
                }
                break;
            case MOTOROLA_84_GROUP_REGROUP_EXTENDED_FUNCTION_COMMAND:
                if(mac instanceof MotorolaGroupRegroupExtendedFunctionCommand efc)
                {
                    switch(efc.getExtendedFunction())
                    {
                        case GROUP_REGROUP_CANCEL_SUPERGROUP:
                            broadcast(message, mac, DecodeEventType.COMMAND, "CANCEL SUPER GROUP FOR RADIO");
                            break;
                        case GROUP_REGROUP_CREATE_SUPERGROUP:
                            broadcast(message, mac, DecodeEventType.COMMAND, "CREATE SUPER GROUP ADD RADIO" +
                                    efc.getTargetAddress());
                    }
                }
                break;

        }
    }

    /**
     * Identifier Update
     */
    private void processNetwork(MacMessage message, MacStructure mac)
    {
        if(message.getMacPduType() == MacPduType.MAC_3_IDLE || message.getMacPduType() == MacPduType.MAC_6_HANGTIME)
        {
            continueState(State.ACTIVE);
        }

        mNetworkConfigurationMonitor.processMacMessage(message);

        if(mac instanceof NetworkStatusBroadcastImplicit nsbi)
        {
            setCurrentChannel(nsbi.getChannel());
        }
        else if(mac instanceof RfssStatusBroadcastImplicit rsbi)
        {
            setCurrentChannel(rsbi.getChannel());
        }

        //Send the frequency bands to the traffic channel manager to use for traffic channel preload data
        if(mac instanceof IFrequencyBand frequencyBand)
        {
            mTrafficChannelManager.processFrequencyBand(frequencyBand);
        }
    }

    /**
     * Roaming Address
     */
    private void processRoamingAddress(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_76_ROAMING_ADDRESS_COMMAND:
                if(mac instanceof RoamingAddressCommand rac)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, rac.getStackOperation() + " ROAMING ADDRESS STACK");
                }
                break;
            case PHASE1_77_ROAMING_ADDRESS_UPDATE:
                if(mac instanceof RoamingAddressUpdate rau)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, "ROAMING ADDRESS UPDATE");
                }
                break;
        }
    }

    /**
     * Queued Response
     */
    private void processQueued(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_61_QUEUED_RESPONSE:
                if(mac instanceof QueuedResponse qr)
                {
                    broadcast(message, mac, DecodeEventType.RESPONSE, "QUEUED - " + qr.getQueuedResponseServiceType() + " REASON:" + qr.getQueuedResponseReason() + " ADDL:" + qr.getAdditionalInfo());
                }
                break;
            case MOTOROLA_A6_QUEUED_RESPONSE:
                if(mac instanceof MotorolaQueuedResponse qr)
                {
                    broadcast(message, mac, DecodeEventType.RESPONSE, "QUEUED - " + qr.getQueuedResponseServiceType() + " REASON:" + qr.getQueuedResponseReason() + " ADDL:" + qr.getAdditionalInfo());
                }
                break;
        }
    }

    /**
     * Null Information and Null Avoid Zero Bias
     */
    private void processNullInformation(MacMessage message, MacStructure mac)
    {
        /**
         * Notionally, we should close out any current call event here, but that causes timing problems because if the
         * control channel creates a call event that is going to be happening on this channel shortly, and we are still
         * seeing null info mac messages here, this traffic channel will close out the event that the control channel
         * created and then recreate a new event once the actual call starts.  So, don't close out the current call
         * based solely on the null info in the traffic channel.  Ultimately, the existing call event will either be
         * updated by a subsequent call, or removed via the traffic channel teardown.
         */
        if(message.getMacPduType() != MacPduType.MAC_4_ACTIVE) //Don't change the state when we're in a call
        {
            continueState(State.ACTIVE);
        }
    }

    /**
     * Unit monitor - places the radio into a traffic channel and forces it to broadcast for safety situations.
     */
    private void processRadioUnitMonitor(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_4C_RADIO_UNIT_MONITOR_COMMAND_ABBREVIATED:
                if(mac instanceof RadioUnitMonitorCommandAbbreviated rum)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, "RADIO UNIT MONITOR" + (rum.isSilentMonitor() ? " (SILENT)" : "") + " TIME:" + rum.getTransmitTime() + " MULTIPLIER:" + rum.getTransmitMultiplier());
                }
                break;
            case PHASE1_5D_RADIO_UNIT_MONITOR_COMMAND_OBSOLETE:
                //Ignore
                break;
            case PHASE1_5E_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_ABBREVIATED:
                if(mac instanceof RadioUnitMonitorEnhancedCommandAbbreviated rum)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, "RADIO UNIT MONITOR" + (rum.isSilentMode() ? " (SILENT)" : "") + " TIME:" + rum.getTransmitTime());
                }
                break;
            case PHASE1_CC_RADIO_UNIT_MONITOR_COMMAND_EXTENDED_VCH:
                if(mac instanceof RadioUnitMonitorCommandExtendedVCH rum)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, "RADIO UNIT MONITOR" + (rum.isSilentMonitor() ? " (STEALTH)" : "") + " TIME:" + rum.getTransmitTime() + "MULTIPLIER:" + rum.getTransmitMultiplier());
                }
                break;
            case PHASE1_CD_RADIO_UNIT_MONITOR_COMMAND_EXTENDED_LCCH:
                if(mac instanceof RadioUnitMonitorCommandExtendedLCCH rum)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, "RADIO UNIT MONITOR" + (rum.isSilentMonitor() ? " (STEALTH)" : "") + " TIME:" + rum.getTransmitTime() + "MULTIPLIER:" + rum.getTransmitMultiplier());
                }
                break;
            case PHASE1_DE_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_EXTENDED:
                if(mac instanceof RadioUnitMonitorEnhancedCommandExtended rum)
                {
                    broadcast(message, mac, DecodeEventType.COMMAND, "RADIO UNIT MONITOR" + (rum.isSilentMode() ? " (STEALTH)" : "") + " TIME:" + rum.getTransmitTime());
                }
                break;
        }
    }

    /**
     * Processes GPS messages
     */
    private void processGPS(MacMessage message, MacStructure structure)
    {
        if(structure instanceof L3HarrisTalkerGpsLocation gps)
        {
            MutableIdentifierCollection collection = getUpdatedMutableIdentifierCollection(gps);

            //Since the L3Harris GPS doesn't have the source radio re-add it here
            Identifier fromRadio = getIdentifierCollection().getFromIdentifier();
            if(fromRadio != null)
            {
                collection.update(fromRadio);
            }

            DecodeEvent decodeEvent = PlottableDecodeEvent.plottableBuilder(DecodeEventType.GPS, message.getTimestamp())
                    .protocol(Protocol.APCO25)
                    .location(gps.getGeoPosition()).channel(getCurrentChannel()).details("LOCATION: " +
                            gps.getLocation().toString())
                    .identifiers(collection)
                    .build();
            broadcast(decodeEvent);
            mTrafficChannelManager.broadcast(decodeEvent);
            mTrafficChannelManager.processP2TrafficCurrentUser(getCurrentFrequency(), getTimeslot(), gps.getLocation(),
                    message.getTimestamp());
        }
    }

    /**
     * Location registration
     */
    private void processLocationRegistration(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_6B_LOCATION_REGISTRATION_RESPONSE:
                if(mac instanceof LocationRegistrationResponse lrr)
                {
                    broadcast(message, mac, DecodeEventType.RESPONSE, "LOCATION REGISTRATION " + lrr.getResponse());
                }
                break;
        }
    }

    /**
     * MAC release.  If the channel is preempted by the controller, it can indicate that the channel is converting from
     * a traffic channel to a control channel.
     */
    private void processMacRelease(MacMessage message, MacStructure mac)
    {
        if(mac instanceof MacRelease mr)
        {
            mTrafficChannelManager.processP2TrafficCallEnd(getCurrentFrequency(), getTimeslot(), message.getTimestamp(), "MAC RELEASE: " + mac.toString());
            getIdentifierCollection().remove(IdentifierClass.USER);
            broadcast(message, mac, DecodeEventType.COMMAND,
                    (mr.isForcedPreemption() ? "FORCED " : "") + "CALL PREEMPTION" +
                            (mr.isTalkerPreemption() ? " BY USER" : " BY CONTROLLER"));
        }
    }

    /**
     * Message Update is the echo (by controller) of the Short Data Message from the SU.  Both requests and responses
     * are handled by this method.
     */
    private void processMessageUpdate(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_5C_MESSAGE_UPDATE_ABBREVIATED:
                if(mac instanceof MessageUpdateAbbreviated mua)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.SDM, "MESSAGE UPDATE - " + mua.getShortDataMessage());
                }
                break;
            case PHASE1_CE_MESSAGE_UPDATE_EXTENDED_LCCH:
                if(mac instanceof MessageUpdateExtendedLCCH mue)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.SDM, "MESSAGE UPDATE - " + mue.getShortDataMessage());
                }
                break;
            case PHASE1_DC_MESSAGE_UPDATE_EXTENDED_VCH:
                if(mac instanceof MessageUpdateExtendedVCH mue)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.SDM, "MESSAGE UPDATE - " + mue.getShortDataMessage());
                }
                break;

        }
    }

    /**
     * Power Control - controller commands the radio to adjust power and provides received signal quality indication
     * from the radio
     */
    private void processPowerControl(MacMessage message, MacStructure mac)
    {
        if(mac instanceof PowerControlSignalQuality pcsq)
        {
            broadcast(message, mac, DecodeEventType.COMMAND,
                    "ADJUST TRANSMIT POWER - RF:" + pcsq.getRFLevel() + " BER:" + pcsq.getBitErrorRate());
        }
    }

    /**
     * Paging.
     * <p>
     * Individual paging means the controller wants the individual radio(s) to return to the control channel
     * from the current call.
     * <p>
     * Group paging indicates that one or more radios in the current call are part of another talkgroup and that
     * talkgroup is also active on the site, so the radio can optionally return to the control channel and pickup the
     * group channel and proceed to join the talkgroup call.
     */
    private void processPaging(MacMessage message, MacStructure mac)
    {
        if(mac instanceof IndividualPagingWithPriority ip)
        {
            boolean p1 = ip.isTalkgroupPriority1();
            broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                    .channel(getCurrentChannel())
                    .details(p1 ? "PRIORITY " : "" + "USER PAGE-RETURN TO CONTROL CHANNEL")
                    .identifiers(getIdentifierCollectionForUser(ip.getTargetAddress1(), message.getTimestamp()))
                    .timeslot(getTimeslot())
                    .build());

            if(ip.getCount() > 1)
            {
                boolean p2 = ip.isTalkgroupPriority2();
                broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                        .channel(getCurrentChannel())
                        .details(p2 ? "PRIORITY " : "" + "USER PAGE-RETURN TO CONTROL CHANNEL")
                        .identifiers(getIdentifierCollectionForUser(ip.getTargetAddress2(), message.getTimestamp()))
                        .timeslot(getTimeslot())
                        .build());

                if(ip.getCount() > 2)
                {
                    boolean p3 = ip.isTalkgroupPriority3();
                    broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                            .channel(getCurrentChannel())
                            .details(p3 ? "PRIORITY " : "" + "USER PAGE-RETURN TO CONTROL CHANNEL")
                            .identifiers(getIdentifierCollectionForUser(ip.getTargetAddress3(), message.getTimestamp()))
                            .timeslot(getTimeslot())
                            .build());

                    if(ip.getCount() > 3)
                    {
                        boolean p4 = ip.isTalkgroupPriority4();
                        broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                                .channel(getCurrentChannel())
                                .details(p4 ? "PRIORITY " : "" + "USER PAGE-RETURN TO CONTROL CHANNEL")
                                .identifiers(getIdentifierCollectionForUser(ip.getTargetAddress4(), message.getTimestamp()))
                                .timeslot(getTimeslot())
                                .build());
                    }
                }
            }
        }
        else if(mac instanceof IndirectGroupPagingWithoutPriority igp)
        {
            broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                    .channel(getCurrentChannel())
                    .details("GROUP PAGE - TALKGROUP IS ACTIVE ON SITE")
                    .identifiers(getIdentifierCollectionForUser(igp.getTargetGroup1(), message.getTimestamp()))
                    .timeslot(getTimeslot())
                    .build());

            if(igp.getCount() > 1)
            {
                broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                        .channel(getCurrentChannel())
                        .details("GROUP PAGE - TALKGROUP IS ACTIVE ON SITE")
                        .identifiers(getIdentifierCollectionForUser(igp.getTargetGroup2(), message.getTimestamp()))
                        .timeslot(getTimeslot())
                        .build());

                if(igp.getCount() > 2)
                {
                    broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                            .channel(getCurrentChannel())
                            .details("GROUP PAGE - TALKGROUP IS ACTIVE ON SITE")
                            .identifiers(getIdentifierCollectionForUser(igp.getTargetGroup3(), message.getTimestamp()))
                            .timeslot(getTimeslot())
                            .build());

                    if(igp.getCount() > 3)
                    {
                        broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                                .channel(getCurrentChannel())
                                .details("GROUP PAGE - TALKGROUP IS ACTIVE ON SITE")
                                .identifiers(getIdentifierCollectionForUser(igp.getTargetGroup4(), message.getTimestamp()))
                                .timeslot(getTimeslot())
                                .build());
                    }
                }
            }
        }
    }

    /**
     * Status query and update provides radio unit and user status reports.
     */
    private void processStatus(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_58_STATUS_UPDATE_ABBREVIATED:
                if(mac instanceof StatusUpdateAbbreviated sua)
                {
                    broadcast(message, mac, DecodeEventType.STATUS, "STATUS UPDATE - UNIT:" + sua.getUnitStatus() + " USER:" + sua.getUserStatus());
                }
                break;
            case PHASE1_5A_STATUS_QUERY_ABBREVIATED:
                if(mac instanceof StatusQueryAbbreviated)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.QUERY, "STATUS");
                }
                break;
            case PHASE1_D8_STATUS_UPDATE_EXTENDED_VCH:
                if(mac instanceof StatusUpdateExtendedVCH sue)
                {
                    broadcast(message, mac, DecodeEventType.STATUS, "STATUS UPDATE - UNIT:" + sue.getUnitStatus() + " USER:" + sue.getUserStatus());
                }
                break;
            case PHASE1_D9_STATUS_UPDATE_EXTENDED_LCCH:
                if(mac instanceof StatusUpdateExtendedLCCH sue)
                {
                    broadcast(message, mac, DecodeEventType.STATUS, "STATUS UPDATE - UNIT:" + sue.getUnitStatus() + " USER:" + sue.getUserStatus());
                }
                break;
            case PHASE1_DA_STATUS_QUERY_EXTENDED_VCH:
                if(mac instanceof StatusQueryExtendedVCH)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.QUERY, "STATUS");
                }
                break;
            case PHASE1_DB_STATUS_QUERY_EXTENDED_LCCH:
                if(mac instanceof StatusQueryExtendedLCCH)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.QUERY, "STATUS");
                }
                break;
        }


    }

    /**
     * Talker Alias
     */
    private void processTalkerAlias(MacMessage message, MacStructure mac)
    {
        if(mac instanceof L3HarrisTalkerAlias talkerAlias)
        {
            P25TalkerAliasIdentifier alias = talkerAlias.getAlias();
            getIdentifierCollection().update(alias);
            mTrafficChannelManager.processP2TrafficCurrentUser(getCurrentFrequency(), getTimeslot(), alias, message.getTimestamp());

            //Add the alias to the talker alias manager if we know the associated radio
            Identifier from = getIdentifierCollection().getFromIdentifier();

            if(from instanceof RadioIdentifier ri)
            {
                mTrafficChannelManager.getTalkerAliasManager().update(ri, alias);
            }
        }
    }

    /**
     * Unit registration
     */
    private void processUnitRegistration(MacMessage message, MacStructure mac)
    {
        switch(mac.getOpcode())
        {
            case PHASE1_6C_UNIT_REGISTRATION_RESPONSE_ABBREVIATED:
            case PHASE1_EC_UNIT_REGISTRATION_RESPONSE_EXTENDED:
                broadcast(message, mac, getCurrentChannel(), DecodeEventType.RESPONSE, "UNIT REGISTRATION");
                break;
            case PHASE1_6D_UNIT_REGISTRATION_COMMAND_ABBREVIATED:
                broadcast(message, mac, getCurrentChannel(), DecodeEventType.COMMAND, "UNIT REGISTER");
                break;
            case PHASE1_6F_DEREGISTRATION_ACKNOWLEDGE:
                broadcast(message, mac, getCurrentChannel(), DecodeEventType.ACKNOWLEDGE, "UNIT DEREGISTERED");
                break;
        }
    }

    /**
     * Creates and broadcasts a decode event and broadcasts it for the specified channel
     *
     * @param message for the event
     * @param mac for the event
     * @param channel for the event
     * @param eventType of event
     * @param details to populate for the event
     */
    private void broadcast(MacMessage message, MacStructure mac, IChannelDescriptor channel, DecodeEventType eventType,
                           String details)
    {
        MutableIdentifierCollection mic = getUpdatedMutableIdentifierCollection(mac);
        broadcast(P25DecodeEvent.builder(eventType, message.getTimestamp()).channel(channel)
                .details(details)
                .identifiers(mic)
                .timeslot(getTimeslot())
                .build());
    }

    /**
     * Creates a decode event and broadcasts it for the current channel.
     *
     * @param message with a timestamp
     * @param structure containing identifiers for the event
     * @param eventType for the generated event.
     * @param details to assign to the generated event.
     */
    private void broadcast(MacMessage message, MacStructure structure, DecodeEventType eventType, String details)
    {
        broadcast(message, structure, getCurrentChannel(), eventType, details);
    }

    /**
     * Creates a copy of the current identifier collection with the USER identifiers removed and updated from the
     * mac argument identifiers.
     *
     * @param mac containing updated identifiers to add to the returned collection.
     * @return mutable identifier collection.
     */
    private MutableIdentifierCollection getUpdatedMutableIdentifierCollection(MacStructure mac)
    {
        MutableIdentifierCollection mic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        mic.remove(IdentifierClass.USER);
        mic.update(mac.getIdentifiers());
        mTrafficChannelManager.getTalkerAliasManager().enrichMutable(mic);
        return mic;
    }

    /**
     * Broadcasts a state continuation.  If we're currently in a call, then we broadcast a call continuation, otherwise
     * we broadcast a continuation of the specified state.
     *
     * @param state to continue
     */
    private void continueState(State state)
    {
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, state, getTimeslot()));
    }


    /**
     * Indicates if the current set of identifiers contains an encryption key indicating that the communication is
     * encrypted.
     *
     * @return true if there is an encryption key indicating encryption is currently in use.
     */
    private boolean isEncrypted()
    {
        for(Identifier<?> identifier : getIdentifierCollection().getIdentifiers(Form.ENCRYPTION_KEY))
        {
            if(identifier.getValue() instanceof EncryptionKey && ((EncryptionKey)identifier.getValue()).isEncrypted())
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mNetworkConfigurationMonitor.getActivitySummary());
        sb.append("\n");
        sb.append(mPatchGroupManager.getPatchGroupSummary());
        sb.append("\n");
        sb.append(mTrafficChannelManager.getTalkerAliasManager().getAliasSummary());
        return sb.toString();
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        if(event.getTimeslot() == getTimeslot())
        {
            switch(event.getEvent())
            {
                case REQUEST_RESET:
                    resetState();
                    mNetworkConfigurationMonitor.reset();
                    break;
                case NOTIFICATION_SOURCE_FREQUENCY:
                    long frequency = event.getFrequency();

                    //Notify the TCM that our control frequency has changed.
                    if(mChannel.isStandardChannel())
                    {
                        mTrafficChannelManager.setCurrentControlFrequency(frequency, mChannel);
                    }
                default:
                    break;
            }
        }
    }

    @Override
    public void start()
    {
        super.start();

        //Change the default (45-second) traffic channel timeout to 1 second
        if(mChannel.isTrafficChannel())
        {
            broadcast(new ChangeChannelTimeoutEvent(this, ChannelType.TRAFFIC, 1000, getTimeslot()));
        }
    }

    @Override
    public void init()
    {
    }
}