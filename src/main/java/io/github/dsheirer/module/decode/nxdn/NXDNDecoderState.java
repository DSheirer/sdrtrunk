/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.channel.state.ChangeChannelTimeoutEvent;
import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.ControlChannelInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.SiteInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Audio;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallAssignment;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallAssignmentDuplicateControl;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallReceptionRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlRequestWithESN;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallRequestHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusInquiryRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusInquiryResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCall;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallAssignment;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallAssignmentDuplicateControl;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallAssignmentDuplicateTraffic;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallConnectionResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallReceptionRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.data.GPS;
import io.github.dsheirer.module.decode.nxdn.layer3.data.NXDNPacketMessage;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryRequestMultiSystem;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryResponseMultiSystem;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationParameterInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.GroupRegistrationResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.GroupRegistrationResponseTypeD;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationClearResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationCommand;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationResponseTypeD;
import io.github.dsheirer.module.decode.nxdn.layer3.proprietary.TalkerAliasComplete;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallType;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * NXDN decoder state
 */
public class NXDNDecoderState extends DecoderState
{
    private static final int IDLE_DURING_CALL_MAX_COUNT = 5;
    private final Channel mChannel;
    private final NXDNNetworkConfigurationMonitor mNetworkConfigurationMonitor = new NXDNNetworkConfigurationMonitor();
    private final NXDNTrafficChannelManager mTrafficChannelManager;
    private final boolean mTrunkingEnabled;
    private DecodeEvent mCurrentConventionalCallEvent;
    private boolean mEncryptedCallStateDetermined = false;
    private boolean mEncryptedCall = false;
    private int mDisconnectResponseCount = 0;
    private int mIdleDuringCallCount = IDLE_DURING_CALL_MAX_COUNT;
    private long mLastValidMessageTimestamp;
    private long mLastAudioTimestamp;
    private long mLastStateTimestamp;
    private State mLastState = State.IDLE;

    /**
     * Constructs an instance
     * @param channel for this state
     * @param trafficChannelManager shared with this channel
     */
    public NXDNDecoderState(Channel channel, NXDNTrafficChannelManager trafficChannelManager)
    {
        mChannel = channel;
        mTrafficChannelManager = trafficChannelManager;
        mTrunkingEnabled = !(channel != null && channel.getDecodeConfiguration() instanceof DecodeConfigNXDN config &&
            config.isConventional());
    }

    /**
     * Indicates if we have a non-null traffic channel manager
     */
    private boolean hasTrafficChannelManager()
    {
        return mTrafficChannelManager != null;
    }

    @Override
    public void init()
    {
    }

    @Override
    public void reset()
    {
        if(!mTrunkingEnabled)
        {
            resetState();
            setCurrentFrequency(0);
        }

        super.reset();
    }

    @Override
    protected void resetState()
    {
        if(!mTrunkingEnabled)
        {
            closeCurrentConventionalCall(System.currentTimeMillis());
        }

        super.resetState();
        mEncryptedCallStateDetermined = false;
        mEncryptedCall = false;
        mDisconnectResponseCount = 0;
        mIdleDuringCallCount = IDLE_DURING_CALL_MAX_COUNT;
    }

    @Override
    public void start()
    {
        super.start();

        //Allow short fades without tearing down an otherwise healthy NXDN traffic call.
        if(mChannel.isTrafficChannel())
        {
            broadcast(new ChangeChannelTimeoutEvent(this, Channel.ChannelType.TRAFFIC, 8000));
        }
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NXDN;
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_RESET:
                resetState();
                break;
            case NOTIFICATION_SOURCE_FREQUENCY:
                if(!mTrunkingEnabled && mChannel.isStandardChannel() &&
                    getCurrentFrequency() != event.getFrequency())
                {
                    closeCurrentConventionalCall(System.currentTimeMillis());
                    setCurrentChannel(null);
                }

                setCurrentFrequency(event.getFrequency());

                //Only update the traffic channel manager if we're not a traffic channel.
                if(mTrunkingEnabled && hasTrafficChannelManager() && mChannel.isStandardChannel())
                {
                    mTrafficChannelManager.setCurrentControlFrequency(getCurrentFrequency(), mChannel);
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
        sb.append("NXDN Channel Activity Summary\n");

        if(mChannel.getDecodeConfiguration() instanceof DecodeConfigNXDN configNXDN)
        {
            sb.append("Transmission Mode: ").append(configNXDN.getTransmissionMode()).append("\n");
        }
        sb.append(mNetworkConfigurationMonitor.getSummary()).append("\n");
        if(hasTrafficChannelManager())
        {
            sb.append(mTrafficChannelManager.getTalkerAliasManager().getAliasSummary()).append("\n");
        }
        sb.append("Channel Mode: ").append(mTrunkingEnabled ? "Trunked" : "Conventional").append("\n");
        sb.append("Last Valid NXDN Message: ").append(mLastValidMessageTimestamp).append("\n");
        sb.append("Last NXDN Audio: ").append(mLastAudioTimestamp).append("\n");
        sb.append("Last State: ").append(mLastState).append(" at ").append(mLastStateTimestamp).append("\n");
        return sb.toString();
    }

    @Override
    public void receive(IMessage iMessage)
    {
        if(iMessage instanceof NXDNMessage nxdn && nxdn.isValid())
        {
            mLastValidMessageTimestamp = nxdn.getTimestamp();

            if(nxdn instanceof NXDNLayer3Message layer3)
            {
                processLayer3(layer3);
            }
            else if(nxdn instanceof Audio audio)
            {
                processAudio(audio);
            }
            else if(nxdn instanceof NXDNPacketMessage packetMessage)
            {
                processPacketMessage(packetMessage);
            }
        }
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
        MutableIdentifierCollection mic = getMutableIdentifierCollection(identifiers);

        if(hasTrafficChannelManager())
        {
            mTrafficChannelManager.getTalkerAliasManager().enrichMutable(mic);
        }
        broadcast(NXDNDecodeEvent.builder(decodeEventType, timestamp)
                .channel(getCurrentChannel())
                .details(details)
                .identifiers(mic)
                .build());
    }

    /**
     * Creates a copy of the current identifier collection, removes any USER identifiers and adds the argument identifiers
     * passing each identifier through the patch group manager to replace with a patch group if it exists
     * @param identifiers to add to the collection copy
     * @return collection
     */
    private MutableIdentifierCollection getMutableIdentifierCollection(List<Identifier> identifiers)
    {
        MutableIdentifierCollection requestCollection = new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());
        requestCollection.remove(IdentifierClass.USER);

        for(Identifier identifier: identifiers)
        {
            requestCollection.update(identifier);
        }

        return requestCollection;
    }

