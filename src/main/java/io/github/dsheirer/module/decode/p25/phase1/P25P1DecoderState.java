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
package io.github.dsheirer.module.decode.p25.phase1;

import com.google.common.eventbus.Subscribe;
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
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.identifier.decoder.DecoderLogicalChannelNameIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupIdentifier;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.identifier.patch.PatchGroupPreLoadDataContent;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.UnknownPacket;
import io.github.dsheirer.module.decode.ip.cellocator.MCGPPacket;
import io.github.dsheirer.module.decode.ip.icmp.ICMPPacket;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.ip.mototrbo.ars.ARSPacket;
import io.github.dsheirer.module.decode.ip.mototrbo.lrrp.LRRPPacket;
import io.github.dsheirer.module.decode.ip.udp.UDPPacket;
import io.github.dsheirer.module.decode.p25.IServiceOptionsProvider;
import io.github.dsheirer.module.decode.p25.P25DecodeEvent;
import io.github.dsheirer.module.decode.p25.P25TrafficChannelManager;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.hdu.HDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.hdu.HeaderData;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlOpcode;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisReturnToControlChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaEmergencyAlarmActivation;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaGroupRegroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaTalkComplete;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaUnitGPS;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCCallTermination;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCExtendedFunctionCommandExtended;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCMessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCMessageUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCNetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCRFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCRFSSStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCTelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.EncryptionSyncParameters;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUSequenceMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCAuthenticationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCIndividualDataServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCLocationRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCMessageUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCUnitAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCUnitToUnitAnswerResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCUnitToUnitVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCIndividualDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCMessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCMotorolaGroupRegroupChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCProtectionParameterBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCRFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCStatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCTelephoneInterconnectChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCTelephoneInterconnectChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitToUnitVoiceServiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitToUnitVoiceServiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.PacketMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.packet.sndcp.SNDCPPacketMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.response.ResponseMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.umbtc.isp.UMBTCTelephoneInterconnectRequestExplicitDialing;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDULinkControlMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.harris.osp.L3HarrisGroupRegroupExplicitEncryptionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaDenyResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaGroupRegroupChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaGroupRegroupChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp.MotorolaQueuedResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.CancelServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.ExtendedFunctionResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.GroupAffiliationQueryResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.GroupDataServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.GroupVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.IndividualDataServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.LocationRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.MessageUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.SNDCPDataChannelRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.SNDCPDataPageResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.SNDCPReconnectRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.StatusQueryRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.StatusQueryResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.StatusUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.TelephoneInterconnectAnswerResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitToUnitVoiceServiceAnswerResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.isp.UnitToUnitVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.AcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.DenyResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.LocationRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.MessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.QueuedResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RoamingAddressCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.StatusQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.StatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitToUnitVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitToUnitVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.PacketUtil;
import java.util.Collections;
import java.util.List;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder state for an APCO25 channel.  Maintains the call/data/idle state of the channel and produces events by
 * monitoring the decoded message stream.
 */
public class P25P1DecoderState extends DecoderState implements IChannelEventListener
{
    private static final Logger mLog = LoggerFactory.getLogger(P25P1DecoderState.class);
    private static final LoggingSuppressor LOGGING_SUPPRESSOR = new LoggingSuppressor(mLog);
    private final Channel mChannel;
    private final P25P1Decoder.Modulation mModulation;
    private final PatchGroupManager mPatchGroupManager = new PatchGroupManager();
    private final P25P1NetworkConfigurationMonitor mNetworkConfigurationMonitor;
    private final Listener<ChannelEvent> mChannelEventListener;
    private P25TrafficChannelManager mTrafficChannelManager;

    /**
     * Constructs an APCO-25 decoder state with an optional traffic channel manager.
     * @param channel with configuration details
     * @param trafficChannelManager for handling traffic channel grants.
     */
    public P25P1DecoderState(Channel channel, P25TrafficChannelManager trafficChannelManager)
    {
        mChannel = channel;
        mModulation = ((DecodeConfigP25Phase1)channel.getDecodeConfiguration()).getModulation();
        mNetworkConfigurationMonitor = new P25P1NetworkConfigurationMonitor(mModulation);

        if(trafficChannelManager != null)
        {
            mTrafficChannelManager = trafficChannelManager;
            mChannelEventListener = trafficChannelManager.getChannelEventListener();
        }
        else
        {
            mTrafficChannelManager = new P25TrafficChannelManager(channel);
            //Do nothing with channel events if we're not configured to process traffic channels
            mChannelEventListener = channelEvent -> {};
        }
    }

    /**
     * Constructs an APCO-25 decoder state for a traffic channel.
     * @param channel with configuration details
     */
    public P25P1DecoderState(Channel channel)
    {
        this(channel, null);
    }

    /**
     * Modulation type for the decoder
     */
    public P25P1Decoder.Modulation getModulation()
    {
        return mModulation;
    }

