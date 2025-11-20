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

package io.github.dsheirer.module.decode.nxdn.layer3;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.AdjacentSiteInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.ControlChannelInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.DigitalStationIDInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.FailureStatusInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.Idle;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.ServiceInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.SiteInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallAcknowledge;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallAssignment;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallAssignmentDuplicateControl;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallAssignmentDuplicateTraffic;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallBlock;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallReceptionRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallReceptionResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DataCallResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Disconnect;
import io.github.dsheirer.module.decode.nxdn.layer3.call.DisconnectRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.HeaderDelay;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlRequestWithESN;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.RemoteControlResponseWithESN;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallBlock;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallRequestHeader;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataCallResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.ShortDataInitializationVector;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusInquiryRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusInquiryResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.StatusResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.TransmissionRelease;
import io.github.dsheirer.module.decode.nxdn.layer3.call.TransmissionReleaseExtension;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCall;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallAssignment;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallAssignmentDuplicateControl;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallAssignmentDuplicateTraffic;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallConnectionRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallConnectionResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallInitializationVector;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallReceptionRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallReceptionResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.call.VoiceCallResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryRequest2;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.AuthenticationInquiryResponse2;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.GroupRegistrationRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.GroupRegistrationResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationClearRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationClearResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationCommand;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationRequest;
import io.github.dsheirer.module.decode.nxdn.layer3.mobility.RegistrationResponse;
import io.github.dsheirer.module.decode.nxdn.layer3.proprietary.ProprietaryForm;
import io.github.dsheirer.module.decode.nxdn.layer3.proprietary.TalkerAlias;

/**
 * Factory for creating NXDN messages
 */
