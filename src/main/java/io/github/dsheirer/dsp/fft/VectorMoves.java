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

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector (SIMD) data movement for the vector FFT: splitting interleaved complex samples into inphase and quadrature
 * arrays and back, and matrix transposes.  Every method gives the same result as the obvious scalar loop, bit for bit.
 *
 * The lane moves are two-vector selects (selectFrom): zip (interleave the lower or upper halves of two vectors) and
 * unzip (the even or odd lanes of two vectors).  They are written as selectFrom calls rather than small helper
 * methods: the JIT can leave a helper out of a large method (NodeCountInliningCutoff), which boxes its vectors.  A transpose of a LANES x LANES block is log2(LANES) rounds of zips, written out
 * for 4 and 8 lanes.  Other widths use the scalar loops for transposes.
 */
final class VectorMoves
{
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
    private static final int LANES = SPECIES.length();
    private static final FloatVector ZIP_LO;
    private static final FloatVector ZIP_HI;
    private static final FloatVector UNZIP_EVEN;
    private static final FloatVector UNZIP_ODD;
    private static final FloatVector REVERSE;

    static
    {
        //Zip lo / hi: a0, b0, a1, b1 ... from the lower / upper halves.  Unzip: the even / odd lanes of a then b.
        int[] zipLo = new int[LANES];
        int[] zipHi = new int[LANES];
        int[] even = new int[LANES];
        int[] odd = new int[LANES];

        for(int x = 0; x < LANES; x++)
        {
            zipLo[x] = (x % 2 == 0 ? 0 : LANES) + x / 2;
            zipHi[x] = (x % 2 == 0 ? 0 : LANES) + LANES / 2 + x / 2;
            even[x] = 2 * x;
            odd[x] = 2 * x + 1;
        }

        ZIP_LO = indexes(zipLo);
        ZIP_HI = indexes(zipHi);
        UNZIP_EVEN = indexes(even);
        UNZIP_ODD = indexes(odd);
        int[] reverse = new int[LANES];

        for(int x = 0; x < LANES; x++)
        {
            reverse[x] = LANES - 1 - x;
        }

        REVERSE = indexes(reverse);
    }

    private VectorMoves()
    {
    }

    /**
     * Index vector for a two-vector selectFrom: index s selects lane s of the first vector for s < LANES, and lane
     * s - LANES of the second.
     */
    private static FloatVector indexes(int[] indexes)
    {
        float[] values = new float[indexes.length];

        for(int x = 0; x < indexes.length; x++)
        {
            values[x] = indexes[x];
        }

        return FloatVector.fromArray(SPECIES, values, 0);
    }

    /**
     * Splits interleaved complex samples: i[base + x] = samples[offset + 2x], q[base + x] = samples[offset + 2x + 1].
     */
    static void deinterleave(float[] samples, int offset, float[] i, float[] q, int base, int count)
    {
        int x = 0;

        for(; x <= count - LANES; x += LANES)
        {
            int s = offset + 2 * x;
            FloatVector a = FloatVector.fromArray(SPECIES, samples, s);
            FloatVector b = FloatVector.fromArray(SPECIES, samples, s + LANES);
            UNZIP_EVEN.selectFrom(a, b).intoArray(i, base + x);
            UNZIP_ODD.selectFrom(a, b).intoArray(q, base + x);
        }

        for(; x < count; x++)
        {
            i[base + x] = samples[offset + 2 * x];
            q[base + x] = samples[offset + 2 * x + 1];
        }
    }

    /**
     * Interleaves complex samples with a gain: samples[offset + 2x] = i[base + x] * gain, samples[offset + 2x + 1] =
     * q[base + x] * gain.
     */
    static void interleave(float[] i, float[] q, int base, float gain, float[] samples, int offset, int count)
    {
        int x = 0;

        for(; x <= count - LANES; x += LANES)
        {
            int s = offset + 2 * x;
            FloatVector a = FloatVector.fromArray(SPECIES, i, base + x).mul(gain);
            FloatVector b = FloatVector.fromArray(SPECIES, q, base + x).mul(gain);
            ZIP_LO.selectFrom(a, b).intoArray(samples, s);
            ZIP_HI.selectFrom(a, b).intoArray(samples, s + LANES);
        }

        for(; x < count; x++)
        {
            samples[offset + 2 * x] = i[base + x] * gain;
            samples[offset + 2 * x + 1] = q[base + x] * gain;
        }
    }

