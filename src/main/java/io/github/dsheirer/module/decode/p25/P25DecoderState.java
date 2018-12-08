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
package io.github.dsheirer.module.decode.p25;

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
import io.github.dsheirer.identifier.patch.PatchGroupManager;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.ars.ARSPacket;
import io.github.dsheirer.module.decode.ip.ipv4.IPV4Packet;
import io.github.dsheirer.module.decode.ip.udp.UDPPacket;
import io.github.dsheirer.module.decode.p25.identifier.APCO25Nac;
import io.github.dsheirer.module.decode.p25.message.IAdjacentSite;
import io.github.dsheirer.module.decode.p25.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.hdu.HDUMessage;
import io.github.dsheirer.module.decode.p25.message.hdu.HeaderData;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCCallTermination;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCMessageUpdate;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCStatusUpdate;
import io.github.dsheirer.module.decode.p25.message.lc.standard.LCTelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.ldu.EncryptionSyncParameters;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.AMBTCMessage;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCAuthenticationResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCIndividualDataServiceRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCLocationRegistrationRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCMessageUpdateRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCStatusQueryResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCStatusUpdateRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCUnitAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCUnitToUnitVoiceServiceAnswerResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.isp.AMBTCUnitToUnitVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCGroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCMessageUpdate;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCProtectionParameterBroadcast;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCStatusUpdate;
import io.github.dsheirer.module.decode.p25.message.pdu.ambtc.osp.AMBTCUnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.message.pdu.packet.PacketMessage;
import io.github.dsheirer.module.decode.p25.message.pdu.packet.sndcp.SNDCPPacketMessage;
import io.github.dsheirer.module.decode.p25.message.pdu.umbtc.isp.UMBTCTelephoneInterconnectRequestExplicitDialing;
import io.github.dsheirer.module.decode.p25.message.tdu.TDULinkControlMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.osp.PatchGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.osp.PatchGroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.CancelServiceRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.ExtendedFunctionResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.GroupAffiliationQueryResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.GroupDataServiceRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.GroupVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.IndividualDataServiceRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.LocationRegistrationRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.MessageUpdateRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.SNDCPDataChannelRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.SNDCPDataPageResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.SNDCPReconnectRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.StatusQueryResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.StatusUpdateRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.TelephoneInterconnectAnswerResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.UnitAcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.UnitRegistrationRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.UnitToUnitVoiceServiceAnswerResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp.UnitToUnitVoiceServiceRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.AcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.DenyResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.GroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.GroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.GroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.LocationRegistrationResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.MessageUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.QueuedResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.RoamingAddressCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.SecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.StatusUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.TelephoneInterconnectVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.TelephoneInterconnectVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.UnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.UnitToUnitVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp.UnitToUnitVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.network.P25NetworkConfiguration;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class P25DecoderState extends DecoderState implements IChannelEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(P25DecoderState.class);

    private DecodeEvent mCurrentCallEvent;

    private static final DecimalFormat mFrequencyFormatter =
        new DecimalFormat("0.000000");

    private NetworkStatusBroadcast mNetworkStatus;
    //    private NetworkStatusBroadcastExtended mNetworkStatusExtended;
//    private ProtectionParameterBroadcast mProtectionParameterBroadcast;
    private RFSSStatusBroadcast mRFSSStatusMessage;
    //    private RFSSStatusBroadcastExtended mRFSSStatusMessageExtended;
//    private SNDCPDataChannelAnnouncementExplicit mSNDCPDataChannel;
    private Set<SecondaryControlChannelBroadcast> mSecondaryControlChannels = new TreeSet<>();

    private Map<Integer,IFrequencyBand> mBands = new HashMap<>();
    private Map<String,Long> mRegistrations = new HashMap<>();
    private Map<String,IAdjacentSite> mNeighborMap = new HashMap<>();

    private String mLastCommandEventID;
    private String mLastPageEventID;
    private String mLastQueryEventID;
    private String mLastRegistrationEventID;
    private String mLastResponseEventID;

    private APCO25Nac mNAC;
    private String mSystem;
    //    private AliasedStringAttributeMonitor mSiteAttributeMonitor;
