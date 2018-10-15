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
            case CALL_TERMINATION_OR_CANCELLATION:
                return new CallTermination(binaryMessage);
            case GROUP_AFFILIATION_QUERY:
                return new GroupAffiliationQuery(binaryMessage);
            case GROUP_VOICE_CHANNEL_USER:
                return new GroupVoiceChannelUser(binaryMessage);
            case GROUP_VOICE_CHANNEL_UPDATE:
                return new GroupVoiceChannelUpdate(binaryMessage);
            case GROUP_VOICE_CHANNEL_UPDATE_EXPLICIT:
                return new GroupVoiceChannelUpdateExplicit(binaryMessage);
            case STATUS_QUERY:
                return new StatusQuery(binaryMessage);
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
