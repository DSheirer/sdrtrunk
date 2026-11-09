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

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for AMBE voice frame error detection.
 */
public class AMBEFrameValidatorTest
{
    //Base station voice frame A burst from a recording where JMBE decodes all three voice frames with zero errors.
    private static final String VOICE_BURST = "5A3AC4B9E881526173002A6BB9E881526755FD7DF75F7173002A6BB9E881526173002A6B";
    //Burst bit indices of the first C0 bits (voice frame bits 0, 4, 8) and first C1 bit (voice frame bit 25) of
    //frame 1, which starts at burst bit 24.
    private static final int FRAME_1_C0_BIT_0 = 24;
    private static final int FRAME_1_C0_BIT_1 = 28;
    private static final int FRAME_1_C0_BIT_2 = 32;
    private static final int FRAME_1_C1_BIT_0 = 49;

    private static CorrectedBinaryMessage getBurst()
    {
        return CorrectedBinaryMessage.loadHex(VOICE_BURST);
    }

    @Test
    void errorFreeBurst()
    {
        CorrectedBinaryMessage burst = getBurst();

        for(int frame = 0; frame < 3; frame++)
        {
            assertEquals(0, AMBEFrameValidator.getErrorCount(burst, frame), "Frame " + frame);
        }

        assertTrue(AMBEFrameValidator.isValid(burst));
    }

    @Test
    void errorsAreCounted()
    {
        CorrectedBinaryMessage burst = getBurst();
        burst.flip(FRAME_1_C0_BIT_0);
        assertEquals(1, AMBEFrameValidator.getErrorCount(burst, 0));
        burst.flip(FRAME_1_C1_BIT_0);
        assertEquals(2, AMBEFrameValidator.getErrorCount(burst, 0));
        burst.flip(FRAME_1_C0_BIT_1);
        assertEquals(3, AMBEFrameValidator.getErrorCount(burst, 0));
    }

    @Test
    void fourC0ErrorsAreDetected()
    {
        CorrectedBinaryMessage burst = getBurst();
        burst.flip(FRAME_1_C0_BIT_0);
        burst.flip(FRAME_1_C0_BIT_1);
        burst.flip(FRAME_1_C0_BIT_2);
        burst.flip(FRAME_1_C0_BIT_2 + 4);
        assertTrue(AMBEFrameValidator.getErrorCount(burst, 0) > AMBEFrameValidator.MAX_ERRORS);
    }

    @Test
    void validWhenAnyFrameIsValid()
    {
        CorrectedBinaryMessage burst = getBurst();

        //Corrupt the C0 vector of each frame in turn (4 bit errors), verifying a single valid frame is sufficient.
        int[] frameOffsets = {24, 96, 216};

        for(int frame = 0; frame < 3; frame++)
        {
            for(int bit = 0; bit < 16; bit += 4)
            {
                burst.flip(frameOffsets[frame] + bit);
            }

            assertTrue(AMBEFrameValidator.getErrorCount(burst, frame) > AMBEFrameValidator.MAX_ERRORS);
            assertEquals(frame < 2, AMBEFrameValidator.isValid(burst), "Corrupted frames: " + (frame + 1));
        }
    }

    @Test
    void randomBurstsAreRarelyValid()
    {
        Random random = new Random(1234);
        int valid = 0;
        int trials = 20000;

        for(int trial = 0; trial < trials; trial++)
        {
            CorrectedBinaryMessage burst = new CorrectedBinaryMessage(288);

            for(int bit = 0; bit < 288; bit++)
            {
                if(random.nextBoolean())
                {
                    burst.set(bit);
                }
            }

            if(AMBEFrameValidator.isValid(burst))
            {
                valid++;
            }
        }

        //Expected false valid rate is ~3 x 1.3e-4 (ie ~8 of 20,000)
        assertTrue(valid < 30, "Random bursts detected as valid: " + valid);
    }

    @Test
    void voiceMessageValidity()
    {
        assertTrue(new VoiceAMessage(DMRSyncPattern.BASE_STATION_VOICE, getBurst(), null, 0, 1).isValid());

        CorrectedBinaryMessage corrupted = getBurst();
        int[] frameOffsets = {24, 96, 216};

        for(int offset : frameOffsets)
        {
            for(int bit = 0; bit < 16; bit += 4)
            {
                corrupted.flip(offset + bit);
            }
        }

        assertFalse(new VoiceAMessage(DMRSyncPattern.BASE_STATION_VOICE, corrupted, null, 0, 1).isValid());
    }

    /**
     * Verifies the AMBE frames match the burst bits, specifically for frame 2 where bits 32-35 are sourced from burst
     * bits 128-131 and bits 36-39 from burst bits 180-183, with burst bit 180 set (ie a negative signed byte).
     */
    @Test
    void ambeFramesMatchBurstBits()
    {
        CorrectedBinaryMessage burst = getBurst();
        burst.set(180);
        burst.clear(128, 132);
        List<byte[]> frames = new VoiceAMessage(DMRSyncPattern.BASE_STATION_VOICE, burst, null, 0, 1).getAMBEFrames();
        int[] frameOffsets = {24, 96, 216};

        for(int frame = 0; frame < 3; frame++)
        {
            for(int bit = 0; bit < 72; bit++)
            {
                int burstIndex = frameOffsets[frame] + bit + ((frame == 1 && bit >= 36) ? 48 : 0);
                boolean frameBit = (frames.get(frame)[bit / 8] & (0x80 >> (bit % 8))) != 0;
                assertEquals(burst.get(burstIndex), frameBit, "Frame " + frame + " bit " + bit);
            }
        }
    }
}
