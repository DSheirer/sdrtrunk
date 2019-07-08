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
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.channel.state.ChangeChannelTimeoutEvent;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.encryption.EncryptionKey;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.p25.P25DecodeEvent;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.network.P25NetworkConfigurationMonitor;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.AcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.CallAlertExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.DenyResponse;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.EndPushToTalk;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.ExtendedFunctionCommandExtended;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.GroupAffiliationQueryExtended;
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
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserAbbreviated;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnitToUnitVoiceChannelUserExtended;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder state for an APCO25 Phase II channel.  Maintains the call/data/idle state of the channel and produces events
 * by monitoring the decoded message stream.
 *
 */
public class P25P2DecoderState extends DecoderState implements IChannelEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2DecoderState.class);

    private ChannelType mChannelType;
    private PatchGroupManager mPatchGroupManager = new PatchGroupManager();
    private P25NetworkConfigurationMonitor mNetworkConfigurationMonitor;
    private P25TrafficChannelManager mTrafficChannelManager;
    private Listener<ChannelEvent> mChannelEventListener;
    private DecodeEvent mCurrentCallEvent;
    private int mTimeslot;

    /**
     * Constructs an APCO-25 decoder state with an optional traffic channel manager.
     * @param channel with configuration details
     * @param trafficChannelManager for handling traffic channel grants.
     */
    public P25P2DecoderState(Channel channel, int timeslot, P25TrafficChannelManager trafficChannelManager)
    {
        mChannelType = channel.getChannelType();
        mTimeslot = timeslot;


        //        mNetworkConfigurationMonitor = new P25NetworkConfigurationMonitor(mModulation);
//
//        if(trafficChannelManager != null)
//        {
//            mTrafficChannelManager = trafficChannelManager;
//            mChannelEventListener = trafficChannelManager.getChannelEventListener();
//        }
//        else
//        {
//            mChannelEventListener = channelEvent -> {
//                //do nothing with channel events if we're not configured to process traffic channels
//            };
//        }
    }

    /**
     * Constructs an APCO-25 decoder state for a traffic channel.
     * @param channel with configuration details
     */
    public P25P2DecoderState(Channel channel, int timeslot)
    {
        this(channel, timeslot, null);
    }

    public int getTimeslot()
    {
        return mTimeslot;
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
     * Implements the IChannelEventListener interface to receive traffic channel teardown notifications so that the
     * traffic channel manager can manage traffic channel allocations.
     */
    @Override
    public Listener<ChannelEvent> getChannelEventListener()
    {
        return mChannelEventListener;
    }

    /**
     * Performs a full reset to prepare this object for reuse on a new channel
     */
    @Override
    public void reset()
    {
        resetState();
    }

/**
     * Resets any temporal state details
     */
    protected void resetState()
    {
        super.resetState();

        if(mCurrentCallEvent != null)
        {
            mCurrentCallEvent.end(System.currentTimeMillis());
            broadcast(mCurrentCallEvent);
            mCurrentCallEvent = null;
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
                processMacMessage((MacMessage)message);
            }
            else if(message instanceof AbstractVoiceTimeslot)
            {
                if(isEncrypted())
                {
                    broadcast(new DecoderStateEvent(this, Event.DECODE, State.ENCRYPTED, getTimeslot()));
                }
                else
                {
                    broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL, getTimeslot()));
                }
            }
            else if(message instanceof EncryptionSynchronizationSequence)
            {
                getIdentifierCollection().update(message.getIdentifiers());
            }
        }
    }

