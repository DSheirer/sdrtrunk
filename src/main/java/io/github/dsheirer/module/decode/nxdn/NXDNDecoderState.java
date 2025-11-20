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

import io.github.dsheirer.channel.state.DecoderState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.MutableIdentifierCollection;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEventType;
import io.github.dsheirer.module.decode.nxdn.layer2.SACCHFragment;
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
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryRequest2;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.GroupRegistrationResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationClearResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationCommand;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationResponse;

import java.util.Collections;
import java.util.List;

/**
 * NXDN decoder state
 */
public class NXDNDecoderState extends DecoderState
{
    private final Channel mChannelConfiguration;
    private final NXDNNetworkConfigurationMonitor mNetworkConfigurationMonitor = new NXDNNetworkConfigurationMonitor();
    private final NXDNTrafficChannelManager mTrafficChannelManager;

    /**
     * Constructs an instance
     * @param channel for this state
     * @param trafficChannelManager shared with this channel
     */
    public NXDNDecoderState(Channel channel, NXDNTrafficChannelManager trafficChannelManager)
    {
        mChannelConfiguration = channel;
        mTrafficChannelManager = trafficChannelManager;
    }

    @Override
    public void init()
    {
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NXDN;
    }

    @Override
    public void receiveDecoderStateEvent(DecoderStateEvent event)
    {
        //Auxiliary decoder event processing is not supported for NXDN.
    }

    @Override
    public String getActivitySummary()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("NXDN Channel Activity Summary\n");

