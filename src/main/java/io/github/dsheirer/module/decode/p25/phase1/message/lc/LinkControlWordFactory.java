/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisReturnToControlChannel;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasBlock1;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasBlock2;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasBlock3;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerAliasBlock4;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerGPSBlock1;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.LCHarrisTalkerGPSBlock2;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaEmergencyAlarmActivation;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaFailsoft;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaGroupGroupDelete;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaGroupRegroupAdd;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaGroupRegroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaGroupRegroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaTalkComplete;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaTalkerAliasDataBlock;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaTalkerAliasHeader;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaUnitGPS;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaUnknownOpcode;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCAdjacentSiteStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCAdjacentSiteStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCCallAlert;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCCallTermination;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCChannelIdentifierUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCChannelIdentifierUpdateVU;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCConventionalFallback;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCExtendedFunctionCommandExtended;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupAffiliationQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUpdateExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCGroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCMessageUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCMessageUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCNetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCNetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCProtectionParameterBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCRFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCRFSSStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSourceIDExtension;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusQuery;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusUpdate;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCStatusUpdateExtended;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSystemServiceBroadcast;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCTelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCTelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitAuthenticationCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitRegistrationCommand;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCUnitToUnitVoiceChannelUserExtended;

/**
 * Factory class for creating link control word (LCW) message parsers.
 */
