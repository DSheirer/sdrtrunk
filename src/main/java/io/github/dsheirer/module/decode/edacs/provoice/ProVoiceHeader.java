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

import io.github.dsheirer.module.decode.edacs.EDACSBCH;

/**
 * Reads the header a ProVoice transmission starts with.
 *
 * The header is one block sent over and over for 0.3 to 0.5 seconds: a 64-bit sync and then a 48-bit word sent three
 * times - the word, the word inverted, the word again - which is how EDACS sends its control channel messages too.
 * A frame of the voice channel holds three of those triples, at bits {@link #TRIPLES} after the frame sync.
 *
 * The word is 36 data bits followed by 12 check bits of the same BCH code the control channel uses
 * ({@link EDACSBCH}).  Its data starts with {@link #HEADER_ID} and holds the talkgroup the transmission is for in
 * bits 14 to 29.  Measured on MBTA 2026-11-12, both sites: the talkgroup of the header is the talkgroup the
 * control channel assigned the call to in 68 of 68 recorded calls.  The six data bits after the talkgroup change
 * with the talkgroup and are not understood; the calling radio is not in the word.
 *
 * A frame is a header only when the three copies of a word agree, its check bits are right and it starts with the
 * header value, so voice is not taken for a header: in the 71 calls recorded that day the 282 frames that read as
 * one are all in the first six frames of a transmission, or at the start of the one transmission that followed
 * another without a gap - none is inside the voice.  A header that does not have this layout - another system
 * type - is not recognized here and is left to {@link ProVoiceHeaderDetector}, as before.
 */
public final class ProVoiceHeader
{
    /** First bit of each word-inverse-word triple in a frame */
    static final int[] TRIPLES = {32, 240, 448};
    static final int WORD_BITS = 48;
    /** First eight bits of the header word */
    public static final int HEADER_ID = 0xA8;
    /** Bits of a triple's three copies allowed to differ from the majority.  Most seen in a header that could be
     * read: 15, at Summit 800; three unrelated runs of bits differ in 36 on average. */
    static final int MAX_DISAGREEMENT = 24;
    public static final int NO_HEADER = -1;

    private ProVoiceHeader()
    {
    }

    /**
     * Reads the talkgroup from a header frame.
     * @param frame bits, without the sync
     * @return talkgroup, or {@link #NO_HEADER} if the frame is not a header
     */
    public static int talkgroup(int[] frame)
    {
        for(int start : TRIPLES)
        {
            long word = word(frame, start);

            if(word >= 0)
            {
                return (int)((word >> 18) & 0xFFFF);
            }
        }

        return NO_HEADER;
    }

    /**
     * Majority vote of the three copies of the word starting at the bit; -1 if they do not make a header word.
     */
    static long word(int[] frame, int start)
    {
        if(start + 3 * WORD_BITS > frame.length)
        {
            return -1;
        }

        long word = 0;
        int disagreement = 0;

        for(int x = 0; x < WORD_BITS; x++)
        {
            int first = frame[start + x];
            int second = 1 - frame[start + WORD_BITS + x];
            int third = frame[start + 2 * WORD_BITS + x];
            int sum = first + second + third;
            int bit = sum >= 2 ? 1 : 0;
            disagreement += bit == 1 ? 3 - sum : sum;
            word = (word << 1) | bit;
        }

        if(disagreement > MAX_DISAGREEMENT || (word >>> 40) != HEADER_ID || syndrome(word) != 0)
        {
            return -1;
        }

        return word;
    }

    /**
     * Remainder of the 48-bit word divided by the EDACS generator - zero for a valid word.
     */
    static int syndrome(long word)
    {
        long v = word & 0xFFFFFFFFFFFFL;

        for(int bit = WORD_BITS - 1; bit >= EDACSBCH.CHECK_BITS; bit--)
        {
            if(((v >> bit) & 1) == 1)
            {
                v ^= ((long)EDACSBCH.GENERATOR) << (bit - EDACSBCH.CHECK_BITS);
            }
        }

        return (int)v;
    }
}