    /**
     * Creates a copy of the current identifier collection, removes any USER identifiers and adds the argument identifier
     * passed through the patch group manager to replace with a patch group if it exists
     * @param identifier to add to the collection copy
     * @return collection
     */
    private MutableIdentifierCollection getMutableIdentifierCollection(Identifier identifier)
    {
        return getMutableIdentifierCollection(Collections.singletonList(identifier));
    }

    /**
     * Process layer 3 messages
     * @param layer3 message to process
     */
    private void processLayer3(NXDNLayer3Message layer3)
    {
        State state = mTrunkingEnabled && layer3.getMessageType().isControl() ? State.CONTROL : State.ACTIVE;
        DecoderStateEvent.Event event = DecoderStateEvent.Event.DECODE;

        switch(layer3.getMessageType())
        {
            case CONTROL_OUT_01_CC_VOICE_CALL_RESPONSE:
            case TYPE_D_OUT_00_CC_CALL_RESPONSE:
                if(layer3 instanceof VoiceCallResponse vcr)
                {
                    //Controller responds to a voice call request with either a channel grant, or this message if
                    //it can't support the call or the called party is not available
                    broadcastEvent(vcr.getIdentifiers(), vcr.getTimestamp(), DecodeEventType.RESPONSE,
                            "VOICE CALL REQUEST - RESPONSE:" + vcr.getCause().toString());
                }
                break;
            case TRAFFIC_IN_01_CC_VOICE_CALL:
            case TRAFFIC_OUT_01_CC_VOICE_CALL:
            case TYPE_D_IN_01_CC_VOICE_CALL:
            case TYPE_D_OUT_01_CC_VOICE_CALL:
                if(layer3 instanceof VoiceCall vc)
                {
                    mIdleDuringCallCount = 0;

                    if(!mTrunkingEnabled)
                    {
                        getIdentifierCollection().remove(IdentifierClass.USER);
                    }

                    getIdentifierCollection().update(vc.getIdentifiers());
                    mEncryptedCallStateDetermined = true;
                    boolean encrypted = vc.getEncryptionKeyIdentifier().isEncrypted();
                    boolean started = true;

                    if(mTrunkingEnabled)
                    {
                        mEncryptedCall = encrypted;

                        if(hasTrafficChannelManager())
                        {
                            mTrafficChannelManager.processVoiceCall(vc, getCurrentChannel());
                        }
                    }
                    else
                    {
                        started = updateConventionalCall(vc);
                        mEncryptedCall = started ? encrypted : mEncryptedCall || encrypted;
                    }

                    state = mEncryptedCall ? State.ENCRYPTED : State.CALL;
                    event = started ? DecoderStateEvent.Event.START : DecoderStateEvent.Event.CONTINUATION;
                }
                break;
            case CONTROL_OUT_02_CC_VOICE_CALL_RECEPTION_REQUEST:
            case TRAFFIC_OUT_02_CC_VOICE_CALL_RECEPTION_REQUEST:
                if(layer3 instanceof VoiceCallReceptionRequest vcrr)
                {
                    broadcastEvent(vcrr.getIdentifiers(), vcrr.getTimestamp(), DecodeEventType.PAGE,
                            "VOICE CALL RECEPTION REQUEST");
                }
                break;
            case CONTROL_OUT_03_CC_VOICE_CALL_CONNECTION_RESPONSE:
                if(layer3 instanceof VoiceCallConnectionResponse vccr)
                {
                    //Controller responds to a voice call connection request for the called party with either a channel
                    //grant, or this message if it can't support the call or connect the called party
                    broadcastEvent(vccr.getIdentifiers(), vccr.getTimestamp(), DecodeEventType.RESPONSE,
                            "CALL CONNECTION REQUEST - RESPONSE:" + vccr.getCause().toString());
                }
                break;
            case TRAFFIC_IN_03_CC_VOICE_CALL_INITIALIZATION_VECTOR:
            case TRAFFIC_OUT_03_CC_VOICE_CALL_INITIALIZATION_VECTOR:
            case TYPE_D_IN_03_CC_VOICE_CALL_INITIALIZATION_VECTOR:
            case TYPE_D_OUT_03_CC_VOICE_CALL_INITIALIZATION_VECTOR:
                state = State.CALL;
                event = DecoderStateEvent.Event.CONTINUATION;
                mIdleDuringCallCount = 0;

                if(!mTrunkingEnabled)
                {
                    updateConventionalCallProgress(layer3.getTimestamp());
                }
                break;
            case CONTROL_OUT_04_CC_VOICE_CALL_ASSIGNMENT:
            case TRAFFIC_OUT_04_CC_VOICE_CALL_ASSIGNMENT:
            case TYPE_D_OUT_04_CC_VOICE_CALL_ASSIGNMENT:
                if(layer3 instanceof VoiceCallAssignment vca)
                {
                    //Decode event is created by the traffic channel manager
                    if(mTrunkingEnabled && hasTrafficChannelManager())
                    {
                        mTrafficChannelManager.processVoiceCallAssignment(vca);
                    }
                }
                break;
            case CONTROL_OUT_05_CC_VOICE_CALL_ASSIGNMENT_DUPLICATE:
                if(layer3 instanceof VoiceCallAssignmentDuplicateControl vcadc)
                {
                    //This informs when there are 2-calls ongoing where a radio can participate in either call
                    if(mTrunkingEnabled && hasTrafficChannelManager())
                    {
                        mTrafficChannelManager.processVoiceCallAssignment(vcadc);
                    }
                }
                break;
            case TRAFFIC_OUT_05_CC_VOICE_CALL_ASSIGNMENT_DUPLICATE:
                if(layer3 instanceof VoiceCallAssignmentDuplicateTraffic vcadt)
                {
                    //This informs when there are 2-calls ongoing where a radio can participate in either call
                    if(mTrunkingEnabled && hasTrafficChannelManager())
                    {
                        mTrafficChannelManager.processVoiceCallAssignment(vcadt);
                    }
                }
                break;
            case TRAFFIC_OUT_07_CC_TRANSMISSION_RELEASE_EXTENSION:
            case TRAFFIC_IN_08_CC_TRANSMISSION_RELEASE:
            case TRAFFIC_OUT_08_CC_TRANSMISSION_RELEASE:
            case TYPE_D_IN_08_CC_TRANSMISSION_RELEASE:
            case TYPE_D_OUT_08_CC_TRANSMISSION_RELEASE:
                if(mTrunkingEnabled && hasTrafficChannelManager())
                {
                    mTrafficChannelManager.processEndCall(getCurrentChannel(), layer3.getTimestamp());
                }
                else if(!mTrunkingEnabled)
                {
                    updateCompletionIdentifiers(layer3);
                    closeCurrentConventionalCall(layer3.getTimestamp());
                }
                mEncryptedCallStateDetermined = false;
                mEncryptedCall = false;
                mIdleDuringCallCount = IDLE_DURING_CALL_MAX_COUNT;
                //Only remove the FROM identifier, anticipating a continuation call for the same talkgroup
                getIdentifierCollection().remove(Role.FROM);
                break;
            case CONTROL_OUT_09_CC_DATA_CALL_RESPONSE:
                if(layer3 instanceof DataCallResponse dcr)
                {
                    //Controller responds to a data call request with either a channel grant, or this message if
                    //it can't support the call or the called party is not available
                    broadcastEvent(dcr.getIdentifiers(), dcr.getTimestamp(), DecodeEventType.RESPONSE,
                            "DATA CALL REQUEST - RESPONSE:" + dcr.getCause().toString());
                }
                break;
            case TRAFFIC_OUT_09_CC_DATA_CALL_HEADER:
            case TYPE_D_OUT_09_CC_DATA_CALL_HEADER:
                state = State.DATA;
                break;
            case CONTROL_OUT_10_CC_DATA_CALL_RECEPTION_REQUEST:
                if(layer3 instanceof DataCallReceptionRequest dcrr)
                {
                    broadcastEvent(dcrr.getIdentifiers(), dcrr.getTimestamp(), DecodeEventType.PAGE,
                            "DATA CALL RECEPTION REQUEST");
                }
                break;
            case TRAFFIC_OUT_10_CC_DATA_CALL_RECEPTION_REQUEST:
                break;
            case TRAFFIC_OUT_11_CC_DATA_CALL_BLOCK:
            case TYPE_D_OUT_11_CC_DATA_CALL_BLOCK:
                state = State.DATA;
                break;
            case TRAFFIC_OUT_12_CC_DATA_CALL_ACKNOWLEDGE:
            case TYPE_D_OUT_12_CC_DATA_CALL_ACKNOWLEDGE:
                break;
            case CONTROL_OUT_13_CC_DATA_CALL_ASSIGNMENT_DUPLICATE:
                if(layer3 instanceof DataCallAssignmentDuplicateControl dcadc)
                {
                    if(mTrunkingEnabled && hasTrafficChannelManager())
                    {
                        mTrafficChannelManager.processDataCallAssignment(dcadc);
                    }
                }
                break;
            case TRAFFIC_OUT_13_CC_DATA_CALL_ASSIGNMENT_DUPLICATE:
                break;
            case CONTROL_OUT_14_CC_DATA_CALL_ASSIGNMENT:
                if(layer3 instanceof DataCallAssignment dca)
                {
                    if(mTrunkingEnabled && hasTrafficChannelManager())
                    {
                        mTrafficChannelManager.processDataCallAssignment(dca);
                    }
                }
                break;
            case TRAFFIC_OUT_14_CC_DATA_CALL_ASSIGNMENT:
                break;
            case TRAFFIC_OUT_15_CC_HEADER_DELAY:
            case TYPE_D_OUT_15_CC_HEADER_DELAY:
                break;
            case CONTROL_OUT_16_CC_IDLE:
                break;
            case TRAFFIC_OUT_16_CC_IDLE:
            case TYPE_D_OUT_16_CC_IDLE:
                //Hack: on both 4800 and 9600 systems there can be instances during a call where they send IDLE frames
                //We'll track when we're in a call and continue a call state until the sequence of IDLE messages
                //exceeds a threshold and then the state will be flipped to ACTIVE from CALL.
                mIdleDuringCallCount++;

                if(mIdleDuringCallCount < IDLE_DURING_CALL_MAX_COUNT)
                {
                    state = State.CALL;
                }
                else
                {
                    if(!mTrunkingEnabled)
                    {
                        closeCurrentConventionalCall(layer3.getTimestamp());
                    }

                    mEncryptedCallStateDetermined = false;
                    mEncryptedCall = false;
                }
                break;
            case CONTROL_OUT_17_CC_DISCONNECT:
                //Ignore.  If this happens on the current/active control channel, in a multi-channel configuration,
                //the frequency rotation manager will simply move to the next channel.
                break;
            case TRAFFIC_OUT_17_CC_DISCONNECT:
            case TYPE_D_OUT_17_CC_DISCONNECT:
                if(mTrunkingEnabled && hasTrafficChannelManager())
                {
                    mTrafficChannelManager.processEndCall(getCurrentChannel(), layer3.getTimestamp());
                }
                else if(!mTrunkingEnabled)
                {
                    updateCompletionIdentifiers(layer3);
                    closeCurrentConventionalCall(layer3.getTimestamp());
                }
                mEncryptedCallStateDetermined = false;
                mEncryptedCall = false;
                getIdentifierCollection().remove(IdentifierClass.USER);
                mDisconnectResponseCount++;

                //Delay channel teardown until after we receive at least 2x disconnect responses
                if(mChannel.isTrafficChannel() && mDisconnectResponseCount >= 2)
                {
                    //Signal that this traffic channel has ended.
                    event = DecoderStateEvent.Event.END;
                    state = State.FADE;
                    mDisconnectResponseCount = 0;
                }
                break;

            //Broadcast messages
            case CONTROL_OUT_23_BC_DIGITAL_STATION_ID_INFORMATION:
            case CONTROL_OUT_24_BC_SITE_INFORMATION:
            case CONTROL_OUT_25_BC_SERVICE_INFORMATION:
            case CONTROL_OUT_27_BC_ADJACENT_SITE_INFORMATION:
            case CONTROL_OUT_28_BC_FAILURE_STATUS_INFORMATION:
                if(mTrunkingEnabled)
                {
                    mNetworkConfigurationMonitor.process(layer3);
                }
                getIdentifierCollection().remove(IdentifierClass.USER);
                break;
            case CONTROL_OUT_26_BC_CONTROL_CHANNEL_INFORMATION:
                if(mTrunkingEnabled)
                {
                    mNetworkConfigurationMonitor.process(layer3);

                    if(layer3 instanceof ControlChannelInformation cci)
                    {
                        setCurrentChannel(cci.getChannel1());
                    }
                }
                break;
            case TRAFFIC_OUT_24_BC_SITE_INFORMATION:
                if(layer3 instanceof SiteInformation si)
                {
                    //Update traffic channel manager so it can use this when allocation traffic channels.
                    if(mTrunkingEnabled && hasTrafficChannelManager())
                    {
                        mTrafficChannelManager.setChannelAccessInformation(si.getChannelAccessInformation());
                    }
                }
                //Deliberate fall through
            case TRAFFIC_OUT_23_BC_DIGITAL_STATION_ID_INFORMATION:
            case TYPE_D_OUT_23_BC_DIGITAL_STATION_ID:
            case TRAFFIC_OUT_25_BC_SERVICE_INFORMATION:
            case TYPE_D_OUT_25_BC_SERVICE_INFORMATION:
            case TRAFFIC_OUT_26_BC_CONTROL_CHANNEL_INFORMATION:
            case TRAFFIC_OUT_27_BC_ADJACENT_SITE_INFORMATION:
            case TYPE_D_OUT_27_BC_ADJACENT_SITE_INFORMATION:
            case TRAFFIC_OUT_28_BC_FAILURE_STATUS_INFORMATION:
            case TYPE_D_SCCH_OUT_INFO_4_REPEATER_IDLE:
            case TYPE_D_SCCH_OUT_INFO_4_REPEATER_FREE:
            case TYPE_D_SCCH_OUT_INFO_4_REPEATER_HALT:
            case TYPE_D_SCCH_OUT_INFO_4_SITE_ID:
                if(mTrunkingEnabled)
                {
                    mNetworkConfigurationMonitor.process(layer3);
                }
                break;
            case CONTROL_OUT_32_MM_REGISTRATION_RESPONSE:
                if(layer3 instanceof RegistrationResponse rr)
                {
                    broadcastEvent(rr.getIdentifiers(), rr.getTimestamp(), DecodeEventType.REGISTER, rr.getCause().toString());
                }
                break;
            case TYPE_D_OUT_32_MM_REGISTRATION_RESPONSE:
                if(layer3 instanceof RegistrationResponseTypeD rr)
                {
                    broadcastEvent(rr.getIdentifiers(), rr.getTimestamp(), DecodeEventType.REGISTER, rr.getCause().toString());
                }
                break;
            case CONTROL_OUT_34_MM_REGISTRATION_CLEAR_RESPONSE:
            case TYPE_D_OUT_34_MM_REGISTRATION_CLEAR_RESPONSE:
                if(layer3 instanceof RegistrationClearResponse rcr)
                {
                    broadcastEvent(rcr.getIdentifiers(), rcr.getTimestamp(), DecodeEventType.RESPONSE,
                            "CLEAR REGISTRATION REQUEST - RESPONSE:" + rcr.getCause());
                }
                break;
            case CONTROL_OUT_35_MM_REGISTRATION_COMMAND:
                if(layer3 instanceof RegistrationCommand rc)
                {
                    broadcastEvent(rc.getIdentifiers(), rc.getTimestamp(), DecodeEventType.REGISTER,
                            rc.getRegistrationOption().isPriorityStation() ? "PRIORITY STATION" : "");
                }
                break;
            case CONTROL_OUT_36_MM_GROUP_REGISTRATION_RESPONSE:
                if(layer3 instanceof GroupRegistrationResponse grr)
                {
                    broadcastEvent(grr.getIdentifiers(), grr.getTimestamp(), DecodeEventType.RESPONSE,
                            "GROUP REGISTRATION REQUEST - RESPONSE:" + grr.getCause());
                }
                break;
            case TYPE_D_OUT_36_MM_GROUP_REGISTRATION_RESPONSE:
                if(layer3 instanceof GroupRegistrationResponseTypeD grr)
                {
                    broadcastEvent(grr.getIdentifiers(), grr.getTimestamp(), DecodeEventType.RESPONSE,
                            "GROUP REGISTRATION REQUEST - RESPONSE:" + grr.getCause());
                }
                break;
            case TYPE_D_OUT_38_MM_AUTHENTICATION_PARAMETER_INFORMATION:
                if(layer3 instanceof AuthenticationParameterInformation api)
                {
                    broadcastEvent(api.getIdentifiers(), api.getTimestamp(), DecodeEventType.REQUEST,
                            "AUTHENTICATE: " + api.getAuthenticationParameter());
                }
                break;
            case CONTROL_OUT_40_MM_AUTHENTICATION_INQUIRY_REQUEST:
            case TYPE_D_OUT_40_MM_AUTHENTICATION_INQUIRY_REQUEST:
                if(layer3 instanceof AuthenticationInquiryRequest air)
                {
                    broadcastEvent(air.getIdentifiers(), air.getTimestamp(), DecodeEventType.REQUEST,
                            "AUTHENTICATE:" + air.getAuthenticationParameter());
                }
                break;
            case TYPE_D_OUT_41_MM_AUTHENTICATION_INQUIRY_RESPONSE:
                if(layer3 instanceof AuthenticationInquiryResponse air)
                {
                    broadcastEvent(air.getIdentifiers(), air.getTimestamp(), DecodeEventType.RESPONSE,
                            "AUTHENTICATION:" + air.getAuthenticationValue());
                }
                break;
            case CONTROL_OUT_42_MM_AUTHENTICATION_INQUIRY_REQUEST_MULTI_SYSTEM:
            case TRAFFIC_OUT_42_MM_AUTHENTICATION_INQUIRY_REQUEST_MULTI_SYSTEM:
                if(layer3 instanceof AuthenticationInquiryRequestMultiSystem air)
                {
                    broadcastEvent(air.getIdentifiers(), air.getTimestamp(), DecodeEventType.REQUEST,
                            "AUTHENTICATE (MULTI-SYSTEM):" + air.getAuthenticationParameter());
                }
                break;
            case TRAFFIC_OUT_43_MM_AUTHORIZATION_INQUIRY_RESPONSE_MULTI_SYSTEM:
                if(layer3 instanceof AuthenticationInquiryResponseMultiSystem airms)
                {
                    broadcast(NXDNDecodeEvent.builder(DecodeEventType.RESPONSE, layer3.getTimestamp())
                            .channel(getCurrentChannel())
                            .identifiers(getMutableIdentifierCollection(airms.getIdentifiers()))
                            .details("AUTHENTICATION: " + airms.getAuthenticationValue())
                            .build());
                }
                break;
            case TYPE_D_OUT_43_MM_DATA_WRITE_HEADER:
            case TYPE_D_OUT_44_MM_DATA_WRITE_DATA:
                state = State.DATA;
                break;
            case CONTROL_OUT_48_CC_STATUS_INQUIRY_REQUEST:
            case TRAFFIC_OUT_48_CC_STATUS_INQUIRY_REQUEST:
            case TYPE_D_OUT_48_CC_STATUS_INQUIRY_REQUEST:
                if(layer3 instanceof StatusInquiryRequest sir)
                {
                    broadcastEvent(sir.getIdentifiers(), sir.getTimestamp(), DecodeEventType.REQUEST,
                            "STATUS INQUIRY");
                }
                break;
            case CONTROL_OUT_49_CC_STATUS_INQUIRY_RESPONSE:
            case TRAFFIC_OUT_49_CC_STATUS_INQUIRY_RESPONSE:
            case TYPE_D_OUT_49_CC_STATUS_INQUIRY_RESPONSE:
                if(layer3 instanceof StatusInquiryResponse sir)
                {
                    broadcastEvent(sir.getIdentifiers(), sir.getTimestamp(), DecodeEventType.RESPONSE,
                            "STATUS INQUIRY:" + sir.getStatus() + ":" + sir.getStatusValue() + " CAUSE:" + sir.getCause());
                }
                break;
            case CONTROL_OUT_50_CC_STATUS_REQUEST:
            case TRAFFIC_OUT_50_CC_STATUS_REQUEST:
            case TYPE_D_OUT_50_CC_STATUS_REQUEST:
                if(layer3 instanceof StatusRequest sr)
                {
                    broadcastEvent(sr.getIdentifiers(), sr.getTimestamp(), DecodeEventType.REQUEST,
                            "STATUS REQUEST:" + sr.getStatus());
                }
                break;
            case CONTROL_OUT_51_CC_STATUS_RESPONSE:
            case TRAFFIC_OUT_51_CC_STATUS_RESPONSE:
            case TYPE_D_OUT_51_CC_STATUS_RESPONSE:
                if(layer3 instanceof StatusResponse sr)
                {
                    broadcastEvent(sr.getIdentifiers(), sr.getTimestamp(), DecodeEventType.REQUEST,
                            "STATUS RESPONSE:" + sr.getCause());
                }
                break;
            case CONTROL_OUT_52_CC_REMOTE_CONTROL_REQUEST:
            case TRAFFIC_OUT_52_CC_REMOTE_CONTROL_REQUEST:
            case TYPE_D_OUT_52_CC_REMOTE_CONTROL_REQUEST:
                if(layer3 instanceof RemoteControlRequest rcr)
                {
                    broadcastEvent(rcr.getIdentifiers(), rcr.getTimestamp(), DecodeEventType.REQUEST,
                            "REMOTE CONTROL REQUEST - PARAMETERS:" + rcr.getControlParameters());
                }
                break;
            case CONTROL_OUT_53_CC_REMOTE_CONTROL_RESPONSE:
            case TRAFFIC_OUT_53_CC_REMOTE_CONTROL_RESPONSE:
            case TYPE_D_OUT_53_CC_REMOTE_CONTROL_RESPONSE:
                if(layer3 instanceof RemoteControlResponse rcr)
                {
                    broadcastEvent(rcr.getIdentifiers(), rcr.getTimestamp(), DecodeEventType.REQUEST,
                            "REMOTE CONTROL " + rcr.getControlCommand() + " " + rcr.getCause());
                }
                break;
            case CONTROL_OUT_54_CC_REMOTE_CONTROL_REQUEST_WITH_ESN:
                if(layer3 instanceof RemoteControlRequestWithESN rcrwe)
                {
                    broadcastEvent(rcrwe.getIdentifiers(), rcrwe.getTimestamp(), DecodeEventType.REQUEST,
                            "REMOTE CONTROL REQUEST WITH ESN - PARAMETERS:" + rcrwe.getAuthenticationParameter());
                }
                break;
            case CONTROL_OUT_56_CC_SHORT_DATA_CALL_REQUEST_HEADER:
            case TRAFFIC_OUT_56_CC_SHORT_DATA_CALL_REQUEST_HEADER:
            case TYPE_D_OUT_56_CC_SHORT_DATA_CALL_REQUEST_HEADER:
                if(layer3 instanceof ShortDataCallRequestHeader sdcrh)
                {
                    broadcast(NXDNDecodeEvent.builder(DecodeEventType.REQUEST, layer3.getTimestamp())
                            .channel(getCurrentChannel())
                            .identifiers(getMutableIdentifierCollection(sdcrh.getIdentifiers()))
                            .details("SHORT DATA CALL")
                            .build());
                    state = State.DATA;
                }
                break;
            case TRAFFIC_OUT_57_CC_SHORT_DATA_CALL_BLOCK:
            case TYPE_D_OUT_57_CC_SHORT_DATA_CALL_REQUEST_USER_DATA:
                state = State.DATA;
            case CONTROL_OUT_57_CC_SHORT_DATA_CALL_REQUEST_USER_DATA:
                break;
            case TRAFFIC_OUT_58_CC_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
            case TYPE_D_OUT_58_CC_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
                state = State.DATA;
            case CONTROL_OUT_58_CC_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
                break;
            case CONTROL_OUT_59_CC_SHORT_DATA_CALL_RESPONSE:
            case TRAFFIC_OUT_59_CC_SHORT_DATA_CALL_RESPONSE:
            case TYPE_D_OUT_59_CC_SHORT_DATA_CALL_RESPONSE:
                if(layer3 instanceof ShortDataCallResponse sdcr)
                {
                    broadcast(NXDNDecodeEvent.builder(DecodeEventType.RESPONSE, layer3.getTimestamp())
                            .channel(getCurrentChannel())
                            .identifiers(getMutableIdentifierCollection(sdcr.getIdentifiers()))
                            .details("SHORT DATA CALL: " + sdcr.getCause())
                            .build());
                }
                break;

            case PROPRIETARY_FORM:
                break;
            case TALKER_ALIAS:
                state = State.CALL;
                event = DecoderStateEvent.Event.CONTINUATION;

                if(!mTrunkingEnabled)
                {
                    updateConventionalCallProgress(layer3.getTimestamp());
                }
                break;

            case TALKER_ALIAS_COMPLETE:
                if(layer3 instanceof TalkerAliasComplete tac)
                {
                    state = State.CALL;
                    event = DecoderStateEvent.Event.CONTINUATION;
                    var radio = getIdentifierCollection().getIdentifier(IdentifierClass.USER, Form.RADIO, Role.FROM);

                    if(radio instanceof RadioIdentifier ri)
                    {
                        if(hasTrafficChannelManager())
                        {
                            mTrafficChannelManager.processTalkerAlias(getCurrentChannel(), tac.getTalkerAlias(), ri,
                                tac.getTimestamp());
                        }

                        if(!mTrunkingEnabled)
                        {
                            updateConventionalCallProgress(tac.getTimestamp());
                        }
                    }
                }
                break;

            case TRAFFIC_IN_02_CC_VOICE_CALL_RECEPTION_RESPONSE:
                state = State.CALL;
                break;
            case TRAFFIC_IN_09_CC_DATA_CALL_HEADER:
                state = State.DATA;
                break;
            case TRAFFIC_IN_11_CC_DATA_CALL_BLOCK:
                state = State.DATA;
                break;
            case TRAFFIC_IN_12_CC_DATA_CALL_ACKNOWLEDGE:
            case TRAFFIC_IN_15_CC_HEADER_DELAY:
            case TRAFFIC_IN_42_MM_AUTHENTICATION_INQUIRY_REQUEST_MULTI_SYSTEM:
            case TRAFFIC_IN_43_MM_AUTHORIZATION_INQUIRY_RESPONSE_MULTI_SYSTEM:
            case TRAFFIC_IN_48_CC_STATUS_INQUIRY_REQUEST:
            case TRAFFIC_IN_49_CC_STATUS_INQUIRY_RESPONSE:
            case TRAFFIC_IN_50_CC_STATUS_REQUEST:
            case TRAFFIC_IN_51_CC_STATUS_RESPONSE:
            case TRAFFIC_IN_52_CC_REMOTE_CONTROL_REQUEST:
            case TRAFFIC_IN_53_CC_REMOTE_CONTROL_RESPONSE:
            case TRAFFIC_IN_56_CC_SHORT_DATA_CALL_REQUEST_HEADER:
            case TRAFFIC_IN_57_CC_SHORT_DATA_CALL_BLOCK:
                state = State.DATA;
                break;
            case TRAFFIC_IN_16_CC_IDLE:
                if(!mTrunkingEnabled)
                {
                    mIdleDuringCallCount++;

                    if(mIdleDuringCallCount < IDLE_DURING_CALL_MAX_COUNT)
                    {
                        state = mEncryptedCall ? State.ENCRYPTED : State.CALL;
                    }
                    else
                    {
                        closeCurrentConventionalCall(layer3.getTimestamp());
                        mEncryptedCallStateDetermined = false;
                        mEncryptedCall = false;
                        state = State.ACTIVE;
                    }
                }
                else
                {
                    state = State.DATA;
                }
                break;
            case TRAFFIC_IN_17_CC_DISCONNECT_REQUEST:
                if(!mTrunkingEnabled)
                {
                    updateCompletionIdentifiers(layer3);
                    closeCurrentConventionalCall(layer3.getTimestamp());
                    mEncryptedCallStateDetermined = false;
                    mEncryptedCall = false;
                    getIdentifierCollection().remove(IdentifierClass.USER);
                    state = State.ACTIVE;
                }
                else
                {
                    state = State.DATA;
                }
                break;
            case TRAFFIC_IN_58_CC_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
                state = State.DATA;
                break;

            case UNKNOWN:
                break;
        }

        if(state != mLastState)
        {
            mLastState = state;
            mLastStateTimestamp = layer3.getTimestamp();
        }

        broadcast(new DecoderStateEvent(this, event, state));
    }