    /**
     * Identifies the decoder type
     */
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE1;
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
                mPatchGroupManager.addPatchGroup(patchGroupIdentifier, preLoadDataContent.getTimestamp());
            }
        }
    }

    /**
     * Primary message processing method.
     */
    @Override
    public void receive(IMessage iMessage)
    {
        if(iMessage instanceof P25P1Message message)
        {
            getIdentifierCollection().update(message.getNAC());

            switch(message.getDUID())
            {
                case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
                    processAMBTC(message);
                    break;
                case HEADER_DATA_UNIT:
                    processHDU(message);
                    break;
                case IP_PACKET_DATA:
                    processPacketData(message);
                    break;
                case LOGICAL_LINK_DATA_UNIT_1:
                case LOGICAL_LINK_DATA_UNIT_2:
                    processLDU(message);
                    break;
                case PACKET_DATA_UNIT:
                    processPDU(message);
                    break;
                case SUBNETWORK_DEPENDENT_CONVERGENCE_PROTOCOL:
                    processSNDCP(message);
                    break;
                case TERMINATOR_DATA_UNIT:
                    processTDU(message);
                    break;
                case TERMINATOR_DATA_UNIT_LINK_CONTROL:
                    processTDULC(message);
                    break;
                case TRUNKING_SIGNALING_BLOCK_1:
                case TRUNKING_SIGNALING_BLOCK_2:
                case TRUNKING_SIGNALING_BLOCK_3:
                    processTSBK(message);
                    break;
                case UNCONFIRMED_MULTI_BLOCK_TRUNKING_CONTROL:
                    processUMBTC(message);
                    break;
                case UNKNOWN:
                    break;
            }
        }
    }

    /**
     * Commands the traffic channel manager to process a traffic channel grant and allocate a decoder to process the
     * traffic channel.
     * @param apco25Channel to allocate
     * @param serviceOptions for the channel
     * @param identifiers to add to the current identifier collection
     * @param opcode that identifies the type of channel grant
     * @param timestamp when the channel grant occurred.
     */
    private void processChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                     List<Identifier> identifiers, Opcode opcode, long timestamp)
    {
        if(apco25Channel.getValue().getDownlinkFrequency() > 0)
        {
            MutableIdentifierCollection mic = getMutableIdentifierCollection(identifiers, timestamp);
            mTrafficChannelManager.processP1ChannelGrant(apco25Channel, serviceOptions, mic, opcode, timestamp);
        }
    }

    /**
     * Process an update for another channel and send it to the traffic channel manager.
     * @param channel where the call activity is happening.
     * @param serviceOptions for the call, optional null.
     * @param identifiers involved in the call
     * @param opcode for the update
     * @param timestamp of the message
     */
    private void processChannelUpdate(APCO25Channel channel, ServiceOptions serviceOptions, List<Identifier> identifiers,
                                      Opcode opcode, long timestamp)
    {
        MutableIdentifierCollection mic = getMutableIdentifierCollection(identifiers, timestamp);
        mTrafficChannelManager.processP1ChannelUpdate(channel, serviceOptions, mic, opcode, timestamp);
    }

    /**
     * Creates a decode event type from the link control word
     */
    private DecodeEventType getLCDecodeEventType(LinkControlWord lcw)
    {
        boolean encrypted = lcw.isEncrypted();;

        switch(lcw.getOpcode())
        {
            case GROUP_VOICE_CHANNEL_USER:
                return encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
            case MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_USER:
                return encrypted ? DecodeEventType.CALL_PATCH_GROUP_ENCRYPTED : DecodeEventType.CALL_PATCH_GROUP;
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                return encrypted ? DecodeEventType.CALL_INTERCONNECT_ENCRYPTED : DecodeEventType.CALL_INTERCONNECT;
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
                return encrypted ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED : DecodeEventType.CALL_UNIT_TO_UNIT;
            default:
                return encrypted ? DecodeEventType.CALL_ENCRYPTED : DecodeEventType.CALL;
        }
    }

    /**
     * Link Control (LC) Channel user (ie current user on this channel).
     */
    private void processLCChannelUser(LinkControlWord lcw, long timestamp)
    {
        List<Identifier> updated = mPatchGroupManager.update(lcw.getIdentifiers(), timestamp);
        getIdentifierCollection().update(updated);
        DecodeEventType decodeEventType = getLCDecodeEventType(lcw);


        ServiceOptions serviceOptions = null;

        if(lcw instanceof IServiceOptionsProvider sop)
        {
            serviceOptions = sop.getServiceOptions();
        }
        else if(lcw.isEncrypted())
        {
            serviceOptions = VoiceServiceOptions.createEncrypted();
        }
        else
        {
            serviceOptions = VoiceServiceOptions.createUnencrypted();
        }

        mTrafficChannelManager.processP1CurrentUser(getCurrentFrequency(), getCurrentChannel(), decodeEventType,
                serviceOptions, getIdentifierCollection(), timestamp, null );

        if(serviceOptions.isEncrypted())
        {
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ENCRYPTED));
        }
        else
        {
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));
        }
    }

    /**
     * Broadcasts an event from the AMBTC message
     * @param ambtcMessage with identifiers
     * @param decodeEventType for the event
     * @param details to add to the event
     */
    private void broadcastEvent(AMBTCMessage ambtcMessage, DecodeEventType decodeEventType, String details)
    {
        broadcastEvent(ambtcMessage.getIdentifiers(), ambtcMessage.getTimestamp(), decodeEventType, details);
    }

    /**
     * Broadcasts an event from the TSBK message
     * @param tsbkMessage with identifiers
     * @param decodeEventType for the event
     * @param details to add to the event
     */
    private void broadcastEvent(TSBKMessage tsbkMessage, DecodeEventType decodeEventType, String details)
    {
        broadcastEvent(tsbkMessage.getIdentifiers(), tsbkMessage.getTimestamp(), decodeEventType, details);
    }

    /**
     * Broadcasts the arguments as a new decode event
     * @param identifiers involved in the event
     * @param timestamp of the message/event
     * @param decodeEventType of the event
     * @param details for the event
     */
    private void broadcastEvent(List<Identifier> identifiers, long timestamp, DecodeEventType decodeEventType, String details)
    {
        MutableIdentifierCollection mic = getMutableIdentifierCollection(identifiers, timestamp);

        broadcast(P25DecodeEvent.builder(decodeEventType, timestamp)
                .channel(getCurrentChannel())
                .details(details)
                .identifiers(mic)
                .build());
    }

    /**
     * Creates a copy of the current identifier collection, removes any USER identifiers and adds the argument identifiers
     * passing each identifier through the patch group manager to replace with a patch group if it exists
     * @param identifiers to add to the collection copy
     * @param timestamp to check for freshness of patch group info.
     * @return collection
     */
    private MutableIdentifierCollection getMutableIdentifierCollection(List<Identifier> identifiers, long timestamp)
    {
        MutableIdentifierCollection requestCollection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        requestCollection.remove(IdentifierClass.USER);

        for(Identifier identifier: identifiers)
        {
            requestCollection.update(mPatchGroupManager.update(identifier, timestamp));
        }

        return requestCollection;
    }

    /**
     * Creates a copy of the current identifier collection, removes any USER identifiers and adds the argument identifier
     * passed through the patch group manager to replace with a patch group if it exists
     * @param identifier to add to the collection copy
     * @param timestamp to check for freshness of patch group info.
     * @return collection
     */
    private MutableIdentifierCollection getMutableIdentifierCollection(Identifier identifier, long timestamp)
    {
        return getMutableIdentifierCollection(Collections.singletonList(identifier), timestamp);
    }

    /**
     * Alternate Multi-Block Trunking Control (AMBTC)
     *
     * @param message
     */
    private void processAMBTC(P25P1Message message)
    {
        if(message.isValid() && message instanceof AMBTCMessage ambtc)
        {
            switch(ambtc.getHeader().getOpcode())
            {
                case ISP_AUTHENTICATION_RESPONSE:
                    if(ambtc instanceof AMBTCAuthenticationResponse ar)
                    {
                        broadcastEvent(ambtc, DecodeEventType.RESPONSE, "AUTHENTICATION:" +
                                ar.getAuthenticationValue());
                    }
                    break;
                case ISP_CALL_ALERT_REQUEST:
                    broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REQUEST,
                            "CALL ALERT");
                    break;
                case ISP_GROUP_AFFILIATION_REQUEST:
                    broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REQUEST,
                            "GROUP AFFILIATION");
                    break;
                case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                    if(ambtc instanceof AMBTCIndividualDataServiceRequest idsr)
                    {
                        broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REQUEST,
                                "INDIVIDUAL DATA SERVICE " + idsr.getDataServiceOptions());
                    }
                    break;
                case ISP_LOCATION_REGISTRATION_REQUEST:
                    if(ambtc instanceof AMBTCLocationRegistrationRequest lrr)
                    {
                        broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REQUEST,
                                "LOCATION REGISTRATION - UNIQUE ID:" + lrr.getSourceId());
                    }
                    break;
                case ISP_MESSAGE_UPDATE_REQUEST:
                    if(ambtc instanceof AMBTCMessageUpdateRequest mur)
                    {
                        broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.SDM,
                                "MESSAGE:" + mur.getShortDataMessage());
                    }
                    break;
                case ISP_ROAMING_ADDRESS_REQUEST:
                    broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REQUEST,
                            "ROAMING ADDRESS");
                    break;
                case ISP_STATUS_QUERY_REQUEST:
                case ISP_STATUS_QUERY_RESPONSE:
                case ISP_STATUS_UPDATE_REQUEST:
                    processAMBTCStatus(ambtc);
                    break;
                case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                    if(ambtc instanceof AMBTCUnitAcknowledgeResponse uar)
                    {
                        broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.RESPONSE,
                                "ACKNOWLEDGE:" + uar.getAcknowledgedService());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                    if(ambtc instanceof AMBTCUnitToUnitVoiceServiceRequest uuvsr)
                    {
                        broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REQUEST,
                                "UNIT-2-UNIT VOICE SERVICE " + uuvsr.getVoiceServiceOptions());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                    if(ambtc instanceof AMBTCUnitToUnitAnswerResponse uuar)
                    {
                        broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REQUEST,
                                "UNIT-2-UNIT ANSWER RESPONSE " + uuar.getAnswerResponse());
                    }
                    break;

                //Network configuration messages
                case OSP_ADJACENT_STATUS_BROADCAST:
                    mNetworkConfigurationMonitor.process(ambtc);
                    break;
                case OSP_NETWORK_STATUS_BROADCAST:
                    if((getCurrentChannel() == null || getCurrentChannel().getDownlinkFrequency() > 0) &&
                            mChannel.isStandardChannel() && ambtc instanceof AMBTCNetworkStatusBroadcast nsb &&
                            nsb.getChannel().getDownlinkFrequency() > 0)
                    {
                        setCurrentChannel(nsb.getChannel());
                        DecoderLogicalChannelNameIdentifier channelID =
                                DecoderLogicalChannelNameIdentifier.create(nsb.getChannel().toString(), Protocol.APCO25);
                        getIdentifierCollection().update(channelID);
                        setCurrentFrequency(nsb.getChannel().getDownlinkFrequency());
                        FrequencyConfigurationIdentifier frequencyID = FrequencyConfigurationIdentifier
                                .create(nsb.getChannel().getDownlinkFrequency());
                        getIdentifierCollection().update(frequencyID);

                    }
                    mNetworkConfigurationMonitor.process(ambtc);
                    break;
                case OSP_RFSS_STATUS_BROADCAST:
                    if((getCurrentChannel() == null || getCurrentChannel().getDownlinkFrequency() > 0) &&
                            mChannel.isStandardChannel() && ambtc instanceof AMBTCRFSSStatusBroadcast rsb &&
                            rsb.getChannel().getDownlinkFrequency() > 0)
                    {
                        setCurrentChannel(rsb.getChannel());
                        DecoderLogicalChannelNameIdentifier channelID =
                                DecoderLogicalChannelNameIdentifier.create(rsb.getChannel().toString(), Protocol.APCO25);
                        getIdentifierCollection().update(channelID);
                        setCurrentFrequency(rsb.getChannel().getDownlinkFrequency());
                        FrequencyConfigurationIdentifier frequencyID = FrequencyConfigurationIdentifier
                                .create(rsb.getChannel().getDownlinkFrequency());
                        getIdentifierCollection().update(frequencyID);

                    }
                    mNetworkConfigurationMonitor.process(ambtc);
                    break;

                //Channel grants
                case OSP_GROUP_DATA_CHANNEL_GRANT:
                case OSP_GROUP_VOICE_CHANNEL_GRANT:
                case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_GRANT:
                    processAMBTCChannelGrant(ambtc);
                    break;

                //Channel grant updates
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                    processAMBTCChannelGrantUpdate(ambtc);
                    break;

                case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                    broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.PAGE,
                            "ANSWER REQUEST");
                    break;
                case OSP_CALL_ALERT:
                    broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.PAGE,
                            "CALL ALERT");
                    break;
                case OSP_GROUP_AFFILIATION_QUERY:
                    broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.QUERY,
                            "GROUP AFFILIATION");
                    break;
                case OSP_GROUP_AFFILIATION_RESPONSE:
                    if(ambtc instanceof AMBTCGroupAffiliationResponse gar)
                    {
                        broadcastEvent(ambtc, DecodeEventType.RESPONSE, "AFFILIATION GROUP:" +
                                gar.getGroupAddress() + " ANNOUNCEMENT GROUP:" + gar.getAnnouncementGroup());
                    }
                    break;
                case OSP_MESSAGE_UPDATE:
                    if(ambtc instanceof AMBTCMessageUpdate mu)
                    {
                        broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.SDM, "MESSAGE:" +
                                mu.getShortDataMessage());
                    }
                    break;
                case OSP_ADJACENT_STATUS_BROADCAST_UNCOORDINATED_BAND_PLAN:
                    if(ambtc instanceof AMBTCProtectionParameterBroadcast ppb)
                    {
                        broadcastEvent(ambtc, DecodeEventType.RESPONSE, "USE ENCRYPTION " + ppb.getEncryptionKey() +
                                " OUTBOUND MI:" + ppb.getOutboundMessageIndicator() +
                                " INBOUND MI:" + ppb.getInboundMessageIndicator());
                    }
                    break;
                case OSP_ROAMING_ADDRESS_UPDATE:
                    broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.RESPONSE,
                            "ROAMING ADDRESS UPDATE");
                    break;
                case OSP_ROAMING_ADDRESS_COMMAND:
                    broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.COMMAND,
                            "ROAMING ADDRESS");
                    break;
                case OSP_STATUS_QUERY:
                case OSP_STATUS_UPDATE:
                    processAMBTCStatus(ambtc);
                    break;
                case OSP_UNIT_REGISTRATION_RESPONSE:
                    if(ambtc instanceof AMBTCUnitRegistrationResponse urr)
                    {
                        broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REGISTER,
                                urr.getResponse() + " UNIT REGISTRATION");
                    }
                    break;
                default:
