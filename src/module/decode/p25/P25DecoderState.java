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
package module.decode.p25;

import alias.Alias;
import alias.AliasList;
import alias.PatchGroupAlias;
import alias.id.AliasIDType;
import alias.id.talkgroup.TalkgroupID;
import channel.metadata.AliasedStringAttributeMonitor;
import channel.metadata.Attribute;
import channel.metadata.AttributeChangeRequest;
import channel.state.ChangeChannelTimeoutEvent;
import channel.state.DecoderState;
import channel.state.DecoderStateEvent;
import channel.state.DecoderStateEvent.Event;
import channel.state.State;
import channel.traffic.TrafficChannelAllocationEvent;
import controller.channel.Channel.ChannelType;
import message.Message;
import module.decode.DecoderType;
import module.decode.event.CallEvent.CallEventType;
import module.decode.p25.P25Decoder.Modulation;
import module.decode.p25.message.IAdjacentSite;
import module.decode.p25.message.IBandIdentifier;
import module.decode.p25.message.P25Message;
import module.decode.p25.message.hdu.HDUMessage;
import module.decode.p25.message.ldu.LDU1Message;
import module.decode.p25.message.ldu.LDUMessage;
import module.decode.p25.message.ldu.lc.CallTermination;
import module.decode.p25.message.ldu.lc.TelephoneInterconnectVoiceChannelUser;
import module.decode.p25.message.ldu.lc.UnitToUnitVoiceChannelUser;
import module.decode.p25.message.pdu.PDUMessage;
import module.decode.p25.message.pdu.confirmed.PDUConfirmedMessage;
import module.decode.p25.message.pdu.confirmed.PDUTypeUnknown;
import module.decode.p25.message.pdu.confirmed.PacketData;
import module.decode.p25.message.pdu.confirmed.SNDCPActivateTDSContextAccept;
import module.decode.p25.message.pdu.confirmed.SNDCPActivateTDSContextReject;
import module.decode.p25.message.pdu.confirmed.SNDCPActivateTDSContextRequest;
import module.decode.p25.message.pdu.confirmed.SNDCPDeactivateTDSContext;
import module.decode.p25.message.pdu.confirmed.SNDCPUserData;
import module.decode.p25.message.pdu.osp.control.AdjacentStatusBroadcastExtended;
import module.decode.p25.message.pdu.osp.control.CallAlertExtended;
import module.decode.p25.message.pdu.osp.control.GroupAffiliationQueryExtended;
import module.decode.p25.message.pdu.osp.control.GroupAffiliationResponseExtended;
import module.decode.p25.message.pdu.osp.control.MessageUpdateExtended;
import module.decode.p25.message.pdu.osp.control.NetworkStatusBroadcastExtended;
import module.decode.p25.message.pdu.osp.control.ProtectionParameterBroadcast;
import module.decode.p25.message.pdu.osp.control.RFSSStatusBroadcastExtended;
import module.decode.p25.message.pdu.osp.control.RoamingAddressUpdateExtended;
import module.decode.p25.message.pdu.osp.control.StatusQueryExtended;
import module.decode.p25.message.pdu.osp.control.StatusUpdateExtended;
import module.decode.p25.message.pdu.osp.control.UnitRegistrationResponseExtended;
import module.decode.p25.message.pdu.osp.data.GroupDataChannelGrantExtended;
import module.decode.p25.message.pdu.osp.data.IndividualDataChannelGrantExtended;
import module.decode.p25.message.pdu.osp.voice.GroupVoiceChannelGrantExplicit;
import module.decode.p25.message.pdu.osp.voice.TelephoneInterconnectChannelGrantExplicit;
import module.decode.p25.message.pdu.osp.voice.UnitToUnitAnswerRequestExplicit;
import module.decode.p25.message.pdu.osp.voice.UnitToUnitVoiceChannelGrantExtended;
import module.decode.p25.message.pdu.osp.voice.UnitToUnitVoiceChannelGrantUpdateExtended;
import module.decode.p25.message.tdu.TDUMessage;
import module.decode.p25.message.tdu.lc.AdjacentSiteStatusBroadcast;
import module.decode.p25.message.tdu.lc.AdjacentSiteStatusBroadcastExplicit;
import module.decode.p25.message.tdu.lc.GroupVoiceChannelUpdate;
import module.decode.p25.message.tdu.lc.GroupVoiceChannelUpdateExplicit;
import module.decode.p25.message.tdu.lc.NetworkStatusBroadcast;
import module.decode.p25.message.tdu.lc.NetworkStatusBroadcastExplicit;
import module.decode.p25.message.tdu.lc.SecondaryControlChannelBroadcast;
import module.decode.p25.message.tdu.lc.SecondaryControlChannelBroadcastExplicit;
import module.decode.p25.message.tdu.lc.TDULinkControlMessage;
import module.decode.p25.message.tsbk.TSBKMessage;
import module.decode.p25.message.tsbk.motorola.MotorolaTSBKMessage;
import module.decode.p25.message.tsbk.motorola.PatchGroupAdd;
import module.decode.p25.message.tsbk.motorola.PatchGroupDelete;
import module.decode.p25.message.tsbk.motorola.PatchGroupVoiceChannelGrant;
import module.decode.p25.message.tsbk.motorola.PatchGroupVoiceChannelGrantUpdate;
import module.decode.p25.message.tsbk.osp.control.AcknowledgeResponse;
import module.decode.p25.message.tsbk.osp.control.AdjacentStatusBroadcast;
import module.decode.p25.message.tsbk.osp.control.AuthenticationCommand;
import module.decode.p25.message.tsbk.osp.control.CallAlert;
import module.decode.p25.message.tsbk.osp.control.DenyResponse;
import module.decode.p25.message.tsbk.osp.control.ExtendedFunctionCommand;
import module.decode.p25.message.tsbk.osp.control.GroupAffiliationQuery;
import module.decode.p25.message.tsbk.osp.control.GroupAffiliationResponse;
import module.decode.p25.message.tsbk.osp.control.IdentifierUpdate;
import module.decode.p25.message.tsbk.osp.control.LocationRegistrationResponse;
import module.decode.p25.message.tsbk.osp.control.MessageUpdate;
import module.decode.p25.message.tsbk.osp.control.ProtectionParameterUpdate;
import module.decode.p25.message.tsbk.osp.control.QueuedResponse;
import module.decode.p25.message.tsbk.osp.control.RFSSStatusBroadcast;
import module.decode.p25.message.tsbk.osp.control.RadioUnitMonitorCommand;
import module.decode.p25.message.tsbk.osp.control.RoamingAddressCommand;
import module.decode.p25.message.tsbk.osp.control.StatusQuery;
import module.decode.p25.message.tsbk.osp.control.StatusUpdate;
import module.decode.p25.message.tsbk.osp.control.SystemService;
import module.decode.p25.message.tsbk.osp.control.UnitDeregistrationAcknowledge;
import module.decode.p25.message.tsbk.osp.control.UnitRegistrationCommand;
import module.decode.p25.message.tsbk.osp.control.UnitRegistrationResponse;
import module.decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncement;
import module.decode.p25.message.tsbk.osp.data.GroupDataChannelAnnouncementExplicit;
import module.decode.p25.message.tsbk.osp.data.GroupDataChannelGrant;
import module.decode.p25.message.tsbk.osp.data.IndividualDataChannelGrant;
import module.decode.p25.message.tsbk.osp.data.SNDCPDataChannelAnnouncementExplicit;
import module.decode.p25.message.tsbk.osp.data.SNDCPDataChannelGrant;
import module.decode.p25.message.tsbk.osp.data.SNDCPDataPageRequest;
import module.decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrant;
import module.decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdate;
import module.decode.p25.message.tsbk.osp.voice.GroupVoiceChannelGrantUpdateExplicit;
import module.decode.p25.message.tsbk.osp.voice.TelephoneInterconnectAnswerRequest;
import module.decode.p25.message.tsbk.osp.voice.TelephoneInterconnectVoiceChannelGrant;
import module.decode.p25.message.tsbk.osp.voice.TelephoneInterconnectVoiceChannelGrantUpdate;
import module.decode.p25.message.tsbk.osp.voice.UnitToUnitAnswerRequest;
import module.decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrant;
import module.decode.p25.message.tsbk.osp.voice.UnitToUnitVoiceChannelGrantUpdate;
import module.decode.p25.reference.IPProtocol;
import module.decode.p25.reference.LinkControlOpcode;
import module.decode.p25.reference.Response;
import module.decode.p25.reference.Vendor;
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
import java.util.concurrent.ScheduledExecutorService;

public class P25DecoderState extends DecoderState
{
    private final static Logger mLog = LoggerFactory.getLogger(P25DecoderState.class);
    private static final DecimalFormat mFrequencyFormatter =
        new DecimalFormat("0.000000");