    /**
     * Creates or updates the decode event for one explicitly conventional call.
     *
     * @return true when this observation starts a new call
     */
    private boolean updateConventionalCall(VoiceCall voiceCall)
    {
        if(mTrunkingEnabled || voiceCall == null)
        {
            return false;
        }

        MutableIdentifierCollection identifiers =
            new MutableIdentifierCollection(getIdentifierCollection().getIdentifiers());

        if(hasTrafficChannelManager())
        {
            mTrafficChannelManager.getTalkerAliasManager().enrichMutable(identifiers);
        }

        if(mCurrentConventionalCallEvent != null &&
            callIdentityChanged(mCurrentConventionalCallEvent.getIdentifierCollection(), identifiers))
        {
            closeCurrentConventionalCall(voiceCall.getTimestamp());
        }

        DecodeEventType type = callEventType(voiceCall.getCallType(), voiceCall.getEncryptionKeyIdentifier());
        boolean started = mCurrentConventionalCallEvent == null;

        if(started)
        {
            mCurrentConventionalCallEvent = NXDNDecodeEvent.builder(type, voiceCall.getTimestamp())
                .channel(getCurrentChannel())
                .details(voiceCall.getCallOption().toString())
                .identifiers(identifiers)
                .build();
        }
        else
        {
            if(DecodeEventType.VOICE_CALLS_ENCRYPTED.contains(type))
            {
                mCurrentConventionalCallEvent.setDecodeEventType(type);
            }

            mCurrentConventionalCallEvent.setIdentifierCollection(identifiers);
            mCurrentConventionalCallEvent.end(voiceCall.getTimestamp());
        }

        broadcast(mCurrentConventionalCallEvent);
        return started;
    }

