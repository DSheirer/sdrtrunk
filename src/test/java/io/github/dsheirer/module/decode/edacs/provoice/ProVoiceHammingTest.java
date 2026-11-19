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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Hamming (15,11) code on two of a voice frame's code words corrects the bit that is wrong.
 * An earlier version flipped another bit for 11 of the 15 positions, so a single bit error in those words left the
 * voice frame with two wrong bits and still counted it as corrected.
 */
public class ProVoiceHammingTest
{
    private static int[] bits(int word)
    {
        int[] bits = new int[15];

        for(int x = 0; x < 15; x++)
        {
            bits[x] = (word >> x) & 1;
        }

        return bits;
    }

    @Test
    void everySingleWrongBitIsCorrected()
    {
        int checked = 0;

        for(int word = 0; word < 32768; word += 37)
        {
            //Any word corrects to a code word; that code word is the reference
            int[] codeWord = new int[15];
            ProVoiceFrameDecoder.hamming(bits(word), codeWord);
            int[] again = new int[15];
            assertEquals(0, ProVoiceFrameDecoder.hamming(codeWord, again), "A code word has no errors");
            assertArrayEquals(codeWord, again);

            for(int wrong = 0; wrong < 15; wrong++)
            {
                int[] received = codeWord.clone();
                received[wrong] ^= 1;
                int[] corrected = new int[15];
                assertEquals(1, ProVoiceFrameDecoder.hamming(received, corrected));
                assertArrayEquals(codeWord, corrected, "wrong bit " + wrong);
                checked++;
            }
        }

        assertTrue(checked > 10000);
    }

    /**
     * A real voice frame with one bit wrong anywhere in its first pair of voice frames decodes to the same voice,
     * or - for a bit of the last code word, which has no protection - to voice with that one bit wrong.
     */
    @Test
    void oneWrongBitInARealFrameChangesAtMostOneParameterBit() throws Exception
    {
        int[] frame = null;

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(ProVoiceHammingTest.class
            .getResourceAsStream("/edacs/provoice_transmission_starts.txt"), StandardCharsets.US_ASCII)))
        {
            //Transmission 0, a frame of clean voice
            String[] fields = reader.readLine().split(" ");
            frame = new int[fields[3].length()];

            for(int x = 0; x < frame.length; x++)
            {
                frame[x] = fields[3].charAt(x) - '0';
            }
        }

        ProVoiceFrameDecoder.VoiceFrame[] clean = ProVoiceFrameDecoder.decode(frame);
        assertEquals(0, clean[0].errors() + clean[1].errors());
        int corrected = 0;
        int unprotected = 0;

        for(int bit = 144; bit < ProVoiceFrameDecoder.FIRST_PAIR_END; bit++)
        {
            int[] damaged = frame.clone();
            damaged[bit] ^= 1;
            ProVoiceFrameDecoder.VoiceFrame[] decoded = ProVoiceFrameDecoder.decode(damaged);

            for(int x = 0; x < 2; x++)
            {
                int different = 0;

                for(int p = 0; p < 88; p++)
                {
                    if(decoded[x].parameters()[p] != clean[x].parameters()[p])
                    {
                        different++;
                    }
                }

                if(different == 0)
                {
                    corrected++;
                }
                else
                {
                    assertEquals(1, different, "frame bit " + bit + ": voice frame " + x + " has " + different +
                        " wrong parameter bits");
                    assertEquals(0, decoded[x].errors(), "frame bit " + bit + " is in an unprotected word");
                    unprotected++;
                }
            }

            assertTrue(Arrays.equals(decoded[2].parameters(), clean[2].parameters()));
        }

        //Two voice frames of 23 unprotected bits each
        assertTrue(unprotected <= 46, "unprotected: " + unprotected);
        assertTrue(corrected >= 2 * (ProVoiceFrameDecoder.FIRST_PAIR_END - 144) - 46 - 8, "corrected: " + corrected);
    }
}
