package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.lc.standard.AdjacentSiteStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.AdjacentSiteStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.CallAlert;
import io.github.dsheirer.module.decode.p25.message.lc.standard.CallTermination;
import io.github.dsheirer.module.decode.p25.message.lc.standard.EncryptionParameterBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.ExtendedFunctionCommand;
import io.github.dsheirer.module.decode.p25.message.lc.standard.GroupAffiliationQuery;
import io.github.dsheirer.module.decode.p25.message.lc.standard.GroupVoiceChannelUpdate;
import io.github.dsheirer.module.decode.p25.message.lc.standard.GroupVoiceChannelUpdateExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.GroupVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.message.lc.standard.IdentifierUpdate;
import io.github.dsheirer.module.decode.p25.message.lc.standard.IdentifierUpdateExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.MessageUpdate;
import io.github.dsheirer.module.decode.p25.message.lc.standard.NetworkStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.NetworkStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.RFSSStatusBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.RFSSStatusBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.SecondaryControlChannelBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.SecondaryControlChannelBroadcastExplicit;
import io.github.dsheirer.module.decode.p25.message.lc.standard.StatusQuery;
import io.github.dsheirer.module.decode.p25.message.lc.standard.StatusUpdate;
import io.github.dsheirer.module.decode.p25.message.lc.standard.SystemServiceBroadcast;
import io.github.dsheirer.module.decode.p25.message.lc.standard.TelephoneInterconnectAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.lc.standard.TelephoneInterconnectVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.message.lc.standard.UnitAuthenticationCommand;
import io.github.dsheirer.module.decode.p25.message.lc.standard.UnitRegistrationCommand;
import io.github.dsheirer.module.decode.p25.message.lc.standard.UnitToUnitAnswerRequest;
import io.github.dsheirer.module.decode.p25.message.lc.standard.UnitToUnitVoiceChannelUser;
import io.github.dsheirer.module.decode.p25.reference.LinkControlOpcode;

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
                return new AdjacentSiteStatusBroadcast(binaryMessage);
            case ADJACENT_SITE_STATUS_BROADCAST_EXPLICIT:
                return new AdjacentSiteStatusBroadcastExplicit(binaryMessage);
            case CALL_ALERT:
                return new CallAlert(binaryMessage);
            case CALL_TERMINATION_OR_CANCELLATION:
                return new CallTermination(binaryMessage);
            case CHANNEL_IDENTIFIER_UPDATE:
                return new IdentifierUpdate(binaryMessage);
            case CHANNEL_IDENTIFIER_UPDATE_EXPLICIT:
                return new IdentifierUpdateExplicit(binaryMessage);
            case EXTENDED_FUNCTION_COMMAND:
                return new ExtendedFunctionCommand(binaryMessage);
            case GROUP_AFFILIATION_QUERY:
                return new GroupAffiliationQuery(binaryMessage);
            case GROUP_VOICE_CHANNEL_USER:
                return new GroupVoiceChannelUser(binaryMessage);
            case GROUP_VOICE_CHANNEL_UPDATE:
                return new GroupVoiceChannelUpdate(binaryMessage);
            case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                return new GroupVoiceChannelUpdateExplicit(binaryMessage);
            case MESSAGE_UPDATE:
                return new MessageUpdate(binaryMessage);
            case NETWORK_STATUS_BROADCAST:
                return new NetworkStatusBroadcast(binaryMessage);
            case NETWORK_STATUS_BROADCAST_EXPLICIT:
                return new NetworkStatusBroadcastExplicit(binaryMessage);
            case PROTECTION_PARAMETER_BROADCAST:
                return new EncryptionParameterBroadcast(binaryMessage);
            case RFSS_STATUS_BROADCAST:
                return new RFSSStatusBroadcast(binaryMessage);
            case RFSS_STATUS_BROADCAST_EXPLICIT:
                return new RFSSStatusBroadcastExplicit(binaryMessage);
            case SECONDARY_CONTROL_CHANNEL_BROADCAST:
                return new SecondaryControlChannelBroadcast(binaryMessage);
            case SECONDARY_CONTROL_CHANNEL_BROADCAST_EXPLICIT:
                return new SecondaryControlChannelBroadcastExplicit(binaryMessage);
            case STATUS_QUERY:
                return new StatusQuery(binaryMessage);
            case STATUS_UPDATE:
                return new StatusUpdate(binaryMessage);
            case SYSTEM_SERVICE_BROADCAST:
                return new SystemServiceBroadcast(binaryMessage);
            case TELEPHONE_INTERCONNECT_ANSWER_REQUEST:
                return new TelephoneInterconnectAnswerRequest(binaryMessage);
            case TELEPHONE_INTERCONNECT_VOICE_CHANNEL_USER:
                return new TelephoneInterconnectVoiceChannelUser(binaryMessage);
            case UNIT_AUTHENTICATION_COMMAND:
                return new UnitAuthenticationCommand(binaryMessage);
            case UNIT_REGISTRATION_COMMAND:
                return new UnitRegistrationCommand(binaryMessage);
            case UNIT_TO_UNIT_ANSWER_REQUEST:
                return new UnitToUnitAnswerRequest(binaryMessage);
            case UNIT_TO_UNIT_VOICE_CHANNEL_USER:
                return new UnitToUnitVoiceChannelUser(binaryMessage);
            default:
                return new UnknownLinkControlWord(binaryMessage);
        }
    }
}
