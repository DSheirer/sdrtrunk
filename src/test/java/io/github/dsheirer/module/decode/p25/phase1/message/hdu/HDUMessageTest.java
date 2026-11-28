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

package io.github.dsheirer.module.decode.p25.phase1.message.hdu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Golay18Test;
import io.github.dsheirer.edac.Golay24;
import io.github.dsheirer.edac.ReedSolomonTestEncoder;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests HDU decoding of the header word: RS(36,20,17) encoded hex bits, each encoded with Golay(18,6,8)
 * (TIA-102.BAAA-B 4.2).
 */
public class HDUMessageTest
{
    private static final String MESSAGE_INDICATOR = "0123456789ABCDEF55";
    private static final int VENDOR = 0x90;
    private static final int ALGORITHM = 0x84;
    private static final int KEY = 0xBEEF;
    private static final int TALKGROUP = 20001;

    /**
     * Header word hex bits 0-35: 20 information hex bits followed by 16 Reed-Solomon parity hex bits.
     */
    private static int[] getHexBits()
    {
        BinaryMessage header = new BinaryMessage(120);
        header.load(0, 36, Long.parseLong(MESSAGE_INDICATOR.substring(0, 9), 16));
        header.load(36, 36, Long.parseLong(MESSAGE_INDICATOR.substring(9), 16));
        header.load(72, 8, VENDOR);
        header.load(80, 8, ALGORITHM);
        header.load(88, 16, KEY);
        header.load(104, 16, TALKGROUP);

        //The RS(63,47,17) codeword holds the hex bits in reverse order: parity at 0-15, information at 16-35
        int[] message = new int[47];

        for(int x = 0; x < 20; x++)
        {
            message[19 - x] = header.getInt(x * 6, x * 6 + 5);
        }

        int[] codeword = ReedSolomonTestEncoder.encode(message, 16);
        int[] hexBits = new int[36];

        for(int x = 0; x < 36; x++)
        {
            hexBits[x] = codeword[35 - x];
        }

        return hexBits;
    }

    private static int[] getGolayWords()
    {
        int[] hexBits = getHexBits();
        int[] words = new int[36];

        for(int x = 0; x < 36; x++)
        {
            words[x] = Golay18Test.encode(hexBits[x]);
        }

        return words;
    }

    private static HDUMessage getHDU(int[] words)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(P25P1DataUnitID.HEADER_DATA_UNIT.getMessageLength());
        message.setSize(P25P1DataUnitID.HEADER_DATA_UNIT.getMessageLength());

        for(int x = 0; x < 36; x++)
        {
            message.load(x * 18, 18, words[x]);
        }

        return new HDUMessage(message, 0x293, 0L);
    }

    private static void assertFields(HDUMessage hdu, String label)
    {
        assertTrue(hdu.isValid(), label);
        HeaderData headerData = hdu.getHeaderData();
        assertTrue(headerData.isValid(), label);
        assertEquals(MESSAGE_INDICATOR, headerData.getMessageIndicator(), label);
        assertEquals(Vendor.MOTOROLA, headerData.getVendor(), label);
        assertEquals(Encryption.AES_256, headerData.getEncryption(), label);
        assertTrue(headerData.isEncryptedAudio(), label);
        EncryptionKeyIdentifier key = headerData.getEncryptionKey();
        assertEquals(ALGORITHM, key.getValue().getAlgorithm(), label);
        assertEquals(KEY, key.getValue().getKey(), label);
        assertEquals(TALKGROUP, headerData.getTalkgroup().getValue(), label);
        List<Identifier> identifiers = hdu.getIdentifiers();
        assertEquals(2, identifiers.size(), label);
        assertTrue(identifiers.contains(headerData.getTalkgroup()), label);
        assertTrue(identifiers.contains(key), label);
    }

    @Test
    public void decodesEveryField()
    {
        HDUMessage hdu = getHDU(getGolayWords());
        assertFields(hdu, "clean");
        assertEquals(0, hdu.getMessage().getCorrectedBitCount());
    }

    @Test
    public void correctsThreeErrorsInEveryGolayWord()
    {
        Random random = new Random(1);

        for(int trial = 0; trial < 20; trial++)
        {
            int[] words = getGolayWords();

            for(int x = 0; x < 36; x++)
            {
                int errors = 0;

                while(Integer.bitCount(errors) < 3)
                {
                    errors |= 1 << random.nextInt(18);
                }

                words[x] ^= errors;
            }

            HDUMessage hdu = getHDU(words);
            assertFields(hdu, "trial " + trial);
            assertEquals(36 * 3, hdu.getMessage().getCorrectedBitCount(), "trial " + trial);
        }
    }

    /**
     * Four errors in the parity bits of a Golay word leave its hex bits intact, but the Golay(24,12) decode alone
     * miscorrects some of those patterns into the hex bits.  Golay18 detects four errors and leaves the word as
     * received, so nine such words, one more than the RS(36,20,17) decoder corrects, still decode.
     */
    @Test
    public void leavesUncorrectableWordsForTheReedSolomonDecoder()
    {
        int[] words = getGolayWords();

        for(int x = 0; x < 9; x++)
        {
            int word = x * 4;
            words[word] ^= getMiscorrectedParityErrors(words[word]);
        }

        assertFields(getHDU(words), "nine words with four parity bit errors");
    }

    /**
     * Finds a four bit error pattern in the 12 parity bits of the codeword that the Golay(24,12) decode alone corrects
     * into the hex bits.
     */
    private static int getMiscorrectedParityErrors(int codeword)
    {
        for(int errors = 0; errors < (1 << 12); errors++)
        {
            if(Integer.bitCount(errors) == 4)
            {
                CorrectedBinaryMessage word = new CorrectedBinaryMessage(24);
                word.load(6, 18, codeword ^ errors);
                Golay24.checkAndCorrect(word, 0);

                if(word.getInt(6, 11) != codeword >> 12)
                {
                    return errors;
                }
            }
        }

        throw new IllegalStateException("No miscorrected pattern for codeword " + Integer.toHexString(codeword));
    }
}
