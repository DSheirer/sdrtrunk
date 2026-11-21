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

package io.github.dsheirer.module.decode.edacs.provoice;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Decoding a ProVoice frame recorded on MBTA LCN 5 (851.9375 MHz, 2026-11-09) and building
 * P25 IMBE frames for JMBE.  The expected IMBE frames were checked by decoding them with JMBE 2.1 to speech.
 */
public class ProVoiceTest
{
    /** One frame as demodulated: sync (inverted, as MBTA sends it) plus 736 bits */
    private static final String RECORDED = "6b27cadcf140cccc969696969696969667fffbaa8a55b3c411cbcdf82482d3eea0c38e1df5c69d" +
        "efc60fdeb4bb188e11cf70e09e01892c8fba57de8aae30804d2e2aa6c7d325800aebbfefa767571d7044ecb291c3382318cfe08bcbbe" +
        "820087";
    private static final String[] EXPECTED = {"6EA922FEA8BDF35001EB7C8E295748845730",
        "761D4CC87BB59B2572052ECCDE6B6F0C1B35", "7A6F0F273B3D89A14F4447CC3A7348A46EF8",
        "7E4B4D3BBA33A5142E36318E5AD5EBE66EF8"};

    private static boolean[] bits(String hex)
    {
        boolean[] bits = new boolean[hex.length() * 4];

        for(int i = 0; i < hex.length(); i++)
        {
            int nibble = Character.digit(hex.charAt(i), 16);

            for(int b = 0; b < 4; b++)
            {
                bits[i * 4 + b] = ((nibble >> (3 - b)) & 1) == 1;
            }
        }

        return bits;
    }

    private static List<ProVoiceMessage> decode(boolean[] bits)
    {
        List<ProVoiceMessage> messages = new ArrayList<>();
        ProVoiceDecoder decoder = new ProVoiceDecoder();
        decoder.setMessageListener(message -> messages.add((ProVoiceMessage)message));

        for(boolean bit : new boolean[40])
        {
            decoder.process(bit);
        }
        for(boolean bit : bits)
        {
            decoder.process(bit);
        }

        return messages;
    }

    /**
     * Test: the recorded frame decodes to the expected four IMBE frames without errors, and still does with bit errors
     * that the voice frame error correction covers.
     */
    @Test
    void testRecordedFrame()
    {
        List<ProVoiceMessage> messages = decode(bits(RECORDED));
        assertEquals(1, messages.size());
        assertEquals(0, messages.get(0).getErrors());

        for(int x = 0; x < 4; x++)
        {
            assertArrayEquals(HexFormat.of().parseHex(EXPECTED[x]), messages.get(0).getIMBEFrames().get(x),
                "IMBE frame " + x);
        }

        //Flip a few payload bits well apart: corrected by the Golay/Hamming codes
        boolean[] damaged = bits(RECORDED);
        damaged[32 + 152] ^= true; //not bit 150, which is the flag bit that is no longer counted
        damaged[32 + 400] ^= true;
        damaged[32 + 650] ^= true;
        List<ProVoiceMessage> corrected = decode(damaged);
        assertEquals(1, corrected.size());
        assertEquals(3, corrected.get(0).getErrors());

        for(int x = 0; x < 4; x++)
        {
            assertArrayEquals(HexFormat.of().parseHex(EXPECTED[x]), corrected.get(0).getIMBEFrames().get(x));
        }
    }

    /**
     * Test: the encoder produces valid P25 codewords - every Golay (23,12) and Hamming (15,11) syndrome is zero, using
     * the checksums JMBE checks with.
     */
    @Test
    void testCodewords()
    {
        int[] golay = {0x63A, 0x31D, 0x7B4, 0x3DA, 0x1ED, 0x6CC, 0x366, 0x1B3, 0x6E3, 0x54B, 0x49F, 0x475, 0x400, 0x200,
            0x100, 0x080, 0x040, 0x020, 0x010, 0x008, 0x004, 0x002, 0x001};
        int[] hamming = {0xF, 0xE, 0xD, 0xC, 0xB, 0xA, 0x9, 0x7, 0x6, 0x5, 0x3, 0x8, 0x4, 0x2, 0x1};
        Random random = new Random(3);

        for(int t = 0; t < 200; t++)
        {
            int[] parameters = new int[88];
            for(int i = 0; i < 88; i++)
            {
                parameters[i] = random.nextInt(2);
            }

            int[] frame = IMBEFrameEncoder.codewords(parameters);

            for(int c = 0; c < 4; c++)
            {
                int syndrome = 0;
                for(int i = 0; i < 23; i++)
                {
                    if(frame[c * 23 + i] == 1)
                    {
                        syndrome ^= golay[i];
                    }
                }
                assertEquals(0, syndrome, "Golay " + c);
            }

            for(int c = 0; c < 3; c++)
            {
                int syndrome = 0;
                for(int i = 0; i < 15; i++)
                {
                    if(frame[92 + c * 15 + i] == 1)
                    {
                        syndrome ^= hamming[i];
                    }
                }
                assertEquals(0, syndrome, "Hamming " + c);
            }
        }
    }

    /**
     * Test: damaged voice frames are replaced by the last good frame up to three times, then by silence (null); a good
     * frame resets it.
     */
    @Test
    void testConcealment()
    {
        ProVoiceDecoder decoder = new ProVoiceDecoder();
        byte[] good = new byte[18];
        byte[] bad = new byte[18];
        byte[] next = new byte[18];

        assertNull(decoder.conceal(bad, 9), "nothing to repeat yet");
        assertSame(good, decoder.conceal(good, ProVoiceDecoder.MAX_ERRORS));
        assertSame(good, decoder.conceal(bad, 6));
        assertSame(good, decoder.conceal(bad, 7));
        assertSame(good, decoder.conceal(bad, 8));
        assertNull(decoder.conceal(bad, 6));
        assertNull(decoder.conceal(bad, 6));
        assertSame(next, decoder.conceal(next, 0));
        assertSame(next, decoder.conceal(bad, 11));
        assertEquals(7, decoder.getConcealedCount());
    }
}
