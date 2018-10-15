/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.id.AliasIDType;
import io.github.dsheirer.channel.metadata.AliasedStringAttributeMonitor;
import io.github.dsheirer.channel.metadata.Attribute;
import io.github.dsheirer.channel.metadata.AttributeChangeRequest;
import io.github.dsheirer.channel.state.ChangeChannelTimeoutEvent;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.DecoderStateEvent.Event;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.channel.traffic.TrafficChannelAllocationEvent;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.CallEvent;
import io.github.dsheirer.module.decode.p25.message.IAdjacentSite;
import io.github.dsheirer.module.decode.p25.message.IBandIdentifier;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.hdu.HDUMessage;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.message.ldu.LDUMessage;
import io.github.dsheirer.module.decode.p25.message.ldu.lc.CallTermination;
import io.github.dsheirer.module.decode.p25.message.pdu.PDUMessage;
import io.github.dsheirer.module.decode.p25.message.pdu.confirmed.PDUConfirmedMessage;
import io.github.dsheirer.module.decode.p25.message.pdu.confirmed.PDUTypeUnknown;
import io.github.dsheirer.module.decode.p25.message.pdu.confirmed.PacketData;
import io.github.dsheirer.module.decode.p25.message.pdu.confirmed.SNDCPActivateTDSContextAccept;
import io.github.dsheirer.module.decode.p25.message.pdu.confirmed.SNDCPActivateTDSContextReject;
import io.github.dsheirer.module.decode.p25.message.pdu.confirmed.SNDCPActivateTDSContextRequest;
import io.github.dsheirer.module.decode.p25.message.pdu.confirmed.SNDCPDeactivateTDSContext;
import io.github.dsheirer.module.decode.p25.message.pdu.confirmed.SNDCPUserData;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.AdjacentStatusBroadcastExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.CallAlertExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.GroupAffiliationQueryExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.GroupAffiliationResponseExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.MessageUpdateExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.NetworkStatusBroadcastExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.ProtectionParameterBroadcast;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.RFSSStatusBroadcastExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.RoamingAddressUpdateExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.StatusQueryExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.StatusUpdateExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.control.UnitRegistrationResponseExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.data.GroupDataChannelGrantExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.data.IndividualDataChannelGrantExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.voice.GroupVoiceChannelGrantExplicit;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.voice.TelephoneInterconnectChannelGrantExplicit;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.voice.UnitToUnitAnswerRequestExplicit;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.voice.UnitToUnitVoiceChannelGrantExtended;
import io.github.dsheirer.module.decode.p25.message.pdu.osp.voice.UnitToUnitVoiceChannelGrantUpdateExtended;
import io.github.dsheirer.module.decode.p25.message.tdu.TDUMessage;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.AdjacentSiteStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.AdjacentSiteStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.CallAlert;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.GroupAffiliationQuery;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.GroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.GroupVoiceChannelUpdateExplicit;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.GroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.MessageUpdate;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.NetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.RFSSStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.StatusQuery;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.StatusUpdate;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.TDULinkControlMessage;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.TelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.UnitAuthenticationCommand;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.UnitRegistrationCommand;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.UnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.tdu.lc.UnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.MotorolaTSBKMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.PatchGroupAdd;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.PatchGroupDelete;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.PatchGroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.motorola.PatchGroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.AcknowledgeResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.AdjacentStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.AuthenticationCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.DenyResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.GroupAffiliationResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.LocationRegistrationResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.ProtectionParameterUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.QueuedResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.RadioUnitMonitorCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.RoamingAddressCommand;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SystemService;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.UnitDeregistrationAcknowledge;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.UnitRegistrationResponse;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncement;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.GroupDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.IndividualDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.SNDCPDataChannelAnnouncementExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.SNDCPDataChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.data.SNDCPDataPageRequest;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdateExplicit;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.TelephoneInterconnectVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.TelephoneInterconnectVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrant;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrantUpdate;
import io.github.dsheirer.module.decode.p25.reference.IPProtocol;
import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;
import io.github.dsheirer.module.decode.p25.reference.Response;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class P25DecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(P25DecoderState.class);
    private static final DecimalFormat mFrequencyFormatter =
            new DecimalFormat("0.000000");


    private NetworkStatusBroadcast mNetworkStatus;
    private NetworkStatusBroadcastExtended mNetworkStatusExtended;
    private ProtectionParameterBroadcast mProtectionParameterBroadcast;
    private RFSSStatusBroadcast mRFSSStatusMessage;
    private RFSSStatusBroadcastExtended mRFSSStatusMessageExtended;
    private SNDCPDataChannelAnnouncementExplicit mSNDCPDataChannel;

    private Set<SecondaryControlChannelBroadcast> mSecondaryControlChannels =
            new TreeSet<>();

    private Map<Integer, IdentifierUpdate> mBands = new HashMap<>();
    private Map<String, Long> mRegistrations = new HashMap<>();
    private Map<String, IAdjacentSite> mNeighborMap = new HashMap<>();

    private String mLastCommandEventID;
    private String mLastPageEventID;
    private String mLastQueryEventID;
    private String mLastRegistrationEventID;
    private String mLastResponseEventID;

    //TODO: create multi-attribute monitor for NAC and System
    private String mNAC;
    private String mSystem;
    private AliasedStringAttributeMonitor mSiteAttributeMonitor;
    private AliasedStringAttributeMonitor mFromTalkgroupMonitor;
    private AliasedStringAttributeMonitor mToTalkgroupMonitor;
    private String mCurrentChannel = "CURRENT";
    private long mCurrentChannelFrequency = 0;

    private ChannelType mChannelType;
    private P25Decoder.Modulation mModulation;
    private boolean mIgnoreDataCalls;
    private boolean mControlChannelShutdownLogged;

    private P25CallEvent mCurrentCallEvent;
    private List<String> mCallDetectTalkgroups = new ArrayList<>();
    private Map<String, P25CallEvent> mChannelCallMap = new HashMap<>();
    private PatchGroupManager mPatchGroupManager;

    public P25DecoderState(AliasList aliasList,
                           ChannelType channelType,
                           P25Decoder.Modulation modulation,
                           boolean ignoreDataCalls)
    {
        super(aliasList);
        mChannelType = channelType;
        mModulation = modulation;
        mIgnoreDataCalls = ignoreDataCalls;

        mPatchGroupManager = new PatchGroupManager(aliasList, getCallEventBroadcaster());
        mSiteAttributeMonitor = new AliasedStringAttributeMonitor(Attribute.NETWORK_ID_2,
                getAttributeChangeRequestListener(), getAliasList(), AliasIDType.SITE);
        mFromTalkgroupMonitor = new AliasedStringAttributeMonitor(Attribute.PRIMARY_ADDRESS_FROM,
                getAttributeChangeRequestListener(), getAliasList(), AliasIDType.TALKGROUP);
        mFromTalkgroupMonitor.addIllegalValue("000000");
        mToTalkgroupMonitor = new AliasedStringAttributeMonitor(Attribute.PRIMARY_ADDRESS_TO,
                getAttributeChangeRequestListener(), getAliasList(), AliasIDType.TALKGROUP);
        mToTalkgroupMonitor.addIllegalValue("0000");
        mToTalkgroupMonitor.addIllegalValue("000000");
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
     * Performs a full reset to prepare this object for reuse on a new channel
     */
    @Override
    public void reset()
    {
        resetState();

        mNAC = null;
        mSiteAttributeMonitor.reset();
        mSystem = null;
    }

    @Override
    public void init()
    {

    }

    /**
     * Resets any temporal state details
     */
    private void resetState()
    {
        mFromTalkgroupMonitor.reset();
        mToTalkgroupMonitor.reset();

        mCallDetectTalkgroups.clear();

        if(mCurrentCallEvent != null)
        {
            mCurrentCallEvent.end();
            broadcast(mCurrentCallEvent);
        }

        mCurrentCallEvent = null;
    }

    /**
     * Indicates if there a call event exists for the specified channel and
     * from and to talkgroup identifiers.
     *
     * @param channel - channel number
     * @param from user id
     * @param to talkgroup or user id
     * @return true if there is a call event
     */
    public boolean hasCallEvent(String channel, String from, String to)
    {
        boolean hasEvent = false;

        if(mChannelCallMap.containsKey(channel))
        {
            P25CallEvent event = mChannelCallMap.get(channel);

            if(to != null &&
                    event.getToID() != null &&
                    to.contentEquals(event.getToID()))
            {
                if(from != null)
                {
                    if(event.getFromID() == null)
                    {
                        hasEvent = true;
                    }
                    else if(from.contentEquals(event.getFromID()))
                    {
                        hasEvent = true;
                    }
                }
                else
                {
                    hasEvent = true;
                }
            }
        }

        return hasEvent;
    }

    /**
     * Adds the channel and event to the current channel call map.  If an entry
     * already exists, terminates the event and broadcasts the update.
     *
     * @param event to place in the map
     */
    public void registerCallEvent(P25CallEvent event)
    {
        String channel = event.getChannel();

        if(mChannelCallMap.containsKey(event.getChannel()))
        {
            P25CallEvent previousEvent = mChannelCallMap.remove(event.getChannel());

            previousEvent.end();

            broadcast(previousEvent);
        }

        mChannelCallMap.put(event.getChannel(), event);
    }

    private void updateCallEvent(String channel, String from, String to)
    {
        P25CallEvent event = mChannelCallMap.get(channel);

        if(event != null &&
                to != null &&
                event.getToID() != null &&
                event.getToID().contentEquals(to))
        {
            if(event.getFromID() == null &&
                    from != null &&
                    !from.contentEquals("0000") &&
                    !from.contentEquals("000000"))
            {
                event.setFromID(from);
                broadcast(event);
            }
        }
    }

    /**
     * Primary message processing method.
     */
    @Override
    public void receive(IMessage message)
    {
        if(message instanceof P25Message)
        {
            updateNAC(((P25Message) message).getNAC());

            /* Voice Vocoder messages */
            if(message instanceof LDUMessage)
            {
                processLDU((LDUMessage) message);
            }

            /* Trunking Signalling Messages */
            else if(message instanceof TSBKMessage)
            {
                processTSBK((TSBKMessage) message);
            }

            /* Terminator Data Unit with Link Control Message */
            else if(message instanceof TDULinkControlMessage)
            {
                processTDULC((TDULinkControlMessage) message);
            }

            /* Packet Data Unit Messages */
            else if(message instanceof PDUMessage)
            {
                processPDU((PDUMessage) message);
            }

            /* Header Data Unit Message - preceeds voice LDUs */
            else if(message instanceof HDUMessage)
            {
                processHDU((HDUMessage) message);
            }

            /* Terminator Data Unit, or default message if CRC failed */
            else if(message instanceof TDUMessage)
            {
                processTDU((TDUMessage) message);
            }
        }
    }

    /**
     * Terminator Data Unit
     */
    private void processTDU(TDUMessage tdu)
    {
        if(mChannelType == ChannelType.STANDARD)
        {
            broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE));
        }
    }

    /**
     * Header Data Unit - first message in a call sequence.
     */
    private void processHDU(HDUMessage hdu)
    {
        if(hdu.isValid())
        {
            broadcast(new DecoderStateEvent(this, Event.START, State.CALL));

            String to = hdu.getToID();

            mToTalkgroupMonitor.process(to);

            if(mCurrentCallEvent == null)
            {
                mCurrentCallEvent = new P25CallEvent.Builder(CallEvent.CallEventType.CALL)
                        .aliasList(getAliasList())
                        .channel(mCurrentChannel)
                        .details((hdu.isEncryptedAudio() ? "ENCRYPTED " : ""))
                        .frequency(mCurrentChannelFrequency)
                        .to(to)
                        .build();

                broadcast(mCurrentCallEvent);
            }

        }
    }

    /**
     * Terminator Data Unit with Link Control - transmitted multiple times at
     * beginning or end of call sequence and includes embedded link control messages
     */
    private void processTDULC(TDULinkControlMessage tdulc)
    {
        if(mCurrentCallEvent != null)
        {
            mCurrentCallEvent.end();
            broadcast(mCurrentCallEvent);
            mCurrentCallEvent = null;
        }

        if(tdulc.getOpcode() == LinkControlOpcode.CALL_TERMINATION_OR_CANCELLATION)
        {
            broadcast(new DecoderStateEvent(this, Event.END, State.FADE));

        }
        else
        {
            broadcast(new DecoderStateEvent(this, Event.DECODE, State.ACTIVE));
        }

        switch(tdulc.getOpcode())
        {
            case ADJACENT_SITE_STATUS_BROADCAST:
                if(tdulc instanceof AdjacentSiteStatusBroadcast)
                {
                    IAdjacentSite ias = (IAdjacentSite) tdulc;

                    mNeighborMap.put(ias.getUniqueID(), ias);

                    updateSystem(ias.getSystemID());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
                if(tdulc instanceof AdjacentSiteStatusBroadcastExplicit)
                {
                    IAdjacentSite ias = (IAdjacentSite) tdulc;

                    mNeighborMap.put(ias.getUniqueID(), ias);

                    updateSystem(ias.getSystemID());
                }
                break;
            case CALL_ALERT:
                if(tdulc instanceof CallAlert)
                {
                    CallAlert ca =
                            (CallAlert) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                            .aliasList(getAliasList())
                            .from(ca.getSourceAddress())
                            .to(ca.getTargetAddress())
                            .details("CALL ALERT")
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case CALL_TERMINATION_OR_CANCELLATION:
                /* This opcode as handled at the beginning of the method */
                break;
            case CHANNEL_IDENTIFIER_UPDATE:
                /* This message is handled by the P25MessageProcessor and
                 * inserted into any channels needing frequency band info */
                break;
            case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
                /* This message is handled by the P25MessageProcessor and
                 * inserted into any channels needing frequency band info */
                break;
            case EXTENDED_FUNCTION_COMMAND:
                if(tdulc instanceof ExtendedFunctionCommand)
                {
                    ExtendedFunctionCommand efc = (ExtendedFunctionCommand) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .to(efc.getTargetAddress())
                            .details("FUNCTION:" + efc.getExtendedFunction().getLabel() +
                                    " ARG:" + efc.getArgument())
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case GROUP_AFFILIATION_QUERY:
                if(tdulc instanceof GroupAffiliationQuery)
                {
                    GroupAffiliationQuery gaq =
                            (GroupAffiliationQuery) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                            .aliasList(getAliasList())
                            .details("GROUP AFFILIATION QUERY")
                            .from(gaq.getSourceAddress())
                            .to(gaq.getTargetAddress())
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case GROUP_VOICE_CHANNEL_UPDATE:
                /* Used only on trunked systems on the outbound channel, to
                 * reflect user activity on other channels.  We process this
                 * as a call detect */
                if(tdulc instanceof GroupVoiceChannelUpdate)
                {
                    GroupVoiceChannelUpdate gvcu = (GroupVoiceChannelUpdate) tdulc;

                    String groupA = gvcu.getGroupAddressA();

                    if(!mCallDetectTalkgroups.contains(groupA))
                    {
                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.CALL_DETECT)
                                .aliasList(getAliasList())
                                .channel(gvcu.getChannelA())
                                .details((gvcu.isEncrypted() ? "ENCRYPTED" : ""))
                                .frequency(gvcu.getDownlinkFrequencyA())
                                .to(groupA)
                                .build());

                        mCallDetectTalkgroups.add(groupA);
                    }

                    String groupB = gvcu.getGroupAddressB();

                    if(!mCallDetectTalkgroups.contains(groupB))
                    {
                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.CALL_DETECT)
                                .aliasList(getAliasList())
                                .channel(gvcu.getChannelB())
                                .details((gvcu.isEncrypted() ? "ENCRYPTED" : ""))
                                .frequency(gvcu.getDownlinkFrequencyB())
                                .to(groupB)
                                .build());

                        mCallDetectTalkgroups.add(groupB);
                    }
                }
                break;
            case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                /* Reflects other call activity on the system: CALL DETECT */
                if(mChannelType == ChannelType.STANDARD &&
                        tdulc instanceof GroupVoiceChannelUpdateExplicit)
                {
                    GroupVoiceChannelUpdateExplicit gvcue =
                            (GroupVoiceChannelUpdateExplicit) tdulc;

                    String group = gvcue.getGroupAddress();

                    if(!mCallDetectTalkgroups.contains(group))
                    {
                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.CALL_DETECT)
                                .aliasList(getAliasList())
                                .channel(gvcue.getTransmitChannel())
                                .details((gvcue.isEncrypted() ? "ENCRYPTED" : ""))
                                .frequency(gvcue.getDownlinkFrequency())
                                .to(group)
                                .build());

                        mCallDetectTalkgroups.add(group);
                    }
                }
                break;
            case GROUP_VOICE_CHANNEL_USER:
                //Used on a traffic channel to reflect that the channel is currently being used by the
                //TO talkgroup, but that channel is currently quiet (ie no voice) */
                if(tdulc instanceof GroupVoiceChannelUser)
                {
                    GroupVoiceChannelUser gvcuser = (GroupVoiceChannelUser) tdulc;

                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.ACTIVE));

                    //The from value should always be "000000" indicating that nobody is currently talking
                    mFromTalkgroupMonitor.reset();

                    String to = gvcuser.getGroupAddress();
                    mToTalkgroupMonitor.process(to);
                }
                break;
            case MESSAGE_UPDATE:
                if(tdulc instanceof MessageUpdate)
                {
                    MessageUpdate mu = (MessageUpdate) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.SDM)
                            .aliasList(getAliasList())
                            .from(mu.getSourceAddress())
                            .to(mu.getTargetAddress())
                            .details("MSG: " + mu.getShortDataMessage())
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case NETWORK_STATUS_BROADCAST:
                if(tdulc instanceof io.github.dsheirer.module.decode.p25.message.tdu.lc.NetworkStatusBroadcast)
                {
                    updateSystem(((io.github.dsheirer.module.decode.p25.message.tdu.lc.NetworkStatusBroadcast) tdulc).getSystem());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case NETWORK_STATUS_BROADCAST_EXPLICIT:
                if(tdulc instanceof NetworkStatusBroadcastExplicit)
                {
                    updateSystem(((NetworkStatusBroadcastExplicit) tdulc).getSystem());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case PROTECTION_PARAMETER_BROADCAST:
                if(tdulc instanceof io.github.dsheirer.module.decode.p25.message.tdu.lc.ProtectionParameterBroadcast)
                {
                    io.github.dsheirer.module.decode.p25.message.tdu.lc.ProtectionParameterBroadcast ppb =
                            (io.github.dsheirer.module.decode.p25.message.tdu.lc.ProtectionParameterBroadcast) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .to(ppb.getTargetAddress())
                            .details("ENCRYPTION: " +
                                    ppb.getEncryption().name() + " KEY:" +
                                    ppb.getEncryptionKey())
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case RFSS_STATUS_BROADCAST:
                if(tdulc instanceof io.github.dsheirer.module.decode.p25.message.tdu.lc.RFSSStatusBroadcast)
                {
                    io.github.dsheirer.module.decode.p25.message.tdu.lc.RFSSStatusBroadcast rfsssb =
                            (io.github.dsheirer.module.decode.p25.message.tdu.lc.RFSSStatusBroadcast) tdulc;

                    updateSystem(rfsssb.getSystem());

                    String site = rfsssb.getRFSubsystemID() + "-" +
                            rfsssb.getSiteID();

                    mSiteAttributeMonitor.process(site);

                    if(mCurrentChannel == null ||
                            !mCurrentChannel.contentEquals(rfsssb.getChannel()))
                    {
                        mCurrentChannel = rfsssb.getChannel();
                        mCurrentChannelFrequency = rfsssb.getDownlinkFrequency();
                    }
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case RFSS_STATUS_BROADCAST_EXPLICIT:
                if(tdulc instanceof RFSSStatusBroadcastExplicit)
                {
                    RFSSStatusBroadcastExplicit rfsssbe =
                            (RFSSStatusBroadcastExplicit) tdulc;

                    String site = rfsssbe.getRFSubsystemID() + "-" +
                            rfsssbe.getSiteID();

                    mSiteAttributeMonitor.process(site);

                    if(mCurrentChannel == null ||
                            !mCurrentChannel.contentEquals(rfsssbe.getTransmitChannel()))
                    {
                        mCurrentChannel = rfsssbe.getTransmitChannel();
                        mCurrentChannelFrequency = rfsssbe.getDownlinkFrequency();
                    }
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                if(tdulc instanceof io.github.dsheirer.module.decode.p25.message.tdu.lc.SecondaryControlChannelBroadcast)
                {
                    io.github.dsheirer.module.decode.p25.message.tdu.lc.SecondaryControlChannelBroadcast sccb =
                            (io.github.dsheirer.module.decode.p25.message.tdu.lc.SecondaryControlChannelBroadcast) tdulc;

                    String site = sccb.getRFSubsystemID() + "-" + sccb.getSiteID();

                    mSiteAttributeMonitor.process(site);
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                if(tdulc instanceof SecondaryControlChannelBroadcastExplicit)
                {
                    SecondaryControlChannelBroadcastExplicit sccb = (SecondaryControlChannelBroadcastExplicit) tdulc;

                    String site = sccb.getRFSubsystemID() + "-" + sccb.getSiteID();

                    mSiteAttributeMonitor.process(site);
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case STATUS_QUERY:
                if(tdulc instanceof StatusQuery)
                {
                    StatusQuery sq = (StatusQuery) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                            .aliasList(getAliasList())
                            .details("STATUS QUERY")
                            .from(sq.getSourceAddress())
                            .to(sq.getTargetAddress())
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case STATUS_UPDATE:
                if(tdulc instanceof StatusUpdate)
                {
                    StatusUpdate su = (StatusUpdate) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.STATUS)
                            .aliasList(getAliasList())
                            .details("STATUS UNIT:" + su.getUnitStatus() +
                                    " USER:" + su.getUserStatus())
                            .from(su.getSourceAddress())
                            .to(su.getTargetAddress())
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case SYSTEM_SERVICE_BROADCAST:
                /* This message doesn't provide anything we need for channel state */
                break;
            case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                if(tdulc instanceof TelephoneInterconnectAnswerRequest)
                {
                    TelephoneInterconnectAnswerRequest tiar = (TelephoneInterconnectAnswerRequest) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                            .aliasList(getAliasList())
                            .from(tiar.getTelephoneNumber())
                            .to(tiar.getTargetAddress())
                            .details("TELEPHONE CALL ALERT")
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                if(mChannelType == ChannelType.STANDARD &&
                        tdulc instanceof TelephoneInterconnectVoiceChannelUser)
                {
                    TelephoneInterconnectVoiceChannelUser tivcu = (TelephoneInterconnectVoiceChannelUser) tdulc;

                    String to = tivcu.getAddress();

                    mToTalkgroupMonitor.process(to);

                    if(mCurrentCallEvent == null)
                    {
                        mCurrentCallEvent = new P25CallEvent.Builder(CallEvent.CallEventType.TELEPHONE_INTERCONNECT)
                                .aliasList(getAliasList())
                                .channel(mCurrentChannel)
                                .details((tivcu.isEncrypted() ? "ENCRYPTED" : "") +
                                        (tivcu.isEmergency() ? " EMERGENCY" : ""))
                                .frequency(mCurrentChannelFrequency)
                                .to(to)
                                .build();

                        broadcast(mCurrentCallEvent);
                    }
                }
                break;
            case UNIT_AUTHENTICATION_COMMAND:
                if(tdulc instanceof UnitAuthenticationCommand)
                {
                    UnitAuthenticationCommand uac = (UnitAuthenticationCommand) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .to(uac.getCompleteTargetAddress())
                            .details("AUTHENTICATE")
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case UNIT_REGISTRATION_COMMAND:
                if(tdulc instanceof UnitRegistrationCommand)
                {
                    UnitRegistrationCommand urc = (UnitRegistrationCommand) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .to(urc.getCompleteTargetAddress())
                            .details("REGISTER")
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case UNIT_TO_UNIT_ANSWER_REQUEST:
                if(tdulc instanceof UnitToUnitAnswerRequest)
                {
                    UnitToUnitAnswerRequest uuar = (UnitToUnitAnswerRequest) tdulc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                            .aliasList(getAliasList())
                            .from(uuar.getSourceAddress())
                            .to(uuar.getTargetAddress())
                            .details("UNIT TO UNIT CALL ALERT")
                            .build());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                /* Used on traffic channels to indicate the current call entities */
                if(tdulc instanceof UnitToUnitVoiceChannelUser)
                {
                    UnitToUnitVoiceChannelUser uuvcu = (UnitToUnitVoiceChannelUser) tdulc;

                    mFromTalkgroupMonitor.reset();

                    String to = uuvcu.getTargetAddress();
                    mToTalkgroupMonitor.process(to);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Log optional vendor format messages that we don't yet support so that we
     * can understand those messages and eventually add support.
     */
    private void logAlternateVendorMessage(P25Message message)
    {
//		if( message.isValid() )
//		{
//			mLog.info( "PLEASE NOTIFY DEVELOPER - UNRECOGNIZED P25 VENDOR FORMAT -"
//					+ " DUID:" + message.getDUID()
//					+ " MSG:" + message.getBinaryMessage()
//					+ " CLASS:" + message.getClass() );
//		}
    }

    /**
     * Processes LDU voice frame messages.  Sends continuation events to keep
     * the channel state synchronized and processes the embedded link control
     * messages to capture/broadcast peripheral events like paging.
     */
    private void processLDU(LDUMessage ldu)
    {
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));

        if(mCurrentCallEvent == null)
        {
            mCurrentCallEvent = new P25CallEvent.Builder(CallEvent.CallEventType.CALL)
                    .aliasList(getAliasList())
                    .channel(mCurrentChannel)
                    .frequency(mCurrentChannelFrequency)
                    .build();

            broadcast(mCurrentCallEvent);
        }

        if(ldu instanceof LDU1Message)
        {
            switch(((LDU1Message) ldu).getOpcode())
            {
                case ADJACENT_SITE_STATUS_BROADCAST:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.AdjacentSiteStatusBroadcast)
                    {
                        IAdjacentSite ias = (IAdjacentSite) ldu;

                        mNeighborMap.put(ias.getUniqueID(), ias);

                        updateSystem(ias.getSystemID());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }

                    break;
                case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.AdjacentSiteStatusBroadcastExplicit)
                    {
                        IAdjacentSite ias = (IAdjacentSite) ldu;

                        mNeighborMap.put(ias.getUniqueID(), ias);

                        updateSystem(ias.getSystemID());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case CALL_ALERT:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.CallAlert)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.CallAlert ca =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.CallAlert) ldu;
                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                                .aliasList(getAliasList())
                                .from(ca.getSourceAddress())
                                .to(ca.getTargetAddress())
                                .details("CALL ALERT")
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case CALL_TERMINATION_OR_CANCELLATION:
                    broadcast(new DecoderStateEvent(this, Event.END, State.FADE));
                    mCurrentCallEvent = null;

                    if(!(ldu instanceof CallTermination))
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case CHANNEL_IDENTIFIER_UPDATE:
                    //TODO: do we need this for the activity summary?

                    /* This message is handled by the P25MessageProcessor and
                     * inserted into any channels needing frequency band info */
                    break;
                case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
                    //TODO: do we need this for the activity summary?

                    /* This message is handled by the P25MessageProcessor and
                     * inserted into any channels needing frequency band info */
                    break;
                case EXTENDED_FUNCTION_COMMAND:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.ExtendedFunctionCommand)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.ExtendedFunctionCommand efc =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.ExtendedFunctionCommand) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                                .aliasList(getAliasList())
                                .to(efc.getTargetAddress())
                                .details("FUNCTION:" + efc.getExtendedFunction().getLabel() +
                                        " ARG:" + efc.getArgument())
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case GROUP_AFFILIATION_QUERY:
                    if(mChannelType == ChannelType.STANDARD &&
                            ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupAffiliationQuery)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupAffiliationQuery gaq =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupAffiliationQuery) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                                .aliasList(getAliasList())
                                .details("GROUP AFFILIATION QUERY")
                                .from(gaq.getSourceAddress())
                                .to(gaq.getTargetAddress())
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case GROUP_VOICE_CHANNEL_UPDATE:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdate)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdate gvcu =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdate) ldu;

                        String userA = gvcu.getGroupAddressA();
                        String userB = gvcu.getGroupAddressB();

                        if(mChannelType == ChannelType.STANDARD)
                        {
                            if(!mCallDetectTalkgroups.contains(userA))
                            {
                                broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.CALL_DETECT)
                                        .aliasList(getAliasList())
                                        .channel(gvcu.getChannelA())
                                        .details((gvcu.isEncryptedLinkControlWord() ? "ENCRYPTED" : ""))
                                        .frequency(gvcu.getDownlinkFrequencyA())
                                        .to(userA)
                                        .build());

                                mCallDetectTalkgroups.add(userA);
                            }

                            if(userB != null &&
                                    !userB.contentEquals("0000") &&
                                    !mCallDetectTalkgroups.contains(userB))
                            {
                                broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.CALL_DETECT)
                                        .aliasList(getAliasList())
                                        .channel(gvcu.getChannelB())
                                        .details((gvcu.isEncryptedLinkControlWord() ? "ENCRYPTED" : ""))
                                        .frequency(gvcu.getDownlinkFrequencyB())
                                        .to(userB)
                                        .build());

                                mCallDetectTalkgroups.add(userB);
                            }
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdateExplicit)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdateExplicit gvcue =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdateExplicit) ldu;

                        String group = gvcue.getGroupAddress();

                        if(mChannelType == ChannelType.STANDARD)
                        {
                            if(!mCallDetectTalkgroups.contains(group))
                            {
                                broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.CALL_DETECT)
                                        .aliasList(getAliasList())
                                        .channel(gvcue.getTransmitChannel())
                                        .details((gvcue.isEncryptedLinkControlWord() ? "ENCRYPTED" : ""))
                                        .frequency(gvcue.getDownlinkFrequency())
                                        .to(group)
                                        .build());

                                mCallDetectTalkgroups.add(group);
                            }
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case GROUP_VOICE_CHANNEL_USER:
                    /* Indicates the current user of the current channel */
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupVoiceChannelUser)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupVoiceChannelUser gvcuser =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.GroupVoiceChannelUser) ldu;

                        mFromTalkgroupMonitor.process(gvcuser.getSourceAddress());
                        mCurrentCallEvent.setFromID(mFromTalkgroupMonitor.getValue());

                        mToTalkgroupMonitor.process(gvcuser.getGroupAddress());

                        String to = mToTalkgroupMonitor.getValue();

                        if(mCurrentCallEvent.getToID() == null || !mCurrentCallEvent.getToID().contentEquals(to))
                        {
                            mCurrentCallEvent.setToID(to);
                        }

                        if(mChannelType == ChannelType.STANDARD)
                        {
                            if(mCurrentCallEvent.getCallEventType() != CallEvent.CallEventType.GROUP_CALL)
                            {
                                mCurrentCallEvent.setCallEventType(CallEvent.CallEventType.GROUP_CALL);
                                broadcast(mCurrentCallEvent);
                            }

                            if(mCurrentCallEvent.getDetails() == null)
                            {
                                mCurrentCallEvent.setDetails(
                                        (gvcuser.isEncryptedLinkControlWord() ? "ENCRYPTED " : "") +
                                                (gvcuser.isEmergency() ? "EMERGENCY " : ""));

                                broadcast(mCurrentCallEvent);
                            }
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case MESSAGE_UPDATE:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.MessageUpdate)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.MessageUpdate mu =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.MessageUpdate) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.SDM)
                                .aliasList(getAliasList())
                                .from(mu.getSourceAddress())
                                .to(mu.getTargetAddress())
                                .details("MSG: " + mu.getShortDataMessage())
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case NETWORK_STATUS_BROADCAST:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.NetworkStatusBroadcast)
                    {
                        updateSystem(((io.github.dsheirer.module.decode.p25.message.ldu.lc.NetworkStatusBroadcast) ldu).getSystem());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case NETWORK_STATUS_BROADCAST_EXPLICIT:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.NetworkStatusBroadcastExplicit)
                    {
                        updateSystem(((io.github.dsheirer.module.decode.p25.message.ldu.lc.NetworkStatusBroadcastExplicit) ldu).getSystem());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case PROTECTION_PARAMETER_BROADCAST:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.ProtectionParameterBroadcast)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.ProtectionParameterBroadcast ppb =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.ProtectionParameterBroadcast) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                                .aliasList(getAliasList())
                                .to(ppb.getTargetAddress())
                                .details("ENCRYPTION: " +
                                        ppb.getEncryption().name() + " KEY:" +
                                        ppb.getEncryptionKey())
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case RFSS_STATUS_BROADCAST:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.RFSSStatusBroadcast)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.RFSSStatusBroadcast rfsssb =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.RFSSStatusBroadcast) ldu;

                        updateSystem(rfsssb.getSystem());

                        String site = rfsssb.getRFSubsystemID() + "-" +
                                rfsssb.getSiteID();

                        mSiteAttributeMonitor.process(site);
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case RFSS_STATUS_BROADCAST_EXPLICIT:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.RFSSStatusBroadcastExplicit)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.RFSSStatusBroadcastExplicit rfsssbe =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.RFSSStatusBroadcastExplicit) ldu;

                        String site = rfsssbe.getRFSubsystemID() + "-" +
                                rfsssbe.getSiteID();

                        mSiteAttributeMonitor.process(site);
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcast)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcast sccb =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcast) ldu;

                        String site = sccb.getRFSubsystemID() + "-" +
                                sccb.getSiteID();

                        mSiteAttributeMonitor.process(site);
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcastExplicit)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcastExplicit sccb =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcastExplicit) ldu;

                        String site = sccb.getRFSubsystemID() + "-" +
                                sccb.getSiteID();

                        mSiteAttributeMonitor.process(site);
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case STATUS_QUERY:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.StatusQuery)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.StatusQuery sq =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.StatusQuery) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                                .aliasList(getAliasList())
                                .details("STATUS QUERY")
                                .from(sq.getSourceAddress())
                                .to(sq.getTargetAddress())
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case STATUS_UPDATE:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.StatusUpdate)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.StatusUpdate su =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.StatusUpdate) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.STATUS)
                                .aliasList(getAliasList())
                                .details("STATUS UNIT:" + su.getUnitStatus() +
                                        " USER:" + su.getUserStatus())
                                .from(su.getSourceAddress())
                                .to(su.getTargetAddress())
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case SYSTEM_SERVICE_BROADCAST:
                    /* This message doesn't provide anything we need for channel state */
                    break;
                case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest tiar =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                                .aliasList(getAliasList())
                                .from(tiar.getTelephoneNumber())
                                .to(tiar.getTargetAddress())
                                .details("TELEPHONE CALL ALERT")
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                    /* Indicates the current user of the current channel */
                    if(mChannelType == ChannelType.STANDARD &&
                            ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.TelephoneInterconnectVoiceChannelUser)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.TelephoneInterconnectVoiceChannelUser tivcu =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.TelephoneInterconnectVoiceChannelUser) ldu;

                        mToTalkgroupMonitor.process(tivcu.getAddress());

                        if(mCurrentCallEvent.getCallEventType() != CallEvent.CallEventType.TELEPHONE_INTERCONNECT)
                        {
                            mCurrentCallEvent.setCallEventType(CallEvent.CallEventType.TELEPHONE_INTERCONNECT);
                            broadcast(mCurrentCallEvent);
                        }

                        if(mCurrentCallEvent.getDetails() == null)
                        {
                            mCurrentCallEvent.setDetails(
                                    (tivcu.isEncryptedLinkControlWord() ? "ENCRYPTED " : "") +
                                            (tivcu.isEmergency() ? "EMERGENCY " : ""));

                            broadcast(mCurrentCallEvent);
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case UNIT_AUTHENTICATION_COMMAND:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitAuthenticationCommand)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitAuthenticationCommand uac =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitAuthenticationCommand) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                                .aliasList(getAliasList())
                                .to(uac.getCompleteTargetAddress())
                                .details("AUTHENTICATE")
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case UNIT_REGISTRATION_COMMAND:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitRegistrationCommand)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitRegistrationCommand urc =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitRegistrationCommand) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                                .aliasList(getAliasList())
                                .to(urc.getCompleteTargetAddress())
                                .details("REGISTER")
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case UNIT_TO_UNIT_ANSWER_REQUEST:
                    if(ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitToUnitAnswerRequest)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitToUnitAnswerRequest uuar =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitToUnitAnswerRequest) ldu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                                .aliasList(getAliasList())
                                .from(uuar.getSourceAddress())
                                .to(uuar.getTargetAddress())
                                .details("UNIT TO UNIT CALL ALERT")
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                    if(mChannelType == ChannelType.STANDARD &&
                            ldu instanceof io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitToUnitVoiceChannelUser)
                    {
                        io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitToUnitVoiceChannelUser uuvcu =
                                (io.github.dsheirer.module.decode.p25.message.ldu.lc.UnitToUnitVoiceChannelUser) ldu;

                        mFromTalkgroupMonitor.process(uuvcu.getSourceAddress());

                        mToTalkgroupMonitor.process(uuvcu.getTargetAddress());

                        if(mCurrentCallEvent.getCallEventType() != CallEvent.CallEventType.UNIT_TO_UNIT_CALL)
                        {
                            mCurrentCallEvent.setCallEventType(CallEvent.CallEventType.UNIT_TO_UNIT_CALL);
                            broadcast(mCurrentCallEvent);
                        }

                        if(mCurrentCallEvent.getDetails() == null)
                        {
                            mCurrentCallEvent.setDetails(
                                    (uuvcu.isEncryptedLinkControlWord() ? "ENCRYPTED " : "") +
                                            (uuvcu.isEmergency() ? "EMERGENCY " : ""));

                            broadcast(mCurrentCallEvent);
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Process a Trunking Signalling Block message
     */
    private void processTSBK(TSBKMessage tsbk)
    {
        /* Trunking Signalling Block Messages - indicates Control Channel */
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));

        if(tsbk.getVendor() == Vendor.STANDARD)
        {
            switch(tsbk.getOpcode())
            {
                case ADJACENT_STATUS_BROADCAST:
                    if(tsbk instanceof AdjacentStatusBroadcast)
                    {
                        IAdjacentSite ias = (IAdjacentSite) tsbk;

                        mNeighborMap.put(ias.getUniqueID(), ias);

                        updateSystem(ias.getSystemID());
                    }
                    break;
                case ACKNOWLEDGE_RESPONSE:
                    processTSBKResponse(tsbk);
                    break;
                case AUTHENTICATION_COMMAND:
                    processTSBKCommand(tsbk);
                    break;
                case CALL_ALERT:
                    processTSBKPage(tsbk);
                    break;
                case DENY_RESPONSE:
                    processTSBKResponse(tsbk);
                    break;
                case EXTENDED_FUNCTION_COMMAND:
                    processTSBKCommand(tsbk);
                    break;
                case GROUP_AFFILIATION_QUERY:
                    processTSBKQuery(tsbk);
                    break;
                case GROUP_AFFILIATION_RESPONSE:
                    processTSBKResponse(tsbk);
                    break;
                case GROUP_DATA_CHANNEL_ANNOUNCEMENT:
                case GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                    processTSBKDataChannelAnnouncement(tsbk);
                    break;
                case GROUP_DATA_CHANNEL_GRANT:
                case GROUP_VOICE_CHANNEL_GRANT:
                case GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                case GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                case INDIVIDUAL_DATA_CHANNEL_GRANT:
                case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                    processTSBKChannelGrant(tsbk);
                    break;
                case IDENTIFIER_UPDATE_NON_VUHF:
                case IDENTIFIER_UPDATE_VHF_UHF_BANDS:
                case IDENTIFIER_UPDATE_TDMA:
                    IdentifierUpdate iu = (IdentifierUpdate) tsbk;

                    if(!mBands.containsKey(iu.getIdentifier()))
                    {
                        mBands.put(iu.getIdentifier(), iu);
                    }
                    break;
                case LOCATION_REGISTRATION_RESPONSE:
                case UNIT_DEREGISTRATION_ACKNOWLEDGE:
                    processTSBKResponse(tsbk);
                    break;
                case MESSAGE_UPDATE:
                    processTSBKMessage(tsbk);
                    break;
                case NETWORK_STATUS_BROADCAST:
                    mNetworkStatus = (NetworkStatusBroadcast) tsbk;
                    break;
                case PROTECTION_PARAMETER_UPDATE:
                    processTSBKResponse(tsbk);
                    break;
                case QUEUED_RESPONSE:
                    processTSBKResponse(tsbk);
                    break;
                case RADIO_UNIT_MONITOR_COMMAND:
                    processTSBKCommand(tsbk);
                    break;
                case RFSS_STATUS_BROADCAST:
                    processTSBKRFSSStatus((RFSSStatusBroadcast) tsbk);
                    break;
                case ROAMING_ADDRESS_COMMAND:
                    processTSBKCommand(tsbk);
                    break;
                case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                    SecondaryControlChannelBroadcast sccb =
                            (SecondaryControlChannelBroadcast) tsbk;

                    if(sccb.getDownlinkFrequency1() > 0)
                    {
                        mSecondaryControlChannels.add(sccb);
                    }
                    break;
                case SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                    mSNDCPDataChannel = (SNDCPDataChannelAnnouncementExplicit) tsbk;
                    break;
                case SNDCP_DATA_CHANNEL_GRANT:
                    processTSBKChannelGrant(tsbk);
                    break;
                case STATUS_QUERY:
                    processTSBKQuery(tsbk);
                    break;
                case STATUS_UPDATE:
                    processTSBKResponse(tsbk);
                    break;
                case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                case UNIT_TO_UNIT_ANSWER_REQUEST:
                    processTSBKPage(tsbk);
                    break;
                case UNIT_REGISTRATION_COMMAND:
                    processTSBKCommand(tsbk);
                    break;
                case UNIT_REGISTRATION_RESPONSE:
                    processTSBKResponse(tsbk);
                    break;
                default:
                    break;
            }
        }
        else if(tsbk.getVendor() == Vendor.MOTOROLA)
        {
            processMotorolaTSBK((MotorolaTSBKMessage) tsbk);
        }
    }

    /**
     * Process a Packet Data Unit message
     */
    private void processPDU(PDUMessage pdu)
    {
        broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));

        if(pdu instanceof PacketData || pdu instanceof PDUTypeUnknown)
        {
        }
        else if(pdu instanceof PDUConfirmedMessage)
        {
            PDUConfirmedMessage pduc = (PDUConfirmedMessage) pdu;

            switch(pduc.getPDUType())
            {
                case SNDCP_ACTIVATE_TDS_CONTEXT_ACCEPT:
                    SNDCPActivateTDSContextAccept satca =
                            (SNDCPActivateTDSContextAccept) pduc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .channel(mCurrentChannel)
                            .details("ACTIVATE SNDCP USE IP:" + satca.getIPAddress())
                            .frequency(mCurrentChannelFrequency)
                            .to(satca.getLogicalLinkID())
                            .build());
                    break;
                case SNDCP_ACTIVATE_TDS_CONTEXT_REJECT:
                    SNDCPActivateTDSContextReject satcr =
                            (SNDCPActivateTDSContextReject) pduc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .channel(mCurrentChannel)
                            .details("REJECT: SNDCP CONTEXT ACTIVATION "
                                    + "REASON:" + satcr.getReason().getLabel())
                            .frequency(mCurrentChannelFrequency)
                            .to(satcr.getLogicalLinkID())
                            .build());
                    break;
                case SNDCP_ACTIVATE_TDS_CONTEXT_REQUEST:
                    SNDCPActivateTDSContextRequest satcreq =
                            (SNDCPActivateTDSContextRequest) pduc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                            .aliasList(getAliasList())
                            .channel(mCurrentChannel)
                            .details("REQUEST SNDCP USE IP:" + satcreq.getIPAddress())
                            .frequency(mCurrentChannelFrequency)
                            .from(satcreq.getLogicalLinkID())
                            .build());
                    break;
                case SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT:
                    SNDCPDeactivateTDSContext sdtca =
                            (SNDCPDeactivateTDSContext) pduc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                            .aliasList(getAliasList())
                            .channel(mCurrentChannel)
                            .details("ACCEPT DEACTIVATE SNDCP CONTEXT")
                            .frequency(mCurrentChannelFrequency)
                            .from(sdtca.getLogicalLinkID())
                            .build());
                    break;
                case SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST:
                    SNDCPDeactivateTDSContext sdtcreq =
                            (SNDCPDeactivateTDSContext) pduc;

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                            .aliasList(getAliasList())
                            .channel(mCurrentChannel)
                            .details("REQUEST DEACTIVATE SNDCP CONTEXT")
                            .frequency(mCurrentChannelFrequency)
                            .from(sdtcreq.getLogicalLinkID())
                            .build());
                    break;
                case SNDCP_RF_CONFIRMED_DATA:
                    if(pduc instanceof SNDCPUserData)
                    {
                        SNDCPUserData sud = (SNDCPUserData) pduc;

                        StringBuilder sbFrom = new StringBuilder();
                        StringBuilder sbTo = new StringBuilder();

                        sbFrom.append(sud.getSourceIPAddress());
                        sbTo.append(sud.getDestinationIPAddress());

                        if(sud.getIPProtocol() == IPProtocol.UDP)
                        {
                            sbFrom.append(":");
                            sbFrom.append(sud.getUDPSourcePort());
                            sbTo.append(":");
                            sbTo.append(sud.getUDPDestinationPort());
                        }

                        mFromTalkgroupMonitor.reset();
                        mFromTalkgroupMonitor.process(sbFrom.toString());
                        mToTalkgroupMonitor.reset();
                        mToTalkgroupMonitor.process(pduc.getLogicalLinkID());

                        broadcast(new DecoderStateEvent(this, Event.START, State.DATA));

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.DATA_CALL)
                                .aliasList(getAliasList())
                                .channel(mCurrentChannel)
                                .details("DATA: " + sud.getPayload() +
                                        " RADIO IP:" + sbTo.toString())
                                .frequency(mCurrentChannelFrequency)
                                .from(sbFrom.toString())
                                .to(pduc.getLogicalLinkID())
                                .build());
                    }
                    else
                    {
                        mLog.error("Error - expected SNDCPUserData instance but class was: " + pduc.getClass() +
                                " and PDU Type:" + pduc.getPDUType().name());
                    }
                    break;
                case SNDCP_RF_UNCONFIRMED_DATA:
                    break;
                default:
