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

package io.github.dsheirer.module.decode.dmr.message.data.lc.shorty;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.CRCDMR;
import io.github.dsheirer.edac.Hamming17;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the short link control assembler, which reassembles 4 CACH fragments into a BPTC(68,36) protected
 * short link control message with a CRC-8 check.
 */
public class SLCAssemblerTest
{
    private static final int ROW_LENGTH = 17;
    private static final int ROW_INFORMATION_LENGTH = 12;
    private static final List<Integer> ROW_CODEWORDS = new ArrayList<>();

    static
    {
        //Hamming(17,12) codewords where bit i of the codeword is message bit i
        for(int word = 0; word < (1 << ROW_LENGTH); word++)
        {
            BinaryMessage message = new BinaryMessage(ROW_LENGTH);

            for(int i = 0; i < ROW_LENGTH; i++)
            {
                if((word & (1 << i)) != 0)
                {
                    message.set(i);
                }
            }

            if(Hamming17.getSyndrome(message, 0) == 0)
            {
                ROW_CODEWORDS.add(word);
            }
        }
    }

    /**
     * Finds the systematic row codeword whose information bits (the low 12 bits) match the information value.
     */
    private static int encodeRow(int information)
    {
        for(int codeword: ROW_CODEWORDS)
        {
            if((codeword & ((1 << ROW_INFORMATION_LENGTH) - 1)) == information)
            {
                return codeword;
            }
        }

        throw new IllegalStateException("No codeword for information value " + information);
    }

    /**
     * Creates a 36-bit short link control payload of 28 random data bits followed by the CRC-8.
     * @param random for the data bits
     * @param validCRC true to append the correct CRC-8, or false to append a CRC-8 with one bit inverted.
     */
    private static BinaryMessage createPayload(Random random, boolean validCRC)
    {
        BinaryMessage payload = new BinaryMessage(36);

        for(int x = 0; x < 28; x++)
        {
            if(random.nextBoolean())
            {
                payload.set(x);
            }
        }

        int crc = CRCDMR.crc8(payload, 28);

        if(!validCRC)
        {
            crc ^= 1;
        }

        for(int x = 0; x < 8; x++)
        {
            if((crc & (1 << (7 - x))) != 0)
            {
                payload.set(28 + x);
            }
        }

        return payload;
    }

    /**
     * Encodes the 36-bit payload as a BPTC(68,36) message in transmitted (interleaved) bit order: 3 Hamming(17,12)
     * rows carrying 12 payload bits each, followed by an even column parity row.
     */
    private static BinaryMessage encode(BinaryMessage payload)
    {
        BinaryMessage deinterleaved = new BinaryMessage(68);
        int parityRow = 0;

        for(int row = 0; row < 4; row++)
        {
            int rowWord;

            if(row < 3)
            {
                int information = 0;

                for(int column = 0; column < ROW_INFORMATION_LENGTH; column++)
                {
                    if(payload.get(row * ROW_INFORMATION_LENGTH + column))
                    {
                        information |= (1 << column);
                    }
                }

                rowWord = encodeRow(information);
                parityRow ^= rowWord;
            }
            else
            {
                rowWord = parityRow;
            }

            for(int column = 0; column < ROW_LENGTH; column++)
            {
                if((rowWord & (1 << column)) != 0)
                {
                    deinterleaved.set(row * ROW_LENGTH + column);
                }
            }
        }

        //De-interleave maps deinterleaved[(index * 17) % 67] = interleaved[index] (index 67 is not interleaved)
        BinaryMessage interleaved = new BinaryMessage(68);

        for(int index = 0; index < 67; index++)
        {
            if(deinterleaved.get((index * 17) % 67))
            {
                interleaved.set(index);
            }
        }

        if(deinterleaved.get(67))
        {
            interleaved.set(67);
        }

        return interleaved;
    }

    /**
     * Splits the interleaved message into 4 CACH fragments and processes them through the assembler.
     * @return the short link control message dispatched with the last fragment.
     */
    private static ShortLCMessage assemble(BinaryMessage interleaved)
    {
        return assemble(interleaved, null);
    }