public class NXDNMessageFactory
{
    /**
     * Creates an NXDN message parser for a message on the outbound control channel (CC)
     * @param message content
     * @param timestamp of the message
     * @param ran value
     * @param lich info
     * @return message parser
     */
    public static NXDNLayer3Message get(NXDNMessageType type, CorrectedBinaryMessage message, long timestamp, int ran, LICH lich)
    {
        switch (type)
        {
            case CONTROL_IN_01_VOICE_CALL_REQUEST:
                return new VoiceCallRequest(message, timestamp, type, ran, lich);
            case CONTROL_OUT_01_VOICE_CALL_RESPONSE:
                return new VoiceCallResponse(message, timestamp, type, ran, lich);
            case TRAFFIC_IN_01_VOICE_CALL:
            case TRAFFIC_OUT_01_VOICE_CALL:
                return new VoiceCall(message, timestamp, type, ran, lich);
            case CONTROL_OUT_02_VOICE_CALL_RECEPTION_REQUEST:
            case TRAFFIC_OUT_02_VOICE_CALL_RECEPTION_REQUEST:
                return new VoiceCallReceptionRequest(message, timestamp, type, ran, lich);
            case CONTROL_IN_02_VOICE_CALL_RECEPTION_RESPONSE:
            case TRAFFIC_IN_02_VOICE_CALL_RECEPTION_RESPONSE:
                return new VoiceCallReceptionResponse(message, timestamp, type, ran, lich);
            case CONTROL_IN_03_VOICE_CALL_CONNECTION_REQUEST:
                return new VoiceCallConnectionRequest(message, timestamp, type, ran, lich);
            case CONTROL_OUT_03_VOICE_CALL_CONNECTION_RESPONSE:
                return new VoiceCallConnectionResponse(message, timestamp, type, ran, lich);
            case TRAFFIC_IN_03_VOICE_CALL_INITIALIZATION_VECTOR:
            case TRAFFIC_OUT_03_VOICE_CALL_INITIALIZATION_VECTOR:
                return new VoiceCallInitializationVector(message, timestamp, type, ran, lich);
            case CONTROL_OUT_04_VOICE_CALL_ASSIGNMENT:
            case TRAFFIC_OUT_04_VOICE_CALL_ASSIGNMENT:
                return new VoiceCallAssignment(message, timestamp, type, ran, lich);
            case CONTROL_OUT_05_VOICE_CALL_ASSIGNMENT_DUPLICATE:
                return new VoiceCallAssignmentDuplicateControl(message, timestamp, type, ran, lich);
            case TRAFFIC_OUT_05_VOICE_CALL_ASSIGNMENT_DUPLICATE:
                return new VoiceCallAssignmentDuplicateTraffic(message, timestamp, type, ran, lich);
            case TRAFFIC_OUT_07_TRANSMISSION_RELEASE_EXTENSION:
                return new TransmissionReleaseExtension(message, timestamp, type, ran, lich);
            case TRAFFIC_IN_08_TRANSMISSION_RELEASE:
            case TRAFFIC_OUT_08_TRANSMISSION_RELEASE:
                return new TransmissionRelease(message, timestamp, type, ran, lich);
            case CONTROL_IN_09_DATA_CALL_REQUEST:
                return new DataCallRequest(message, timestamp, type, ran, lich);
            case CONTROL_OUT_09_DATA_CALL_RESPONSE:
                return new DataCallResponse(message, timestamp, type, ran, lich);
            case TRAFFIC_IN_09_DATA_CALL_HEADER:
            case TRAFFIC_OUT_09_DATA_CALL_HEADER:
                return new DataCallHeader(message, timestamp, type, ran, lich);
            case CONTROL_OUT_10_DATA_CALL_RECEPTION_REQUEST:
            case TRAFFIC_OUT_10_DATA_CALL_RECEPTION_REQUEST:
                return new DataCallReceptionRequest(message, timestamp, type, ran, lich);
            case CONTROL_IN_10_DATA_CALL_RECEPTION_RESPONSE:
                return new DataCallReceptionResponse(message, timestamp, type, ran, lich);
            case TRAFFIC_IN_11_DATA_CALL_BLOCK:
            case TRAFFIC_OUT_11_DATA_CALL_BLOCK:
                return new DataCallBlock(message, timestamp, type, ran, lich);
            case TRAFFIC_IN_12_DATA_CALL_ACKNOWLEDGE:
            case TRAFFIC_OUT_12_DATA_CALL_ACKNOWLEDGE:
                return new DataCallAcknowledge(message, timestamp, type, ran, lich);
            case CONTROL_OUT_13_DATA_CALL_ASSIGNMENT_DUPLICATE:
                return new DataCallAssignmentDuplicateControl(message, timestamp, type, ran, lich);
            case TRAFFIC_OUT_13_DATA_CALL_ASSIGNMENT_DUPLICATE:
                return new DataCallAssignmentDuplicateTraffic(message, timestamp, type, ran, lich);
            case CONTROL_OUT_14_DATA_CALL_ASSIGNMENT:
            case TRAFFIC_OUT_14_DATA_CALL_ASSIGNMENT:
                return new DataCallAssignment(message, timestamp, type, ran, lich);
            case TRAFFIC_OUT_15_HEADER_DELAY:
            case TRAFFIC_IN_15_HEADER_DELAY:
                return new HeaderDelay(message, timestamp, type, ran, lich);
            case CONTROL_OUT_16_IDLE:
            case TRAFFIC_OUT_16_IDLE:
                return new Idle(message, timestamp, type, ran, lich);
            case CONTROL_OUT_17_DISCONNECT:
            case TRAFFIC_OUT_17_DISCONNECT:
                return new Disconnect(message, timestamp, type, ran, lich);
            case CONTROL_IN_17_DISCONNECT_REQUEST:
            case TRAFFIC_IN_17_DISCONNECT_REQUEST:
                return new DisconnectRequest(message, timestamp, type, ran, lich);

            //Broadcast Messages
            case CONTROL_OUT_23_DIGITAL_STATION_ID_INFORMATION:
            case TRAFFIC_OUT_23_DIGITAL_STATION_ID_INFORMATION:
                return new DigitalStationIDInformation(message, timestamp, type, ran, lich);
            case CONTROL_OUT_24_SITE_INFORMATION:
            case TRAFFIC_OUT_24_SITE_INFORMATION:
                return new SiteInformation(message, timestamp, type, ran, lich);
            case CONTROL_OUT_25_SERVICE_INFORMATION:
            case TRAFFIC_OUT_25_SERVICE_INFORMATION:
                return new ServiceInformation(message, timestamp, type, ran, lich);
            case CONTROL_OUT_26_CONTROL_CHANNEL_INFORMATION:
            case TRAFFIC_OUT_26_CONTROL_CHANNEL_INFORMATION:
                return new ControlChannelInformation(message, timestamp, type, ran, lich);
            case CONTROL_OUT_27_ADJACENT_SITE_INFORMATION:
            case TRAFFIC_OUT_27_ADJACENT_SITE_INFORMATION:
                return new AdjacentSiteInformation(message, timestamp, type, ran, lich);
            case CONTROL_OUT_28_FAILURE_STATUS_INFORMATION:
            case TRAFFIC_OUT_28_FAILURE_STATUS_INFORMATION:
                return new FailureStatusInformation(message, timestamp, type, ran, lich);

            //Mobility Management Messages
            case CONTROL_IN_32_REGISTRATION_REQUEST:
                return new RegistrationRequest(message, timestamp, type, ran, lich);
            case CONTROL_OUT_32_REGISTRATION_RESPONSE:
                return new RegistrationResponse(message, timestamp, type, ran, lich);
            case CONTROL_IN_34_REGISTRATION_CLEAR_REQUEST:
                return new RegistrationClearRequest(message, timestamp, type, ran, lich);
            case CONTROL_OUT_34_REGISTRATION_CLEAR_RESPONSE:
                return new RegistrationClearResponse(message, timestamp, type, ran, lich);
            case CONTROL_OUT_35_REGISTRATION_COMMAND:
                return new RegistrationCommand(message, timestamp, type, ran, lich);
            case CONTROL_IN_36_GROUP_REGISTRATION_REQUEST:
                return new GroupRegistrationRequest(message, timestamp, type, ran, lich);
            case CONTROL_OUT_36_GROUP_REGISTRATION_RESPONSE:
                return new GroupRegistrationResponse(message, timestamp, type, ran, lich);
            case CONTROL_OUT_40_AUTHENTICATION_INQUIRY_REQUEST:
                return new AuthenticationInquiryRequest(message, timestamp, type, ran, lich);
            case CONTROL_IN_41_AUTHENTICATION_INQUIRY_RESPONSE:
                return new AuthenticationInquiryResponse(message, timestamp, type, ran, lich);
            case CONTROL_OUT_42_AUTHENTICATION_INQUIRY_REQUEST_MULTI_SYSTEM:
            case TRAFFIC_IN_42_AUTHENTICATION_INQUIRY_REQUEST_MULTI_SYSTEM:
            case TRAFFIC_OUT_42_AUTHENTICATION_INQUIRY_REQUEST_MULTI_SYSTEM:
                return new AuthenticationInquiryRequest2(message, timestamp, type, ran, lich);
            case CONTROL_IN_43_AUTHENTICATION_INQUIRY_RESPONSE_MULTI_SYSTEM:
            case TRAFFIC_IN_43_AUTHORIZATION_INQUIRY_RESPONSE_MULTI_SYSTEM:
            case TRAFFIC_OUT_43_AUTHORIZATION_INQUIRY_RESPONSE_MULTI_SYSTEM:
                return new AuthenticationInquiryResponse2(message, timestamp, type, ran, lich);

            case CONTROL_IN_48_STATUS_INQUIRY_REQUEST:
            case CONTROL_OUT_48_STATUS_INQUIRY_REQUEST:
            case TRAFFIC_IN_48_STATUS_INQUIRY_REQUEST:
            case TRAFFIC_OUT_48_STATUS_INQUIRY_REQUEST:
                return new StatusInquiryRequest(message, timestamp, type, ran, lich);
            case CONTROL_IN_49_STATUS_INQUIRY_RESPONSE:
            case CONTROL_OUT_49_STATUS_INQUIRY_RESPONSE:
            case TRAFFIC_IN_49_STATUS_INQUIRY_RESPONSE:
            case TRAFFIC_OUT_49_STATUS_INQUIRY_RESPONSE:
                return new StatusInquiryResponse(message, timestamp, type, ran, lich);
            case CONTROL_IN_50_STATUS_REQUEST:
            case CONTROL_OUT_50_STATUS_REQUEST:
            case TRAFFIC_IN_50_STATUS_REQUEST:
            case TRAFFIC_OUT_50_STATUS_REQUEST:
                return new StatusRequest(message, timestamp, type, ran, lich);
            case CONTROL_IN_51_STATUS_RESPONSE:
            case CONTROL_OUT_51_STATUS_RESPONSE:
            case TRAFFIC_IN_51_STATUS_RESPONSE:
            case TRAFFIC_OUT_51_STATUS_RESPONSE:
                return new StatusResponse(message, timestamp, type, ran, lich);
            case CONTROL_IN_52_REMOTE_CONTROL_REQUEST:
            case CONTROL_OUT_52_REMOTE_CONTROL_REQUEST:
            case TRAFFIC_IN_52_REMOTE_CONTROL_REQUEST:
            case TRAFFIC_OUT_52_REMOTE_CONTROL_REQUEST:
                return new RemoteControlRequest(message, timestamp, type, ran, lich);
            case CONTROL_IN_53_REMOTE_CONTROL_RESPONSE:
            case CONTROL_OUT_53_REMOTE_CONTROL_RESPONSE:
            case TRAFFIC_IN_53_REMOTE_CONTROL_RESPONSE:
            case TRAFFIC_OUT_53_REMOTE_CONTROL_RESPONSE:
                return new RemoteControlResponse(message, timestamp, type, ran, lich);
            case CONTROL_OUT_54_REMOTE_CONTROL_REQUEST_WITH_ESN:
                return new RemoteControlRequestWithESN(message, timestamp, type, ran, lich);
            case CONTROL_IN_55_REMOTE_CONTROL_RESPONSE_WITH_ESN:
                return new RemoteControlResponseWithESN(message, timestamp, type, ran, lich);
            case TRAFFIC_IN_56_SHORT_DATA_CALL_REQUEST_HEADER:
            case TRAFFIC_OUT_56_SHORT_DATA_CALL_REQUEST_HEADER:
                return  new ShortDataCallRequestHeader(message, timestamp, type, ran, lich);
            case TRAFFIC_IN_57_SHORT_DATA_CALL_BLOCK:
            case TRAFFIC_OUT_57_SHORT_DATA_CALL_BLOCK:
                return new ShortDataCallBlock(message, timestamp, type, ran, lich);
            case TRAFFIC_OUT_58_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
            case TRAFFIC_IN_58_SHORT_DATA_CALL_INITIALIZATION_VECTOR:
                return new ShortDataInitializationVector(message, timestamp, type, ran, lich);
            case TRAFFIC_OUT_59_SHORT_DATA_CALL_RESPONSE:
            case TRAFFIC_IN_59_SHORT_DATA_CALL_RESPONSE:
                return new ShortDataCallResponse(message, timestamp, type, ran, lich);

            case PROPRIETARY_FORM:
                return new ProprietaryForm(message, timestamp, type, ran, lich);
            case TALKER_ALIAS:
                return new TalkerAlias(message, timestamp, type, ran, lich);
        }

        return new UnknownMessage(message, timestamp, type, ran, lich);
    }
}