    private module.decode.p25.message.tsbk.osp.control.NetworkStatusBroadcast mNetworkStatus;
    private NetworkStatusBroadcastExtended mNetworkStatusExtended;
    private ProtectionParameterBroadcast mProtectionParameterBroadcast;
    private RFSSStatusBroadcast mRFSSStatusMessage;
    private RFSSStatusBroadcastExtended mRFSSStatusMessageExtended;
    private SNDCPDataChannelAnnouncementExplicit mSNDCPDataChannel;

    private Set<module.decode.p25.message.tsbk.osp.control.SecondaryControlChannelBroadcast> mSecondaryControlChannels =
        new TreeSet<>();

    private Map<Integer,IdentifierUpdate> mBands = new HashMap<>();
    private Map<String,Long> mRegistrations = new HashMap<>();
    private Map<String,IAdjacentSite> mNeighborMap = new HashMap<>();
    private Map<String,List<String>> mPatchGroupMap = new HashMap<>();

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
    private Modulation mModulation;
    private boolean mIgnoreDataCalls;
    private boolean mControlChannelShutdownLogged;

    private P25CallEvent mCurrentCallEvent;
    private List<String> mCallDetectTalkgroups = new ArrayList<>();
    private Map<String,P25CallEvent> mChannelCallMap = new HashMap<>();

    public P25DecoderState(AliasList aliasList,
                           ChannelType channelType,
                           Modulation modulation,
                           boolean ignoreDataCalls)
    {
        super(aliasList);
        mChannelType = channelType;
        mModulation = modulation;
        mIgnoreDataCalls = ignoreDataCalls;

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

    public Modulation getModulation()
    {
        return mModulation;
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE1;
    }

    @Override
    public void stop()
    {
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
        }

        mCurrentCallEvent = null;
    }

    /**
     * Performs any initialization operations to prepare for use
     */
    @Override
    public void init()
    {
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
    public void receive(Message message)
    {
        if(message instanceof P25Message)
        {
            updateNAC(((P25Message)message).getNAC());

			/* Voice Vocoder messages */
            if(message instanceof LDUMessage)
            {
                processLDU((LDUMessage)message);
            }

			/* Trunking Signalling Messages */
            else if(message instanceof TSBKMessage)
            {
                processTSBK((TSBKMessage)message);
            }

			/* Terminator Data Unit with Link Control Message */
            else if(message instanceof TDULinkControlMessage)
            {
                processTDULC((TDULinkControlMessage)message);
            }

			/* Packet Data Unit Messages */
            else if(message instanceof PDUMessage)
            {
                processPDU((PDUMessage)message);
            }

			/* Header Data Unit Message - preceeds voice LDUs */
            else if(message instanceof HDUMessage)
            {
                processHDU((HDUMessage)message);
            }

			/* Terminator Data Unit, or default message if CRC failed */
            else if(message instanceof TDUMessage)
            {
                processTDU((TDUMessage)message);
            }
        }
    }

    /**
     * Terminator Data Unit
     */
    private void processTDU(TDUMessage tdu)
    {
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
                mCurrentCallEvent = new P25CallEvent.Builder(CallEventType.CALL)
                    .aliasList(getAliasList())
                    .channel(mCurrentChannel)
                    .details((hdu.isEncrypted() ? "ENCRYPTED " : ""))
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
        if(tdulc.getOpcode() == LinkControlOpcode.CALL_TERMINATION_OR_CANCELLATION)
        {
            broadcast(new DecoderStateEvent(this, Event.END, State.FADE));

            if(mCurrentCallEvent != null)
            {
                mCurrentCallEvent.end();
                mCurrentCallEvent = null;
            }
        }
        else
        {
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));
        }

