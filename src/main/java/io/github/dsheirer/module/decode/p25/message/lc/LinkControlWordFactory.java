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
     * @param correctedBinaryMessage containing the LCW binary message sequence.
     */
    public static LinkControlWord create(BinaryMessage correctedBinaryMessage)
    {
        LinkControlOpcode opcode = LinkControlWord.getOpcode(correctedBinaryMessage);

        switch(opcode)
        {
            default:
                return new LinkControlWord(correctedBinaryMessage);
        }
    }
}
