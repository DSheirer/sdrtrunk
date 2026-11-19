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

package io.github.dsheirer.module.decode.smartnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OSW coding, framer, band plan and demodulator.  Values use Massachusetts State Police: Troop A
 * North dispatch talkgroup 33168 (0x8190), Saugus site control channel 857.0375 MHz.
 */
public class SmartNetControlChannelTest
{
    private static int[] payload(int[] frame)
    {
        int[] payload = new int[SmartNetOSWFramer.PAYLOAD_LENGTH];
        System.arraycopy(frame, 8, payload, 0, payload.length);
        return payload;
    }

    /**
     * Test: encode/decode round trip, one corrected bit error, CRC rejection of a damaged command.
     */
    @Test
    void testCoding()
    {
        int[] frame = SmartNetOSWFramer.encode(0x8190, true, 241);
        SmartNetOSW osw = SmartNetOSWFramer.decode(payload(frame), 0);
        assertNotNull(osw);
        assertEquals(0x8190, osw.getAddress());
        assertEquals(33168, osw.getTalkgroup());
        assertTrue(osw.isGroup());
        assertEquals(241, osw.getCommand());

        SmartNetOSW unit = SmartNetOSWFramer.decode(payload(SmartNetOSWFramer.encode(12345, false,
                SmartNetOSW.CMD_UNIT_ID)), 0);
        assertNotNull(unit);
        assertEquals(12345, unit.getAddress());
        assertFalse(unit.isGroup());
        assertEquals(SmartNetOSW.CMD_UNIT_ID, unit.getCommand());

        //A single data bit error is corrected by the parity code
        int[] damaged = payload(frame);
        damaged[20] ^= 1;
        SmartNetOSW corrected = SmartNetOSWFramer.decode(damaged, 0);
        assertNotNull(corrected, "single bit error corrected");
        assertEquals(0x8190, corrected.getAddress());

        //Many errors fail the CRC
        int[] broken = payload(frame);
        for(int x = 0; x < 76; x += 3)
        {
            broken[x] ^= 1;
        }
        assertNull(SmartNetOSWFramer.decode(broken, 0));
    }

    /**
     * Test: status bits in a talkgroup address.
     */
    @Test
    void testStatusBits()
    {
        SmartNetOSW emergency = new SmartNetOSW(0x8192, true, 241, 0);
        assertEquals(0x8190, emergency.getTalkgroup());
        assertTrue(emergency.isEmergency());
        assertFalse(emergency.isEncrypted());
        assertTrue(new SmartNetOSW(0x8198, true, 241, 0).isEncrypted());
    }

    /**
     * Test: MSP channels on the rebanded 800 MHz plan.
     */
    @Test
    void testBandPlan()
    {
        SmartNetBandPlan plan = SmartNetBandPlan.BAND_800_REBANDED;
        assertEquals(857_037_500L, plan.getFrequency(241));       //Saugus control
        assertEquals(859_987_500L, plan.getFrequency(359));       //Boston simulcast control
        assertEquals(851_012_500L, plan.getFrequency(0));
        assertTrue(plan.isChannel(241));
        assertFalse(plan.isChannel(SmartNetOSW.CMD_IDLE));
        assertFalse(plan.isChannel(SmartNetOSW.CMD_UNIT_ID));
    }

    private static List<Boolean> stream(int count)
    {
        List<Boolean> bits = new ArrayList<>();

        for(int f = 0; f < count; f++)
        {
            int[] frame = f % 2 == 0 ? SmartNetOSWFramer.encode(1000 + f, false, SmartNetOSW.CMD_UNIT_ID) :
                    SmartNetOSWFramer.encode(0x8190 + (f << 4), true, 200 + f % 100);
            for(int b : frame)
            {
                bits.add(b == 1);
            }
        }
        return bits;
    }

