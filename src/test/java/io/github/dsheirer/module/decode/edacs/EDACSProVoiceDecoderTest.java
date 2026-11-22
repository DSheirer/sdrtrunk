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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSProVoiceEndMessage;
import io.github.dsheirer.module.decode.edacs.message.EDACSProVoiceMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import jmbe.frame.CorrectedProVoiceFrame;
import org.junit.jupiter.api.Test;

/**
 * Tests the ProVoice traffic channel framer against a synthesized discriminator signal: 9600 baud two-level data at
 * 5 samples per symbol, one 768-bit ProVoice frame every 80 milliseconds.
 */
class EDACSProVoiceDecoderTest
{
    private static final String SYNC = "13131333111311311133113311331133";
    private static final int FRAME_PERIOD_BITS = 768;
    private static final int MARKER_OFFSET = 464;
    private static final int IV_OFFSET = 64;
    private static final int VOICE_MARKER = 0x2175;
    private static final int SIGNALLING_MARKER = 0xAA55;
    private static final int SAMPLES_PER_SYMBOL = 5;
    private static final int FRAMES = 10;

    /**
     * Creates the bits for a number of consecutive frames with random payloads and the specified marker.
     */
    private static boolean[] frames(int count, int marker)
    {
        Random random = new Random(1);
        boolean[] bits = new boolean[count * FRAME_PERIOD_BITS + 16];

        for(int frame = 0; frame < count; frame++)
        {
            int start = frame * FRAME_PERIOD_BITS;

            for(int x = 0; x < FRAME_PERIOD_BITS; x++)
            {
                bits[start + x] = random.nextBoolean();
            }

            for(int x = 0; x < SYNC.length(); x++)
            {
                bits[start + x] = SYNC.charAt(x) == '3';
            }

            for(int x = 0; x < 16; x++)
            {
                bits[start + MARKER_OFFSET + x] = ((marker >> (15 - x)) & 1) == 1;
            }
        }

        return bits;
    }

    /**
     * Converts bits to discriminator samples, a positive deviation for a set bit, preceded by a number of samples so
     * that the symbols can start on any of the 5 sample phases.
     */
    private static float[] samples(boolean[] bits, int leadingSamples)
    {
        float[] samples = new float[leadingSamples + bits.length * SAMPLES_PER_SYMBOL];

        for(int x = 0; x < bits.length; x++)
        {
            for(int y = 0; y < SAMPLES_PER_SYMBOL; y++)
            {
                samples[leadingSamples + x * SAMPLES_PER_SYMBOL + y] = bits[x] ? 1.0f : -1.0f;
            }
        }

        return samples;
    }

    private static List<IMessage> decode(EDACSProVoiceDecoder decoder, float[] samples)
    {
        List<IMessage> messages = new ArrayList<>();
        decoder.process(samples, messages::add);
        return messages;
    }

    /**
     * Creates frames that carry the IV field value.
     */
    private static boolean[] frames(int count, int marker, long iv)
    {
        boolean[] bits = frames(count, marker);

        for(int frame = 0; frame < count; frame++)
        {
            for(int x = 0; x < 64; x++)
            {
                bits[frame * FRAME_PERIOD_BITS + IV_OFFSET + x] = ((iv >>> (63 - x)) & 1) == 1;
            }
        }

        return bits;
    }

    private static long voiceFrames(List<IMessage> messages)
    {
        return messages.stream().filter(m -> m instanceof EDACSProVoiceMessage && m.isValid()).count();
    }

    @Test
    void decodesEveryFrameOnAnySamplePhase()
    {
        for(int phase = 0; phase < SAMPLES_PER_SYMBOL; phase++)
        {
            List<IMessage> messages = decode(new EDACSProVoiceDecoder(), samples(frames(FRAMES, VOICE_MARKER), phase));
            assertEquals(FRAMES, voiceFrames(messages), "voice frames with the symbols starting on sample phase " + phase);
            assertEquals(FRAMES, messages.stream().filter(m -> m instanceof EDACSProVoiceMessage).count(),
                "each frame should be sent once, not once per phase that decodes it");
        }
    }

    @Test
    void toleratesTwoSyncBitErrorsButNotThree()
    {
        boolean[] twoErrors = frames(FRAMES, VOICE_MARKER);
        int thirdFrame = 3 * FRAME_PERIOD_BITS;
        twoErrors[thirdFrame + 5] = !twoErrors[thirdFrame + 5];
        twoErrors[thirdFrame + 20] = !twoErrors[thirdFrame + 20];
        assertEquals(FRAMES, voiceFrames(decode(new EDACSProVoiceDecoder(), samples(twoErrors, 0))));

        boolean[] threeErrors = twoErrors.clone();
        threeErrors[thirdFrame + 29] = !threeErrors[thirdFrame + 29];
        assertEquals(FRAMES - 1, voiceFrames(decode(new EDACSProVoiceDecoder(), samples(threeErrors, 0))));
    }

