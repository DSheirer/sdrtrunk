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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRBurstTestUtility;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the DMR BPTC(196,96) hard decision decoder.
 */
public class BPTC_196_96Test
{
    /**
     * Test: when the hard decision decoder recovers the transmitted codeword, the corrected bit count equals the
     * number of bit errors.  The turbo decoder's incremental accounting previously counted some corrections twice
     * (ie reporting 13-14 corrected bits for 10 bit errors).
     */
    @Test
    void testCorrectedBitCount()
    {
        Random random = new Random(1);
        int[] correctDecodes = new int[9];

        for(int errorCount = 1; errorCount <= 8; errorCount++)
        {
            for(int trial = 0; trial < 200; trial++)
            {
                CorrectedBinaryMessage bptc = DMRBurstTestUtility.createInterleavedBPTC(random);
                CorrectedBinaryMessage expected = BPTC_196_96.extract(bptc);
                CorrectedBinaryMessage received = new CorrectedBinaryMessage(bptc);

                //Bit 0 is a pad bit that isn't protected by the row or column codes
                Set<Integer> errors = new HashSet<>();

                while(errors.size() < errorCount)
                {
                    errors.add(1 + random.nextInt(195));
                }

                for(int error: errors)
                {
                    received.flip(error);
                }

                CorrectedBinaryMessage decoded = BPTC_196_96.extract(received);

                if(decoded.getCorrectedBitCount() >= 0 && decoded.toString().equals(expected.toString()))
                {
                    correctDecodes[errorCount]++;
                    assertEquals(errorCount, decoded.getCorrectedBitCount(), "Errors " + errorCount + " trial " + trial);
                }
            }
        }

        //Up to 2 bit errors are always corrected
        assertEquals(200, correctDecodes[1]);
        assertEquals(200, correctDecodes[2]);

        for(int errorCount = 3; errorCount <= 8; errorCount++)
        {
            assertTrue(correctDecodes[errorCount] > 100, "Expected most decodes to be correct for " + errorCount +
                    " errors, but found " + correctDecodes[errorCount]);
        }
    }

    /**
     * Test: soft decision extraction of an error free codeword matches the hard decision extraction.
     */
    @Test
    void testExtractSoft()
    {
        Random random = new Random(2);

        for(int trial = 0; trial < 20; trial++)
        {
            CorrectedBinaryMessage bptc = DMRBurstTestUtility.createInterleavedBPTC(random);
            float[] softBits = new float[196];

            for(int x = 0; x < 196; x++)
            {
                softBits[x] = bptc.get(x) ? 1.0f : -1.0f;
            }

            CorrectedBinaryMessage soft = BPTC_196_96.extractSoft(softBits);
            assertNotNull(soft);
            assertEquals(BPTC_196_96.extract(bptc).toString(), soft.toString());
            assertEquals(0, soft.getCorrectedBitCount());
        }
    }
}