    private void updateConventionalCallProgress(long timestamp)
    {
        if(!mTrunkingEnabled && mCurrentConventionalCallEvent != null)
        {
            mCurrentConventionalCallEvent.end(timestamp);
            broadcast(mCurrentConventionalCallEvent);
        }
    }

    private void updateCompletionIdentifiers(NXDNLayer3Message layer3)
    {
        if(layer3 == null)
        {
            return;
        }

        for(Identifier identifier: layer3.getIdentifiers())
        {
            if(identifier != null && identifier.getValue() instanceof Number number && number.intValue() > 0)
            {
                getIdentifierCollection().update(identifier);
            }
        }
    }

    private void closeCurrentConventionalCall(long timestamp)
    {
        if(mCurrentConventionalCallEvent != null)
        {
            mCurrentConventionalCallEvent.end(timestamp);
            broadcast(mCurrentConventionalCallEvent);
            mCurrentConventionalCallEvent = null;
        }
    }

    private static DecodeEventType callEventType(CallType callType, EncryptionKeyIdentifier encryption)
    {
        boolean encrypted = encryption != null && encryption.isEncrypted();
        return switch(callType)
        {
            case GROUP_BROADCAST, GROUP_CONFERENCE ->
                encrypted ? DecodeEventType.CALL_GROUP_ENCRYPTED : DecodeEventType.CALL_GROUP;
            case INDIVIDUAL ->
                encrypted ? DecodeEventType.CALL_UNIT_TO_UNIT_ENCRYPTED : DecodeEventType.CALL_UNIT_TO_UNIT;
            case INTERCONNECT, SPEED_DIAL ->
                encrypted ? DecodeEventType.CALL_INTERCONNECT_ENCRYPTED : DecodeEventType.CALL_INTERCONNECT;
            default -> encrypted ? DecodeEventType.CALL_ENCRYPTED : DecodeEventType.CALL;
        };
    }

