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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The header at the start of a ProVoice transmission, against the first frames of five real
 * transmissions (MBTA LCN 5, 851.9375 MHz, recorded 2026-11-09 22:04, demodulated by EDACSDemodulator).  Each line of
 * the resource is: transmission, frame number, the corrected errors of the frame's four voice frames as the decoder
 * reported them when the fixture was made, and the 736 frame bits.
 */
public class ProVoiceHeaderDetectorTest
{
    private static final Map<Integer,List<int[]>> TRANSMISSIONS = new LinkedHashMap<>();

    @BeforeAll
    static void load() throws Exception
    {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(ProVoiceHeaderDetectorTest.class
            .getResourceAsStream("/edacs/provoice_transmission_starts.txt"), StandardCharsets.US_ASCII)))
        {
            String line;

            while((line = reader.readLine()) != null)
            {
                String[] fields = line.split(" ");
                int[] bits = new int[fields[3].length()];

                for(int x = 0; x < bits.length; x++)
                {
                    bits[x] = fields[3].charAt(x) - '0';
                }

                TRANSMISSIONS.computeIfAbsent(Integer.parseInt(fields[0]), t -> new ArrayList<>()).add(bits);
            }
        }

        assertEquals(5, TRANSMISSIONS.size());
        assertEquals(ProVoiceFrameDecoder.FRAME_BITS, TRANSMISSIONS.get(3).get(0).length);
    }

    private static int[] errors(int[] frame)
    {
        ProVoiceFrameDecoder.VoiceFrame[] voiceFrames = ProVoiceFrameDecoder.decode(frame);
        int[] errors = new int[voiceFrames.length];

        for(int x = 0; x < errors.length; x++)
        {
            errors[x] = voiceFrames[x].errors();
        }

        return errors;
    }

    /**
     * Number of frames at the start of the transmission that the detector calls header.  Fails if a frame after the
     * first voice frame is called header.
     */
    private static int headerFrames(int transmission)
    {
        ProVoiceHeaderDetector detector = new ProVoiceHeaderDetector();
        detector.reset();
        int headers = 0;
        boolean voice = false;

        for(int[] frame : TRANSMISSIONS.get(transmission))
        {
            if(detector.isHeader(frame, errors(frame)))
            {
                assertFalse(voice, "Header after voice in transmission " + transmission);
                headers++;
            }
            else
            {
                voice = true;
            }
        }

        return headers;
    }

    @Test
    void headerRepeatedThreeTimesThenOneOtherFrame()
    {
        //Voice frame errors 6987 6987 6987 7865, then clean voice
        assertEquals(4, headerFrames(3));
        assertEquals(4, headerFrames(4));
    }

    @Test
    void headerRepeatedFourTimesThenOneOtherFrame()
    {
        //7998 x4, 9687, then voice
        assertEquals(5, headerFrames(5));
    }

    @Test
    void headerRepeatedFiveTimesThenTwoOtherFrames()
    {
        //7577 x5 - one voice frame of each passes the error check by chance and used to be played - 7569, 8688
        assertEquals(7, headerFrames(2));
    }

    @Test
    void joiningATransmissionAlreadyInVoiceLosesNothing()
    {
        assertEquals(0, headerFrames(0));
    }

    @Test
    void theHeaderFramesAreNoiseWhenReadAsVoice()
    {
        //What made the noise: header frames with a voice frame at or under the error limit
        int passing = 0;

        for(int x = 0; x < 5; x++)
        {
            for(int error : errors(TRANSMISSIONS.get(2).get(x)))
            {
                if(error <= ProVoiceDecoder.MAX_ERRORS)
                {
                    passing++;
                }
            }
        }

        assertEquals(5, passing, "One voice frame in each of the five header repeats passes the check");
    }

    @Test
    void afterTheWindowEverythingIsVoice()
    {
        ProVoiceHeaderDetector detector = new ProVoiceHeaderDetector();
        detector.reset();
        int[] frame = TRANSMISSIONS.get(0).get(0);
        int[] clean = {0, 0, 0, 0};

        for(int x = 0; x < ProVoiceHeaderDetector.WINDOW_FRAMES; x++)
        {
            detector.isHeader(TRANSMISSIONS.get(0).get(x % 5), clean);
        }

        //The same frame twice, and damaged: inside the window that is header, after it is voice to be concealed
        assertFalse(detector.isHeader(frame, new int[]{9, 9, 9, 9}));
        assertFalse(detector.isHeader(frame, new int[]{9, 9, 9, 9}));
    }

    @Test
    void aWeakStartIsNotSilencedForLong()
    {
        //First frame damaged, then frames with one bad voice frame of four: those are voice
        ProVoiceHeaderDetector detector = new ProVoiceHeaderDetector();
        detector.reset();
        List<int[]> voice = TRANSMISSIONS.get(0);

        assertTrue(detector.isHeader(voice.get(0), new int[]{7, 1, 6, 0}));
        assertFalse(detector.isHeader(voice.get(1), new int[]{0, 8, 1, 2}));
        assertFalse(detector.isHeader(voice.get(2), new int[]{9, 9, 9, 9}), "Once voice has started, damage is concealed");
    }

    @Test
    void theDecoderDoesNotPlayTheHeader()
    {
        ProVoiceDecoder decoder = new ProVoiceDecoder();
        List<IMessage> messages = new ArrayList<>();
        List<IMessage> headers = new ArrayList<>();
        decoder.setMessageListener(message -> (message instanceof ProVoiceHeaderMessage ? headers : messages)
            .add(message));
        int frames = 0;

        for(int transmission : new int[]{3, 2, 0})
        {
            //A gap longer than a quarter of a second starts a new transmission
            for(int x = 0; x < 3000; x++)
            {
                decoder.process(false);
            }

            for(int[] frame : TRANSMISSIONS.get(transmission))
            {
                for(int x = 31; x >= 0; x--)
                {
                    decoder.process(((ProVoiceDecoder.SYNC_EA >> x) & 1) == 1);
                }

                for(int bit : frame)
                {
                    decoder.process(bit == 1);
                }

                frames++;
            }
        }

        assertEquals(29, frames);
        assertEquals(4 + 7, decoder.getHeaderCount());
        assertEquals(frames - 11, messages.size());
        assertEquals(2, headers.size(), "The header of each transmission that has one is read, once");
        assertEquals(0, decoder.getConcealedCount(), "With the header out of the way no voice frame here is damaged");

        for(IMessage message : messages)
        {
            for(byte[] imbe : ((ProVoiceMessage)message).getIMBEFrames())
            {
                assertTrue(imbe != null, "No silence is inserted in clean voice");
            }
        }
    }
}
