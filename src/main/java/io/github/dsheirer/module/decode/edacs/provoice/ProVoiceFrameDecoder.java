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

import java.util.ArrayList;
import java.util.List;

/**
 * Unpacks a ProVoice voice frame into four IMBE 7100x4400 voice frames, corrects them and
 * converts each to the 88 parameter bits of a P25 (IMBE 7200x4400) voice frame.
 *
 * A ProVoice frame is 768 bits at 9600 baud (80 ms): 32-bit sync, 144 header bits, two voice frames interleaved,
 * 18 bits, two more voice frames interleaved, 2 bits.  The bit placement follows DSD-FME's provoice.c (GPL).
 *
 * The voice frame error correction (Golay 23/12, Hamming 15/11), descrambling and the 7100 to 7200 bit order
 * conversion are ported from mbelib's imbe7100x4400.c and ecc.c:
 * <pre>
 * Copyright (C) 2010 mbelib Author
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby
 * granted, provided that the above copyright notice and this permission notice appear in all copies.
 * THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
 * </pre>
 */
public class ProVoiceFrameDecoder
{
    /** Bits after the sync */
    public static final int FRAME_BITS = 736;
    /** First bit after the first pair of voice frames (144 + 286) */
    public static final int FIRST_PAIR_END = 430;
    /** First bit of the second pair of voice frames */
    public static final int SECOND_PAIR_START = 448;
    /** Bit of the first two code words of a voice frame that some radios send inverted as a flag - not an error */
    static final int FLAG_BIT = 17;

    private static final int[] W = {0, 1, 2, 3, 4, 6, 0, 1, 2, 3, 4, 6, 0, 1, 2, 3, 4, 6, 0, 1, 2, 3, 5, 6, 0, 1, 2, 3, 5,
        6, 0, 1, 2, 3, 5, 6, 0, 1, 3, 4, 5, 6, 1, 2, 3, 4, 5, 6, 0, 1, 2, 3, 4, 6, 0, 1, 2, 3, 4, 6, 0, 1, 2, 3, 4, 6, 0, 1,
        2, 3, 5, 6, 0, 1, 2, 3, 5, 6, 0, 1, 2, 3, 5, 6, 1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 5, 0, 1, 2, 3, 4, 6, 0, 1, 2, 3, 4, 6,
        0, 1, 2, 3, 5, 6, 0, 1, 2, 3, 5, 6, 0, 1, 2, 3, 5, 6, 0, 1, 2, 4, 5, 6, 1, 2, 3, 4, 5, 6, 1, 2, 3, 4, 6};
    private static final int[] X = {18, 18, 17, 16, 7, 21, 15, 15, 14, 13, 4, 18, 12, 12, 11, 10, 1, 15, 9, 9, 8, 7, 13,
        12, 6, 6, 5, 4, 10, 9, 3, 3, 2, 1, 7, 6, 0, 0, 22, 13, 4, 3, 21, 20, 19, 10, 1, 0, 17, 17, 16, 15, 6, 20, 14, 14,
        13, 12, 3, 17, 11, 11, 10, 9, 0, 14, 8, 8, 7, 6, 12, 11, 5, 5, 4, 3, 9, 8, 2, 2, 1, 0, 6, 5, 23, 22, 21, 12, 3, 2,
        20, 19, 18, 9, 0, 16, 16, 15, 14, 5, 19, 13, 13, 12, 11, 2, 16, 10, 10, 9, 8, 14, 13, 7, 7, 6, 5, 11, 10, 4, 4, 3,
        2, 8, 7, 1, 1, 0, 14, 5, 4, 22, 21, 20, 11, 2, 1, 19, 18, 17, 8, 22};
    private static final int[] GOLAY_GENERATOR = {0x63a, 0x31d, 0x7b4, 0x3da, 0x1ed, 0x6cc, 0x366, 0x1b3, 0x6e3, 0x54b,
        0x49f, 0x475};
    private static final int[] HAMMING_GENERATOR = {0x7ac8, 0x3d64, 0x1eb2, 0x7591};
    private static final int[] GOLAY_CORRECTION = new int[2048];
    /** Hamming (15,11): the bit to flip for each syndrome */
    private static final int[] HAMMING_CORRECTION = new int[16];

