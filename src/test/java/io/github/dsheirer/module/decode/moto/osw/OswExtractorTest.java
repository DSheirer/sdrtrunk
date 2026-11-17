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

package io.github.dsheirer.module.decode.moto.osw;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.module.decode.moto.Bandplan;
import io.github.dsheirer.module.decode.moto.BandplanType;
import io.github.dsheirer.module.decode.moto.message.MotorolaTypeIIMessage;
import io.github.dsheirer.module.decode.moto.message.MotorolaTypeIIMessageType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the control channel OSW extractor against a synthesized discriminator signal: 3,600 baud two-level data at
 * 5 samples per symbol, one 84-bit OSW after another.
 */
class OswExtractorTest
{
    private static final double SAMPLE_RATE = 18000.0;
    private static final int SAMPLES_PER_SYMBOL = 5;
    private static final int SYNC = 0xAC;
    private static final int SYNC_BITS = 8;
    private static final int PAYLOAD_BITS = 76;
    private static final int INFO_BITS = 38;
    private static final int DATA_BITS = 27;
    private static final int CRC_BITS = 10;
    private static final int ID_XOR = 0xCC38;
    private static final int CMD_XOR = 0xCD5;
    private static final int CMD_SYSTEM_STATUS = 0x3C0;
    //The message factory holds this many OSWs back to assemble multi-OSW messages.
    private static final int QUEUE_HOLD = 5;

    /**
     * Creates the bits for one OSW: the sync word, then the interleaved information and parity bits.
     */
    private static boolean[] osw(int address, boolean group, int command)
    {
        int[] info = new int[INFO_BITS];
        int scrambledAddress = address ^ ID_XOR;
        int scrambledCommand = command ^ CMD_XOR;

        for(int x = 0; x < 16; x++)
        {
            info[x] = (scrambledAddress >> (15 - x)) & 1;
        }

        info[16] = group ? 0 : 1;

        for(int x = 0; x < 10; x++)
        {
            info[17 + x] = (scrambledCommand >> (9 - x)) & 1;
        }

        int crc = OswExtractor.computeCrc(info);

        for(int x = 0; x < CRC_BITS; x++)
        {
            info[DATA_BITS + x] = (~(crc >> (CRC_BITS - 1 - x))) & 1;
        }

        int[] coded = new int[PAYLOAD_BITS];

        for(int x = 0; x < INFO_BITS; x++)
        {
            coded[2 * x] = info[x];
            coded[2 * x + 1] = x == 0 ? info[0] : info[x] ^ info[x - 1];
        }

        boolean[] bits = new boolean[SYNC_BITS + PAYLOAD_BITS];

        for(int x = 0; x < SYNC_BITS; x++)
        {
            bits[x] = ((SYNC >> (SYNC_BITS - 1 - x)) & 1) == 1;
        }

        for(int x = 0; x < PAYLOAD_BITS; x++)
        {
            bits[SYNC_BITS + (x % 4) * 19 + (x / 4)] = coded[x] == 1;
        }

        return bits;
    }

    /**
     * Creates a stream of system status OSWs, each with its position in the stream as the address.
     */
    private static boolean[] stream(int count)
    {
        boolean[] bits = new boolean[(count + 1) * (SYNC_BITS + PAYLOAD_BITS)];
        int pointer = 0;

        //One extra OSW so that the last counted OSW is followed by a sync word.
        for(int x = 0; x <= count; x++)
        {
            boolean[] osw = osw(x + 1, false, CMD_SYSTEM_STATUS);
            System.arraycopy(osw, 0, bits, pointer, osw.length);
            pointer += osw.length;
        }

        return bits;
    }

    /**
     * Creates discriminator samples for the bits.
     * @param offset count of idle samples ahead of the first bit, which sets the sample phase of the symbols.
     * @param samplesPerSymbol actual symbol period, to model a transmitter clock that differs from the receiver.
     */
    private static float[] modulate(boolean[] bits, int offset, double samplesPerSymbol)
    {
        float[] samples = new float[offset + (int)(bits.length * samplesPerSymbol)];

        for(int x = offset; x < samples.length; x++)
        {
            int bit = Math.min(bits.length - 1, (int)((x - offset) / samplesPerSymbol));
            samples[x] = bits[bit] ? 1.0f : -1.0f;
        }

        return samples;
    }

    private static List<Integer> decode(float[] samples)
    {
        OswExtractor extractor = new OswExtractor(new Bandplan(BandplanType.EIGHT_HUNDRED_REBANDED));
        extractor.setSampleRate(SAMPLE_RATE);
        List<Integer> addresses = new ArrayList<>();
        extractor.process(samples, message -> {
            if(message instanceof MotorolaTypeIIMessage moto &&
                moto.getMessageType() == MotorolaTypeIIMessageType.SYSTEM_STATUS)
            {
                addresses.add(moto.getAddress());
            }
        });
        return addresses;
    }

    private static void assertInOrderWithoutRepeats(List<Integer> addresses)
    {
        for(int x = 1; x < addresses.size(); x++)
        {
            assertTrue(addresses.get(x) > addresses.get(x - 1), "OSW " + addresses.get(x) + " follows OSW " +
                addresses.get(x - 1));
        }
    }

    @Test
    void decodesEveryOswOnceOnEachSamplePhase()
    {
        int count = 100;

        for(int offset = 0; offset < SAMPLES_PER_SYMBOL; offset++)
        {
            List<Integer> addresses = decode(modulate(stream(count), offset, SAMPLES_PER_SYMBOL));
            assertEquals(count - QUEUE_HOLD, addresses.size(), "sample phase " + offset);
            assertInOrderWithoutRepeats(addresses);
            assertEquals(1, addresses.get(0));
        }
    }

    @Test
    void followsSymbolClockThatDrifts()
    {
        //200 ppm slips a whole symbol every 5,000 symbols, so this stream crosses every sample phase ten times.
        int count = 600;
        List<Integer> addresses = decode(modulate(stream(count), 0, SAMPLES_PER_SYMBOL * 1.0002));
        assertTrue(addresses.size() >= count - QUEUE_HOLD - 5, "decoded " + addresses.size() + " of " + count);
        assertInOrderWithoutRepeats(addresses);
    }

    @Test
    void rejectsCorruptedOswAndResumes()
    {
        int count = 60;
        int corrupted = 30;
        boolean[] bits = stream(count);
        int start = (corrupted - 1) * (SYNC_BITS + PAYLOAD_BITS) + SYNC_BITS;

        //Three bit errors spread across the payload are more than the code corrects.
        bits[start + 3] = !bits[start + 3];
        bits[start + 30] = !bits[start + 30];
        bits[start + 60] = !bits[start + 60];

        List<Integer> addresses = decode(modulate(bits, 2, SAMPLES_PER_SYMBOL));
        assertFalse(addresses.contains(corrupted));
        assertTrue(addresses.contains(count - QUEUE_HOLD), "decoding resumes after the corrupted OSW");
        assertInOrderWithoutRepeats(addresses);
    }
}