//    private void processMacMessage(MacMessage macMessage)
//    {
//        MacStructure mac = macMessage.getMacStructure();
//
//        switch(macMessage.getMacPduType())
//        {
//            case MAC_1_PTT:
//            case MAC_4_ACTIVE:
//                broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL, getTimeslot()));
//                processMacStructure(macMessage.getMacStructure(), macMessage.getTimestamp());
//                break;
//            case MAC_6_HANGTIME:
//                broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE, getTimeslot()));
//                processMacStructure(macMessage.getMacStructure(), macMessage.getTimestamp());
//                break;
//            case MAC_2_END_PTT:
//                broadcast(new DecoderStateEvent(this, Event.DECODE, State.IDLE, getTimeslot()));
//                processMacStructure(macMessage.getMacStructure(), macMessage.getTimestamp());
//            case MAC_3_IDLE:
//                broadcast(new DecoderStateEvent(this, Event.DECODE, State.IDLE, getTimeslot()));
//                break;
//            default:
//                mLog.info("Unrecognized MAC PDU Type [" + macMessage.getMacPduType() + "]");
//                break;
//        }
//    }
//
    private void processMacMessage(MacMessage message)
    {
        MacStructure mac = message.getMacStructure();

        switch((mac.getOpcode()))
        {
            case PUSH_TO_TALK:
                for(Identifier identifier : mac.getIdentifiers())
                {
                    //Add to the identifier collection after filtering through the patch group manager
                    getIdentifierCollection().update(mPatchGroupManager.update(identifier));
                }

                PushToTalk ptt = (PushToTalk)mac;

                if(ptt.isEncrypted())
                {
                    broadcast(new DecoderStateEvent(this, Event.DECODE, State.ENCRYPTED, getTimeslot()));
                    updateCurrentCall(DecodeEventType.CALL_ENCRYPTED, ptt.getEncryptionKey().toString(), message.getTimestamp());
                }
                else
                {
                    broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL, getTimeslot()));
                    updateCurrentCall(DecodeEventType.CALL, null, message.getTimestamp());
                }
                break;
            case END_PUSH_TO_TALK:
                if(mac instanceof EndPushToTalk)
                {
                    //Add the set of identifiers before we close out the call event to ensure they're captured in
                    //the closing event.
                    for(Identifier identifier : mac.getIdentifiers())
                    {
                        //Add to the identifier collection after filtering through the patch group manager
                        getIdentifierCollection().update(mPatchGroupManager.update(identifier));
                    }

                    closeCurrentCallEvent(message.getTimestamp());

                    getIdentifierCollection().remove(IdentifierClass.USER);

                    broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE, getTimeslot()));
                }
                break;
            case TDMA_1_GROUP_VOICE_CHANNEL_USER_ABBREVIATED:
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
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ENCRYPTED, getTimeslot()));
                        updateCurrentCall(DecodeEventType.CALL_GROUP_ENCRYPTED, null, message.getTimestamp());
                    }
                    else
                    {
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL, getTimeslot()));
                        updateCurrentCall(DecodeEventType.CALL_GROUP, null, message.getTimestamp());
                    }
                }
                break;
            case TDMA_33_GROUP_VOICE_CHANNEL_USER_EXTENDED:
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
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ENCRYPTED, getTimeslot()));
                        updateCurrentCall(DecodeEventType.CALL_GROUP_ENCRYPTED, null, message.getTimestamp());
                    }
                    else
                    {
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL, getTimeslot()));
                        updateCurrentCall(DecodeEventType.CALL_GROUP, null, message.getTimestamp());
                    }
                }
                break;
            case TDMA_2_UNIT_TO_UNIT_VOICE_CHANNEL_USER:
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
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ENCRYPTED, getTimeslot()));
                    }
                    else
                    {
                        updateCurrentCall(DecodeEventType.CALL_UNIT_TO_UNIT, null, message.getTimestamp());
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL, getTimeslot()));
                    }
                }
                break;
            case TDMA_34_UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
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
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ENCRYPTED, getTimeslot()));
                    }
                    else
                    {
                        updateCurrentCall(DecodeEventType.CALL_UNIT_TO_UNIT, null, message.getTimestamp());
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL, getTimeslot()));
                    }
                }
                break;
            case TDMA_3_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
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
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ENCRYPTED, getTimeslot()));
                    }
                    else
                    {
                        updateCurrentCall(DecodeEventType.CALL_INTERCONNECT, null, message.getTimestamp());
                        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CALL, getTimeslot()));
                    }
                }
                break;


            case TDMA_5_GROUP_VOICE_CHANNEL_GRANT_UPDATE_MULTIPLE:
            case TDMA_37_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
            case PHASE1_64_GROUP_VOICE_CHANNEL_GRANT_ABBREVIATED:
            case PHASE1_66_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
            case PHASE1_70_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_ABBREVIATED:
            case PHASE1_192_GROUP_VOICE_CHANNEL_GRANT_EXTENDED:
            case PHASE1_195_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
            case PHASE1_196_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_EXTENDED:
            case PHASE1_198_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE_EXTENDED:
                //Ignore - update on calls on this and other channels
                break;
            case TDMA_17_INDIRECT_GROUP_PAGING:
                MutableIdentifierCollection icGroupPaging = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icGroupPaging.remove(IdentifierClass.USER);
                icGroupPaging.update(mac.getIdentifiers());

                broadcast(P25DecodeEvent.builder(message.getTimestamp())
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.PAGE.toString())
                    .details("GROUP PAGE")
                    .identifiers(icGroupPaging)
                    .build());
                break;
            case TDMA_18_INDIVIDUAL_PAGING_MESSAGE_WITH_PRIORITY:
                if(mac instanceof IndividualPagingMessage)
                {
                    IndividualPagingMessage ipm = (IndividualPagingMessage)mac;
                    MutableIdentifierCollection icIndividualPaging = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icIndividualPaging.remove(IdentifierClass.USER);
                    icIndividualPaging.update(mac.getIdentifiers());
                    boolean priority = ipm.isTalkgroupPriority1() || ipm.isTalkgroupPriority2() ||
                        ipm.isTalkgroupPriority3() || ipm.isTalkgroupPriority4();

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details((priority ? "PRIORITY " : "") + "USER PAGE")
                        .identifiers(icIndividualPaging)
                        .build());
                }
                break;
            case TDMA_48_POWER_CONTROL_SIGNAL_QUALITY:
                if(mac instanceof PowerControlSignalQuality)
                {
                    PowerControlSignalQuality pcsq = (PowerControlSignalQuality)mac;

                    MutableIdentifierCollection icPowerControl = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icPowerControl.remove(IdentifierClass.USER);
                    icPowerControl.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("ADJUST TRANSMIT POWER - RF:" + pcsq.getRFLevel() + " BER:" + pcsq.getBitErrorRate())
                        .identifiers(icPowerControl)
                        .build());
                }
                break;
            case TDMA_49_MAC_RELEASE:
                if(mac instanceof MacRelease)
                {
                    MacRelease mr = (MacRelease)mac;

                    MutableIdentifierCollection icMacRelease = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icMacRelease.remove(IdentifierClass.USER);
                    icMacRelease.update(mac.getIdentifiers());

                    closeCurrentCallEvent(message.getTimestamp());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details((mr.isForcedPreemption() ? "FORCED " : "") + "CALL PREEMPTION" + (mr.isTalkerPreemption() ? " BY USER" : ""))
                        .identifiers(icMacRelease)
                        .build());
                }
                break;
            case PHASE1_65_GROUP_VOICE_SERVICE_REQUEST:
                if(mac instanceof GroupVoiceServiceRequest)
                {
                    GroupVoiceServiceRequest gvsr = (GroupVoiceServiceRequest)mac;

                    MutableIdentifierCollection icMacRelease = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icMacRelease.remove(IdentifierClass.USER);
                    icMacRelease.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("GROUP VOICE SERVICE " + gvsr.getServiceOptions())
                        .identifiers(icMacRelease)
                        .build());
                }
                break;
            case PHASE1_68_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_ABBREVIATED:
                if(mac instanceof UnitToUnitVoiceChannelGrantAbbreviated)
                {
                    UnitToUnitVoiceChannelGrantAbbreviated uuvcga = (UnitToUnitVoiceChannelGrantAbbreviated)mac;
                    MutableIdentifierCollection icGrant = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icGrant.remove(IdentifierClass.USER);
                    icGrant.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(uuvcga.getChannel())
                        .eventDescription(DecodeEventType.CALL_UNIT_TO_UNIT.toString())
                        .details("VOICE CHANNEL GRANT")
                        .identifiers(icGrant)
                        .build());
                }
                break;
            case PHASE1_69_UNIT_TO_UNIT_ANSWER_REQUEST_ABBREVIATED:
                if(mac instanceof UnitToUnitAnswerRequestAbbreviated)
                {
                    UnitToUnitAnswerRequestAbbreviated uuara = (UnitToUnitAnswerRequestAbbreviated)mac;
                    MutableIdentifierCollection icRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRequest.remove(IdentifierClass.USER);
                    icRequest.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("UNIT-TO-UNIT ANSWER REQUEST - " + uuara.getServiceOptions())
                        .identifiers(icRequest)
                        .build());
                }
                break;
            case PHASE1_74_TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                if(mac instanceof TelephoneInterconnectAnswerRequest)
                {
                    TelephoneInterconnectAnswerRequest tiar = (TelephoneInterconnectAnswerRequest)mac;
                    MutableIdentifierCollection icRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRequest.remove(IdentifierClass.USER);
                    icRequest.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("TELEPHONE INTERCONNECT ANSWER REQUEST")
                        .identifiers(icRequest)
                        .build());
                }
                break;
            case PHASE1_76_RADIO_UNIT_MONITOR_COMMAND_ABBREVIATED:
                if(mac instanceof RadioUnitMonitorCommand)
                {
                    RadioUnitMonitorCommand rumc = (RadioUnitMonitorCommand)mac;
                    MutableIdentifierCollection icRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRequest.remove(IdentifierClass.USER);
                    icRequest.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("RADIO UNIT MONITOR" + (rumc.isSilentMonitor() ? " (STEALTH)" : "") +
                            " TIME:" + rumc.getTransmitTime() + " MULTIPLIER:" + rumc.getTransmitMultiplier())
                        .identifiers(icRequest)
                        .build());
                }
                break;
            case PHASE1_84_SNDCP_DATA_CHANNEL_GRANT:
                if(mac instanceof SNDCPDataChannelGrant)
                {
                    SNDCPDataChannelGrant sdcg = (SNDCPDataChannelGrant)mac;
                    MutableIdentifierCollection icGrant = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icGrant.remove(IdentifierClass.USER);
                    icGrant.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(sdcg.getChannel())
                        .eventDescription(sdcg.getServiceOptions().isEncrypted() ? DecodeEventType.DATA_CALL_ENCRYPTED.toString() : DecodeEventType.DATA_CALL.toString())
                        .details("SNDCP CHANNEL GRANT " + sdcg.getServiceOptions())
                        .identifiers(icGrant)
                        .build());
                }
                break;
            case PHASE1_85_SNDCP_DATA_PAGE_REQUEST:
                if(mac instanceof SNDCPDataPageRequest)
                {
                    SNDCPDataPageRequest sdpr = (SNDCPDataPageRequest)mac;
                    MutableIdentifierCollection icPage = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icPage.remove(IdentifierClass.USER);
                    icPage.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("SNDCP DATA PAGE " + sdpr.getServiceOptions())
                        .identifiers(icPage)
                        .build());
                }
                break;
            case PHASE1_88_STATUS_UPDATE_ABBREVIATED:
                if(mac instanceof StatusUpdateAbbreviated)
                {
                    StatusUpdateAbbreviated sua = (StatusUpdateAbbreviated)mac;
                    MutableIdentifierCollection icStatusUpdate = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icStatusUpdate.remove(IdentifierClass.USER);
                    icStatusUpdate.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.STATUS.toString())
                        .details("STATUS UPDATE - UNIT:" + sua.getUnitStatus() + " USER:" + sua.getUserStatus())
                        .identifiers(icStatusUpdate)
                        .build());
                }
                break;
            case PHASE1_90_STATUS_QUERY_ABBREVIATED:
                if(mac instanceof StatusQueryAbbreviated)
                {
                    MutableIdentifierCollection icStatusUpdate = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icStatusUpdate.remove(IdentifierClass.USER);
                    icStatusUpdate.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.STATUS.toString())
                        .details("STATUS QUERY")
                        .identifiers(icStatusUpdate)
                        .build());
                }
                break;
            case PHASE1_92_MESSAGE_UPDATE_ABBREVIATED:
                if(mac instanceof MessageUpdateAbbreviated)
                {
                    MessageUpdateAbbreviated mua = (MessageUpdateAbbreviated)mac;
                    MutableIdentifierCollection icStatusUpdate = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icStatusUpdate.remove(IdentifierClass.USER);
                    icStatusUpdate.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.SDM.toString())
                        .details("MESSAGE UPDATE - " + mua.getShortDataMessage())
                        .identifiers(icStatusUpdate)
                        .build());
                }
                break;
            case PHASE1_94_RADIO_UNIT_MONITOR_COMMAND_ENHANCED:
                if(mac instanceof RadioUnitMonitorCommandEnhanced)
                {
                    RadioUnitMonitorCommandEnhanced rumc = (RadioUnitMonitorCommandEnhanced)mac;
                    MutableIdentifierCollection icRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRequest.remove(IdentifierClass.USER);
                    icRequest.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("RADIO UNIT MONITOR" + (rumc.isStealthMode() ? " (STEALTH)" : "") +
                            " ENCRYPTION:" + rumc.getEncryption() +
                            " TIME:" + rumc.getTransmitTime())
                        .identifiers(icRequest)
                        .build());
                }
                break;
            case PHASE1_95_CALL_ALERT_ABBREVIATED:
                MutableIdentifierCollection icCallAlert = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icCallAlert.remove(IdentifierClass.USER);
                icCallAlert.update(mac.getIdentifiers());

                broadcast(P25DecodeEvent.builder(message.getTimestamp())
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.CALL_ALERT.toString())
                    .identifiers(icCallAlert)
                    .build());
                break;
            case PHASE1_96_ACK_RESPONSE:
                if(mac instanceof AcknowledgeResponse)
                {
                    AcknowledgeResponse ar = (AcknowledgeResponse)mac;
                    MutableIdentifierCollection icAckResponse = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icAckResponse.remove(IdentifierClass.USER);
                    icAckResponse.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("ACKNOWLEDGE: " + ar.getServiceType())
                        .identifiers(icAckResponse)
                        .build());
                }
                break;
            case PHASE1_97_QUEUED_RESPONSE:
                if(mac instanceof QueuedResponse)
                {
                    QueuedResponse qr = (QueuedResponse)mac;

                    MutableIdentifierCollection icQueuedResponse = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icQueuedResponse.remove(IdentifierClass.USER);
                    icQueuedResponse.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("QUEUED - " + qr.getQueuedResponseServiceType() +
                            " REASON:" + qr.getQueuedResponseReason() + " ADDL:" + qr.getAdditionalInfo())
                        .identifiers(icQueuedResponse)
                        .build());
                }
                break;
            case PHASE1_100_EXTENDED_FUNCTION_COMMAND_ABBREVIATED:
                if(mac instanceof ExtendedFunctionCommand)
                {
                    ExtendedFunctionCommand efc = (ExtendedFunctionCommand)mac;

                    MutableIdentifierCollection icExtendedFunction = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icExtendedFunction.remove(IdentifierClass.USER);
                    icExtendedFunction.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("EXTENDED FUNCTION: " + efc.getExtendedFunction() + " ARGUMENTS:" + efc.getArguments())
                        .identifiers(icExtendedFunction)
                        .build());
                }
                break;
            case PHASE1_103_DENY_RESPONSE:
                if(mac instanceof DenyResponse)
                {
                    DenyResponse dr = (DenyResponse)mac;
                    MutableIdentifierCollection icDenyResponse = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icDenyResponse.remove(IdentifierClass.USER);
                    icDenyResponse.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("DENY: " + dr.getDeniedServiceType() + " REASON:" + dr.getDenyReason() + " ADDL:" + dr.getAdditionalInfo())
                        .identifiers(icDenyResponse)
                        .build());
                }
                break;
            case PHASE1_106_GROUP_AFFILIATION_QUERY_ABBREVIATED:
                MutableIdentifierCollection icGroupAffiliationQuery = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icGroupAffiliationQuery.remove(IdentifierClass.USER);
                icGroupAffiliationQuery.update(mac.getIdentifiers());

                broadcast(P25DecodeEvent.builder(message.getTimestamp())
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.QUERY.toString())
                    .details("GROUP AFFILIATION")
                    .identifiers(icGroupAffiliationQuery)
                    .build());
                break;
            case PHASE1_109_UNIT_REGISTRATION_COMMAND_ABBREVIATED:
                MutableIdentifierCollection icUnitRegisterCommand = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icUnitRegisterCommand.remove(IdentifierClass.USER);
                icUnitRegisterCommand.update(mac.getIdentifiers());

                broadcast(P25DecodeEvent.builder(message.getTimestamp())
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.COMMAND.toString())
                    .details("UNIT REGISTRATION")
                    .identifiers(icUnitRegisterCommand)
                    .build());
                break;
            case PHASE1_197_UNIT_TO_UNIT_ANSWER_REQUEST_EXTENDED:
                if(mac instanceof UnitToUnitAnswerRequestExtended)
                {
                    UnitToUnitAnswerRequestExtended uuare = (UnitToUnitAnswerRequestExtended)mac;

                    MutableIdentifierCollection icUnitAnswerRequestExtended =
                        new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icUnitAnswerRequestExtended.remove(IdentifierClass.USER);
                    icUnitAnswerRequestExtended.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("UNIT-TO-UNIT ANSWER REQUEST " + uuare.getServiceOptions())
                        .identifiers(icUnitAnswerRequestExtended)
                        .build());
                }
                break;
            case PHASE1_204_RADIO_UNIT_MONITOR_COMMAND_EXTENDED:
                if(mac instanceof RadioUnitMonitorCommandExtended)
                {
                    RadioUnitMonitorCommandExtended rumce = (RadioUnitMonitorCommandExtended)mac;
                    MutableIdentifierCollection icRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRequest.remove(IdentifierClass.USER);
                    icRequest.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("RADIO UNIT MONITOR" + (rumce.isSilentMonitor() ? " (STEALTH)" : "") +
                            " TIME:" + rumce.getTransmitTime() + "MULTIPLIER:" + rumce.getTransmitMultiplier())
                        .identifiers(icRequest)
                        .build());
                }
                break;
            case PHASE1_216_STATUS_UPDATE_EXTENDED:
                if(mac instanceof StatusUpdateExtended)
                {
                    StatusUpdateExtended sue = (StatusUpdateExtended)mac;
                    MutableIdentifierCollection icStatusUpdate = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icStatusUpdate.remove(IdentifierClass.USER);
                    icStatusUpdate.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.STATUS.toString())
                        .details("STATUS UPDATE - UNIT:" + sue.getUnitStatus() + " USER:" + sue.getUserStatus())
                        .identifiers(icStatusUpdate)
                        .build());
                }
                break;
            case PHASE1_218_STATUS_QUERY_EXTENDED:
                MutableIdentifierCollection icStatusQueryExtended = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icStatusQueryExtended.remove(IdentifierClass.USER);
                icStatusQueryExtended.update(mac.getIdentifiers());

                broadcast(P25DecodeEvent.builder(message.getTimestamp())
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.STATUS.toString())
                    .details("STATUS QUERY")
                    .identifiers(icStatusQueryExtended)
                    .build());
                break;
            case PHASE1_220_MESSAGE_UPDATE_EXTENDED:
                if(mac instanceof MessageUpdateExtended)
                {
                    MessageUpdateExtended mue = (MessageUpdateExtended)mac;
                    MutableIdentifierCollection icStatusUpdate = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icStatusUpdate.remove(IdentifierClass.USER);
                    icStatusUpdate.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.SDM.toString())
                        .details("MESSAGE UPDATE - " + mue.getShortDataMessage())
                        .identifiers(icStatusUpdate)
                        .build());
                }
                break;
            case PHASE1_223_CALL_ALERT_EXTENDED:
                if(mac instanceof CallAlertExtended)
                {
                    MutableIdentifierCollection icCallAlertExtended = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icCallAlertExtended.remove(IdentifierClass.USER);
                    icCallAlertExtended.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.CALL_ALERT.toString())
                        .identifiers(icCallAlertExtended)
                        .build());
                }
                break;
            case PHASE1_228_EXTENDED_FUNCTION_COMMAND_EXTENDED:
                if(mac instanceof ExtendedFunctionCommandExtended)
                {
                    ExtendedFunctionCommandExtended efce = (ExtendedFunctionCommandExtended)mac;

                    MutableIdentifierCollection icExtendedFunction = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icExtendedFunction.remove(IdentifierClass.USER);
                    icExtendedFunction.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("EXTENDED FUNCTION: " + efce.getExtendedFunction() + " ARGUMENTS:" + efce.getArguments())
                        .identifiers(icExtendedFunction)
                        .build());
                }
                break;
            case PHASE1_234_GROUP_AFFILIATION_QUERY_EXTENDED:
                if(mac instanceof GroupAffiliationQueryExtended)
                {
                    MutableIdentifierCollection icGroupAffiliationQueryExtended =
                        new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icGroupAffiliationQueryExtended.remove(IdentifierClass.USER);
                    icGroupAffiliationQueryExtended.update(mac.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("GROUP AFFILIATION")
                        .identifiers(icGroupAffiliationQueryExtended)
                        .build());
                }
                break;

            case PHASE1_115_IDENTIFIER_UPDATE_TDMA:
            case PHASE1_116_IDENTIFIER_UPDATE_V_UHF:
            case PHASE1_117_TIME_AND_DATE_ANNOUNCEMENT:
            case PHASE1_120_SYSTEM_SERVICE_BROADCAST:
            case PHASE1_121_SECONDARY_CONTROL_CHANNEL_BROADCAST:
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
            case UNKNOWN:
            default:
                //ignore
                break;
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
                .eventDescription(type.toString())
                .details(details)
                .identifiers(getIdentifierCollection().copyOf())
                .build();

            broadcast(mCurrentCallEvent);
            broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
        }
        else
        {
            if(type == DecodeEventType.CALL_ENCRYPTED)
            {
                mCurrentCallEvent.setEventDescription(type.toString());

                if(details != null)
                {
                    mCurrentCallEvent.setDetails(details);
                }
            }
            mCurrentCallEvent.setIdentifierCollection(getIdentifierCollection().copyOf());
            mCurrentCallEvent.end(timestamp);
            broadcast(mCurrentCallEvent);
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));
        }
    }

    /**
     * Ends/closes the current call event.
     *
     * @param timestamp of the message that indicates the event has ended.
     */
    private void closeCurrentCallEvent(long timestamp)
    {
        if(mCurrentCallEvent != null)
        {
            mCurrentCallEvent.end(timestamp);
            broadcast(mCurrentCallEvent);
            mCurrentCallEvent = null;

            //Only clear the from identifier at this point ... the channel may still be allocated to the TO talkgroup
            getIdentifierCollection().remove(IdentifierClass.USER, Role.FROM);
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
        switch(event.getEvent())
        {
            case RESET:
                resetState();
                mNetworkConfigurationMonitor.reset();
                break;
            default:
                break;
        }
    }

    @Override
    public void start()
    {
        //Change the default (45-second) traffic channel timeout to 1 second
        if(mChannelType == ChannelType.TRAFFIC)
        {
            broadcast(new ChangeChannelTimeoutEvent(this, ChannelType.TRAFFIC, 1000));
        }
    }

    @Override
    public void init()
    {
    }

    @Override
    public void stop()
    {
    }
}
