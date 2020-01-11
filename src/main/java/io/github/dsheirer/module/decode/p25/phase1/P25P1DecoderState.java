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
package io.github.dsheirer.module.decode.p25.phase1;

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
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
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
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCCallTermination;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCMessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCTelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.EncryptionSyncParameters;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCAuthenticationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCIndividualDataServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCLocationRegistrationRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCMessageUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCStatusQueryResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCStatusUpdateRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCUnitAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCUnitToUnitVoiceServiceAnswerResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.isp.AMBTCUnitToUnitVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCIndividualDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCMessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCProtectionParameterBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCStatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCTelephoneInterconnectChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCTelephoneInterconnectChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitToUnitVoiceServiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.pdu.ambtc.osp.AMBTCUnitToUnitVoiceServiceChannelGrantUpdate;
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
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.QueuedResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.RoamingAddressCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.StatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.TelephoneInterconnectVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitToUnitVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp.UnitToUnitVoiceChannelGrantUpdate;
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
public class P25P1DecoderState extends DecoderState implements IChannelEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1DecoderState.class);

    private ChannelType mChannelType;
    private P25P1Decoder.Modulation mModulation;
    private PatchGroupManager mPatchGroupManager = new PatchGroupManager();
    private P25P1NetworkConfigurationMonitor mNetworkConfigurationMonitor;
    private P25TrafficChannelManager mTrafficChannelManager;
    private Listener<ChannelEvent> mChannelEventListener;
    private DecodeEvent mCurrentCallEvent;

    /**
     * Constructs an APCO-25 decoder state with an optional traffic channel manager.
     * @param channel with configuration details
     * @param trafficChannelManager for handling traffic channel grants.
     */
    public P25P1DecoderState(Channel channel, P25TrafficChannelManager trafficChannelManager)
    {
        mChannelType = channel.getChannelType();
        mModulation = ((DecodeConfigP25Phase1)channel.getDecodeConfiguration()).getModulation();
        mNetworkConfigurationMonitor = new P25P1NetworkConfigurationMonitor(mModulation);

        if(trafficChannelManager != null)
        {
            mTrafficChannelManager = trafficChannelManager;
            mChannelEventListener = trafficChannelManager.getChannelEventListener();
        }
        else
        {
            mChannelEventListener = channelEvent -> {
                //do nothing with channel events if we're not configured to process traffic channels
            };
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
        if(iMessage instanceof P25Message)
        {
            P25Message message = (P25Message)iMessage;

            getIdentifierCollection().update(message.getNAC());

            switch(message.getDUID())
            {
                case ALTERNATE_MULTI_BLOCK_TRUNKING_CONTROL:
                    processAMBTC(message);
                    break;
                case HEADER_DATA_UNIT:
                    processHDU((HDUMessage)message);
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
     * Commands the traffic channel manager to process a traffic channel grant and allocate a decoder
     * to process the traffic channel.
     * @param apco25Channel to allocate
     * @param serviceOptions for the channel
     * @param identifierCollection identifying the users of the channel
     * @param opcode that identifies the type of channel grant
     * @param timestamp when the channel grant occurred.
     */
    private void processChannelGrant(APCO25Channel apco25Channel, ServiceOptions serviceOptions,
                                     IdentifierCollection identifierCollection, Opcode opcode, long timestamp)
    {
        if(mTrafficChannelManager != null && apco25Channel.getValue().getFrequencyBand() != null)
        {
            mTrafficChannelManager.processChannelGrant(apco25Channel, serviceOptions, identifierCollection, opcode,
                    timestamp);
        }
    }

    /**
     * Alternate Multi-Block Trunking Control (AMBTC)
     *
     * @param message
     */
    private void processAMBTC(P25Message message)
    {
        if(message instanceof AMBTCMessage && message.isValid())
        {
            AMBTCMessage ambtc = (AMBTCMessage)message;

            switch(ambtc.getHeader().getOpcode())
            {
                case ISP_AUTHENTICATION_RESPONSE:
                    if(message instanceof AMBTCAuthenticationResponse)
                    {
                        AMBTCAuthenticationResponse ar = (AMBTCAuthenticationResponse)ambtc;

                        MutableIdentifierCollection icAuthenticationResponse = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icAuthenticationResponse.remove(IdentifierClass.USER);
                        icAuthenticationResponse.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("AUTHENTICATION:" + ar.getAuthenticationValue())
                            .identifiers(icAuthenticationResponse)
                            .build());
                    }
                    break;
                case ISP_CALL_ALERT_REQUEST:
                    MutableIdentifierCollection icCallAlertRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icCallAlertRequest.remove(IdentifierClass.USER);
                    icCallAlertRequest.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("CALL ALERT")
                        .identifiers(icCallAlertRequest)
                        .build());
                    break;
                case ISP_GROUP_AFFILIATION_REQUEST:
                    MutableIdentifierCollection icGroupAffiliationRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icGroupAffiliationRequest.remove(IdentifierClass.USER);
                    icGroupAffiliationRequest.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("GROUP AFFILIATION")
                        .identifiers(icGroupAffiliationRequest)
                        .build());
                    break;
                case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                    if(ambtc instanceof AMBTCIndividualDataServiceRequest)
                    {
                        AMBTCIndividualDataServiceRequest idsr = (AMBTCIndividualDataServiceRequest)ambtc;

                        MutableIdentifierCollection icDataRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icDataRequest.remove(IdentifierClass.USER);
                        icDataRequest.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("INDIVIDUAL DATA SERVICE " + idsr.getDataServiceOptions())
                            .identifiers(icDataRequest)
                            .build());
                    }
                    break;
                case ISP_LOCATION_REGISTRATION_REQUEST:
                    if(ambtc instanceof AMBTCLocationRegistrationRequest)
                    {
                        AMBTCLocationRegistrationRequest lrr = (AMBTCLocationRegistrationRequest)ambtc;

                        MutableIdentifierCollection icLocationRegistrationRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icLocationRegistrationRequest.remove(IdentifierClass.USER);
                        icLocationRegistrationRequest.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("LOCATION REGISTRATION - UNIQUE ID:" + lrr.getSourceId())
                            .identifiers(icLocationRegistrationRequest)
                            .build());
                    }
                    break;
                case ISP_MESSAGE_UPDATE_REQUEST:
                    if(ambtc instanceof AMBTCMessageUpdateRequest)
                    {
                        AMBTCMessageUpdateRequest mur = (AMBTCMessageUpdateRequest)ambtc;

                        MutableIdentifierCollection icMessageUpdateRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icMessageUpdateRequest.remove(IdentifierClass.USER);
                        icMessageUpdateRequest.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.SDM.toString())
                            .details("MESSAGE:" + mur.getShortDataMessage())
                            .identifiers(icMessageUpdateRequest)
                            .build());
                    }
                    break;
                case ISP_ROAMING_ADDRESS_REQUEST:
                    MutableIdentifierCollection icRoamingAddressRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRoamingAddressRequest.remove(IdentifierClass.USER);
                    icRoamingAddressRequest.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("ROAMING ADDRESS")
                        .identifiers(icRoamingAddressRequest)
                        .build());
                    break;
                case ISP_STATUS_QUERY_REQUEST:
                    MutableIdentifierCollection icStatusQuery = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icStatusQuery.remove(IdentifierClass.USER);
                    icStatusQuery.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("STATUS QUERY")
                        .identifiers(icStatusQuery)
                        .build());
                    break;
                case ISP_STATUS_QUERY_RESPONSE:
                    if(ambtc instanceof AMBTCStatusQueryResponse)
                    {
                        MutableIdentifierCollection icStatusQueryResponse = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icStatusQueryResponse.remove(IdentifierClass.USER);
                        icStatusQueryResponse.update(ambtc.getIdentifiers());

                        AMBTCStatusQueryResponse sqr = (AMBTCStatusQueryResponse)ambtc;
                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + sqr.getUnitStatus() + " USER:" + sqr.getUserStatus())
                            .identifiers(icStatusQueryResponse)
                            .build());
                    }
                    break;
                case ISP_STATUS_UPDATE_REQUEST:
                    if(ambtc instanceof AMBTCStatusUpdateRequest)
                    {
                        AMBTCStatusUpdateRequest sur = (AMBTCStatusUpdateRequest)ambtc;

                        MutableIdentifierCollection icStatusUpdateRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icStatusUpdateRequest.remove(IdentifierClass.USER);
                        icStatusUpdateRequest.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + sur.getUnitStatus() + " USER:" + sur.getUserStatus())
                            .identifiers(icStatusUpdateRequest)
                            .build());
                    }
                    break;
                case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                    if(ambtc instanceof AMBTCUnitAcknowledgeResponse)
                    {
                        AMBTCUnitAcknowledgeResponse uar = (AMBTCUnitAcknowledgeResponse)ambtc;

                        MutableIdentifierCollection icUnitAcknowledge = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icUnitAcknowledge.remove(IdentifierClass.USER);
                        icUnitAcknowledge.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("ACKNOWLEDGE:" + uar.getAcknowledgedService())
                            .identifiers(icUnitAcknowledge)
                            .build());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                    if(ambtc instanceof AMBTCUnitToUnitVoiceServiceRequest)
                    {
                        AMBTCUnitToUnitVoiceServiceRequest uuvsr = (AMBTCUnitToUnitVoiceServiceRequest)ambtc;

                        MutableIdentifierCollection icUnitVoiceService = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icUnitVoiceService.remove(IdentifierClass.USER);
                        icUnitVoiceService.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("UNIT-2-UNIT VOICE SERVICE " + uuvsr.getVoiceServiceOptions())
                            .identifiers(icUnitVoiceService)
                            .build());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                    if(ambtc instanceof AMBTCUnitToUnitVoiceServiceAnswerResponse)
                    {
                        AMBTCUnitToUnitVoiceServiceAnswerResponse uuvsar = (AMBTCUnitToUnitVoiceServiceAnswerResponse)ambtc;

                        MutableIdentifierCollection icUnitAnswerResponse = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icUnitAnswerResponse.remove(IdentifierClass.USER);
                        icUnitAnswerResponse.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(uuvsar.getAnswerResponse() + " UNIT-2-UNIT VOICE SERVICE " +
                                uuvsar.getVoiceServiceOptions())
                            .identifiers(icUnitAnswerResponse)
                            .build());
                    }
                    break;

                //Network configuration messages
                case OSP_ADJACENT_STATUS_BROADCAST:
                    mNetworkConfigurationMonitor.process(ambtc);
                    break;
                case OSP_NETWORK_STATUS_BROADCAST:
                    mNetworkConfigurationMonitor.process(ambtc);
                    break;
                case OSP_RFSS_STATUS_BROADCAST:
                    mNetworkConfigurationMonitor.process(ambtc);
                    break;

                //Channel grants
                case OSP_GROUP_DATA_CHANNEL_GRANT:
                    if(ambtc instanceof AMBTCGroupDataChannelGrant)
                    {
                        AMBTCGroupDataChannelGrant gdcg = (AMBTCGroupDataChannelGrant)ambtc;

                        MutableIdentifierCollection identifierCollection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifierCollection.remove(IdentifierClass.USER);
                        identifierCollection.update(gdcg.getIdentifiers());
                        processChannelGrant(gdcg.getChannel(), gdcg.getDataServiceOptions(),
                                identifierCollection, ambtc.getHeader().getOpcode(),
                                ambtc.getTimestamp());
                    }
                    break;
                case OSP_GROUP_VOICE_CHANNEL_GRANT:
                    if(ambtc instanceof AMBTCGroupVoiceChannelGrant)
                    {
                        AMBTCGroupVoiceChannelGrant gvcg = (AMBTCGroupVoiceChannelGrant)ambtc;

                        MutableIdentifierCollection identifierCollection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifierCollection.remove(IdentifierClass.USER);
                        identifierCollection.update(gvcg.getIdentifiers());

                        processChannelGrant(gvcg.getChannel(), gvcg.getVoiceServiceOptions(),
                                identifierCollection, ambtc.getHeader().getOpcode(),
                                ambtc.getTimestamp());
                    }
                    break;
                case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
                    if(ambtc instanceof AMBTCIndividualDataChannelGrant)
                    {
                        AMBTCIndividualDataChannelGrant idcg = (AMBTCIndividualDataChannelGrant)ambtc;

                        MutableIdentifierCollection identifierCollection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifierCollection.remove(IdentifierClass.USER);
                        identifierCollection.update(idcg.getIdentifiers());

                        processChannelGrant(idcg.getChannel(), idcg.getDataServiceOptions(),
                                identifierCollection, ambtc.getHeader().getOpcode(),
                                ambtc.getTimestamp());
                    }
                    break;
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                    if(ambtc instanceof AMBTCTelephoneInterconnectChannelGrant)
                    {
                        AMBTCTelephoneInterconnectChannelGrant ticg = (AMBTCTelephoneInterconnectChannelGrant)ambtc;

                        MutableIdentifierCollection identifierCollection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifierCollection.remove(IdentifierClass.USER);
                        identifierCollection.update(ticg.getIdentifiers());

                        processChannelGrant(ticg.getChannel(), ticg.getVoiceServiceOptions(),
                                identifierCollection, ambtc.getHeader().getOpcode(),
                                ambtc.getTimestamp());
                    }
                    break;
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                    if(ambtc instanceof AMBTCTelephoneInterconnectChannelGrantUpdate)
                    {
                        AMBTCTelephoneInterconnectChannelGrantUpdate ticgu = (AMBTCTelephoneInterconnectChannelGrantUpdate)ambtc;

                        MutableIdentifierCollection identifierCollection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifierCollection.remove(IdentifierClass.USER);
                        identifierCollection.update(ticgu.getIdentifiers());

                        processChannelGrant(ticgu.getChannel(), ticgu.getVoiceServiceOptions(),
                                identifierCollection, ambtc.getHeader().getOpcode(),
                                ambtc.getTimestamp());
                    }
                    break;
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                    if(ambtc instanceof AMBTCUnitToUnitVoiceServiceChannelGrant)
                    {
                        AMBTCUnitToUnitVoiceServiceChannelGrant uuvscg = (AMBTCUnitToUnitVoiceServiceChannelGrant)ambtc;

                        MutableIdentifierCollection identifierCollection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifierCollection.remove(IdentifierClass.USER);
                        identifierCollection.update(uuvscg.getIdentifiers());

                        processChannelGrant(uuvscg.getChannel(), uuvscg.getVoiceServiceOptions(),
                                identifierCollection, ambtc.getHeader().getOpcode(),
                                ambtc.getTimestamp());
                    }
                    break;
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                    if(ambtc instanceof AMBTCUnitToUnitVoiceServiceChannelGrantUpdate)
                    {
                        AMBTCUnitToUnitVoiceServiceChannelGrantUpdate uuvscgu = (AMBTCUnitToUnitVoiceServiceChannelGrantUpdate)ambtc;

                        MutableIdentifierCollection identifierCollection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifierCollection.remove(IdentifierClass.USER);
                        identifierCollection.update(uuvscgu.getIdentifiers());

                        processChannelGrant(uuvscgu.getChannel(), uuvscgu.getVoiceServiceOptions(),
                                identifierCollection, ambtc.getHeader().getOpcode(),
                                ambtc.getTimestamp());
                    }
                    break;
                case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                    MutableIdentifierCollection icUUAnswerRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icUUAnswerRequest.remove(IdentifierClass.USER);
                    icUUAnswerRequest.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("ANSWER REQUEST")
                        .identifiers(icUUAnswerRequest)
                        .build());
                case OSP_CALL_ALERT:
                    MutableIdentifierCollection icCallAlert = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icCallAlert.remove(IdentifierClass.USER);
                    icCallAlert.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("CALL ALERT")
                        .identifiers(icCallAlert)
                        .build());
                    break;
                case OSP_GROUP_AFFILIATION_QUERY:
                    MutableIdentifierCollection icGroupAffiliationQuery = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icGroupAffiliationQuery.remove(IdentifierClass.USER);
                    icGroupAffiliationQuery.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("GROUP AFFILIATION")
                        .identifiers(icGroupAffiliationQuery)
                        .build());
                    break;
                case OSP_GROUP_AFFILIATION_RESPONSE:
                    if(ambtc instanceof AMBTCGroupAffiliationResponse)
                    {
                        AMBTCGroupAffiliationResponse gar = (AMBTCGroupAffiliationResponse)ambtc;

                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("AFFILIATION GROUP:" + gar.getGroupId() +
                                " ANNOUNCEMENT GROUP:" + gar.getAnnouncementGroupId())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_MESSAGE_UPDATE:
                    if(ambtc instanceof AMBTCMessageUpdate)
                    {
                        AMBTCMessageUpdate mu = (AMBTCMessageUpdate)ambtc;

                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.SDM.toString())
                            .details("MESSAGE:" + mu.getShortDataMessage())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_PROTECTION_PARAMETER_BROADCAST:
                    if(ambtc instanceof AMBTCProtectionParameterBroadcast)
                    {
                        AMBTCProtectionParameterBroadcast ppb = (AMBTCProtectionParameterBroadcast)ambtc;

                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("USE ENCRYPTION " + ppb.getEncryptionKey() +
                                " OUTBOUND MI:" + ppb.getOutboundMessageIndicator() +
                                " INBOUND MI:" + ppb.getInboundMessageIndicator())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_ROAMING_ADDRESS_UPDATE:
                    MutableIdentifierCollection icRoamingAddressUpdate = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRoamingAddressUpdate.remove(IdentifierClass.USER);
                    icRoamingAddressUpdate.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("ROAMING ADDRESS UPDATE")
                        .identifiers(icRoamingAddressUpdate)
                        .build());
                    break;
                case OSP_ROAMING_ADDRESS_COMMAND:
                    MutableIdentifierCollection icRoamingAddressCommand = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRoamingAddressCommand.remove(IdentifierClass.USER);
                    icRoamingAddressCommand.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("ROAMING ADDRESS")
                        .identifiers(icRoamingAddressCommand)
                        .build());
                    break;
                case OSP_STATUS_QUERY:
                    MutableIdentifierCollection icOStatusQuery = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icOStatusQuery.remove(IdentifierClass.USER);
                    icOStatusQuery.update(ambtc.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("STATUS")
                        .identifiers(icOStatusQuery)
                        .build());
                    break;
                case OSP_STATUS_UPDATE:
                    if(ambtc instanceof AMBTCStatusUpdate)
                    {
                        AMBTCStatusUpdate su = (AMBTCStatusUpdate)ambtc;

                        MutableIdentifierCollection icStatusUpdate = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icStatusUpdate.remove(IdentifierClass.USER);
                        icStatusUpdate.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + su.getUnitStatus() + " USER:" + su.getUserStatus())
                            .identifiers(icStatusUpdate)
                            .build());
                    }
                    break;
                case OSP_UNIT_REGISTRATION_RESPONSE:
                    if(ambtc instanceof AMBTCUnitRegistrationResponse)
                    {
                        AMBTCUnitRegistrationResponse urr = (AMBTCUnitRegistrationResponse)ambtc;

                        MutableIdentifierCollection icUnitRegistrationResponse = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        icUnitRegistrationResponse.remove(IdentifierClass.USER);
                        icUnitRegistrationResponse.update(ambtc.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details(urr.getResponse() + " UNIT REGISTRATION")
                            .identifiers(icUnitRegistrationResponse)
                            .build());
                    }
                    break;
                default:
                    mLog.debug("Unrecognized AMBTC Opcode: " + ambtc.getHeader().getOpcode().name());
                    break;
            }
        }

        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CONTROL));
    }

    /**
     * Processes a Header Data Unit message and starts a new call event.
     */
    private void processHDU(HDUMessage message)
    {
        if(message.isValid())
        {
            HeaderData headerData = message.getHeaderData();

            if(headerData.isValid())
            {
                closeCurrentCallEvent(message.getTimestamp());

                for(Identifier identifier : headerData.getIdentifiers())
                {
                    //Add to the identifier collection after filtering through the patch group manager
                    getIdentifierCollection().update(mPatchGroupManager.update(identifier));
                }

                updateCurrentCall(headerData.isEncryptedAudio() ? DecodeEventType.CALL_ENCRYPTED :
                    DecodeEventType.CALL, null, message.getTimestamp());

                return;
            }
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
     * Trunking Signalling Block (TSBK)
     *
     * @param message
     */
    private void processTSBK(P25Message message)
    {
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CONTROL));

        if(message.isValid() && message instanceof TSBKMessage)
        {
            TSBKMessage tsbk = (TSBKMessage)message;

            switch(tsbk.getOpcode())
            {
                //Channel Grant messages
                case OSP_GROUP_DATA_CHANNEL_GRANT:
                    if(tsbk instanceof GroupDataChannelGrant)
                    {
                        GroupDataChannelGrant gdcg = (GroupDataChannelGrant)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiers = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiers.remove(IdentifierClass.USER);
                        for(Identifier identifier : gdcg.getIdentifiers())
                        {
                            identifiers.update(mPatchGroupManager.update(identifier));
                        }

                        processChannelGrant(gdcg.getChannel(), gdcg.getDataServiceOptions(),
                                identifiers, tsbk.getOpcode(), gdcg.getTimestamp());
                    }
                    break;
                case OSP_GROUP_VOICE_CHANNEL_GRANT:
                    if(tsbk instanceof GroupVoiceChannelGrant)
                    {
                        GroupVoiceChannelGrant gvcg = (GroupVoiceChannelGrant)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiers = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiers.remove(IdentifierClass.USER);
                        for(Identifier identifier : gvcg.getIdentifiers())
                        {
                            identifiers.update(mPatchGroupManager.update(identifier));
                        }

                        processChannelGrant(gvcg.getChannel(), gvcg.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), gvcg.getTimestamp());
                    }
                    break;
                case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                    if(tsbk instanceof GroupVoiceChannelGrantUpdate)
                    {
                        GroupVoiceChannelGrantUpdate gvcgu = (GroupVoiceChannelGrantUpdate)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiersA = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiersA.remove(IdentifierClass.USER);
                        identifiersA.update(mPatchGroupManager.update(gvcgu.getGroupAddressA()));

                        processChannelGrant(gvcgu.getChannelA(), null, identifiersA,
                                tsbk.getOpcode(), gvcgu.getTimestamp());

                        if(gvcgu.hasGroupB())
                        {
                            //Make a copy of current identifiers and remove current user identifiers and replace from message
                            MutableIdentifierCollection identifiersB = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                            identifiersB.remove(IdentifierClass.USER);
                            identifiersB.update(mPatchGroupManager.update(gvcgu.getGroupAddressB()));

                            processChannelGrant(gvcgu.getChannelB(), null, identifiersB,
                                    tsbk.getOpcode(), gvcgu.getTimestamp());
                        }
                    }
                    break;
                case OSP_GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                    if(tsbk instanceof GroupVoiceChannelGrantUpdateExplicit)
                    {
                        GroupVoiceChannelGrantUpdateExplicit gvcgue = (GroupVoiceChannelGrantUpdateExplicit)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiers = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiers.remove(IdentifierClass.USER);
                        identifiers.update(mPatchGroupManager.update(gvcgue.getGroupAddress()));

                        processChannelGrant(gvcgue.getChannel(), gvcgue.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), gvcgue.getTimestamp());
                    }
                    break;
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                    if(tsbk instanceof UnitToUnitVoiceChannelGrant)
                    {
                        UnitToUnitVoiceChannelGrant uuvcg = (UnitToUnitVoiceChannelGrant)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiers = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiers.remove(IdentifierClass.USER);
                        identifiers.update(uuvcg.getIdentifiers());

                        processChannelGrant(uuvcg.getChannel(), null, identifiers,
                                tsbk.getOpcode(), uuvcg.getTimestamp());
                    }
                    break;
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                    if(tsbk instanceof UnitToUnitVoiceChannelGrantUpdate)
                    {
                        UnitToUnitVoiceChannelGrantUpdate uuvcgu = (UnitToUnitVoiceChannelGrantUpdate)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiers = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiers.remove(IdentifierClass.USER);
                        identifiers.update(uuvcgu.getIdentifiers());

                        processChannelGrant(uuvcgu.getChannel(), null, identifiers,
                                tsbk.getOpcode(), uuvcgu.getTimestamp());
                    }
                    break;
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                    if(tsbk instanceof TelephoneInterconnectVoiceChannelGrant)
                    {
                        TelephoneInterconnectVoiceChannelGrant tivcg = (TelephoneInterconnectVoiceChannelGrant)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiers = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiers.remove(IdentifierClass.USER);
                        identifiers.update(tivcg.getIdentifiers());

                        processChannelGrant(tivcg.getChannel(), tivcg.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), tivcg.getTimestamp());
                    }
                    break;
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                    if(tsbk instanceof TelephoneInterconnectVoiceChannelGrantUpdate)
                    {
                        TelephoneInterconnectVoiceChannelGrantUpdate tivcgu = (TelephoneInterconnectVoiceChannelGrantUpdate)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiers = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiers.remove(IdentifierClass.USER);
                        identifiers.update(tivcgu.getIdentifiers());

                        processChannelGrant(tivcgu.getChannel(), tivcgu.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), tivcgu.getTimestamp());
                    }
                    break;
                case OSP_SNDCP_DATA_CHANNEL_GRANT:
                    if(tsbk instanceof SNDCPDataChannelGrant)
                    {
                        SNDCPDataChannelGrant dcg = (SNDCPDataChannelGrant)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiers = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiers.remove(IdentifierClass.USER);
                        identifiers.update(dcg.getIdentifiers());

                        processChannelGrant(dcg.getChannel(), dcg.getServiceOptions(),
                                identifiers, tsbk.getOpcode(), dcg.getTimestamp());
                    }
                    break;
                case MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT:
                    if(tsbk instanceof PatchGroupVoiceChannelGrant)
                    {
                        PatchGroupVoiceChannelGrant pgvcg = (PatchGroupVoiceChannelGrant)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiers = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiers.remove(IdentifierClass.USER);
                        for(Identifier identifier : pgvcg.getIdentifiers())
                        {
                            identifiers.update(mPatchGroupManager.update(identifier));
                        }

                        processChannelGrant(pgvcg.getChannel(), pgvcg.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), pgvcg.getTimestamp());
                    }
                    break;
                case MOTOROLA_OSP_PATCH_GROUP_CHANNEL_GRANT_UPDATE:
                    if(tsbk instanceof PatchGroupVoiceChannelGrantUpdate)
                    {
                        PatchGroupVoiceChannelGrantUpdate pgvcgu = (PatchGroupVoiceChannelGrantUpdate)tsbk;

                        //Make a copy of current identifiers and remove current user identifiers and replace from message
                        MutableIdentifierCollection identifiersPG1 = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        identifiersPG1.remove(IdentifierClass.USER);
                        identifiersPG1.update(mPatchGroupManager.update(pgvcgu.getPatchGroup1()));

                        processChannelGrant(pgvcgu.getChannel1(), null, identifiersPG1,
                                tsbk.getOpcode(), pgvcgu.getTimestamp());

                        if(pgvcgu.hasPatchGroup2())
                        {
                            //Make a copy of current identifiers and remove current user identifiers and replace from message
                            MutableIdentifierCollection identifiersPG2 = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                            identifiersPG2.remove(IdentifierClass.USER);
                            identifiersPG2.update(mPatchGroupManager.update(pgvcgu.getPatchGroup2()));

                            processChannelGrant(pgvcgu.getChannel2(), null,
                                    identifiersPG2, tsbk.getOpcode(), pgvcgu.getTimestamp());
                        }
                    }
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
                case OSP_RFSS_STATUS_BROADCAST:
                case OSP_NETWORK_STATUS_BROADCAST:
                case OSP_ADJACENT_STATUS_BROADCAST:
                case OSP_IDENTIFIER_UPDATE:
                case OSP_PROTECTION_PARAMETER_BROADCAST:
                case OSP_PROTECTION_PARAMETER_UPDATE:
                    mNetworkConfigurationMonitor.process(tsbk);
                    break;

                case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                    MutableIdentifierCollection icUnitAnswerRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icUnitAnswerRequest.remove(IdentifierClass.USER);
                    icUnitAnswerRequest.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("UNIT-TO-UNIT ANSWER REQUEST")
                        .identifiers(icUnitAnswerRequest)
                        .build());
                    break;
                case OSP_TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                    if(tsbk instanceof TelephoneInterconnectAnswerRequest)
                    {
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        TelephoneInterconnectAnswerRequest tiar = (TelephoneInterconnectAnswerRequest)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.PAGE.toString())
                            .details("TELEPHONE ANSWER REQUEST: " + tiar.getTelephoneNumber())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_SNDCP_DATA_PAGE_REQUEST:
                    MutableIdentifierCollection icDataPage = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icDataPage.remove(IdentifierClass.USER);
                    icDataPage.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("SNDCP DATA PAGE REQUEST")
                        .identifiers(icDataPage)
                        .build());
                    break;
                case OSP_STATUS_UPDATE:
                    if(tsbk instanceof StatusUpdate)
                    {
                        StatusUpdate su = (StatusUpdate)tsbk;

                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + su.getUnitStatus() + " USER:" + su.getUserStatus())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_STATUS_QUERY:
                    MutableIdentifierCollection icStatusQuery = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icStatusQuery.remove(IdentifierClass.USER);
                    icStatusQuery.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("STATUS")
                        .identifiers(icStatusQuery)
                        .build());
                    break;
                case OSP_MESSAGE_UPDATE:
                    if(tsbk instanceof MessageUpdate)
                    {
                        MessageUpdate mu = (MessageUpdate)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.SDM.toString())
                            .details("MSG:" + mu.getShortDataMessage())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_RADIO_UNIT_MONITOR_COMMAND:
                    MutableIdentifierCollection icUnitMonitor = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icUnitMonitor.remove(IdentifierClass.USER);
                    icUnitMonitor.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("RADIO UNIT MONITOR")
                        .identifiers(icUnitMonitor)
                        .build());
                    break;
                case OSP_CALL_ALERT:
                    MutableIdentifierCollection icCallAlert = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icCallAlert.remove(IdentifierClass.USER);
                    icCallAlert.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("CALL ALERT")
                        .identifiers(icCallAlert)
                        .build());
                    break;
                case OSP_ACKNOWLEDGE_RESPONSE:
                    if(tsbk instanceof AcknowledgeResponse)
                    {
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        AcknowledgeResponse ar = (AcknowledgeResponse)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("ACKNOWLEDGE " + ar.getAcknowledgedServiceType().getDescription())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_QUEUED_RESPONSE:
                    if(tsbk instanceof QueuedResponse)
                    {
                        QueuedResponse qr = (QueuedResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("QUEUED: " + qr.getQueuedResponseServiceType().getDescription() +
                                " REASON: " + qr.getQueuedResponseReason() +
                                " INFO: " + qr.getAdditionalInfo())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_EXTENDED_FUNCTION_COMMAND:
                    if(tsbk instanceof ExtendedFunctionCommand)
                    {
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        ExtendedFunctionCommand efc = (ExtendedFunctionCommand)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.COMMAND.toString())
                            .details("EXTENDED FUNCTION: " + efc.getExtendedFunction() +
                                " ARGUMENTS:" + efc.getArguments())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_DENY_RESPONSE:
                    if(tsbk instanceof DenyResponse)
                    {
                        DenyResponse dr = (DenyResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("DENY: " + dr.getDeniedServiceType().getDescription() +
                                " REASON: " + dr.getDenyReason() + " - INFO: " + dr.getAdditionalInfo())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_GROUP_AFFILIATION_RESPONSE:
                    if(tsbk instanceof GroupAffiliationResponse)
                    {
                        GroupAffiliationResponse gar = (GroupAffiliationResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(gar.getAffiliationResponse() +
                                " AFFILIATION GROUP: " + gar.getGroupAddress() +
                                (gar.isGlobalAffiliation() ? " (GLOBAL)" : " (LOCAL)") +
                                " ANNOUNCEMENT GROUP:" + gar.getAnnouncementGroupAddress())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_GROUP_AFFILIATION_QUERY:
                    MutableIdentifierCollection icGroupAffiliationQuery = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icGroupAffiliationQuery.remove(IdentifierClass.USER);
                    icGroupAffiliationQuery.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("GROUP AFFILIATION")
                        .identifiers(icGroupAffiliationQuery)
                        .build());
                    break;
                case OSP_LOCATION_REGISTRATION_RESPONSE:
                    if(tsbk instanceof LocationRegistrationResponse)
                    {
                        LocationRegistrationResponse lrr = (LocationRegistrationResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details(lrr.getResponse() + " LOCATION REGISTRATION - GROUP:" + lrr.getGroupAddress())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_UNIT_REGISTRATION_RESPONSE:
                    if(tsbk instanceof UnitRegistrationResponse)
                    {
                        UnitRegistrationResponse urr = (UnitRegistrationResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details(urr.getResponse() + " UNIT REGISTRATION - UNIT ID:" + urr.getTargetUniqueId())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case OSP_UNIT_REGISTRATION_COMMAND:
                    MutableIdentifierCollection icUnitRegistration = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icUnitRegistration.remove(IdentifierClass.USER);
                    icUnitRegistration.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("UNIT REGISTRATION")
                        .identifiers(icUnitRegistration)
                        .build());
                    break;
                case OSP_AUTHENTICATION_COMMAND:
                    MutableIdentifierCollection icAuthenticationCommand = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icAuthenticationCommand.remove(IdentifierClass.USER);
                    icAuthenticationCommand.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("AUTHENTICATE")
                        .identifiers(icAuthenticationCommand)
                        .build());
                    break;
                case OSP_UNIT_DEREGISTRATION_ACKNOWLEDGE:
                    MutableIdentifierCollection icUnitDeregistration = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icUnitDeregistration.remove(IdentifierClass.USER);
                    icUnitDeregistration.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.DEREGISTER.toString())
                        .details("ACKNOWLEDGE UNIT DE-REGISTRATION")
                        .identifiers(icUnitDeregistration)
                        .build());
                    break;
                case OSP_ROAMING_ADDRESS_COMMAND:
                    if(tsbk instanceof RoamingAddressCommand)
                    {
                        RoamingAddressCommand rac = (RoamingAddressCommand)tsbk;

                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.COMMAND.toString())
                            .details(rac.getStackOperation() + " ROAMING ADDRESS")
                            .identifiers(ic)
                            .build());
                    }
                    break;

                //MOTOROLA PATCH GROUP OPCODES
                case MOTOROLA_OSP_PATCH_GROUP_ADD:
                    mPatchGroupManager.addPatchGroups(tsbk.getIdentifiers());
                    break;
                case MOTOROLA_OSP_PATCH_GROUP_DELETE:
                    mPatchGroupManager.removePatchGroups(tsbk.getIdentifiers());
                    break;

                //STANDARD - INBOUND OPCODES
                case ISP_GROUP_VOICE_SERVICE_REQUEST:
                    if(tsbk instanceof GroupVoiceServiceRequest)
                    {
                        GroupVoiceServiceRequest gvsr = (GroupVoiceServiceRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("GROUP VOICE SERVICE " + gvsr.getVoiceServiceOptions())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                    if(tsbk instanceof UnitToUnitVoiceServiceRequest)
                    {
                        UnitToUnitVoiceServiceRequest uuvsr = (UnitToUnitVoiceServiceRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("UNIT-2-UNIT VOICE SERVICE " + uuvsr.getVoiceServiceOptions())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                    if(tsbk instanceof UnitToUnitVoiceServiceAnswerResponse)
                    {
                        UnitToUnitVoiceServiceAnswerResponse uuvsar = (UnitToUnitVoiceServiceAnswerResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(uuvsar.getAnswerResponse() + " UNIT-2-UNIT VOICE SERVICE " + uuvsar.getVoiceServiceOptions())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_TELEPHONE_INTERCONNECT_PSTN_REQUEST:
                    MutableIdentifierCollection icInterconnect = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icInterconnect.remove(IdentifierClass.USER);
                    icInterconnect.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("TELEPHONE INTERCONNECT")
                        .identifiers(icInterconnect)
                        .build());
                    break;
                case ISP_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
                    if(tsbk instanceof TelephoneInterconnectAnswerResponse)
                    {
                        TelephoneInterconnectAnswerResponse tiar = (TelephoneInterconnectAnswerResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(tiar.getAnswerResponse() + " TELEPHONE INTERCONNECT " + tiar.getVoiceServiceOptions())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                    if(tsbk instanceof IndividualDataServiceRequest)
                    {
                        IndividualDataServiceRequest idsr = (IndividualDataServiceRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("INDIVIDUAL DATA SERVICE " + idsr.getVoiceServiceOptions())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_GROUP_DATA_SERVICE_REQUEST:
                    if(tsbk instanceof GroupDataServiceRequest)
                    {
                        GroupDataServiceRequest gdsr = (GroupDataServiceRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("GROUP DATA SERVICE " + gdsr.getVoiceServiceOptions())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_SNDCP_DATA_CHANNEL_REQUEST:
                    if(tsbk instanceof SNDCPDataChannelRequest)
                    {
                        SNDCPDataChannelRequest sdcr = (SNDCPDataChannelRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("SNDCP DATA CHANNEL " + sdcr.getDataServiceOptions())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_SNDCP_DATA_PAGE_RESPONSE:
                    if(tsbk instanceof SNDCPDataPageResponse)
                    {
                        SNDCPDataPageResponse sdpr = (SNDCPDataPageResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(sdpr.getAnswerResponse() + " SNDCP DATA " + sdpr.getDataServiceOptions())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_SNDCP_RECONNECT_REQUEST:
                    if(tsbk instanceof SNDCPReconnectRequest)
                    {
                        SNDCPReconnectRequest srr = (SNDCPReconnectRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("SNDCP RECONNECT " + (srr.hasDataToSend() ? "- DATA TO SEND " : "")
                                + srr.getDataServiceOptions())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_STATUS_UPDATE_REQUEST:
                    if(tsbk instanceof StatusUpdateRequest)
                    {
                        StatusUpdateRequest sur = (StatusUpdateRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + sur.getUnitStatus() + " USER:" + sur.getUserStatus())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_STATUS_QUERY_RESPONSE:
                    if(tsbk instanceof StatusQueryResponse)
                    {
                        StatusQueryResponse sqr = (StatusQueryResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + sqr.getUnitStatus() + " USER:" + sqr.getUserStatus())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_STATUS_QUERY_REQUEST:
                    MutableIdentifierCollection icStatusQueryRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icStatusQueryRequest.remove(IdentifierClass.USER);
                    icStatusQueryRequest.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("UNIT AND USER STATUS")
                        .identifiers(icStatusQueryRequest)
                        .build());
                    break;
                case ISP_MESSAGE_UPDATE_REQUEST:
                    if(tsbk instanceof MessageUpdateRequest)
                    {
                        MessageUpdateRequest mur = (MessageUpdateRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.SDM.toString())
                            .details("MESSAGE:" + mur.getShortDataMessage())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_RADIO_UNIT_MONITOR_REQUEST:
                    MutableIdentifierCollection icUnitMonitorRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icUnitMonitorRequest.remove(IdentifierClass.USER);
                    icUnitMonitorRequest.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("RADIO UNIT MONITOR")
                        .identifiers(icUnitMonitorRequest)
                        .build());
                    break;
                case ISP_CALL_ALERT_REQUEST:
                    MutableIdentifierCollection icCallAlertRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icCallAlertRequest.remove(IdentifierClass.USER);
                    icCallAlertRequest.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("CALL ALERT")
                        .identifiers(icCallAlertRequest)
                        .build());
                    break;
                case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                    if(tsbk instanceof UnitAcknowledgeResponse)
                    {
                        UnitAcknowledgeResponse uar = (UnitAcknowledgeResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("UNIT ACKNOWLEDGE:" + uar.getAcknowledgedServiceType().getDescription())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_CANCEL_SERVICE_REQUEST:
                    if(tsbk instanceof CancelServiceRequest)
                    {
                        CancelServiceRequest csr = (CancelServiceRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("CANCEL SERVICE:" + csr.getServiceType() +
                                " REASON:" + csr.getCancelReason() + (csr.hasAdditionalInformation() ?
                                " INFO:" + csr.getAdditionalInformation() : ""))
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_EXTENDED_FUNCTION_RESPONSE:
                    if(tsbk instanceof ExtendedFunctionResponse)
                    {
                        ExtendedFunctionResponse efr = (ExtendedFunctionResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("EXTENDED FUNCTION:" + efr.getExtendedFunction() +
                                " ARGUMENTS:" + efr.getArguments())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_EMERGENCY_ALARM_REQUEST:
                    MutableIdentifierCollection icAlarm = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icAlarm.remove(IdentifierClass.USER);
                    icAlarm.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("EMERGENCY ALARM")
                        .identifiers(icAlarm)
                        .build());
                    break;
                case ISP_GROUP_AFFILIATION_REQUEST:
                    MutableIdentifierCollection icGroupAffiliationRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icGroupAffiliationRequest.remove(IdentifierClass.USER);
                    icGroupAffiliationRequest.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("GROUP AFFILIATION")
                        .identifiers(icGroupAffiliationRequest)
                        .build());
                    break;
                case ISP_GROUP_AFFILIATION_QUERY_RESPONSE:
                    if(tsbk instanceof GroupAffiliationQueryResponse)
                    {
                        GroupAffiliationQueryResponse gaqr = (GroupAffiliationQueryResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("AFFILIATION - GROUP:" + gaqr.getGroupAddress() +
                                " ANNOUNCEMENT GROUP:" + gaqr.getAnnouncementGroupAddress())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_UNIT_DE_REGISTRATION_REQUEST:
                    MutableIdentifierCollection icDeregistrationRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icDeregistrationRequest.remove(IdentifierClass.USER);
                    icDeregistrationRequest.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.DEREGISTER.toString())
                        .details("UNIT DE-REGISTRATION REQUEST")
                        .identifiers(icDeregistrationRequest)
                        .build());
                    break;
                case ISP_UNIT_REGISTRATION_REQUEST:
                    if(tsbk instanceof UnitRegistrationRequest)
                    {
                        UnitRegistrationRequest urr = (UnitRegistrationRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details((urr.isEmergency() ? "EMERGENCY " : "") +
                                "UNIT REGISTRATION REQUEST - CAPABILITY:" + urr.getCapability())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_LOCATION_REGISTRATION_REQUEST:
                    if(tsbk instanceof LocationRegistrationRequest)
                    {
                        LocationRegistrationRequest lrr = (LocationRegistrationRequest)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details((lrr.isEmergency() ? "EMERGENCY " : "") +
                                "LOCATION REGISTRATION REQUEST - CAPABILITY:" + lrr.getCapability())
                            .identifiers(ic)
                            .build());
                    }
                    break;
                case ISP_PROTECTION_PARAMETER_REQUEST:
                    MutableIdentifierCollection icProtection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icProtection.remove(IdentifierClass.USER);
                    icProtection.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("ENCRYPTION PARAMETERS")
                        .identifiers(icProtection)
                        .build());
                    break;
                case ISP_IDENTIFIER_UPDATE_REQUEST:
                    MutableIdentifierCollection icIdentifierUpdate = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icIdentifierUpdate.remove(IdentifierClass.USER);
                    icIdentifierUpdate.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("FREQUENCY BAND DETAILS")
                        .identifiers(icIdentifierUpdate)
                        .build());
                    break;
                case ISP_ROAMING_ADDRESS_REQUEST:
                    MutableIdentifierCollection icRoamingRequest = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRoamingRequest.remove(IdentifierClass.USER);
                    icRoamingRequest.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("ROAMING ADDRESS")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_ROAMING_ADDRESS_RESPONSE:
                    MutableIdentifierCollection icRoaming = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                    icRoaming.remove(IdentifierClass.USER);
                    icRoaming.update(tsbk.getIdentifiers());

                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("ROAMING ADDRESS")
                        .identifiers(icRoaming)
                        .build());
                    break;
                case MOTOROLA_OSP_DENY_RESPONSE:
                    if(tsbk instanceof MotorolaDenyResponse)
                    {
                        MotorolaDenyResponse dr = (MotorolaDenyResponse)tsbk;
                        MutableIdentifierCollection ic = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                        ic.remove(IdentifierClass.USER);
                        ic.update(tsbk.getIdentifiers());

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("DENY: " + dr.getDeniedServiceType().getDescription() +
                                " REASON: " + dr.getDenyReason() + " - INFO: " + dr.getAdditionalInfo())
                            .identifiers(ic)
                            .build());
                    }
                default:
//                    mLog.debug("Unrecognized TSBK Opcode: " + tsbk.getOpcode().name() + " VENDOR:" + tsbk.getVendor() +
//                        " OPCODE:" + tsbk.getOpcodeNumber());
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