    /**
     * Splits the interleaved message and optional soft bits into 4 CACH fragments and processes them through the
     * assembler.
     * @param interleaved message
     * @param softBits for the interleaved message, or null.
     * @return the short link control message dispatched with the last fragment.
     */
    private static ShortLCMessage assemble(BinaryMessage interleaved, float[] softBits)
    {
        SLCAssembler assembler = new SLCAssembler();
        LCSS[] lcss = new LCSS[]{LCSS.FIRST_FRAGMENT, LCSS.CONTINUATION_FRAGMENT, LCSS.CONTINUATION_FRAGMENT,
                LCSS.LAST_FRAGMENT};
        ShortLCMessage message = null;

        for(int fragment = 0; fragment < 4; fragment++)
        {
            BinaryMessage cachPayload = interleaved.getSubMessage(fragment * 17, fragment * 17 + 17);
            float[] softFragment = softBits != null ? Arrays.copyOfRange(softBits, fragment * 17, fragment * 17 + 17) :
                    null;
            message = assembler.process(lcss[fragment], cachPayload, softFragment, 0L);

            if(fragment < 3)
            {
                assertNull(message, "Message dispatched before the last fragment");
            }
        }

        assertNotNull(message, "Message not dispatched with the last fragment");
        return message;
    }

    /**
     * Test: an error free short link control message with a valid CRC is valid and carries the payload.
     */
    @Test
    void testValidMessage()
    {
        Random random = new Random(1);

        for(int trial = 0; trial < 20; trial++)
        {
            BinaryMessage payload = createPayload(random, true);
            ShortLCMessage message = assemble(encode(payload));
            assertTrue(message.isValid(), "Trial " + trial);
            assertEquals(payload.toString(), message.getMessage().getSubMessage(0, 36).toString(), "Trial " + trial);
        }
    }

    /**
     * Test: a short link control message with a single bit transmission error is corrected and valid.
     */
    @Test
    void testCorrectedMessage()
    {
        Random random = new Random(2);

        for(int trial = 0; trial < 68; trial++)
        {
            BinaryMessage payload = createPayload(random, true);
            BinaryMessage interleaved = encode(payload);
            interleaved.flip(trial);
            ShortLCMessage message = assemble(interleaved);
            assertTrue(message.isValid(), "Bit error at " + trial);
            assertEquals(payload.toString(), message.getMessage().getSubMessage(0, 36).toString(),
                    "Bit error at " + trial);
        }
    }

    /**
     * Test: a short link control message that passes BPTC error correction but fails the CRC-8 check is invalid.
     * This occurs when the BPTC decoder converges on an incorrect codeword (miscorrection).  The assembler previously
     * overwrote the CRC-8 result with the BPTC result, reporting these messages as valid.
     */
    @Test
    void testInvalidCRC()
    {
        Random random = new Random(3);

        for(int trial = 0; trial < 20; trial++)
        {
            ShortLCMessage message = assemble(encode(createPayload(random, false)));
            assertEquals(0, message.getMessage().getCorrectedBitCount(), "Trial " + trial);
            assertFalse(message.isValid(), "Trial " + trial);
        }
    }

    /**
     * Test: low reliability bit errors that defeat the hard decision decoder (decode failure, or a miscorrection that
     * fails the CRC-8 check) are corrected by soft decision decoding when the fragment soft bits are provided.
     */
    @Test
    void testSoftDecision()
    {
        int hardFailures = 0;

        for(int trial = 0; trial < 200 && hardFailures < 10; trial++)
        {
            Random random = new Random(100 + trial);
            BinaryMessage payload = createPayload(random, true);
            BinaryMessage interleaved = encode(payload);
            float[] softBits = new float[68];

            for(int x = 0; x < 68; x++)
            {
                softBits[x] = interleaved.get(x) ? 1.0f : -1.0f;
            }

            //Add 6 low reliability bit errors
            Set<Integer> errors = new HashSet<>();

            while(errors.size() < 6)
            {
                errors.add(random.nextInt(68));
            }

            for(int error: errors)
            {
                interleaved.flip(error);
                softBits[error] = -0.2f * softBits[error];
            }

            //Only evaluate trials where the hard decision result is invalid
            if(assemble(interleaved).isValid())
            {
                continue;
            }

            hardFailures++;
            ShortLCMessage message = assemble(interleaved, softBits);
            assertTrue(message.isValid(), "Trial " + trial);
            assertEquals(payload.toString(), message.getMessage().getSubMessage(0, 36).toString(), "Trial " + trial);
        }

        assertEquals(10, hardFailures, "Expected to find 10 hard decision failures");
    }
}