//                    mLog.debug("Unrecognized AMBTC Opcode: " + ambtc.getHeader().getOpcode().name());
                    break;
            }
        }

        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CONTROL));
    }

    /**
     * Process AMBTC status message
     */
    private void processAMBTCStatus(AMBTCMessage ambtc)
    {
        switch(ambtc.getHeader().getOpcode())
        {
            case ISP_STATUS_QUERY_REQUEST:
            case OSP_STATUS_QUERY:
                broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REQUEST,
                        "STATUS QUERY");
                break;
            case ISP_STATUS_QUERY_RESPONSE:
                broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.RESPONSE,
                        "STATUS QUERY");
                break;
            case ISP_STATUS_UPDATE_REQUEST:
                broadcastEvent(ambtc.getIdentifiers(), ambtc.getTimestamp(), DecodeEventType.REQUEST,
                        "STATUS UPDATE");
                break;
            case OSP_STATUS_UPDATE:
                if(ambtc instanceof AMBTCStatusUpdate su)
                {
                    broadcastEvent(ambtc, DecodeEventType.STATUS, "UNIT:" + su.getUnitStatus() + " USER:" +
                            su.getUserStatus());
                }
                break;
        }
    }

    /**
     * AMBTC Channel Grant Updates
     */
    private void processAMBTCChannelGrantUpdate(AMBTCMessage ambtc)
    {
        switch(ambtc.getHeader().getOpcode())
        {
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                if(ambtc instanceof AMBTCUnitToUnitVoiceServiceChannelGrantUpdate upd)
                {
                    processChannelUpdate(upd.getChannel(), upd.getServiceOptions(), upd.getIdentifiers(),
                            ambtc.getHeader().getOpcode(), ambtc.getTimestamp());
                }
                break;
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                if(ambtc instanceof AMBTCTelephoneInterconnectChannelGrantUpdate upd)
                {
                    processChannelUpdate(upd.getChannel(), upd.getServiceOptions(), upd.getIdentifiers(),
                            ambtc.getHeader().getOpcode(), ambtc.getTimestamp());
                }
                break;
        }
    }

    /**
     * AMBTC Channel Grants
     */
    private void processAMBTCChannelGrant(AMBTCMessage ambtc)
    {
        switch(ambtc.getHeader().getOpcode())
        {
            case OSP_GROUP_DATA_CHANNEL_GRANT:
                if(ambtc instanceof AMBTCGroupDataChannelGrant gdcg)
                {
                    processChannelGrant(gdcg.getChannel(), gdcg.getServiceOptions(), gdcg.getIdentifiers(),
                            ambtc.getHeader().getOpcode(), ambtc.getTimestamp());
                }
                break;
            case OSP_GROUP_VOICE_CHANNEL_GRANT:
                if(ambtc instanceof AMBTCGroupVoiceChannelGrant gvcg)
                {
                    processChannelGrant(gvcg.getChannel(), gvcg.getServiceOptions(), gvcg.getIdentifiers(),
                            ambtc.getHeader().getOpcode(), ambtc.getTimestamp());
                }
                break;
            case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
                if(ambtc instanceof AMBTCIndividualDataChannelGrant idcg)
                {
                    processChannelGrant(idcg.getChannel(), idcg.getServiceOptions(), idcg.getIdentifiers(),
                            ambtc.getHeader().getOpcode(), ambtc.getTimestamp());
                }
                break;
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                if(ambtc instanceof AMBTCTelephoneInterconnectChannelGrant ticg)
                {
                    processChannelGrant(ticg.getChannel(), ticg.getServiceOptions(), ticg.getIdentifiers(),
                            ambtc.getHeader().getOpcode(), ambtc.getTimestamp());
                }
                break;
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                if(ambtc instanceof AMBTCUnitToUnitVoiceServiceChannelGrant uuvscg)
                {
                    processChannelGrant(uuvscg.getChannel(), uuvscg.getServiceOptions(), uuvscg.getIdentifiers(),
                            ambtc.getHeader().getOpcode(), ambtc.getTimestamp());
                }
                break;
            case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_GRANT:
                if(ambtc instanceof AMBTCMotorolaGroupRegroupChannelGrant mgrcg)
                {
                    processChannelGrant(mgrcg.getChannel(), mgrcg.getServiceOptions(), mgrcg.getIdentifiers(),
                            ambtc.getHeader().getOpcode(), ambtc.getTimestamp());
                }
                break;
        }
    }

    /**
     * Processes a Header Data Unit message and starts a new call event.
     */
    private void processHDU(IMessage message)
    {
        if(message.isValid() && message instanceof HDUMessage hdu)
        {
            HeaderData headerData = hdu.getHeaderData();
            ServiceOptions serviceOptions = headerData.isEncryptedAudio() ?
                    VoiceServiceOptions.createEncrypted() : VoiceServiceOptions.createUnencrypted();
            MutableIdentifierCollection mic = getMutableIdentifierCollection(hdu.getIdentifiers(), message.getTimestamp());
            String details = headerData.isEncryptedAudio() ? headerData.getEncryptionKey().toString() : null;
            DecodeEventType type = headerData.isEncryptedAudio() ? DecodeEventType.CALL_ENCRYPTED : DecodeEventType.CALL;
            mTrafficChannelManager.processP1CurrentUser(getCurrentFrequency(), getCurrentChannel(), type,
                    serviceOptions, mic, message.getTimestamp(), details);

            if(headerData.isEncryptedAudio())
            {
                broadcast(new DecoderStateEvent(this, Event.START, State.ENCRYPTED));
            }
            else
            {
                broadcast(new DecoderStateEvent(this, Event.START, State.CALL));
            }
        }
    }


    /**
     * Processes an LDU voice message and forwards Link Control and/or Encryption Sync Parameters for
     * additional processing.
     *
     * @param message that is an instance of an LDU1 or LDU2 message
     */
    private void processLDU(P25P1Message message)
    {
        if(message instanceof LDU1Message ldu1)
        {
            LinkControlWord lcw = ldu1.getLinkControlWord();

            if(lcw != null && lcw.isValid())
            {
                processLC(lcw, message.getTimestamp(), false);
            }
        }
        else if(message instanceof LDU2Message ldu2)
        {
            EncryptionSyncParameters esp = ldu2.getEncryptionSyncParameters();

            if(esp != null && esp.isValid())
            {
                if(esp.isEncryptedAudio())
                {
                    getIdentifierCollection().update(esp.getIdentifiers());
                    mTrafficChannelManager.processP1CurrentUser(getCurrentFrequency(), esp.getEncryptionKey(),
                            message.getTimestamp());
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ENCRYPTED));
                }
                else
                {
                    getIdentifierCollection().remove(Form.ENCRYPTION_KEY);
                    mTrafficChannelManager.processP1CurrentUser(getCurrentFrequency(), null,
                            message.getTimestamp());
                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));
                }
            }
            else
            {
                mTrafficChannelManager.processP1CurrentUser(getCurrentFrequency(), null, message.getTimestamp());
            }
        }

    }

    /**
     * Process Terminator Data Unit (TDU).
     */
    private void processTDU(P25P1Message message)
    {
        mTrafficChannelManager.closeP1CallEvent(getCurrentFrequency(), message.getTimestamp());
        getIdentifierCollection().remove(IdentifierClass.USER, Role.FROM);
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE));
    }

    /**
     * Process Terminator Data Unit with Link Control (TDULC) message and forwards valid Link Control Word message for
     * additional processing.
     *
     * @param message that is an instance of a TDULC
     */
    private void processTDULC(P25P1Message message)
    {
        mTrafficChannelManager.closeP1CallEvent(getCurrentFrequency(), message.getTimestamp());
        getIdentifierCollection().remove(IdentifierClass.USER, Role.FROM);

        if(message instanceof TDULinkControlMessage tdulc)
        {
            LinkControlWord lcw = tdulc.getLinkControlWord();

            if(lcw != null && lcw.isValid())
            {
                //Send an ACTIVE decoder state event for everything except the CALL TERMINATION opcode which is
                //handled by the processLC() method.
                if(lcw.getOpcode() != LinkControlOpcode.CALL_TERMINATION_OR_CANCELLATION)
                {
                    //Set the state to ACTIVE while the call continues in hangtime.  The processLC() method will signal
                    // the channel teardown.
                    broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE));
                }

                processLC(lcw, message.getTimestamp(), true);
            }
        }
    }

    /**
     * Packet Data Unit
     *
     * @param message
     */
    private void processPDU(P25P1Message message)
    {
        if(message.isValid() && message instanceof PDUMessage pdu)
        {
            broadcastEvent(pdu.getIdentifiers(), message.getTimestamp(), DecodeEventType.DATA_PACKET, pdu.toString());
        }
        else if(message.isValid() && message instanceof ResponseMessage response)
        {
            broadcastEvent(message.getIdentifiers(), message.getTimestamp(), DecodeEventType.RESPONSE, response.getResponseText());
        }
        else if(message.isValid() && message instanceof PDUSequenceMessage pdu && pdu.getPDUSequence().isComplete())
        {
            broadcastEvent(pdu.getIdentifiers(), message.getTimestamp(), DecodeEventType.DATA_PACKET, pdu.toString());
        }

        broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));
    }

    /**
     * Unconfirmed Multi-Block Trunking Control (UMBTC)
     *
     * @param message
     */
    private void processUMBTC(P25P1Message message)
    {
        if(message.isValid() && message instanceof UMBTCTelephoneInterconnectRequestExplicitDialing tired)
        {
            broadcastEvent(message.getIdentifiers(), message.getTimestamp(), DecodeEventType.REQUEST,
                    "TELEPHONE INTERCONNECT:" + tired.getTelephoneNumber());
        }

        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CONTROL));
    }

    /**
     * IP Packet Data
     *
     * @param message
     */
    private void processPacketData(P25P1Message message)
    {
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));

        if(message instanceof SNDCPPacketMessage sndcp)
        {
            processSNDCP(sndcp);
        }
        else if(message instanceof PacketMessage packetMessage)
        {
            IPacket packet = packetMessage.getPacket();

            if(packet instanceof IPV4Packet ipv4)
            {
                IPacket ipPayload = ipv4.getPayload();

                if(ipPayload instanceof UDPPacket udpPacket)
                {
                    IPacket udpPayload = udpPacket.getPayload();

                    if(udpPayload instanceof ARSPacket arsPacket)
                    {
                        MutableIdentifierCollection mic = getMutableIdentifierCollection(message.getIdentifiers(), message.getTimestamp());

                        DecodeEvent packetEvent = P25DecodeEvent.builder(DecodeEventType.AUTOMATIC_REGISTRATION_SERVICE,
                                        message.getTimestamp())
                                .channel(getCurrentChannel())
                                .identifiers(mic)
                                .details(arsPacket + " " + ipv4)
                                .build();

                        broadcast(packetEvent);
                    }
                    else if(udpPayload instanceof MCGPPacket mcgp)
                    {
                        MutableIdentifierCollection mic = getMutableIdentifierCollection(message.getIdentifiers(), message.getTimestamp());

                        DecodeEvent cellocatorEvent = P25DecodeEvent.builder(DecodeEventType.CELLOCATOR,
                                        message.getTimestamp())
                                .channel(getCurrentChannel())
                                .identifiers(mic)
                                .details(mcgp + " " + ipv4)
                                .build();

                        broadcast(cellocatorEvent);
                    }
                    else if(udpPayload instanceof LRRPPacket lrrpPacket)
                    {
                        MutableIdentifierCollection mic = getMutableIdentifierCollection(message.getIdentifiers(), message.getTimestamp());

                        DecodeEvent lrrpEvent = P25DecodeEvent.builder(DecodeEventType.LRRP, message.getTimestamp())
                                .channel(getCurrentChannel())
                                .details(lrrpPacket + " " + ipv4)
                                .identifiers(mic)
                                .protocol(Protocol.LRRP)
                                .build();

                        broadcast(lrrpEvent);

                        GeoPosition geoPosition = PacketUtil.extractGeoPosition(lrrpPacket);

                        if(geoPosition != null)
                        {
                            PlottableDecodeEvent plottableDecodeEvent = PlottableDecodeEvent
                                    .plottableBuilder(DecodeEventType.GPS, message.getTimestamp())
                                    .channel(getCurrentChannel())
                                    .identifiers(mic)
                                    .protocol(Protocol.LRRP)
                                    .location(geoPosition)
                                    .build();

                            broadcast(plottableDecodeEvent);
                        }
                    }
                    else
                    {
                        MutableIdentifierCollection mic = getMutableIdentifierCollection(message.getIdentifiers(), message.getTimestamp());

                        DecodeEvent packetEvent = P25DecodeEvent.builder(DecodeEventType.UDP_PACKET, message.getTimestamp())
                                .channel(getCurrentChannel())
                                .identifiers(mic)
                                .details(ipv4.toString())
                                .build();

                        broadcast(packetEvent);
                    }
                }
                else if(ipPayload instanceof ICMPPacket)
                {
                    MutableIdentifierCollection mic = getMutableIdentifierCollection(message.getIdentifiers(), message.getTimestamp());

                    DecodeEvent packetEvent = P25DecodeEvent.builder(DecodeEventType.ICMP_PACKET, message.getTimestamp())
                            .channel(getCurrentChannel())
                            .identifiers(mic)
                            .details(ipv4.toString())
                            .build();

                    broadcast(packetEvent);
                }
                else
                {
                    MutableIdentifierCollection mic = getMutableIdentifierCollection(message.getIdentifiers(), message.getTimestamp());

                    DecodeEvent packetEvent = P25DecodeEvent.builder(DecodeEventType.IP_PACKET, message.getTimestamp())
                            .channel(getCurrentChannel())
                            .identifiers(mic)
                            .details(ipv4.toString())
                            .build();

                    broadcast(packetEvent);
                }
            }
            else if(packet instanceof UnknownPacket)
            {
                MutableIdentifierCollection mic = getMutableIdentifierCollection(message.getIdentifiers(), message.getTimestamp());

                DecodeEvent packetEvent = P25DecodeEvent.builder(DecodeEventType.UNKNOWN_PACKET, message.getTimestamp())
                        .channel(getCurrentChannel())
                        .identifiers(mic)
                        .details(packet.toString())
                        .build();

                broadcast(packetEvent);
            }
        }
    }

    /**
     * Sub-Network Dependent Convergence Protocol (SNDCP)
     *
     * @param message to process
     */
    private void processSNDCP(P25P1Message message)
    {
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));

        if(message.isValid() && message instanceof SNDCPPacketMessage sndcpPacket)
        {
            switch(sndcpPacket.getSNDCPMessage().getPDUType())
            {
                case OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_ACCEPT:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.RESPONSE, "SNDCP ACTIVATE TDS CONTEXT ACCEPT");
                    break;
                case OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.RESPONSE, "SNDCP DEACTIVATE TDS CONTEXT ACCEPT");
                    break;
                case OUTBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.REQUEST, "SNDCP DEACTIVATE TDS CONTEXT");
                    break;
                case OUTBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REJECT:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.RESPONSE, "SNDCP ACTIVATE TDS CONTEXT REJECT");
                    break;
                case OUTBOUND_SNDCP_RF_UNCONFIRMED_DATA:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.REQUEST, sndcpPacket.toString());
                    break;
                case OUTBOUND_SNDCP_RF_CONFIRMED_DATA:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.REQUEST, sndcpPacket.toString());
                    break;
                case OUTBOUND_UNKNOWN:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.REQUEST, sndcpPacket.toString());
                    break;
                case INBOUND_SNDCP_ACTIVATE_TDS_CONTEXT_REQUEST:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.REQUEST, "SNDCP ACTIVATE TDS CONTEXT");
                    break;
                case INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.RESPONSE, "SNDCP DEACTIVATE TDS CONTEXT ACCEPT");
                    break;
                case INBOUND_SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.REQUEST, "SNDCP DEACTIVATE TDS CONTEXT");
                    break;
                case INBOUND_SNDCP_RF_CONFIRMED_DATA:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.REQUEST, sndcpPacket.toString());
                    break;
                case INBOUND_UNKNOWN:
                    broadcastEvent(sndcpPacket.getIdentifiers(), message.getTimestamp(),
                            DecodeEventType.REQUEST, sndcpPacket.toString());
                    break;
            }
        }
    }

    /**
     * Trunking Signalling Block (TSBK) messages
     */
    private void processTSBK(P25P1Message message)
    {
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CONTROL));

        if(message.isValid() && message instanceof TSBKMessage tsbk)
        {
            switch(tsbk.getOpcode())
            {
                //Channel Grant messages
                case OSP_GROUP_DATA_CHANNEL_GRANT:
                case OSP_GROUP_VOICE_CHANNEL_GRANT:
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                case OSP_SNDCP_DATA_CHANNEL_GRANT:
                case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_GRANT:
                    processTSBKChannelGrant(tsbk);
                    break;

                //Channel Grant Update messages
                case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_UPDATE:
                    processTSBKChannelGrantUpdate(tsbk);
                    break;

                    //Network Configuration Messages
                case MOTOROLA_OSP_TRAFFIC_CHANNEL_ID:
                case MOTOROLA_OSP_SYSTEM_LOADING:
                case MOTOROLA_OSP_BASE_STATION_ID:
                case MOTOROLA_OSP_CONTROL_CHANNEL_PLANNED_SHUTDOWN:
                case OSP_IDENTIFIER_UPDATE_TDMA:
                case OSP_IDENTIFIER_UPDATE_VHF_UHF_BANDS:
                case OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                case OSP_SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                case OSP_TIME_DATE_ANNOUNCEMENT:
                case OSP_TDMA_SYNC_BROADCAST:
                case OSP_SYSTEM_SERVICE_BROADCAST:
                case OSP_SECONDARY_CONTROL_CHANNEL_BROADCAST:
                case OSP_ADJACENT_STATUS_BROADCAST:
                case OSP_IDENTIFIER_UPDATE:
                case OSP_ADJACENT_STATUS_BROADCAST_UNCOORDINATED_BAND_PLAN:
                case OSP_RESERVED_3F:
                    mNetworkConfigurationMonitor.process(tsbk);

                    //Send the frequency bands to the traffic channel manager to use for traffic channel preload data
                    if(tsbk instanceof IFrequencyBand frequencyBand)
                    {
                        mTrafficChannelManager.processFrequencyBand(frequencyBand);
                    }
                    break;
                case OSP_NETWORK_STATUS_BROADCAST:
                    if((getCurrentChannel() == null || getCurrentChannel().getDownlinkFrequency() > 0) &&
                            mChannel.isStandardChannel() && tsbk instanceof NetworkStatusBroadcast nsb &&
                            nsb.getChannel().getDownlinkFrequency() > 0)
                    {
                        setCurrentChannel(nsb.getChannel());
                        DecoderLogicalChannelNameIdentifier channelID =
                                DecoderLogicalChannelNameIdentifier.create(nsb.getChannel().toString(), Protocol.APCO25);
                        getIdentifierCollection().update(channelID);
                        setCurrentFrequency(nsb.getChannel().getDownlinkFrequency());
                        FrequencyConfigurationIdentifier frequencyID = FrequencyConfigurationIdentifier
                                .create(nsb.getChannel().getDownlinkFrequency());
                        getIdentifierCollection().update(frequencyID);

                    }
                    mNetworkConfigurationMonitor.process(tsbk);
                    break;
                case OSP_RFSS_STATUS_BROADCAST:
                    if((getCurrentChannel() == null || getCurrentChannel().getDownlinkFrequency() > 0) &&
                            mChannel.isStandardChannel() && tsbk instanceof RFSSStatusBroadcast rfss &&
                            rfss.getChannel().getDownlinkFrequency() > 0)
                    {
                        setCurrentChannel(rfss.getChannel());
                        DecoderLogicalChannelNameIdentifier channelID =
                                DecoderLogicalChannelNameIdentifier.create(rfss.getChannel().toString(), Protocol.APCO25);
                        getIdentifierCollection().update(channelID);
                        setCurrentFrequency(rfss.getChannel().getDownlinkFrequency());
                        FrequencyConfigurationIdentifier frequencyID = FrequencyConfigurationIdentifier
                                .create(rfss.getChannel().getDownlinkFrequency());
                        getIdentifierCollection().update(frequencyID);
                    }
                    mNetworkConfigurationMonitor.process(tsbk);
                    break;

                case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.PAGE,
                            "UNIT-TO-UNIT ANSWER REQUEST");
                    break;
                case OSP_TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                    if(tsbk instanceof TelephoneInterconnectAnswerRequest tiar)
                    {
                        broadcastEvent(tsbk, DecodeEventType.PAGE, "TELEPHONE ANSWER REQUEST: " +
                                tiar.getTelephoneNumber());
                    }
                    break;
                case OSP_SNDCP_DATA_PAGE_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.PAGE,
                            "SNDCP DATA PAGE REQUEST");
                    break;
                case OSP_STATUS_UPDATE:
                case OSP_STATUS_QUERY:
                    processTSBKStatus(tsbk);
                    break;
                case OSP_MESSAGE_UPDATE:
                    if(tsbk instanceof MessageUpdate mu)
                    {
                        broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.SDM, "MSG:" +
                                mu.getShortDataMessage());
                    }
                    break;
                case OSP_RADIO_UNIT_MONITOR_COMMAND:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.COMMAND,
                            "RADIO UNIT MONITOR");
                    break;
                case OSP_CALL_ALERT:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.PAGE, "CALL ALERT");
                    break;
                case OSP_ACKNOWLEDGE_RESPONSE:
                    processTSBKAcknowledgeResponse(tsbk);
                    break;
                case OSP_QUEUED_RESPONSE:
                    processTSBKQueuedResponse(tsbk);
                    break;
                case OSP_EXTENDED_FUNCTION_COMMAND:
                    processTSBKExtendedFunctionCommand(tsbk);
                    break;
                case OSP_DENY_RESPONSE:
                    processTSBKDenyResponse(tsbk);
                    break;
                case OSP_GROUP_AFFILIATION_RESPONSE:
                    processTSBKGroupAffiliationResponse(tsbk);
                    break;
                case OSP_GROUP_AFFILIATION_QUERY:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.QUERY,
                            "GROUP AFFILIATION");
                    break;
                case OSP_LOCATION_REGISTRATION_RESPONSE:
                    processTSBKLocationRegistrationResponse(tsbk);
                    break;
                case OSP_UNIT_REGISTRATION_RESPONSE:
                    processTSBKUnitRegistrationResponse(tsbk);
                    break;
                case OSP_UNIT_REGISTRATION_COMMAND:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.COMMAND,
                            "UNIT REGISTRATION");
                    break;
                case OSP_AUTHENTICATION_COMMAND:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.COMMAND,
                            "AUTHENTICATE");
                    break;
                case OSP_UNIT_DEREGISTRATION_ACKNOWLEDGE:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.DEREGISTER,
                            "ACKNOWLEDGE UNIT DE-REGISTRATION");
                    break;
                case OSP_ROAMING_ADDRESS_COMMAND:
                    if(tsbk instanceof RoamingAddressCommand rac)
                    {
                        broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.COMMAND,
                                rac.getStackOperation() + " ROAMING ADDRESS");
                    }
                    break;

                //MOTOROLA PATCH GROUP OPCODES
                case MOTOROLA_OSP_GROUP_REGROUP_ADD:
                    mPatchGroupManager.addPatchGroups(tsbk.getIdentifiers(), message.getTimestamp());
                    break;
                case MOTOROLA_OSP_GROUP_REGROUP_DELETE:
                    mPatchGroupManager.removePatchGroups(tsbk.getIdentifiers());
                    break;

                //L3HARRIS PATCH GROUP OPCODES
                case HARRIS_OSP_GRG_EXENC_CMD:
                    if(tsbk instanceof L3HarrisGroupRegroupExplicitEncryptionCommand regroup)
                    {
                        if(regroup.getRegroupOptions().isActivate())
                        {
                            mPatchGroupManager.addPatchGroup(regroup.getPatchGroup(), tsbk.getTimestamp());
                        }
                        else
                        {
                            mPatchGroupManager.removePatchGroup(regroup.getPatchGroup());
                        }
                    }
                    break;

                //STANDARD - INBOUND OPCODES
                case ISP_GROUP_VOICE_SERVICE_REQUEST:
                    if(tsbk instanceof GroupVoiceServiceRequest gvsr)
                    {
                        broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                                "GROUP VOICE SERVICE " + gvsr.getServiceOptions());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                    if(tsbk instanceof UnitToUnitVoiceServiceRequest uuvsr)
                    {
                        broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                                "UNIT-2-UNIT VOICE SERVICE " + uuvsr.getServiceOptions());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                    processTSBKUnitToUnitAnswerResponse(tsbk);
                    break;
                case ISP_TELEPHONE_INTERCONNECT_PSTN_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                            "TELEPHONE INTERCONNECT");
                    break;
                case ISP_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
                    processTSBKTelephoneInterconnectAnswerResponse(tsbk);
                    break;
                case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                    if(tsbk instanceof IndividualDataServiceRequest idsr)
                    {
                        broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                                "INDIVIDUAL DATA SERVICE " + idsr.getServiceOptions());
                    }
                    break;
                case ISP_GROUP_DATA_SERVICE_REQUEST:
                    if(tsbk instanceof GroupDataServiceRequest gdsr)
                    {
                        broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                                "GROUP DATA SERVICE " + gdsr.getServiceOptions());
                    }
                    break;
                case ISP_SNDCP_DATA_CHANNEL_REQUEST:
                    if(tsbk instanceof SNDCPDataChannelRequest sdcr)
                    {
                        broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                                "SNDCP DATA CHANNEL " + sdcr.getDataServiceOptions());
                    }
                    break;
                case ISP_SNDCP_DATA_PAGE_RESPONSE:
                    processTSBKSndcpDataPageResponse(tsbk);
                    break;
                case ISP_SNDCP_RECONNECT_REQUEST:
                    processTSBKSndcpReconnectRequest(tsbk);
                    break;
                case ISP_STATUS_UPDATE_REQUEST:
                case ISP_STATUS_QUERY_RESPONSE:
                case ISP_STATUS_QUERY_REQUEST:
                    processTSBKStatus(tsbk);
                    break;
                case ISP_MESSAGE_UPDATE_REQUEST:
                    if(tsbk instanceof MessageUpdateRequest mur)
                    {
                        broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.SDM,
                                "MESSAGE:" + mur.getShortDataMessage());
                    }
                    break;
                case ISP_RADIO_UNIT_MONITOR_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                            "RADIO UNIT MONITOR");
                    break;
                case ISP_CALL_ALERT_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                            "CALL ALERT");
                    break;
                case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                    if(tsbk instanceof UnitAcknowledgeResponse uar)
                    {
                        broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.RESPONSE,
                                "UNIT ACKNOWLEDGE:" + uar.getAcknowledgedServiceType().getDescription());
                    }
                    break;
                case ISP_CANCEL_SERVICE_REQUEST:
                    if(tsbk instanceof CancelServiceRequest csr)
                    {
                        broadcastEvent(tsbk, DecodeEventType.REQUEST, "CANCEL SERVICE:" + csr.getServiceType() +
                            " REASON:" + csr.getCancelReason() + (csr.hasAdditionalInformation() ? " INFO:" +
                                csr.getAdditionalInformation() : ""));
                    }
                    break;
                case ISP_EXTENDED_FUNCTION_RESPONSE:
                    if(tsbk instanceof ExtendedFunctionResponse efr)
                    {
                        broadcastEvent(tsbk, DecodeEventType.RESPONSE, "EXTENDED FUNCTION:" +
                                efr.getExtendedFunction() + " ARGUMENTS:" + efr.getArguments());
                    }
                    break;
                case ISP_EMERGENCY_ALARM_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                            "EMERGENCY ALARM");
                    break;
                case ISP_GROUP_AFFILIATION_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                            "GROUP AFFILIATION");
                    break;
                case ISP_GROUP_AFFILIATION_QUERY_RESPONSE:
                    if(tsbk instanceof GroupAffiliationQueryResponse gaqr)
                    {
                        broadcastEvent(tsbk, DecodeEventType.RESPONSE, "AFFILIATION - GROUP:" +
                            gaqr.getGroupAddress() + " ANNOUNCEMENT GROUP:" + gaqr.getAnnouncementGroupAddress());
                    }
                    break;
                case ISP_UNIT_DE_REGISTRATION_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.DEREGISTER,
                            "UNIT DE-REGISTRATION REQUEST");
                    break;
                case ISP_UNIT_REGISTRATION_REQUEST:
                    if(tsbk instanceof UnitRegistrationRequest urr)
                    {
                        broadcastEvent(tsbk, DecodeEventType.REGISTER, (urr.isEmergency() ? "EMERGENCY " : "") +
                            "UNIT REGISTRATION REQUEST - CAPABILITY:" + urr.getCapability());
                    }
                    break;
                case ISP_LOCATION_REGISTRATION_REQUEST:
                    if(tsbk instanceof LocationRegistrationRequest lrr)
                    {
                        broadcastEvent(tsbk, DecodeEventType.REGISTER, (lrr.isEmergency() ? "EMERGENCY " : "") +
                            "LOCATION REGISTRATION REQUEST - CAPABILITY:" + lrr.getCapability());
                    }
                    break;
                case ISP_PROTECTION_PARAMETER_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                            "ENCRYPTION PARAMETERS");
                    break;
                case ISP_IDENTIFIER_UPDATE_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                            "FREQUENCY BAND DETAILS");
                    break;
                case ISP_ROAMING_ADDRESS_REQUEST:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.REQUEST,
                            "ROAMING ADDRESS");
                    break;
                case ISP_ROAMING_ADDRESS_RESPONSE:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.RESPONSE,
                            "ROAMING ADDRESS");
                    break;
                case MOTOROLA_OSP_ACKNOWLEDGE_RESPONSE:
                    processTSBKAcknowledgeResponse(tsbk);
                    break;
                case MOTOROLA_OSP_DENY_RESPONSE:
                    processTSBKDenyResponse(tsbk);
                    break;
                case MOTOROLA_OSP_EMERGENCY_ALARM_ACTIVATION:
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.EMERGENCY,
                            "RADIO EMERGENCY ALARM ACTIVATION");
                    break;
                case MOTOROLA_OSP_EXTENDED_FUNCTION_COMMAND:
                    processTSBKExtendedFunctionCommand(tsbk);
                    break;
                case MOTOROLA_OSP_QUEUED_RESPONSE:
                    processTSBKQueuedResponse(tsbk);
                default:
