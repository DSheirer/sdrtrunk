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

package io.github.dsheirer.edac.galois.bch.linux;

import io.github.dsheirer.bits.IntField;
import java.util.Arrays;

/**
 * P25 BCH(63,16,23) code with T=11 error bit correction capacity.
 */
public class P25BCHDecoder_63_16 extends BCHDecoder_63
{
    public static final int K = 16;
    private static final int T = 11; // Error-correcting capability
    public static final IntField NAC_FIELD = IntField.length12(0);
    public static final IntField DUID_FIELD = IntField.length4(12);

    public P25BCHDecoder_63_16()
    {
        super(K, T);
    }

    public static void main(String[] args)
    {
        BCHDecoder bch = new BCHDecoder_31_21();

        int[] r = {0, 3, 4, 5, 6, 8, 10, 14, 16, 17, 18, 20, 21, 23, 24, 25};

        //Note: the codeword is 31 bits in a 32 bit integer, that is MSB aligned, meaning the final 32 bit (ie LSB) is not used.
        int codeword = 0;

        for(int index : r)
        {
            int value = 1 << index;
            //                System.out.println("Bit " + index + " = " + Integer.toHexString(value).toUpperCase());
            codeword |= value;
        }

        System.out.println("Codeword: " + Integer.toHexString(codeword).toUpperCase());

        int[] errors = {4, 18};

        for(int index : errors)
        {
            int value = 1 << index;
            codeword ^= value;
        }

        System.out.println("Codeword: " + Integer.toHexString(codeword).toUpperCase() + " With Errors At: " + Arrays.toString(errors));

        int[] syndromes = bch.compute_syndromes(codeword);
        System.out.println("Syndromes: " + Arrays.toString(syndromes));

        GFPoly elp = bch.compute_error_locator_polynomial(syndromes);
        System.out.println("ELP: " + Arrays.toString(elp.mC) + " Of Degree:" + elp.mDegree);

        int[] roots = bch.find_poly_roots(elp);
        System.out.println("Roots: " + Arrays.toString(roots));
    }

}