public class LinkControlWordFactory
{
    /**
     * Creates a link control word from the binary message sequence.
     *
     * @param message containing the LCW binary message sequence.
     * @param timestamp of the message that carries the link control word.
     * @param isTerminator to indicate if message is carried by a TDULC terminator message
     */
    public static LinkControlWord create(CorrectedBinaryMessage message, long timestamp, boolean isTerminator)
    {
        LinkControlOpcode opcode = LinkControlWord.getOpcode(message);
        switch(opcode)
        {
            case ADJACENT_SITE_STATUS_BROADCAST:
                return new LCAdjacentSiteStatusBroadcast(message);
            case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
                return new LCAdjacentSiteStatusBroadcastExplicit(message);
            case CALL_ALERT:
                return new LCCallAlert(message);
            case CALL_TERMINATION_OR_CANCELLATION:
                return new LCCallTermination(message);
            case CHANNEL_IDENTIFIER_UPDATE:
                return new LCChannelIdentifierUpdate(message);
            case CHANNEL_IDENTIFIER_UPDATE_VU:
                return new LCChannelIdentifierUpdateVU(message);
            case CONVENTIONAL_FALLBACK_INDICATION:
                return new LCConventionalFallback(message);
            case EXTENDED_FUNCTION_COMMAND:
                return new LCExtendedFunctionCommand(message);
            case EXTENDED_FUNCTION_COMMAND_EXTENDED:
                return new LCExtendedFunctionCommandExtended(message, timestamp, isTerminator);
            case GROUP_AFFILIATION_QUERY:
                return new LCGroupAffiliationQuery(message);
            case GROUP_VOICE_CHANNEL_USER:
                return new LCGroupVoiceChannelUser(message, timestamp, isTerminator);
            case GROUP_VOICE_CHANNEL_UPDATE:
                return new LCGroupVoiceChannelUpdate(message);
            case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                return new LCGroupVoiceChannelUpdateExplicit(message);
            case MESSAGE_UPDATE:
                return new LCMessageUpdate(message);
            case MESSAGE_UPDATE_EXTENDED:
                return new LCMessageUpdateExtended(message, timestamp, isTerminator);
            case NETWORK_STATUS_BROADCAST:
                return new LCNetworkStatusBroadcast(message);
            case NETWORK_STATUS_BROADCAST_EXPLICIT:
                return new LCNetworkStatusBroadcastExplicit(message);
            case PROTECTION_PARAMETER_BROADCAST:
                return new LCProtectionParameterBroadcast(message);
            case RFSS_STATUS_BROADCAST:
                return new LCRFSSStatusBroadcast(message);
            case RFSS_STATUS_BROADCAST_EXPLICIT:
                return new LCRFSSStatusBroadcastExplicit(message);
            case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                return new LCSecondaryControlChannelBroadcast(message);
            case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                return new LCSecondaryControlChannelBroadcastExplicit(message);
            case SOURCE_ID_EXTENSION:
                return new LCSourceIDExtension(message, timestamp, isTerminator);
            case STATUS_QUERY:
                return new LCStatusQuery(message);
            case STATUS_UPDATE:
                return new LCStatusUpdate(message);
            case STATUS_UPDATE_EXTENDED:
                return new LCStatusUpdateExtended(message, timestamp, isTerminator);
            case SYSTEM_SERVICE_BROADCAST:
                return new LCSystemServiceBroadcast(message);
            case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                return new LCTelephoneInterconnectAnswerRequest(message);
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                return new LCTelephoneInterconnectVoiceChannelUser(message);
            case UNIT_AUTHENTICATION_COMMAND:
                return new LCUnitAuthenticationCommand(message);
            case UNIT_REGISTRATION_COMMAND:
                return new LCUnitRegistrationCommand(message);
            case UNIT_TO_UNIT_ANSWER_REQUEST:
                return new LCUnitToUnitAnswerRequest(message);
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                return new LCUnitToUnitVoiceChannelUser(message);
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER_EXTENDED:
                return new LCUnitToUnitVoiceChannelUserExtended(message, timestamp, isTerminator);

            case L3HARRIS_RETURN_TO_CONTROL_CHANNEL:
                return new LCHarrisReturnToControlChannel(message);
            case L3HARRIS_TALKER_GPS_BLOCK1:
                return new LCHarrisTalkerGPSBlock1(message);
            case L3HARRIS_TALKER_GPS_BLOCK2:
                return new LCHarrisTalkerGPSBlock2(message);
            case L3HARRIS_TALKER_ALIAS_BLOCK_1:
                return new LCHarrisTalkerAliasBlock1(message);
            case L3HARRIS_TALKER_ALIAS_BLOCK_2:
                return new LCHarrisTalkerAliasBlock2(message);
            case L3HARRIS_TALKER_ALIAS_BLOCK_3:
                return new LCHarrisTalkerAliasBlock3(message);
            case L3HARRIS_TALKER_ALIAS_BLOCK_4:
                return new LCHarrisTalkerAliasBlock4(message);
            case L3HARRIS_UNKNOWN:
                return new UnknownLinkControlWord(message);

            case MOTOROLA_FAILSOFT:
                return new LCMotorolaFailsoft(message);
            case MOTOROLA_GROUP_REGROUP_ADD:
                return new LCMotorolaGroupRegroupAdd(message);
            case MOTOROLA_GROUP_REGROUP_DELETE:
                return new LCMotorolaGroupGroupDelete(message);
            case MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_USER:
                return new LCMotorolaGroupRegroupVoiceChannelUser(message);
            case MOTOROLA_TALK_COMPLETE:
                return new LCMotorolaTalkComplete(message);
            case MOTOROLA_GROUP_REGROUP_VOICE_CHANNEL_UPDATE:
                return new LCMotorolaGroupRegroupVoiceChannelUpdate(message);
            case MOTOROLA_UNIT_GPS:
                return new LCMotorolaUnitGPS(message);
            case MOTOROLA_TALKER_ALIAS_HEADER:
                return new LCMotorolaTalkerAliasHeader(message);
            case MOTOROLA_TALKER_ALIAS_DATA_BLOCK:
                return new LCMotorolaTalkerAliasDataBlock(message);
            case MOTOROLA_EMERGENCY_ALARM_ACTIVATION:
                return new LCMotorolaEmergencyAlarmActivation(message);
            case MOTOROLA_UNKNOWN:
                return new LCMotorolaUnknownOpcode(message);

            default:
                return new UnknownLinkControlWord(message);
        }
    }
}
