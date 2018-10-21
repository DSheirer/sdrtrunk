package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
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