//                    if(!tsbk.getOpcode().name().startsWith("ISP"))
//                    {
//                        LOGGING_SUPPRESSOR.info(tsbk.getOpcode().name() + tsbk.getMessage().toHexString(),
//                        1, "Unrecognized TSBK Opcode: " + tsbk.getOpcode().name() +
//                            " VENDOR:" + tsbk.getVendor() + " OPCODE:" + tsbk.getOpcodeNumber() +
//                                " MSG:" + tsbk.getMessage().toHexString());
//                    }
                    break;
            }
        }
    }

    /**
     * TSBK Status messaging
     */
    private void processTSBKStatus(TSBKMessage tsbk)
    {
        switch(tsbk.getOpcode())
        {
            case ISP_STATUS_UPDATE_REQUEST:
                if(tsbk instanceof StatusUpdateRequest sur)
                {
                    broadcastEvent(tsbk, DecodeEventType.STATUS, "UNIT:" + sur.getUnitStatus() + " USER:" +
                            sur.getUserStatus());
                }
                break;
            case ISP_STATUS_QUERY_RESPONSE:
                if(tsbk instanceof StatusQueryResponse sqr)
                {
                    broadcastEvent(tsbk, DecodeEventType.STATUS, "UNIT:" + sqr.getUnitStatus() + " USER:" +
                            sqr.getUserStatus());
                }
                break;
            case ISP_STATUS_QUERY_REQUEST:
                if(tsbk instanceof StatusQueryRequest sqr)
                {
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.QUERY,
                            "UNIT AND USER STATUS");
                }
                break;
            case OSP_STATUS_QUERY:
                if(tsbk instanceof StatusQuery sq)
                {
                    broadcastEvent(tsbk.getIdentifiers(), tsbk.getTimestamp(), DecodeEventType.QUERY,
                            "UNIT AND USER STATUS");
                }
                break;
            case OSP_STATUS_UPDATE:
                if(tsbk instanceof StatusUpdate su)
                {
                    broadcastEvent(tsbk, DecodeEventType.STATUS,
                            "UNIT:" + su.getUnitStatus() + " USER:" + su.getUserStatus());
                }
                break;
        }
    }

    private void processTSBKSndcpReconnectRequest(TSBKMessage tsbk)
    {
        if(tsbk instanceof SNDCPReconnectRequest srr)
        {
            broadcastEvent(tsbk, DecodeEventType.REQUEST,
 		"SNDCP RECONNECT " + (srr.hasDataToSend() ? "- DATA TO SEND " : "")
                    + srr.getDataServiceOptions())
;
        }
    }

    private void processTSBKSndcpDataPageResponse(TSBKMessage tsbk)
    {
        if(tsbk instanceof SNDCPDataPageResponse sdpr)
        {
            broadcastEvent(tsbk, DecodeEventType.RESPONSE,
 		sdpr.getAnswerResponse() + " SNDCP DATA " + sdpr.getDataServiceOptions());
        }
    }

    private void processTSBKTelephoneInterconnectAnswerResponse(TSBKMessage tsbk)
    {
        if(tsbk instanceof TelephoneInterconnectAnswerResponse tiar)
        {
            broadcastEvent(tsbk, DecodeEventType.RESPONSE,
 		tiar.getAnswerResponse() + " TELEPHONE INTERCONNECT " + tiar.getServiceOptions());
        }
    }

    private void processTSBKUnitToUnitAnswerResponse(TSBKMessage tsbk)
    {
        if(tsbk instanceof UnitToUnitVoiceServiceAnswerResponse uuvsar)
        {
            broadcastEvent(tsbk, DecodeEventType.RESPONSE,
 		uuvsar.getAnswerResponse() + " UNIT-2-UNIT VOICE SERVICE " + uuvsar.getServiceOptions());
        }
    }

    private void processTSBKUnitRegistrationResponse(TSBKMessage tsbk)
    {
        if(tsbk instanceof UnitRegistrationResponse urr)
        {
            broadcastEvent(tsbk, DecodeEventType.REGISTER, urr.getResponse() + " UNIT REGISTRATION - UNIT ID:" +
                    urr.getRegisteredRadio());
        }
    }

    private void processTSBKLocationRegistrationResponse(TSBKMessage tsbk)
    {
        if(tsbk instanceof LocationRegistrationResponse lrr)
        {
            broadcastEvent(tsbk, DecodeEventType.REGISTER,
 		lrr.getResponse() + " LOCATION REGISTRATION - GROUP:" + lrr.getGroupAddress());
        }
    }

    private void processTSBKGroupAffiliationResponse(TSBKMessage tsbk)
    {
        if(tsbk instanceof GroupAffiliationResponse gar)
        {
            broadcastEvent(tsbk, DecodeEventType.RESPONSE, gar.getAffiliationResponse() +
                    " AFFILIATION GROUP: " + gar.getGroupAddress() +
                    (gar.isGlobalAffiliation() ? " (GLOBAL)" : " (LOCAL)") +
                    " ANNOUNCEMENT GROUP:" + gar.getAnnouncementGroupAddress());
        }
    }

    private void processTSBKDenyResponse(TSBKMessage tsbk)
    {
        if(tsbk instanceof DenyResponse dr)
        {
            broadcastEvent(tsbk, DecodeEventType.RESPONSE,
 		"DENY: " + dr.getDeniedServiceType().getDescription() +
                    " REASON: " + dr.getDenyReason() + " - INFO: " + dr.getAdditionalInfo());
        }
        else if(tsbk instanceof MotorolaDenyResponse mdr)
        {
            broadcastEvent(tsbk, DecodeEventType.RESPONSE, "DENY: " + mdr.getDeniedServiceType().getDescription()
                    + " REASON: " + mdr.getDenyReason() + " - INFO: " + mdr.getAdditionalInfo());
        }
    }

    private void processTSBKExtendedFunctionCommand(TSBKMessage tsbk)
    {
        if(tsbk instanceof ExtendedFunctionCommand efc)
        {
            broadcastEvent(tsbk, DecodeEventType.COMMAND, "FUNCTION: " + efc.getExtendedFunction() +
                    " ARGUMENTS:" + efc.getArguments());
        }
        else if(tsbk instanceof MotorolaExtendedFunctionCommand mefc)
        {
            if(mefc.isSupergroupCreate())
            {
                mPatchGroupManager.addPatchGroup(mefc.getSuperGroup(), tsbk.getTimestamp());
                broadcastEvent(tsbk, DecodeEventType.COMMAND, "CREATE SUPERGROUP:" + mefc.getSuperGroup());
            }
            else if(mefc.isSupergroupCancel())
            {
                mPatchGroupManager.removePatchGroup(mefc.getSuperGroup());
                broadcastEvent(tsbk, DecodeEventType.COMMAND, "CANCEL SUPERGROUP:" + mefc.getSuperGroup());
            }
            else
            {
                broadcastEvent(tsbk, DecodeEventType.COMMAND, "FUNCTION CLASS: " + mefc.getFunctionClass() + " OPERAND:" + mefc.getFunctionOperand() + " ARGUMENTS:" + mefc.getFunctionArguments());
            }
        }

    }

    private void processTSBKQueuedResponse(TSBKMessage tsbk)
    {
        if(tsbk instanceof QueuedResponse qr)
        {
            broadcastEvent(tsbk, DecodeEventType.RESPONSE, "QUEUED: " +
                    qr.getQueuedResponseServiceType().getDescription() + " REASON: " + qr.getQueuedResponseReason() +
                    " INFO: " + qr.getAdditionalInfo());
        }
        else if(tsbk instanceof MotorolaQueuedResponse mqr)
        {
            broadcastEvent(tsbk, DecodeEventType.RESPONSE, "QUEUED: " + mqr.getQueuedServiceType().getDescription() +
                    " REASON: " + mqr.getQueuedResponseReason() + " INFO: " + mqr.getAdditionalInfo());
        }
    }

    private void processTSBKAcknowledgeResponse(TSBKMessage tsbk)
    {
        if(tsbk instanceof AcknowledgeResponse ar)
        {
            broadcastEvent(tsbk, DecodeEventType.RESPONSE, "ACKNOWLEDGE " + ar.getAcknowledgedService().getDescription());
        }
        else if(tsbk instanceof MotorolaAcknowledgeResponse mar)
        {
            broadcastEvent(tsbk, DecodeEventType.ACKNOWLEDGE, "ACKNOWLEDGE " + mar.getAcknowledgedService().getDescription());
        }
    }

    /**
     * TSBK Channel Grant Update messages
     */
    private void processTSBKChannelGrantUpdate(TSBKMessage tsbk)
    {
        switch(tsbk.getOpcode())
        {
            case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_UPDATE:
                if(tsbk instanceof MotorolaGroupRegroupChannelUpdate pgvcgu)
                {
                    processChannelUpdate(pgvcgu.getChannel1(), null, Collections.singletonList(pgvcgu.getPatchGroup1()),
                            tsbk.getOpcode(), pgvcgu.getTimestamp());

                    if(pgvcgu.hasPatchGroup2())
                    {
                        processChannelUpdate(pgvcgu.getChannel2(), null, Collections.singletonList(pgvcgu.getPatchGroup2()),
                                tsbk.getOpcode(), pgvcgu.getTimestamp());
                    }
                }
                break;
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                if(tsbk instanceof GroupVoiceChannelGrantUpdate gvcgu)
                {
                    processChannelUpdate(gvcgu.getChannelA(), null, Collections.singletonList(gvcgu.getGroupAddressA()),
                            tsbk.getOpcode(), gvcgu.getTimestamp());

                    if(gvcgu.hasGroupB())
                    {
                        processChannelGrant(gvcgu.getChannelB(), null, Collections.singletonList(gvcgu.getGroupAddressB()),
                                tsbk.getOpcode(), gvcgu.getTimestamp());
                    }
                }
                break;
            case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                if(tsbk instanceof GroupVoiceChannelGrantUpdateExplicit gvcgue)
                {
                    processChannelUpdate(gvcgue.getChannel(), gvcgue.getServiceOptions(), gvcgue.getIdentifiers(),
                            tsbk.getOpcode(), gvcgue.getTimestamp());
                }
                break;
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                if(tsbk instanceof TelephoneInterconnectVoiceChannelGrantUpdate tivcgu)
                {
                    processChannelUpdate(tivcgu.getChannel(), tivcgu.getServiceOptions(), tivcgu.getIdentifiers(),
                            tsbk.getOpcode(), tivcgu.getTimestamp());
                }
                break;
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                if(tsbk instanceof UnitToUnitVoiceChannelGrantUpdate uuvcgu)
                {
                    processChannelUpdate(uuvcgu.getChannel(), null, uuvcgu.getIdentifiers(), tsbk.getOpcode(),
                            uuvcgu.getTimestamp());
                }
                break;
        }
    }

    /**
     * TSBK Channel Grant messages
     */
    private void processTSBKChannelGrant(TSBKMessage tsbk)
    {
        switch(tsbk.getOpcode())
        {
            case MOTOROLA_OSP_GROUP_REGROUP_CHANNEL_GRANT:
                if(tsbk instanceof MotorolaGroupRegroupChannelGrant mgrcg)
                {
                    processChannelGrant(mgrcg.getChannel(), mgrcg.getServiceOptions(), mgrcg.getIdentifiers(), tsbk.getOpcode(),
                            mgrcg.getTimestamp());
                }
                break;
            case OSP_GROUP_DATA_CHANNEL_GRANT:
                if(tsbk instanceof GroupDataChannelGrant gdcg)
                {
                    processChannelGrant(gdcg.getChannel(), gdcg.getDataServiceOptions(), gdcg.getIdentifiers(),
                            tsbk.getOpcode(), gdcg.getTimestamp());
                }
                break;
            case OSP_GROUP_VOICE_CHANNEL_GRANT:
                if(tsbk instanceof GroupVoiceChannelGrant gvcg)
                {
                    processChannelGrant(gvcg.getChannel(), gvcg.getServiceOptions(), gvcg.getIdentifiers(), tsbk.getOpcode(),
                            gvcg.getTimestamp());
                }
                break;
            case OSP_SNDCP_DATA_CHANNEL_GRANT:
                if(tsbk instanceof SNDCPDataChannelGrant dcg)
                {
                    processChannelGrant(dcg.getChannel(), dcg.getServiceOptions(), dcg.getIdentifiers(), tsbk.getOpcode(),
                            dcg.getTimestamp());
                }
                break;
            case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                if(tsbk instanceof UnitToUnitVoiceChannelGrant uuvcg)
                {
                    processChannelGrant(uuvcg.getChannel(), null, uuvcg.getIdentifiers(), tsbk.getOpcode(),
                            uuvcg.getTimestamp());
                }
                break;
            case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                if(tsbk instanceof TelephoneInterconnectVoiceChannelGrant tivcg)
                {
                    processChannelGrant(tivcg.getChannel(), tivcg.getServiceOptions(), tivcg.getIdentifiers(), tsbk.getOpcode(),
                            tivcg.getTimestamp());
                }
                break;
        }
    }

    /**
     * Processes a Link Control Word (LCW) that is carried by either an LDU1 or a TDULC message.
     *
     * Note: this method does not broadcast a DecoderStateEvent -- that is handled by the parent message processing
     * method.
     *
     * @param lcw that is non-null and valid
     */
    private void processLC(LinkControlWord lcw, long timestamp, boolean isTerminator)
    {
        switch(lcw.getOpcode())
        {
            //Calls in-progress on this channel
            case GROUP_VOICE_CHANNEL_USER:
            case MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_USER:
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                else
                {
                    processLCChannelUser(lcw, timestamp);
                }
                break;
            case MOTOROLA_TALK_COMPLETE:
                if(lcw instanceof LCMotorolaTalkComplete tc)
                {
                    getIdentifierCollection().update(tc.getAddress());
                    mTrafficChannelManager.processP1CurrentUser(getCurrentFrequency(), tc.getAddress(), timestamp);
                    closeCurrentCallEvent(timestamp);
                }
                break;

            //Call termination
            case CALL_TERMINATION_OR_CANCELLATION:
                closeCurrentCallEvent(timestamp);

                //Note: we only broadcast an END state if this is a network-commanded channel teardown
                if(lcw instanceof LCCallTermination lcct && lcct.isNetworkCommandedTeardown())
                {
                    broadcast(new DecoderStateEvent(this, Event.END, State.FADE));
                }
                break;

            //Calls in-progress on another channel
            case GROUP_VOICE_CHANNEL_UPDATE:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }

                if(lcw instanceof LCGroupVoiceChannelUpdate vcu)
                {
                    MutableIdentifierCollection mic = getMutableIdentifierCollection(vcu.getGroupAddressA(), timestamp);
                    mTrafficChannelManager.processP1ChannelUpdate(vcu.getChannelA(), null, mic,
                            null, timestamp);

                    if(vcu.hasChannelB())
                    {
                        MutableIdentifierCollection micB = getMutableIdentifierCollection(vcu.getGroupAddressB(), timestamp);
                        mTrafficChannelManager.processP1ChannelUpdate(vcu.getChannelB(), null, micB,
                                null, timestamp);
                    }
                }
                break;
            case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }

                if(lcw instanceof LCGroupVoiceChannelUpdateExplicit vcu)
                {
                    MutableIdentifierCollection mic = getMutableIdentifierCollection(vcu.getGroupAddress(), timestamp);
                    mTrafficChannelManager.processP1ChannelUpdate(vcu.getChannel(), vcu.getServiceOptions(), mic,
                            null, timestamp);
                }
                break;

            //Network configuration messages
            case RFSS_STATUS_BROADCAST:
                if((getCurrentChannel() == null || getCurrentChannel().getDownlinkFrequency() > 0) &&
                        mChannel.isStandardChannel() && lcw instanceof LCRFSSStatusBroadcast sb &&
                        sb.getChannel().getDownlinkFrequency() > 0)
                {
                    setCurrentChannel(sb.getChannel());
                    DecoderLogicalChannelNameIdentifier channelID =
                            DecoderLogicalChannelNameIdentifier.create(sb.getChannel().toString(), Protocol.APCO25);
                    getIdentifierCollection().update(channelID);
                    setCurrentFrequency(sb.getChannel().getDownlinkFrequency());
                    FrequencyConfigurationIdentifier frequencyID = FrequencyConfigurationIdentifier
                            .create(sb.getChannel().getDownlinkFrequency());
                    getIdentifierCollection().update(frequencyID);
                }

                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }

                mNetworkConfigurationMonitor.process(lcw);
                break;
            case RFSS_STATUS_BROADCAST_EXPLICIT:
                if((getCurrentChannel() == null || getCurrentChannel().getDownlinkFrequency() > 0) &&
                        mChannel.isStandardChannel() && lcw instanceof LCRFSSStatusBroadcastExplicit sb &&
                        sb.getChannel().getDownlinkFrequency() > 0)
                {
                    setCurrentChannel(sb.getChannel());
                    DecoderLogicalChannelNameIdentifier channelID =
                            DecoderLogicalChannelNameIdentifier.create(sb.getChannel().toString(), Protocol.APCO25);
                    getIdentifierCollection().update(channelID);
                    setCurrentFrequency(sb.getChannel().getDownlinkFrequency());
                    FrequencyConfigurationIdentifier frequencyID = FrequencyConfigurationIdentifier
                            .create(sb.getChannel().getDownlinkFrequency());
                    getIdentifierCollection().update(frequencyID);
                }

                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }

                mNetworkConfigurationMonitor.process(lcw);
                break;

            case NETWORK_STATUS_BROADCAST:
                if((getCurrentChannel() == null || getCurrentChannel().getDownlinkFrequency() > 0) &&
                        mChannel.isStandardChannel() && lcw instanceof LCNetworkStatusBroadcast sb &&
                        sb.getChannel().getDownlinkFrequency() > 0)
                {
                    setCurrentChannel(sb.getChannel());
                    DecoderLogicalChannelNameIdentifier channelID =
                            DecoderLogicalChannelNameIdentifier.create(sb.getChannel().toString(), Protocol.APCO25);
                    getIdentifierCollection().update(channelID);
                    setCurrentFrequency(sb.getChannel().getDownlinkFrequency());
                    FrequencyConfigurationIdentifier frequencyID = FrequencyConfigurationIdentifier
                            .create(sb.getChannel().getDownlinkFrequency());
                    getIdentifierCollection().update(frequencyID);
                }

                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }

                mNetworkConfigurationMonitor.process(lcw);
                break;
            case NETWORK_STATUS_BROADCAST_EXPLICIT:
                if((getCurrentChannel() == null || getCurrentChannel().getDownlinkFrequency() > 0) &&
                        mChannel.isStandardChannel() && lcw instanceof LCNetworkStatusBroadcastExplicit sb &&
                        sb.getChannel().getDownlinkFrequency() > 0)
                {
                    setCurrentChannel(sb.getChannel());
                    DecoderLogicalChannelNameIdentifier channelID =
                            DecoderLogicalChannelNameIdentifier.create(sb.getChannel().toString(), Protocol.APCO25);
                    getIdentifierCollection().update(channelID);
                    setCurrentFrequency(sb.getChannel().getDownlinkFrequency());
                    FrequencyConfigurationIdentifier frequencyID = FrequencyConfigurationIdentifier
                            .create(sb.getChannel().getDownlinkFrequency());
                    getIdentifierCollection().update(frequencyID);
                }

                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }

                mNetworkConfigurationMonitor.process(lcw);
                break;

            case ADJACENT_SITE_STATUS_BROADCAST:
            case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
            case PROTECTION_PARAMETER_BROADCAST:
            case SECONDARY_CONTROL_CHANNEL_BROADCAST:
            case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
            case SYSTEM_SERVICE_BROADCAST:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                mNetworkConfigurationMonitor.process(lcw);
                break;

            //Patch Group management
            case MOTOROLA_GROUP_REGROUP_ADD:
                mPatchGroupManager.addPatchGroups(lcw.getIdentifiers(), timestamp);
                break;
            case MOTOROLA_GROUP_REGROUP_DELETE:
                mPatchGroupManager.removePatchGroups(lcw.getIdentifiers());
                break;
            case MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_UPDATE:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }

                if(lcw instanceof LCMotorolaGroupRegroupVoiceChannelUpdate vcu)
                {
                    MutableIdentifierCollection mic = getMutableIdentifierCollection(vcu.getSupergroupAddress(), timestamp);
                    mTrafficChannelManager.processP1ChannelUpdate(vcu.getChannel(), vcu.getServiceOptions(), mic,
                            null, timestamp);
                }
                break;
            case MOTOROLA_RADIO_REPROGRAM_HEADER:
            case MOTOROLA_RADIO_REPROGRAM_RECORD:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                break;

            //Other events
            case CALL_ALERT:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.PAGE, "Call Alert");
                break;
            case EXTENDED_FUNCTION_COMMAND:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                if(lcw instanceof LCExtendedFunctionCommand efc)
                {
                    broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.COMMAND, "Function: " +
                            efc.getExtendedFunction() +
                            " Arguments:" + efc.getExtendedFunctionArguments());
                }
                break;
            case EXTENDED_FUNCTION_COMMAND_EXTENDED:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                if(lcw instanceof LCExtendedFunctionCommandExtended efce)
                {
                    broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.COMMAND, "Function: " +
                            efce.getExtendedFunction() + " Arguments:" + efce.getExtendedFunctionArguments());
                }
            case GROUP_AFFILIATION_QUERY:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.QUERY, "Group Affiliation");
                break;
            case MESSAGE_UPDATE:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                if(lcw instanceof LCMessageUpdate mu)
                {
                    broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.SDM,
                            "MSG:" + mu.getShortDataMessage());
                }
                break;
            case MESSAGE_UPDATE_EXTENDED:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                if(lcw instanceof LCMessageUpdateExtended mue)
                {
                    broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.SDM, "MSG:" +
                            mue.getShortDataMessage());
                }
                break;
            case STATUS_QUERY:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.QUERY, "Status");
                break;
            case STATUS_UPDATE:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                if(lcw instanceof LCStatusUpdate su)
                {
                    broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.STATUS,
                            "UNIT:" + su.getUnitStatus() + " USER:" + su.getUserStatus());
                }
                break;
            case STATUS_UPDATE_EXTENDED:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                if(lcw instanceof LCStatusUpdateExtended sue)
                {
                    broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.STATUS, "UNIT:" +
                            sue.getUnitStatus() + " USER:" + sue.getUserStatus());
                }
                break;
            case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                if(lcw instanceof LCTelephoneInterconnectAnswerRequest tiar)
                {
                    broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.PAGE, "Telephone Call:" +
                            tiar.getTelephoneNumber());
                }
                break;
            case UNIT_AUTHENTICATION_COMMAND:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.COMMAND, "Authenticate Unit");
                break;
            case UNIT_REGISTRATION_COMMAND:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.COMMAND, "Unit Registration");
                break;
            case UNIT_TO_UNIT_ANSWER_REQUEST:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.PAGE, "Unit-to-Unit Answer Request");
                break;
            case L3HARRIS_RETURN_TO_CONTROL_CHANNEL:
                if(lcw instanceof LCHarrisReturnToControlChannel)
                {
                    broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.RESPONSE, "L3Harris Opcode 10 - Unknown");
                }
                break;
            case MOTOROLA_EMERGENCY_ALARM_ACTIVATION:
                if(lcw instanceof LCMotorolaEmergencyAlarmActivation)
                {
                    broadcastEvent(lcw.getIdentifiers(), timestamp, DecodeEventType.EMERGENCY, "EMERGENCY ALARM ACTIVATION");
                }
                break;
            case MOTOROLA_UNIT_GPS:
                if(lcw instanceof LCMotorolaUnitGPS gps)
                {
                    mTrafficChannelManager.processP1CurrentUser(getCurrentFrequency(), gps.getLocation(), timestamp);
                    MutableIdentifierCollection mic = getMutableIdentifierCollection(gps.getIdentifiers(), timestamp);

                    PlottableDecodeEvent event = PlottableDecodeEvent.plottableBuilder(DecodeEventType.GPS, timestamp)
                            .location(gps.getGeoPosition())
                            .channel(getCurrentChannel())
                            .details(gps.getLocation().toString())
                            .end(timestamp)
                            .protocol(Protocol.APCO25)
                            .identifiers(mic)
                            .build();
                    broadcast(event);
                }

                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
                break;
            case SOURCE_ID_EXTENSION:
                //Ignore - handled elsewhere
                break;
            default:
                if(isTerminator)
                {
                    closeCurrentCallEvent(timestamp);
                }
