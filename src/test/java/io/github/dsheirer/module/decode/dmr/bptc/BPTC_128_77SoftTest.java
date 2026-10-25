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

package io.github.dsheirer.module.decode.dmr.bptc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Hamming16;
import io.github.dsheirer.edac.soft.SoftBlockCode;
import io.github.dsheirer.module.decode.dmr.message.voice.EMB;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for DMR soft decision decoding of the embedded signalling BPTC(128,77) product code and the EMB.
 */
public class BPTC_128_77SoftTest
{
    private static final int[] WORD = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final SoftBlockCode ROW_CODE = SoftBlockCode.fromCodewordTest(16, word -> {
        BinaryMessage message = new BinaryMessage(16);

        for(int i = 0; i < 16; i++)
        {
            if((word & (1 << i)) != 0)
            {
                message.set(i);
            }
        }

        return Hamming16.getSyndrome(message, 0) == 0;
    });

    /**
     * Creates a random valid (de-interleaved) BPTC(128,77) product codeword: 7 random Hamming(16,11,4) rows followed
     * by the column parity row.
     */
    private static CorrectedBinaryMessage createCodeword(Random random)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(128);
        int[] codewords = ROW_CODE.getCodewords();
        int parityRow = 0;

        for(int row = 0; row < 7; row++)
        {
            int rowWord = codewords[random.nextInt(codewords.length)];
            parityRow ^= rowWord;

            for(int column = 0; column < 16; column++)
            {
                if((rowWord & (1 << column)) != 0)
                {
                    message.set(row * 16 + column);
                }
            }
        }

        for(int column = 0; column < 16; column++)
        {
            if((parityRow & (1 << column)) != 0)
            {
                message.set(7 * 16 + column);
            }
        }

        return message;
    }

    /**
     * Test: the row code is Hamming(16,11) with 2048 codewords and the generated product codewords are valid for the
     * hard decision decoder.
     */
    @Test
    void testValidCodeword()
    {
        assertEquals(2048, ROW_CODE.getCodewords().length);

        BPTC_128_77 bptc = new BPTC_128_77();
        CorrectedBinaryMessage codeword = createCodeword(new Random(1));
        assertTrue(bptc.getRowErrors(codeword).isEmpty());
        assertTrue(bptc.getColumnErrors(codeword).isEmpty());

        float[] softBits = new float[128];

        for(int x = 0; x < 128; x++)
        {
            softBits[x] = codeword.get(x) ? 1.0f : -1.0f;
        }

        CorrectedBinaryMessage decoded = bptc.decodeSoft(softBits);
        assertNotNull(decoded);
        assertEquals(codeword, decoded);
        assertEquals(0, decoded.getCorrectedBitCount());
    }

    /**
     * Test: soft decoding corrects 6 low reliability bit errors placed randomly across the product codeword.
     */
    @Test
    void testSoftDecodeRandomErrors()
    {
        Random random = new Random(2);
        BPTC_128_77 bptc = new BPTC_128_77();
        int hardFailures = 0;

        for(int trial = 0; trial < 200; trial++)
        {
            CorrectedBinaryMessage codeword = createCodeword(random);
            float[] softBits = new float[128];

            for(int x = 0; x < 128; x++)
            {
                float magnitude = 0.6f + 0.8f * random.nextFloat();
                softBits[x] = codeword.get(x) ? magnitude : -magnitude;
            }

            Set<Integer> errors = new HashSet<>();

            while(errors.size() < 6)
            {
                errors.add(random.nextInt(128));
            }

            CorrectedBinaryMessage hard = new CorrectedBinaryMessage(codeword);

            for(int error: errors)
            {
                softBits[error] = -0.3f * random.nextFloat() * Math.signum(softBits[error]);
                hard.flip(error);
            }

            if(!bptc.correct(hard) || !hard.equals(codeword))
            {
                hardFailures++;
            }

            CorrectedBinaryMessage decoded = bptc.decodeSoft(softBits);
            assertNotNull(decoded, "Soft decode failed on trial " + trial);
            assertEquals(codeword, decoded, "Soft decode miscorrected on trial " + trial);
            assertEquals(6, decoded.getCorrectedBitCount());
        }

        //Sanity check that this scenario exceeds the hard decision decoder capability
        assertTrue(hardFailures > 0, "Expected hard decision failures");
    }

    /**
     * Test: EMB soft decoding corrects 4 low reliability bit errors (beyond the hard decoder's 3-bit capability) and
     * flags the result invalid since it exceeds the soft decision validity threshold.  Also verifies that a single
     * low reliability bit error is corrected and valid.
     */
    @Test
    void testEMBSoftDecode()
    {
        Random random = new Random(3);
        CorrectedBinaryMessage reference = new CorrectedBinaryMessage(16);

        for(int trial = 0; trial < 100; trial++)
        {
            //Find a valid word by hard decoding a random word with no more than 1 bit error after correction
            int word;
            EMB referenceEMB;

            do
            {
                word = random.nextInt(1 << 16);
                reference.setInt(word, WORD);
                referenceEMB = new EMB(new CorrectedBinaryMessage(reference));
            }
            while(!referenceEMB.isValid());

            //Recover the corrected valid word
            CorrectedBinaryMessage corrected = new CorrectedBinaryMessage(reference);
            new EMB(corrected);
            int validWord = corrected.getInt(0, 15);

            for(int errorCount: new int[]{1, 4})
            {
                float[] softBits = new float[16];
                CorrectedBinaryMessage received = new CorrectedBinaryMessage(16);
                received.setInt(validWord, WORD);

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
                assertEquals(validWord, received.getInt(0, 15), "Trial " + trial + " errors " + errorCount);

                if(errorCount == 1)
                {
                    assertTrue(emb.isValid());
                }
                else
                {
                    assertFalse(emb.isValid());
                }
            }
        }
    }
}
