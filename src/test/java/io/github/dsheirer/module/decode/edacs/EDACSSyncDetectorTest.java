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

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSMessage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the control channel slicer and frame processor against a synthesized discriminator signal: 9600 baud
 * two-level data at 5 samples per symbol, one 288-bit frame after another.
 */
class EDACSSyncDetectorTest
{
    private static final long SYNC = 0x555557125555L;
    private static final int SYNC_BITS = 48;
    private static final int WORD_BITS = 40;
    private static final int FRAME_BITS = SYNC_BITS + 6 * WORD_BITS;
    private static final int SAMPLES_PER_SYMBOL = 5;
    private static final int FRAMES = 50;

    /**
     * Creates the bits for a control channel frame: the sync word, then each of the two 28-bit data words as a
     * BCH(40,28) code word sent three times with the middle copy inverted.
     */
    private static boolean[] frame(long data1, long data2)
    {
        boolean[] bits = new boolean[FRAME_BITS];

        for(int x = 0; x < SYNC_BITS; x++)
        {
            bits[x] = ((SYNC >> (SYNC_BITS - 1 - x)) & 1) == 1;
        }

        int offset = SYNC_BITS;

        for(long data : new long[]{data1, data2})
        {
            long codeWord = EDACSVoter.encode(data);

            for(int copy = 0; copy < 3; copy++)
            {
                for(int x = 0; x < WORD_BITS; x++)
                {
                    boolean bit = ((codeWord >> (WORD_BITS - 1 - x)) & 1) == 1;
                    bits[offset++] = (copy == 1) != bit;
                }
            }
        }

        return bits;
    }

    /**
     * Converts repeated frames to discriminator samples.  The slicer reads a negative deviation as a set bit.
     */
    private static float[] samples(boolean[] frame, int frames, int leadingSamples)
    {
        float[] samples = new float[leadingSamples + frames * frame.length * SAMPLES_PER_SYMBOL];

        for(int x = 0; x < frames * frame.length; x++)
        {
            for(int y = 0; y < SAMPLES_PER_SYMBOL; y++)
            {
                samples[leadingSamples + x * SAMPLES_PER_SYMBOL + y] = frame[x % frame.length] ? -1.0f : 1.0f;
            }
        }

        return samples;
    }

    private static List<IMessage> decode(float[] samples)
    {
        return decode(samples, false);
    }

    private static List<IMessage> decode(float[] samples, boolean esk)
    {
        List<IMessage> messages = new ArrayList<>();
        new EDACSSyncDetector(esk).process(samples, messages::add);
        return messages;
    }

    /** ESK mask positioned over the first 8 bits of a 28-bit message */
    private static final long ESK = (long)DecodeConfigEDACS.ESK_MASK << 20;

    /** Digital (ProVoice) group call assignment: MT1 0x03, LCN 5, talkgroup 1234 */
    private static final long DIGITAL_GROUP_CALL = (0x03L << 23) | (5L << 17) | 1234L;

    /** Analog group call assignment: MT1 0x06, LCN 5, talkgroup 1234 */
    private static final long ANALOG_GROUP_CALL = (0x06L << 23) | (5L << 17) | 1234L;

    /** Second message of a group call: source radio 5678 */
    private static final long SOURCE = 5678L;

    private static EDACSMessage first(long data1, long data2, boolean esk)
    {
        return (EDACSMessage)decode(samples(frame(data1, data2), 2, 0), esk).getFirst();
    }

    @Test
    void eskSettingRecoversAMaskedGroupCall()
    {
        //These frames are masked by this test, not captured from an ESK system
        EDACSMessage plain = first(DIGITAL_GROUP_CALL, SOURCE, false);
        EDACSMessage masked = first(DIGITAL_GROUP_CALL ^ ESK, SOURCE ^ ESK, true);

        assertEquals(EDACSMessageType.DIGITAL_GROUP_CALL, plain.getMessageType());
        assertEquals(EDACSMessageType.DIGITAL_GROUP_CALL, masked.getMessageType());
        assertEquals(5, masked.getLCN());
        assertEquals(1234, masked.getGroup());
        assertEquals(5678, masked.getSource());
    }

    @Test
    void wrongEskSettingDoesNotProduceGroupCalls()
    {
        //ESK system with the setting off: MT1 0x03 is sent as 0x17, which is not a known message type
        assertEquals(EDACSMessageType.UNKNOWN, first(DIGITAL_GROUP_CALL ^ ESK, SOURCE ^ ESK, false).getMessageType());

        //ESK system with the setting off: MT1 0x06 is sent as 0x12, which reads as a channel assignment
        assertEquals(EDACSMessageType.CHANNEL_ASSIGNMENT,
                first(ANALOG_GROUP_CALL ^ ESK, SOURCE ^ ESK, false).getMessageType());

        //System without ESK with the setting on
        assertEquals(EDACSMessageType.UNKNOWN, first(DIGITAL_GROUP_CALL, SOURCE, true).getMessageType());
    }

    @Test
    void decodesEachFrameOnceOnAnySamplePhase()
    {
        boolean[] frame = frame(0x1234567L, 0x1234567L);

        for(int phase = 0; phase < SAMPLES_PER_SYMBOL; phase++)
        {
            List<IMessage> messages = decode(samples(frame, FRAMES, phase));
            assertEquals(FRAMES, messages.size(), "messages with the symbols starting on sample phase " + phase);
            assertEquals(FRAMES, messages.stream().filter(m -> m instanceof EDACSMessage && m.isValid()).count());
        }
    }

    @Test
    void votesOutASingleCorruptedCopyOfAWord()
    {
        boolean[] frame = frame(0x1234567L, 0x1234567L);

        //Corrupt bits in the first copy of the first word: the other two copies outvote it
        frame[SYNC_BITS + 3] = !frame[SYNC_BITS + 3];
        frame[SYNC_BITS + 17] = !frame[SYNC_BITS + 17];
        assertEquals(FRAMES, decode(samples(frame, FRAMES, 0)).size());
    }

    @Test
    void rejectsAFrameThatFailsTheParityCheck()
    {
        boolean[] frame = frame(0x1234567L, 0x1234567L);

        //The same bit wrong in two of the three copies of the first word wins the vote and fails the BCH check
        frame[SYNC_BITS + 3] = !frame[SYNC_BITS + 3];
        frame[SYNC_BITS + 2 * WORD_BITS + 3] = !frame[SYNC_BITS + 2 * WORD_BITS + 3];
        assertEquals(0, decode(samples(frame, FRAMES, 0)).size());
    }
}