    static
    {
        //Each single wrong bit gives its own syndrome; the table maps the syndrome back to the bit.
        //The code this was ported from (mbelib) flipped bit (syndrome - 1), which is the wrong bit for 11 of the 15
        //positions: it left the wrong bit in place and damaged a second one.  mbelib-neo corrects with a table too.
        for(int bit = 0; bit < 15; bit++)
        {
            HAMMING_CORRECTION[hammingSyndrome(1 << bit)] = 1 << bit;
        }

        //Golay (23,12) is a perfect code: each of the 2048 syndromes belongs to exactly one error pattern of weight 3 or
        //less.  mbelib ships this as a table (golayMatrix); here it is generated.
        List<Integer> patterns = new ArrayList<>();
        patterns.add(0);

        for(int a = 0; a < 23; a++)
        {
            patterns.add(1 << a);

            for(int b = a + 1; b < 23; b++)
            {
                patterns.add((1 << a) | (1 << b));

                for(int c = b + 1; c < 23; c++)
                {
                    patterns.add((1 << a) | (1 << b) | (1 << c));
                }
            }
        }

        for(int pattern : patterns)
        {
            GOLAY_CORRECTION[expectedParity(pattern) ^ (pattern & 0x7FF)] = pattern >> 11;
        }
    }

    /**
     * One decoded voice frame
     *
     * @param parameters 88 parameter bits (0/1) in P25 (IMBE 7200x4400) order
     * @param errors corrected bit errors
     */
    public record VoiceFrame(int[] parameters, int errors) {}

    private ProVoiceFrameDecoder()
    {
    }

    /**
     * Decodes the four voice frames of a ProVoice frame.
     *
     * @param bits 736 bits (0/1) following the sync
     * @return four voice frames in time order
     */
    public static VoiceFrame[] decode(int[] bits)
    {
        int[][][] frames = new int[4][7][24];
        int[] pointer = {144};
        unpackPair(bits, pointer, frames[0], frames[1]);
        pointer[0] += 18;
        unpackPair(bits, pointer, frames[2], frames[3]);

        VoiceFrame[] voiceFrames = new VoiceFrame[4];

        for(int x = 0; x < 4; x++)
        {
            voiceFrames[x] = decodeVoiceFrame(frames[x]);
        }

        return voiceFrames;
    }

    private static void unpackPair(int[] bits, int[] p, int[][] first, int[][] second)
    {
        int w = 0;

        for(int i = 0; i < 11; i++)
        {
            w = fill(bits, p, first, second, w, 6);
        }

        for(int j = 0; j < 6; j++)
        {
            first[W[w + j]][X[w + j]] = bits[p[0]++];
        }
        for(int j = 0; j < 4; j++)
        {
            second[W[w + j]][X[w + j]] = bits[p[0]++];
        }
        p[0] += 2;
        for(int j = 4; j < 6; j++)
        {
            second[W[w + j]][X[w + j]] = bits[p[0]++];
        }
        w += 6;

        for(int i = 0; i < 3; i++)
        {
            w = fill(bits, p, first, second, w, 6);
        }

        w = fill(bits, p, first, second, w, 5);

        for(int i = 0; i < 7; i++)
        {
            w = fill(bits, p, first, second, w, 6);
        }

        fill(bits, p, first, second, w, 5);
    }

    private static int fill(int[] bits, int[] p, int[][] first, int[][] second, int w, int count)
    {
        for(int j = 0; j < count; j++)
        {
            first[W[w + j]][X[w + j]] = bits[p[0]++];
        }
        for(int j = 0; j < count; j++)
        {
            second[W[w + j]][X[w + j]] = bits[p[0]++];
        }
        return w + count;
    }

    private static int expectedParity(int block)
    {
        int mask = 0x400000;
        int parity = 0;

        for(int i = 0; i < 12; i++)
        {
            if((block & mask) != 0)
            {
                parity ^= GOLAY_GENERATOR[i];
            }
            mask >>= 1;
        }

        return parity;
    }

    /**
     * mbe_golay2312: in[22..11] data, in[10..0] parity; corrected data written to out
     */
    private static int golay(int[] in, int[] out)
    {
        return golay(in, out, -1);
    }

    /**
     * As above, without counting a correction of the flag bit.
     *
     * Some radios send bit {@link #FLAG_BIT} of the first two code words of every voice frame inverted, for a whole
     * call - MBTA 2026-11-12, 33 calls recorded at Cambridge at 55-68 dB: 11 calls had no corrected bit in any voice
     * frame, about 10 had this one bit corrected in the first code word, the second or both in every voice frame, and
     * of 1,570 voice frames with a correction 1,112 and 732 were this bit.  The code words that follow descramble and
     * decode with no errors once it is corrected, so the correction is right and the voice is not altered - but
     * counted as errors the flag used up one or two of the five allowed in every voice frame of those calls.
     * @param flagBit index of the bit whose correction is not an error, or -1
     */
    private static int golay(int[] in, int[] out, int flagBit)
    {
        int block = 0;

        for(int i = 22; i >= 0; i--)
        {
            block = (block << 1) + in[i];
        }

        int data = (block >> 11) ^ GOLAY_CORRECTION[expectedParity(block) ^ (block & 0x7FF)];

        for(int i = 22; i >= 11; i--)
        {
            out[i] = (data & 2048) >> 11;
            data <<= 1;
        }
        System.arraycopy(in, 0, out, 0, 11);

        int errors = 0;
        for(int i = 22; i >= 11; i--)
        {
            if(out[i] != in[i] && i != flagBit)
            {
                errors++;
            }
        }
        return errors;
    }

