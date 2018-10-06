/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Message factory for P25 Logical Link Data Unit 1 messages that contain an embedded Link Control message.
 */
public class LDULCMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(LDULCMessageFactory.class);

    //TODO: create a static method to determine the LDULC type and construct the message accordingly, instead
    //TODO: of creating a generic LDU1 message first
    //TODO: update constructor to accept nac and timestamp
    public static P25Message create(DataUnitID dataUnitID, int nac, long timestamp,
                                    CorrectedBinaryMessage correctedBinaryMessage)
    {
        LDU1Message lduMesssage = new LDU1Message(correctedBinaryMessage, dataUnitID, null);
        return getMessage(lduMesssage);
    }

    /**
     * Constructs a new sub-class of the TDULC message if the opcode is recognized
     * or returns the original message.
     */
    public static LDU1Message getMessage(LDU1Message message)
    {
        if(message.isImplicitFormat() || message.getVendor() == Vendor.STANDARD)
        {
            switch(message.getOpcode())
            {
                case ADJACENT_SITE_STATUS_BROADCAST:
                    return new AdjacentSiteStatusBroadcast(message);
                case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
                    return new AdjacentSiteStatusBroadcastExplicit(message);
                case CALL_ALERT:
                    return new CallAlert(message);
                case CALL_TERMINATION_OR_CANCELLATION:
                    return new CallTermination(message);
                case CHANNEL_IDENTIFIER_UPDATE:
                    return new ChannelIdentifierUpdate(message);
                case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
                    return new ChannelIdentifierUpdateExplicit(message);
                case EXTENDED_FUNCTION_COMMAND:
                    return new ExtendedFunctionCommand(message);
                case GROUP_AFFILIATION_QUERY:
                    return new GroupAffiliationQuery(message);
                case GROUP_VOICE_CHANNEL_UPDATE:
                    return new GroupVoiceChannelUpdate(message);
                case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                    return new GroupVoiceChannelUpdateExplicit(message);
                case GROUP_VOICE_CHANNEL_USER:
                    return new GroupVoiceChannelUser(message);
                case MESSAGE_UPDATE:
                    return new MessageUpdate(message);
                case NETWORK_STATUS_BROADCAST:
                    return new NetworkStatusBroadcast(message);
                case NETWORK_STATUS_BROADCAST_EXPLICIT:
                    return new NetworkStatusBroadcastExplicit(message);
                case PROTECTION_PARAMETER_BROADCAST:
                    return new ProtectionParameterBroadcast(message);
                case RFSS_STATUS_BROADCAST:
                    return new RFSSStatusBroadcast(message);
                case RFSS_STATUS_BROADCAST_EXPLICIT:
                    return new RFSSStatusBroadcastExplicit(message);
                case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                    return new SecondaryControlChannelBroadcast(message);
                case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                    return new SecondaryControlChannelBroadcastExplicit(message);
                case STATUS_QUERY:
                    return new StatusQuery(message);
                case STATUS_UPDATE:
                    return new StatusUpdate(message);
                case SYSTEM_SERVICE_BROADCAST:
                    return new SystemServiceBroadcast(message);
                case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                    return new TelephoneInterconnectAnswerRequest(message);
                case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                    return new TelephoneInterconnectVoiceChannelUser(message);
                case UNIT_AUTHENTICATION_COMMAND:
                    return new UnitAuthenticationCommand(message);
                case UNIT_REGISTRATION_COMMAND:
                    return new UnitRegistrationCommand(message);
                case UNIT_TO_UNIT_ANSWER_REQUEST:
                    return new UnitToUnitAnswerRequest(message);
                case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                    return new UnitToUnitVoiceChannelUser(message);
                default:
                    break;
            }
        }
        else
        {
            switch(message.getVendor())
            {
                default:
                    break;
            }
        }

        return message;
    }
}
