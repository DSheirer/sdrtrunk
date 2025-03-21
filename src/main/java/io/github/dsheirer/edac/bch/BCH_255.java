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

import java.util.Arrays;

public class BCH_255 extends BCH
{
    public static final int M = 8;

    /**
     * Constructs an instance
     * @param k data bits size
     * @param t error detection and correction capacity of the generator polynomial used to create the codewords.
     */
    public BCH_255(int k, int t)
    {
//        super(M, k, t, PRIMITIVE_POLYNOMIAL_GF_255);
        super(M, k, t, 0x12D);
    }

    public static void main(String[] args)
    {
        BCH_255 b = new BCH_255(M, 1);

        System.out.println("ALOG: " + Arrays.toString(b.a_log_tab));
        System.out.println("APOW: " + Arrays.toString(b.a_pow_tab));
    }
}