        switch(tdulc.getOpcode())
        {
            case ADJACENT_SITE_STATUS_BROADCAST:
                if(tdulc instanceof AdjacentSiteStatusBroadcast)
                {
                    IAdjacentSite ias = (IAdjacentSite)tdulc;

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
                    IAdjacentSite ias = (IAdjacentSite)tdulc;

                    mNeighborMap.put(ias.getUniqueID(), ias);

                    updateSystem(ias.getSystemID());
                }
                break;
            case CALL_ALERT:
                if(tdulc instanceof module.decode.p25.message.tdu.lc.CallAlert)
                {
                    module.decode.p25.message.tdu.lc.CallAlert ca =
                        (module.decode.p25.message.tdu.lc.CallAlert)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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
                //TODO: does the activity summary need this message?

				/* This message is handled by the P25MessageProcessor and
                 * inserted into any channels needing frequency band info */
                break;
            case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
                //TODO: does the activity summary need this message?

				/* This message is handled by the P25MessageProcessor and
                 * inserted into any channels needing frequency band info */
                break;
            case EXTENDED_FUNCTION_COMMAND:
                if(tdulc instanceof module.decode.p25.message.tdu.lc.ExtendedFunctionCommand)
                {
                    module.decode.p25.message.tdu.lc.ExtendedFunctionCommand efc =
                        (module.decode.p25.message.tdu.lc.ExtendedFunctionCommand)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                if(tdulc instanceof module.decode.p25.message.tdu.lc.GroupAffiliationQuery)
                {
                    module.decode.p25.message.tdu.lc.GroupAffiliationQuery gaq =
                        (module.decode.p25.message.tdu.lc.GroupAffiliationQuery)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
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
                    GroupVoiceChannelUpdate gvcu = (GroupVoiceChannelUpdate)tdulc;

                    String groupA = gvcu.getGroupAddressA();

                    if(!mCallDetectTalkgroups.contains(groupA))
                    {
                        broadcast(new P25CallEvent.Builder(CallEventType.CALL_DETECT)
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
                        broadcast(new P25CallEvent.Builder(CallEventType.CALL_DETECT)
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
                        (GroupVoiceChannelUpdateExplicit)tdulc;

                    String group = gvcue.getGroupAddress();

                    if(!mCallDetectTalkgroups.contains(group))
                    {
                        broadcast(new P25CallEvent.Builder(CallEventType.CALL_DETECT)
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
				/* Used on a traffic channel to reflect current call entities */
                if(tdulc instanceof module.decode.p25.message.tdu.lc.GroupVoiceChannelUser)
                {
                    module.decode.p25.message.tdu.lc.GroupVoiceChannelUser gvcuser =
                        (module.decode.p25.message.tdu.lc.GroupVoiceChannelUser)tdulc;

                    broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CALL));

                    String from = gvcuser.getSourceAddress();
                    String to = gvcuser.getGroupAddress();

                    mFromTalkgroupMonitor.process(from);
                    mToTalkgroupMonitor.process(to);

                    if(mCurrentCallEvent == null)
                    {
                        mCurrentCallEvent = new P25CallEvent.Builder(CallEventType.GROUP_CALL)
                            .aliasList(getAliasList())
                            .channel(mCurrentChannel)
                            .details("TDULC GROUP VOICE CHANNEL USER" +
                                (gvcuser.isEncrypted() ? "ENCRYPTED " : "") +
                                (gvcuser.isEmergency() ? "EMERGENCY " : ""))
                            .frequency(mCurrentChannelFrequency)
                            .from(from)
                            .to(to)
                            .build();

                        broadcast(mCurrentCallEvent);
                    }
                }
                break;
            case MESSAGE_UPDATE:
                if(tdulc instanceof module.decode.p25.message.tdu.lc.MessageUpdate)
                {
                    module.decode.p25.message.tdu.lc.MessageUpdate mu =
                        (module.decode.p25.message.tdu.lc.MessageUpdate)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.SDM)
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
                if(tdulc instanceof NetworkStatusBroadcast)
                {
                    updateSystem(((NetworkStatusBroadcast)tdulc).getSystem());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case NETWORK_STATUS_BROADCAST_EXPLICIT:
                if(tdulc instanceof NetworkStatusBroadcastExplicit)
                {
                    updateSystem(((NetworkStatusBroadcastExplicit)tdulc).getSystem());
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case PROTECTION_PARAMETER_BROADCAST:
                if(tdulc instanceof module.decode.p25.message.tdu.lc.ProtectionParameterBroadcast)
                {
                    module.decode.p25.message.tdu.lc.ProtectionParameterBroadcast ppb =
                        (module.decode.p25.message.tdu.lc.ProtectionParameterBroadcast)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                if(tdulc instanceof module.decode.p25.message.tdu.lc.RFSSStatusBroadcast)
                {
                    module.decode.p25.message.tdu.lc.RFSSStatusBroadcast rfsssb =
                        (module.decode.p25.message.tdu.lc.RFSSStatusBroadcast)tdulc;

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
                if(tdulc instanceof module.decode.p25.message.tdu.lc.RFSSStatusBroadcastExplicit)
                {
                    module.decode.p25.message.tdu.lc.RFSSStatusBroadcastExplicit rfsssbe =
                        (module.decode.p25.message.tdu.lc.RFSSStatusBroadcastExplicit)tdulc;

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
                if(tdulc instanceof SecondaryControlChannelBroadcast)
                {
                    SecondaryControlChannelBroadcast sccb =
                        (SecondaryControlChannelBroadcast)tdulc;

                    String site = sccb.getRFSubsystemID() + "-" +
                        sccb.getSiteID();

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
                    SecondaryControlChannelBroadcastExplicit sccb =
                        (SecondaryControlChannelBroadcastExplicit)tdulc;

                    String site = sccb.getRFSubsystemID() + "-" +
                        sccb.getSiteID();

                    mSiteAttributeMonitor.process(site);
                }
                else
                {
                    logAlternateVendorMessage(tdulc);
                }
                break;
            case STATUS_QUERY:
                if(tdulc instanceof module.decode.p25.message.tdu.lc.StatusQuery)
                {
                    module.decode.p25.message.tdu.lc.StatusQuery sq =
                        (module.decode.p25.message.tdu.lc.StatusQuery)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
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
                if(tdulc instanceof module.decode.p25.message.tdu.lc.StatusUpdate)
                {
                    module.decode.p25.message.tdu.lc.StatusUpdate su =
                        (module.decode.p25.message.tdu.lc.StatusUpdate)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.STATUS)
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
                if(tdulc instanceof module.decode.p25.message.tdu.lc.TelephoneInterconnectAnswerRequest)
                {
                    module.decode.p25.message.tdu.lc.TelephoneInterconnectAnswerRequest tiar =
                        (module.decode.p25.message.tdu.lc.TelephoneInterconnectAnswerRequest)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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
                    tdulc instanceof module.decode.p25.message.tdu.lc.TelephoneInterconnectVoiceChannelUser)
                {
                    module.decode.p25.message.tdu.lc.TelephoneInterconnectVoiceChannelUser tivcu =
                        (module.decode.p25.message.tdu.lc.TelephoneInterconnectVoiceChannelUser)tdulc;

                    String to = tivcu.getAddress();

                    mToTalkgroupMonitor.process(to);

                    if(mCurrentCallEvent == null)
                    {
                        mCurrentCallEvent = new P25CallEvent.Builder(CallEventType.TELEPHONE_INTERCONNECT)
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
                if(tdulc instanceof module.decode.p25.message.tdu.lc.UnitAuthenticationCommand)
                {
                    module.decode.p25.message.tdu.lc.UnitAuthenticationCommand uac =
                        (module.decode.p25.message.tdu.lc.UnitAuthenticationCommand)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                if(tdulc instanceof module.decode.p25.message.tdu.lc.UnitRegistrationCommand)
                {
                    module.decode.p25.message.tdu.lc.UnitRegistrationCommand urc =
                        (module.decode.p25.message.tdu.lc.UnitRegistrationCommand)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                if(tdulc instanceof module.decode.p25.message.tdu.lc.UnitToUnitAnswerRequest)
                {
                    module.decode.p25.message.tdu.lc.UnitToUnitAnswerRequest uuar =
                        (module.decode.p25.message.tdu.lc.UnitToUnitAnswerRequest)tdulc;

                    broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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
                if(tdulc instanceof module.decode.p25.message.tdu.lc.UnitToUnitVoiceChannelUser)
                {
                    module.decode.p25.message.tdu.lc.UnitToUnitVoiceChannelUser uuvcu =
                        (module.decode.p25.message.tdu.lc.UnitToUnitVoiceChannelUser)tdulc;

                    String from = uuvcu.getSourceAddress();
                    mFromTalkgroupMonitor.process(from);

                    String to = uuvcu.getTargetAddress();
                    mToTalkgroupMonitor.process(to);

                    if(mCurrentCallEvent != null)
                    {
                        mCurrentCallEvent = new P25CallEvent.Builder(CallEventType.UNIT_TO_UNIT_CALL)
                            .aliasList(getAliasList())
                            .channel(mCurrentChannel)
                            .details((uuvcu.isEncrypted() ? "ENCRYPTED " : "") +
                                (uuvcu.isEmergency() ? "EMERGENCY " : ""))
                            .frequency(mCurrentChannelFrequency)
                            .from(from)
                            .to(to)
                            .build();
                    }
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
            mCurrentCallEvent = new P25CallEvent.Builder(CallEventType.CALL)
                .aliasList(getAliasList())
                .channel(mCurrentChannel)
                .frequency(mCurrentChannelFrequency)
                .build();

            broadcast(mCurrentCallEvent);
        }

        if(ldu instanceof LDU1Message)
        {
            switch(((LDU1Message)ldu).getOpcode())
            {
                case ADJACENT_SITE_STATUS_BROADCAST:
                    if(ldu instanceof module.decode.p25.message.ldu.lc.AdjacentSiteStatusBroadcast)
                    {
                        IAdjacentSite ias = (IAdjacentSite)ldu;

                        mNeighborMap.put(ias.getUniqueID(), ias);

                        updateSystem(ias.getSystemID());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }

                    break;
                case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
                    if(ldu instanceof module.decode.p25.message.ldu.lc.AdjacentSiteStatusBroadcastExplicit)
                    {
                        IAdjacentSite ias = (IAdjacentSite)ldu;

                        mNeighborMap.put(ias.getUniqueID(), ias);

                        updateSystem(ias.getSystemID());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case CALL_ALERT:
                    if(ldu instanceof module.decode.p25.message.ldu.lc.CallAlert)
                    {
                        module.decode.p25.message.ldu.lc.CallAlert ca =
                            (module.decode.p25.message.ldu.lc.CallAlert)ldu;
                        broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.ExtendedFunctionCommand)
                    {
                        module.decode.p25.message.ldu.lc.ExtendedFunctionCommand efc =
                            (module.decode.p25.message.ldu.lc.ExtendedFunctionCommand)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                        ldu instanceof module.decode.p25.message.ldu.lc.GroupAffiliationQuery)
                    {
                        module.decode.p25.message.ldu.lc.GroupAffiliationQuery gaq =
                            (module.decode.p25.message.ldu.lc.GroupAffiliationQuery)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdate)
                    {
                        module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdate gvcu =
                            (module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdate)ldu;

                        String userA = gvcu.getGroupAddressA();
                        String userB = gvcu.getGroupAddressB();

                        if(mChannelType == ChannelType.STANDARD)
                        {
                            if(!mCallDetectTalkgroups.contains(userA))
                            {
                                broadcast(new P25CallEvent.Builder(CallEventType.CALL_DETECT)
                                    .aliasList(getAliasList())
                                    .channel(gvcu.getChannelA())
                                    .details((gvcu.isEncrypted() ? "ENCRYPTED" : ""))
                                    .frequency(gvcu.getDownlinkFrequencyA())
                                    .to(userA)
                                    .build());

                                mCallDetectTalkgroups.add(userA);
                            }

                            if(userB != null &&
                                !userB.contentEquals("0000") &&
                                !mCallDetectTalkgroups.contains(userB))
                            {
                                broadcast(new P25CallEvent.Builder(CallEventType.CALL_DETECT)
                                    .aliasList(getAliasList())
                                    .channel(gvcu.getChannelB())
                                    .details((gvcu.isEncrypted() ? "ENCRYPTED" : ""))
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdateExplicit)
                    {
                        module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdateExplicit gvcue =
                            (module.decode.p25.message.ldu.lc.GroupVoiceChannelUpdateExplicit)ldu;

                        String group = gvcue.getGroupAddress();

                        if(mChannelType == ChannelType.STANDARD)
                        {
                            if(!mCallDetectTalkgroups.contains(group))
                            {
                                broadcast(new P25CallEvent.Builder(CallEventType.CALL_DETECT)
                                    .aliasList(getAliasList())
                                    .channel(gvcue.getTransmitChannel())
                                    .details((gvcue.isEncrypted() ? "ENCRYPTED" : ""))
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.GroupVoiceChannelUser)
                    {
                        module.decode.p25.message.ldu.lc.GroupVoiceChannelUser gvcuser =
                            (module.decode.p25.message.ldu.lc.GroupVoiceChannelUser)ldu;

                        mFromTalkgroupMonitor.process(gvcuser.getSourceAddress());

                        mToTalkgroupMonitor.process(gvcuser.getGroupAddress());

                        if(mChannelType == ChannelType.STANDARD)
                        {
                            if(mCurrentCallEvent.getCallEventType() != CallEventType.GROUP_CALL)
                            {
                                mCurrentCallEvent.setCallEventType(CallEventType.GROUP_CALL);
                                broadcast(mCurrentCallEvent);
                            }

                            if(mCurrentCallEvent.getDetails() == null)
                            {
                                mCurrentCallEvent.setDetails(
                                    (gvcuser.isEncrypted() ? "ENCRYPTED " : "") +
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.MessageUpdate)
                    {
                        module.decode.p25.message.ldu.lc.MessageUpdate mu =
                            (module.decode.p25.message.ldu.lc.MessageUpdate)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.SDM)
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.NetworkStatusBroadcast)
                    {
                        updateSystem(((module.decode.p25.message.ldu.lc.NetworkStatusBroadcast)ldu).getSystem());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case NETWORK_STATUS_BROADCAST_EXPLICIT:
                    if(ldu instanceof module.decode.p25.message.ldu.lc.NetworkStatusBroadcastExplicit)
                    {
                        updateSystem(((module.decode.p25.message.ldu.lc.NetworkStatusBroadcastExplicit)ldu).getSystem());
                    }
                    else
                    {
                        logAlternateVendorMessage(ldu);
                    }
                    break;
                case PROTECTION_PARAMETER_BROADCAST:
                    if(ldu instanceof module.decode.p25.message.ldu.lc.ProtectionParameterBroadcast)
                    {
                        module.decode.p25.message.ldu.lc.ProtectionParameterBroadcast ppb =
                            (module.decode.p25.message.ldu.lc.ProtectionParameterBroadcast)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.RFSSStatusBroadcast)
                    {
                        module.decode.p25.message.ldu.lc.RFSSStatusBroadcast rfsssb =
                            (module.decode.p25.message.ldu.lc.RFSSStatusBroadcast)ldu;

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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.RFSSStatusBroadcastExplicit)
                    {
                        module.decode.p25.message.ldu.lc.RFSSStatusBroadcastExplicit rfsssbe =
                            (module.decode.p25.message.ldu.lc.RFSSStatusBroadcastExplicit)ldu;

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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcast)
                    {
                        module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcast sccb =
                            (module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcast)ldu;

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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcastExplicit)
                    {
                        module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcastExplicit sccb =
                            (module.decode.p25.message.ldu.lc.SecondaryControlChannelBroadcastExplicit)ldu;

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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.StatusQuery)
                    {
                        module.decode.p25.message.ldu.lc.StatusQuery sq =
                            (module.decode.p25.message.ldu.lc.StatusQuery)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.StatusUpdate)
                    {
                        module.decode.p25.message.ldu.lc.StatusUpdate su =
                            (module.decode.p25.message.ldu.lc.StatusUpdate)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.STATUS)
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest)
                    {
                        module.decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest tiar =
                            (module.decode.p25.message.ldu.lc.TelephoneInterconnectAnswerRequest)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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
                        ldu instanceof TelephoneInterconnectVoiceChannelUser)
                    {
                        TelephoneInterconnectVoiceChannelUser tivcu =
                            (TelephoneInterconnectVoiceChannelUser)ldu;

                        mToTalkgroupMonitor.process(tivcu.getAddress());

                        if(mCurrentCallEvent.getCallEventType() != CallEventType.TELEPHONE_INTERCONNECT)
                        {
                            mCurrentCallEvent.setCallEventType(CallEventType.TELEPHONE_INTERCONNECT);
                            broadcast(mCurrentCallEvent);
                        }

                        if(mCurrentCallEvent.getDetails() == null)
                        {
                            mCurrentCallEvent.setDetails(
                                (tivcu.isEncrypted() ? "ENCRYPTED " : "") +
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.UnitAuthenticationCommand)
                    {
                        module.decode.p25.message.ldu.lc.UnitAuthenticationCommand uac =
                            (module.decode.p25.message.ldu.lc.UnitAuthenticationCommand)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.UnitRegistrationCommand)
                    {
                        module.decode.p25.message.ldu.lc.UnitRegistrationCommand urc =
                            (module.decode.p25.message.ldu.lc.UnitRegistrationCommand)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                    if(ldu instanceof module.decode.p25.message.ldu.lc.UnitToUnitAnswerRequest)
                    {
                        module.decode.p25.message.ldu.lc.UnitToUnitAnswerRequest uuar =
                            (module.decode.p25.message.ldu.lc.UnitToUnitAnswerRequest)ldu;

                        broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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
                        ldu instanceof UnitToUnitVoiceChannelUser)
                    {
                        UnitToUnitVoiceChannelUser uuvcu =
                            (UnitToUnitVoiceChannelUser)ldu;

                        mFromTalkgroupMonitor.process(uuvcu.getSourceAddress());

                        mToTalkgroupMonitor.process(uuvcu.getTargetAddress());

                        if(mCurrentCallEvent.getCallEventType() != CallEventType.UNIT_TO_UNIT_CALL)
                        {
                            mCurrentCallEvent.setCallEventType(CallEventType.UNIT_TO_UNIT_CALL);
                            broadcast(mCurrentCallEvent);
                        }

                        if(mCurrentCallEvent.getDetails() == null)
                        {
                            mCurrentCallEvent.setDetails(
                                (uuvcu.isEncrypted() ? "ENCRYPTED " : "") +
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
                        IAdjacentSite ias = (IAdjacentSite)tsbk;

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
                    IdentifierUpdate iu = (IdentifierUpdate)tsbk;

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
                    mNetworkStatus = (module.decode.p25.message.tsbk.osp
                        .control.NetworkStatusBroadcast)tsbk;
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
                    processTSBKRFSSStatus((RFSSStatusBroadcast)tsbk);
                    break;
                case ROAMING_ADDRESS_COMMAND:
                    processTSBKCommand(tsbk);
                    break;
                case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                    module.decode.p25.message.tsbk.osp.control.SecondaryControlChannelBroadcast sccb =
                        (module.decode.p25.message.tsbk.osp.control.SecondaryControlChannelBroadcast)tsbk;

                    if(sccb.getDownlinkFrequency1() > 0)
                    {
                        mSecondaryControlChannels.add(sccb);
                    }
                    break;
                case SNDCP_DATA_CHANNEL_ANNOUNCEMENT_EXPLICIT:
                    mSNDCPDataChannel = (SNDCPDataChannelAnnouncementExplicit)tsbk;
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
            processMotorolaTSBK((MotorolaTSBKMessage)tsbk);
        }
    }

    /**
     * Process a Packet Data Unit message
     */
    private void processPDU(PDUMessage pdu)
    {
        if(pdu instanceof PacketData || pdu instanceof PDUTypeUnknown)
        {
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));
        }
        else if(pdu instanceof PDUConfirmedMessage)
        {
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.DATA));

            PDUConfirmedMessage pduc = (PDUConfirmedMessage)pdu;

            switch(pduc.getPDUType())
            {
                case SNDCP_ACTIVATE_TDS_CONTEXT_ACCEPT:
                    SNDCPActivateTDSContextAccept satca =
                        (SNDCPActivateTDSContextAccept)pduc;

                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
                        .aliasList(getAliasList())
                        .channel(mCurrentChannel)
                        .details("ACTIVATE SNDCP USE IP:" + satca.getIPAddress())
                        .frequency(mCurrentChannelFrequency)
                        .to(satca.getLogicalLinkID())
                        .build());
                    break;
                case SNDCP_ACTIVATE_TDS_CONTEXT_REJECT:
                    SNDCPActivateTDSContextReject satcr =
                        (SNDCPActivateTDSContextReject)pduc;

                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                        (SNDCPActivateTDSContextRequest)pduc;

                    broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
                        .aliasList(getAliasList())
                        .channel(mCurrentChannel)
                        .details("REQUEST SNDCP USE IP:" + satcreq.getIPAddress())
                        .frequency(mCurrentChannelFrequency)
                        .from(satcreq.getLogicalLinkID())
                        .build());
                    break;
                case SNDCP_DEACTIVATE_TDS_CONTEXT_ACCEPT:
                    SNDCPDeactivateTDSContext sdtca =
                        (SNDCPDeactivateTDSContext)pduc;

                    broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
                        .aliasList(getAliasList())
                        .channel(mCurrentChannel)
                        .details("ACCEPT DEACTIVATE SNDCP CONTEXT")
                        .frequency(mCurrentChannelFrequency)
                        .from(sdtca.getLogicalLinkID())
                        .build());
                    break;
                case SNDCP_DEACTIVATE_TDS_CONTEXT_REQUEST:
                    SNDCPDeactivateTDSContext sdtcreq =
                        (SNDCPDeactivateTDSContext)pduc;

                    broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
                        .aliasList(getAliasList())
                        .channel(mCurrentChannel)
                        .details("REQUEST DEACTIVATE SNDCP CONTEXT")
                        .frequency(mCurrentChannelFrequency)
                        .from(sdtcreq.getLogicalLinkID())
                        .build());
                    break;
                case SNDCP_RF_CONFIRMED_DATA:
                    SNDCPUserData sud = (SNDCPUserData)pduc;

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

                    broadcast(new DecoderStateEvent(this, Event.START, State.DATA));

                    broadcast(new P25CallEvent.Builder(CallEventType.DATA_CALL)
                        .aliasList(getAliasList())
                        .channel(mCurrentChannel)
                        .details("DATA: " + sud.getPayload() +
                            " RADIO IP:" + sbTo.toString())
                        .frequency(mCurrentChannelFrequency)
                        .from(sbFrom.toString())
                        .to(pduc.getLogicalLinkID())
                        .build());
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
			/* These are alternate trunking control blocks in PDU format */
            broadcast(new DecoderStateEvent(this, Event.CONTINUATION, State.CONTROL));

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
                        IAdjacentSite ias = (IAdjacentSite)pdu;

                        mNeighborMap.put(ias.getUniqueID(), ias);

                        updateSystem(ias.getSystemID());
                    }
                    break;
                case CALL_ALERT:
                    if(pdu instanceof CallAlertExtended)
                    {
                        CallAlertExtended ca = (CallAlertExtended)pdu;

                        broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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
                            (GroupAffiliationQueryExtended)pdu;

                        if(mLastQueryEventID == null || !gaqe.getTargetAddress()
                            .contentEquals(mLastQueryEventID))
                        {
                            broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
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
                            (GroupAffiliationResponseExtended)pdu;

                        if(mLastResponseEventID == null || !gar.getTargetAddress()
                            .contentEquals(mLastResponseEventID))
                        {
                            broadcast(new P25CallEvent.Builder(CallEventType.RESPONSE)
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
                        MessageUpdateExtended mu = (MessageUpdateExtended)pdu;

                        broadcast(new P25CallEvent.Builder(CallEventType.SDM)
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
                        mNetworkStatusExtended = (NetworkStatusBroadcastExtended)pdu;
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
                            (ProtectionParameterBroadcast)pdu;
                    }
                    else
                    {
                        logAlternateVendorMessage(pdu);
                    }
                    break;
                case RFSS_STATUS_BROADCAST:
                    if(pdu instanceof RFSSStatusBroadcastExtended)
                    {
                        mRFSSStatusMessageExtended = (RFSSStatusBroadcastExtended)pdu;

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
                            (RoamingAddressUpdateExtended)pdu;

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

                        broadcast(new P25CallEvent.Builder(CallEventType.RESPONSE)
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
                        StatusQueryExtended sq = (StatusQueryExtended)pdu;

                        if(mLastQueryEventID == null || !sq.getTargetAddress()
                            .contentEquals(mLastQueryEventID))
                        {
                            broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
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
                        StatusUpdateExtended su = (StatusUpdateExtended)pdu;

                        if(mLastResponseEventID == null || !mLastResponseEventID
                            .contentEquals(su.getTargetAddress()))
                        {
                            broadcast(new P25CallEvent.Builder(CallEventType.RESPONSE)
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
                            (UnitRegistrationResponseExtended)pdu;

                        if(urr.getResponse() == Response.ACCEPT)
                        {
                            mRegistrations.put(urr.getAssignedSourceAddress(),
                                System.currentTimeMillis());
                        }

                        if(mLastRegistrationEventID == null || !mLastRegistrationEventID
                            .contentEquals(urr.getAssignedSourceAddress()))
                        {
                            broadcast(new P25CallEvent.Builder(CallEventType.REGISTER)
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
                            (UnitToUnitAnswerRequestExplicit)pdu;

                        if(mLastPageEventID == null || !mLastPageEventID
                            .contentEquals(utuare.getTargetAddress()))
                        {
                            broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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
                        (GroupDataChannelGrantExtended)pdu;

                    channel = gdcge.getTransmitChannel();
                    from = gdcge.getSourceAddress();
                    to = gdcge.getGroupAddress();

                    if(hasCallEvent(channel, from, to))
                    {
                        updateCallEvent(channel, from, to);
                    }
                    else
                    {
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEventType.DATA_CALL)
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
                        (GroupVoiceChannelGrantExplicit)pdu;

                    channel = gvcge.getTransmitChannel();
                    from = gvcge.getSourceAddress();
                    to = gvcge.getGroupAddress();

                    if(hasCallEvent(channel, from, to))
                    {
                        updateCallEvent(channel, from, to);
                    }
                    else
                    {
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEventType.GROUP_CALL)
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
                        (IndividualDataChannelGrantExtended)pdu;

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
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEventType.DATA_CALL)
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
                    (TelephoneInterconnectChannelGrantExplicit)pdu;

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
                    P25CallEvent callEvent = new P25CallEvent.Builder(CallEventType.TELEPHONE_INTERCONNECT)
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
                        (UnitToUnitVoiceChannelGrantExtended)pdu;

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
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEventType.UNIT_TO_UNIT_CALL)
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
                        (UnitToUnitVoiceChannelGrantUpdateExtended)pdu;

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
                        P25CallEvent callEvent = new P25CallEvent.Builder(CallEventType.UNIT_TO_UNIT_CALL)
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
                AuthenticationCommand ac = (AuthenticationCommand)message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                    .contentEquals(ac.getFullTargetID()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
                        .aliasList(getAliasList())
                        .details("AUTHENTICATE")
                        .to(ac.getWACN() + "-" + ac.getSystemID() + "-" +
                            ac.getTargetID())
                        .build());

                    mLastCommandEventID = ac.getFullTargetID();
                }
                break;
            case EXTENDED_FUNCTION_COMMAND:
                ExtendedFunctionCommand efc = (ExtendedFunctionCommand)message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                    .contentEquals(efc.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.FUNCTION)
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
                RadioUnitMonitorCommand rumc = (RadioUnitMonitorCommand)message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                    .contentEquals(rumc.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
                        .aliasList(getAliasList())
                        .details("RADIO UNIT MONITOR")
                        .from(rumc.getSourceAddress())
                        .to(rumc.getTargetAddress())
                        .build());

                    mLastCommandEventID = rumc.getTargetAddress();
                }
                break;
            case ROAMING_ADDRESS_COMMAND:
                RoamingAddressCommand rac = (RoamingAddressCommand)message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                    .contentEquals(rac.getTargetID()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
                UnitRegistrationCommand urc = (UnitRegistrationCommand)message;

                if(mLastCommandEventID == null || !mLastCommandEventID
                    .contentEquals(urc.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.COMMAND)
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
        MessageUpdate mu = (MessageUpdate)message;

        broadcast(new P25CallEvent.Builder(CallEventType.SDM)
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
                GroupAffiliationQuery gaq = (GroupAffiliationQuery)message;

                if(mLastQueryEventID == null || !mLastQueryEventID
                    .contentEquals(gaq.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
                        .aliasList(getAliasList())
                        .details("GROUP AFFILIATION")
                        .from(gaq.getSourceAddress())
                        .to(gaq.getTargetAddress())
                        .build());
                }
                break;
            case STATUS_QUERY:
                StatusQuery sq = (StatusQuery)message;

                if(mLastQueryEventID == null || !mLastQueryEventID
                    .contentEquals(sq.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.QUERY)
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
                AcknowledgeResponse ar = (AcknowledgeResponse)message;

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

                    broadcast(new P25CallEvent.Builder(CallEventType.RESPONSE)
                        .aliasList(getAliasList())
                        .details("ACKNOWLEDGE")
                        .from(from)
                        .to(to)
                        .build());

                    mLastResponseEventID = ar.getTargetAddress();
                }
                break;
            case DENY_RESPONSE:
                DenyResponse dr = (DenyResponse)message;

                if(mLastResponseEventID == null || !dr.getTargetAddress()
                    .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.RESPONSE)
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
                GroupAffiliationResponse gar = (GroupAffiliationResponse)message;

                if(mLastResponseEventID == null || !gar.getTargetAddress()
                    .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.RESPONSE)
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
                    (LocationRegistrationResponse)message;

                if(lrr.getResponse() == Response.ACCEPT)
                {
                    mRegistrations.put(lrr.getTargetAddress(),
                        System.currentTimeMillis());
                }

                if(mLastRegistrationEventID == null ||
                    !mLastRegistrationEventID.contentEquals(lrr.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.REGISTER)
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
                ProtectionParameterUpdate ppu = (ProtectionParameterUpdate)message;

                if(mLastResponseEventID == null || !ppu.getTargetAddress()
                    .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.RESPONSE)
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
                QueuedResponse qr = (QueuedResponse)message;

                if(mLastResponseEventID == null || !qr.getTargetAddress()
                    .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.RESPONSE)
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
                StatusUpdate su = (StatusUpdate)message;

                if(mLastResponseEventID == null || !su.getTargetAddress()
                    .contentEquals(mLastResponseEventID))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.RESPONSE)
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
                UnitRegistrationResponse urr = (UnitRegistrationResponse)message;

                if(urr.getResponse() == Response.ACCEPT)
                {
                    mRegistrations.put(urr.getSourceAddress(),
                        System.currentTimeMillis());
                }

                if(mLastRegistrationEventID == null ||
                    !mLastRegistrationEventID.contentEquals(urr.getSourceAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.REGISTER)
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
                    (UnitDeregistrationAcknowledge)message;

                if(mLastRegistrationEventID == null ||
                    !mLastRegistrationEventID.contentEquals(udr.getSourceID()))
                {

                    broadcast(new P25CallEvent.Builder(CallEventType.DEREGISTER)
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
                GroupDataChannelAnnouncement gdca = (GroupDataChannelAnnouncement)message;

                broadcast(new P25CallEvent.Builder(CallEventType.ANNOUNCEMENT)
                    .aliasList(getAliasList())
                    .channel(gdca.getChannel1())
                    .details((gdca.isEncrypted() ? "ENCRYPTED" : "") +
                        (gdca.isEmergency() ? " EMERGENCY" : ""))
                    .frequency(gdca.getDownlinkFrequency1())
                    .to(gdca.getGroupAddress1())
                    .build());

                if(gdca.hasChannelNumber2())
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.ANNOUNCEMENT)
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
                    (GroupDataChannelAnnouncementExplicit)message;

                broadcast(new P25CallEvent.Builder(CallEventType.DATA_CALL)
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

        switch(((MotorolaTSBKMessage)tsbk).getMotorolaOpcode())
        {
            case PATCH_GROUP_CHANNEL_GRANT:
                PatchGroupVoiceChannelGrant pgvcg = (PatchGroupVoiceChannelGrant)tsbk;

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
                    details.append(pgvcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(pgvcg.getPriority());
                    details.append(pgvcg.isEncryptedChannel() ? " ENCRYPTED" : "");
                    details.append(" ").append(pgvcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEventType.PATCH_GROUP_CALL)
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

                broadcast(new TrafficChannelAllocationEvent(this,
                    mChannelCallMap.get(channel)));
                break;
            case PATCH_GROUP_CHANNEL_GRANT_UPDATE:
                PatchGroupVoiceChannelGrantUpdate gvcgu = (PatchGroupVoiceChannelGrantUpdate)tsbk;

                channel = gvcgu.getChannel1();
                to = gvcgu.getPatchGroupAddress1();

                if(hasCallEvent(channel, null, to))
                {
                    updateCallEvent(channel, null, to);
                }
                else
                {
                    P25CallEvent event = new P25CallEvent.Builder(CallEventType.PATCH_GROUP_CALL)
                        .aliasList(getAliasList())
                        .channel(channel)
                        .details((gvcgu.isEncrypted() ? "ENCRYPTED " : "") +
                            "PATCH UPDATE - GROUP 2:" + gvcgu.getPatchGroupAddress2() +
                            " DN:" + gvcgu.getDownlinkFrequency2())
                        .frequency(gvcgu.getDownlinkFrequency1())
                        .to(to)
                        .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                broadcast(new TrafficChannelAllocationEvent(this,
                    mChannelCallMap.get(channel)));
                break;
            case PATCH_GROUP_ADD:
                PatchGroupAdd pga = (PatchGroupAdd)tsbk;

                if(addPatchGroup(pga.getPatchGroupAddress(), pga.getPatchedTalkgroups()))
                {
                    StringBuilder sb = new StringBuilder();

                    sb.append("PATCH GROUP:").append(pga.getPatchGroupAddress());
                    sb.append(" ").append(pga.getPatchedTalkgroups());

                    broadcast(new P25CallEvent.Builder(CallEventType.PATCH_GROUP_ADD)
                        .aliasList(getAliasList())
                        .details(sb.toString())
                        .build());
                }
                break;
            case PATCH_GROUP_DELETE:
                PatchGroupDelete pgd = (PatchGroupDelete)tsbk;

                if(removePatchGroup(pgd.getPatchGroupAddress(), pgd.getPatchedTalkgroups()))
                {
                    StringBuilder sbpgd = new StringBuilder();

                    sbpgd.append("PATCH GROUP:").append(pgd.getPatchGroupAddress());
                    sbpgd.append(" ").append(pgd.getPatchedTalkgroups());

                    broadcast(new P25CallEvent.Builder(CallEventType.PATCH_GROUP_DELETE)
                        .aliasList(getAliasList())
                        .details(sbpgd.toString())
                        .build());
                }
                break;
            case CCH_PLANNED_SHUTDOWN:
                if(!mControlChannelShutdownLogged)
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.NOTIFICATION)
                        .details("PLANNED CONTROL CHANNEL SHUTDOWN")
                        .build());

                    mControlChannelShutdownLogged = true;
                }
                break;
        }
    }

    /**
     * Creates a new patch group or updates an existing patch group with the patched talkgroup entries.
     *
     * @param patchGroupID for the patch group
     * @param talkgroupsToAdd to include in the patch group
     * @return true if the patch group or the patch group map was updated
     */
    private boolean addPatchGroup(String patchGroupID, List<String> talkgroupsToAdd)
    {
        boolean updated = false;

        if(patchGroupID != null && !talkgroupsToAdd.isEmpty())
        {
            List<String> patchedTalkgroups = mPatchGroupMap.get(patchGroupID);

            if(patchedTalkgroups == null)
            {
                patchedTalkgroups = new ArrayList<>();
            }

            for(String talkgroupToAdd : talkgroupsToAdd)
            {
                //Exclude the patch group ID if it is included in the patched talkgroup list so that we don't have
                //an infinite loop situation
                if(talkgroupToAdd.equals(patchGroupID))
                {
                    continue;
                }

                if(!patchedTalkgroups.contains(talkgroupToAdd))
                {
                    patchedTalkgroups.add(talkgroupToAdd);
                    updated = true;
                }
            }

            if(updated)
            {
                mPatchGroupMap.put(patchGroupID, patchedTalkgroups);
                updatePatchAlias(patchGroupID, patchedTalkgroups);
            }
        }

        return updated;
    }

    /**
     * Removes the patched talkgroups from the patch group and removes the patch group from the patch group map if
     * there are no more patched talkgroups in the patch group.
     *
     * @param patchGroupID for the patch group
     * @param talkgroupsToRemove to remove from the patch group
     * @return true if the patch group or the patch group map was updated
     */
    private boolean removePatchGroup(String patchGroupID, List<String> talkgroupsToRemove)
    {
        boolean updated = false;

        if(patchGroupID != null && !talkgroupsToRemove.isEmpty())
        {
            List<String> patchedTalkgroups = mPatchGroupMap.get(patchGroupID);

            if(patchedTalkgroups != null)
            {
                for(String talkgroupToRemove : talkgroupsToRemove)
                {
                    if(patchedTalkgroups.contains(talkgroupToRemove))
                    {
                        patchedTalkgroups.remove(talkgroupToRemove);
                        updated = true;
                    }
                }

                if(patchedTalkgroups.isEmpty())
                {
                    mPatchGroupMap.remove(patchGroupID);
                    removePatchGroupAlias(patchGroupID);
                    updated = true;
                }
                else
                {
                    updatePatchAlias(patchGroupID, patchedTalkgroups);
                }
            }
        }

        return updated;
    }

    /**
     * Adds/updates a patch group alias to the alias list containing aliases for each of the patched talkgroups.  If the
     * patch group alias already exists, any patched talkgroups will be removed from the existing patch group alias and
     * replaced with the aliases corresponding to the patched talkgroup aliases.
     *
     * @param patchGroupID for the patch group
     * @param patchedTalkgroups containing the talkgroup IDs for each of the patched talkgroups
     */
    private void updatePatchAlias(String patchGroupID, List<String> patchedTalkgroups)
    {
        PatchGroupAlias patchGroupAlias = null;

        //Check for an existing alias for the patch talkgroup - do not include wildcard aliases
        Alias existingAlias = getAliasList().getTalkgroupAlias(patchGroupID, false);

        if(existingAlias instanceof PatchGroupAlias)
        {
            patchGroupAlias = (PatchGroupAlias)existingAlias;
        }
        else
        {
            patchGroupAlias = new PatchGroupAlias();

            patchGroupAlias.addAliasID(new TalkgroupID(patchGroupID));

            if(existingAlias != null)
            {
                getAliasList().removeAlias(existingAlias);

                patchGroupAlias.setPatchGroupAlias(existingAlias);
            }

            getAliasList().addAlias(patchGroupAlias);
        }

        if(patchGroupAlias != null)
        {
            patchGroupAlias.setPatchedTalkgroupIDs(patchedTalkgroups);

            patchGroupAlias.clearPatchedAliases();

            for(String patchedTalkgroup : patchedTalkgroups)
            {
                Alias patchedAlias = getAliasList().getTalkgroupAlias(patchedTalkgroup);

                if(patchedAlias != null)
                {
                    patchGroupAlias.addPatchedAlias(patchedAlias);
                }
            }
        }
    }

    /**
     * Removes the temporary patch group alias from the alias list
     */
    private void removePatchGroupAlias(String patchGroupID)
    {
        Alias alias = getAliasList().getTalkgroupAlias(patchGroupID);

        if(alias instanceof PatchGroupAlias)
        {
            PatchGroupAlias patchGroupAlias = (PatchGroupAlias)alias;

            getAliasList().removeAlias(patchGroupAlias);

            //Replace our temporary patch group alias with the original alias for the patch group ID
            if(patchGroupAlias.hasPatchGroupAlias())
            {
                getAliasList().addAlias(patchGroupAlias.getPatchGroupAlias());
            }
        }
    }

    /**
     * Process a traffic channel allocation message
     */
    private void processTSBKChannelGrant(TSBKMessage message)
    {
        String channel = null;
        String from = null;
        String to = null;

        switch(message.getOpcode())
        {
            case GROUP_DATA_CHANNEL_GRANT:
                GroupDataChannelGrant gdcg = (GroupDataChannelGrant)message;

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
                    details.append(gdcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(gdcg.getPriority()).append(" ");
                    details.append(gdcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(gdcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEventType.DATA_CALL)
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

                if(!mIgnoreDataCalls)
                {
                    broadcast(new TrafficChannelAllocationEvent(this,
                        mChannelCallMap.get(channel)));
                }
                break;
            case GROUP_VOICE_CHANNEL_GRANT:
                GroupVoiceChannelGrant gvcg = (GroupVoiceChannelGrant)message;

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
                    details.append(gvcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(gvcg.getPriority()).append(" ");
                    details.append(gvcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(gvcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEventType.GROUP_CALL)
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

                broadcast(new TrafficChannelAllocationEvent(this,
                    mChannelCallMap.get(channel)));
                break;
            case GROUP_VOICE_CHANNEL_GRANT_UPDATE:
                GroupVoiceChannelGrantUpdate gvcgu =
                    (GroupVoiceChannelGrantUpdate)message;

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
                    details.append(gvcgu.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(gvcgu.getPriority()).append(" ");
                    details.append(gvcgu.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(gvcgu.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEventType.GROUP_CALL)
                        .aliasList(getAliasList())
                        .channel(channel)
                        .channel(gvcgu.getChannel1())
                        .details(details.toString())
                        .frequency(gvcgu.getDownlinkFrequency1())
                        .from(from)
                        .to(to)
                        .build();

                    registerCallEvent(event);
                    broadcast(event);
                }

                broadcast(new TrafficChannelAllocationEvent(this,
                    mChannelCallMap.get(channel)));

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
                        details.append(gvcgu.isEmergency() ? "EMERGENCY " : "");
                        details.append("PRI:").append(gvcgu.getPriority()).append(" ");
                        details.append(gvcgu.isEncryptedChannel() ? "ENCRYPTED " : "");
                        details.append(gvcgu.getSessionMode().name());

                        P25CallEvent event2 = new P25CallEvent.Builder(CallEventType.GROUP_CALL)
                            .aliasList(getAliasList())
                            .channel(gvcgu.getChannel2())
                            .details(details.toString())
                            .frequency(gvcgu.getDownlinkFrequency2())
                            .to(gvcgu.getGroupAddress2())
                            .build();

                        registerCallEvent(event2);
                        broadcast(event2);
                    }

                    broadcast(new TrafficChannelAllocationEvent(this,
                        mChannelCallMap.get(channel)));
                }
                break;
            case GROUP_VOICE_CHANNEL_GRANT_UPDATE_EXPLICIT:
                GroupVoiceChannelGrantUpdateExplicit gvcgue =
                    (GroupVoiceChannelGrantUpdateExplicit)message;

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
                    details.append(gvcgue.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(gvcgue.getPriority()).append(" ");
                    details.append(gvcgue.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(gvcgue.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEventType.GROUP_CALL)
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

                broadcast(new TrafficChannelAllocationEvent(this,
                    mChannelCallMap.get(channel)));
                break;
            case INDIVIDUAL_DATA_CHANNEL_GRANT:
                IndividualDataChannelGrant idcg = (IndividualDataChannelGrant)message;

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
                    details.append(idcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(idcg.getPriority()).append(" ");
                    details.append(idcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(idcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEventType.DATA_CALL)
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

                if(!mIgnoreDataCalls)
                {
                    broadcast(new TrafficChannelAllocationEvent(this,
                        mChannelCallMap.get(channel)));
                }
                break;
            case SNDCP_DATA_CHANNEL_GRANT:
                SNDCPDataChannelGrant sdcg = (SNDCPDataChannelGrant)message;

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
                    details.append(sdcg.isEmergency() ? "EMERGENCY " : "");
                    details.append(sdcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(sdcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(CallEventType.DATA_CALL)
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

                if(!mIgnoreDataCalls)
                {
                    broadcast(new TrafficChannelAllocationEvent(this,
                        mChannelCallMap.get(channel)));
                }
                break;
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT:
                TelephoneInterconnectVoiceChannelGrant tivcg =
                    (TelephoneInterconnectVoiceChannelGrant)message;

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
                    details.append(tivcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(tivcg.getPriority()).append(" ");
                    details.append(tivcg.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(tivcg.getSessionMode().name()).append(" ");
                    details.append("CALL TIMER:").append(tivcg.getCallTimer());

                    P25CallEvent event = new P25CallEvent.Builder(
                        CallEventType.TELEPHONE_INTERCONNECT)
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

                broadcast(new TrafficChannelAllocationEvent(this,
                    mChannelCallMap.get(channel)));
                break;
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_GRANT_UPDATE:
                TelephoneInterconnectVoiceChannelGrantUpdate tivcgu =
                    (TelephoneInterconnectVoiceChannelGrantUpdate)message;

                channel = tivcgu.getChannelIdentifier() + "-" +
                    tivcgu.getChannelNumber();
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
                    details.append(tivcgu.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(tivcgu.getPriority()).append(" ");
                    details.append(tivcgu.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(tivcgu.getSessionMode().name()).append(" ");
                    details.append("CALL TIMER:").append(tivcgu.getCallTimer());

                    P25CallEvent event = new P25CallEvent.Builder(
                        CallEventType.TELEPHONE_INTERCONNECT)
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

                broadcast(new TrafficChannelAllocationEvent(this,
                    mChannelCallMap.get(channel)));
                break;
            case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT:
                UnitToUnitVoiceChannelGrant uuvcg =
                    (UnitToUnitVoiceChannelGrant)message;

                channel = uuvcg.getChannelIdentifier() + "-" +
                    uuvcg.getChannelNumber();
                from = uuvcg.getSourceAddress();
                to = uuvcg.getTargetAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(uuvcg.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(uuvcg.getPriority()).append(" ");
                    details.append(uuvcg.isEncryptedChannel() ? " ENCRYPTED " : "");
                    details.append(uuvcg.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(
                        CallEventType.UNIT_TO_UNIT_CALL)
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

                broadcast(new TrafficChannelAllocationEvent(this,
                    mChannelCallMap.get(channel)));
                break;
            case UNIT_TO_UNIT_VOICE_CHANNEL_GRANT_UPDATE:
                UnitToUnitVoiceChannelGrantUpdate uuvcgu =
                    (UnitToUnitVoiceChannelGrantUpdate)message;

                channel = uuvcgu.getChannelIdentifier() + "-" +
                    uuvcgu.getChannelNumber();
                from = uuvcgu.getSourceAddress();
                to = uuvcgu.getTargetAddress();

                if(hasCallEvent(channel, from, to))
                {
                    updateCallEvent(channel, from, to);
                }
                else
                {
                    StringBuilder details = new StringBuilder();
                    details.append(uuvcgu.isEmergency() ? "EMERGENCY " : "");
                    details.append("PRI:").append(uuvcgu.getPriority()).append(" ");
                    details.append(uuvcgu.isEncryptedChannel() ? "ENCRYPTED " : "");
                    details.append(uuvcgu.getSessionMode().name());

                    P25CallEvent event = new P25CallEvent.Builder(
                        CallEventType.UNIT_TO_UNIT_CALL)
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

                broadcast(new TrafficChannelAllocationEvent(this,
                    mChannelCallMap.get(channel)));
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
                CallAlert ca = (CallAlert)message;

                broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
                    .aliasList(getAliasList())
                    .from(ca.getSourceID())
                    .to(ca.getTargetAddress())
                    .build());
                break;
            case UNIT_TO_UNIT_ANSWER_REQUEST:
                UnitToUnitAnswerRequest utuar = (UnitToUnitAnswerRequest)message;

                if(mLastPageEventID == null || !mLastPageEventID
                    .contentEquals(utuar.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
                        .aliasList(getAliasList())
                        .details((utuar.isEmergency() ? "EMERGENCY" : ""))
                        .from(utuar.getSourceAddress())
                        .to(utuar.getTargetAddress())
                        .build());

                    mLastPageEventID = utuar.getTargetAddress();
                }
                break;
            case SNDCP_DATA_PAGE_REQUEST:
                SNDCPDataPageRequest sdpr = (SNDCPDataPageRequest)message;

                if(mLastPageEventID == null || !mLastPageEventID
                    .contentEquals(sdpr.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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
                TelephoneInterconnectAnswerRequest tiar =
                    (TelephoneInterconnectAnswerRequest)message;

                if(mLastPageEventID == null || !mLastPageEventID
                    .contentEquals(tiar.getTargetAddress()))
                {
                    broadcast(new P25CallEvent.Builder(CallEventType.PAGE)
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

        sb.append("Activity Summary\n");
        sb.append("Decoder:\tP25\n");
        sb.append("===================== THIS SITE ======================");

        if(mNetworkStatus != null)
        {
            sb.append("\nNAC:\t" + mNetworkStatus.getNAC());
            sb.append("\nWACN-SYS:\t" + mNetworkStatus.getWACN());
            sb.append("-" + mNetworkStatus.getSystemID());
            sb.append(" [");
            sb.append(mNetworkStatus.getNetworkCallsign());
            sb.append("]");
            sb.append("\nLRA:\t" + mNetworkStatus.getLocationRegistrationArea());
        }
        else if(mNetworkStatusExtended != null)
        {
            sb.append("\nNAC:\t" + mNetworkStatusExtended.getNAC());
            sb.append("\nWACN-SYS:\t" + mNetworkStatusExtended.getWACN());
            sb.append("-" + mNetworkStatusExtended.getSystemID());
            sb.append(" [");
            sb.append(mNetworkStatusExtended.getNetworkCallsign());
            sb.append("]");
            sb.append("\nLRA:\t" + mNetworkStatusExtended.getLocationRegistrationArea());
        }

        String site = null;

        if(mRFSSStatusMessage != null)
        {
            site = mRFSSStatusMessage.getRFSubsystemID() + "-" + mRFSSStatusMessage.getSiteID();
        }
        else if(mRFSSStatusMessageExtended != null)
        {
            site = mRFSSStatusMessageExtended.getRFSubsystemID() + "-" +
                mRFSSStatusMessageExtended.getSiteID();
        }

        sb.append("\nRFSS-SITE:\t" + site);

        if(hasAliasList())
        {
            Alias siteAlias = getAliasList().getSiteID(site);

            if(siteAlias != null)
            {
                sb.append(" " + siteAlias.getName());
            }
        }

        if(mNetworkStatus != null)
        {
            sb.append("\nSERVICES:\t" + SystemService.toString(
                mNetworkStatus.getSystemServiceClass()));
            sb.append("\nPCCH:\tDNLINK " + mFrequencyFormatter.format(
                (double)mNetworkStatus.getDownlinkFrequency() / 1E6d) +
                " [" + mNetworkStatus.getIdentifier() + "-" +
                mNetworkStatus.getChannel() + "]\n");
            sb.append("\tUPLINK " + mFrequencyFormatter.format(
                (double)mNetworkStatus.getUplinkFrequency() / 1E6d) + " [" +
                mNetworkStatus.getIdentifier() + "-" +
                mNetworkStatus.getChannel() + "]\n");
        }
        else if(mNetworkStatusExtended != null)
        {
            sb.append("\nSERVICES:\t" + SystemService.toString(
                mNetworkStatusExtended.getSystemServiceClass()));
            sb.append("\nPCCH:\tDNLINK " + mFrequencyFormatter.format(
                (double)mNetworkStatusExtended.getDownlinkFrequency() / 1E6d) +
                " [" + mNetworkStatusExtended.getTransmitIdentifier() + "-" +
                mNetworkStatusExtended.getTransmitChannel() + "]\n");
            sb.append("\tUPLINK " + mFrequencyFormatter.format(
                (double)mNetworkStatusExtended.getUplinkFrequency() / 1E6d) +
                " [" + mNetworkStatusExtended.getReceiveIdentifier() + "-" +
                mNetworkStatusExtended.getReceiveChannel() + "]");
        }

        if(mSecondaryControlChannels.isEmpty())
        {
            sb.append("\nSCCH:\tNONE");
        }
        else
        {
            for(module.decode.p25.message.tsbk.osp.control.SecondaryControlChannelBroadcast
                sec : mSecondaryControlChannels)
            {
                sb.append("\nSCCH:\tDNLINK " + mFrequencyFormatter.format(
                    (double)sec.getDownlinkFrequency1() / 1E6d) +
                    " [" + sec.getIdentifier1() + "-" + sec.getChannel1() + "]\n");
                sb.append("\tUPLINK " + mFrequencyFormatter.format(
                    (double)sec.getUplinkFrequency1() / 1E6d) + " [" +
                    sec.getIdentifier1() + "-" + sec.getChannel1() + "]\n");

                if(sec.hasChannel2())
                {
                    sb.append("\nSCCH:\tDNLINK " + mFrequencyFormatter.format(
                        (double)sec.getDownlinkFrequency2() / 1E6d) +
                        " [" + sec.getIdentifier2() + "-" + sec.getChannel2() + "]\n");
                    sb.append("\tUPLINK " + mFrequencyFormatter.format(
                        (double)sec.getUplinkFrequency2() / 1E6d) + " [" +
                        sec.getIdentifier2() + "-" + sec.getChannel2() + "]");
                }
            }
        }

        if(mSNDCPDataChannel != null)
        {
            sb.append("\nSNDCP:");
            sb.append("\tDNLINK " + mFrequencyFormatter.format(
                (double)mSNDCPDataChannel.getDownlinkFrequency() / 1E6D) +
                " [" + mSNDCPDataChannel.getTransmitChannel() + "]");
            sb.append("\tUPLINK " + mFrequencyFormatter.format(
                (double)mSNDCPDataChannel.getUplinkFrequency() / 1E6D) +
                " [" + mSNDCPDataChannel.getReceiveChannel() + "]");
        }

        if(mProtectionParameterBroadcast != null)
        {
            sb.append("\nENCRYPTION:");
            sb.append("\nTYPE:\t" + mProtectionParameterBroadcast
                .getEncryptionType().name());
            sb.append("\nALGORITHM:\t" + mProtectionParameterBroadcast
                .getAlgorithmID());
            sb.append("\nKEY:\t" + mProtectionParameterBroadcast.getKeyID());
            sb.append("\nINBOUND IV:\t" + mProtectionParameterBroadcast
                .getInboundInitializationVector());
            sb.append("\nOUTBOUND IV:\t" + mProtectionParameterBroadcast
                .getOutboundInitializationVector());
        }

        List<Integer> identifiers = new ArrayList<>(mBands.keySet());

        Collections.sort(identifiers);

        sb.append("\nFREQUENCY BANDS:");
        for(Integer id : identifiers)
        {
            IBandIdentifier band = mBands.get(id);

            sb.append("\n\t" + id);
            sb.append("- BASE: " + mFrequencyFormatter.format(
                (double)band.getBaseFrequency() / 1E6d));
            sb.append(" CHANNEL SIZE: " + mFrequencyFormatter.format(
                (double)band.getChannelSpacing() / 1E6d));
            sb.append(" UPLINK OFFSET: " + mFrequencyFormatter.format(
                (double)band.getTransmitOffset() / 1E6D));
        }

        sb.append("\n\n=================== NEIGHBORS ======================");

        if(mNeighborMap.isEmpty())
        {
            sb.append("\n\tNONE\n");
            sb.append("\n----------------------------------------------------");
        }
        else
        {
            for(IAdjacentSite neighbor : mNeighborMap.values())
            {
                sb.append("\nNAC:\t" + ((P25Message)neighbor).getNAC());
                sb.append("\nSYSTEM:\t" + neighbor.getSystemID());
                sb.append("\nLRA:\t" + neighbor.getLRA());

                String neighborID = neighbor.getRFSS() + "-" + neighbor.getSiteID();
                sb.append("\nRFSS-SITE:\t" + neighborID);

                if(hasAliasList())
                {
                    Alias siteAlias = getAliasList().getSiteID(neighborID);

                    if(siteAlias != null)
                    {
                        sb.append(" " + siteAlias.getName());
                    }
                }

                sb.append("\nPCCH:\tDNLINK " + mFrequencyFormatter.format(
                    (double)neighbor.getDownlinkFrequency() / 1E6d) +
                    " [" + neighbor.getDownlinkChannel() + "]");
                sb.append("\n\tUPLINK:" + mFrequencyFormatter.format(
                    (double)neighbor.getUplinkFrequency() / 1E6d) +
                    " [" + neighbor.getDownlinkChannel() + "]\n");
                sb.append("\nSERVICES:\t" + neighbor.getSystemServiceClass());
                sb.append("\n----------------------------------------------------");
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
                        TrafficChannelAllocationEvent allocationEvent = (TrafficChannelAllocationEvent)event;

                        mCurrentCallEvent = (P25CallEvent)allocationEvent.getCallEvent();

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
    public void start(ScheduledExecutorService executor)
    {
        //Change the default (45-second) traffic channel timeout to 1 second
        if(mChannelType == ChannelType.TRAFFIC)
        {
            broadcast(new ChangeChannelTimeoutEvent(this, ChannelType.TRAFFIC, 1000));
        }
    }
}
