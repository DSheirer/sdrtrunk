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
import io.github.dsheirer.identifier.encryption.EncryptionKey;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.identifier.patch.PatchGroupPreLoadDataContent;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.module.decode.p25.P25DecodeEvent;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
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
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationResponseAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationResponseExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupRegroupVoiceChannelUserAbbreviated;
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
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacRelease;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MessageUpdateExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PowerControlSignalQuality;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.QueuedResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataPageRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelGrantUpdateImplicit;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateExtendedLCCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateExtendedVCH;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisGpsLocation;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisGroupRegroupExplicitEncryptionCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris.L3HarrisTalkerAlias;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaDenyResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupAddCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupDeleteCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola.MotorolaGroupRegroupVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import io.github.dsheirer.module.decode.p25.reference.ExtendedFunction;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.Date;
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
    private static int SYSTEM_CONTROLLER = 0xFFFFFF;
    private ChannelType mChannelType;
    private DecodeEvent mCurrentCallEvent;
    private PatchGroupManager mPatchGroupManager = new PatchGroupManager();
    private P25P2NetworkConfigurationMonitor mNetworkConfigurationMonitor = new P25P2NetworkConfigurationMonitor();
    private P25TrafficChannelManager mTrafficChannelManager;
    private int mEndPttOnFacchCounter = 0;

    /**
     * Constructs an APCO-25 decoder state instance for a traffic or control channel.
     * @param channel with configuration details
     * @param timeslot for this decoder state
     * @param trafficChannelManager to coordinate traffic channel activity
     */
    public P25P2DecoderState(Channel channel, int timeslot, P25TrafficChannelManager trafficChannelManager)
    {
        super(timeslot);
        mChannelType = channel.getChannelType();
        mTrafficChannelManager = trafficChannelManager;
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
        closeCurrentCallEvent(System.currentTimeMillis(), true, MacPduType.MAC_3_IDLE);
        mEndPttOnFacchCounter = 0;
    }

    /**
     * Processes an identifier collection to harvest Patch Groups to preload when this channel is first starting up.
     * @param preLoadDataContent containing an identifier collection with optional patch group identifier(s).
     */
    @Subscribe
    public void process(PatchGroupPreLoadDataContent preLoadDataContent)
    {
        for(Identifier identifier: preLoadDataContent.getData().getIdentifiers(Role.TO))
        {
            if(identifier instanceof PatchGroupIdentifier patchGroupIdentifier)
            {
                mPatchGroupManager.addPatchGroup(patchGroupIdentifier);
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

                MacPduType macPduType = macMessage.getMacPduType();

                //Ignore End PTT - this is handled in the processMacMessage() method
                if(macPduType != MacPduType.MAC_2_END_PTT)
                {
                    mEndPttOnFacchCounter = 0;
                    continueState(getStateFromPduType(macPduType));
                }

                //Close current call event on IDLE or HANGTIME
                if(macPduType == MacPduType.MAC_3_IDLE || macPduType == MacPduType.MAC_6_HANGTIME)
                {
                    closeCurrentCallEvent(message.getTimestamp(), true, macPduType);
                }
            }
            else if(message instanceof AbstractVoiceTimeslot)
            {
                if(mCurrentCallEvent != null)
                {
                    if(isEncrypted())
                    {
                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ENCRYPTED, getTimeslot()));
                    }
                    else
                    {
                        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL, getTimeslot()));
                    }
                }

                updateCurrentCall(null, null, message.getTimestamp());
            }
            else if(message instanceof EncryptionSynchronizationSequence)
            {
                //We don't send any state events for this message since it can only occur in conjunction with
                //an audio frame that already sends the call state event
                getIdentifierCollection().update(message.getIdentifiers());
            }
        }
    }

    /**
     * Updates the channel state according to the PDU type
     */
    private static State getStateFromPduType(MacPduType macPduType)
    {
        switch(macPduType)
        {
            case MAC_0_SIGNAL:
                return State.CONTROL;
            case MAC_1_PTT:
                return State.CALL;
            case MAC_2_END_PTT:
                return State.TEARDOWN;
            case MAC_4_ACTIVE:
            case MAC_6_HANGTIME:
            case MAC_3_IDLE:
            default:
                return State.ACTIVE;
        }
    }

    /**
     * Adds the current channel to the local identifier collection which will cause it to be broadcast to all of the
     * other listeners and will allow both timeslots on this channel to receive it and update accordingly.
     *
     * @param channel to broadcast
     */
    private void broadcastCurrentChannel(APCO25Channel channel)
    {
        getIdentifierCollection().update(channel);
    }

    /**
     * Process MAC message structures
     */
    private void processMacMessage(MacMessage message)
    {
        mNetworkConfigurationMonitor.processMacMessage(message);

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
                processChannelUser(message, mac);
                break;
            case TDMA_02_UNIT_TO_UNIT_VOICE_CHANNEL_USER_ABBREVIATED:
            case TDMA_03_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                processChannelUser(message, mac);
                break;
            case TDMA_05_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_IMPLICIT:
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
            case TDMA_21_GROUP_VOICE_CHANNEL_USER_EXTENDED:
            case TDMA_22_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
                processChannelUser(message, mac);
                break;
            case TDMA_25_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_EXPLICIT:
                processChannelGrantUpdate(message, mac);
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
                processChannelGrant(message, mac);
                break;
            case PHASE1_41_GROUP_VOICE_SERVICE_REQUEST:
                if(mac instanceof GroupVoiceServiceRequest gvsr)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST, "GROUP VOICE SERVICE " + gvsr.getServiceOptions());
                }
                break;
            case PHASE1_42_GROUP_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
                processChannelGrantUpdate(message, mac);
                break;
            case PHASE1_44_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_ABBREVIATED:
                processChannelGrant(message, mac);
                break;
            case PHASE1_45_UNIT_TO_UNIT_ANSWER_REQUEST_ABBREVIATED:
                processAnswer(message, mac);
                break;
            case PHASE1_46_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED:
                processChannelGrantUpdate(message, mac);
                break;
            case PHASE1_48_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_IMPLICIT:
                processChannelGrant(message, mac);
                break;
            case PHASE1_49_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
                processChannelGrantUpdate(message, mac);
                break;
            case PHASE1_4A_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
                processAnswer(message, mac);
                break;
            case PHASE1_4C_RADIO_UNIT_MONITOR_COMMAND_ABBREVIATED:
                processRadioUnitMonitor(message, mac);
                break;
            case PHASE1_52_SNDCP_DATA_CHANNEL_REQUEST:
                //Ignore - this is an inbound request by the SU
                break;
            case PHASE1_53_SNDCP_DATA_PAGE_RESPONSE:
                //Ignore - this is an inbound request by the SU
                break;
            case PHASE1_54_SNDCP_DATA_CHANNEL_GRANT:
                processChannelGrant(message, mac);
                break;
            case PHASE1_55_SNDCP_DATA_PAGE_REQUEST:
                processDataPageRequest(message, mac);
                break;
            case PHASE1_58_STATUS_UPDATE_ABBREVIATED:
                processStatus(message, mac);
                break;
            case PHASE1_5A_STATUS_QUERY_ABBREVIATED:
                processStatus(message, mac);
                break;
            case PHASE1_5C_MESSAGE_UPDATE_ABBREVIATED:
                processMessageUpdate(message, mac);
                break;
            case PHASE1_5D_RADIO_UNIT_MONITOR_COMMAND_OBSOLETE:
            case PHASE1_5E_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_ABBREVIATED:
                processRadioUnitMonitor(message, mac);
                break;
            case PHASE1_5F_CALL_ALERT_ABBREVIATED:
                processCallAlert(message, mac);
                break;
            case PHASE1_60_ACKNOWLEDGE_RESPONSE_FNE_ABBREVIATED:
                processAcknowledge(message, mac);
                break;
            case PHASE1_61_QUEUED_RESPONSE:
                processQueued(message, mac);
                break;
            case PHASE1_64_EXTENDED_FUNCTION_COMMAND_ABBREVIATED:
                processExtendedFunctionCommand(message, mac);
                break;
            case PHASE1_67_DENY_RESPONSE:
                processDeny(message, mac);
                break;
            case PHASE1_68_GROUP_AFFILIATION_RESPONSE_ABBREVIATED:
            case PHASE1_6A_GROUP_AFFILIATION_QUERY_ABBREVIATED:
                processAffiliation(message, mac);
                break;
            case PHASE1_6B_LOCATION_REGISTRATION_RESPONSE:
                processLocationRegistration(message, mac);
                break;
            case PHASE1_6C_UNIT_REGISTRATION_RESPONSE_ABBREVIATED:
            case PHASE1_6D_UNIT_REGISTRATION_COMMAND_ABBREVIATED:
            case PHASE1_6F_DEREGISTRATION_ACKNOWLEDGE:
                processUnitRegistration(message, mac);
                break;
            case PHASE1_70_SYNCHRONIZATION_BROADCAST:
                //Ignore - channel timing information
                break;
            case PHASE1_71_AUTHENTICATION_DEMAND:
            case PHASE1_72_AUTHENTICATION_FNE_RESPONSE_ABBREVIATED:
                processAuthentication(message, mac);
                break;
            case PHASE1_76_ROAMING_ADDRESS_COMMAND:
            case PHASE1_77_ROAMING_ADDRESS_UPDATE:
                processRoamingAddress(message, mac);
                break;
            case PHASE1_73_IDENTIFIER_UPDATE_TDMA_ABBREVIATED:
            case PHASE1_74_IDENTIFIER_UPDATE_V_UHF:
                processNetwork(message, mac);
                break;
            case PHASE1_75_TIME_AND_DATE_ANNOUNCEMENT:
                //Ignore
                break;
            case PHASE1_78_SYSTEM_SERVICE_BROADCAST:
            case PHASE1_79_SECONDARY_CONTROL_CHANNEL_BROADCAST_IMPLICIT:
            case PHASE1_7A_RFSS_STATUS_BROADCAST_IMPLICIT:
            case PHASE1_7B_NETWORK_STATUS_BROADCAST_IMPLICIT:
            case PHASE1_7C_ADJACENT_STATUS_BROADCAST_IMPLICIT:
            case PHASE1_7D_IDENTIFIER_UPDATE:
                processNetwork(message, mac);
                break;

            /**
             * Partition 3 Opcodes
             */
            case PHASE1_EXTENDED_PARTITION_3_UNKNOWN_OPCODE:
                //Ignore
                break;
            case PHASE1_90_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                processChannelUser(message, mac);
                break;
            case PHASE1_C0_GROUP_VOICE_CHANNEL_GRANT_EXPLICIT:
                processChannelGrant(message, mac);
                break;
            case PHASE1_C3_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                processChannelGrantUpdate(message, mac);
                break;
            case PHASE1_C4_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_EXTENDED_VCH:
                processChannelGrant(message, mac);
                break;
            case PHASE1_C5_UNIT_TO_UNIT_ANSWER_REQUEST_EXTENDED:
                processAnswer(message, mac);
                break;
            case PHASE1_C6_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_VCH:
            case PHASE1_C7_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_LCCH:
                processChannelGrantUpdate(message, mac);
                break;
            case PHASE1_C8_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_EXPLICIT:
                processChannelGrant(message, mac);
                break;
            case PHASE1_C9_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                processChannelGrantUpdate(message, mac);
                break;
            case PHASE1_CB_CALL_ALERT_EXTENDED_LCCH:
                processCallAlert(message, mac);
                break;
            case PHASE1_CC_RADIO_UNIT_MONITOR_COMMAND_EXTENDED_VCH:
            case PHASE1_CD_RADIO_UNIT_MONITOR_COMMAND_EXTENDED_LCCH:
                processRadioUnitMonitor(message, mac);
                break;
            case PHASE1_CE_MESSAGE_UPDATE_EXTENDED_LCCH:
                processMessageUpdate(message, mac);
                break;
            case PHASE1_CF_UNIT_TO_UNIT_VOICE_SERVICE_CHANNEL_GRANT_EXTENDED_LCCH:
                processChannelGrant(message, mac);
                break;
            case PHASE1_D6_SNDCP_DATA_CHANNEL_ANNOUNCEMENT:
                processNetwork(message, mac);
                break;
            case PHASE1_D8_STATUS_UPDATE_EXTENDED_VCH:
            case PHASE1_D9_STATUS_UPDATE_EXTENDED_LCCH:
            case PHASE1_DA_STATUS_QUERY_EXTENDED_VCH:
            case PHASE1_DB_STATUS_QUERY_EXTENDED_LCCH:
                processStatus(message, mac);
                break;
            case PHASE1_DC_MESSAGE_UPDATE_EXTENDED_VCH:
                processMessageUpdate(message, mac);
                break;
            case PHASE1_DE_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_EXTENDED:
                processRadioUnitMonitor(message, mac);
                break;
            case PHASE1_DF_CALL_ALERT_EXTENDED_VCH:
                processCallAlert(message, mac);
                break;
            case PHASE1_E0_ACKNOWLEDGE_RESPONSE_FNE_EXTENDED:
                processAcknowledge(message, mac);
                break;
            case PHASE1_E4_EXTENDED_FUNCTION_COMMAND_EXTENDED_VCH:
            case PHASE1_E5_EXTENDED_FUNCTION_COMMAND_EXTENDED_LCCH:
                processExtendedFunctionCommand(message, mac);
                break;
            case PHASE1_E8_GROUP_AFFILIATION_RESPONSE_EXTENDED:
                processAffiliation(message, mac);
                break;
            case PHASE1_E9_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                processNetwork(message, mac);
                break;
            case PHASE1_EA_GROUP_AFFILIATION_QUERY_EXTENDED:
                processAffiliation(message, mac);
                break;
            case PHASE1_EC_UNIT_REGISTRATION_RESPONSE_EXTENDED:
                processUnitRegistration(message, mac);
                break;
            case PHASE1_F2_AUTHENTICATION_FNE_RESPONSE_EXTENDED:
                processAuthentication(message, mac);
                break;
            case PHASE1_F3_IDENTIFIER_UPDATE_TDMA_EXTENDED:
            case PHASE1_FA_RFSS_STATUS_BROADCAST_EXPLICIT:
            case PHASE1_FB_NETWORK_STATUS_BROADCAST_EXPLICIT:
            case PHASE1_FC_ADJACENT_STATUS_BROADCAST_EXPLICIT:
            case PHASE1_FE_ADJACENT_STATUS_BROADCAST_EXTENDED_EXPLICIT:
                processNetwork(message, mac);
                break;

            /**
             * Partition 2 Opcodes
             */
            case VENDOR_PARTITION_2_UNKNOWN_OPCODE:
                //Ignore
                break;

            //Vendor: L3Harris
            case L3HARRIS_AA_GPS_LOCATION:
                processGPS(message, mac);
                break;
            case L3HARRIS_A8_TALKER_ALIAS:
                processTalkerAlias(message,mac);
                break;
            case L3HARRIS_B0_GROUP_REGROUP_EXPLICIT_ENCRYPTION_COMMAND:
                processDynamicRegrouping(message, mac);
                break;

            //Vendor: Motorola
            case MOTOROLA_80_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                processChannelUser(message, mac);
                break;
            case MOTOROLA_81_GROUP_REGROUP_ADD:
                processDynamicRegrouping(message, mac);
                break;
            case MOTOROLA_83_GROUP_REGROUP_VOICE_CHANNEL_UPDATE:
                processChannelGrantUpdate(message, mac);
                break;
            case MOTOROLA_84_EXTENDED_FUNCTION_COMMAND:
                processExtendedFunctionCommand(message, mac);
                break;
            case MOTOROLA_89_GROUP_REGROUP_DELETE:
                processDynamicRegrouping(message, mac);
                break;
            case MOTOROLA_91_GROUP_REGROUP_UNKNOWN:
                //Unknown
                break;
            case MOTOROLA_95_UNKNOWN_149:
                //Unknown
                break;
            case MOTOROLA_A0_GROUP_REGROUP_VOICE_CHANNEL_USER_EXTENDED:
                processChannelUser(message, mac);
                break;
            case MOTOROLA_A3_GROUP_REGROUP_CHANNEL_GRANT_IMPLICIT:
            case MOTOROLA_A4_GROUP_REGROUP_CHANNEL_GRANT_EXPLICIT:
                processChannelGrant(message, mac);
                break;
            case MOTOROLA_A5_GROUP_REGROUP_CHANNEL_GRANT_UPDATE:
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
     */
    private MutableIdentifierCollection getIdentifierCollectionForUser(Identifier userIdentifierToAdd)
    {
        return getIdentifierCollectionForUsers(Collections.singletonList(userIdentifierToAdd));
    }

    /**
     * Creates a copy of the current identifier collection with all USER and CHANNEL identifiers removed and adds the
     * user identifiers argument added to the collection.
     */
    private MutableIdentifierCollection getIdentifierCollectionForUsers(List<Identifier> identifiersToAdd)
    {
        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        ic.remove(IdentifierClass.USER);
        ic.remove(Form.CHANNEL);
        for(Identifier identifier: identifiersToAdd)
        {
            //Filter the identifiers through the patch group manager
            ic.update(mPatchGroupManager.update(identifier));
        }
        return ic;
    }

    /**
     * Acknowledgements.
     */
    private void processAcknowledge(MacMessage message, MacStructure mac)
    {
        if(mac instanceof AcknowledgeResponseFNEAbbreviated ar)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.ACKNOWLEDGE, "ACKNOWLEDGE: " +
                    ar.getServiceType());
        }
        else if(mac instanceof AcknowledgeResponseFNEExtended ar)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.ACKNOWLEDGE, "ACKNOWLEDGE: " +
                    ar.getServiceType());
        }
        else if(mac instanceof MotorolaAcknowledgeResponse ar)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.ACKNOWLEDGE, "ACKNOWLEDGE: " +
                    ar.getServiceType());
        }
    }

    /**
     * Affiliation request and response (for talkgroups).
     */
    private void processAffiliation(MacMessage message, MacStructure mac)
    {
        if(mac instanceof GroupAffiliationQueryAbbreviated || mac instanceof GroupAffiliationQueryExtended)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.QUERY, "GROUP AFFILIATION");
        }
        else if(mac instanceof GroupAffiliationResponseAbbreviated || mac instanceof GroupAffiliationResponseExtended)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.RESPONSE, "GROUP AFFILIATION");
        }
    }

    /**
     * Answer Request & Response
     */
    private void processAnswer(MacMessage message, MacStructure mac)
    {
        if(mac instanceof TelephoneInterconnectAnswerRequest tiar)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST,
                    "TELEPHONE INTERCONNECT ANSWER REQUEST");
        }

        if(mac instanceof UnitToUnitAnswerRequestAbbreviated uuara)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST,
                    "UNIT-TO-UNIT ANSWER REQUEST - " + uuara.getServiceOptions());
        }

        if(mac instanceof UnitToUnitAnswerRequestExtended uuare)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST, "UNIT-TO-UNIT ANSWER REQUEST "
                    + uuare.getServiceOptions());
        }
    }

    /**
     * Authentication
     */
    private void processAuthentication(MacMessage message, MacStructure mac)
    {
        if(mac instanceof AuthenticationDemand ad)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.COMMAND,
                    "AUTHENTICATE - SEED:" + ad.getRandomSeed() + " CHALLENGE:" + ad.getChallenge());
        }
        else if(mac instanceof AuthenticationFNEResponseAbbreviated ar)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.RESPONSE,
                "AUTHENTICATION " + ar.getResponse());
        }
        else if(mac instanceof AuthenticationFNEResponseExtended ar)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.RESPONSE,
                    "AUTHENTICATION " + ar.getResponse());
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
        //All channel grant messages implement this interface
        if(mac instanceof IP25ChannelGrantDetailProvider cgdp)
        {
            MutableIdentifierCollection ic = getIdentifierCollectionForUsers(mac.getIdentifiers());
            //Add the traffic channel to the IC
            ic.update(cgdp.getChannel());
            mTrafficChannelManager.processChannelGrant(cgdp.getChannel(), cgdp.getServiceOptions(), ic, mac.getOpcode(),
                    message.getTimestamp());
        }


