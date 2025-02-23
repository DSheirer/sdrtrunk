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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import java.util.Arrays;

public class BCHDecoder_31_21 extends BCHDecoder_31
{
    public static final int K = 21;
    private static final int T = 2; // Error-correcting capability  (N - K / M) = 2

    public BCHDecoder_31_21()
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

        int[] errors = {2,3};

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

        CorrectedBinaryMessage messageOriginal = new CorrectedBinaryMessage(31);
        CorrectedBinaryMessage messageWithErrors = new CorrectedBinaryMessage(31);

        for(int bit: r)
        {
            messageOriginal.set(bit);
        }

        for(int bit: r)
        {
            messageWithErrors.set(bit);
        }

        for(int error: errors)
        {
            messageWithErrors.flip(error);
        }

        System.out.println("\n*** TESTING NEW APPROACH ***");

        bch.decode(messageWithErrors);
//        int[] syndromes2 = bch.computeSyndromes(messageWithErrors);
//        System.out.println("Syndromes: " + Arrays.toString(syndromes2));

    }
}