    /**
     * Indicates if the transposes are vectorized at this vector width.
     */
    static boolean isTransposeVectorized()
    {
        return LANES == 4 || LANES == 8;
    }

    /**
     * Transpose: d[dBase + c * dStride + r] = s[sBase + r * sStride + c] for r < rows and c < columns.
     */
    static void transpose(float[] s, int sBase, int sStride, float[] d, int dBase, int dStride, int rows,
                          int columns)
    {
        int r0 = 0;

        if(isTransposeVectorized())
        {
            int fullRows = rows - rows % LANES;
            int fullColumns = columns - columns % LANES;

            for(int r = 0; r < fullRows; r += LANES)
            {
                for(int c = 0; c < fullColumns; c += LANES)
                {
                    if(LANES == 4)
                    {
                        transpose4(s, sBase + r * sStride + c, sStride, d, dBase + c * dStride + r, dStride);
                    }
                    else
                    {
                        transpose8(s, sBase + r * sStride + c, sStride, d, dBase + c * dStride + r, dStride);
                    }
                }

                //Columns that are not a whole block
                for(int c = fullColumns; c < columns; c++)
                {
                    for(int x = r; x < r + LANES; x++)
                    {
                        d[dBase + c * dStride + x] = s[sBase + x * sStride + c];
                    }
                }
            }

            r0 = fullRows;
        }

        //Rows that are not a whole block
        for(int c = 0; c < columns; c++)
        {
            for(int r = r0; r < rows; r++)
            {
                d[dBase + c * dStride + r] = s[sBase + r * sStride + c];
            }
        }
    }

    /**
     * Transposes a 4 x 4 block: d[d0 + c * ds + r] = s[s0 + r * ss + c].
     */
    private static void transpose4(float[] s, int s0, int ss, float[] d, int d0, int ds)
    {
        FloatVector a0 = FloatVector.fromArray(SPECIES, s, s0);
        FloatVector a1 = FloatVector.fromArray(SPECIES, s, s0 + ss);
        FloatVector a2 = FloatVector.fromArray(SPECIES, s, s0 + 2 * ss);
        FloatVector a3 = FloatVector.fromArray(SPECIES, s, s0 + 3 * ss);
        FloatVector b0 = ZIP_LO.selectFrom(a0, a2);
        FloatVector b1 = ZIP_HI.selectFrom(a0, a2);
        FloatVector b2 = ZIP_LO.selectFrom(a1, a3);
        FloatVector b3 = ZIP_HI.selectFrom(a1, a3);
        ZIP_LO.selectFrom(b0, b2).intoArray(d, d0);
        ZIP_HI.selectFrom(b0, b2).intoArray(d, d0 + ds);
        ZIP_LO.selectFrom(b1, b3).intoArray(d, d0 + 2 * ds);
        ZIP_HI.selectFrom(b1, b3).intoArray(d, d0 + 3 * ds);
    }

