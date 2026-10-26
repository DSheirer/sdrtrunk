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
import io.github.dsheirer.edac.Hamming13;
import io.github.dsheirer.edac.Hamming15;
import io.github.dsheirer.edac.soft.SoftBlockCode;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for DMR soft decision decoding of the BPTC(196,96) product code.
 */
public class BPTC_196_96SoftTest
{
    private static final SoftBlockCode ROW_CODE = SoftBlockCode.fromCodewordTest(15,
            word -> Hamming15.getSyndrome(toMessage(word, 15), 0) == 0);
    private static final SoftBlockCode COLUMN_CODE = SoftBlockCode.fromCodewordTest(13,
            word -> Hamming13.getSyndrome(toMessage(word, 13), new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12}) == 0);

    private static BinaryMessage toMessage(int word, int length)
    {
        BinaryMessage message = new BinaryMessage(length);

        for(int i = 0; i < length; i++)
        {
            if((word & (1 << i)) != 0)
            {
                message.set(i);
            }
        }

        return message;
    }

    /**
     * Finds the systematic codeword whose information bits (the low bits) match the information value.
     */
    private static int encode(SoftBlockCode code, int information, int informationLength)
    {
        int mask = (1 << informationLength) - 1;

        for(int codeword: code.getCodewords())
        {
            if((codeword & mask) == information)
            {
                return codeword;
            }
        }

        throw new IllegalStateException("No codeword for information value " + information);
    }

    /**
     * Creates a random valid (de-interleaved) BPTC(196,96) product codeword.
     */
    private static CorrectedBinaryMessage createCodeword(Random random)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(196);

        //Information rows 0-8, each encoded with the row code
        for(int row = 0; row < 9; row++)
        {
            int rowWord = encode(ROW_CODE, random.nextInt(1 << 11), 11);

            for(int x = 0; x < 15; x++)
            {
                if((rowWord & (1 << x)) != 0)
                {
                    message.set(BPTC_196_96.getIndex(x, row));
                }
            }
        }

        //Parity rows 9-12 from the column code
        for(int column = 0; column < 15; column++)
        {
            int information = 0;

            for(int row = 0; row < 9; row++)
            {
                if(message.get(BPTC_196_96.COLUMN_INDEXES[column][row]))
                {
                    information |= (1 << row);
                }
            }

            int columnWord = encode(COLUMN_CODE, information, 9);

            for(int row = 9; row < 13; row++)
            {
                if((columnWord & (1 << row)) != 0)
                {
                    message.set(BPTC_196_96.COLUMN_INDEXES[column][row]);
                }
            }
        }

        return message;
    }

    /**
     * Test: valid product codewords have no row or column errors and soft decoding of an error free codeword
     * returns the codeword.
     */
    @Test
    void testValidCodeword()
    {
        Random random = new Random(2);
        CorrectedBinaryMessage codeword = createCodeword(random);
        CorrectedBinaryMessage copy = new CorrectedBinaryMessage(codeword);
        assertTrue(BPTC_196_96.correct(copy, true));
        assertEquals(codeword, copy);

        float[] softBits = new float[196];

        for(int x = 0; x < 196; x++)
        {
            softBits[x] = codeword.get(x) ? 1.0f : -1.0f;
        }

        CorrectedBinaryMessage decoded = BPTC_196_96.decodeSoft(softBits);
        assertNotNull(decoded);
        assertEquals(codeword, decoded);
        assertEquals(0, decoded.getCorrectedBitCount());
    }

    /**
     * Test: soft decoding corrects 10 low reliability bit errors placed randomly across the product codeword.
     */
    @Test
    void testSoftDecodeRandomErrors()
    {
        Random random = new Random(3);
        int hardFailures = 0;

        for(int trial = 0; trial < 200; trial++)
        {
            CorrectedBinaryMessage codeword = createCodeword(random);
            float[] softBits = new float[196];

            for(int x = 0; x < 196; x++)
            {
                float magnitude = 0.6f + 0.8f * random.nextFloat();
                softBits[x] = codeword.get(x) ? magnitude : -magnitude;
            }

            Set<Integer> errors = new HashSet<>();

            while(errors.size() < 10)
            {
                errors.add(1 + random.nextInt(195));
            }

            CorrectedBinaryMessage hard = new CorrectedBinaryMessage(codeword);

            for(int error: errors)
            {
                softBits[error] = -0.3f * random.nextFloat() * Math.signum(softBits[error]);
                hard.flip(error);
            }

            if(!BPTC_196_96.correct(hard, true) || !hard.equals(codeword))
            {
                hardFailures++;
            }

            CorrectedBinaryMessage decoded = BPTC_196_96.decodeSoft(softBits);
            assertNotNull(decoded, "Soft decode failed on trial " + trial);
            assertEquals(codeword, decoded, "Soft decode miscorrected on trial " + trial);
            assertEquals(10, decoded.getCorrectedBitCount());
        }

        //Sanity check that this scenario exceeds the hard decision decoder capability
        assertTrue(hardFailures > 0, "Expected hard decision failures");
    }
}
