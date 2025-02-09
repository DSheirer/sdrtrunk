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

package io.github.dsheirer.edac.galois;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import java.util.Arrays;

/**
 * P25 BCH(63,16) decoder using the Berlekamp-Massey algorithm.
 */
public class P25BCHDecoder
{
    private static final int PRIMITIVE_POLYNOMIAL_BCH_63_61_1 = 0x43; //M1(x) = x^6 + x + 1
    public static final int FLAG_MESSAGE_UNCORRECTABLE = -1;
    private static final int N = 63; // Codeword length
    private static final int K = 16; // Message length
    private static final int T = 11; // Error-correcting capability
    public static final IntField NAC_FIELD = IntField.length12(0);
    public static final IntField DUID_FIELD = IntField.length4(12);

//    //Galois field GF(6) using primitive polynomial: x^6 + x + 1 (63,61,1)
//    private static final int[] ALPHA_TO = {1, 2, 4, 8, 16, 32, 3, 6, 12, 24, 48, 35, 5, 10, 20, 40, 19, 38, 15, 30, 60,
//            59, 53, 41, 17, 34, 7, 14, 28, 56, 51, 37, 9, 18, 36, 11, 22, 44, 27, 54, 47, 29, 58, 55, 45, 25, 50, 39,
//            13, 26, 52, 43, 21, 42, 23, 46, 31, 62, 63, 61, 57, 49, 33, 0};
//
//    private static final int[] INDEX_OF = {-1, 0, 1, 6, 2, 12, 7, 26, 3, 32, 13, 35, 8, 48, 27, 18, 4, 24, 33, 16, 14,
//            52, 36, 54, 9, 45, 49, 38, 28, 41, 19, 56, 5, 62, 25, 11, 34, 31, 17, 47, 15, 23, 53, 51, 37, 44, 55, 40,
//            10, 61, 46, 30, 50, 22, 39, 43, 29, 60, 42, 21, 20, 59, 57, 58};

    private static final int[] ALPHA_TO = new int[N + 1];
    private static final int[] INDEX_OF = new int[N + 1];
    static
    {
        initializeGaloisField();
    }

    public P25BCHDecoder()
    {
    }

    private static void initializeGaloisField()
    {
        // Initialize Galois Field GF(2^6)
        int m = 6;
        int primitive_poly = PRIMITIVE_POLYNOMIAL_BCH_63_61_1;
        int mask = 1;
        ALPHA_TO[m] = 0;
        for (int i = 0; i < m; i++) {
            ALPHA_TO[i] = mask;
            INDEX_OF[ALPHA_TO[i]] = i;
            if ((primitive_poly & (1 << i)) != 0)
                ALPHA_TO[m] ^= mask;
            mask <<= 1;
        }
        INDEX_OF[ALPHA_TO[m]] = m;
        mask >>= 1;
        for (int i = m + 1; i < N; i++)
        {
            if (ALPHA_TO[i - 1] >= mask)
            {
                ALPHA_TO[i] = ALPHA_TO[m] ^ ((ALPHA_TO[i - 1] ^ mask) << 1);
            }
            else
            {
                ALPHA_TO[i] = ALPHA_TO[i - 1] << 1;
            }

            INDEX_OF[ALPHA_TO[i]] = i;
        }

        INDEX_OF[0] = -1;
    }


    /**
     * Decodes the received BCH(63,16) protected 64-bit NID containing the NAC (bits 0-11) and DUID (bits 12-15).
     * @param receivedMessage with NAC and DUID in index positions 0-15 and BCH parity bits in positions 16-62 with the
     * optional message parity bit in position 63.  Note the final parity bit is ignored for decoding.
     * @param observedNAC that has previously been decoded to use when attempting to repair a non-repairable codeword
     * by overwriting the NAC field with this value and then attempting error correction again.
     *
     * Note: the message codeword is repaired in place and the results of that attempted repair are stored in the
     * corrected bits count field.  A zero value indicates no errors.  A positive value, up to 11, indicates the
     * number of bits that were repaired.  A negative value indicates a non-repairable codeword.
     */
    public void decode(CorrectedBinaryMessage receivedMessage, int observedNAC)
    {
        //Calculate the error syndromes from the message.
        int[] syndromes = calculateSyndromes(receivedMessage);

        //If the syndromes are all zero, then there are no errors in the codeword.
        if(isZero(syndromes))
        {
            System.out.println("PASS - SYNDROMES: " + Arrays.toString(syndromes));
            receivedMessage.setCorrectedBitCount(0);
            return;
        }

        //Use Berlekamp-Massy algorithm to calculate the error roots.  Error Locator Polynomial (elp)
        int[] elp = berlekampMassey(syndromes);

        int[] elp2 = berlekampMassey(syndromes);

        //Use Chien search to decode the error roots into bit position error locations.
        int[] errorLocations = chienSearch(elp);

        System.out.println("SYNDROMES: " + Arrays.toString(syndromes) + " ELP: " + Arrays.toString(elp) + " ERROR LOCATIONS: " + Arrays.toString(errorLocations));

        for(int errorLocation : errorLocations)
        {
            //Flip the original message bits according to the error locations.  Note: the error locations are in
            //reverse bit order, so they have to be subtracted from (63-1) to get the normal message index location.
            receivedMessage.flip(62 - errorLocation);
        }

        //Re-test the syndromes to validate the correction operation
        syndromes = calculateSyndromes(receivedMessage);

        if(isZero(syndromes))
        {
            receivedMessage.setCorrectedBitCount(errorLocations.length);
        }
        else
        {
            //Uncorrectable bit errors in the message.  Reverse the error locations because they didn't work
            for(int errorLocation : errorLocations)
            {
                receivedMessage.flip(62 - errorLocation);
            }

            //If the observedNAC value is greater than zero, attempt to recover the codeword by overwriting the NAC
            //field in the codeword and attempt error correction one more time.
            if(observedNAC >= 0)
            {
                receivedMessage.setInt(observedNAC, NAC_FIELD);
                decode(receivedMessage, -1); //Recursive call
            }
            else
            {
                receivedMessage.setCorrectedBitCount(FLAG_MESSAGE_UNCORRECTABLE);
            }
        }
    }