//					mLog.debug( "PDUC - Unrecognized Message: " + pduc.toString() );
                    break;
            }
        }
        else
        {
            switch(pdu.getOpcode())
            {
                case GROUP_DATA_CHANNEL_GRANT:
                case GROUP_VOICE_CHANNEL_GRANT:
                case INDIVIDUAL_DATA_CHANNEL_GRANT:
                case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                    processPDUChannelGrant(pdu);
                    break;
                case ADJACENT_STATUS_BROADCAST:
                    if(pdu instanceof AdjacentStatusBroadcastExtended)
                    {
                        IAdjacentSite ias = (IAdjacentSite) pdu;

                        mNeighborMap.put(ias.getUniqueID(), ias);

                        updateSystem(ias.getSystemID());
                    }
                    break;
                case CALL_ALERT:
                    if(pdu instanceof CallAlertExtended)
                    {
                        CallAlertExtended ca = (CallAlertExtended) pdu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                                .aliasList(getAliasList())
                                .from(ca.getWACN() + "-" + ca.getSystemID() + "-" +
                                        ca.getSourceID())
                                .to(ca.getTargetAddress())
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case GROUP_AFFILIATION_QUERY:
                    if(pdu instanceof GroupAffiliationQueryExtended)
                    {
                        GroupAffiliationQueryExtended gaqe =
                                (GroupAffiliationQueryExtended) pdu;

                        if(mLastQueryEventID == null || !gaqe.getTargetAddress()
                                .contentEquals(mLastQueryEventID))
                        {
                            broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                                    .aliasList(getAliasList())
                                    .details("GROUP AFFILIATION")
                                    .from(gaqe.getWACN() + "-" + gaqe.getSystemID() +
                                            "-" + gaqe.getSourceID())
                                    .to(gaqe.getTargetAddress())
                                    .build());

                            mLastQueryEventID = gaqe.getToID();
                        }
                    }
                    break;
                case GROUP_AFFILIATION_RESPONSE:
                    if(pdu instanceof GroupAffiliationResponseExtended)
                    {
                        GroupAffiliationResponseExtended gar =
                                (GroupAffiliationResponseExtended) pdu;

                        if(mLastResponseEventID == null || !gar.getTargetAddress()
                                .contentEquals(mLastResponseEventID))
                        {
                            broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.RESPONSE)
                                    .aliasList(getAliasList())
                                    .details("AFFILIATION:" + gar.getResponse().name() +
                                            " FOR GROUP:" + gar.getGroupWACN() + "-" +
                                            gar.getGroupSystemID() + "-" +
                                            gar.getGroupID() + " ANNOUNCEMENT GROUP:" +
                                            gar.getAnnouncementGroupID())
                                    .from(gar.getSourceWACN() + "-" +
                                            gar.getSourceSystemID() + "-" +
                                            gar.getSourceID())
                                    .to(gar.getTargetAddress())
                                    .build());

                            mLastResponseEventID = gar.getTargetAddress();
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case MESSAGE_UPDATE:
                    if(pdu instanceof MessageUpdateExtended)
                    {
                        MessageUpdateExtended mu = (MessageUpdateExtended) pdu;

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.SDM)
                                .aliasList(getAliasList())
                                .details("MESSAGE: " + mu.getMessage())
                                .from(mu.getSourceWACN() + "-" + mu.getSourceSystemID() +
                                        "-" + mu.getSourceID())
                                .to(mu.getTargetAddress())
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case NETWORK_STATUS_BROADCAST:
                    if(pdu instanceof NetworkStatusBroadcastExtended)
                    {
                        mNetworkStatusExtended = (NetworkStatusBroadcastExtended) pdu;
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case PROTECTION_PARAMETER_BROADCAST:
                    if(pdu instanceof ProtectionParameterBroadcast)
                    {
                        mProtectionParameterBroadcast =
                                (ProtectionParameterBroadcast) pdu;
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case RFSS_STATUS_BROADCAST:
                    if(pdu instanceof RFSSStatusBroadcastExtended)
                    {
                        mRFSSStatusMessageExtended = (RFSSStatusBroadcastExtended) pdu;

                        updateNAC(mRFSSStatusMessageExtended.getNAC());
                        updateSystem(mRFSSStatusMessageExtended.getSystemID());
                        mSiteAttributeMonitor.process(mRFSSStatusMessageExtended.getRFSubsystemID() +
                                "-" + mRFSSStatusMessageExtended.getSiteID());
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case ROAMING_ADDRESS_UPDATE:
                    if(pdu instanceof RoamingAddressUpdateExtended)
                    {
                        RoamingAddressUpdateExtended raue =
                                (RoamingAddressUpdateExtended) pdu;

                        StringBuilder sb = new StringBuilder();
                        sb.append("ROAMING ADDRESS STACK A:");
                        sb.append(raue.getWACNA() + "-" + raue.getSystemIDA());

                        if(raue.isFormat2())
                        {
                            sb.append(" B:");
                            sb.append(raue.getWACNB() + "-" + raue.getSystemIDB());
                            sb.append(" C:");
                            sb.append(raue.getWACNC() + "-" + raue.getSystemIDC());
                            sb.append(" D:");
                            sb.append(raue.getWACND() + "-" + raue.getSystemIDD());
                        }

                        if(raue.isFormat3())
                        {
                            sb.append(" E:");
                            sb.append(raue.getWACNE() + "-" + raue.getSystemIDE());
                            sb.append(" F:");
                            sb.append(raue.getWACNF() + "-" + raue.getSystemIDF());
                            sb.append(" G:");
                            sb.append(raue.getWACNG() + "-" + raue.getSystemIDG());
                        }

                        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.RESPONSE)
                                .aliasList(getAliasList())
                                .details(sb.toString())
                                .from(raue.getSourceID())
                                .to(raue.getTargetAddress())
                                .build());
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case STATUS_QUERY:
                    if(pdu instanceof StatusQueryExtended)
                    {
                        StatusQueryExtended sq = (StatusQueryExtended) pdu;

                        if(mLastQueryEventID == null || !sq.getTargetAddress()
                                .contentEquals(mLastQueryEventID))
                        {
                            broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                                    .aliasList(getAliasList())
                                    .details("STATUS QUERY")
                                    .from(sq.getSourceWACN() + "-" +
                                            sq.getSourceSystemID() + "-" +
                                            sq.getSourceID())
                                    .to(sq.getTargetAddress())
                                    .build());

                            mLastQueryEventID = sq.getToID();
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case STATUS_UPDATE:
                    if(pdu instanceof StatusUpdateExtended)
                    {
                        StatusUpdateExtended su = (StatusUpdateExtended) pdu;

                        if(mLastResponseEventID == null || !mLastResponseEventID
                                .contentEquals(su.getTargetAddress()))
                        {
                            broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.RESPONSE)
                                    .aliasList(getAliasList())
                                    .details("STATUS USER: " + su.getUserStatus() +
                                            " UNIT: " + su.getUnitStatus())
                                    .from(su.getSourceWACN() + "-" +
                                            su.getSourceSystemID() + "-" +
                                            su.getSourceID())
                                    .to(su.getTargetAddress())
                                    .build());

                            mLastResponseEventID = su.getTargetAddress();
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case UNIT_REGISTRATION_RESPONSE:
                    if(pdu instanceof UnitRegistrationResponseExtended)
                    {
                        UnitRegistrationResponseExtended urr =
                                (UnitRegistrationResponseExtended) pdu;

                        if(urr.getResponse() == Response.ACCEPT)
                        {
                            mRegistrations.put(urr.getAssignedSourceAddress(),
                                    System.currentTimeMillis());
                        }

                        if(mLastRegistrationEventID == null || !mLastRegistrationEventID
                                .contentEquals(urr.getAssignedSourceAddress()))
                        {
                            broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.REGISTER)
                                    .aliasList(getAliasList())
                                    .details("REGISTRATION:" + urr.getResponse().name() +
                                            " FOR EXTERNAL SYSTEM ADDRESS: " +
                                            urr.getWACN() + "-" +
                                            urr.getSystemID() + "-" +
                                            urr.getSourceAddress() +
                                            " SOURCE ID: " + urr.getSourceID())
                                    .from(urr.getAssignedSourceAddress())
                                    .build());

                            mLastRegistrationEventID = urr.getAssignedSourceAddress();
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case UNIT_TO_UNIT_ANSWER_REQUEST:
                    if(pdu instanceof UnitToUnitAnswerRequestExplicit)
                    {
                        UnitToUnitAnswerRequestExplicit utuare =
                                (UnitToUnitAnswerRequestExplicit) pdu;

                        if(mLastPageEventID == null || !mLastPageEventID
                                .contentEquals(utuare.getTargetAddress()))
                        {
                            broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                                    .aliasList(getAliasList())
                                    .details((utuare.isEmergency() ? "EMERGENCY" : ""))
                                    .from(utuare.getWACN() + "-" +
                                            utuare.getSystemID() + "-" +
                                            utuare.getSourceID())
                                    .to(utuare.getTargetAddress())
                                    .build());

                            mLastPageEventID = utuare.getTargetAddress();
                        }
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void processPDUChannelGrant(PDUMessage pdu)
    {
        String channel = null;
        String from = null;
        String to = null;

        switch(pdu.getOpcode())
        {
            case GROUP_DATA_CHANNEL_GRANT:
                if(pdu instanceof GroupDataChannelGrantExtended)
                {
                    GroupDataChannelGrantExtended gdcge =
                            (GroupDataChannelGrantExtended) pdu;

                    channel = gdcge.getTransmitChannel();
                    from = gdcge.getSourceAddress();
                    to = gdcge.getGroupAddress();

                    if(hasCallEvent(channel, from, to))
                    {
                        updateCallEvent(channel, from, to);
                    }
                    else
                    {
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEvent.CallEventType.DATA_CALL)
                                .aliasList(getAliasList())
                                .channel(channel)
                                .details((gdcge.isEncrypted() ? "ENCRYPTED" : "") +
                                        (gdcge.isEmergency() ? " EMERGENCY" : ""))
                                .frequency(gdcge.getDownlinkFrequency())
                                .from(from)
                                .to(to)
                                .build();

                        registerCallEvent(callEvent);
                        broadcast(callEvent);
                    }
                }
                else
                {
                    logAlternateVendorMessage(pdu);
                }

                if(!mIgnoreDataCalls)
                {
                    broadcast(new TrafficChannelAllocationEvent(this,
                            mChannelCallMap.get(channel)));
                }
                break;
            case GROUP_VOICE_CHANNEL_GRANT:
                if(pdu instanceof GroupVoiceChannelGrantExplicit)
                {
                    GroupVoiceChannelGrantExplicit gvcge =
                            (GroupVoiceChannelGrantExplicit) pdu;

                    channel = gvcge.getTransmitChannel();
                    from = gvcge.getSourceAddress();
                    to = gvcge.getGroupAddress();

                    if(hasCallEvent(channel, from, to))
                    {
                        updateCallEvent(channel, from, to);
                    }
                    else
                    {
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEvent.CallEventType.GROUP_CALL)
                                .aliasList(getAliasList())
                                .channel(channel)
                                .details((gvcge.isEncrypted() ? "ENCRYPTED" : "") +
                                        (gvcge.isEmergency() ? " EMERGENCY" : ""))
                                .frequency(gvcge.getDownlinkFrequency())
                                .from(from)
                                .to(to)
                                .build();

                        registerCallEvent(callEvent);
                        broadcast(callEvent);
                    }

                    broadcast(new TrafficChannelAllocationEvent(this,
                            mChannelCallMap.get(channel)));
                }
                else
                {
                    logAlternateVendorMessage(pdu);
                }
                break;
            case INDIVIDUAL_DATA_CHANNEL_GRANT:
                if(pdu instanceof IndividualDataChannelGrantExtended)
                {
                    IndividualDataChannelGrantExtended idcge =
                            (IndividualDataChannelGrantExtended) pdu;

                    channel = idcge.getTransmitChannel();
                    from = idcge.getSourceWACN() + "-" +
                            idcge.getSourceSystemID() + "-" +
                            idcge.getSourceAddress();
                    to = idcge.getTargetAddress();

                    if(hasCallEvent(channel, from, to))
                    {
                        updateCallEvent(channel, from, to);
                    }
                    else
                    {
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEvent.CallEventType.DATA_CALL)
                                .aliasList(getAliasList())
                                .channel(channel)
                                .details((idcge.isEncrypted() ? "ENCRYPTED" : "") +
                                        (idcge.isEmergency() ? " EMERGENCY" : ""))
                                .frequency(idcge.getDownlinkFrequency())
                                .from(from)
                                .to(to)
                                .build();

                        registerCallEvent(callEvent);
                        broadcast(callEvent);
                    }

                    if(!mIgnoreDataCalls)
                    {
                        broadcast(new TrafficChannelAllocationEvent(this,
                                mChannelCallMap.get(channel)));
                    }
                }
                else
                {
                    logAlternateVendorMessage(pdu);
                }
                break;
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                TelephoneInterconnectChannelGrantExplicit ticge =
                        (TelephoneInterconnectChannelGrantExplicit) pdu;

                channel = ticge.getTransmitChannel();

                //We don't know if the subscriber is calling or being called, so
                //we use the same address in both from/to fields
                from = ticge.getAddress();
                to = ticge.getAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    P25CallEvent callEvent = new P25CallEvent.Builder(CallEvent.CallEventType.TELEPHONE_INTERCONNECT)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details((ticge.isEncrypted() ? "ENCRYPTED" : "") +
                                    (ticge.isEmergency() ? " EMERGENCY" : "") +
                                    " CALL TIMER:" + ticge.getCallTimer())
                            .frequency(ticge.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(callEvent);
                    broadcast(callEvent);
                }

                broadcast(new TrafficChannelAllocationEvent(this,
                        mChannelCallMap.get(channel)));
                break;
            case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                if(pdu instanceof UnitToUnitVoiceChannelGrantExtended)
                {
                    UnitToUnitVoiceChannelGrantExtended uuvcge =
                            (UnitToUnitVoiceChannelGrantExtended) pdu;

                    channel = uuvcge.getTransmitChannel();
                    from = uuvcge.getSourceWACN() + "-" +
                            uuvcge.getSourceSystemID() + "-" +
                            uuvcge.getSourceID();
                    to = uuvcge.getTargetAddress();

                    if(hasCallEvent(channel, from, to))
                    {
                        updateCallEvent(channel, from, to);
                    }
                    else
                    {
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEvent.CallEventType.UNIT_TO_UNIT_CALL)
                                .aliasList(getAliasList())
                                .channel(channel)
                                .details((uuvcge.isEncrypted() ? "ENCRYPTED" : "") +
                                        (uuvcge.isEmergency() ? " EMERGENCY" : ""))
                                .frequency(uuvcge.getDownlinkFrequency())
                                .from(from)
                                .to(to)
                                .build();

                        registerCallEvent(callEvent);
                        broadcast(callEvent);
                    }

                    broadcast(new TrafficChannelAllocationEvent(this,
                            mChannelCallMap.get(channel)));
                }
                else
                {
                    logAlternateVendorMessage(pdu);
                }
                break;
            case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                if(pdu instanceof UnitToUnitVoiceChannelGrantUpdateExtended)
                {
                    UnitToUnitVoiceChannelGrantUpdateExtended uuvcgue =
                            (UnitToUnitVoiceChannelGrantUpdateExtended) pdu;

                    channel = uuvcgue.getTransmitChannel();
                    from = uuvcgue.getSourceWACN() + "-" +
                            uuvcgue.getSourceSystemID() + "-" +
                            uuvcgue.getSourceID();
                    to = uuvcgue.getTargetAddress();

                    if(hasCallEvent(channel, from, to))
                    {
                        updateCallEvent(channel, from, to);
                    }
                    else
                    {
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEvent.CallEventType.UNIT_TO_UNIT_CALL)
                                .aliasList(getAliasList())
                                .channel(channel)
                                .details((uuvcgue.isEncrypted() ? "ENCRYPTED" : "") +
                                        (uuvcgue.isEmergency() ? " EMERGENCY" : ""))
                                .frequency(uuvcgue.getDownlinkFrequency())
                                .from(from)
                                .to(to)
                                .build();

                        registerCallEvent(callEvent);
                        broadcast(callEvent);
                    }

                    broadcast(new TrafficChannelAllocationEvent(this,
                            mChannelCallMap.get(channel)));

                }
                else
                {
                    logAlternateVendorMessage(pdu);
                }
                break;
            default:
                break;
        }
    }

    private void processTSBKCommand(TSBKMessage message)
    {
        switch(message.getOpcode())
        {
            case AUTHENTICATION_COMMAND:
                AuthenticationCommand ac = (AuthenticationCommand) message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                        .contentEquals(ac.getFullTargetID()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .details("AUTHENTICATE")
                            .to(ac.getWACN() + "-" + ac.getSystemID() + "-" +
                                    ac.getTargetID())
                            .build());

                    mLastCommandEventID = ac.getFullTargetID();
                }
                break;
            case EXTENDED_FUNCTION_COMMAND:
                io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.ExtendedFunctionCommand efc = (io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.ExtendedFunctionCommand) message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                        .contentEquals(efc.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.FUNCTION)
                            .aliasList(getAliasList())
                            .details("EXTENDED FUNCTION: " +
                                    efc.getExtendedFunction().getLabel())
                            .from(efc.getSourceAddress())
                            .to(efc.getTargetAddress())
                            .build());

                    mLastCommandEventID = efc.getTargetAddress();
                }
                break;
            case RADIO_UNIT_MONITOR_COMMAND:
                RadioUnitMonitorCommand rumc = (RadioUnitMonitorCommand) message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                        .contentEquals(rumc.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .details("RADIO UNIT MONITOR")
                            .from(rumc.getSourceAddress())
                            .to(rumc.getTargetAddress())
                            .build());

                    mLastCommandEventID = rumc.getTargetAddress();
                }
                break;
            case ROAMING_ADDRESS_COMMAND:
                RoamingAddressCommand rac = (RoamingAddressCommand) message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                        .contentEquals(rac.getTargetID()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .details(rac.getStackOperation().name() +
                                    " ROAMING ADDRESS " + rac.getWACN() + "-" +
                                    rac.getSystemID())
                            .to(rac.getTargetID())
                            .build());

                    mLastCommandEventID = rac.getTargetID();
                }
                break;
            case UNIT_REGISTRATION_COMMAND:
                io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.UnitRegistrationCommand urc = (io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.UnitRegistrationCommand) message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                        .contentEquals(urc.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.COMMAND)
                            .aliasList(getAliasList())
                            .details("REGISTER")
                            .from(urc.getSourceAddress())
                            .to(urc.getTargetAddress())
                            .build());

                    mLastCommandEventID = urc.getTargetAddress();
                }
                break;
            default:
                break;
        }
    }

    private void processTSBKMessage(TSBKMessage message)
    {
        io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.MessageUpdate mu = (io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.MessageUpdate) message;

        broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.SDM)
                .aliasList(getAliasList())
                .details("MESSAGE: " + mu.getMessage())
                .from(mu.getSourceAddress())
                .to(mu.getTargetAddress())
                .build());
    }

    private void processTSBKQuery(TSBKMessage message)
    {
        switch(message.getOpcode())
        {
            case GROUP_AFFILIATION_QUERY:
                io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.GroupAffiliationQuery gaq = (io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.GroupAffiliationQuery) message;

                if(mLastQueryEventID == null || !mLastQueryEventID
                        .contentEquals(gaq.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                            .aliasList(getAliasList())
                            .details("GROUP AFFILIATION")
                            .from(gaq.getSourceAddress())
                            .to(gaq.getTargetAddress())
                            .build());
                }
                break;
            case STATUS_QUERY:
                io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.StatusQuery sq = (io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.StatusQuery) message;

                if(mLastQueryEventID == null || !mLastQueryEventID
                        .contentEquals(sq.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.QUERY)
                            .aliasList(getAliasList())
                            .details("STATUS QUERY")
                            .from(sq.getSourceAddress())
                            .to(sq.getTargetAddress())
                            .build());
                }
                break;
            default:
                break;
        }
    }

    private void processTSBKResponse(TSBKMessage message)
    {
        switch(message.getOpcode())
        {
            case ACKNOWLEDGE_RESPONSE:
                AcknowledgeResponse ar = (AcknowledgeResponse) message;

                if(mLastResponseEventID == null || !ar.getTargetAddress()
                        .contentEquals(mLastResponseEventID))
                {
                    String to = ar.getTargetAddress();

                    if(ar.hasAdditionalInformation() && ar.hasExtendedAddress())
                    {
                        to = ar.getWACN() + "-" + ar.getSystemID() + "-" +
                                ar.getTargetAddress();
                    }

                    String from = null;

                    if(ar.hasAdditionalInformation() && !ar.hasExtendedAddress())
                    {
                        from = ar.getFromID();
                    }

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.RESPONSE)
                            .aliasList(getAliasList())
                            .details("ACKNOWLEDGE")
                            .from(from)
                            .to(to)
                            .build());

                    mLastResponseEventID = ar.getTargetAddress();
                }
                break;
            case DENY_RESPONSE:
                DenyResponse dr = (DenyResponse) message;

                if(mLastResponseEventID == null || !dr.getTargetAddress()
                        .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.RESPONSE)
                            .aliasList(getAliasList())
                            .details("DENY REASON: " + dr.getReason().name() +
                                    " REQUESTED: " + dr.getServiceType().name())
                            .from(dr.getSourceAddress())
                            .to(dr.getTargetAddress())
                            .build());

                    mLastResponseEventID = dr.getTargetAddress();
                }
                break;
            case GROUP_AFFILIATION_RESPONSE:
                GroupAffiliationResponse gar = (GroupAffiliationResponse) message;

                if(mLastResponseEventID == null || !gar.getTargetAddress()
                        .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.RESPONSE)
                            .aliasList(getAliasList())
                            .details("AFFILIATION:" + gar.getResponse().name() +
                                    " FOR " + gar.getAffiliationScope() +
                                    " GROUP:" + gar.getGroupAddress() +
                                    " ANNOUNCEMENT GROUP:" +
                                    gar.getAnnouncementGroupAddress())
                            .to(gar.getTargetAddress())
                            .build());

                    mLastResponseEventID = gar.getTargetAddress();
                }
                break;
            case LOCATION_REGISTRATION_RESPONSE:
                LocationRegistrationResponse lrr =
                        (LocationRegistrationResponse) message;

                if(lrr.getResponse() == Response.ACCEPT)
                {
                    mRegistrations.put(lrr.getTargetAddress(),
                            System.currentTimeMillis());
                }

                if(mLastRegistrationEventID == null ||
                        !mLastRegistrationEventID.contentEquals(lrr.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.REGISTER)
                            .aliasList(getAliasList())
                            .details("REGISTRATION:" + lrr.getResponse().name() +
                                    " SITE: " + lrr.getRFSSID() + "-" +
                                    lrr.getSiteID())
                            .from(lrr.getTargetAddress())
                            .to(lrr.getGroupAddress())
                            .build());

                    mLastRegistrationEventID = lrr.getTargetAddress();
                }
                break;
            case PROTECTION_PARAMETER_UPDATE:
                ProtectionParameterUpdate ppu = (ProtectionParameterUpdate) message;

                if(mLastResponseEventID == null || !ppu.getTargetAddress()
                        .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.RESPONSE)
                            .aliasList(getAliasList())
                            .details("USE ENCRYPTION ALGORITHM:" +
                                    ppu.getAlgorithm().name() + " KEY:" +
                                    ppu.getKeyID())
                            .to(ppu.getTargetAddress())
                            .build());

                    mLastResponseEventID = ppu.getTargetAddress();
                }
                break;
            case QUEUED_RESPONSE:
                QueuedResponse qr = (QueuedResponse) message;

                if(mLastResponseEventID == null || !qr.getTargetAddress()
                        .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.RESPONSE)
                            .aliasList(getAliasList())
                            .details("QUEUED REASON: " + qr.getReason().name() +
                                    " REQUESTED: " + qr.getServiceType().name())
                            .from(qr.getSourceAddress())
                            .to(qr.getTargetAddress())
                            .build());

                    mLastResponseEventID = qr.getTargetAddress();
                }
                break;
            case STATUS_UPDATE:
                io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.StatusUpdate su =
                        (io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.StatusUpdate) message;

                if(mLastResponseEventID == null || !su.getTargetAddress()
                        .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.RESPONSE)
                            .aliasList(getAliasList())
                            .details("STATUS USER: " + su.getUserStatus() +
                                    " UNIT: " + su.getUnitStatus())
                            .from(su.getSourceAddress())
                            .to(su.getTargetAddress())
                            .build());

                    mLastResponseEventID = su.getTargetAddress();
                }

                break;
            case UNIT_REGISTRATION_RESPONSE:
                UnitRegistrationResponse urr = (UnitRegistrationResponse) message;

                if(urr.getResponse() == Response.ACCEPT)
                {
                    mRegistrations.put(urr.getSourceAddress(),
                            System.currentTimeMillis());
                }

                if(mLastRegistrationEventID == null ||
                        !mLastRegistrationEventID.contentEquals(urr.getSourceAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.REGISTER)
                            .aliasList(getAliasList())
                            .details("REGISTRATION:" + urr.getResponse().name() +
                                    " SYSTEM: " + urr.getSystemID() +
                                    " SOURCE ID: " + urr.getSourceID())
                            .from(urr.getSourceAddress())
                            .build());

                    mLastRegistrationEventID = urr.getSourceAddress();
                }
                break;
            case UNIT_DEREGISTRATION_ACKNOWLEDGE:
                UnitDeregistrationAcknowledge udr =
                        (UnitDeregistrationAcknowledge) message;

                if(mLastRegistrationEventID == null ||
                        !mLastRegistrationEventID.contentEquals(udr.getSourceID()))
                {

                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.DEREGISTER)
                            .aliasList(getAliasList())
                            .from(udr.getSourceID())
                            .build());

                    List<String> keysToRemove = new ArrayList<String>();

                    /* Remove this radio from the registrations set */
                    for(String key : mRegistrations.keySet())
                    {
                        if(key.startsWith(udr.getSourceID()))
                        {
                            keysToRemove.add(key);
                        }
                    }

                    for(String key : keysToRemove)
                    {
                        mRegistrations.remove(key);
                    }

                    mLastRegistrationEventID = udr.getSourceID();
                }
                break;
            default:
                break;
        }
    }

    private void processTSBKRFSSStatus(RFSSStatusBroadcast message)
    {
        mRFSSStatusMessage = message;

        updateNAC(message.getNAC());

        updateSystem(message.getSystemID());

        mSiteAttributeMonitor.process(message.getRFSubsystemID() + "-" + message.getSiteID());
    }

    private void processTSBKDataChannelAnnouncement(TSBKMessage message)
    {
        switch(message.getOpcode())
        {
            case GROUP_DATA_CHANNEL_ANNOUNCEMENT:
                GroupDataChannelAnnouncement gdca = (GroupDataChannelAnnouncement) message;

                broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.ANNOUNCEMENT)
                        .aliasList(getAliasList())
                        .channel(gdca.getChannel1())
                        .details((gdca.isEncrypted() ? "ENCRYPTED" : "") +
                                (gdca.isEmergency() ? " EMERGENCY" : ""))
                        .frequency(gdca.getDownlinkFrequency1())
                        .to(gdca.getGroupAddress1())
                        .build());

                if(gdca.hasChannelNumber2())
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.ANNOUNCEMENT)
                            .aliasList(getAliasList())
                            .channel(gdca.getChannel2())
                            .details((gdca.isEncrypted() ? "ENCRYPTED" : "") +
                                    (gdca.isEmergency() ? " EMERGENCY" : ""))
                            .frequency(gdca.getDownlinkFrequency2())
                            .to(gdca.getGroupAddress2())
                            .build());
                }
                break;
            case GROUP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                GroupDataChannelAnnouncementExplicit gdcae =
                        (GroupDataChannelAnnouncementExplicit) message;

                broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.DATA_CALL)
                        .aliasList(getAliasList())
                        .channel(gdcae.getTransmitChannel())
                        .details((gdcae.isEncrypted() ? "ENCRYPTED" : "") +
                                (gdcae.isEmergency() ? " EMERGENCY" : ""))
                        .frequency(gdcae.getDownlinkFrequency())
                        .to(gdcae.getGroupAddress())
                        .build());
                break;
            default:
                break;
        }
    }

    /**
     * Process Motorola vendor-specific Trunking Signaling Block messages
     */
    private void processMotorolaTSBK(MotorolaTSBKMessage tsbk)
    {
        String channel;
        String from;
        String to;

        switch(((MotorolaTSBKMessage) tsbk).getMotorolaOpcode())
        {
            case PATCH_GROUP_CHANNEL_GRANT:
                //Cleanup patch groups - auto-expire any patch groups before we allocate a channel
                mPatchGroupManager.cleanupPatchGroups();

                PatchGroupVoiceChannelGrant pgvcg = (PatchGroupVoiceChannelGrant) tsbk;

                channel = pgvcg.getChannel();
                from = pgvcg.getSourceAddress();
                to = pgvcg.getPatchGroupAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(pgvcg.isTDMAChannel() ? "TDMA " : "");
                    details.append(pgvcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(pgvcg.getPriority());
                    details.append(pgvcg.isEncryptedChannel() ? " ENCRYPTED" : "");
                    details.append(" ").append(pgvcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEvent.CallEventType.PATCH_GROUP_CALL)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(pgvcg.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!pgvcg.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }
                break;
            case PATCH_GROUP_CHANNEL_GRANT_UPDATE:
                //Cleanup patch groups - auto-expire any patch groups before we allocate a channel
                mPatchGroupManager.cleanupPatchGroups();

                PatchGroupVoiceChannelGrantUpdate gvcgu = (PatchGroupVoiceChannelGrantUpdate) tsbk;

                channel = gvcgu.getChannel1();
                to = gvcgu.getPatchGroupAddress1();

                if(hasCallEvent(channel, null, to))
                {
                    updateCallEvent(channel, null, to);
                }
                else
                {
                    P25CallEvent event = new P25CallEvent.Builder(CallEvent.CallEventType.PATCH_GROUP_CALL)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details((gvcgu.isTDMAChannel1() ? "TDMA " : "") +
                                    (gvcgu.isEncrypted() ? "ENCRYPTED " : ""))
                            .frequency(gvcgu.getDownlinkFrequency1())
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!gvcgu.isTDMAChannel1())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }

                String channel2 = gvcgu.getChannel2();
                String to2 = gvcgu.getPatchGroupAddress2();

                if(hasCallEvent(channel2, null, to2))
                {
                    updateCallEvent(channel2, null, to2);
                }
                else
                {
                    P25CallEvent event = new P25CallEvent.Builder(CallEvent.CallEventType.PATCH_GROUP_CALL)
                            .aliasList(getAliasList())
                            .channel(channel2)
                            .details((gvcgu.isTDMAChannel2() ? "TDMA " : "") +
                                    (gvcgu.isEncrypted() ? "ENCRYPTED " : ""))
                            .frequency(gvcgu.getDownlinkFrequency2())
                            .to(to2)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!gvcgu.isTDMAChannel2())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel2)));
                }

                break;
            case PATCH_GROUP_ADD:
                PatchGroupAdd pga = (PatchGroupAdd) tsbk;
                mPatchGroupManager.updatePatchGroup(pga.getPatchGroupAddress(), pga.getPatchedTalkgroups());
                break;
            case PATCH_GROUP_DELETE:
                PatchGroupDelete pgd = (PatchGroupDelete) tsbk;
                mPatchGroupManager.removePatchGroup(pgd.getPatchGroupAddress());
                break;
            case CCH_PLANNED_SHUTDOWN:
                if(!mControlChannelShutdownLogged)
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.NOTIFICATION)
                            .details("PLANNED CONTROL CHANNEL SHUTDOWN")
                            .build());

                    mControlChannelShutdownLogged = true;
                }
                break;
        }
    }

    /**
     * Process a traffic channel allocation message
     */
    private void processTSBKChannelGrant(TSBKMessage message)
    {
        //Cleanup patch groups - auto-expire any patch groups before we allocate a channel
        mPatchGroupManager.cleanupPatchGroups();

        String channel = null;
        String from = null;
        String to = null;

        switch(message.getOpcode())
        {
            case GROUP_DATA_CHANNEL_GRANT:
                GroupDataChannelGrant gdcg = (GroupDataChannelGrant) message;

                channel = gdcg.getChannel();
                from = gdcg.getSourceAddress();
                to = gdcg.getGroupAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(gdcg.isTDMAChannel() ? "TDMA " : "");
                    details.append(gdcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(gdcg.getPriority()).append(" ");
                    details.append(gdcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(gdcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEvent.CallEventType.DATA_CALL)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(gdcg.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!mIgnoreDataCalls && !gdcg.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this,
                            mChannelCallMap.get(channel)));
                }
                break;
            case GROUP_VOICE_CHANNEL_GRANT:
                GroupVoiceChannelGrant gvcg = (GroupVoiceChannelGrant) message;

                channel = gvcg.getChannel();
                from = gvcg.getSourceAddress();
                to = gvcg.getGroupAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(gvcg.isTDMAChannel() ? "TDMA " : "");
                    details.append(gvcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(gvcg.getPriority()).append(" ");
                    details.append(gvcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(gvcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEvent.CallEventType.GROUP_CALL)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(gvcg.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!gvcg.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }
                break;
            case GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                GroupVoiceChannelGrantUpdate gvcgu = (GroupVoiceChannelGrantUpdate) message;

                channel = gvcgu.getChannel1();
                from = null;
                to = gvcgu.getGroupAddress1();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(gvcgu.isTDMAChannel() ? "TDMA " : "");
                    details.append(gvcgu.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(gvcgu.getPriority()).append(" ");
                    details.append(gvcgu.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(gvcgu.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEvent.CallEventType.GROUP_CALL)
                            .aliasList(getAliasList())
                            .channel(gvcgu.getChannel1())
                            .details(details.toString())
                            .frequency(gvcgu.getDownlinkFrequency1())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!gvcgu.isTDMAChannel1())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }

                if(gvcgu.hasChannelNumber2())
                {
                    channel = gvcgu.getChannel2();
                    to = gvcgu.getGroupAddress2();

                    if(hasCallEvent(channel, from, to))
                    {
                        updateCallEvent(channel, from, to);
                    }
                    else
                    {
                        StringBuilder details = new StringBuilder();
                        details.append(gvcgu.isTDMAChannel() ? "TDMA " : "");
                        details.append(gvcgu.isEmergency() ? "EMERGENCY " : "");
                        details.append("PRI:").append(gvcgu.getPriority()).append(" ");
                        details.append(gvcgu.isEncryptedChannel() ? "ENCRYPTED " : "");
                        details.append(gvcgu.getSessionMode().name());

                        P25CallEvent event2 = new P25CallEvent.Builder(CallEvent.CallEventType.GROUP_CALL)
                                .aliasList(getAliasList())
                                .channel(gvcgu.getChannel2())
                                .details(details.toString())
                                .frequency(gvcgu.getDownlinkFrequency2())
                                .to(gvcgu.getGroupAddress2())
                                .build();

                        registerCallEvent(event2);
                        broadcast(event2);
                    }

                    if(!gvcgu.isTDMAChannel2())
                    {
                        broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                    }
                }
                break;
            case GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                GroupVoiceChannelGrantUpdateExplicit gvcgue =
                        (GroupVoiceChannelGrantUpdateExplicit) message;

                channel = gvcgue.getTransmitChannelIdentifier() + "-" +
                        gvcgue.getTransmitChannelNumber();
                from = null;
                to = gvcgue.getGroupAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(gvcgue.isTDMAChannel() ? "TDMA " : "");
                    details.append(gvcgue.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(gvcgue.getPriority()).append(" ");
                    details.append(gvcgue.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(gvcgue.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEvent.CallEventType.GROUP_CALL)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(gvcgue.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!gvcgue.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }
                break;
            case INDIVIDUAL_DATA_CHANNEL_GRANT:
                IndividualDataChannelGrant idcg = (IndividualDataChannelGrant) message;

                channel = idcg.getChannel();
                from = idcg.getSourceAddress();
                to = idcg.getTargetAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(idcg.isTDMAChannel() ? "TDMA " : "");
                    details.append(idcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(idcg.getPriority()).append(" ");
                    details.append(idcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(idcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEvent.CallEventType.DATA_CALL)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(idcg.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!mIgnoreDataCalls && !idcg.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }
                break;
            case SNDCP_DATA_CHANNEL_GRANT:
                SNDCPDataChannelGrant sdcg = (SNDCPDataChannelGrant) message;

                channel = sdcg.getTransmitChannel();
                from = null;
                to = sdcg.getTargetAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(sdcg.isTDMAChannel() ? "TDMA " : "");
                    details.append(sdcg.isEmergency() ? "EMERGENCY " : "");
                    details.append(sdcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(sdcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEvent.CallEventType.DATA_CALL)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(sdcg.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!mIgnoreDataCalls && !sdcg.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }
                break;
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                TelephoneInterconnectVoiceChannelGrant tivcg = (TelephoneInterconnectVoiceChannelGrant) message;

                channel = tivcg.getChannel();
                from = null;
                /* Address is ambiguous and could mean either source or target,
                 * so we'll place the value in the to field */
                to = tivcg.getAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(tivcg.isTDMAChannel() ? "TDMA " : "");
                    details.append(tivcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(tivcg.getPriority()).append(" ");
                    details.append(tivcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(tivcg.getSessionMode().name()).append(" ");
                    details.append("CALL TIMER:").append(tivcg.getCallTimer());

                    P25CallEvent event = new P25CallEvent.Builder(
                            CallEvent.CallEventType.TELEPHONE_INTERCONNECT)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(tivcg.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!tivcg.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }
                break;
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                TelephoneInterconnectVoiceChannelGrantUpdate tivcgu = (TelephoneInterconnectVoiceChannelGrantUpdate) message;

                channel = tivcgu.getChannelIdentifier() + "-" + tivcgu.getChannelNumber();
                from = null;

                /* Address is ambiguous and could mean either source or target,
                 * so we'll place the value in the to field */
                to = tivcgu.getAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(tivcgu.isTDMAChannel() ? "TDMA " : "");
                    details.append(tivcgu.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(tivcgu.getPriority()).append(" ");
                    details.append(tivcgu.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(tivcgu.getSessionMode().name()).append(" ");
                    details.append("CALL TIMER:").append(tivcgu.getCallTimer());

                    P25CallEvent event = new P25CallEvent.Builder(
                            CallEvent.CallEventType.TELEPHONE_INTERCONNECT)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(tivcgu.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!tivcgu.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }
                break;
            case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                UnitToUnitVoiceChannelGrant uuvcg = (UnitToUnitVoiceChannelGrant) message;

                channel = uuvcg.getChannelIdentifier() + "-" + uuvcg.getChannelNumber();
                from = uuvcg.getSourceAddress();
                to = uuvcg.getTargetAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(uuvcg.isTDMAChannel() ? "TDMA " : "");
                    details.append(uuvcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(uuvcg.getPriority()).append(" ");
                    details.append(uuvcg.isEncryptedChannel() ? " ENCRYPTED " : "");
                    details.append(uuvcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(
                            CallEvent.CallEventType.UNIT_TO_UNIT_CALL)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(uuvcg.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!uuvcg.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }
                break;
            case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                UnitToUnitVoiceChannelGrantUpdate uuvcgu = (UnitToUnitVoiceChannelGrantUpdate) message;

                channel = uuvcgu.getChannelIdentifier() + "-" + uuvcgu.getChannelNumber();
                from = uuvcgu.getSourceAddress();
                to = uuvcgu.getTargetAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(uuvcgu.isTDMAChannel() ? "TDMA " : "");
                    details.append(uuvcgu.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(uuvcgu.getPriority()).append(" ");
                    details.append(uuvcgu.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(uuvcgu.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(
                            CallEvent.CallEventType.UNIT_TO_UNIT_CALL)
                            .aliasList(getAliasList())
                            .channel(channel)
                            .details(details.toString())
                            .frequency(uuvcgu.getDownlinkFrequency())
                            .from(from)
                            .to(to)
                            .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                if(!uuvcgu.isTDMAChannel())
                {
                    broadcast(new TrafficChannelAllocationEvent(this, mChannelCallMap.get(channel)));
                }
                break;
            default:
                break;
        }
    }

    private void updateSystem(String system)
    {
        if(mSystem == null || (system != null && !mSystem.contentEquals(system)))
        {
            mSystem = system;
            broadcastSystemAndNACUpdate();
        }
    }

    /**
     * Broadcasts an update to the NAC
     */
    private void updateNAC(String nac)
    {
        if(mNAC == null || (nac != null && !mNAC.contentEquals(nac)))
        {
            mNAC = nac;
            broadcastSystemAndNACUpdate();
        }
    }

    private void broadcastSystemAndNACUpdate()
    {
        String label = String.format("SYS:%s NAC:%s", mSystem, mNAC);
        broadcast(new AttributeChangeRequest<String>(Attribute.NETWORK_ID_1, label));
    }

    /**
     * Process a unit paging event message
     */
    private void processTSBKPage(TSBKMessage message)
    {
        switch(message.getOpcode())
        {
            case CALL_ALERT:
                io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.CallAlert ca = (io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.CallAlert) message;

                broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                        .aliasList(getAliasList())
                        .from(ca.getSourceID())
                        .to(ca.getTargetAddress())
                        .build());
                break;
            case UNIT_TO_UNIT_ANSWER_REQUEST:
                io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.UnitToUnitAnswerRequest utuar = (io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.UnitToUnitAnswerRequest) message;

                if(mLastPageEventID == null || !mLastPageEventID
                        .contentEquals(utuar.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                            .aliasList(getAliasList())
                            .details((utuar.isEmergency() ? "EMERGENCY" : ""))
                            .from(utuar.getSourceAddress())
                            .to(utuar.getTargetAddress())
                            .build());

                    mLastPageEventID = utuar.getTargetAddress();
                }
                break;
            case SNDCP_DATA_PAGE_REQUEST:
                SNDCPDataPageRequest sdpr = (SNDCPDataPageRequest) message;

                if(mLastPageEventID == null || !mLastPageEventID
                        .contentEquals(sdpr.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                            .aliasList(getAliasList())
                            .details("SNDCP DATA DAC: " +
                                    sdpr.getDataAccessControl() +
                                    " NSAPI:" + sdpr.getNSAPI())
                            .to(sdpr.getTargetAddress())
                            .build());

                    mLastPageEventID = sdpr.getTargetAddress();
                }
                break;
            case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.TelephoneInterconnectAnswerRequest tiar =
                        (io.github.dsheirer.module.decode.p25.message.tsbk.osp.voice.TelephoneInterconnectAnswerRequest) message;

                if(mLastPageEventID == null || !mLastPageEventID
                        .contentEquals(tiar.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEvent.CallEventType.PAGE)
                            .aliasList(getAliasList())
                            .details(("TELEPHONE INTERCONNECT"))
                            .from(tiar.getTelephoneNumber())
                            .to(tiar.getTargetAddress())
                            .build());

                    mLastPageEventID = tiar.getTargetAddress();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("Activity Summary - Decoder:P25 ").append(getModulation().getLabel()).append("\n");
        sb.append(DIVIDER1);
        sb.append("SITE ");

        if(mNetworkStatus != null)
        {
            sb.append("NAC:" + mNetworkStatus.getNAC());
            sb.append("h WACN:" + mNetworkStatus.getWACN());
            sb.append("h SYS:" + mNetworkStatus.getSystemID());
            sb.append("h [").append(mNetworkStatus.getNetworkCallsign()).append("]");
            sb.append(" LRA:" + mNetworkStatus.getLocationRegistrationArea());
        }
        else if(mNetworkStatusExtended != null)
        {
            sb.append("NAC:" + mNetworkStatusExtended.getNAC());
            sb.append("h WACN:" + mNetworkStatusExtended.getWACN());
            sb.append("h SYS:" + mNetworkStatusExtended.getSystemID());
            sb.append("h [").append(mNetworkStatusExtended.getNetworkCallsign()).append("]");
            sb.append(" LRA:" + mNetworkStatusExtended.getLocationRegistrationArea());
        }

        String site = null;

        if(mRFSSStatusMessage != null)
        {
            site = mRFSSStatusMessage.getRFSubsystemID() + "-" + mRFSSStatusMessage.getSiteID();
        }
        else if(mRFSSStatusMessageExtended != null)
        {
            site = mRFSSStatusMessageExtended.getRFSubsystemID() + "-" + mRFSSStatusMessageExtended.getSiteID();
        }

        sb.append("h RFSS-SITE:").append(site).append("h");

        if(hasAliasList())
        {
            Alias siteAlias = getAliasList().getSiteID(site);

            if(siteAlias != null)
            {
                sb.append(" " + siteAlias.getName());
            }
        }

        sb.append("\n").append(DIVIDER2);

        if(mNetworkStatus != null)
        {
            sb.append("SERVICES: " + SystemService.toString(mNetworkStatus.getSystemServiceClass())).append("\n");
            sb.append(DIVIDER2);
            sb.append("PCCH DOWNLINK ")
                    .append(mFrequencyFormatter.format((double) mNetworkStatus.getDownlinkFrequency() / 1E6d))
                    .append(" [").append(mNetworkStatus.getIdentifier()).append("-").append(mNetworkStatus.getChannel())
                    .append("] UPLINK ")
                    .append(mFrequencyFormatter.format((double) mNetworkStatus.getUplinkFrequency() / 1E6d))
                    .append(" [").append(mNetworkStatus.getIdentifier()).append("-").append(mNetworkStatus.getChannel())
                    .append("]\n");
        }
        else if(mNetworkStatusExtended != null)
        {
            sb.append("\nSERVICES:").append(SystemService.toString(mNetworkStatusExtended.getSystemServiceClass()))
                    .append("\n");
            sb.append(DIVIDER2);
            sb.append("PCCH DOWNLINK ")
                    .append(mFrequencyFormatter.format((double) mNetworkStatusExtended.getDownlinkFrequency() / 1E6d))
                    .append(" [").append(mNetworkStatusExtended.getTransmitIdentifier()).append("-")
                    .append(mNetworkStatusExtended.getTransmitChannel()).append("] UPLINK ")
                    .append(mFrequencyFormatter.format((double) mNetworkStatusExtended.getUplinkFrequency() / 1E6d))
                    .append(" [").append(mNetworkStatusExtended.getReceiveIdentifier()).append("-")
                    .append(mNetworkStatusExtended.getReceiveChannel()).append("]\n");
        }

        if(mSecondaryControlChannels.isEmpty())
        {
            sb.append("SCCH: NONE\n");
        }
        else
        {
            for(SecondaryControlChannelBroadcast sec : mSecondaryControlChannels)
            {
                sb.append("SCCH DOWNLINK ")
                        .append(mFrequencyFormatter.format((double) sec.getDownlinkFrequency1() / 1E6d))
                        .append(" [").append(sec.getIdentifier1()).append("-").append(sec.getChannel1()).append("] UPLINK ")
                        .append(mFrequencyFormatter.format((double) sec.getUplinkFrequency1() / 1E6d))
                        .append(" [").append(sec.getIdentifier1()).append("-").append(sec.getChannel1()).append("]");

                if(sec.hasChannel2())
                {
                    sb.append("  SCCH 2 DOWNLINK ")
                            .append(mFrequencyFormatter.format((double) sec.getDownlinkFrequency2() / 1E6d))
                            .append(" [").append(sec.getIdentifier2()).append("-").append(sec.getChannel2())
                            .append("] UPLINK ").append(mFrequencyFormatter.format((double) sec.getUplinkFrequency2() / 1E6d))
                            .append(" [").append(sec.getIdentifier2()).append("-").append(sec.getChannel2()).append("]");
                }

                sb.append("\n");
            }
        }


        if(mSNDCPDataChannel != null)
        {
            sb.append("SNDCP DOWNLINK ")
                    .append(mFrequencyFormatter.format((double) mSNDCPDataChannel.getDownlinkFrequency() / 1E6D))
                    .append(" [").append(mSNDCPDataChannel.getTransmitChannel()).append("]").append(" UPLINK ")
                    .append(mFrequencyFormatter.format((double) mSNDCPDataChannel.getUplinkFrequency() / 1E6D))
                    .append(" [").append(mSNDCPDataChannel.getReceiveChannel()).append("]\n");
        }

        if(mProtectionParameterBroadcast != null)
        {
            sb.append(DIVIDER2);
            sb.append("ENCRYPTION TYPE:").append(mProtectionParameterBroadcast.getEncryptionType().name());
            sb.append(" ALGORITHM:").append(mProtectionParameterBroadcast.getAlgorithmID());
            sb.append(" KEY:").append(mProtectionParameterBroadcast.getKeyID());
            sb.append(" INBOUND IV:").append(mProtectionParameterBroadcast.getInboundInitializationVector());
            sb.append(" OUTBOUND IV:").append(mProtectionParameterBroadcast.getOutboundInitializationVector());
            sb.append("\n");
        }

        List<Integer> identifiers = new ArrayList<>(mBands.keySet());

        Collections.sort(identifiers);

        sb.append(DIVIDER2).append("FREQUENCY BANDS:\n");
        for(Integer id : identifiers)
        {
            IBandIdentifier band = mBands.get(id);
            sb.append(band.toString()).append("\n");
//            sb.append("  ").append(id);
//            sb.append(" - BASE: " + mFrequencyFormatter.format(
//                (double)band.getBaseFrequency() / 1E6d));
//            sb.append(" CHANNEL SIZE: " + mFrequencyFormatter.format(
//                (double)band.getChannelSpacing() / 1E6d));
//            sb.append(" UPLINK OFFSET: " + mFrequencyFormatter.format(
//                (double)band.getTransmitOffset() / 1E6D));
//            sb.append("\n");
        }

        sb.append(DIVIDER2).append("NEIGHBOR SITES: ");

        if(mNeighborMap.isEmpty())
        {
            sb.append("NONE\n");
        }
        else
        {
            for(IAdjacentSite neighbor : mNeighborMap.values())
            {
                sb.append("\n");
                sb.append("NAC:").append(((P25Message) neighbor).getNAC());
                sb.append("h SYSTEM:" + neighbor.getSystemID());
                sb.append("h LRA:" + neighbor.getLRA());

                String neighborID = neighbor.getRFSS() + "-" + neighbor.getSiteID();
                sb.append("h RFSS-SITE:" + neighborID);

                sb.append("h ");

                if(hasAliasList())
                {
                    Alias siteAlias = getAliasList().getSiteID(neighborID);

                    if(siteAlias != null)
                    {
                        sb.append(siteAlias.getName());
                    }
                }

                sb.append("\n  PCCH: DOWNLINK ")
                        .append(mFrequencyFormatter.format((double) neighbor.getDownlinkFrequency() / 1E6d))
                        .append(" [").append(neighbor.getDownlinkChannel()).append("] UPLINK:")
                        .append(mFrequencyFormatter.format((double) neighbor.getUplinkFrequency() / 1E6d))
                        .append(" [").append(neighbor.getDownlinkChannel()).append("] SERVICES: ")
                        .append(neighbor.getSystemServiceClass());
            }
        }

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
            case TRAFFIC_CHANNEL_ALLOCATION:
                if(event.getSource() != P25DecoderState.this)
                {
                    if(event instanceof TrafficChannelAllocationEvent)
                    {
                        TrafficChannelAllocationEvent allocationEvent = (TrafficChannelAllocationEvent) event;

                        mCurrentCallEvent = (P25CallEvent) allocationEvent.getCallEvent();

                        mCurrentChannel = allocationEvent.getCallEvent().getChannel();
                        broadcast(new AttributeChangeRequest<String>(Attribute.CHANNEL_FREQUENCY_LABEL, mCurrentChannel));

                        mCurrentChannelFrequency = allocationEvent.getCallEvent().getFrequency();
                        broadcast(new AttributeChangeRequest<Long>(Attribute.CHANNEL_FREQUENCY, mCurrentChannelFrequency));

                        mFromTalkgroupMonitor.reset();
                        mFromTalkgroupMonitor.process(allocationEvent.getCallEvent().getFromID());

                        mToTalkgroupMonitor.reset();
                        mToTalkgroupMonitor.process(allocationEvent.getCallEvent().getToID());
                    }
                }
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
    public void stop()
    {

    }
}
