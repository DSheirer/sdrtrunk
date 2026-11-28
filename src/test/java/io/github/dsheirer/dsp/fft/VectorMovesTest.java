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
package io.github.dsheirer.dsp.fft;

import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The vector data moves match the scalar loops bit for bit, including the partial vectors and blocks at the ends.
 */
public class VectorMovesTest
{
    private static float[] random(int length, Random random)
    {
        float[] values = new float[length];

        for(int x = 0; x < length; x++)
        {
            values[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        return values;
    }

    @Test
    void interleaveMatchesScalar()
    {
        assumeTrue(VectorComplexFFT.isVectorSupported());
        Random random = new Random(1);

        for(int count : new int[]{0, 1, 3, 4, 7, 8, 9, 16, 17, 31, 96, 100, 2187})
        {
            for(int offset : new int[]{0, 1, 5})
            {
                float[] samples = random(offset + 2 * count + 3, random);
                int base = 2;
                float[] i = new float[base + count + 1];
                float[] q = new float[base + count + 1];
                float[] expectedI = i.clone();
                float[] expectedQ = q.clone();

                for(int x = 0; x < count; x++)
                {
                    expectedI[base + x] = samples[offset + 2 * x];
                    expectedQ[base + x] = samples[offset + 2 * x + 1];
                }

                VectorMoves.deinterleave(samples, offset, i, q, base, count);
                assertArrayEquals(expectedI, i, "deinterleave inphase count " + count);
                assertArrayEquals(expectedQ, q, "deinterleave quadrature count " + count);

                float gain = 0.37f;
                float[] expected = random(samples.length, random);
                float[] actual = expected.clone();

                for(int x = 0; x < count; x++)
                {
                    expected[offset + 2 * x] = i[base + x] * gain;
                    expected[offset + 2 * x + 1] = q[base + x] * gain;
                }

                VectorMoves.interleave(i, q, base, gain, actual, offset, count);
                assertArrayEquals(expected, actual, "interleave count " + count);
            }
        }
    }

    @Test
    void transposeMatchesScalar()
    {
        assumeTrue(VectorComplexFFT.isVectorSupported());
        Random random = new Random(2);

        for(int rows : new int[]{1, 3, 4, 5, 8, 12, 17, 64, 96})
        {
            for(int columns : new int[]{1, 4, 7, 8, 9, 16, 33, 100})
            {
                int sStride = columns + 3;
                int dStride = rows + 5;
                int sBase = 2;
                int dBase = 7;
                float[] s = random(sBase + rows * sStride, random);
                float[] expected = random(dBase + columns * dStride, random);
                float[] actual = expected.clone();

                for(int r = 0; r < rows; r++)
                {
                    for(int c = 0; c < columns; c++)
                    {
                        expected[dBase + c * dStride + r] = s[sBase + r * sStride + c];
                    }
                }

                VectorMoves.transpose(s, sBase, sStride, actual, dBase, dStride, rows, columns);
                assertArrayEquals(expected, actual, "transpose rows " + rows + " columns " + columns);
            }
        }
    }
}
