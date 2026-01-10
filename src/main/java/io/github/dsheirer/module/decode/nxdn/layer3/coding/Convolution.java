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

package io.github.dsheirer.module.decode.nxdn.layer3.coding;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import java.util.Arrays;

/**
 * NXDN Convolutional encoder and Viterbi decoder for Rate 1/2, K=5
 */
public class Convolution
{
    private static final int M = 4;
    private static final int[] COST_TABLE_0 = {0, 0, 0, 0, 2, 2, 2, 2};
    private static final int[] COST_TABLE_1 = {0, 2, 2, 0, 0, 2, 2, 0};
    private static final int STATES_COUNT = 8;

    /**
     * Decodes the 1/2 rate, K=5 encoded message.
     * @param encoded message
     * @param length of the encoded message
     * @return decoded message
     */
    public static CorrectedBinaryMessage decode(CorrectedBinaryMessage encoded, PunctureProvider punctureProvider)
    {
        int length = encoded.size();
        int position = 0;
        int[] previousMetrics = new int[STATES_COUNT * 2];
        int[] currentMetrics = new int[STATES_COUNT * 2];
        int[] history = new int[length / 2];

        for(int i = 0; i < length; i += 2)
        {
            int s0 = encoded.get(i) ? 2 : 0;
            int s1 = encoded.get(i + 1) ? 2 : 0;

            for(int j = 0; j < STATES_COUNT; j++)
            {
                int metric = Math.abs(COST_TABLE_0[j] - s0) + Math.abs(COST_TABLE_1[j] - s1);
                int m0 = previousMetrics[j] + metric;
                int m1 = previousMetrics[j + STATES_COUNT] + (M - metric);
                int m2 = previousMetrics[j] + (M - metric);
                int m3 = previousMetrics[j + STATES_COUNT] + metric;
                int i0 = 2 * j;
                int i1 = i0 + 1;

                if(m0 >= m1)
                {
                    history[position] |= (1 << i0);
                    currentMetrics[i0] = m1;
                }
                else
                {
                    history[position] &= ~(1 << i0);
                    currentMetrics[i0] = m0;
                }

                if(m2 >= m3)
                {
                    history[position] |= (1 << i1);
                    currentMetrics[i1] = m3;
                }
                else
                {
                    history[position] &= ~(1 << i1);
                    currentMetrics[i1] = m2;
                }
            }

            //swap
            int[] temp = Arrays.copyOf(currentMetrics, currentMetrics.length);
            currentMetrics = previousMetrics;
            previousMetrics = temp;
            position++;
        }

        //Output is half of the input length
        length /= 2;
        CorrectedBinaryMessage decoded = new CorrectedBinaryMessage(length);

        int state = 0, i, bit;

        while(length-- > M)
        {
            i = state >> M;
            bit = (history[length] >> i) & 1;
            state = ((bit << 7) & 0xFF) | (state >> 1);
            decoded.set(length - M, bit != 0);
        }

        //Calculate bit errors by re-encoding the decoded message, compare to original, and ignore punctured bits
        CorrectedBinaryMessage recoded = encode(decoded);
        int bitErrors = 0;

        for(int x = 0; x < encoded.size(); x++)
        {
            if(punctureProvider.isPreserved(x) && (encoded.get(x) ^ recoded.get(x)))
            {
                bitErrors++;
            }
        }

        decoded.setCorrectedBitCount(bitErrors);
        return decoded;
    }

    /**
     * Encodes the message
     * @param message to encode
     * @return encoded message
     */
    public static CorrectedBinaryMessage encode(CorrectedBinaryMessage message)
    {
        CorrectedBinaryMessage encoded = new CorrectedBinaryMessage(message.size() * 2);

        int g1, g2, d, d1 = 0, d2 = 0, d3 = 0, d4 = 0, k = 0;

        for(int x = 0; x < message.size(); x++)
        {
            d = message.get(x) ? 1 : 0;
            g1 = (d + d3 + d4) & 1;
            g2 = (d + d1 + d2 + d4) & 1;
            d4 = d3;
            d3 = d2;
            d2 = d1;
            d1 = d;

            encoded.set(k++, g1 != 0);
            encoded.set(k++, g2 != 0);
        }

        return encoded;
    }

    static void main()
    {
        //Test of decoding and encoding of CAC bits.
        String encodedCAC = "00000011100011101001100010000000011110101100000000000000111000000010100010001110100110101101011000010001011100011110001000101001001010001000111000001000010010011100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001001011000100000001001101011001110101000";
        CorrectedBinaryMessage cbm = CorrectedBinaryMessage.load(encodedCAC);
        PunctureProvider puncture = new PunctureProviderCACAndFACCH2();
        CorrectedBinaryMessage decoded = Convolution.decode(cbm, puncture);
        System.out.println("    DECODED: " + decoded);
        System.out.println(" BIT ERRORS: " + decoded.getCorrectedBitCount());
        System.out.println("  PUNCTURED: " + puncture.visualize(cbm));
        System.out.println("TRANSMITTED: " + encodedCAC);
        System.out.println(" RE-ENCODED: " + encode(decoded));
        System.out.println(" DECODED LENGTH: " + decoded.size());
        boolean passes = NXDNCRC.checkCAC(decoded);
        System.out.println("Passes: " + passes);
    }
}