    private static int hammingSyndrome(int block)
    {
        int syndrome = 0;

        for(int i = 0; i < 4; i++)
        {
            syndrome = (syndrome << 1) | (Integer.bitCount(block & HAMMING_GENERATOR[i]) & 1);
        }

        return syndrome;
    }

    /**
     * mbe_7100x4400hamming1511, with the correction by table (see HAMMING_CORRECTION)
     */
    static int hamming(int[] in, int[] out)
    {
        int block = 0;

        for(int i = 14; i >= 0; i--)
        {
            block = (block << 1) | in[i];
        }

        int syndrome = hammingSyndrome(block);
        int errors = 0;

        if(syndrome > 0)
        {
            errors = 1;
            block ^= HAMMING_CORRECTION[syndrome];
        }

        for(int i = 14; i >= 0; i--)
        {
            out[i] = (block & 0x4000) >> 14;
            block <<= 1;
        }

        return errors;
    }

    private static VoiceFrame decodeVoiceFrame(int[][] fr)
    {
        int[] in = new int[23];
        int[] out = new int[23];

        //C0
        for(int j = 0; j < 18; j++)
        {
            in[j] = fr[0][j + 1];
        }
        int errors = golay(in, out, FLAG_BIT);
        for(int j = 0; j < 18; j++)
        {
            fr[0][j + 1] = out[j];
        }

        //Descramble C1-C5 with the sequence seeded from C0
        int seed = 0;
        for(int i = 18; i > 11; i--)
        {
            seed = (seed << 1) | fr[0][i];
        }
        int[] pr = new int[101];
        pr[0] = 16 * seed;
        for(int i = 1; i < 101; i++)
        {
            pr[i] = (173 * pr[i - 1] + 13849) % 65536;
        }
        int k = 1;
        for(int j = 23; j >= 0; j--)
        {
            fr[1][j] ^= pr[k++] / 32768;
        }
        for(int i = 2; i < 4; i++)
        {
            for(int j = 22; j >= 0; j--)
            {
                fr[i][j] ^= pr[k++] / 32768;
            }
        }
        for(int i = 4; i < 6; i++)
        {
            for(int j = 14; j >= 0; j--)
            {
                fr[i][j] ^= pr[k++] / 32768;
            }
        }

        int[] d = new int[88];
        int n = 0;

        for(int j = 18; j > 11; j--)
        {
            d[n++] = fr[0][j];
        }
        for(int j = 0; j < 23; j++)
        {
            in[j] = fr[1][j + 1];
        }
        errors += golay(in, out, FLAG_BIT);
        for(int j = 22; j > 10; j--)
        {
            d[n++] = out[j];
        }
        for(int i = 2; i < 4; i++)
        {
            System.arraycopy(fr[i], 0, in, 0, 23);
            errors += golay(in, out);
            for(int j = 22; j > 10; j--)
            {
                d[n++] = out[j];
            }
        }
        int[] hin = new int[15];
        int[] hout = new int[15];
        for(int i = 4; i < 6; i++)
        {
            System.arraycopy(fr[i], 0, hin, 0, 15);
            errors += hamming(hin, hout);
            for(int j = 14; j >= 4; j--)
            {
                d[n++] = hout[j];
            }
        }
        for(int j = 22; j >= 0; j--)
        {
            d[n++] = fr[6][j];
        }

        return new VoiceFrame(convert7100to7200(d), errors);
    }

    /**
     * mbe_convertImbe7100to7200: reorders the parameter bits into the P25 order
     */
    static int[] convert7100to7200(int[] d)
    {
        int b0 = (d[1] << 7) | (d[2] << 6) | (d[3] << 5) | (d[4] << 4) | (d[5] << 3) | (d[6] << 2) | (d[86] << 1) | d[87];
        float w0 = (float)(4 * Math.PI) / ((float)b0 + 39.5f);
        int L = (int)(0.9254 * (int)((Math.PI / w0) + 0.25));
        int K = L < 37 ? (int)((float)(L + 2) / 3f) : 12;

        int[] t = new int[88];
        t[87] = d[0];
        t[48 + K] = d[42];
        t[49 + K] = d[43];

        for(int i = 0, j = 48, k = 44; i < K; i++)
        {
            t[j++] = d[k++];
        }

        int j = 0;
        int k = 1;

        while(j < 87)
        {
            t[j] = d[k];

            if(++j == 48)
            {
                j += K + 2;
            }
            if(++k == 42)
            {
                k += K + 2;
            }
        }

        return t;
    }
}