//TODO: verify if this is still needed or can it be accomplished elsewhere?
        if(getCurrentChannel() == null && mac instanceof GroupVoiceChannelGrantImplicit gvcga)
        {
            if(isCurrentGroup(gvcga.getTargetAddress()))
            {
                broadcastCurrentChannel(gvcga.getChannel());
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
        switch(mac.getOpcode())
        {
            case TDMA_05_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_IMPLICIT:
                if(mac instanceof GroupVoiceChannelGrantUpdateMultipleImplicit cgu)
                {
                    MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getGroupAddress1());
                    mic.update(cgu.getChannel1());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions1(), mic,
                            mac.getOpcode(), message.getTimestamp());

                    if(cgu.hasGroup2())
                    {
                        MutableIdentifierCollection mic2 = getIdentifierCollectionForUser(cgu.getGroupAddress2());
                        mic2.update(cgu.getChannel1());
                        mTrafficChannelManager.processChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions2(), mic2,
                                mac.getOpcode(), message.getTimestamp());
                    }

                    if(cgu.hasGroup3())
                    {
                        MutableIdentifierCollection mic3 = getIdentifierCollectionForUser(cgu.getGroupAddress3());
                        mic3.update(cgu.getChannel1());
                        mTrafficChannelManager.processChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions3(), mic3,
                                mac.getOpcode(), message.getTimestamp());
                    }
                }
                break;
            case TDMA_25_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_EXPLICIT:
                if(mac instanceof GroupVoiceChannelGrantUpdateMultipleExplicit cgu)
                {
                    MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getGroupAddress1());
                    mic.update(cgu.getChannel1());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions1(), mic,
                            mac.getOpcode(), message.getTimestamp());

                    if(cgu.hasGroup2())
                    {
                        MutableIdentifierCollection mic2 = getIdentifierCollectionForUser(cgu.getGroupAddress2());
                        mic2.update(cgu.getChannel1());
                        mTrafficChannelManager.processChannelUpdate(cgu.getChannel1(), cgu.getServiceOptions2(), mic2,
                                mac.getOpcode(), message.getTimestamp());
                    }
                }
                break;
            case PHASE1_42_GROUP_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
                if(mac instanceof GroupVoiceChannelGrantUpdateImplicit cgu)
                {
                    //Create an empty service options
                    VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);
                    MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getGroupAddress1());
                    mic.update(cgu.getChannel1());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel1(), serviceOptions, mic, mac.getOpcode(),
                            message.getTimestamp());

                    if(cgu.hasGroup2())
                    {
                        MutableIdentifierCollection mic2 = getIdentifierCollectionForUser(cgu.getGroupAddress2());
                        mic2.update(cgu.getChannel1());
                        mTrafficChannelManager.processChannelUpdate(cgu.getChannel1(), serviceOptions, mic2,
                                mac.getOpcode(), message.getTimestamp());
                    }
                }
                break;
            case PHASE1_46_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED:
                if(mac instanceof UnitToUnitVoiceChannelGrantUpdateAbbreviated cgu)
                {
                    //Create an empty service options
                    VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);
                    MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers());
                    mic.update(cgu.getChannel());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel(), serviceOptions, mic, mac.getOpcode(),
                            message.getTimestamp());
                }
                break;
            case PHASE1_49_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_IMPLICIT:
                if(mac instanceof TelephoneInterconnectVoiceChannelGrantUpdateImplicit cgu)
                {
                    //Create an empty service options
                    VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);
                    MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers());
                    mic.update(cgu.getChannel());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel(), serviceOptions, mic, mac.getOpcode(),
                            message.getTimestamp());
                }
                break;
            case PHASE1_C3_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                if(mac instanceof GroupVoiceChannelGrantUpdateExplicit cgu)
                {
                    MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getGroupAddress());
                    mic.update(cgu.getChannel());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel(), cgu.getServiceOptions(), mic,
                            mac.getOpcode(), message.getTimestamp());
                }
                break;
            case PHASE1_C6_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_VCH:
                if(mac instanceof UnitToUnitVoiceChannelGrantUpdateExtendedVCH cgu)
                {
                    //Create an empty service options
                    VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);
                    MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers());
                    mic.update(cgu.getChannel());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel(), serviceOptions, mic, mac.getOpcode(),
                            message.getTimestamp());
                }
                break;
            case PHASE1_C7_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED_LCCH:
                if(mac instanceof UnitToUnitVoiceChannelGrantUpdateExtendedLCCH cgu)
                {
                    MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers());
                    mic.update(cgu.getChannel());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel(), cgu.getServiceOptions(), mic,
                            mac.getOpcode(), message.getTimestamp());
                }
                break;
            case PHASE1_C9_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                if(mac instanceof TelephoneInterconnectVoiceChannelGrantUpdateExplicit cgu)
                {
                    MutableIdentifierCollection mic = getIdentifierCollectionForUsers(cgu.getIdentifiers());
                    mic.update(cgu.getChannel());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel(), cgu.getServiceOptions(), mic,
                            mac.getOpcode(), message.getTimestamp());
                }
                break;
            case MOTOROLA_83_GROUP_REGROUP_VOICE_CHANNEL_UPDATE:
                if(mac instanceof MotorolaGroupRegroupVoiceChannelUpdate cgu)
                {
                    MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getSupergroup());
                    mic.update(cgu.getChannel());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannel(), cgu.getServiceOptions(), mic,
                            mac.getOpcode(), message.getTimestamp());
                }
                break;
            case MOTOROLA_A5_GROUP_REGROUP_CHANNEL_GRANT_UPDATE:
                if(mac instanceof MotorolaGroupRegroupChannelGrantUpdate cgu)
                {
                    //Create an empty service options
                    VoiceServiceOptions serviceOptions = new VoiceServiceOptions(0);
                    MutableIdentifierCollection mic = getIdentifierCollectionForUser(cgu.getPatchgroupA());
                    mic.update(cgu.getChannelA());
                    mTrafficChannelManager.processChannelUpdate(cgu.getChannelA(), serviceOptions, mic, mac.getOpcode(),
                            message.getTimestamp());

                    if(cgu.hasPatchgroupB())
                    {
                        MutableIdentifierCollection mic2 = getIdentifierCollectionForUser(cgu.getPatchgroupB());
                        mic2.update(cgu.getChannelB());
                        mTrafficChannelManager.processChannelUpdate(cgu.getChannelB(), serviceOptions, mic2,
                                mac.getOpcode(), message.getTimestamp());
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
     * Channel user (ie current user on this channel).
     */
    private void processChannelUser(MacMessage message, MacStructure mac)
    {
//TODO:
//        TDMA_01_GROUP_VOICE_CHANNEL_USER_ABBREVIATED
//        TDMA_02_UNIT_TO_UNIT_VOICE_CHANNEL_USER_ABBREVIATED:
//        TDMA_03_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER
//        TDMA_21_GROUP_VOICE_CHANNEL_USER_EXTENDED:
//        TDMA_22_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
//        PHASE1_90_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED
//        MOTOROLA_80_GROUP_REGROUP_VOICE_CHANNEL_USER_ABBREVIATED:
//        MOTOROLA_A0_GROUP_REGROUP_VOICE_CHANNEL_USER_EXTENDED

        if(message.getMacPduType() == MacPduType.MAC_6_HANGTIME)
        {
            closeCurrentCallEvent(message.getTimestamp(), false, MacPduType.MAC_6_HANGTIME);

            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }
        }
        else
        {
            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }

            if(mac instanceof GroupVoiceChannelUserAbbreviated gvcua)
            {
                if(gvcua.getServiceOptions().isEncrypted())
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP_ENCRYPTED, null, message.getTimestamp());
                }
                else
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP, null, message.getTimestamp());
                }
            }
            else if(mac instanceof GroupRegroupVoiceChannelUserAbbreviated grvcua)
            {
                updateCurrentCall(DecodeEventType.CALL_GROUP, "SUPERGROUP", message.getTimestamp());
            }
            else if(mac instanceof MotorolaGroupRegroupVoiceChannelUserAbbreviated mgrvcua)
            {
                if(mgrvcua.getServiceOptions().isEncrypted())
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP_ENCRYPTED, "SUPERGROUP", message.getTimestamp());
                }
                else
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP, "SUPERGROUP", message.getTimestamp());
                }
            }
        }

        if(message.getMacPduType() == MacPduType.MAC_6_HANGTIME)
        {
            closeCurrentCallEvent(message.getTimestamp(), false, MacPduType.MAC_6_HANGTIME);

            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }
        }
        else
        {
            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }

            if(mac instanceof UnitToUnitVoiceChannelUserAbbreviated)
            {
                UnitToUnitVoiceChannelUserAbbreviated uuvcua = (UnitToUnitVoiceChannelUserAbbreviated)mac;

                if(uuvcua.getServiceOptions().isEncrypted())
                {
                    updateCurrentCall(DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED, null, message.getTimestamp());
                }
                else
                {
                    updateCurrentCall(DecodeEventType.CALL_UNIT_TO_UNIT, null, message.getTimestamp());
                }
            }
        }


        if(message.getMacPduType() == MacPduType.MAC_6_HANGTIME)
        {
            closeCurrentCallEvent(message.getTimestamp(), false, MacPduType.MAC_6_HANGTIME);

            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }
        }
        else
        {
            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }

            if(mac instanceof TelephoneInterconnectVoiceChannelUser tivcu)
            {
                if(tivcu.getServiceOptions().isEncrypted())
                {
                    updateCurrentCall(DecodeEventType.CALL_INTERCONNECT_ENCRYPTED, null, message.getTimestamp());
                }
                else
                {
                    updateCurrentCall(DecodeEventType.CALL_INTERCONNECT, null, message.getTimestamp());
                }
            }
        }

        if(message.getMacPduType() == MacPduType.MAC_6_HANGTIME)
        {
            closeCurrentCallEvent(message.getTimestamp(), false, MacPduType.MAC_6_HANGTIME);

            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }
        }
        else
        {
            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }

            if(mac instanceof GroupVoiceChannelUserExtended gvcue)
            {
                if(gvcue.getServiceOptions().isEncrypted())
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP_ENCRYPTED, null, message.getTimestamp());
                }
                else
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP, null, message.getTimestamp());
                }
            }
            else if(mac instanceof MotorolaGroupRegroupVoiceChannelUserExtended mgrvcue)
            {
                if(mgrvcue.getServiceOptions().isEncrypted())
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP_ENCRYPTED, null, message.getTimestamp());
                }
                else
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP, null, message.getTimestamp());
                }
            }
        }


        if(message.getMacPduType() == MacPduType.MAC_6_HANGTIME)
        {
            closeCurrentCallEvent(message.getTimestamp(), false, MacPduType.MAC_6_HANGTIME);

            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }
        }
        else
        {
            for(Identifier identifier : mac.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }

            if(mac instanceof UnitToUnitVoiceChannelUserExtended uuvcue)
            {
                if(uuvcue.getServiceOptions().isEncrypted())
                {
                    updateCurrentCall(DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED, null, message.getTimestamp());
                }
                else
                {
                    updateCurrentCall(DecodeEventType.CALL_UNIT_TO_UNIT, null, message.getTimestamp());
                }
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
            getIdentifierCollection().update(mPatchGroupManager.update(identifier));
        }

        PushToTalk ptt = (PushToTalk)mac;

        if(ptt.isEncrypted())
        {
            updateCurrentCall(DecodeEventType.CALL_ENCRYPTED, ptt.getEncryptionKey().toString(), message.getTimestamp());
        }
        else
        {
            updateCurrentCall(DecodeEventType.CALL, null, message.getTimestamp());
        }
    }

    /**
     * Dynamic Regrouping
     */
    private void processDynamicRegrouping(MacMessage message, MacStructure mac)
    {
        if(mac instanceof L3HarrisGroupRegroupExplicitEncryptionCommand regroup)
        {
            if(regroup.getRegroupOptions().isActivate())
            {
                mPatchGroupManager.addPatchGroup(regroup.getPatchGroup());
            }
            else
            {
                mPatchGroupManager.removePatchGroup(regroup.getPatchGroup());
            }
        }
        if(mac instanceof MotorolaGroupRegroupAddCommand mgrac)
        {
            mPatchGroupManager.addPatchGroup(mgrac.getPatchGroup());
        }
        if(mac instanceof MotorolaGroupRegroupDeleteCommand mgrdc)
        {
            mPatchGroupManager.removePatchGroup(mgrdc.getPatchGroup());
        }
    }

    /**
     * End Push-To-Talk (ie end of current audio segment on this channel).
     */
    private void processEndPushToTalk(MacMessage message, MacStructure mac)
    {
        if(mac instanceof EndPushToTalk)
        {
            //Add the set of identifiers before we close out the call event to ensure they're captured in
            //the closing event.
            if(mCurrentCallEvent != null)
            {
                for(Identifier identifier : mac.getIdentifiers())
                {
                    if(identifier.getRole() == Role.FROM)
                    {
                        if(identifier instanceof APCO25RadioIdentifier)
                        {
                            int value = ((APCO25RadioIdentifier)identifier).getValue();

                            //Group call End PTT uses a FROM value of 0xFFFFFF - don't overwrite the correct id
                            if(value != SYSTEM_CONTROLLER)
                            {
                                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
                            }
                        }
                    }
                    else if(identifier.getRole() == Role.TO)
                    {
                        if(identifier instanceof APCO25Talkgroup)
                        {
                            int value = ((APCO25Talkgroup)identifier).getValue();

                            //Individual call End PTT uses a TO value of 0 - don't overwrite the correct id
                            if(value != 0)
                            {
                                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
                            }
                        }
                    }
                    else
                    {
                        getIdentifierCollection().update(mPatchGroupManager.update(identifier));
                    }
                }
            }

            if(message.getDataUnitID().isFACCH())
            {
                mEndPttOnFacchCounter++;

                //FNE sending 2 or more End PTT in FACCH timeslots indicates a channel teardown event.
                if(mEndPttOnFacchCounter > 1)
                {
                    if(mChannelType == ChannelType.TRAFFIC)
                    {
                        closeCurrentCallEvent(message.getTimestamp(), true, MacPduType.MAC_2_END_PTT);
                        //Don't send a continue state here ... the close() method will send the TEARDOWN state
                    }
                    //Otherwise the channel state is set to ACTIVE in anticipation of further call activity
                    else
                    {
                        closeCurrentCallEvent(message.getTimestamp(), true, MacPduType.MAC_4_ACTIVE);
                        continueState(State.RESET);
                    }
                }
            }
            else
            {
                closeCurrentCallEvent(message.getTimestamp(), true, MacPduType.MAC_4_ACTIVE);
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
        if(mac instanceof DenyResponse dr)
        {
            broadcast(message, mac, DecodeEventType.RESPONSE, "DENY: " + dr.getDeniedServiceType() + " REASON:" +
                dr.getDenyReason() + " ADDL:" + dr.getAdditionalInfo());
        }
        else if(mac instanceof MotorolaDenyResponse dr)
        {
            broadcast(message, mac, DecodeEventType.RESPONSE, "DENY: " + dr.getDeniedServiceType() + " REASON:" +
                dr.getDenyReason() + (dr.hasAdditionalInformation() ? " ADDL:" + dr.getAdditionalInfo() : ""));
        }
    }

    /**
     * Processes the Extended Function Command
     */
    private void processExtendedFunctionCommand(MacMessage message, MacStructure mac)
    {
        if(mac instanceof ExtendedFunctionCommandAbbreviated efc)
        {
            broadcast(message, mac, DecodeEventType.COMMAND,
                    "EXTENDED FUNCTION: " + efc.getExtendedFunction() + " ARGUMENTS:" + efc.getArguments());
        }
        else if(mac instanceof ExtendedFunctionCommandExtendedVCH efce)
        {
            broadcast(message, mac, DecodeEventType.COMMAND,
                    "EXTENDED FUNCTION: " + efce.getExtendedFunction() + " ARGUMENTS:" + efce.getArguments());
        }
        else if(mac instanceof MotorolaGroupRegroupExtendedFunctionCommand efc)
        {
            if(efc.getExtendedFunction() == ExtendedFunction.GROUP_REGROUP_CREATE_SUPERGROUP)
            {
                broadcast(message, efc, DecodeEventType.COMMAND, "CREATE SUPER GROUP ADD RADIO");
            }
            else if(efc.getExtendedFunction() == ExtendedFunction.GROUP_REGROUP_CANCEL_SUPERGROUP)
            {
                broadcast(message, efc, DecodeEventType.COMMAND, "CANCEL SUPER GROUP FOR RADIO");
            }
            else
            {
                broadcast(message, efc, DecodeEventType.COMMAND, efc.toString());
            }
        }
    }

    /**
     * Identifier Update
     */
    private void processNetwork(MacMessage message, MacStructure mac)
    {
//TODO: send these to the network config monitor, also
//        case PHASE1_73_IDENTIFIER_UPDATE_TDMA_ABBREVIATED:
//        case PHASE1_74_IDENTIFIER_UPDATE_V_UHF:
//        case PHASE1_78_SYSTEM_SERVICE_BROADCAST:
//        case PHASE1_79_SECONDARY_CONTROL_CHANNEL_BROADCAST_IMPLICIT:
//        case PHASE1_7A_RFSS_STATUS_BROADCAST_IMPLICIT:
//        case PHASE1_7B_NETWORK_STATUS_BROADCAST_IMPLICIT:
//        case PHASE1_7C_ADJACENT_STATUS_BROADCAST_IMPLICIT:
//        case PHASE1_7D_IDENTIFIER_UPDATE:


    }

    /**
     * Roaming Address
     */
    private void processRoamingAddress(MacMessage message, MacStructure mac)
    {
        //TODO:
//        case PHASE1_76_ROAMING_ADDRESS_COMMAND:
//        case PHASE1_77_ROAMING_ADDRESS_UPDATE:
    }

    /**
     * Queued Response
     */
    private void processQueued(MacMessage message, MacStructure mac)
    {
//TODO: MOTOROLA_A6_QUEUED_RESPONSE
        if(mac instanceof QueuedResponse qr)
        {
            broadcast(message, mac, DecodeEventType.RESPONSE,
                    "QUEUED - " + qr.getQueuedResponseServiceType() +
                    " REASON:" + qr.getQueuedResponseReason() + " ADDL:" + qr.getAdditionalInfo());
        }
    }

    /**
     * Null Information and Null Avoid Zero Bias
     */
    private void processNullInformation(MacMessage message, MacStructure mac)
    {
        MacPduType type = message.getMacPduType();

        if(type == MacPduType.MAC_3_IDLE || type == MacPduType.MAC_6_HANGTIME)
        {
            closeCurrentCallEvent(message.getTimestamp(), true, type);
        }
    }

    /**
     * Unit monitor - places the radio into a traffic channel and forces it to broadcast for safety situations.
     */
    private void processRadioUnitMonitor(MacMessage message, MacStructure mac)
    {
//        PHASE1_5D_RADIO_UNIT_MONITOR_COMMAND_OBSOLETE
//        PHASE1_5E_RADIO_UNIT_MONITOR_ENHANCED_COMMAND_ABBREVIATED
        if(mac instanceof RadioUnitMonitorCommandAbbreviated rumc)
        {
            broadcast(message, mac, DecodeEventType.COMMAND,
                "RADIO UNIT MONITOR" + (rumc.isSilentMonitor() ? " (SILENT)" : "") +
                " TIME:" + rumc.getTransmitTime() + " MULTIPLIER:" + rumc.getTransmitMultiplier());
        }
        if(mac instanceof RadioUnitMonitorCommandExtendedVCH rumce)
        {
            broadcast(message, mac, DecodeEventType.COMMAND,
                    "RADIO UNIT MONITOR" + (rumce.isSilentMonitor() ? " (STEALTH)" : "") +
                            " TIME:" + rumce.getTransmitTime() + "MULTIPLIER:" + rumce.getTransmitMultiplier());
        }
    }

    /**
     * Processes GPS messages
     */
    private void processGPS(MacMessage message, MacStructure structure)
    {
        if(structure instanceof L3HarrisGpsLocation gps)
        {
            MutableIdentifierCollection collection = getUpdatedMutableIdentifierCollection(gps);

            broadcast(PlottableDecodeEvent.plottableBuilder(DecodeEventType.GPS, message.getTimestamp())
                    .protocol(Protocol.APCO25)
                    .location(gps.getGeoPosition())
                    .channel(getCurrentChannel())
                    .details(gps.getLocation().toString() + " " + new Date(gps.getTimestampMs()))
                    .identifiers(collection)
                    .build());
        }
    }

    /**
     * Location registration
     */
    private void processLocationRegistration(MacMessage message, MacStructure mac)
    {
//TODO: PHASE1_6B_LOCATION_REGISTRATION_RESPONSE

    }

    /**
     * MAC release.  If the channel is preempted by the controller, it can indicate that the channel is converting from
     * a traffic channel to a control channel.
     */
    private void processMacRelease(MacMessage message, MacStructure mac)
    {
        if(mac instanceof MacRelease mr)
        {
            closeCurrentCallEvent(message.getTimestamp(), true, message.getMacPduType());
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
        if(mac instanceof MessageUpdateAbbreviated mua)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.SDM, "MESSAGE UPDATE - " + mua.getShortDataMessage());
        }
        if(mac instanceof MessageUpdateExtendedVCH mue)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.SDM, "MESSAGE UPDATE - " + mue.getShortDataMessage());
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
     *
     * Individual paging means the controller wants the individual radio(s) to return to the control channel
     * from the current call.
     *
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
                    .identifiers(getIdentifierCollectionForUser(ip.getTargetAddress1()))
                    .timeslot(getTimeslot())
                    .build());

            if(ip.getCount() > 1)
            {
                boolean p2 = ip.isTalkgroupPriority2();
                broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                        .channel(getCurrentChannel())
                        .details(p2 ? "PRIORITY " : "" + "USER PAGE-RETURN TO CONTROL CHANNEL")
                        .identifiers(getIdentifierCollectionForUser(ip.getTargetAddress2()))
                        .timeslot(getTimeslot())
                        .build());

                if(ip.getCount() > 2)
                {
                    boolean p3 = ip.isTalkgroupPriority3();
                    broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                            .channel(getCurrentChannel())
                            .details(p3 ? "PRIORITY " : "" + "USER PAGE-RETURN TO CONTROL CHANNEL")
                            .identifiers(getIdentifierCollectionForUser(ip.getTargetAddress3()))
                            .timeslot(getTimeslot())
                            .build());

                    if(ip.getCount() > 3)
                    {
                        boolean p4 = ip.isTalkgroupPriority4();
                        broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                                .channel(getCurrentChannel())
                                .details(p4 ? "PRIORITY " : "" + "USER PAGE-RETURN TO CONTROL CHANNEL")
                                .identifiers(getIdentifierCollectionForUser(ip.getTargetAddress4()))
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
                    .identifiers(getIdentifierCollectionForUser(igp.getTargetGroup1()))
                    .timeslot(getTimeslot())
                    .build());

            if(igp.getCount() > 1)
            {
                broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                        .channel(getCurrentChannel())
                        .details("GROUP PAGE - TALKGROUP IS ACTIVE ON SITE")
                        .identifiers(getIdentifierCollectionForUser(igp.getTargetGroup2()))
                        .timeslot(getTimeslot())
                        .build());

                if(igp.getCount() > 2)
                {
                    broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                            .channel(getCurrentChannel())
                            .details("GROUP PAGE - TALKGROUP IS ACTIVE ON SITE")
                            .identifiers(getIdentifierCollectionForUser(igp.getTargetGroup3()))
                            .timeslot(getTimeslot())
                            .build());

                    if(igp.getCount() > 3)
                    {
                        broadcast(P25DecodeEvent.builder(DecodeEventType.PAGE, message.getTimestamp())
                                .channel(getCurrentChannel())
                                .details("GROUP PAGE - TALKGROUP IS ACTIVE ON SITE")
                                .identifiers(getIdentifierCollectionForUser(igp.getTargetGroup4()))
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
        broadcast(message, mac, getCurrentChannel(), DecodeEventType.STATUS, "STATUS QUERY");

        if(mac instanceof StatusUpdateAbbreviated sua)
        {
            broadcast(message, mac, DecodeEventType.STATUS,
                    "STATUS UPDATE - UNIT:" + sua.getUnitStatus() + " USER:" + sua.getUserStatus());
        }

        if(mac instanceof StatusQueryAbbreviated)
        {
            broadcast(message, mac, getCurrentChannel(), DecodeEventType.STATUS, "STATUS QUERY");
        }
        if(mac instanceof StatusUpdateExtendedVCH sue)
        {
            broadcast(message, mac, DecodeEventType.STATUS,
                    "STATUS UPDATE - UNIT:" + sue.getUnitStatus() + " USER:" + sue.getUserStatus());
        }
    }

    /**
     * Talker Alias
     */
    private void processTalkerAlias(MacMessage message, MacStructure mac)
    {
        if(mac instanceof L3HarrisTalkerAlias talkerAlias)
        {
            getIdentifierCollection().update(talkerAlias.getAlias());
        }
    }

    /**
     * Unit registration
     */
    private void processUnitRegistration(MacMessage message, MacStructure mac)
    {
        broadcast(message, mac, getCurrentChannel(), DecodeEventType.COMMAND, "UNIT REGISTRATION");
//TODO: PHASE1_6C_UNIT_REGISTRATION_RESPONSE_ABBREVIATED
//        case PHASE1_6D_UNIT_REGISTRATION_COMMAND_ABBREVIATED:
//        case PHASE1_6F_DEREGISTRATION_ACKNOWLEDGE:
    }


    /**
     * Creates and broadcasts a decode event.
     * @param message for the event
     * @param mac for the event
     * @param currentChannel for the event
     * @param eventType of event
     * @param details to populate for the event
     */
    private void broadcast(MacMessage message, MacStructure mac, IChannelDescriptor currentChannel, DecodeEventType eventType, String details)
    {
        MutableIdentifierCollection collection = getUpdatedMutableIdentifierCollection(mac);

        broadcast(P25DecodeEvent.builder(eventType, message.getTimestamp())
                .channel(currentChannel)
                .details(details)
                .identifiers(collection)
                .timeslot(getTimeslot())
                .build());
    }

    private void broadcast(MacMessage message, MacStructure structure, DecodeEventType eventType, String details)
    {
        MutableIdentifierCollection icQueuedResponse = getUpdatedMutableIdentifierCollection(structure);

        broadcast(P25DecodeEvent.builder(eventType, message.getTimestamp())
                .channel(getCurrentChannel())
                .details(details)
                .identifiers(icQueuedResponse)
                .timeslot(getTimeslot())
                .build());
    }

    private MutableIdentifierCollection getUpdatedMutableIdentifierCollection(MacStructure mac)
    {
        MutableIdentifierCollection icQueuedResponse = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        icQueuedResponse.remove(IdentifierClass.USER);
        icQueuedResponse.update(mac.getIdentifiers());
        return icQueuedResponse;
    }

    /**
     * Indicates if the identifier argument matches the current (TO) talkgroup for this channel and timeslot
     * @param identifier to match
     * @return true if the identifier matches the current channel's TO talkgroup
     */
    private boolean isCurrentGroup(Identifier<?> identifier)
    {
        if(identifier != null)
        {
            for(Identifier id: getIdentifierCollection().getIdentifiers(Role.TO))
            {
                if(identifier.equals(id))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Broadcasts a state continuation.  If we're currently in a call, then we broadcast a call continuation, otherwise
     * we broadcast a continuation of the specified state.
     * @param state to continue
     */
    private void continueState(State state)
    {
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, state, getTimeslot()));
    }


    /**
     * Updates or creates a current call event.
     *
     * @param type of call that will be used as an event description
     * @param details of the call (optional)
     * @param timestamp of the message indicating a call or continuation
     */
    private void updateCurrentCall(DecodeEventType type, String details, long timestamp)
    {
        if(mCurrentCallEvent == null)
        {
            if(type == null)
            {
                type = DecodeEventType.CALL;
            }

            mCurrentCallEvent = P25DecodeEvent.builder(type, timestamp)
                .channel(getCurrentChannel())
                .details(details)
                .identifiers(getIdentifierCollection().copyOf())
                    .timeslot(getTimeslot())
                .build();

            broadcast(mCurrentCallEvent);

            if(isEncrypted())
            {
                broadcast(new DecoderStateEvent(this, Event.START, State.ENCRYPTED, getTimeslot()));
            }
            else
            {
                broadcast(new DecoderStateEvent(this, Event.START, State.CALL, getTimeslot()));
            }
        }
        else
        {
            if(details != null)
            {
                mCurrentCallEvent.setDetails(details);
            }

            mCurrentCallEvent.setIdentifierCollection(getIdentifierCollection().copyOf());
            mCurrentCallEvent.end(timestamp);
            broadcast(mCurrentCallEvent);
        }
    }

    /**
     * Ends/closes the current call event.
     *
     * @param timestamp of the message that indicates the event has ended.
     * @param resetIdentifiers to reset the FROM/TO identifiers (true) or reset just the FROM identifiers (false)
     * @param pduType of the message that caused the close call event - to determine channel state after call
     */
    private void closeCurrentCallEvent(long timestamp, boolean resetIdentifiers, MacPduType pduType)
    {
        if(mCurrentCallEvent != null)
        {
            //Refresh the identifier collection before we close out the event
            mCurrentCallEvent.setIdentifierCollection(getIdentifierCollection().copyOf());
            mCurrentCallEvent.end(timestamp);
            broadcast(mCurrentCallEvent);
            mCurrentCallEvent = null;

            broadcast(new DecoderStateEvent(this, Event.END, getStateFromPduType(pduType), getTimeslot()));

            if(resetIdentifiers)
            {
                getIdentifierCollection().remove(IdentifierClass.USER);
            }
            else
            {
                //Only clear the from identifier at this point ... the channel may still be allocated to the TO talkgroup
                getIdentifierCollection().remove(IdentifierClass.USER, Role.FROM);
            }
        }
        else if(pduType == MacPduType.MAC_2_END_PTT)
        {
            broadcast(new DecoderStateEvent(this, Event.END, getStateFromPduType(pduType), getTimeslot()));
        }
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
        return mNetworkConfigurationMonitor.getActivitySummary();
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
                    LOGGER.info("Got the source frequency: " + frequency + " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                default:
                    break;
            }
        }
    }

    @Override
    public void start()
    {
        super.start();
        mPatchGroupManager.clear();

        //Change the default (45-second) traffic channel timeout to 1 second
        if(mChannelType == ChannelType.TRAFFIC)
        {
            broadcast(new ChangeChannelTimeoutEvent(this, ChannelType.TRAFFIC, 1000, getTimeslot()));
        }
    }

    @Override
    public void init()
    {
    }

    @Override
    public void stop()
    {
        super.stop();
        mPatchGroupManager.clear();
    }
}
