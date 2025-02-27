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

package io.github.dsheirer.edac.bch;

/**
 * BCH(63) decoder base implementation that uses GF(2^6) and standard primitive polynomial.
 */
public abstract class BCH_63 extends BCH
{
    public static final int M = 6;

    /**
     * Constructs an instance
     * @param k data bits size
     * @param t error detection and correction capacity of the generator polynomial used to create the codewords.
     */
    public BCH_63(int k, int t)
    {
        super(M, k, t, PRIMITIVE_POLYNOMIAL_GF_63);
    }
}
