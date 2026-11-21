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

import io.github.dsheirer.message.IMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A voice frame that passes the error check with corrected errors is not played when another
 * voice frame of the same 80 ms frame failed the check.  The frames are clean voice recorded from MBTA on 2026-11-09
 * (transmission 0 of {@link ProVoiceHeaderDetectorTest}'s fixture), damaged here one bit at a time.
 */
public class ProVoiceSuspectTest
{
    private static final List<int[]> VOICE = new ArrayList<>();

    @BeforeAll
    static void load() throws Exception
    {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(ProVoiceSuspectTest.class
            .getResourceAsStream("/edacs/provoice_transmission_starts.txt"), StandardCharsets.US_ASCII)))
        {
            String line;

            while((line = reader.readLine()) != null)
            {
                String[] fields = line.split(" ");

                if(fields[0].equals("0"))
                {
                    int[] bits = new int[fields[3].length()];

                    for(int x = 0; x < bits.length; x++)
                    {
                        bits[x] = fields[3].charAt(x) - '0';
                    }

                    VOICE.add(bits);
                }
            }
        }
    }

    /**
     * Flips bits of the first pair of voice frames, one at a time, until the given voice frame has the wanted number
     * of counted errors and nothing else in the frame has changed.
     */
    private static void damage(int[] frame, int voiceFrame, int wanted)
    {
        for(int bit = 144; bit < ProVoiceFrameDecoder.FIRST_PAIR_END; bit++)
        {
            ProVoiceFrameDecoder.VoiceFrame[] before = ProVoiceFrameDecoder.decode(frame);

            if(before[voiceFrame].errors() >= wanted)
            {
                return;
            }

            frame[bit] ^= 1;
            ProVoiceFrameDecoder.VoiceFrame[] after = ProVoiceFrameDecoder.decode(frame);
            boolean keep = after[voiceFrame].errors() == before[voiceFrame].errors() + 1;

            for(int x = 0; x < 4; x++)
            {
                keep &= x == voiceFrame || after[x].errors() == before[x].errors();
            }

            if(!keep)
            {
                frame[bit] ^= 1;
            }
        }

        assertTrue(ProVoiceFrameDecoder.decode(frame)[voiceFrame].errors() >= wanted, "could not build the frame");
    }

    /**
     * Three clean voice frames, then the frame under test.
     * @return the decoder, and in received the number of concealed voice frames of each message
     */
    private static ProVoiceDecoder decode(int[] frame, List<Integer> concealed)
    {
        ProVoiceDecoder decoder = new ProVoiceDecoder();
        decoder.setMessageListener((IMessage message) -> concealed.add(((ProVoiceMessage)message).getConcealed()));
        List<int[]> frames = new ArrayList<>(VOICE.subList(0, 3));
        frames.add(frame);

        for(int[] bits : frames)
        {
            for(int x = 31; x >= 0; x--)
            {
                decoder.process(((ProVoiceDecoder.SYNC_EA >> x) & 1) == 1);
            }

            for(int bit : bits)
            {
                decoder.process(bit == 1);
            }
        }

        return decoder;
    }

    @Test
    void aVoiceFrameWithErrorsBesideOneThatFailedIsNotPlayed()
    {
        int[] frame = VOICE.get(3).clone();
        damage(frame, 0, ProVoiceDecoder.MAX_ERRORS + 1);
        damage(frame, 1, ProVoiceDecoder.SUSPECT_ERRORS);
        ProVoiceFrameDecoder.VoiceFrame[] decoded = ProVoiceFrameDecoder.decode(frame);
        assertTrue(decoded[0].errors() > ProVoiceDecoder.MAX_ERRORS);
        assertEquals(ProVoiceDecoder.SUSPECT_ERRORS, decoded[1].errors());

        List<Integer> concealed = new ArrayList<>();
        ProVoiceDecoder decoder = decode(frame, concealed);
        assertEquals(List.of(0, 0, 0, 2), concealed, "The failed voice frame and the suspect one are concealed");
        assertEquals(1, decoder.getSuspectCount());
    }

    @Test
    void aVoiceFrameWithErrorsInAnOtherwiseGoodFrameIsPlayed()
    {
        int[] frame = VOICE.get(3).clone();
        damage(frame, 1, ProVoiceDecoder.MAX_ERRORS);

        List<Integer> concealed = new ArrayList<>();
        ProVoiceDecoder decoder = decode(frame, concealed);
        assertEquals(List.of(0, 0, 0, 0), concealed);
        assertEquals(0, decoder.getSuspectCount());
    }

    @Test
    void oneCorrectedErrorBesideAFailedVoiceFrameIsStillPlayed()
    {
        int[] frame = VOICE.get(3).clone();
        damage(frame, 0, ProVoiceDecoder.MAX_ERRORS + 1);
        damage(frame, 1, ProVoiceDecoder.SUSPECT_ERRORS - 1);

        List<Integer> concealed = new ArrayList<>();
        ProVoiceDecoder decoder = decode(frame, concealed);
        assertEquals(List.of(0, 0, 0, 1), concealed, "Only the failed voice frame is concealed");
        assertEquals(0, decoder.getSuspectCount());
    }
}
