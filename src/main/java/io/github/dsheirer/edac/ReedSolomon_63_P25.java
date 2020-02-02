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
 * Berlekemp Massey decoder using APCO-25 Generator Polynomial over GF(6)
 */
public class ReedSolomon_63_P25 extends BerlekempMassey
{
    /**
     * APCO-25 Reed-Solomon code is generated from a Galois Field(2^6) by the polynomial: a6 + a1 + a0.
     * In binary, this is expressed as: 1000011 which is reversed to big-endian format for this algorithm
     * See: TIA 102-BAAA paragraph 5.9 Reed-Solomon Code Generator Matrices
     */
    public static final int[] P25_GENERATOR_POLYNOMIAL = { 1, 1, 0, 0, 0, 0, 1 };

    /**
     * Constructs an instance
     *
     * @param n total symbols
     * @param k message symbols
     */
    public ReedSolomon_63_P25(int n, int k)
    {
        super(6, n, k, P25_GENERATOR_POLYNOMIAL);
    }
}
