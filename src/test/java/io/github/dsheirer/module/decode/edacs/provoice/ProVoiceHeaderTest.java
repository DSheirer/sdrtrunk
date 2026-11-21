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
 * Reading the header of a ProVoice transmission.  Header words are the ones MBTA sent on
 * 2026-11-12 for talkgroups the control channel named (273, 280, 533, 561); the frames are the first frames of five
 * real transmissions recorded 2026-11-09 (see {@link ProVoiceHeaderDetectorTest}).
 */
public class ProVoiceHeaderTest
{
    private static final long WORD_273 = 0xA80004473BBFL;
    private static final long WORD_280 = 0xA8000462A434L;
    private static final long WORD_533 = 0xA80008560454L;
    private static final long WORD_561 = 0xA80008C7F8B7L;
    private static final Map<Integer,List<int[]>> TRANSMISSIONS = new LinkedHashMap<>();

    @BeforeAll
    static void load() throws Exception
    {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(ProVoiceHeaderTest.class
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
    }

    /**
     * A header frame as it is sent: the word, the word inverted, the word, at each of the three places.  The bits
     * in between are left zero.
     */
    static int[] headerFrame(long word)
    {
        int[] frame = new int[ProVoiceFrameDecoder.FRAME_BITS];

        for(int start : ProVoiceHeader.TRIPLES)
        {
            for(int x = 0; x < ProVoiceHeader.WORD_BITS; x++)
            {
                int bit = (int)((word >> (ProVoiceHeader.WORD_BITS - 1 - x)) & 1);
                frame[start + x] = bit;
                frame[start + ProVoiceHeader.WORD_BITS + x] = 1 - bit;
                frame[start + 2 * ProVoiceHeader.WORD_BITS + x] = bit;
            }
        }

        return frame;
    }

    @Test
    void theTalkgroupIsReadFromTheHeaderWord()
    {
        assertEquals(273, ProVoiceHeader.talkgroup(headerFrame(WORD_273)));
        assertEquals(280, ProVoiceHeader.talkgroup(headerFrame(WORD_280)));
        assertEquals(533, ProVoiceHeader.talkgroup(headerFrame(WORD_533)));
        assertEquals(561, ProVoiceHeader.talkgroup(headerFrame(WORD_561)));
    }

    @Test
    void theHeaderWordsAreCodeWords()
    {
        for(long word : new long[]{WORD_273, WORD_280, WORD_533, WORD_561})
        {
            assertEquals(0, ProVoiceHeader.syndrome(word));
            assertTrue(ProVoiceHeader.syndrome(word ^ 0x40000) != 0, "One wrong bit is noticed");
        }
    }

    @Test
    void bitErrorsInOneCopyAreOutvoted()
    {
        int[] frame = headerFrame(WORD_280);

        //Ten wrong bits in the first copy of the first triple, the whole second triple destroyed
        for(int x = 0; x < 10; x++)
        {
            frame[32 + 3 * x] ^= 1;
        }

        for(int x = 240; x < 240 + 144; x += 2)
        {
            frame[x] ^= 1;
        }

        assertEquals(280, ProVoiceHeader.talkgroup(frame));
    }

    @Test
    void aWordThatFailsTheCheckIsNotAHeader()
    {
        int[] frame = headerFrame(WORD_273);

        //The same talkgroup bit wrong in every copy: the copies agree, the check bits do not
        for(int start : ProVoiceHeader.TRIPLES)
        {
            frame[start + 29] ^= 1;
            frame[start + 48 + 29] ^= 1;
            frame[start + 96 + 29] ^= 1;
        }

        assertEquals(ProVoiceHeader.NO_HEADER, ProVoiceHeader.talkgroup(frame));
        assertEquals(ProVoiceHeader.NO_HEADER, ProVoiceHeader.talkgroup(new int[ProVoiceFrameDecoder.FRAME_BITS]));
    }

    @Test
    void recordedHeadersAreReadAndRecordedVoiceIsNot()
    {
        int headers = 0;

        for(Map.Entry<Integer,List<int[]>> transmission : TRANSMISSIONS.entrySet())
        {
            int talkgroup = ProVoiceHeader.NO_HEADER;

            for(int[] frame : transmission.getValue())
            {
                int read = ProVoiceHeader.talkgroup(frame);

                if(read != ProVoiceHeader.NO_HEADER)
                {
                    headers++;
                    assertTrue(talkgroup == ProVoiceHeader.NO_HEADER || talkgroup == read,
                        "Every header of a transmission names the same talkgroup");
                    talkgroup = read;
                }
            }

            //Transmission 0 was joined when it was already in voice
            assertEquals(transmission.getKey() != 0, talkgroup != ProVoiceHeader.NO_HEADER,
                "transmission " + transmission.getKey());
        }

        assertTrue(headers >= 12, "headers read: " + headers);
    }

    /**
     * Feeds frames to a decoder with no gap between them.
     * @return what the decoder sent: "H" + talkgroup for a header, the number of voice frames for voice
     */
    private static List<String> decode(ProVoiceDecoder decoder, List<int[]> frames)
    {
        List<String> received = new ArrayList<>();
        decoder.setMessageListener((IMessage message) -> received.add(
            message instanceof ProVoiceHeaderMessage header ? "H" + header.getTalkgroup() :
                String.valueOf(((ProVoiceMessage)message).getIMBEFrames().size())));

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

        return received;
    }

    @Test
    void aTransmissionThatFollowsAnotherWithoutAGapIsToldApartByItsHeader()
    {
        //Five frames of voice (transmission 0), then at once three header frames for another talkgroup and voice
        List<int[]> frames = new ArrayList<>(TRANSMISSIONS.get(0).subList(0, 5));
        frames.add(headerFrame(WORD_533));
        frames.add(headerFrame(WORD_533));
        frames.add(headerFrame(WORD_533));
        frames.addAll(TRANSMISSIONS.get(0).subList(0, 5));

        ProVoiceDecoder decoder = new ProVoiceDecoder();
        List<String> received = decode(decoder, frames);

        assertEquals(List.of("4", "4", "4", "4", "4", "H533", "4", "4", "4", "4", "4"), received);
        assertEquals(1, decoder.getBackToBackCount());
        assertEquals(3, decoder.getHeaderCount());
        assertEquals(0, decoder.getConcealedCount(), "The header is not concealed as damaged voice");
    }

    @Test
    void theHeaderIsReportedOncePerTransmission()
    {
        List<int[]> frames = new ArrayList<>();

        for(int x = 0; x < 4; x++)
        {
            frames.add(headerFrame(WORD_273));
        }

        frames.addAll(TRANSMISSIONS.get(0).subList(0, 3));

        ProVoiceDecoder decoder = new ProVoiceDecoder();
        assertEquals(List.of("H273", "4", "4", "4"), decode(decoder, frames));
        assertEquals(0, decoder.getBackToBackCount());
    }
}