    /**
     * Transposes an 8 x 8 block: d[d0 + c * ds + r] = s[s0 + r * ss + c].
     */
    private static void transpose8(float[] s, int s0, int ss, float[] d, int d0, int ds)
    {
        FloatVector a0 = FloatVector.fromArray(SPECIES, s, s0);
        FloatVector a1 = FloatVector.fromArray(SPECIES, s, s0 + ss);
        FloatVector a2 = FloatVector.fromArray(SPECIES, s, s0 + 2 * ss);
        FloatVector a3 = FloatVector.fromArray(SPECIES, s, s0 + 3 * ss);
        FloatVector a4 = FloatVector.fromArray(SPECIES, s, s0 + 4 * ss);
        FloatVector a5 = FloatVector.fromArray(SPECIES, s, s0 + 5 * ss);
        FloatVector a6 = FloatVector.fromArray(SPECIES, s, s0 + 6 * ss);
        FloatVector a7 = FloatVector.fromArray(SPECIES, s, s0 + 7 * ss);
        FloatVector b0 = ZIP_LO.selectFrom(a0, a4);
        FloatVector b1 = ZIP_HI.selectFrom(a0, a4);
        FloatVector b2 = ZIP_LO.selectFrom(a1, a5);
        FloatVector b3 = ZIP_HI.selectFrom(a1, a5);
        FloatVector b4 = ZIP_LO.selectFrom(a2, a6);
        FloatVector b5 = ZIP_HI.selectFrom(a2, a6);
        FloatVector b6 = ZIP_LO.selectFrom(a3, a7);
        FloatVector b7 = ZIP_HI.selectFrom(a3, a7);
        FloatVector c0 = ZIP_LO.selectFrom(b0, b4);
        FloatVector c1 = ZIP_HI.selectFrom(b0, b4);
        FloatVector c2 = ZIP_LO.selectFrom(b1, b5);
        FloatVector c3 = ZIP_HI.selectFrom(b1, b5);
        FloatVector c4 = ZIP_LO.selectFrom(b2, b6);
        FloatVector c5 = ZIP_HI.selectFrom(b2, b6);
        FloatVector c6 = ZIP_LO.selectFrom(b3, b7);
        FloatVector c7 = ZIP_HI.selectFrom(b3, b7);
        ZIP_LO.selectFrom(c0, c4).intoArray(d, d0);
        ZIP_HI.selectFrom(c0, c4).intoArray(d, d0 + ds);
        ZIP_LO.selectFrom(c1, c5).intoArray(d, d0 + 2 * ds);
        ZIP_HI.selectFrom(c1, c5).intoArray(d, d0 + 3 * ds);
        ZIP_LO.selectFrom(c2, c6).intoArray(d, d0 + 4 * ds);
        ZIP_HI.selectFrom(c2, c6).intoArray(d, d0 + 5 * ds);
        ZIP_LO.selectFrom(c3, c7).intoArray(d, d0 + 6 * ds);
        ZIP_HI.selectFrom(c3, c7).intoArray(d, d0 + 7 * ds);
    }

    /**
     * The vector part of the RealFFT split step (see RealFFT.forward(float[], float[])), for k = 1, 1 + LANES ...
     * while the vector of bins k and the mirrored vector of bins half - k don't overlap.  Each bin is calculated with
     * the same operations, in the same order, as the scalar loop.
     * @return the first k that is left for the scalar loop
     */
    static int realSplit(float[] even, float[] odd, float[] cos, float[] sin, int half)
    {
        int k = 1;

        //Bins k to k + LANES - 1 and the mirrored bins m = half - k - LANES + 1 to half - k
        for(; half - k - LANES + 1 > k + LANES - 1; k += LANES)
        {
            int m = half - k - LANES + 1;
            FloatVector zki = FloatVector.fromArray(SPECIES, even, k);
            FloatVector zkq = FloatVector.fromArray(SPECIES, odd, k);
            FloatVector zmi = REVERSE.selectFrom(FloatVector.fromArray(SPECIES, even, m));
            FloatVector zmq = REVERSE.selectFrom(FloatVector.fromArray(SPECIES, odd, m));
            FloatVector ei = zki.add(zmi).mul(0.5f);
            FloatVector eq = zkq.sub(zmq).mul(0.5f);
            FloatVector oi = zkq.add(zmq).mul(0.5f);
            FloatVector oq = zki.sub(zmi).mul(-0.5f);
            FloatVector wi = FloatVector.fromArray(SPECIES, cos, k);
            FloatVector wq = FloatVector.fromArray(SPECIES, sin, k).neg();
            FloatVector woi = oi.mul(wi).sub(oq.mul(wq));
            FloatVector woq = oi.mul(wq).add(oq.mul(wi));
            ei.add(woi).intoArray(even, k);
            eq.add(woq).intoArray(odd, k);
            REVERSE.selectFrom(ei.sub(woi)).intoArray(even, m);
            REVERSE.selectFrom(woq.sub(eq)).intoArray(odd, m);
        }

        return k;
    }
}