    /**
     * Calculates the 2*T error syndromes for the received message.
     * @param received message in reverse bit order.
     * @return syndrome array.  An all-zeros array indicates no errors detected.
     */
    private int[] calculateSyndromes(CorrectedBinaryMessage received)
    {
        int[] s = new int[2 * T];
        for(int i = 1; i <= 2 * T; i++)
        {
            s[i - 1] = 0;
            for(int j = 0; j < N; j++)
            {
                if(received.get(62 - j)) //Access the message bits (0 - 62) in reverse order
                {
                    s[i - 1] ^= ALPHA_TO[(i * j) % N];
                }
            }
        }

        return s;
    }

    /**
     * Checks the syndrome array to detect an all-zeroes condition.
     * @param syndromes to check.
     * @return true if the array contains all zeros.
     */
    private boolean isZero(int[] syndromes)
    {
        for(int syndrome : syndromes)
        {
            if(syndrome != 0)
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Applies Berlekamp-Massey algorithm to calculate the error roots as the Error Locator Polynomial (elp).
     * @param S_syndromes to process
     * @return elp array of coefficients.
     */
    private int[] berlekampMassey(int[] S_syndromes)
    {
        // Implement simplified version of Berlekamp-Massey algorithm
        int[] A_Lambda = new int[N];
        int[] B = new int[N];
        A_Lambda[0] = 1;
        B[0] = 1;
        int L = 0;
        int m = 1;
        for(int k = 0; k < 2 * T; k++)  //k can be either 0-2T or 1-(2T + 1)
        {
            int discrepancy = S_syndromes[k];

            for(int i = 1; i <= L; i++)
            {
                //Minus operation is implemented as XOR
                discrepancy ^= mul(A_Lambda[i], S_syndromes[k - i]);
            }

            if(discrepancy != 0)
            {
                int[] T = Arrays.copyOf(A_Lambda, N);

                for(int i = 0; i < N - m; i++)
                {
                    A_Lambda[m + i] ^= mul(discrepancy, B[i]);
                }

                if(2 * L <= k)
                {
                    L = k + 1 - L;
                    B = T;
                    m = 0;
                }
            }
            m++;
        }
        return Arrays.copyOf(A_Lambda, L + 1);
    }

    public static int[] berlekampMassey2(int[] sequence)
    {
        int N = sequence.length;
        int[] C = new int[N];
        int[] B = new int[N];
        C[0] = 1;
        B[0] = 1;
        int L = 0;
        int m = 1;

        for (int n = 0; n < N; n++) {
            int d = 0;
            for (int i = 0; i <= L; i++) {
                d ^= C[i] * sequence[n - i];
            }

            if (d == 1) {
                int[] T = Arrays.copyOf(C, N);
                for (int i = 0; i < N - m; i++) {
                    C[i + m] ^= B[i];
                }
                if (L < n / 2) {
                    L = n - L;
                    m = n + 1 - L;
                    B = T;
                }
            }
        }

        return Arrays.copyOfRange(C, 0, L + 1);
    }

    /**
     * Calculates the bit error index locations from the error roots array.
     * @param elp error roots array.
     * @return array of error indices where the array length indicates the quantity of errors, up to T (11) bit errors.
     */
    private int[] chienSearch(int[] elp)
    {
        //Short circuit for 1-bit error case
        if(elp.length == 2)
        {
            return new int[]{INDEX_OF[elp[1]]};
        }

        // Implement Chien search to find error locations
        int[] errorLocations = new int[T];
        int count = 0;
        for(int i = 1; i <= N; i++)
        {
            int sum = 1;
            for(int j = 1; j < elp.length; j++)
            {
                sum ^= mul(elp[j], ALPHA_TO[(j * i) % N]);
            }
            if(sum == 0)
            {
                //This is an unrecoverable error ... we'll just keep updating the highest location
                if(count >= T)
                {
                    count = T - 1;
                }

//TODO: can we change this from 'N - i' to just i, so that we don't have to reverse it?  Maybe not.
                errorLocations[count++] = N - i;
            }
        }
        return Arrays.copyOf(errorLocations, count);
    }

    /**
     * Multiplies two integer values using mod N (63) polynomial math.
     * @param a first value
     * @param b second value
     * @return product of (a * b) % N
     */
    private static int mul(int a, int b)
    {
        if(a == 0 || b == 0)
        {
            return 0;
        }

        return ALPHA_TO[(INDEX_OF[a] + INDEX_OF[b]) % N];
    }
}