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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The sign-off at the end of a ProVoice transmission and the flag bit, against real frames (MBTA,
 * recorded 2026-11-09 and 2026-11-12).  Each line of the resource is: case, the errors of the frame's four voice
 * frames as an earlier version counted them, and the 736 frame bits.
 */
public class ProVoiceSignOffTest
{
    private static final Map<String,List<int[]>> CASES = new LinkedHashMap<>();

    @BeforeAll
    static void load() throws Exception
    {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(ProVoiceSignOffTest.class
            .getResourceAsStream("/edacs/provoice_transmission_ends.txt"), StandardCharsets.US_ASCII)))
        {
            String line;

            while((line = reader.readLine()) != null)
            {
                String[] fields = line.split(" ");
                int[] bits = new int[fields[2].length()];

                for(int x = 0; x < bits.length; x++)
                {
                    bits[x] = fields[2].charAt(x) - '0';
                }

                CASES.computeIfAbsent(fields[0], t -> new ArrayList<>()).add(bits);
            }
        }

        assertEquals(4, CASES.size());
    }

    /**
     * Runs the frames through a decoder as one transmission that is already in voice.
     * @return voice frames (20 ms) per message, then the decoder
     */
    private static Object[] decode(String name)
    {
        ProVoiceDecoder decoder = new ProVoiceDecoder();
        List<Integer> played = new ArrayList<>();
        decoder.setMessageListener((IMessage message) -> played.add(((ProVoiceMessage)message).getIMBEFrames().size()));

        for(int[] frame : CASES.get(name))
        {
            for(int x = 31; x >= 0; x--)
            {
                decoder.process(((ProVoiceDecoder.SYNC_EA >> x) & 1) == 1);
            }

            for(int bit : frame)
            {
                decoder.process(bit == 1);
            }
        }

        return new Object[]{played, decoder};
    }

    @Test
    void signOffStartingAfterTheFirstPairKeepsTheFirstPair()
    {
        //Errors 0000 0000 0000 0088, dotting from bit 496: the last frame's first two voice frames are voice
        Object[] result = decode("clean-then-half");
        ProVoiceDecoder decoder = (ProVoiceDecoder)result[1];

        assertEquals(List.of(4, 4, 4, 2), result[0]);
        assertEquals(2, decoder.getSignOffCount());
        assertEquals(0, decoder.getConcealedCount(), "The sign-off is not concealed as damaged voice");
    }

    @Test
    void aLastFrameThatIsAllSignOffIsNotPlayed()
    {
        //0000 0000 0000 8899 with 160 bits of dotting
        Object[] result = decode("all-bad-end");
        ProVoiceDecoder decoder = (ProVoiceDecoder)result[1];

        assertEquals(List.of(4, 4, 4), result[0]);
        assertEquals(4, decoder.getSignOffCount());
        assertEquals(0, decoder.getConcealedCount());
    }

    @Test
    void theFramesBetweenTheFirstDottingAndTheEndAreNotPlayed()
    {
        //Three voice frames, then 4699 with dotting at bit 431, then seven frames of other data - one of them with a
        //voice frame that passes the error check by chance - before the carrier drops
        Object[] result = decode("junk-after-dotting");
        ProVoiceDecoder decoder = (ProVoiceDecoder)result[1];

        assertEquals(List.of(4, 4, 4, 1), result[0], "Nothing is played after the dotting starts");
        //The frames after the sign-off repeat one another, as the header does, and the header rule takes those
        assertEquals(3 + 7 * 4, decoder.getSignOffCount() + 4 * decoder.getHeaderCount());
        assertEquals(0, decoder.getConcealedCount(), "Nothing of the sign-off is concealed as damaged voice");
    }

    @Test
    void cleanVoiceWithARunOfAlternatingBitsIsPlayed()
    {
        //The second frame holds 17 alternating bits and every voice frame in it is clean
        assertTrue(ProVoiceDecoder.dottingStart(CASES.get("clean-with-run").get(1)) >= 0);

        Object[] result = decode("clean-with-run");
        ProVoiceDecoder decoder = (ProVoiceDecoder)result[1];

        assertEquals(List.of(4, 4, 4, 4, 4), result[0]);
        assertEquals(0, decoder.getSignOffCount());
    }

    @Test
    void voiceAfterAFalseSignOffIsPlayedAgain()
    {
        //A sign-off followed by clean voice in the same transmission
        ProVoiceDecoder decoder = new ProVoiceDecoder();
        List<Integer> played = new ArrayList<>();
        decoder.setMessageListener((IMessage message) -> played.add(((ProVoiceMessage)message).getIMBEFrames().size()));
        List<int[]> frames = new ArrayList<>(CASES.get("all-bad-end"));
        frames.addAll(CASES.get("clean-with-run"));

        for(int[] frame : frames)
        {
            for(int x = 31; x >= 0; x--)
            {
                decoder.process(((ProVoiceDecoder.SYNC_EA >> x) & 1) == 1);
            }

            for(int bit : frame)
            {
                decoder.process(bit == 1);
            }
        }

        assertEquals(List.of(4, 4, 4, 4, 4, 4, 4, 4), played);
    }

    @Test
    void theFlagBitIsNotAnError()
    {
        //Transmission 5 of the header fixture: every voice frame was counted with 1 or 2 errors - the flag bit in the
        //first and second code words - on a 60 dB signal
        int errors = 0;
        int voiceFrames = 0;

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(ProVoiceSignOffTest.class
            .getResourceAsStream("/edacs/provoice_transmission_starts.txt"), StandardCharsets.US_ASCII)))
        {
            String line;

            while((line = reader.readLine()) != null)
            {
                String[] fields = line.split(" ");

                if(fields[0].equals("5") && Integer.parseInt(fields[1]) >= 6)
                {
                    int[] bits = new int[fields[3].length()];

                    for(int x = 0; x < bits.length; x++)
                    {
                        bits[x] = fields[3].charAt(x) - '0';
                    }

                    for(ProVoiceFrameDecoder.VoiceFrame voiceFrame : ProVoiceFrameDecoder.decode(bits))
                    {
                        errors += voiceFrame.errors();
                        voiceFrames++;
                    }
                }
            }
        }
        catch(Exception e)
        {
            throw new IllegalStateException(e);
        }

        assertEquals(24, voiceFrames);
        assertEquals(0, errors, "Build 15 counted 32 errors in these 24 voice frames, all of them the flag bit");
    }
}