        if(mChannelConfiguration.getDecodeConfiguration() instanceof DecodeConfigNXDN configNXDN)
        {
            sb.append("Transmission Mode: ").append(configNXDN.getTransmissionMode()).append("\n");
        }
        sb.append(mNetworkConfigurationMonitor.getSummary()).append("\n");
        return sb.toString();
    }

    @Override
    public void receive(IMessage iMessage)
    {
        if(iMessage instanceof NXDNMessage nxdn && nxdn.isValid())
        {
            if(nxdn instanceof NXDNLayer3Message layer3)
            {
                if(layer3.getMessageType().isControl())
                {
                    processControlLayer3(layer3);
                }
                else
                {
                    processTrafficLayer3(layer3);
                }
            }
            else if(nxdn instanceof Audio nxdnAudio)
            {
                processAudio(nxdnAudio);
            }
            else if(nxdn instanceof SACCHFragment fragment)
            {
//                broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.ACTIVE));
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

//        mTrafficChannelManager.getTalkerAliasManager().enrichMutable(mic);

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
     * Process layer 3 messages from the control channel
     * @param layer3 message to process
     */
    private void processControlLayer3(NXDNLayer3Message layer3)
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.CONTROL));

        switch(layer3.getMessageType())
        {
            case CONTROL_OUT_01_VOICE_CALL_RESPONSE:
                if(layer3 instanceof VoiceCallResponse vcr)
                {
                    //Controller responds to a voice call request with either a channel grant, or this message if
                    //it can't support the call or the called party is not available
                    broadcastEvent(vcr.getIdentifiers(), vcr.getTimestamp(), DecodeEventType.RESPONSE,
                            "VOICE CALL REQUEST - RESPONSE:" + vcr.getCause().toString());
                }
                break;
            case CONTROL_OUT_02_VOICE_CALL_RECEPTION_REQUEST:
                if(layer3 instanceof VoiceCallReceptionRequest vcrr)
                {
                    broadcastEvent(vcrr.getIdentifiers(), vcrr.getTimestamp(), DecodeEventType.PAGE,
                            "VOICE CALL RECEPTION REQUEST");
                }
                break;
            case CONTROL_OUT_03_VOICE_CALL_CONNECTION_RESPONSE:
                if(layer3 instanceof VoiceCallConnectionResponse vccr)
                {
                    //Controller responds to a voice call connection request for the called party with either a channel
                    //grant, or this message if it can't support the call or connect the called party
                    broadcastEvent(vccr.getIdentifiers(), vccr.getTimestamp(), DecodeEventType.RESPONSE,
                            "CALL CONNECTION REQUEST - RESPONSE:" + vccr.getCause().toString());
                }
                break;
            case CONTROL_OUT_04_VOICE_CALL_ASSIGNMENT:
                if(layer3 instanceof VoiceCallAssignment vca)
                {
                    //Decode event is created by the traffic channel manager
                    mTrafficChannelManager.processVoiceCallAssignment(vca);
                }
                break;
            case CONTROL_OUT_05_VOICE_CALL_ASSIGNMENT_DUPLICATE:
                if(layer3 instanceof VoiceCallAssignmentDuplicateControl vcadc)
                {
                    //This informs when there are 2-calls ongoing where a radio can participate in either call
                    mTrafficChannelManager.processVoiceCallAssignment(vcadc);
                }
                break;
            case CONTROL_OUT_09_DATA_CALL_RESPONSE:
                if(layer3 instanceof DataCallResponse dcr)
                {
                    //Controller responds to a data call request with either a channel grant, or this message if
                    //it can't support the call or the called party is not available
                    broadcastEvent(dcr.getIdentifiers(), dcr.getTimestamp(), DecodeEventType.RESPONSE,
                            "DATA CALL REQUEST - RESPONSE:" + dcr.getCause().toString());
                }
                break;
            case CONTROL_OUT_10_DATA_CALL_RECEPTION_REQUEST:
                if(layer3 instanceof DataCallReceptionRequest dcrr)
                {
                    broadcastEvent(dcrr.getIdentifiers(), dcrr.getTimestamp(), DecodeEventType.PAGE,
                            "DATA CALL RECEPTION REQUEST");
                }
                break;
            case CONTROL_OUT_13_DATA_CALL_ASSIGNMENT_DUPLICATE:
                if(layer3 instanceof DataCallAssignmentDuplicateControl dcadc)
                {
                    mTrafficChannelManager.processDataCallAssignment(dcadc);
                }
                break;
            case CONTROL_OUT_14_DATA_CALL_ASSIGNMENT:
                if(layer3 instanceof DataCallAssignment dca)
                {
                    mTrafficChannelManager.processDataCallAssignment(dca);
                }
                break;
            case CONTROL_OUT_16_IDLE:
                break;
            case CONTROL_OUT_17_DISCONNECT:
                //Ignore.  If this happens on the current/active control channel, in a multi-channel configuration,
                //the frequency rotation manager will simply move to the next channel.
                break;

            //Broadcast messages
            case CONTROL_OUT_23_DIGITAL_STATION_ID_INFORMATION:
            case CONTROL_OUT_24_SITE_INFORMATION:
            case CONTROL_OUT_25_SERVICE_INFORMATION:
            case CONTROL_OUT_27_ADJACENT_SITE_INFORMATION:
            case CONTROL_OUT_28_FAILURE_STATUS_INFORMATION:
                mNetworkConfigurationMonitor.process(layer3);
                break;
            case CONTROL_OUT_26_CONTROL_CHANNEL_INFORMATION:
                mNetworkConfigurationMonitor.process(layer3);

                if(layer3 instanceof ControlChannelInformation cci)
                {
                    setCurrentChannel(cci.getChannel1());
                }
                break;
            case CONTROL_OUT_32_REGISTRATION_RESPONSE:
                if(layer3 instanceof RegistrationResponse rr)
                {
                    broadcastEvent(rr.getIdentifiers(), rr.getTimestamp(), DecodeEventType.REGISTER, rr.getCause().toString());
                }
                break;
            case CONTROL_OUT_34_REGISTRATION_CLEAR_RESPONSE:
                if(layer3 instanceof RegistrationClearResponse rcr)
                {
                    broadcastEvent(rcr.getIdentifiers(), rcr.getTimestamp(), DecodeEventType.RESPONSE,
                            "CLEAR REGISTRATION REQUEST - RESPONSE:" + rcr.getCause());
                }
                break;
            case CONTROL_OUT_35_REGISTRATION_COMMAND:
                if(layer3 instanceof RegistrationCommand rc)
                {
                    broadcastEvent(rc.getIdentifiers(), rc.getTimestamp(), DecodeEventType.REGISTER,
                            rc.getRegistrationOption().isPriorityStation() ? "PRIORITY STATION" : "");
                }
                break;
            case CONTROL_OUT_36_GROUP_REGISTRATION_RESPONSE:
                if(layer3 instanceof GroupRegistrationResponse grr)
                {
                    broadcastEvent(grr.getIdentifiers(), grr.getTimestamp(), DecodeEventType.RESPONSE,
                            "GROUP REGISTRATION REQUEST - RESPONSE:" + grr.getCause());
                }
                break;
            case CONTROL_OUT_40_AUTHENTICATION_INQUIRY_REQUEST:
                if(layer3 instanceof AuthenticationInquiryRequest air)
                {
                    broadcastEvent(air.getIdentifiers(), air.getTimestamp(), DecodeEventType.REQUEST,
                            "AUTHENTICATE:" + air.getAuthenticationParameter());
                }
                break;
            case CONTROL_OUT_42_AUTHENTICATION_INQUIRY_REQUEST_MULTI_SYSTEM:
                if(layer3 instanceof AuthenticationInquiryRequest2 air)
                {
                    broadcastEvent(air.getIdentifiers(), air.getTimestamp(), DecodeEventType.REQUEST,
                            "AUTHENTICATE (MULTI-SYSTEM):" + air.getAuthenticationParameter());
                }
                break;
            case CONTROL_OUT_48_STATUS_INQUIRY_REQUEST:
                if(layer3 instanceof StatusInquiryRequest sir)
                {
                    broadcastEvent(sir.getIdentifiers(), sir.getTimestamp(), DecodeEventType.REQUEST,
                            "STATUS INQUIRY");
                }
                break;
            case CONTROL_OUT_49_STATUS_INQUIRY_RESPONSE:
                if(layer3 instanceof StatusInquiryResponse sir)
                {
                    broadcastEvent(sir.getIdentifiers(), sir.getTimestamp(), DecodeEventType.STATUS,
                            "STATUS:" + sir.getStatus() + " CAUSE:" + sir.getCause());
                }
                break;
            case CONTROL_OUT_50_STATUS_REQUEST:
                if(layer3 instanceof StatusRequest sr)
                {
                    broadcastEvent(sr.getIdentifiers(), sr.getTimestamp(), DecodeEventType.REQUEST,
                            "STATUS REQUEST:" + sr.getStatus());
                }
                break;
            case CONTROL_OUT_51_STATUS_RESPONSE:
                if(layer3 instanceof StatusResponse sr)
                {
                    broadcastEvent(sr.getIdentifiers(), sr.getTimestamp(), DecodeEventType.REQUEST,
                            "STATUS RESPONSE:" + sr.getCause());
                }
                break;
            case CONTROL_OUT_52_REMOTE_CONTROL_REQUEST:
                if(layer3 instanceof RemoteControlRequest rcr)
                {
                    broadcastEvent(rcr.getIdentifiers(), rcr.getTimestamp(), DecodeEventType.REQUEST,
                            "REMOTE CONTROL REQUEST - PARAMETERS:" + rcr.getControlParameters());
                }
                break;
            case CONTROL_OUT_53_REMOTE_CONTROL_RESPONSE:
                if(layer3 instanceof RemoteControlResponse rcr)
                {
                    broadcastEvent(rcr.getIdentifiers(), rcr.getTimestamp(), DecodeEventType.REQUEST,
                            "REMOTE CONTROL RESPONSE:" + rcr.getCause());
                }
                break;
            case CONTROL_OUT_54_REMOTE_CONTROL_REQUEST_WITH_ESN:
                if(layer3 instanceof RemoteControlRequestWithESN rcrwe)
                {
                    broadcastEvent(rcrwe.getIdentifiers(), rcrwe.getTimestamp(), DecodeEventType.REQUEST,
                            "REMOTE CONTROL REQUEST WITH ESN - PARAMETERS:" + rcrwe.getAuthenticationParameter());
                }
                break;
            case CONTROL_OUT_56_SHORT_DATA_CALL_REQUEST_HEADER:
                //TODO: implement a short data packet assembler
                break;
            case CONTROL_OUT_57_SHORT_DATA_CALL_REQUEST_USER_DATA:
                //TODO: implement a short data packet assembler
                break;
            case CONTROL_OUT_58_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
                //TODO: implement a short data packet assembler
                break;
            case CONTROL_OUT_59_SHORT_DATA_CALL_RESPONSE:
                break;

            case PROPRIETARY_FORM:
                break;

            case CONTROL_IN_01_VOICE_CALL_REQUEST:
            case CONTROL_IN_02_VOICE_CALL_RECEPTION_RESPONSE:
            case CONTROL_IN_03_VOICE_CALL_CONNECTION_REQUEST:
            case CONTROL_IN_09_DATA_CALL_REQUEST:
            case CONTROL_IN_10_DATA_CALL_RECEPTION_RESPONSE:
            case CONTROL_IN_17_DISCONNECT_REQUEST:
            case CONTROL_IN_32_REGISTRATION_REQUEST:
            case CONTROL_IN_34_REGISTRATION_CLEAR_REQUEST:
            case CONTROL_IN_36_GROUP_REGISTRATION_REQUEST:
            case CONTROL_IN_41_AUTHENTICATION_INQUIRY_RESPONSE:
            case CONTROL_IN_43_AUTHENTICATION_INQUIRY_RESPONSE_MULTI_SYSTEM:
            case CONTROL_IN_48_STATUS_INQUIRY_REQUEST:
            case CONTROL_IN_49_STATUS_INQUIRY_RESPONSE:
            case CONTROL_IN_50_STATUS_REQUEST:
            case CONTROL_IN_51_STATUS_RESPONSE:
            case CONTROL_IN_52_REMOTE_CONTROL_REQUEST:
            case CONTROL_IN_53_REMOTE_CONTROL_RESPONSE:
            case CONTROL_IN_55_REMOTE_CONTROL_RESPONSE_WITH_ESN:
            case CONTROL_IN_56_SHORT_DATA_CALL_REQUEST_HEADER:
            case CONTROL_IN_57_SHORT_DATA_CALL_REQUEST_USER_DATA:
            case CONTROL_IN_58_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
            case CONTROL_IN_59_SHORT_DATA_CALL_RESPONSE:
                break;
            case UNKNOWN:
                break;
        }
    }

    /**
     * Process layer 3 messages from the traffic channel
     * @param layer3 message to process
     */
    private void processTrafficLayer3(NXDNLayer3Message layer3)
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.ACTIVE));

        if(layer3.getMessageType().isBroadcast())
        {
            mNetworkConfigurationMonitor.process(layer3);
        }

        switch(layer3.getMessageType())
        {
            case TRAFFIC_OUT_01_VOICE_CALL:
                if(layer3 instanceof VoiceCall vc)
                {
                }
                break;
            case TRAFFIC_OUT_02_VOICE_CALL_RECEPTION_REQUEST:
                if(layer3 instanceof VoiceCallReceptionRequest vcrr)
                {
                    broadcast(NXDNDecodeEvent.builder(DecodeEventType.PAGE, layer3.getTimestamp())
                            .channel(getCurrentChannel())
                            .identifiers(getMutableIdentifierCollection(vcrr.getIdentifiers()))
                            .details("VOICE CALL RECEPTION REQUEST")
                            .build());
                }
                break;
            case TRAFFIC_OUT_03_VOICE_CALL_INITIALIZATION_VECTOR:
                break;
            case TRAFFIC_OUT_04_VOICE_CALL_ASSIGNMENT:
                if(layer3 instanceof VoiceCallAssignment vca)
                {
                    getIdentifierCollection().update(vca.getIdentifiers());

                    //ICD says this is only on the control channel, but the messages table shows traffic channel also
                    //Decode event is created by the traffic channel manager
                    mTrafficChannelManager.processVoiceCallAssignment(vca);
                }
                break;
            case TRAFFIC_OUT_05_VOICE_CALL_ASSIGNMENT_DUPLICATE:
                if(layer3 instanceof VoiceCallAssignmentDuplicateTraffic vcadt)
                {
                    getIdentifierCollection().update(vcadt.getIdentifiers());

                    //This informs when there are 2-calls ongoing where a radio can participate in either call
                    mTrafficChannelManager.processVoiceCallAssignment(vcadt);
                }
                break;
            case TRAFFIC_OUT_07_TRANSMISSION_RELEASE_EXTENSION:
            case TRAFFIC_OUT_08_TRANSMISSION_RELEASE:
                getIdentifierCollection().remove(IdentifierClass.USER);
                broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.ACTIVE));
                break;
            case TRAFFIC_OUT_09_DATA_CALL_HEADER:
                break;
            case TRAFFIC_OUT_10_DATA_CALL_RECEPTION_REQUEST:
                break;
            case TRAFFIC_OUT_11_DATA_CALL_BLOCK:
                break;
            case TRAFFIC_OUT_12_DATA_CALL_ACKNOWLEDGE:
                break;
            case TRAFFIC_OUT_13_DATA_CALL_ASSIGNMENT_DUPLICATE:
                break;
            case TRAFFIC_OUT_14_DATA_CALL_ASSIGNMENT:
                break;
            case TRAFFIC_OUT_15_HEADER_DELAY:
                break;
            case TRAFFIC_OUT_16_IDLE:
                break;
            case TRAFFIC_OUT_17_DISCONNECT:
                getIdentifierCollection().remove(IdentifierClass.USER);
                 broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.END, State.FADE));
                break;
            case TRAFFIC_OUT_24_SITE_INFORMATION:
                if(layer3 instanceof SiteInformation si)
                {
                    //Update traffic channel manager so it can use this when allocation traffic channels.
                    mTrafficChannelManager.setChannelAccessInformation(si.getChannelAccessInformation());
                }
                break;
            case TRAFFIC_OUT_23_DIGITAL_STATION_ID_INFORMATION:
            case TRAFFIC_OUT_25_SERVICE_INFORMATION:
            case TRAFFIC_OUT_26_CONTROL_CHANNEL_INFORMATION:
            case TRAFFIC_OUT_27_ADJACENT_SITE_INFORMATION:
            case TRAFFIC_OUT_28_FAILURE_STATUS_INFORMATION:
                break;
            case TRAFFIC_OUT_42_AUTHENTICATION_INQUIRY_REQUEST_MULTI_SYSTEM:
                break;
            case TRAFFIC_OUT_43_AUTHORIZATION_INQUIRY_RESPONSE_MULTI_SYSTEM:
                break;
            case TRAFFIC_OUT_48_STATUS_INQUIRY_REQUEST:
                break;
            case TRAFFIC_OUT_49_STATUS_INQUIRY_RESPONSE:
                break;
            case TRAFFIC_OUT_50_STATUS_REQUEST:
                break;
            case TRAFFIC_OUT_51_STATUS_RESPONSE:
                break;
            case TRAFFIC_OUT_52_REMOTE_CONTROL_REQUEST:
                break;
            case TRAFFIC_OUT_53_REMOTE_CONTROL_RESPONSE:
                break;
            case TRAFFIC_OUT_56_SHORT_DATA_CALL_REQUEST_HEADER:
                break;
            case TRAFFIC_OUT_57_SHORT_DATA_CALL_BLOCK:
                break;
            case TRAFFIC_OUT_58_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
                break;
            case TRAFFIC_OUT_59_SHORT_DATA_CALL_RESPONSE:
                break;
            case PROPRIETARY_FORM:
                break;
            case TALKER_ALIAS:
                break;

            case TALKER_ALIAS_COMPLETE:
                //TODO: create a talker alias manager that caches the values and shares them with the channel details panel
                break;


            case TRAFFIC_IN_01_VOICE_CALL:
            case TRAFFIC_IN_02_VOICE_CALL_RECEPTION_RESPONSE:
            case TRAFFIC_IN_03_VOICE_CALL_INITIALIZATION_VECTOR:
            case TRAFFIC_IN_08_TRANSMISSION_RELEASE:
            case TRAFFIC_IN_09_DATA_CALL_HEADER:
            case TRAFFIC_IN_11_DATA_CALL_BLOCK:
            case TRAFFIC_IN_12_DATA_CALL_ACKNOWLEDGE:
            case TRAFFIC_IN_15_HEADER_DELAY:
            case TRAFFIC_IN_16_IDLE:
            case TRAFFIC_IN_17_DISCONNECT_REQUEST:
            case TRAFFIC_IN_42_AUTHENTICATION_INQUIRY_REQUEST_MULTI_SYSTEM:
            case TRAFFIC_IN_43_AUTHORIZATION_INQUIRY_RESPONSE_MULTI_SYSTEM:
            case TRAFFIC_IN_48_STATUS_INQUIRY_REQUEST:
            case TRAFFIC_IN_49_STATUS_INQUIRY_RESPONSE:
            case TRAFFIC_IN_50_STATUS_REQUEST:
            case TRAFFIC_IN_51_STATUS_RESPONSE:
            case TRAFFIC_IN_52_REMOTE_CONTROL_REQUEST:
            case TRAFFIC_IN_53_REMOTE_CONTROL_RESPONSE:
            case TRAFFIC_IN_56_SHORT_DATA_CALL_REQUEST_HEADER:
            case TRAFFIC_IN_57_SHORT_DATA_CALL_BLOCK:
            case TRAFFIC_IN_58_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
            case TRAFFIC_IN_59_SHORT_DATA_CALL_RESPONSE:
                break;
            case UNKNOWN:
                break;

        }
    }

    /**
     * Process an audio message
     * @param audio to process
     */
    private void processAudio(Audio audio)
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.DECODE, State.CALL));
    }
}
