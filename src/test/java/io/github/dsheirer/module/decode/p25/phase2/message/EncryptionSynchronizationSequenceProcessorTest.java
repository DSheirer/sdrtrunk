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

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.edac.ReedSolomonTestEncoder;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * JUnit tests for the P25 Phase 2 Encryption Synchronization Sequence (ESS) RS(44,16,29) decoding.
 */
public class EncryptionSynchronizationSequenceProcessorTest
{
    /**
     * Creates the RS codeword for the 16 ESS information symbols, where codeword index 43 is the first information
     * symbol, indices 28-43 are information symbols and indices 0-27 are parity symbols.
     */
    private static int[] encode(int[] information)
    {
        int[] message = new int[35];

        for(int x = 0; x < 16; x++)
        {
            message[15 - x] = information[x];
        }

        return ReedSolomonTestEncoder.encode(message);
    }

    /**
     * ESS-A fragment (168 bits) containing the 28 parity symbols, where hexbit x is codeword index 27 - x.
     */
    private static BinaryMessage getEssA(int[] codeword)
    {
        BinaryMessage essA = new BinaryMessage(168);

        for(int x = 0; x < 28; x++)
        {
            essA.load(x * 6, 6, codeword[27 - x]);
        }

        return essA;
    }

    /**
     * ESS-B fragment (24 bits) containing 4 information symbols, where fragment 1 is codeword indices 40-43 and
     * fragment 4 is codeword indices 28-31, and hexbit x is codeword index (base + 3 - x).
     */
    private static BinaryMessage getEssB(int[] codeword, int fragment)
    {
        int base = 28 + (4 - fragment) * 4;
        BinaryMessage essB = new BinaryMessage(24);

        for(int x = 0; x < 4; x++)
        {
            essB.load(x * 6, 6, codeword[base + 3 - x]);
        }

        return essB;
    }

    /**
     * Expected decoded 96-bit ESS message.
     */
    private static String getExpected(int[] information)
    {
        BinaryMessage expected = new BinaryMessage(96);

        for(int x = 0; x < 16; x++)
        {
            expected.load(x * 6, 6, information[x]);
        }

        return expected.toHexString();
    }

    /**
     * Corrupts the specified number of distinct ESS-A (parity) symbols.
     */
    private static void corruptParity(int[] codeword, int errors, Random random)
    {
        boolean[] corrupted = new boolean[28];
        int count = 0;

        while(count < errors)
        {
            int index = random.nextInt(28);

            if(!corrupted[index])
            {
                corrupted[index] = true;
                codeword[index] ^= 1 + random.nextInt(63);
                count++;
            }
        }
    }

    private static int[] getEncryptedInformation(Random random)
    {
        int[] information = new int[16];

        for(int x = 0; x < 16; x++)
        {
            information[x] = random.nextInt(64);
        }

        return information;
    }

    @Test
    public void decodesCompleteSequenceWithErrors()
    {
        Random random = new Random(1);

        for(int trial = 0; trial < 100; trial++)
        {
            int[] information = getEncryptedInformation(random);
            int[] codeword = encode(information);
            int[] received = codeword.clone();
            corruptParity(received, 14, random);

            EncryptionSynchronizationSequenceProcessor processor = new EncryptionSynchronizationSequenceProcessor(1);
            processor.setESSA(getEssA(received));
            processor.setESSB1(getEssB(received, 1));
            processor.setESSB2(getEssB(received, 2));
            processor.setESSB3(getEssB(received, 3));
            processor.setESSB4(getEssB(received, 4));

            EncryptionSynchronizationSequence sequence = processor.getSequence();
            assertNotNull(sequence);
            assertEquals(getExpected(information), sequence.getMessage().toHexString());
        }
    }

    /**
     * Encrypted sequence (random message indicator) with 3 missing ESS-B fragments and 4 parity symbol errors.  The
     * missing fragments can't be decoded as zero valued symbols (12 + 4 errors exceeds the 14 symbol error capacity),
     * but can be decoded as erasures (12 erasures + 4 errors).
     */
    @Test
    public void decodesEncryptedSequenceWithMissingFragmentsAsErasures()
    {
        Random random = new Random(2);

        for(int trial = 0; trial < 100; trial++)
        {
            int[] information = getEncryptedInformation(random);
            int[] codeword = encode(information);
            int[] received = codeword.clone();
            corruptParity(received, 4, random);

            EncryptionSynchronizationSequenceProcessor processor = new EncryptionSynchronizationSequenceProcessor(1);
            processor.setESSA(getEssA(received));
            processor.setESSB1(getEssB(received, 1));

            EncryptionSynchronizationSequence sequence = processor.getSequence();
            assertNotNull(sequence, "Trial " + trial);
            assertEquals(getExpected(information), sequence.getMessage().toHexString());
        }
    }

    /**
     * Unencrypted sequence (algorithm 0x80 with zero key ID and message indicator) with 3 missing ESS-B fragments and
     * 9 parity symbol errors.  The missing fragments decode as zero valued symbols (9 errors), but can't be decoded
     * as erasures (12 erasures + 9 errors exceeds the capacity).
     */
    @Test
    public void decodesUnencryptedSequenceWithMissingFragmentsAsZeros()
    {
        Random random = new Random(3);
        int[] information = new int[16];
        information[0] = 0x20; //Algorithm 0x80 (unencrypted) in the first 8 bits

        for(int trial = 0; trial < 100; trial++)
        {
            int[] codeword = encode(information);
            int[] received = codeword.clone();
            corruptParity(received, 9, random);

            EncryptionSynchronizationSequenceProcessor processor = new EncryptionSynchronizationSequenceProcessor(1);
            processor.setESSA(getEssA(received));
            processor.setESSB1(getEssB(received, 1));

            EncryptionSynchronizationSequence sequence = processor.getSequence();
            assertNotNull(sequence, "Trial " + trial);
            assertEquals(getExpected(information), sequence.getMessage().toHexString());
            assertEquals(0x80, sequence.getAlgorithmId());
        }
    }
}
