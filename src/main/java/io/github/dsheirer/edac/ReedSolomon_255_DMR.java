/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.edac;

/**
 * Berlekemp Massey decoder for GF(8) using DMR Generator Polynomial
 */
public class ReedSolomon_255_DMR extends BerlekempMassey
{
    /**
     * DMR Reed-Solomon code is generated from a Galois Field(2^8) by the polynomial: a8 + a4 + a3 + a2 + a0.
     * In binary, this is expressed as: 100011101 which is reversed to big-endian format for this algorithm
     * See: TS 102-361-1 paragraph B.3.6 Reed-Solomon(12,9)
     */
    public static final int[] DMR_GENERATOR_POLYNOMIAL = {1, 0, 1, 1, 1, 0, 0, 0, 1};

    /**
     * Constructs an instance of RS(total symbols, message symbols)
     *
     * @param n total symbols
     * @param k message symbols
     */
    public ReedSolomon_255_DMR(int n, int k)
    {
        super(8, n, k, DMR_GENERATOR_POLYNOMIAL);
    }
}
