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
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.channel.state.ChangeChannelTimeoutEvent;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.IChannelEventListener;
import io.github.dsheirer.identifier.*;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceAMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceMessage;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.ars.ARSPacket;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.ip.udp.UDPPacket;
import io.github.dsheirer.module.decode.p25.P25DecodeEvent;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.P25Message;
import io.github.dsheirer.module.decode.p25.phase1.message.hdu.HDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.hdu.HeaderData;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.*;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.EncryptionSyncParameters;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.*;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.*;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp.SNDCPMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp.SNDCPPacketMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.umbtc.isp.UMBTCTelephoneInterconnectRequestExplicitDialing;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDULinkControlMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaDenyResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.PatchGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.PatchGroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.*;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.*;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder state for an APCO25 channel.  Maintains the call/data/idle state of the channel and produces events by
 * monitoring the decoded message stream.
 *
 */
public class DMRDecoderState extends DecoderState implements IChannelEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRDecoderState.class);

    private ChannelType mChannelType;
    private DMRDecoder.Modulation mModulation;
    private PatchGroupManager mPatchGroupManager = new PatchGroupManager();
    private DMRNetworkConfigurationMonitor mNetworkConfigurationMonitor;
    private Listener<ChannelEvent> mChannelEventListener;
    private DMRTrafficChannelManager mTrafficChannelManager;
    private DecodeEvent mCurrentCallEvent;

    /**
     * Constructs an APCO-25 decoder state with an optional traffic channel manager.
     * @param channel with configuration details
     * @param trafficChannelManager for handling traffic channel grants.
     */
    public DMRDecoderState(Channel channel, DMRTrafficChannelManager trafficChannelManager)
    {
        mChannelType = channel.getChannelType();
        mModulation = ((DecodeConfigDMR)channel.getDecodeConfiguration()).getModulation();
        mTrafficChannelManager = trafficChannelManager;
        mNetworkConfigurationMonitor = new DMRNetworkConfigurationMonitor(mModulation);
    }

    /**
     * Constructs an APCO-25 decoder state for a traffic channel.
     * @param channel with configuration details
     */
    public DMRDecoderState(Channel channel)
    {
        this(channel, null);
    }

    /**
     * Modulation type for the decoder
     */
    public DMRDecoder.Modulation getModulation()
    {
        return mModulation;
    }

    /**
     * Identifies the decoder type
     */
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR;
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
        super.reset();
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
    public void receive(IMessage iMessage)
    {
        if(iMessage instanceof DataMessage)
        {
            DataMessage message = (DataMessage)iMessage;
            if(message instanceof CSBKMessage) {
                CSBKMessage csbk = (CSBKMessage)message;
                if(csbk.hasLCNChange() > 0) {
                    if(mTrafficChannelManager != null)
                    {
                        mTrafficChannelManager.processChannelGrant(csbk.hasLCNChange());
                    }
                }
            }
            broadcast(new DecoderStateEvent(this, Event.DECODE, State.CONTROL));
        } else if(iMessage instanceof VoiceAMessage) {
            VoiceMessage vm = (VoiceMessage)iMessage;
            processVoiceA(vm);
        }
    }

    /**
     * Processes a Header Data Unit message and starts a new call event.
     */
    private void processVoiceA(VoiceMessage message)
    {
        if(message.isValid())
        {
            //HeaderData headerData = message.getHeaderData();

            closeCurrentCallEvent(message.getTimestamp());

            updateCurrentCall(DecodeEventType.CALL, null, message.getTimestamp());

            return;
        }

        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE));
    }


    /**
     * Processes an LDU voice message and forwards Link Control and/or Encryption Sync Parameters for
     * additional processing.
     *
     * @param message that is an instance of an LDU1 or LDU2 message
     */
    private void processLDU(P25Message message)
    {
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));

        if(message instanceof LDU1Message)
        {
            LinkControlWord lcw = ((LDU1Message)message).getLinkControlWord();

            if(lcw != null && lcw.isValid())
            {
                processLinkControl(lcw, message.getTimestamp());
            }

            updateCurrentCall(DecodeEventType.CALL, null, message.getTimestamp());
        }
        else if(message instanceof LDU2Message)
        {
            EncryptionSyncParameters esp = ((LDU2Message)message).getEncryptionSyncParameters();

            if(esp != null && esp.isValid())
            {
                processEncryptionSyncParameters(esp, message.getTimestamp());
            }

            updateCurrentCall(DecodeEventType.CALL, null, message.getTimestamp());
        }
    }

    /**
     * Processes a Terminator Data Unit with Link Control (TDULC) message and forwards valid
     * Link Control Word messages for additional processing.
     *
     * @param message that is an instance of a TDULC
     */
    private void processTDULC(P25Message message)
    {
        closeCurrentCallEvent(message.getTimestamp());
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE));

        if(message instanceof TDULinkControlMessage)
        {
            LinkControlWord lcw = ((TDULinkControlMessage)message).getLinkControlWord();

            if(lcw != null && lcw.isValid())
            {
                processLinkControl(lcw, message.getTimestamp());
            }
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
                mCurrentCallEvent.setDetails(details);
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
     * Terminator Data Unit (TDU).
     */
    private void processTDU(P25Message message)
    {
        closeCurrentCallEvent(message.getTimestamp());
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE));
    }

    /**
     * Packet Data Unit
     *
     * @param message
     */
    private void processPDU(P25Message message)
    {
        if(message.isValid() && message instanceof PDUMessage)
        {
            PDUMessage pdu = (PDUMessage)message;

            MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
            ic.remove(IdentifierClass.USER);
            ic.update(pdu.getIdentifiers());

            broadcast(P25DecodeEvent.builder(message.getTimestamp())
                .channel(getCurrentChannel())
                .eventDescription(DecodeEventType.DATA_PACKET.toString())
                .details(pdu.toString())
                .identifiers(ic)
                .build());

        }

        broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));
    }

    /**
     * Unconfirmed Multi-Block Trunking Control (UMBTC)
     *
     * @param message
     */
    private void processUMBTC(P25Message message)
    {
        if(message.isValid() && message instanceof UMBTCTelephoneInterconnectRequestExplicitDialing)
        {
            UMBTCTelephoneInterconnectRequestExplicitDialing tired = (UMBTCTelephoneInterconnectRequestExplicitDialing)message;

            MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
            ic.remove(IdentifierClass.USER);
            ic.update(tired.getIdentifiers());

            broadcast(P25DecodeEvent.builder(tired.getTimestamp())
                .channel(getCurrentChannel())
                .eventDescription(DecodeEventType.REQUEST.toString())
                .details("TELEPHONE INTERCONNECT:" + tired.getTelephoneNumber())
                .identifiers(ic)
                .build());
        }

        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CONTROL));
    }

    /**
     * IP Packet Data
     *
     * @param message
     */
    private void processPacketData(P25Message message)
    {
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));

        if(message instanceof SNDCPPacketMessage)
        {
            SNDCPPacketMessage sndcp = (SNDCPPacketMessage)message;
            getIdentifierCollection().update(sndcp.getIdentifiers());
        }
        else if(message instanceof PacketMessage)
        {
            PacketMessage packetMessage = (PacketMessage)message;
            getIdentifierCollection().remove(IdentifierClass.USER);
            getIdentifierCollection().update(packetMessage.getIdentifiers());

            IPacket packet = packetMessage.getPacket();

            if(packet instanceof IPV4Packet)
            {
                IPV4Packet ipv4 = (IPV4Packet)packet;

                IPacket ipPayload = ipv4.getPayload();

                if(ipPayload instanceof UDPPacket)
                {
                    UDPPacket udpPacket = (UDPPacket)ipPayload;

                    IPacket udpPayload = udpPacket.getPayload();

                    if(udpPayload instanceof ARSPacket)
                    {
                        ARSPacket arsPacket = (ARSPacket)udpPayload;

                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());

                        DecodeEvent packetEvent = P25DecodeEvent.builder(message.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.AUTOMATIC_REGISTRATION_SERVICE.toString())
                            .identifiers(ic)
                            .details(arsPacket.toString() + " " + ipv4.toString())
                            .build();

                        broadcast(packetEvent);
                    }
                    else
                    {
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());

                        DecodeEvent packetEvent = P25DecodeEvent.builder(message.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.UDP_PACKET.toString())
                            .identifiers(ic)
                            .details(ipv4.toString())
                            .build();

                        broadcast(packetEvent);
                    }
                }
            }
        }
    }

    /**
     * Sub-Network Dependent Convergence Protocol (SNDCP)
     *
     * @param message
     */
    private void processSNDCP(P25Message message)
    {
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));

        if(message.isValid() && message instanceof SNDCPPacketMessage)
        {
            SNDCPPacketMessage sndcpPacket = (SNDCPPacketMessage)message;

            SNDCPMessage sndcpMessage = sndcpPacket.getSNDCPMessage();

            MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
            ic.remove(IdentifierClass.USER);
            ic.update(sndcpPacket.getIdentifiers());

            switch(sndcpPacket.getSNDCPPacketHeader().getPDUType())
            {
                case OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_ACCEPT:
                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("SNDCP ACTIVATE TDS CONTEXT ACCEPT")
                        .identifiers(ic)
                        .build());
                    break;
                case OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT:
                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("SNDCP DEACTIVATE TDS CONTEXT ACCEPT")
                        .identifiers(ic)
                        .build());
                    break;
                case OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST:
                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("SNDCP DEACTIVATE TDS CONTEXT")
                        .identifiers(ic)
                        .build());
                    break;
                case OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REJECT:
                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("SNDCP ACTIVATE TDS CONTEXT REJECT")
                        .identifiers(ic)
                        .build());
                    break;
                case INBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REQUEST:
                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("SNDCP ACTIVATE TDS CONTEXT")
                        .identifiers(ic)
                        .build());
                    break;
                case INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT:
                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("SNDCP DEACTIVATE TDS CONTEXT ACCEPT")
                        .identifiers(ic)
                        .build());
                    break;
                case INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST:
                    broadcast(P25DecodeEvent.builder(message.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("SNDCP DEACTIVATE TDS CONTEXT")
                        .identifiers(ic)
                        .build());
                    break;
            }
        }
    }

    /**
     * Processes encryption sync parameters carried by an LDU2 message
     *
     * @param esp that is non-null and valid
     */
    private void processEncryptionSyncParameters(EncryptionSyncParameters esp, long timestamp)
    {
        if(esp.isEncryptedAudio())
        {
            for(Identifier identifier : esp.getIdentifiers())
            {
                //Add to the identifier collection after filtering through the patch group manager
                getIdentifierCollection().update(mPatchGroupManager.update(identifier));
            }
            Encryption encryption = Encryption.fromValue(esp.getEncryptionKey().getValue().getAlgorithm());
            updateCurrentCall(DecodeEventType.CALL_ENCRYPTED, "ALGORITHM:" + encryption.toString(), timestamp);
        }
        else
        {
            getIdentifierCollection().remove(Form.ENCRYPTION_KEY);
            updateCurrentCall(DecodeEventType.CALL, null, timestamp);
        }
    }

    /**
     * Processes a Link Control Word (LCW) that is carried by either an LDU1 or a TDULC message.
     *
     * @param lcw that is non-null and valid
     */
    private void processLinkControl(LinkControlWord lcw, long timestamp)
    {
        switch(lcw.getOpcode())
        {
            //Calls in-progress on this channel
            case GROUP_VOICE_CHANNEL_USER:
            case MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_USER:
            case MOTOROLA_TALK_COMPLETE:
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                for(Identifier identifier : lcw.getIdentifiers())
                {
                    //Add to the identifier collection after filtering through the patch group manager
                    getIdentifierCollection().update(mPatchGroupManager.update(identifier));
                }
                break;

            //Call termination
            case CALL_TERMINATION_OR_CANCELLATION:
                //Note: we only broadcast an END state if this is a network-commanded channel teardown
                if(lcw instanceof LCCallTermination && ((LCCallTermination)lcw).isNetworkCommandedTeardown())
                {
                    broadcast(new DecoderStateEvent(this, Event.END, State.FADE));
                }
                break;

            //Calls in-progress on another channel
            case GROUP_VOICE_CHANNEL_UPDATE:
            case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                break;

            //Network configuration messages
            case ADJACENT_SITE_STATUS_BROADCAST:
            case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
            case NETWORK_STATUS_BROADCAST:
            case NETWORK_STATUS_BROADCAST_EXPLICIT:
            case PROTECTION_PARAMETER_BROADCAST:
            case RFSS_STATUS_BROADCAST:
            case RFSS_STATUS_BROADCAST_EXPLICIT:
            case SECONDARY_CONTROL_CHANNEL_BROADCAST:
            case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
            case SYSTEM_SERVICE_BROADCAST:
                mNetworkConfigurationMonitor.process(lcw);
                break;

            //Patch Group management
            case MOTOROLA_PATCH_GROUP_ADD:
                mPatchGroupManager.addPatchGroups(lcw.getIdentifiers());
                break;
            case MOTOROLA_PATCH_GROUP_DELETE:
                mPatchGroupManager.removePatchGroups(lcw.getIdentifiers());
                break;
            case MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_UPDATE:
                mPatchGroupManager.addPatchGroups(lcw.getIdentifiers());
                break;

            //Other events
            case CALL_ALERT:
                MutableIdentifierCollection icCallAlert = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icCallAlert.remove(IdentifierClass.USER);
                icCallAlert.update(lcw.getIdentifiers());

                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.PAGE.toString())
                    .details("Call Alert")
                    .identifiers(icCallAlert)
                    .build());
                break;
            case EXTENDED_FUNCTION_COMMAND:
                if(lcw instanceof LCExtendedFunctionCommand)
                {
                    LCExtendedFunctionCommand efc = (LCExtendedFunctionCommand)lcw;

                    MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    ic.remove(IdentifierClass.USER);
                    ic.update(lcw.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(timestamp)
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("Extended Function: " + efc.getExtendedFunction() +
                            " Arguments:" + efc.getExtendedFunctionArguments())
                        .identifiers(ic)
                        .build());
                }
                break;
            case GROUP_AFFILIATION_QUERY:
                MutableIdentifierCollection icGroupAffiliation = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icGroupAffiliation.remove(IdentifierClass.USER);
                icGroupAffiliation.update(lcw.getIdentifiers());

                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.QUERY.toString())
                    .details("Group Affiliation")
                    .identifiers(icGroupAffiliation)
                    .build());
                break;
            case MESSAGE_UPDATE:
                if(lcw instanceof LCMessageUpdate)
                {
                    MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    ic.remove(IdentifierClass.USER);
                    ic.update(lcw.getIdentifiers());

                    LCMessageUpdate mu = (LCMessageUpdate)lcw;
                    broadcast(P25DecodeEvent.builder(timestamp)
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.SDM.toString())
                        .details("MSG:" + mu.getShortDataMessage())
                        .identifiers(ic)
                        .build());
                }
                break;
            case STATUS_QUERY:
                MutableIdentifierCollection icStatusQuery = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icStatusQuery.remove(IdentifierClass.USER);
                icStatusQuery.update(lcw.getIdentifiers());

                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.QUERY.toString())
                    .details("Status")
                    .identifiers(icStatusQuery)
                    .build());
                break;
            case STATUS_UPDATE:
                if(lcw instanceof LCStatusUpdate)
                {
                    LCStatusUpdate su = (LCStatusUpdate)lcw;

                    MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    ic.remove(IdentifierClass.USER);
                    ic.update(lcw.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(timestamp)
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.STATUS.toString())
                        .details("UNIT:" + su.getUnitStatus() + " USER:" + su.getUserStatus())
                        .identifiers(ic)
                        .build());
                }
                break;
            case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                if(lcw instanceof LCTelephoneInterconnectAnswerRequest)
                {
                    LCTelephoneInterconnectAnswerRequest tiar = (LCTelephoneInterconnectAnswerRequest)lcw;

                    MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    ic.remove(IdentifierClass.USER);
                    ic.update(lcw.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(timestamp)
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("Telephone Call:" + tiar.getTelephoneNumber())
                        .identifiers(ic)
                        .build());
                }
                break;
            case UNIT_AUTHENTICATION_COMMAND:
                MutableIdentifierCollection icAuthenticationCommand = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icAuthenticationCommand.remove(IdentifierClass.USER);
                icAuthenticationCommand.update(lcw.getIdentifiers());

                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.COMMAND.toString())
                    .details("Authenticate Unit")
                    .identifiers(icAuthenticationCommand)
                    .build());
                break;
            case UNIT_REGISTRATION_COMMAND:
                MutableIdentifierCollection icUnitRegistrationCommand = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icUnitRegistrationCommand.remove(IdentifierClass.USER);
                icUnitRegistrationCommand.update(lcw.getIdentifiers());

                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.COMMAND.toString())
                    .details("Unit Registration")
                    .identifiers(icUnitRegistrationCommand)
                    .build());
                break;
            case UNIT_TO_UNIT_ANSWER_REQUEST:
                MutableIdentifierCollection icUnitAnswerRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                icUnitAnswerRequest.remove(IdentifierClass.USER);
                icUnitAnswerRequest.update(lcw.getIdentifiers());

                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.PAGE.toString())
                    .details("Unit-to-Unit Answer Request")
                    .identifiers(icUnitAnswerRequest)
                    .build());
                break;
            default:
//                mLog.debug("Unrecognized LCW Opcode: " + lcw.getOpcode().name() + " VENDOR:" + lcw.getVendor() +
//                    " OPCODE:" + lcw.getOpcodeNumber());
                break;
        }
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
