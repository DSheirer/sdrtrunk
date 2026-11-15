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

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the DMR EMB QR(16,7,6) codeword table and the hard and soft decision decoders.
 */
public class EMBTest
{
    private static final int[] WORD = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    //Codewords for the 7 single-bit information words (1, 2, 4, ... 64) from the TS 102 361-1 B.3.2 codeword table.
    //Because the code is linear, these generator rows determine every other codeword.
    private static final int[] GENERATOR_ROWS = new int[]{0x0273, 0x04E5, 0x09C9, 0x11E2, 0x21B7, 0x411E, 0x804F};

    /**
     * Creates an EMB message holding the 16-bit word.
     */
    private static CorrectedBinaryMessage message(int word)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(16);
        message.setInt(word, WORD);
        return message;
    }

    /**
     * Asserts that the EMB fields decode to the information word (color code, privacy indicator, LCSS).
     */
    private static void assertFields(int information, EMB emb, String context)
    {
        assertEquals(information >> 3, emb.getColorCode(), context);
        assertEquals(((information >> 2) & 1) == 1, emb.isEncrypted(), context);
        assertEquals(LCSS.fromValue(information & 3), emb.getLCSS(), context);
    }

    /**
     * Test: the codeword table is the systematic, linear QR(16,7,6) code from the specification with minimum distance
     * 6, so that every 1 and 2 bit error is uniquely correctable.
     */
    @Test
    void testCodewordTable()
    {
        int[] words = EMB.getValidWords();
        assertEquals(128, words.length);

        for(int bit = 0; bit < 7; bit++)
        {
            assertEquals(GENERATOR_ROWS[bit], words[1 << bit], "Generator row for information bit " + bit);
        }

        int minimumDistance = Integer.MAX_VALUE;

        for(int a = 0; a < 128; a++)
        {
            assertEquals(a, words[a] >> 9, "Information bits are the leading 7 bits");

            for(int b = 0; b < 128; b++)
            {
                assertEquals(words[a ^ b], words[a] ^ words[b], "Linearity " + a + " ^ " + b);

                if(a != b)
                {
                    minimumDistance = Math.min(minimumDistance, Integer.bitCount(words[a] ^ words[b]));
                }
            }
        }

        assertEquals(6, minimumDistance);
    }

    /**
     * Test: the hard decoder accepts every codeword and corrects every 1 and 2 bit error pattern on every codeword
     * back to that codeword, with the corrected fields (including color codes 8-15).
     */
    @Test
    void testHardDecodeCorrectsUpToTwoBitErrors()
    {
        int[] words = EMB.getValidWords();

        for(int information = 0; information < 128; information++)
        {
            int codeword = words[information];

            EMB clean = new EMB(message(codeword));
            assertTrue(clean.isValid(), "Codeword " + information);
            assertFields(information, clean, "Codeword " + information);

            for(int first = 0; first < 16; first++)
            {
                for(int second = first; second < 16; second++)
                {
                    int errors = (1 << first) | (1 << second);
                    String context = "Codeword " + information + " errors " + Integer.toHexString(errors);
                    CorrectedBinaryMessage received = message(codeword ^ errors);
                    EMB emb = new EMB(received);
                    assertTrue(emb.isValid(), context);
                    assertEquals(codeword, received.getInt(WORD), context);
                    assertEquals(Integer.bitCount(errors), received.getCorrectedBitCount(), context);
                    assertFields(information, emb, context);
                }
            }
        }
    }

    /**
     * Test: the hard decoder rejects every 3 bit error pattern on every codeword, which is beyond the correction
     * capability of a distance 6 code, and leaves the received bits unchanged rather than miscorrecting them.
     */
    @Test
    void testHardDecodeRejectsThreeBitErrors()
    {
        int[] words = EMB.getValidWords();

        for(int codeword: words)
        {
            for(int a = 0; a < 16; a++)
            {
                for(int b = a + 1; b < 16; b++)
                {
                    for(int c = b + 1; c < 16; c++)
                    {
                        int receivedWord = codeword ^ (1 << a) ^ (1 << b) ^ (1 << c);
                        CorrectedBinaryMessage received = message(receivedWord);
                        EMB emb = new EMB(received);
                        String context = "Received " + Integer.toHexString(receivedWord);
                        assertFalse(emb.isValid(), context);
                        assertEquals(receivedWord, received.getInt(WORD), context);
                        assertEquals(0, received.getCorrectedBitCount(), context);
                    }
                }
            }
        }
    }

    /**
     * Test: soft decoding corrects 1 and 2 low reliability bit errors and flags the result valid.  With 4 low
     * reliability bit errors, it still corrects to the transmitted word (best estimate, beyond the hard decoder's
     * capability) but flags the result invalid since it exceeds the correction capability.
     */
    @Test
    void testSoftDecode()
    {
        int[] words = EMB.getValidWords();
        Random random = new Random(3);

        for(int trial = 0; trial < 1000; trial++)
        {
            int information = random.nextInt(128);
            int codeword = words[information];

            for(int errorCount: new int[]{1, 2, 4})
            {
                CorrectedBinaryMessage received = message(codeword);
                float[] softBits = new float[16];

                for(int x = 0; x < 16; x++)
                {
                    softBits[x] = received.get(x) ? 1.0f : -1.0f;
                }

                Set<Integer> errors = new HashSet<>();

                while(errors.size() < errorCount)
                {
                    errors.add(random.nextInt(16));
                }

                for(int error: errors)
                {
                    received.flip(error);
                    softBits[error] = -0.2f * softBits[error];
                }

                EMB emb = new EMB(received, softBits);
                String context = "Trial " + trial + " errors " + errorCount;
                assertEquals(errorCount <= 2, emb.isValid(), context);
                assertEquals(codeword, received.getInt(WORD), context);
                assertEquals(errorCount, received.getCorrectedBitCount(), context);
                assertFields(information, emb, context);
            }
        }
    }

    /**
     * Test: without soft bits, the soft decision constructor falls back to the hard decoder.
     */
    @Test
    void testNullSoftBitsUsesHardDecoder()
    {
        int codeword = EMB.getValidWords()[93];
        CorrectedBinaryMessage received = message(codeword ^ 0x0101);
        EMB emb = new EMB(received, null);
        assertTrue(emb.isValid());
        assertEquals(codeword, received.getInt(WORD));
        assertEquals(2, received.getCorrectedBitCount());
        assertFields(93, emb, "Null soft bits");
    }
}