    @Test
    void framesWithoutTheVoiceMarkerAreNotVoice()
    {
        List<IMessage> messages = decode(new EDACSProVoiceDecoder(), samples(frames(FRAMES, SIGNALLING_MARKER), 0));
        assertEquals(FRAMES, messages.stream().filter(m -> m instanceof EDACSProVoiceMessage).count(),
            "frames without the marker are still reported");
        assertEquals(0, voiceFrames(messages), "but they are not valid voice frames");
    }

    @Test
    void voiceMarkerIsConfigurable()
    {
        float[] samples = samples(frames(FRAMES, 0x1234), 0);
        assertEquals(0, voiceFrames(decode(new EDACSProVoiceDecoder(), samples)), "default marker");
        assertEquals(FRAMES, voiceFrames(decode(new EDACSProVoiceDecoder(0x1234), samples)), "configured marker");
        assertEquals(FRAMES, voiceFrames(decode(new EDACSProVoiceDecoder(DecodeConfigEDACS.PROVOICE_VOICE_MARKER_NONE),
            samples)), "no marker configured treats every frame as voice");
    }

    @Test
    void markerToleratesThreeBitErrors()
    {
        //0x2175 with 3 bits changed, and with 4 bits changed
        assertEquals(FRAMES, voiceFrames(decode(new EDACSProVoiceDecoder(), samples(frames(FRAMES, 0x2175 ^ 0x0111), 0))));
        assertEquals(0, voiceFrames(decode(new EDACSProVoiceDecoder(), samples(frames(FRAMES, 0x2175 ^ 0x1111), 0))));
    }

    @Test
    void reportsEndOfTransmissionOnDottingAfterVoice()
    {
        boolean[] voice = frames(FRAMES, VOICE_MARKER);
        boolean[] bits = new boolean[voice.length + 400];
        System.arraycopy(voice, 0, bits, 0, voice.length);

        for(int x = voice.length; x < bits.length; x++)
        {
            bits[x] = (x & 1) == 0;
        }

        List<IMessage> messages = decode(new EDACSProVoiceDecoder(), samples(bits, 0));
        assertEquals(FRAMES, voiceFrames(messages));
        assertEquals(1, messages.stream().filter(m -> m instanceof EDACSProVoiceEndMessage).count());
        assertEquals(EDACSProVoiceEndMessage.class, messages.get(messages.size() - 1).getClass(),
            "the end of transmission follows the last voice frame");
    }

    @Test
    void providesCorrectedVoiceFrames()
    {
        List<IMessage> messages = decode(new EDACSProVoiceDecoder(), samples(frames(FRAMES, VOICE_MARKER), 0));
        int checked = 0;

        for(IMessage message : messages)
        {
            if(message instanceof EDACSProVoiceMessage voice && voice.isValid())
            {
                List<CorrectedProVoiceFrame> corrected = voice.getCorrectedImbe7100Frames();
                assertEquals(voice.getPackedImbe7100Frames().length, corrected.size(),
                    "one corrected frame for each voice frame");
                assertSame(corrected, voice.getCorrectedImbe7100Frames(), "the corrected frames are cached");
                checked++;
            }
        }

        assertEquals(FRAMES, checked);
    }

    @Test
    void readsTheEncryptionIvField()
    {
        List<IMessage> clear = decode(new EDACSProVoiceDecoder(),
            samples(frames(FRAMES, VOICE_MARKER, EDACSProVoiceMessage.CLEAR_IV), 0));
        assertEquals(FRAMES, voiceFrames(clear));

        for(IMessage message : clear)
        {
            if(message instanceof EDACSProVoiceMessage voice)
            {
                assertEquals(EDACSProVoiceMessage.CLEAR_IV, voice.getIv());
                assertEquals(0x6969, voice.getLid(), "the LID is the middle of the IV field");
                assertFalse(voice.isEncrypted());
            }
        }

        //Encrypted IV value from a recording of an encrypted call
        List<IMessage> encrypted = decode(new EDACSProVoiceDecoder(),
            samples(frames(FRAMES, VOICE_MARKER, 0x3C22D9F7ED84F6EDL), 0));
        assertEquals(FRAMES, encrypted.stream()
            .filter(m -> m instanceof EDACSProVoiceMessage voice && voice.isEncrypted()).count());
    }

    @Test
    void dottingWithoutVoiceIsNotAnEndOfTransmission()
    {
        boolean[] bits = new boolean[2000];

        for(int x = 0; x < bits.length; x++)
        {
            bits[x] = (x & 1) == 0;
        }

        List<IMessage> messages = decode(new EDACSProVoiceDecoder(), samples(bits, 0));
        assertEquals(0, messages.stream().filter(m -> m instanceof EDACSProVoiceEndMessage).count());
    }
}