//                LOGGING_SUPPRESSOR.info(lcw.getVendor().toString() + lcw.getOpcodeNumber() + lcw.getMessage().toHexString(),
//                        1, "Unrecognized LCW Opcode: " + lcw.getOpcode().name() + " VENDOR:" + lcw.getVendor() +
//                    " OPCODE:" + lcw.getOpcodeNumber() + " MSG:" + lcw.getMessage().toHexString() +
//                                " CHAN:" + getCurrentChannel() + " FREQ:" + getCurrentFrequency());
                break;
        }
    }

    /**
     * Closes the call event on the current channel.
     * @param timestamp
     */
    private void closeCurrentCallEvent(long timestamp)
    {
        mTrafficChannelManager.closeP1CallEvent(getCurrentFrequency(), timestamp);
        getIdentifierCollection().remove(IdentifierClass.USER);
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mNetworkConfigurationMonitor.getActivitySummary());
        sb.append("\n");
        sb.append(mPatchGroupManager.getPatchGroupSummary());
        return sb.toString();
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
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

    @Override
    public void start()
    {
        super.start();
        mPatchGroupManager.clear();

        //Change the default (45-second) traffic channel timeout to 1 second
        if(mChannel.isTrafficChannel())
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
        super.stop();
        mPatchGroupManager.clear();
    }
}
