package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;

import java.util.Collections;
import java.util.List;

/**
 * Unknown link control word.
 */
public class UnknownLinkControlWord extends LinkControlWord
{
    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public UnknownLinkControlWord(BinaryMessage message)
    {
        super(message);
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" UNKNOWN/UNRECOGNIZED OPCODE");
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }
}
