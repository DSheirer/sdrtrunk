/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisUnknownOpcode10;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisUnknownOpcode42;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisUnknownOpcode43;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaPatchGroupAdd;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaPatchGroupDelete;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaPatchGroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaPatchGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaTalkComplete;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaUnitGPS;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaUnknownOpcode;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCAdjacentSiteStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCAdjacentSiteStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCCallAlert;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCCallTermination;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCFrequencyBandUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCFrequencyBandUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupAffiliationQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCMessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCNetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCProtectionParameterBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCRFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCRFSSStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSystemServiceBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCTelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCTelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitAuthenticationCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitRegistrationCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitVoiceChannelUser;

/**
 * Factory class for creating link control word (LCW) message parsers.
 */
public class LinkControlWordFactory
{
    /**
     * Creates a link control word from the binary message sequence.
     *
     * @param binaryMessage containing the LCW binary message sequence.
     */
    public static LinkControlWord create(BinaryMessage binaryMessage)
    {
        LinkControlOpcode opcode = LinkControlWord.getOpcode(binaryMessage);
        switch(opcode)
        {
            case ADJACENT_SITE_STATUS_BROADCAST:
                return new LCAdjacentSiteStatusBroadcast(binaryMessage);
            case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
                return new LCAdjacentSiteStatusBroadcastExplicit(binaryMessage);
            case CALL_ALERT:
                return new LCCallAlert(binaryMessage);
            case CALL_TERMINATION_OR_CANCELLATION:
                return new LCCallTermination(binaryMessage);
            case CHANNEL_IDENTIFIER_UPDATE:
                return new LCFrequencyBandUpdate(binaryMessage);
            case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
                return new LCFrequencyBandUpdateExplicit(binaryMessage);
            case EXTENDED_FUNCTION_COMMAND:
                return new LCExtendedFunctionCommand(binaryMessage);
            case GROUP_AFFILIATION_QUERY:
                return new LCGroupAffiliationQuery(binaryMessage);
            case GROUP_VOICE_CHANNEL_USER:
                return new LCGroupVoiceChannelUser(binaryMessage);
            case GROUP_VOICE_CHANNEL_UPDATE:
                return new LCGroupVoiceChannelUpdate(binaryMessage);
            case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                return new LCGroupVoiceChannelUpdateExplicit(binaryMessage);
            case MESSAGE_UPDATE:
                return new LCMessageUpdate(binaryMessage);
            case NETWORK_STATUS_BROADCAST:
                return new LCNetworkStatusBroadcast(binaryMessage);
            case NETWORK_STATUS_BROADCAST_EXPLICIT:
                return new LCNetworkStatusBroadcastExplicit(binaryMessage);
            case PROTECTION_PARAMETER_BROADCAST:
                return new LCProtectionParameterBroadcast(binaryMessage);
            case RFSS_STATUS_BROADCAST:
                return new LCRFSSStatusBroadcast(binaryMessage);
            case RFSS_STATUS_BROADCAST_EXPLICIT:
                return new LCRFSSStatusBroadcastExplicit(binaryMessage);
            case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                return new LCSecondaryControlChannelBroadcast(binaryMessage);
            case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                return new LCSecondaryControlChannelBroadcastExplicit(binaryMessage);
            case STATUS_QUERY:
                return new LCStatusQuery(binaryMessage);
            case STATUS_UPDATE:
                return new LCStatusUpdate(binaryMessage);
            case SYSTEM_SERVICE_BROADCAST:
                return new LCSystemServiceBroadcast(binaryMessage);
            case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                return new LCTelephoneInterconnectAnswerRequest(binaryMessage);
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                return new LCTelephoneInterconnectVoiceChannelUser(binaryMessage);
            case UNIT_AUTHENTICATION_COMMAND:
                return new LCUnitAuthenticationCommand(binaryMessage);
            case UNIT_REGISTRATION_COMMAND:
                return new LCUnitRegistrationCommand(binaryMessage);
            case UNIT_TO_UNIT_ANSWER_REQUEST:
                return new LCUnitToUnitAnswerRequest(binaryMessage);
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                return new LCUnitToUnitVoiceChannelUser(binaryMessage);

            case L3HARRIS_UNKNOWN_0A:
                return new LCHarrisUnknownOpcode10(binaryMessage);
            case L3HARRIS_UNKNOWN_2A:
                return new LCHarrisUnknownOpcode42(binaryMessage);
            case L3HARRIS_UNKNOWN_2B:
                return new LCHarrisUnknownOpcode43(binaryMessage);
            case L3HARRIS_UNKNOWN:
                return new UnknownLinkControlWord(binaryMessage);

            case MOTOROLA_PATCH_GROUP_ADD:
                return new LCMotorolaPatchGroupAdd(binaryMessage);
            case MOTOROLA_PATCH_GROUP_DELETE:
                return new LCMotorolaPatchGroupDelete(binaryMessage);
            case MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_USER:
                return new LCMotorolaPatchGroupVoiceChannelUser(binaryMessage);
            case MOTOROLA_TALK_COMPLETE:
                return new LCMotorolaTalkComplete(binaryMessage);
            case MOTOROLA_PATCH_GROUP_VOICE_CHANNEL_UPDATE:
                return new LCMotorolaPatchGroupVoiceChannelUpdate(binaryMessage);
            case MOTOROLA_UNIT_GPS:
                return new LCMotorolaUnitGPS(binaryMessage);
            case MOTOROLA_UNKNOWN:
                return new LCMotorolaUnknownOpcode(binaryMessage);

            default:
                return new UnknownLinkControlWord(binaryMessage);
        }
    }
}