//    private AliasedStringAttributeMonitor mFromTalkgroupMonitor;
//    private AliasedStringAttributeMonitor mToTalkgroupMonitor;
//    private String mCurrentChannel = "CURRENT";
    private long mCurrentChannelFrequency = 0;

    private ChannelType mChannelType;
    private P25Decoder.Modulation mModulation;
    private boolean mControlChannelShutdownLogged;

    private List<String> mCallDetectTalkgroups = new ArrayList<>();
    private Map<String,P25ChannelGrantEvent> mChannelCallMap = new HashMap<>();
    private PatchGroupManager mPatchGroupManager = new PatchGroupManager();
    private P25NetworkConfiguration mNetworkConfiguration = new P25NetworkConfiguration();
    private P25TrafficChannelManager mTrafficChannelManager;
    private Listener<ChannelEvent> mChannelEventListener;

    public P25DecoderState(Channel channel, P25TrafficChannelManager trafficChannelManager)
    {
        mChannelType = channel.getChannelType();
        mModulation = ((DecodeConfigP25Phase1)channel.getDecodeConfiguration()).getModulation();

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

        //TODO: update patch group manager to catch patch group add and delete and use
        //TODO: those when we have a patch group channel grant or update
//        mPatchGroupManager = new PatchGroupManager(aliasList, getCallEventBroadcaster());
//        mSiteAttributeMonitor = new AliasedStringAttributeMonitor(Attribute.NETWORK_ID_2,
//            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.SITE);
//        mFromTalkgroupMonitor = new AliasedStringAttributeMonitor(Attribute.PRIMARY_ADDRESS_FROM,
//            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.TALKGROUP);
//        mFromTalkgroupMonitor.addIllegalValue("000000");
//        mToTalkgroupMonitor = new AliasedStringAttributeMonitor(Attribute.PRIMARY_ADDRESS_TO,
//            getAttributeChangeRequestListener(), getAliasList(), AliasIDType.TALKGROUP);
//        mToTalkgroupMonitor.addIllegalValue("0000");
//        mToTalkgroupMonitor.addIllegalValue("000000");
    }

    public P25DecoderState(Channel channel)
    {
        this(channel, null);
    }

    public P25Decoder.Modulation getModulation()
    {
        return mModulation;
    }

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
        resetState();
    }

    @Override
    public void init()
    {

    }

    /**
     * Resets any temporal state details
     */
    protected void resetState()
    {
        super.resetState();
//        mFromTalkgroupMonitor.reset();
//        mToTalkgroupMonitor.reset();

        mNAC = null;
        mSystem = null;
        mCallDetectTalkgroups.clear();

        if(mCurrentCallEvent != null)
        {
            mCurrentCallEvent.end(System.currentTimeMillis());
            broadcast(mCurrentCallEvent);
        }

        mCurrentCallEvent = null;
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
        endCurrentCall(message.getTimestamp());
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
     * Processes a Header Data Unit message and starts a new call event.
     */
    private void processHDU(HDUMessage message)
    {
        if(message.isValid())
        {
            HeaderData headerData = message.getHeaderData();

            if(headerData.isValid())
            {
                endCurrentCall(message.getTimestamp());

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
     * Ends the current call event.
     *
     * @param timestamp of the message that indicates the event has ended.
     */
    private void endCurrentCall(long timestamp)
    {
        if(mCurrentCallEvent != null)
        {
            mCurrentCallEvent.end(timestamp);
            broadcast(mCurrentCallEvent);
            mCurrentCallEvent = null;
            broadcast(new DecoderStateEvent(this, Event.END, State.CALL));

            //Clear any temporal user or device identifiers
//TODO: this is being handled by the resetState() method ... do we need it here also?
            getIdentifierCollection().remove(IdentifierClass.USER);
        }
    }

    private void processTDU(P25Message message)
    {
        endCurrentCall(message.getTimestamp());
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE));
    }

    /**
     * Packet Data Unit
     *
     * @param message
     */
    private void processPDU(P25Message message)
    {
        broadcast(new DecoderStateEvent(this, Event.DECODE, State.DATA));
        //TODO: implement
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

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("AUTHENTICATION:" + ar.getAuthenticationValue())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_CALL_ALERT_REQUEST:
                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("CALL ALERT")
                        .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                        .build());
                    break;
                case ISP_GROUP_AFFILIATION_REQUEST:
                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("GROUP AFFILIATION")
                        .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                        .build());
                    break;
                case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                    if(ambtc instanceof AMBTCIndividualDataServiceRequest)
                    {
                        AMBTCIndividualDataServiceRequest idsr = (AMBTCIndividualDataServiceRequest)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("INDIVIDUAL DATA SERVICE " + idsr.getDataServiceOptions())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_LOCATION_REGISTRATION_REQUEST:
                    if(ambtc instanceof AMBTCLocationRegistrationRequest)
                    {
                        AMBTCLocationRegistrationRequest lrr = (AMBTCLocationRegistrationRequest)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("LOCATION REGISTRATION - UNIQUE ID:" + lrr.getSourceId())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_MESSAGE_UPDATE_REQUEST:
                    if(ambtc instanceof AMBTCMessageUpdateRequest)
                    {
                        AMBTCMessageUpdateRequest mur = (AMBTCMessageUpdateRequest)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.SDM.toString())
                            .details("MESSAGE:" + mur.getShortDataMessage())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_ROAMING_ADDRESS_REQUEST:
                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("ROAMING ADDRESS")
                        .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                        .build());
                    break;
                case ISP_STATUS_QUERY_REQUEST:
                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("STATUS QUERY")
                        .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                        .build());
                    break;
                case ISP_STATUS_QUERY_RESPONSE:
                    if(ambtc instanceof AMBTCStatusQueryResponse)
                    {
                        AMBTCStatusQueryResponse sqr = (AMBTCStatusQueryResponse)ambtc;
                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + sqr.getUnitStatus() + " USER:" + sqr.getUserStatus())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_STATUS_UPDATE_REQUEST:
                    if(ambtc instanceof AMBTCStatusUpdateRequest)
                    {
                        AMBTCStatusUpdateRequest sur = (AMBTCStatusUpdateRequest)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + sur.getUnitStatus() + " USER:" + sur.getUserStatus())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                    if(ambtc instanceof AMBTCUnitAcknowledgeResponse)
                    {
                        AMBTCUnitAcknowledgeResponse uar = (AMBTCUnitAcknowledgeResponse)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("ACKNOWLEDGE:" + uar.getAcknowledgedService())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                    if(ambtc instanceof AMBTCUnitToUnitVoiceServiceRequest)
                    {
                        AMBTCUnitToUnitVoiceServiceRequest uuvsr = (AMBTCUnitToUnitVoiceServiceRequest)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("UNIT-2-UNIT VOICE SERVICE " + uuvsr.getVoiceServiceOptions())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                    if(ambtc instanceof AMBTCUnitToUnitVoiceServiceAnswerResponse)
                    {
                        AMBTCUnitToUnitVoiceServiceAnswerResponse uuvsar = (AMBTCUnitToUnitVoiceServiceAnswerResponse)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(uuvsar.getAnswerResponse() + " UNIT-2-UNIT VOICE SERVICE " +
                                uuvsar.getVoiceServiceOptions())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;

                //Network configuration messages
                case OSP_ADJACENT_STATUS_BROADCAST:
                    mNetworkConfiguration.process(ambtc);
                    break;

                //Channel grants
                case OSP_GROUP_DATA_CHANNEL_GRANT:
                case OSP_GROUP_VOICE_CHANNEL_GRANT:
//                    AMBTCGroupVoiceChannelGrant gvcg = (AMBTCGroupVoiceChannelGrant)ambtc;
                case OSP_INDIVIDUAL_DATA_CHANNEL_GRANT:
//                    return new AMBTCIndividualDataChannelGrant(pduSequence, nac, timestamp);
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
//                    return new AMBTCTelephoneInterconnectChannelGrant(pduSequence, nac, timestamp);
                case OSP_TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
//                    return new AMBTCTelephoneInterconnectChannelGrantUpdate(pduSequence, nac, timestamp);
                case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
//                    return new AMBTCUnitToUnitAnswerRequest(pduSequence, nac, timestamp);
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
//                    return new AMBTCUnitToUnitVoiceServiceChannelGrant(pduSequence, nac, timestamp);
                case OSP_UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
//                    return new AMBTCUnitToUnitVoiceServiceChannelGrantUpdate(pduSequence, nac, timestamp);
//TODO: ....
                    break;

                case OSP_CALL_ALERT:
                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("CALL ALERT")
                        .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                        .build());
                    break;
                case OSP_GROUP_AFFILIATION_QUERY:
                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("GROUP AFFILIATION")
                        .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                        .build());
                    break;
                case OSP_GROUP_AFFILIATION_RESPONSE:
                    if(ambtc instanceof AMBTCGroupAffiliationResponse)
                    {
                        AMBTCGroupAffiliationResponse gar = (AMBTCGroupAffiliationResponse)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("AFFILIATION GROUP:" + gar.getGroupId() +
                                " ANNOUNCEMENT GROUP:" + gar.getAnnouncementGroupId())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_MESSAGE_UPDATE:
                    if(ambtc instanceof AMBTCMessageUpdate)
                    {
                        AMBTCMessageUpdate mu = (AMBTCMessageUpdate)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.SDM.toString())
                            .details("MESSAGE:" + mu.getShortDataMessage())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_PROTECTION_PARAMETER_BROADCAST:
                    if(ambtc instanceof AMBTCProtectionParameterBroadcast)
                    {
                        AMBTCProtectionParameterBroadcast ppb = (AMBTCProtectionParameterBroadcast)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("USE ENCRYPTION " + ppb.getEncryptionKey() +
                                " OUTBOUND MI:" + ppb.getOutboundMessageIndicator() +
                                " INBOUND MI:" + ppb.getInboundMessageIndicator())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_ROAMING_ADDRESS_UPDATE:
                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("ROAMING ADDRESS UPDATE")
                        .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                        .build());
                    break;
                case OSP_ROAMING_ADDRESS_COMMAND:
                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("ROAMING ADDRESS")
                        .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                        .build());
                    break;
                case OSP_STATUS_QUERY:
                    broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("STATUS")
                        .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                        .build());
                    break;
                case OSP_STATUS_UPDATE:
                    if(ambtc instanceof AMBTCStatusUpdate)
                    {
                        AMBTCStatusUpdate su = (AMBTCStatusUpdate)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + su.getUnitStatus() + " USER:" + su.getUserStatus())
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_UNIT_REGISTRATION_RESPONSE:
                    if(ambtc instanceof AMBTCUnitRegistrationResponse)
                    {
                        AMBTCUnitRegistrationResponse urr = (AMBTCUnitRegistrationResponse)ambtc;

                        broadcast(P25DecodeEvent.builder(ambtc.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details(urr.getResponse() + " UNIT REGISTRATION")
                            .identifiers(new IdentifierCollection(ambtc.getIdentifiers()))
                            .build());
                    }
                    break;
            }
        }

        broadcast(new DecoderStateEvent(this, Event.DECODE, State.CONTROL));
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

            broadcast(P25DecodeEvent.builder(tired.getTimestamp())
                .channel(getCurrentChannel())
                .eventDescription(DecodeEventType.REQUEST.toString())
                .details("TELEPHONE INTERCONNECT:" + tired.getTelephoneNumber())
                .identifiers(new IdentifierCollection(tired.getIdentifiers()))
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

            IPacket packet = packetMessage.getPacket();

            if(packet instanceof IPV4Packet)
            {
                IPV4Packet ipv4 = (IPV4Packet)packet;

                IPacket ipPayload = ipv4.getPayload();

                if(ipPayload instanceof UDPPacket)
                {
                    UDPPacket udpPacket = (UDPPacket)ipPayload;

                    IPacket udpPayload = udpPacket.getPayload();

                    Identifier from = packetMessage.getHeader().isOutbound() ?
                        ipv4.getHeader().getFromAddress() : packetMessage.getHeader().getLLID();
                    Identifier to = packetMessage.getHeader().isOutbound() ?
                        packetMessage.getHeader().getLLID() : ipv4.getHeader().getToAddress();

                    getIdentifierCollection().update(from);
                    getIdentifierCollection().update(to);

                    if(udpPayload instanceof ARSPacket)
                    {
                        ARSPacket arsPacket = (ARSPacket)udpPayload;

                        DecodeEvent packetEvent = P25DecodeEvent.builder(message.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.AUTOMATIC_REGISTRATION_SERVICE.toString())
                            .identifiers(getIdentifierCollection().copyOf())
                            .details(arsPacket.toString() + " " + ipv4.toString())
                            .build();

                        broadcast(packetEvent);
                    }
                    else
                    {
                        DecodeEvent packetEvent = P25DecodeEvent.builder(message.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.UDP_PACKET.toString())
                            .identifiers(getIdentifierCollection().copyOf())
                            .details(ipv4.toString())
                            .build();

                        broadcast(packetEvent);
                    }

                    //Once we broadcast the event, cleanup the identifier collection
//                    getIdentifierCollection().remove(IdentifierClass.USER);
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
        //TODO: implement
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(gvcg.getChannel(), gvcg.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), gvcg.getTimestamp());
                        }
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(gvcgu.getChannelA(), null, identifiersA,
                                tsbk.getOpcode(), gvcgu.getTimestamp());
                        }

                        if(gvcgu.hasGroupB())
                        {
                            //Make a copy of current identifiers and remove current user identifiers and replace from message
                            MutableIdentifierCollection identifiersB = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                            identifiersB.remove(IdentifierClass.USER);
                            identifiersB.update(mPatchGroupManager.update(gvcgu.getGroupAddressB()));

                            if(mTrafficChannelManager != null)
                            {
                                mTrafficChannelManager.processChannelGrant(gvcgu.getChannelB(), null, identifiersB,
                                    tsbk.getOpcode(), gvcgu.getTimestamp());
                            }
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(gvcgue.getChannel(), gvcgue.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), gvcgue.getTimestamp());
                        }
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(uuvcg.getChannel(), null, identifiers,
                                tsbk.getOpcode(), uuvcg.getTimestamp());
                        }
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(uuvcgu.getChannel(), null, identifiers,
                                tsbk.getOpcode(), uuvcgu.getTimestamp());
                        }
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(tivcg.getChannel(), tivcg.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), tivcg.getTimestamp());
                        }
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(tivcgu.getChannel(), tivcgu.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), tivcgu.getTimestamp());
                        }
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(dcg.getChannel(), dcg.getServiceOptions(),
                                identifiers, tsbk.getOpcode(), dcg.getTimestamp());
                        }
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(pgvcg.getChannel(), pgvcg.getVoiceServiceOptions(),
                                identifiers, tsbk.getOpcode(), pgvcg.getTimestamp());
                        }
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

                        if(mTrafficChannelManager != null)
                        {
                            mTrafficChannelManager.processChannelGrant(pgvcgu.getChannel1(), null, identifiersPG1,
                                tsbk.getOpcode(), pgvcgu.getTimestamp());
                        }

                        if(pgvcgu.hasPatchGroup2())
                        {
                            //Make a copy of current identifiers and remove current user identifiers and replace from message
                            MutableIdentifierCollection identifiersPG2 = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
                            identifiersPG2.remove(IdentifierClass.USER);
                            identifiersPG2.update(mPatchGroupManager.update(pgvcgu.getPatchGroup2()));

                            if(mTrafficChannelManager != null)
                            {
                                mTrafficChannelManager.processChannelGrant(pgvcgu.getChannel2(), null,
                                    identifiersPG2, tsbk.getOpcode(), pgvcgu.getTimestamp());
                            }
                        }
                    }
                    break;

                //Network Configuration Messages
                case MOTOROLA_OSP_TRAFFIC_CHANNEL_ID:
                case MOTOROLA_OSP_SYSTEM_LOADING:
                case MOTOROLA_OSP_CONTROL_CHANNEL_ID:
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
                    mNetworkConfiguration.process(tsbk);
                    break;

                case OSP_UNIT_TO_UNIT_ANSWER_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("UNIT-TO-UNIT ANSWER REQUEST")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case OSP_TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                    if(tsbk instanceof TelephoneInterconnectAnswerRequest)
                    {
                        TelephoneInterconnectAnswerRequest tiar = (TelephoneInterconnectAnswerRequest)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.PAGE.toString())
                            .details("TELEPHONE ANSWER REQUEST: " + tiar.getTelephoneNumber())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_SNDCP_DATA_PAGE_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("SNDCP DATA PAGE REQUEST")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case OSP_STATUS_UPDATE:
                    if(tsbk instanceof StatusUpdate)
                    {
                        StatusUpdate su = (StatusUpdate)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + su.getUnitStatus() + " USER:" + su.getUserStatus())
                            .identifiers(new IdentifierCollection(su.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_STATUS_QUERY:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("STATUS")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case OSP_MESSAGE_UPDATE:
                    if(tsbk instanceof MessageUpdate)
                    {
                        MessageUpdate mu = (MessageUpdate)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.SDM.toString())
                            .details("MSG:" + mu.getShortDataMessage())
                            .identifiers(new IdentifierCollection(mu.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_RADIO_UNIT_MONITOR_COMMAND:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("RADIO UNIT MONITOR")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case OSP_CALL_ALERT:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("CALL ALERT")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case OSP_ACKNOWLEDGE_RESPONSE:
                    if(tsbk instanceof AcknowledgeResponse)
                    {
                        AcknowledgeResponse ar = (AcknowledgeResponse)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("ACKNOWLEDGE " + ar.getAcknowledgedServiceType().getDescription())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_QUEUED_RESPONSE:
                    if(tsbk instanceof QueuedResponse)
                    {
                        QueuedResponse qr = (QueuedResponse)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("QUEUED: " + qr.getQueuedResponseServiceType().getDescription() +
                                " REASON: " + qr.getQueuedResponseReason() +
                                " INFO: " + qr.getAdditionalInfo())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_EXTENDED_FUNCTION_COMMAND:
                    if(tsbk instanceof ExtendedFunctionCommand)
                    {
                        ExtendedFunctionCommand efc = (ExtendedFunctionCommand)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.COMMAND.toString())
                            .details("EXTENDED FUNCTION: " + efc.getExtendedFunction() +
                                " ARGUMENTS:" + efc.getArguments())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_DENY_RESPONSE:
                    if(tsbk instanceof DenyResponse)
                    {
                        DenyResponse dr = (DenyResponse)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("DENY: " + dr.getDeniedServiceType().getDescription() +
                                " REASON: " + dr.getDenyReason() + " - INFO: " + dr.getAdditionalInfo())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_GROUP_AFFILIATION_RESPONSE:
                    if(tsbk instanceof GroupAffiliationResponse)
                    {
                        GroupAffiliationResponse gar = (GroupAffiliationResponse)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(gar.getAffiliationResponse() +
                                " AFFILIATION GROUP: " + gar.getGroupAddress() +
                                (gar.isGlobalAffiliation() ? " (GLOBAL)" : " (LOCAL)") +
                                " ANNOUNCEMENT GROUP:" + gar.getAnnouncementGroupAddress())
                            .identifiers(new IdentifierCollection(gar.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_GROUP_AFFILIATION_QUERY:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("GROUP AFFILIATION")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case OSP_LOCATION_REGISTRATION_RESPONSE:
                    if(tsbk instanceof LocationRegistrationResponse)
                    {
                        LocationRegistrationResponse lrr = (LocationRegistrationResponse)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details(lrr.getResponse() + " LOCATION REGISTRATION - GROUP:" + lrr.getGroupAddress())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_UNIT_REGISTRATION_RESPONSE:
                    if(tsbk instanceof UnitRegistrationResponse)
                    {
                        UnitRegistrationResponse urr = (UnitRegistrationResponse)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details(urr.getResponse() + " UNIT REGISTRATION - UNIT ID:" + urr.getTargetUniqueId())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case OSP_UNIT_REGISTRATION_COMMAND:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("UNIT REGISTRATION")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case OSP_AUTHENTICATION_COMMAND:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("AUTHENTICATE")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case OSP_UNIT_DEREGISTRATION_ACKNOWLEDGE:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.DEREGISTER.toString())
                        .details("ACKNOWLEDGE UNIT DE-REGISTRATION")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case OSP_ROAMING_ADDRESS_COMMAND:
                    if(tsbk instanceof RoamingAddressCommand)
                    {
                        RoamingAddressCommand rac = (RoamingAddressCommand)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.COMMAND.toString())
                            .details(rac.getStackOperation() + " ROAMING ADDRESS")
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
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
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("GROUP VOICE SERVICE " + gvsr.getVoiceServiceOptions())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_VOICE_SERVICE_REQUEST:
                    if(tsbk instanceof UnitToUnitVoiceServiceRequest)
                    {
                        UnitToUnitVoiceServiceRequest uuvsr = (UnitToUnitVoiceServiceRequest)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("UNIT-2-UNIT VOICE SERVICE " + uuvsr.getVoiceServiceOptions())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_UNIT_TO_UNIT_ANSWER_RESPONSE:
                    if(tsbk instanceof UnitToUnitVoiceServiceAnswerResponse)
                    {
                        UnitToUnitVoiceServiceAnswerResponse uuvsar = (UnitToUnitVoiceServiceAnswerResponse)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(uuvsar.getAnswerResponse() + " UNIT-2-UNIT VOICE SERVICE " + uuvsar.getVoiceServiceOptions())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_TELEPHONE_INTERCONNECT_PSTN_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("TELEPHONE INTERCONNECT")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_TELEPHONE_INTERCONNECT_ANSWER_RESPONSE:
                    if(tsbk instanceof TelephoneInterconnectAnswerResponse)
                    {
                        TelephoneInterconnectAnswerResponse tiar = (TelephoneInterconnectAnswerResponse)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(tiar.getAnswerResponse() + " TELEPHONE INTERCONNECT " + tiar.getVoiceServiceOptions())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_INDIVIDUAL_DATA_SERVICE_REQUEST:
                    if(tsbk instanceof IndividualDataServiceRequest)
                    {
                        IndividualDataServiceRequest idsr = (IndividualDataServiceRequest)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("INDIVIDUAL DATA SERVICE " + idsr.getVoiceServiceOptions())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_GROUP_DATA_SERVICE_REQUEST:
                    if(tsbk instanceof GroupDataServiceRequest)
                    {
                        GroupDataServiceRequest gdsr = (GroupDataServiceRequest)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("GROUP DATA SERVICE " + gdsr.getVoiceServiceOptions())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_SNDCP_DATA_CHANNEL_REQUEST:
                    if(tsbk instanceof SNDCPDataChannelRequest)
                    {
                        SNDCPDataChannelRequest sdcr = (SNDCPDataChannelRequest)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("SNDCP DATA CHANNEL " + sdcr.getDataServiceOptions())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_SNDCP_DATA_PAGE_RESPONSE:
                    if(tsbk instanceof SNDCPDataPageResponse)
                    {
                        SNDCPDataPageResponse sdpr = (SNDCPDataPageResponse)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details(sdpr.getAnswerResponse() + " SNDCP DATA " + sdpr.getDataServiceOptions())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_SNDCP_RECONNECT_REQUEST:
                    if(tsbk instanceof SNDCPReconnectRequest)
                    {
                        SNDCPReconnectRequest srr = (SNDCPReconnectRequest)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("SNDCP RECONNECT " + (srr.hasDataToSend() ? "- DATA TO SEND " : "")
                                + srr.getDataServiceOptions())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_STATUS_UPDATE_REQUEST:
                    if(tsbk instanceof StatusUpdateRequest)
                    {
                        StatusUpdateRequest sur = (StatusUpdateRequest)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + sur.getUnitStatus() + " USER:" + sur.getUserStatus())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_STATUS_QUERY_RESPONSE:
                    if(tsbk instanceof StatusQueryResponse)
                    {
                        StatusQueryResponse sqr = (StatusQueryResponse)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.STATUS.toString())
                            .details("UNIT:" + sqr.getUnitStatus() + " USER:" + sqr.getUserStatus())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_STATUS_QUERY_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.QUERY.toString())
                        .details("UNIT AND USER STATUS")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_MESSAGE_UPDATE_REQUEST:
                    if(tsbk instanceof MessageUpdateRequest)
                    {
                        MessageUpdateRequest mur = (MessageUpdateRequest)tsbk;
                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.SDM.toString())
                            .details("MESSAGE:" + mur.getShortDataMessage())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_RADIO_UNIT_MONITOR_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("RADIO UNIT MONITOR")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_CALL_ALERT_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("CALL ALERT")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_UNIT_ACKNOWLEDGE_RESPONSE:
                    if(tsbk instanceof UnitAcknowledgeResponse)
                    {
                        UnitAcknowledgeResponse uar = (UnitAcknowledgeResponse)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("UNIT ACKNOWLEDGE:" + uar.getAcknowledgedServiceType().getDescription())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_CANCEL_SERVICE_REQUEST:
                    if(tsbk instanceof CancelServiceRequest)
                    {
                        CancelServiceRequest csr = (CancelServiceRequest)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REQUEST.toString())
                            .details("CANCEL SERVICE:" + csr.getServiceType() +
                                " REASON:" + csr.getCancelReason() + (csr.hasAdditionalInformation() ?
                                " INFO:" + csr.getAdditionalInformation() : ""))
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_EXTENDED_FUNCTION_RESPONSE:
                    if(tsbk instanceof ExtendedFunctionResponse)
                    {
                        ExtendedFunctionResponse efr = (ExtendedFunctionResponse)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("EXTENDED FUNCTION:" + efr.getExtendedFunction() +
                                " ARGUMENTS:" + efr.getArguments())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_EMERGENCY_ALARM_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("EMERGENCY ALARM")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_GROUP_AFFILIATION_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("GROUP AFFILIATION")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_GROUP_AFFILIATION_QUERY_RESPONSE:
                    if(tsbk instanceof GroupAffiliationQueryResponse)
                    {
                        GroupAffiliationQueryResponse gaqr = (GroupAffiliationQueryResponse)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.RESPONSE.toString())
                            .details("AFFILIATION - GROUP:" + gaqr.getGroupAddress() +
                                " ANNOUNCEMENT GROUP:" + gaqr.getAnnouncementGroupAddress())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_UNIT_DE_REGISTRATION_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.DEREGISTER.toString())
                        .details("UNIT DE-REGISTRATION REQUEST")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_UNIT_REGISTRATION_REQUEST:
                    if(tsbk instanceof UnitRegistrationRequest)
                    {
                        UnitRegistrationRequest urr = (UnitRegistrationRequest)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details((urr.isEmergency() ? "EMERGENCY " : "") +
                                "UNIT REGISTRATION REQUEST - CAPABILITY:" + urr.getCapability())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_LOCATION_REGISTRATION_REQUEST:
                    if(tsbk instanceof LocationRegistrationRequest)
                    {
                        LocationRegistrationRequest lrr = (LocationRegistrationRequest)tsbk;

                        broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                            .channel(getCurrentChannel())
                            .eventDescription(DecodeEventType.REGISTER.toString())
                            .details((lrr.isEmergency() ? "EMERGENCY " : "") +
                                "LOCATION REGISTRATION REQUEST - CAPABILITY:" + lrr.getCapability())
                            .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                            .build());
                    }
                    break;
                case ISP_PROTECTION_PARAMETER_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("ENCRYPTION PARAMETERS")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_IDENTIFIER_UPDATE_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("FREQUENCY BAND DETAILS")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_ROAMING_ADDRESS_REQUEST:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.REQUEST.toString())
                        .details("ROAMING ADDRESS")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
                        .build());
                    break;
                case ISP_ROAMING_ADDRESS_RESPONSE:
                    broadcast(P25DecodeEvent.builder(tsbk.getTimestamp())
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.RESPONSE.toString())
                        .details("ROAMING ADDRESS")
                        .identifiers(new IdentifierCollection(tsbk.getIdentifiers()))
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
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
            case MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_USER:
            case MOTOROLA_TALK_COMPLETE:
                for(Identifier identifier : lcw.getIdentifiers())
                {
                    //Add to the identifier collection after filtering through the patch group manager
                    getIdentifierCollection().update(mPatchGroupManager.update(identifier));
                }
                break;

            //Calls in-progress on another channel
            case GROUP_VOICE_CHANNEL_UPDATE:
            case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
//TODO: handle call detections
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
                mNetworkConfiguration.process(lcw);
                break;

            //Other events
            case UNIT_TO_UNIT_ANSWER_REQUEST:
                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.PAGE.toString())
                    .details("Unit-to-Unit Answer Request")
                    .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                    .build());
                break;
            case MOTOROLA_PATCH_GROUP_ADD:
                mPatchGroupManager.addPatchGroups(lcw.getIdentifiers());
                break;
            case MOTOROLA_PATCH_GROUP_DELETE:
                mPatchGroupManager.removePatchGroups(lcw.getIdentifiers());
                break;
            case MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_UPDATE:
                mPatchGroupManager.addPatchGroups(lcw.getIdentifiers());
                break;
            case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                if(lcw instanceof LCTelephoneInterconnectAnswerRequest)
                {
                    LCTelephoneInterconnectAnswerRequest tiar = (LCTelephoneInterconnectAnswerRequest)lcw;
                    broadcast(P25DecodeEvent.builder(timestamp)
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.PAGE.toString())
                        .details("Telephone Call:" + tiar.getTelephoneNumber())
                        .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                        .build());
                }
                break;
            case CALL_TERMINATION_OR_CANCELLATION:
                if(lcw instanceof LCCallTermination && ((LCCallTermination)lcw).isNetworkCommandedTeardown())
                {
                    broadcast(new DecoderStateEvent(this, Event.END, State.FADE));
                }
                break;
            case GROUP_AFFILIATION_QUERY:
                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.QUERY.toString())
                    .details("Group Affiliation")
                    .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                    .build());
                break;
            case UNIT_REGISTRATION_COMMAND:
                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.COMMAND.toString())
                    .details("Unit Registration")
                    .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                    .build());
                break;
            case UNIT_AUTHENTICATION_COMMAND:
                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.COMMAND.toString())
                    .details("Authenticate Unit")
                    .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                    .build());
                break;
            case STATUS_QUERY:
                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.QUERY.toString())
                    .details("Status")
                    .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                    .build());
                break;
            case STATUS_UPDATE:
                if(lcw instanceof LCStatusUpdate)
                {
                    LCStatusUpdate su = (LCStatusUpdate)lcw;

                    broadcast(P25DecodeEvent.builder(timestamp)
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.STATUS.toString())
                        .details("UNIT:" + su.getUnitStatus() + " USER:" + su.getUserStatus())
                        .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                        .build());
                }
                break;
            case MESSAGE_UPDATE:
                if(lcw instanceof LCMessageUpdate)
                {
                    LCMessageUpdate mu = (LCMessageUpdate)lcw;
                    broadcast(P25DecodeEvent.builder(timestamp)
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.SDM.toString())
                        .details("MSG:" + mu.getShortDataMessage())
                        .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                        .build());
                }
                break;
            case CALL_ALERT:
                broadcast(P25DecodeEvent.builder(timestamp)
                    .channel(getCurrentChannel())
                    .eventDescription(DecodeEventType.PAGE.toString())
                    .details("Call Alert")
                    .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                    .build());
                break;
            case EXTENDED_FUNCTION_COMMAND:
                if(lcw instanceof LCExtendedFunctionCommand)
                {
                    LCExtendedFunctionCommand efc = (LCExtendedFunctionCommand)lcw;
                    broadcast(P25DecodeEvent.builder(timestamp)
                        .channel(getCurrentChannel())
                        .eventDescription(DecodeEventType.COMMAND.toString())
                        .details("Extended Function: " + efc.getExtendedFunction() +
                            " Arguments:" + efc.getExtendedFunctionArguments())
                        .identifiers(new IdentifierCollection(lcw.getIdentifiers()))
                        .build());
                }
                break;
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

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

//        sb.append("Activity Summary - Decoder:P25 ").append(getModulation().getLabel()).append("\n");
//        sb.append(DIVIDER1);
//        sb.append("SITE ");
//
//        if(mNetworkStatus != null)
//        {
//            sb.append("NAC:" + mNetworkStatus.getNAC());
//            sb.append("h WACN:" + mNetworkStatus.getWACN());
//            sb.append("h SYS:" + mNetworkStatus.getSystemID());
//            sb.append("h [").append(mNetworkStatus.getNetworkCallsign()).append("]");
//            sb.append(" LRA:" + mNetworkStatus.getLocationRegistrationArea());
//        }
//        else if(mNetworkStatusExtended != null)
//        {
//            sb.append("NAC:" + mNetworkStatusExtended.getNAC());
//            sb.append("h WACN:" + mNetworkStatusExtended.getWACN());
//            sb.append("h SYS:" + mNetworkStatusExtended.getSystemID());
//            sb.append("h [").append(mNetworkStatusExtended.getNetworkCallsign()).append("]");
//            sb.append(" LRA:" + mNetworkStatusExtended.getLocationRegistrationArea());
//        }
//
//        String site = null;
//
//        if(mRFSSStatusMessage != null)
//        {
//            site = mRFSSStatusMessage.getRFSubsystemID() + "-" + mRFSSStatusMessage.getSiteID();
//        }
//        else if(mRFSSStatusMessageExtended != null)
//        {
//            site = mRFSSStatusMessageExtended.getRFSubsystemID() + "-" + mRFSSStatusMessageExtended.getSiteID();
//        }
//
//        sb.append("h RFSS-SITE:").append(site).append("h");
//
//        if(hasAliasList())
//        {
//            Alias siteAlias = getAliasList().getSiteID(site);
//
//            if(siteAlias != null)
//            {
//                sb.append(" " + siteAlias.getName());
//            }
//        }
//
//        sb.append("\n").append(DIVIDER2);
//
//        if(mNetworkStatus != null)
//        {
//            sb.append("SERVICES: " + SystemService.toString(mNetworkStatus.getSystemServiceClass())).append("\n");
//            sb.append(DIVIDER2);
//            sb.append("PCCH DOWNLINK ")
//                    .append(mFrequencyFormatter.format((double) mNetworkStatus.getDownlinkFrequency() / 1E6d))
//                    .append(" [").append(mNetworkStatus.getIdentifier()).append("-").append(mNetworkStatus.getChannel())
//                    .append("] UPLINK ")
//                    .append(mFrequencyFormatter.format((double) mNetworkStatus.getUplinkFrequency() / 1E6d))
//                    .append(" [").append(mNetworkStatus.getIdentifier()).append("-").append(mNetworkStatus.getChannel())
//                    .append("]\n");
//        }
//        else if(mNetworkStatusExtended != null)
//        {
//            sb.append("\nSERVICES:").append(SystemService.toString(mNetworkStatusExtended.getSystemServiceClass()))
//                    .append("\n");
//            sb.append(DIVIDER2);
//            sb.append("PCCH DOWNLINK ")
//                    .append(mFrequencyFormatter.format((double) mNetworkStatusExtended.getDownlinkFrequency() / 1E6d))
//                    .append(" [").append(mNetworkStatusExtended.getTransmitIdentifier()).append("-")
//                    .append(mNetworkStatusExtended.getTransmitChannel()).append("] UPLINK ")
//                    .append(mFrequencyFormatter.format((double) mNetworkStatusExtended.getUplinkFrequency() / 1E6d))
//                    .append(" [").append(mNetworkStatusExtended.getReceiveIdentifier()).append("-")
//                    .append(mNetworkStatusExtended.getReceiveChannel()).append("]\n");
//        }
//
//        if(mSecondaryControlChannels.isEmpty())
//        {
//            sb.append("SCCH: NONE\n");
//        }
//        else
//        {
//            for(SecondaryControlChannelBroadcast sec : mSecondaryControlChannels)
//            {
//                sb.append("SCCH DOWNLINK ")
//                        .append(mFrequencyFormatter.format((double) sec.getDownlinkFrequency1() / 1E6d))
//                        .append(" [").append(sec.getIdentifier1()).append("-").append(sec.getChannel1()).append("] UPLINK ")
//                        .append(mFrequencyFormatter.format((double) sec.getUplinkFrequency1() / 1E6d))
//                        .append(" [").append(sec.getIdentifier1()).append("-").append(sec.getChannel1()).append("]");
//
//                if(sec.hasChannel2())
//                {
//                    sb.append("  SCCH 2 DOWNLINK ")
//                            .append(mFrequencyFormatter.format((double) sec.getDownlinkFrequency2() / 1E6d))
//                            .append(" [").append(sec.getIdentifier2()).append("-").append(sec.getChannel2())
//                            .append("] UPLINK ").append(mFrequencyFormatter.format((double) sec.getUplinkFrequency2() / 1E6d))
//                            .append(" [").append(sec.getIdentifier2()).append("-").append(sec.getChannel2()).append("]");
//                }
//
//                sb.append("\n");
//            }
//        }
//
//
//        if(mSNDCPDataChannel != null)
//        {
//            sb.append("SNDCP DOWNLINK ")
//                    .append(mFrequencyFormatter.format((double) mSNDCPDataChannel.getDownlinkFrequency() / 1E6D))
//                    .append(" [").append(mSNDCPDataChannel.getTransmitChannel()).append("]").append(" UPLINK ")
//                    .append(mFrequencyFormatter.format((double) mSNDCPDataChannel.getUplinkFrequency() / 1E6D))
//                    .append(" [").append(mSNDCPDataChannel.getReceiveChannel()).append("]\n");
//        }
//
//        if(mProtectionParameterBroadcast != null)
//        {
//            sb.append(DIVIDER2);
//            sb.append("ENCRYPTION TYPE:").append(mProtectionParameterBroadcast.getEncryptionType().name());
//            sb.append(" ALGORITHM:").append(mProtectionParameterBroadcast.getAlgorithmID());
//            sb.append(" KEY:").append(mProtectionParameterBroadcast.getKeyID());
//            sb.append(" INBOUND IV:").append(mProtectionParameterBroadcast.getInboundInitializationVector());
//            sb.append(" OUTBOUND IV:").append(mProtectionParameterBroadcast.getOutboundInitializationVector());
//            sb.append("\n");
//        }
//
//        List<Integer> identifiers = new ArrayList<>(mBands.keySet());
//
//        Collections.sort(identifiers);
//
//        sb.append(DIVIDER2).append("FREQUENCY BANDS:\n");
//        for(Integer id : identifiers)
//        {
//            IFrequencyBand band = mBands.get(id);
//            sb.append(band.toString()).append("\n");
////            sb.append("  ").append(id);
////            sb.append(" - BASE: " + mFrequencyFormatter.format(
////                (double)band.getBaseFrequency() / 1E6d));
////            sb.append(" CHANNEL_NUMBER SIZE: " + mFrequencyFormatter.format(
////                (double)band.getChannelSpacing() / 1E6d));
////            sb.append(" UPLINK OFFSET: " + mFrequencyFormatter.format(
////                (double)band.getTransmitOffset() / 1E6D));
////            sb.append("\n");
//        }
//
//        sb.append(DIVIDER2).append("NEIGHBOR SITES: ");
//
//        if(mNeighborMap.isEmpty())
//        {
//            sb.append("NONE\n");
//        }
//        else
//        {
//            for(IAdjacentSite neighbor : mNeighborMap.values())
//            {
//                sb.append("\n");
//                sb.append("NAC:").append(((P25Message) neighbor).getNAC());
//                sb.append("h SYSTEM:" + neighbor.getSystemID());
//                sb.append("h LRA:" + neighbor.getLRAId());
//
//                String neighborID = neighbor.getRFSSId() + "-" + neighbor.getSiteID();
//                sb.append("h RFSS-SITE:" + neighborID);
//
//                sb.append("h ");
//
//                if(hasAliasList())
//                {
//                    Alias siteAlias = getAliasList().getSiteID(neighborID);
//
//                    if(siteAlias != null)
//                    {
//                        sb.append(siteAlias.getName());
//                    }
//                }
//
//                sb.append("\n  PCCH: DOWNLINK ")
//                        .append(mFrequencyFormatter.format((double) neighbor.getDownlinkFrequency() / 1E6d))
//                        .append(" [").append(neighbor.getDownlinkChannel()).append("] UPLINK:")
//                        .append(mFrequencyFormatter.format((double) neighbor.getUplinkFrequency() / 1E6d))
//                        .append(" [").append(neighbor.getDownlinkChannel()).append("] SERVICES: ")
//                        .append(neighbor.getSystemServiceClass());
//            }
//        }

        return sb.toString();
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case RESET:
                resetState();
                break;
            case SOURCE_FREQUENCY:
                mCurrentChannelFrequency = event.getFrequency();
                break;
//            case TRAFFIC_CHANNEL_ALLOCATION:
//                if(event.getSource() != P25DecoderState.this)
//                {
//                    if(event instanceof TrafficChannelAllocationEvent)
//                    {
//                        TrafficChannelAllocationEvent allocationEvent = (TrafficChannelAllocationEvent)event;
//
//                        mCurrentCallEvent = (P25CallEvent) allocationEvent.getCallEvent();
//
//                        mCurrentChannelFrequency = allocationEvent.getCallEvent().getFrequency();
//                        broadcast(new AttributeChangeRequest<Long>(Attribute.CHANNEL_FREQUENCY, mCurrentChannelFrequency));
//
//                        mFromTalkgroupMonitor.reset();
//                        mFromTalkgroupMonitor.process(allocationEvent.getCallEvent().getFromID());
//
//                        mToTalkgroupMonitor.reset();
//                        mToTalkgroupMonitor.process(allocationEvent.getCallEvent().getToID());
//                    }
//                }
//                break;
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
    public void stop()
    {
    }
}