    /**
     * Test: framer decodes consecutive OSWs in both polarities and needs the next sync.
     */
    @Test
    void testFramer()
    {
        for(boolean invert : new boolean[]{false, true})
        {
            List<SmartNetOSW> osws = new ArrayList<>();
            SmartNetOSWFramer framer = new SmartNetOSWFramer(osws::add);
            Random random = new Random(3);

            for(int x = 0; x < 50; x++)
            {
                framer.process(random.nextBoolean(), 0);
            }
            for(boolean b : stream(20))
            {
                framer.process(b ^ invert, 0);
            }

            assertEquals(19, osws.size(), "the last OSW has no trailing sync");
            assertEquals(1000, osws.get(0).getAddress());
            assertEquals(0x8190 + (1 << 4), osws.get(1).getAddress());
            assertEquals(invert, framer.isInverted());
        }
    }

    /**
     * Test: the framer marks an OSW that does not come straight after the previous decoded OSW.  Frame 6 is damaged
     * beyond repair, so OSW 7 follows OSW 5 with a lost control word in between.
     */
    @Test
    void testFollowsPrevious()
    {
        List<SmartNetOSW> osws = new ArrayList<>();
        SmartNetOSWFramer framer = new SmartNetOSWFramer(osws::add);
        List<Boolean> bits = stream(12);

        for(int x = 20; x < 40; x++)
        {
            int index = 6 * SmartNetOSWFramer.FRAME_LENGTH + x;
            bits.set(index, !bits.get(index));
        }

        for(boolean b : bits)
        {
            framer.process(b, 0);
        }

        assertEquals(10, osws.size(), "12 frames, one damaged, the last without a trailing sync");
        assertEquals(1, framer.getInvalidCount());
        assertFalse(osws.get(0).followsPrevious(), "nothing is known about what came before the first OSW");

        for(int x = 1; x < osws.size(); x++)
        {
            assertEquals(x != 6, osws.get(x).followsPrevious(), "OSW " + x);
        }

        assertEquals(0x8190 + (7 << 4), osws.get(6).getAddress(), "the OSW after the lost one is frame 7");
    }

    /**
     * Test: demodulator at 50 kHz with a clock offset and noise.
     */
    @Test
    void testDemodulator()
    {
        List<SmartNetOSW> osws = new ArrayList<>();
        SmartNetOSWFramer framer = new SmartNetOSWFramer(osws::add);
        double sampleRate = 50_000;
        SmartNetDemodulator demodulator = new SmartNetDemodulator(sampleRate, b -> framer.process(b, 0));

        List<Boolean> bits = stream(200);
        double symbolRate = 3600 * 0.9998;
        int samples = (int)(bits.size() / symbolRate * sampleRate);
        float[] i = new float[samples];
        float[] q = new float[samples];
        double phase = 0;
        Random random = new Random(1);

        for(int n = 0; n < samples; n++)
        {
            int symbol = (int)(n * symbolRate / sampleRate);
            double deviation = bits.get(Math.min(symbol, bits.size() - 1)) ? 1500 : -1500;
            phase += 2 * Math.PI * deviation / sampleRate;
            i[n] = (float)(Math.cos(phase) + random.nextGaussian() * 0.1);
            q[n] = (float)(Math.sin(phase) + random.nextGaussian() * 0.1);
        }

        demodulator.process(i, q, samples);
        assertTrue(osws.size() >= 190, "Decoded " + osws.size() + " of 199 OSWs");
    }

    /**
     * Test: the reception line counts the OSWs that were never found, not only the ones that failed their
     * check.  Numbers of the MSP Central MA recording of 2026-11-14 (6 minutes).
     */
    @Test
    void testStatistics()
    {
        assertEquals("13111 of about 15429 OSWs decoded - 15.0% lost (529 failed the check, 1789 not found)",
            SmartNetDecoder.statistics(13111, 529, 360 * 3600));
        assertEquals("43 of about 43 OSWs decoded - 0.0% lost (0 failed the check, 0 not found)",
            SmartNetDecoder.statistics(43, 0, 3600));
        assertEquals("0 OSWs decoded - no signal", SmartNetDecoder.statistics(0, 12, 216000));
    }
}
