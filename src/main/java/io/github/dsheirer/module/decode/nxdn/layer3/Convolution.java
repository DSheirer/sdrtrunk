/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * NXDN Convolutional decoder
 */
public class Convolution
{
    private static final byte[] BIT_MASK_TABLE = {(byte)0x80, (byte)0x40, (byte)0x20, (byte)0x10, (byte)0x08,
            (byte)0x04, (byte)0x02, (byte)0x01};
    private static final byte[] BRANCH_TABLE_1 = {(byte)0, (byte)0, (byte)0, (byte)0, (byte)2, (byte)2, (byte)2, (byte)2};
    private static final byte[] BRANCH_TABLE_2 = {(byte)0, (byte)2, (byte)2, (byte)0, (byte)0, (byte)2, (byte)2, (byte)0};
    private static final int NUMBER_OF_STATES = 8;
    private static final int M = 4;
    private static final int K = 5;

    public CorrectedBinaryMessage decode(CorrectedBinaryMessage encoded, int length)
    {
        CorrectedBinaryMessage decoded = new CorrectedBinaryMessage(length / 2);
        int decodedPointer = 0;

        int[] oldMetrics = new int[16];
        int[] newMetrics = new int[16];

        int i, j;
        byte s0, s1;
        boolean decision;
        int metric, m0, m1, tmp;

        for(int x = 0; x < length; x += 2)
        {
            s0 = encoded.get(x) ? (byte)1 : (byte)0;
            s1 = encoded.get(x + 1) ? (byte)1 : (byte)0;

            for(i = 0; i < NUMBER_OF_STATES; i++)
            {
                j = i * 2;

                //Processing bit 1
                metric = Math.abs(BRANCH_TABLE_1[i] - s0) + Math.abs(BRANCH_TABLE_2[i] - s1);
                m0 = oldMetrics[i] + metric;
                m1 = oldMetrics[i + NUMBER_OF_STATES] + (M - metric);
                decision = (m0 >= m1);
                newMetrics[j++] = decision ? m1 : m0;

                if(decision)
                {
                    decoded.set(decodedPointer);
                }

                decodedPointer++;

                //Processing bit 2
                m0 = oldMetrics[i] + (M - metric);
                m1 = oldMetrics[i + NUMBER_OF_STATES] + metric;
                decision = (m0 >= m1);
                newMetrics[j] = decision ? m1 : m0;

                if(decision)
                {
                    decoded.set(decodedPointer);
                }

                decodedPointer++;

                //TODO: wip - finish this
            }
        }

        return decoded;

    }
}
