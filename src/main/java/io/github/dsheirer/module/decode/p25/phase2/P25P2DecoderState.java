/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.p25.P25DecodeEvent;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacPduType;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.CallAlertExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.DenyResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.EndPushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupVoiceChannelGrantAbbreviated;
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
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PowerControlSignalQuality;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.PushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.QueuedResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandEnhanced;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.RadioUnitMonitorCommandExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.SNDCPDataPageRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusQueryAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.StatusUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.TelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitAnswerRequestExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelGrantUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder state for an APCO25 Phase II channel.  Maintains the call/data/idle state of the channel and produces events
 * by monitoring the decoded message stream.
 *
 */
public class P25P2DecoderState extends TimeslotDecoderState implements IdentifierUpdateListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2DecoderState.class);
    private static int SYSTEM_CONTROLLER = 0xFFFFFF;
    private ChannelType mChannelType;
    private PatchGroupManager mPatchGroupManager = new PatchGroupManager();
    private P25P2NetworkConfigurationMonitor mNetworkConfigurationMonitor = new P25P2NetworkConfigurationMonitor();
    private DecodeEvent mCurrentCallEvent;
    private int mEndPttOnFacchCounter = 0;

    /**
     * Constructs an APCO-25 decoder state for a traffic channel.
     * @param channel with configuration details
     */
    public P25P2DecoderState(Channel channel, int timeslot)
    {
        super(timeslot);
        mChannelType = channel.getChannelType();
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
            if(message instanceof MacMessage)
            {
                MacMessage macMessage = (MacMessage)message;
                processMacMessage(macMessage);

                MacPduType macPduType = macMessage.getMacPduType();

                //Ignore End PTT - this is handled in the processMacMessage() method
                if(macPduType != MacPduType.MAC_2_END_PTT)
                {
                    mEndPttOnFacchCounter = 0;
                    continueState(getStateFromPduType(macPduType));
                }

                if(macPduType == MacPduType.MAC_3_IDLE)
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
            case MAC_1_PTT:
                return State.CALL;
            case MAC_2_END_PTT:
                return State.TEARDOWN;
            case MAC_4_ACTIVE:
            case MAC_6_HANGTIME:
                return State.ACTIVE;
            case MAC_3_IDLE:
            default:
                return State.IDLE;
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

    private void processMacMessage(MacMessage message)
    {
        mNetworkConfigurationMonitor.processMacMessage(message);

        MacStructure mac = message.getMacStructure();

        switch((mac.getOpcode()))
        {
            case PUSH_TO_TALK:
                processPushToTalk(message, mac);
                break;
            case END_PUSH_TO_TALK:
                processEndPushToTalk(message, mac);
                break;
            case TDMA_0_NULL_INFORMATION_MESSAGE:
                MacPduType type = message.getMacPduType();

                if(type == MacPduType.MAC_3_IDLE || type == MacPduType.MAC_6_HANGTIME)
                {
                    closeCurrentCallEvent(message.getTimestamp(), true, type);
                }
                break;
            case TDMA_1_GROUP_VOICE_CHANNEL_USER_ABBREVIATED:
                processGVCUAbbreviated(message, mac);
                break;
            case TDMA_33_GROUP_VOICE_CHANNEL_USER_EXTENDED:
                processGVCUExtended(message, mac);
                break;
            case TDMA_2_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                processUTUVCU(message, mac);
                break;
            case TDMA_34_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
                processUTUVCUExtended(message, mac);
                break;
            case TDMA_3_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                processTIVCU(message, mac);
                break;
            case TDMA_5_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE:
                processGVCGUM(mac);
                break;
            case TDMA_37_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE_EXPLICIT:
                processGVCGUMExplicit(mac);
                break;
            case PHASE1_64_GROUP_VOICE_CHANNEL_GRANT_ABBREVIATED:
                processGVCGAbbreviated(mac);
                break;
            case PHASE1_66_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                processGVCGUpdate(mac);
                break;
            case PHASE1_70_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED:
                processUTUVCGUAbbreviated(mac);
                break;
            case PHASE1_195_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                processGVCGUExplicit(mac);
                break;
            case PHASE1_198_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED:
                processUTUVCGUExtended(mac);
                break;
            case TDMA_17_INDIRECT_GROUP_PAGING:
                broadcast(message, mac, getCurrentChannel(), DecodeEventType.PAGE, "GROUP PAGE");
                break;
            case TDMA_18_INDIVIDUAL_PAGING_MESSAGE_WITH_PRIORITY:
                processIPMWP(message, mac);
                break;
            case TDMA_48_POWER_CONTROL_SIGNAL_QUALITY:
                processPCSQ(message, mac);
                break;
            case TDMA_49_MAC_RELEASE:
                processMacRelease(message, mac);
                break;
            case PHASE1_65_GROUP_VOICE_SERVICE_REQUEST:
                if(mac instanceof GroupVoiceServiceRequest)
                {
                    GroupVoiceServiceRequest gvsr = (GroupVoiceServiceRequest)mac;

                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST, "GROUP VOICE SERVICE " + gvsr.getServiceOptions());
                }
                break;
            case PHASE1_68_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_ABBREVIATED:
                if(mac instanceof UnitToUnitVoiceChannelGrantAbbreviated)
                {
                    UnitToUnitVoiceChannelGrantAbbreviated uuvcga = (UnitToUnitVoiceChannelGrantAbbreviated)mac;
                    broadcast(message, mac, uuvcga.getChannel(), DecodeEventType.CALL_UNIT_TO_UNIT, "VOICE CHANNEL GRANT");
                }
                break;
            case PHASE1_69_UNIT_TO_UNIT_ANSWER_REQUEST_ABBREVIATED:
                if(mac instanceof UnitToUnitAnswerRequestAbbreviated)
                {
                    UnitToUnitAnswerRequestAbbreviated uuara = (UnitToUnitAnswerRequestAbbreviated)mac;
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST, "UNIT-TO-UNIT ANSWER REQUEST - " + uuara.getServiceOptions());
                }
                break;
            case PHASE1_74_TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                if(mac instanceof TelephoneInterconnectAnswerRequest)
                {
                    TelephoneInterconnectAnswerRequest tiar = (TelephoneInterconnectAnswerRequest)mac;
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST, "TELEPHONE INTERCONNECT ANSWER REQUEST");
                }
                break;
            case PHASE1_76_RADIO_UNIT_MONITOR_COMMAND_ABBREVIATED:
                processRUMCA(message, mac);
                break;
            case PHASE1_84_SNDCP_DATA_CHANNEL_GRANT:
                processSDCG(message, mac);
                break;
            case PHASE1_85_SNDCP_DATA_PAGE_REQUEST:
                if(mac instanceof SNDCPDataPageRequest)
                {
                    SNDCPDataPageRequest sdpr = (SNDCPDataPageRequest)mac;
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.PAGE, "SNDCP DATA PAGE " + sdpr.getServiceOptions());
                }
                break;
            case PHASE1_88_STATUS_UPDATE_ABBREVIATED:
                processSUA(message, mac);
                break;
            case PHASE1_90_STATUS_QUERY_ABBREVIATED:
                if(mac instanceof StatusQueryAbbreviated)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.STATUS, "STATUS QUERY");
                }
                break;
            case PHASE1_92_MESSAGE_UPDATE_ABBREVIATED:
                if(mac instanceof MessageUpdateAbbreviated)
                {
                    MessageUpdateAbbreviated mua = (MessageUpdateAbbreviated)mac;
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.SDM, "MESSAGE UPDATE - " + mua.getShortDataMessage());
                }
                break;
            case PHASE1_94_RADIO_UNIT_MONITOR_COMMAND_ENHANCED:
                processRUMCE(message, mac);
                break;
            case PHASE1_95_CALL_ALERT_ABBREVIATED:
                processCallAlert(message, mac);
                break;
            case PHASE1_96_ACK_RESPONSE:
                if(mac instanceof AcknowledgeResponse)
                {
                    AcknowledgeResponse ar = (AcknowledgeResponse)mac;
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.RESPONSE, "ACKNOWLEDGE: " + ar.getServiceType());
                }
                break;
            case PHASE1_97_QUEUED_RESPONSE:
                processQueuedResponse(message, mac);
                break;
            case PHASE1_100_EXTENDED_FUNCTION_COMMAND_ABBREVIATED:
                processEFCA(message, mac);
                break;
            case PHASE1_103_DENY_RESPONSE:
                processDenyResponse(message, mac);
                break;
            case PHASE1_106_GROUP_AFFILIATION_QUERY_ABBREVIATED:
                broadcast(message, mac, getCurrentChannel(), DecodeEventType.QUERY, "GROUP AFFILIATION");
                break;
            case PHASE1_109_UNIT_REGISTRATION_COMMAND_ABBREVIATED:
                broadcast(message, mac, getCurrentChannel(), DecodeEventType.COMMAND, "UNIT REGISTRATION");
                break;
            case PHASE1_197_UNIT_TO_UNIT_ANSWER_REQUEST_EXTENDED:
                if(mac instanceof UnitToUnitAnswerRequestExtended)
                {
                    UnitToUnitAnswerRequestExtended uuare = (UnitToUnitAnswerRequestExtended)mac;

                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.REQUEST, "UNIT-TO-UNIT ANSWER REQUEST " + uuare.getServiceOptions());
                }
                break;
            case PHASE1_204_RADIO_UNIT_MONITOR_COMMAND_EXTENDED:
                processRUMCExtended(message, mac);
                break;
            case PHASE1_216_STATUS_UPDATE_EXTENDED:
                processSUE(message, mac);
                break;
            case PHASE1_218_STATUS_QUERY_EXTENDED:
                broadcast(message, mac, getCurrentChannel(), DecodeEventType.STATUS, "STATUS QUERY");
                break;
            case PHASE1_220_MESSAGE_UPDATE_EXTENDED:
                if(mac instanceof MessageUpdateExtended)
                {
                    MessageUpdateExtended mue = (MessageUpdateExtended)mac;
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.SDM, "MESSAGE UPDATE - " + mue.getShortDataMessage());
                }
                break;
            case PHASE1_223_CALL_ALERT_EXTENDED:
                if(mac instanceof CallAlertExtended)
                {
                    processCallAlert(message, mac);
                }
                break;
            case PHASE1_228_EXTENDED_FUNCTION_COMMAND_EXTENDED:
                processEFCE(message, mac);
                break;
            case PHASE1_234_GROUP_AFFILIATION_QUERY_EXTENDED:
                if(mac instanceof GroupAffiliationQueryExtended)
                {
                    broadcast(message, mac, getCurrentChannel(), DecodeEventType.QUERY, "GROUP AFFILIATION");
                }
                break;

            case PHASE1_115_IDENTIFIER_UPDATE_TDMA:
            case PHASE1_116_IDENTIFIER_UPDATE_V_UHF:
            case PHASE1_117_TIME_AND_DATE_ANNOUNCEMENT:
            case PHASE1_120_SYSTEM_SERVICE_BROADCAST:
            case PHASE1_121_SECONDARY_CONTROL_CHANNEL_BROADCAST_ABBREVIATED:
            case PHASE1_122_RFSS_STATUS_BROADCAST_ABBREVIATED:
            case PHASE1_123_NETWORK_STATUS_BROADCAST_ABBREVIATED:
            case PHASE1_124_ADJACENT_STATUS_BROADCAST_ABBREVIATED:
            case PHASE1_125_IDENTIFIER_UPDATE:
            case PHASE1_PARTITION_1_UNKNOWN_OPCODE:
            case VENDOR_PARTITION_2_UNKNOWN_OPCODE:
            case PHASE1_214_SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
            case PHASE1_233_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
            case PHASE1_250_RFSS_STATUS_BROADCAST_EXTENDED:
            case PHASE1_251_NETWORK_STATUS_BROADCAST_EXTENDED:
            case PHASE1_252_ADJACENT_STATUS_BROADCAST_EXTENDED:
            case PHASE1_EXTENDED_PARTITION_3_UNKNOWN_OPCODE:
            case TDMA_PARTITION_0_UNKNOWN_OPCODE:
            case OBSOLETE_PHASE1_93_RADIO_UNIT_MONITOR_COMMAND:
            case PHASE1_192_GROUP_VOICE_CHANNEL_GRANT_EXTENDED:
            case PHASE1_196_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_EXTENDED:
            case UNKNOWN:
            default:
                break;
        }
    }

    private void processQueuedResponse(MacMessage message, MacStructure mac) {
        if(mac instanceof QueuedResponse)
        {
            QueuedResponse qr = (QueuedResponse)mac;
            broadcast(message, mac, DecodeEventType.RESPONSE,
                    "QUEUED - " + qr.getQueuedResponseServiceType() +
                    " REASON:" + qr.getQueuedResponseReason() + " ADDL:" + qr.getAdditionalInfo());
        }
    }


    private void processEFCA(MacMessage message, MacStructure mac) {
        if(mac instanceof ExtendedFunctionCommand)
        {
            ExtendedFunctionCommand efc = (ExtendedFunctionCommand)mac;
            broadcast(message, mac, DecodeEventType.COMMAND,
                    "EXTENDED FUNCTION: " + efc.getExtendedFunction() + " ARGUMENTS:" + efc.getArguments());
        }
    }

    private void processDenyResponse(MacMessage message, MacStructure mac) {
        if(mac instanceof DenyResponse)
        {
            DenyResponse dr = (DenyResponse)mac;
            broadcast(message, mac, DecodeEventType.RESPONSE,
                "DENY: " + dr.getDeniedServiceType() + " REASON:" + dr.getDenyReason() + " ADDL:" + dr.getAdditionalInfo());
        }
    }

    private void processRUMCExtended(MacMessage message, MacStructure mac) {
        if(mac instanceof RadioUnitMonitorCommandExtended)
        {
            RadioUnitMonitorCommandExtended rumce = (RadioUnitMonitorCommandExtended)mac;
            broadcast(message, mac, DecodeEventType.COMMAND,
                "RADIO UNIT MONITOR" + (rumce.isSilentMonitor() ? " (STEALTH)" : "") +
                    " TIME:" + rumce.getTransmitTime() + "MULTIPLIER:" + rumce.getTransmitMultiplier());
        }
    }

    private void processSUE(MacMessage message, MacStructure mac) {
        if(mac instanceof StatusUpdateExtended)
        {
            StatusUpdateExtended sue = (StatusUpdateExtended)mac;
            broadcast(message, mac, DecodeEventType.STATUS,
                "STATUS UPDATE - UNIT:" + sue.getUnitStatus() + " USER:" + sue.getUserStatus());
        }
    }

    private void processEFCE(MacMessage message, MacStructure mac) {
        if(mac instanceof ExtendedFunctionCommandExtended)
        {
            ExtendedFunctionCommandExtended efce = (ExtendedFunctionCommandExtended)mac;
            broadcast(message, mac, DecodeEventType.COMMAND,
                "EXTENDED FUNCTION: " + efce.getExtendedFunction() + " ARGUMENTS:" + efce.getArguments());
        }
    }

    private void processCallAlert(MacMessage message, MacStructure mac) {
        broadcast(message, mac, DecodeEventType.CALL_ALERT, null);
    }

    private void processRUMCE(MacMessage message, MacStructure mac) {
        if(mac instanceof RadioUnitMonitorCommandEnhanced)
        {
            RadioUnitMonitorCommandEnhanced rumc = (RadioUnitMonitorCommandEnhanced)mac;
            broadcast(message, mac, DecodeEventType.COMMAND,
                "RADIO UNIT MONITOR" + (rumc.isStealthMode() ? " (STEALTH)" : "") +
                    " ENCRYPTION:" + rumc.getEncryption() +
                    " TIME:" + rumc.getTransmitTime());
        }
    }

    private void processSUA(MacMessage message, MacStructure mac) {
        if(mac instanceof StatusUpdateAbbreviated)
        {
            StatusUpdateAbbreviated sua = (StatusUpdateAbbreviated)mac;
            broadcast(message, mac, DecodeEventType.STATUS,
                "STATUS UPDATE - UNIT:" + sua.getUnitStatus() + " USER:" + sua.getUserStatus());
        }
    }

    private void processSDCG(MacMessage message, MacStructure mac) {
        if(mac instanceof SNDCPDataChannelGrant)
        {
            SNDCPDataChannelGrant sdcg = (SNDCPDataChannelGrant)mac;
            broadcast(message, mac,
                sdcg.getServiceOptions().isEncrypted() ? DecodeEventType.DATA_CALL_ENCRYPTED : DecodeEventType.DATA_CALL,
                "SNDCP CHANNEL GRANT " + sdcg.getServiceOptions());
        }
    }

    private void processRUMCA(MacMessage message, MacStructure mac) {
        if(mac instanceof RadioUnitMonitorCommand)
        {
            RadioUnitMonitorCommand rumc = (RadioUnitMonitorCommand)mac;
            broadcast(message, mac, DecodeEventType.COMMAND,
                "RADIO UNIT MONITOR" + (rumc.isSilentMonitor() ? " (STEALTH)" : "") +
                " TIME:" + rumc.getTransmitTime() + " MULTIPLIER:" + rumc.getTransmitMultiplier());
        }
    }

    private void processMacRelease(MacMessage message, MacStructure mac) {
        if(mac instanceof MacRelease)
        {
            MacRelease mr = (MacRelease)mac;
            closeCurrentCallEvent(message.getTimestamp(), true, message.getMacPduType());
            broadcast(message, mac, DecodeEventType.COMMAND,
                (mr.isForcedPreemption() ? "FORCED " : "") + "CALL PREEMPTION" + (mr.isTalkerPreemption() ? " BY USER" : ""));
        }
    }

    private void processPCSQ(MacMessage message, MacStructure mac) {
        if(mac instanceof PowerControlSignalQuality)
        {
            PowerControlSignalQuality pcsq = (PowerControlSignalQuality)mac;
            broadcast(message, mac, DecodeEventType.COMMAND,
                "ADJUST TRANSMIT POWER - RF:" + pcsq.getRFLevel() + " BER:" + pcsq.getBitErrorRate());
        }
    }

    private void processIPMWP(MacMessage message, MacStructure mac) {
        if(mac instanceof IndividualPagingMessage)
        {
            IndividualPagingMessage ipm = (IndividualPagingMessage)mac;
            boolean priority = ipm.isTalkgroupPriority1() || ipm.isTalkgroupPriority2() ||
                ipm.isTalkgroupPriority3() || ipm.isTalkgroupPriority4();
            broadcast(message, mac, DecodeEventType.PAGE,
                (priority ? "PRIORITY " : "") + "USER PAGE");
        }
    }

    private void broadcast(MacMessage message, MacStructure mac, IChannelDescriptor currentChannel, DecodeEventType eventType, String details) {
        MutableIdentifierCollection collection = getUpdatedMutableIdentifierCollection(mac);

        broadcast(P25DecodeEvent.builder(message.getTimestamp())
                .channel(currentChannel)
                .eventType(eventType)
                .eventDescription(eventType.toString())
                .details(details)
                .identifiers(collection)
                .build());
    }

    private void broadcast(MacMessage message, MacStructure structure, DecodeEventType eventType, String details) {
        MutableIdentifierCollection icQueuedResponse = getUpdatedMutableIdentifierCollection(structure);

        broadcast(P25DecodeEvent.builder(message.getTimestamp())
                .channel(getCurrentChannel())
                .eventType(eventType)
                .eventDescription(eventType.toString())
                .details(details)
                .identifiers(icQueuedResponse)
                .build());
    }

    private MutableIdentifierCollection getUpdatedMutableIdentifierCollection(MacStructure mac) {
        MutableIdentifierCollection icQueuedResponse = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        icQueuedResponse.remove(IdentifierClass.USER);
        icQueuedResponse.update(mac.getIdentifiers());
        return icQueuedResponse;
    }

    private void processUTUVCGUExtended(MacStructure mac) {
        if(getCurrentChannel() == null && mac instanceof UnitToUnitVoiceChannelGrantUpdateExtended)
        {
            UnitToUnitVoiceChannelGrantUpdateExtended uuvcgue = (UnitToUnitVoiceChannelGrantUpdateExtended)mac;

            if(isCurrentGroup(uuvcgue.getTargetAddress()))
            {
                broadcastCurrentChannel(uuvcgue.getChannel());
            }
        }
    }

    private void processGVCGUExplicit(MacStructure mac) {
        if(getCurrentChannel() == null && mac instanceof GroupVoiceChannelGrantUpdateExplicit)
        {
            GroupVoiceChannelGrantUpdateExplicit gvcgue = (GroupVoiceChannelGrantUpdateExplicit)mac;

            if(isCurrentGroup(gvcgue.getGroupAddress()))
            {
                broadcastCurrentChannel(gvcgue.getChannel());
            }
        }
    }

    private void processUTUVCGUAbbreviated(MacStructure mac) {
        if(getCurrentChannel() == null && mac instanceof UnitToUnitVoiceChannelGrantAbbreviated)
        {
            UnitToUnitVoiceChannelGrantAbbreviated uuvcga = (UnitToUnitVoiceChannelGrantAbbreviated)mac;

            if(isCurrentGroup(uuvcga.getSourceAddress()) || isCurrentGroup(uuvcga.getTargetAddress()))
            {
                broadcastCurrentChannel(uuvcga.getChannel());
            }
        }
    }

    private void processGVCGUpdate(MacStructure mac) {
        if(getCurrentChannel() == null && mac instanceof GroupVoiceChannelGrantUpdate)
        {
            GroupVoiceChannelGrantUpdate gvcgu = (GroupVoiceChannelGrantUpdate)mac;

            if(isCurrentGroup(gvcgu.getGroupAddressA()))
            {
                broadcastCurrentChannel(gvcgu.getChannelA());
            }

            if(getCurrentChannel() == null && isCurrentGroup(gvcgu.getGroupAddressB()))
            {
                broadcastCurrentChannel(gvcgu.getChannelB());
            }
        }
    }

    private void processGVCGAbbreviated(MacStructure mac) {
        if(getCurrentChannel() == null && mac instanceof GroupVoiceChannelGrantAbbreviated)
        {
            GroupVoiceChannelGrantAbbreviated gvcga = (GroupVoiceChannelGrantAbbreviated)mac;

            if(isCurrentGroup(gvcga.getGroupAddress()))
            {
                broadcastCurrentChannel(gvcga.getChannel());
            }
        }
    }

    private void processGVCGUMExplicit(MacStructure mac) {
        if(getCurrentChannel() == null && mac instanceof GroupVoiceChannelGrantUpdateMultipleExplicit)
        {
            GroupVoiceChannelGrantUpdateMultipleExplicit gvcgume = (GroupVoiceChannelGrantUpdateMultipleExplicit)mac;

            if(isCurrentGroup(gvcgume.getGroupAddressA()))
            {
                broadcastCurrentChannel(gvcgume.getChannelA());
            }

            if(getCurrentChannel() == null && isCurrentGroup(gvcgume.getGroupAddressB()))
            {
                broadcastCurrentChannel(gvcgume.getChannelB());
            }
        }
    }

    private void processGVCGUM(MacStructure mac) {
        if(getCurrentChannel() == null && mac instanceof GroupVoiceChannelGrantUpdateMultiple)
        {
            GroupVoiceChannelGrantUpdateMultiple gvcgum = (GroupVoiceChannelGrantUpdateMultiple)mac;

            if(isCurrentGroup(gvcgum.getGroupAddressA()))
            {
                broadcastCurrentChannel(gvcgum.getChannelA());
            }

            if(getCurrentChannel() == null && gvcgum.hasGroupB() && isCurrentGroup(gvcgum.getGroupAddressB()))
            {
                broadcastCurrentChannel(gvcgum.getChannelB());
            }

            if(getCurrentChannel() == null && gvcgum.hasGroupC() && isCurrentGroup(gvcgum.getGroupAddressC()))
            {
                broadcastCurrentChannel(gvcgum.getChannelC());
            }
        }
    }

    private void processTIVCU(MacMessage message, MacStructure mac) {
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

            if(mac instanceof TelephoneInterconnectVoiceChannelUser)
            {
                TelephoneInterconnectVoiceChannelUser tivcu = (TelephoneInterconnectVoiceChannelUser)mac;

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
    }

    private void processUTUVCUExtended(MacMessage message, MacStructure mac) {
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

            if(mac instanceof UnitToUnitVoiceChannelUserExtended)
            {
                UnitToUnitVoiceChannelUserExtended uuvcue = (UnitToUnitVoiceChannelUserExtended)mac;

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

    private void processUTUVCU(MacMessage message, MacStructure mac) {
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
    }

    private void processGVCUExtended(MacMessage message, MacStructure mac) {
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

            if(mac instanceof GroupVoiceChannelUserExtended)
            {
                GroupVoiceChannelUserExtended gvcue = (GroupVoiceChannelUserExtended)mac;

                if(gvcue.getServiceOptions().isEncrypted())
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP_ENCRYPTED, null, message.getTimestamp());
                }
                else
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP, null, message.getTimestamp());
                }
            }
        }
    }

    private void processGVCUAbbreviated(MacMessage message, MacStructure mac) {
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

            if(mac instanceof GroupVoiceChannelUserAbbreviated)
            {
                GroupVoiceChannelUserAbbreviated gvcua = (GroupVoiceChannelUserAbbreviated)mac;

                if(gvcua.getServiceOptions().isEncrypted())
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP_ENCRYPTED, null, message.getTimestamp());
                }
                else
                {
                    updateCurrentCall(DecodeEventType.CALL_GROUP, null, message.getTimestamp());
                }
            }
        }
    }

    private void processEndPushToTalk(MacMessage message, MacStructure mac) {
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

    private void processPushToTalk(MacMessage message, MacStructure mac) {
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
        if(mCurrentCallEvent != null)
        {
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL, getTimeslot()));
        }
        else
        {
            broadcast(new DecoderStateEvent(this, Event.DECODE, state, getTimeslot()));
        }
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
            mCurrentCallEvent = P25DecodeEvent.builder(timestamp)
                .channel(getCurrentChannel())
                .eventDescription(type != null ? type.toString() : DecodeEventType.CALL.toString())
                .details(details)
                .identifiers(getIdentifierCollection().copyOf())
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
            if(type != null)
            {
                mCurrentCallEvent.setEventDescription(type.toString());
            }

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
     * @param resetIdentifiers to reset the FROM/TO identifiers
     * @param pduType of the message that caused the close call event - to determine channel state after call
     */
    private void closeCurrentCallEvent(long timestamp, boolean resetIdentifiers, MacPduType pduType)
    {
        if(mCurrentCallEvent != null)
        {
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
                default:
                    break;
            }
        }
    }

    @Override
    public void start()
    {
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
        mPatchGroupManager.clear();
    }
}