    private static boolean callIdentityChanged(IdentifierCollection previous, IdentifierCollection current)
    {
        if(previous == null || current == null)
        {
            return false;
        }

        return identityChanged(previous.getFromIdentifier(), current.getFromIdentifier()) ||
            identityChanged(previous.getToIdentifier(), current.getToIdentifier());
    }

    private static boolean identityChanged(Identifier previous, Identifier current)
    {
        return previous != null && current != null && !previous.equals(current);
    }

    /**
     * Process an audio message
     * @param audio to process
     */
    private void processAudio(Audio audio)
    {
        mLastAudioTimestamp = audio.getTimestamp();
        mIdleDuringCallCount = 0;
        State state = State.CALL;

        if(mEncryptedCallStateDetermined && mEncryptedCall)
        {
            state = State.ENCRYPTED;
        }

        if(mTrunkingEnabled && hasTrafficChannelManager())
        {
            mTrafficChannelManager.processCallProgressUpdate(getCurrentChannel(), audio.getTimestamp());
        }
        else if(!mTrunkingEnabled)
        {
            updateConventionalCallProgress(audio.getTimestamp());
        }

        if(state != mLastState)
        {
            mLastState = state;
            mLastStateTimestamp = audio.getTimestamp();
        }

        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION, state));
    }

    /**
     * Process packet-based messages
     * @param message to process
     */
    private void processPacketMessage(NXDNPacketMessage message)
    {
        if(message instanceof GPS gps)
        {
            PlottableDecodeEvent plottableEvent = PlottableDecodeEvent.plottableBuilder(DecodeEventType.GPS, gps.getTimestamp())
                    .identifiers(new IdentifierCollection(message.getPacketSequence().getHeader().getIdentifiers()))
                    .channel(getCurrentChannel())
                    .details(gps.getLocationFormatted() + " HDG: " + gps.getHeading() + " SPD:" + gps.getSpeed() +
                            " ELE:" + gps.getElevation())
                    .protocol(Protocol.NXDN)
                    .location(gps.getLocation())
                    .heading(gps.getHeading())
                    .speed(gps.getSpeed())
                    .build();
            broadcast(plottableEvent);
        }
    }
}
