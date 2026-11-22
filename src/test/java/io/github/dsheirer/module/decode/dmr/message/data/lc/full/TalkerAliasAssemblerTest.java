package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * Tests assembly of talker alias fragments, using Motorola Capacity Max (FID 0x10, FLCO 20-23) link control.
 */
public class TalkerAliasAssemblerTest
{
    private static final int FLCO_HEADER = 20;
    private static final int FID_MOTOROLA = 0x10;
    private static final int FORMAT_UTF8 = 2;

    /**
     * Creates a 72-bit link control message (minus CRC) carrying the 7 octets that follow the FLCO and FID.
     */
    private static FullLCMessage message(int flco, byte[] payload)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(96);
        message.load(2, 6, flco);
        message.load(8, 8, FID_MOTOROLA);

        for(int x = 0; x < 7; x++)
        {
            message.load(16 + (x * 8), 8, payload[x] & 0xFF);
        }

        return switch(flco - FLCO_HEADER)
        {
            case 0 -> new TalkerAliasHeader(message, 0, 1);
            case 1 -> new TalkerAliasBlock1(message, 0, 1);
            case 2 -> new TalkerAliasBlock2(message, 0, 1);
            default -> new TalkerAliasBlock3(message, 0, 1);
        };
    }

    /**
     * Fragments the alias into the header and three blocks, padded with nulls.
     */
    private static FullLCMessage[] fragment(String alias, int length)
    {
        ByteBuffer buffer = ByteBuffer.allocate(28);
        buffer.put((byte)((FORMAT_UTF8 << 6) | (length << 1)));
        buffer.put(alias.getBytes(StandardCharsets.UTF_8));
        byte[] bytes = buffer.array();

        FullLCMessage[] messages = new FullLCMessage[4];

        for(int x = 0; x < 4; x++)
        {
            byte[] payload = new byte[7];
            System.arraycopy(bytes, x * 7, payload, 0, 7);
            messages[x] = message(FLCO_HEADER + x, payload);
        }

        return messages;
    }

    private static TalkerAliasComplete assemble(String alias, int length)
    {
        TalkerAliasAssembler assembler = new TalkerAliasAssembler();
        TalkerAliasComplete complete = null;

        for(FullLCMessage message : fragment(alias, length))
        {
            assertTrue(message.getOpcode().isTalkerAliasOpcode(), "Talker alias opcode: " + message.getOpcode());
            TalkerAliasComplete latest = assembler.process(message);
            complete = latest != null ? latest : complete;
        }

        return complete;
    }

    @Test
    public void utf8LengthInCharacters()
    {
        assertEquals("ТЕРНАВА", assemble("ТЕРНАВА", 7).getAliasValue());
        assertEquals("САЛАМАНДРА", assemble("САЛАМАНДРА", 10).getAliasValue());
    }

    @Test
    public void utf8LengthInBytes()
    {
        assertEquals("ТЕРНАВА", assemble("ТЕРНАВА", 14).getAliasValue());
        assertEquals("САЛАМАНДРА", assemble("САЛАМАНДРА", 20).getAliasValue());
    }

    @Test
    public void asciiInUtf8Format()
    {
        assertEquals("DISPATCH 1", assemble("DISPATCH 1", 10).getAliasValue());
    }

    @Test
    public void partialCharacterIsNotDisplayed()
    {
        TalkerAliasAssembler assembler = new TalkerAliasAssembler();
        FullLCMessage[] messages = fragment("САЛАМАНДРА", 10);
        assembler.process(messages[0]);
        TalkerAliasComplete partial = assembler.process(messages[1]);
        assertEquals("САЛАМА", partial.getAliasValue());
        assertFalse(partial.getAliasValue().contains("\uFFFD"));
    }
}
