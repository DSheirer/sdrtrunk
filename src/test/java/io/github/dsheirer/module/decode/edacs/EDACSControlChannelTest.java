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

package io.github.dsheirer.module.decode.edacs;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BCH, message fields, framer and demodulator.  Message values are real MBTA control channel messages
 * (853.725 MHz, 2026-11-09).
 */
public class EDACSControlChannelTest
{
    /**
     * Test: MBTA messages re-encode to valid codewords, and any single bit error is detected.
     */
    @Test
    void testBCH()
    {
        for(int data : new int[]{0x18B0128, 0x1A0116D, 0xFD60121, 0xFC00000, 0x600C1CA})
        {
            long codeword = EDACSBCH.encode(data);
            assertTrue(EDACSBCH.isValid(codeword));

            for(int bit = 0; bit < 40; bit++)
            {
                assertFalse(EDACSBCH.isValid(codeword ^ (1L << bit)), "bit " + bit);
            }
        }
    }

    /**
     * Test: group voice call fields (Orange Line Wellington Yard 296 on LCN 5, source 4461) per DSD-FME's layout.
     */
    @Test
    void testMessageFields()
    {
        EDACSMessage call = new EDACSMessage(0x18B0128, 0x1A0116D, 0, false);
        assertTrue(call.isGroupVoice());
        assertTrue(call.isDigital());
        assertTrue(call.isUpdate()); //recorded as an update of the call
        assertEquals(5, call.getLCN());
        assertEquals(296, call.getGroup());
        assertEquals(4461, call.getSource());
        assertTrue(call.isTransmissionTrunking());
        assertFalse(call.isEmergency());

        //Update flag, analog type, emergency
        EDACSMessage update = new EDACSMessage(0x3010111, 0x0100010, 0, false);
        assertTrue(update.isGroupVoice());
        assertFalse(update.isDigital());
        assertTrue(update.isUpdate());
        assertEquals(273, update.getGroup());
        assertTrue(update.isEmergency());

        //Without its second message a call has no source and is not used
        assertFalse(new EDACSMessage(0x18B0128, -1, 0, false).isGroupVoice());
        assertFalse(new EDACSMessage(0xFC00000, 0, 0, false).isGroupVoice());
    }

    /**
     * Builds the 288 bits of one frame carrying two messages.
     */
    private static boolean[] frame(int first, int second)
    {
        boolean[] bits = new boolean[288];
        int index = 0;

        for(int x = 47; x >= 0; x--)
        {
            bits[index++] = ((EDACSMessageFramer.SYNC >> x) & 1) == 1;
        }

        for(int data : new int[]{first, second})
        {
            long codeword = EDACSBCH.encode(data);

            for(int copy = 0; copy < 3; copy++)
            {
                for(int x = 39; x >= 0; x--)
                {
                    boolean bit = ((codeword >> x) & 1) == 1;
                    bits[index++] = copy == 1 != bit;
                }
            }
        }

        return bits;
    }

    /**
     * Test: framer finds both polarities, majority-votes a damaged copy and rejects a damaged message.
     */
    @Test
    void testFramer()
    {
        List<EDACSMessage> messages = new ArrayList<>();
        EDACSMessageFramer framer = new EDACSMessageFramer(messages::add);

        boolean[] normal = frame(0x18B0128, 0x1A0116D);
        boolean[] inverted = frame(0x1850111, 0x1A010DA);
        boolean[] damaged = frame(0x1910131, 0x1A00065);
        damaged[48 + 5] ^= true;           //one copy of the first message - majority vote fixes it
        damaged[168 + 3] ^= true;          //two copies of the second message, same bit - rejected
        damaged[168 + 80 + 3] ^= true;

        for(boolean b : new boolean[37])
        {
            framer.process(b, 0);
        }
        for(boolean b : normal)
        {
            framer.process(b, 0);
        }
        for(boolean b : inverted)
        {
            framer.process(!b, 0);
        }
        for(boolean b : damaged)
        {
            framer.process(b, 0);
        }

        //Frames: normal, inverted, damaged (first message voted good, second rejected)
        assertEquals(3, messages.size());
        assertEquals(296, messages.get(0).getGroup());
        assertEquals(4461, messages.get(0).getSource());
        assertEquals(273, messages.get(1).getGroup());
        assertEquals(305, messages.get(2).getGroup());
        assertFalse(messages.get(2).hasMessage2());
        assertTrue(messages.get(2).isCorrected());
        assertEquals(1, framer.getInvalidCount());
    }

    /**
     * Test: with an EDACS System Key (ESK) the top 8 bits of each message are XORed with the key on the air; the
     * framer removes it after the BCH check.  Without the key set, the same frames decode to the wrong values.
     */
    @Test
    void testSystemKey()
    {
        int key = 0xA0 << 20;
        boolean[] keyed = frame(0x18B0128 ^ key, 0x1A0116D ^ key);

        List<EDACSMessage> withKey = new ArrayList<>();
        EDACSMessageFramer keyedFramer = new EDACSMessageFramer(withKey::add);
        keyedFramer.setSystemKey(0xA0);

        List<EDACSMessage> withoutKey = new ArrayList<>();
        EDACSMessageFramer plainFramer = new EDACSMessageFramer(withoutKey::add);

        for(boolean b : keyed)
        {
            keyedFramer.process(b, 0);
            plainFramer.process(b, 0);
        }

        assertEquals(1, withKey.size());
        assertEquals(296, withKey.get(0).getGroup());
        assertEquals(4461, withKey.get(0).getSource());

        assertEquals(1, withoutKey.size(), "the keyed frame still passes the BCH check");
        assertEquals(0x18B0128 ^ key, withoutKey.get(0).getMessage1());
    }

    /**
     * Test: demodulator recovers frames from synthetic 9600 baud FSK at 50 kHz with a slight clock offset and noise.
     */
    @Test
    void testDemodulator()
    {
        List<EDACSMessage> messages = new ArrayList<>();
        EDACSMessageFramer framer = new EDACSMessageFramer(messages::add);
        double sampleRate = 50_000;
        EDACSDemodulator demodulator = new EDACSDemodulator(sampleRate, b -> framer.process(b, 0));

        List<Boolean> bits = new ArrayList<>();
        for(int f = 0; f < 50; f++)
        {
            for(boolean b : frame(0x1800000 | ((f % 14 + 1) << 17) | (273 + f), 0x1A00000 | (4000 + f)))
            {
                bits.add(!b); //transmitted inverted, like MBTA
            }
        }

        double symbolRate = 9600 * 1.0002; //200 ppm clock offset
        int samples = (int)(bits.size() / symbolRate * sampleRate);
        float[] i = new float[samples];
        float[] q = new float[samples];
        double phase = 0;
        java.util.Random random = new java.util.Random(1);

        for(int n = 0; n < samples; n++)
        {
            int symbol = (int)(n * symbolRate / sampleRate);
            double deviation = bits.get(Math.min(symbol, bits.size() - 1)) ? 2400 : -2400;
            phase += 2 * Math.PI * deviation / sampleRate;
            i[n] = (float)(Math.cos(phase) + random.nextGaussian() * 0.1);
            q[n] = (float)(Math.sin(phase) + random.nextGaussian() * 0.1);
        }

        demodulator.process(i, q, samples);

        assertTrue(messages.size() >= 48, "Decoded " + messages.size() + " of 50 frames");
        assertTrue(messages.stream().anyMatch(m -> m.getGroup() == 273 + 10 && m.getSource() == 4010),
            "Frame 10 decoded");
    }
}
