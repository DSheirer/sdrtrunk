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
import java.util.Arrays;
import org.apache.commons.math3.util.FastMath;

/**
 * BCH(31,21,2) decoder using the Berlekamp-Massey algorithm.
 */
public class BCHDecoder31_21
{
    private static final int PRIMITIVE_POLYNOMIAL_BCH_31 = 0x25; //M1(x) = x^5 + x^2 + 1 = 100101 (0x25) or 101001 (0x29) in reverse
    public static final int FLAG_MESSAGE_UNCORRECTABLE = -1;
    private static final int N = 31; // Codeword length
    private static final int K = 21; // Message length
    private static final int T = 2; // Error-correcting capability

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

    public BCHDecoder31_21()
    {
    }

    private static void initializeGaloisField()
    {
        // Initialize Galois Field GF(2^5)
        int m = 5;
        int primitive_poly = PRIMITIVE_POLYNOMIAL_BCH_31;
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
     * Decodes the received BCH(31,21) protected
     */
    public void decode(CorrectedBinaryMessage receivedMessage)
    {
//        System.out.println("*** Experiment Start ..");
//        int[] testELP = {0, 26, 9};
//        int[] chienTest = chienSearch2(testELP);
//        System.out.println("Chien Test: " + Arrays.toString(chienTest));
//        System.out.println("*** Experiment End\n\n");

        //Calculate the error syndromes from the message.
        int[] syndromes = calculateSyndromes(receivedMessage);

        //If the syndromes are all zero, then there are no errors in the codeword.
        if(isZero(syndromes))
        {
            System.out.println("PASS - SYNDROMES: " + Arrays.toString(syndromes));
            receivedMessage.setCorrectedBitCount(0);
            return;
        }

        int[] syndromesIndexForm = new int[syndromes.length];

        //Convert the syndromes to index form.
        for(int x = 0; x < syndromes.length; x++)
        {
            syndromesIndexForm[x] = INDEX_OF[syndromes[x]];
        }

        int[] errorLocations = new int[0];

        System.out.println("           Syndromes: " + Arrays.toString(syndromes));
        System.out.println("Syndromes Index Form: " + Arrays.toString(syndromesIndexForm));

        if(isSingleBitError(syndromesIndexForm))
        {
            errorLocations = Arrays.copyOf(syndromesIndexForm, 1);
        }
        else
        {

            //Use Berlekamp-Massey algorithm to calculate the Error Locator Polynomial (elp)
            int[] errorLocatorPolynomial = berlekampMassey(syndromesIndexForm);
            System.out.println("BM ELP:" + Arrays.toString(errorLocatorPolynomial));

            int[] syndromesExtended = new int[syndromesIndexForm.length + 1];
            System.arraycopy(syndromesIndexForm, 0, syndromesExtended, 1, syndromesIndexForm.length);
            errorLocatorPolynomial = berlekampMassey2(syndromesExtended);
            System.out.println("BM2 ELP:" + Arrays.toString(errorLocatorPolynomial));

            //Use Chien search to decode the error roots of the ELP into bit position error locations.  Note: the returned
            // array of the error roots are already inverted to the true message bit indexes.
            errorLocations = chienSearch(errorLocatorPolynomial);

            System.out.println("           SYNDROMES: " + Arrays.toString(syndromes));
            System.out.println("  SYNDROMES EXTENDED: " + Arrays.toString(syndromesExtended));
            System.out.println("SYNDROMES INDEX FORM: " + Arrays.toString(syndromesIndexForm));
            System.out.println("                 ELP: " + Arrays.toString(errorLocatorPolynomial));
            System.out.println("            ERR-LOCS: " + Arrays.toString(errorLocations));
        }

        for(int errorLocation : errorLocations)
        {
            //Flip the original message bits according to the error locations.  Note: the error locations are in
            //reverse bit order, so they have to be subtracted from (63-1) to get the normal message index location.
            receivedMessage.flip(errorLocation);
        }

        //Re-test the syndromes to validate the correction operation
        syndromes = calculateSyndromes(receivedMessage);

        if(isZero(syndromes))
        {
            System.out.println("Message Was Corrected - Bit Errors: " + errorLocations.length + " At: " + Arrays.toString(errorLocations));
            receivedMessage.setCorrectedBitCount(errorLocations.length);
        }
        else
        {
            System.out.println("Message Was NOT Corrected");
            //Uncorrectable bit errors in the message.  Reverse the error locations because they didn't work
            for(int errorLocation : errorLocations)
            {
                receivedMessage.flip(errorLocation);
            }
        }
    }

    public boolean isSingleBitError(int[] syndromes)
    {
        for(int index = 1; index < syndromes.length; index++)
        {
            int a = syndromes[index];
            int b = syndromes[0] * (index + 1) % N;
            if(syndromes[index] != (syndromes[0] * (index + 1) % N))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates the 2*T syndromes for the received message.  These are the output sequences of a non minimal LFSR
     * that's using the received codeword as polynomial coefficients or 'taps' in the LFSR.  The BM algorithm then finds
     * a minimal LFSR that can produce the same sets of sequences.
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
//                if(received.get(N - 1 - j)) //Access the message bits (0 - 30) in reverse order
                if(received.get(j)) //Access the message bits (0 - 30) in forward order
                {
                    int lookup = (i * j) % N;
                    int alpha = ALPHA_TO[lookup];
                    s[i - 1] ^= ALPHA_TO[(i * j) % N];
                    System.out.println(" s[" + (i-1) + "] Bit: " + j + " lookup:" + lookup + " alpha:" + alpha + " s[" + i + "]=" + s[i - 1]);
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
        int N_As2T = 2 * T;

        // Implement simplified version of Berlekamp-Massey algorithm
        int[] A_ConnectionPolynomial = new int[N_As2T];
        //Start with a single tap polynomial
        A_ConnectionPolynomial[0] = 1;

        int[] B = new int[N_As2T];
        B[0] = 1;

        int L_Length = 0;
        int m = 1;
        for(int k = 0; k < 2 * T; k++)  //k can be either 0-2T or 1-(2T + 1)
        {
            //Discrepancy is zero (0) for all odd k when using a 0-based syndrome indexing, so we skip the odds.
            if(k % 2 == 0)
            {
                int discrepancy = S_syndromes[k];

                for(int i = 1; i <= L_Length; i++)
                {
//TODO: is this correct way to calculate the discrepancy?                    //Minus operation is implemented as XOR
                    discrepancy ^= mul(A_ConnectionPolynomial[i], S_syndromes[k - i]);
                }

                if(discrepancy != 0)
                {
                    int[] T_Temp = Arrays.copyOf(A_ConnectionPolynomial, N_As2T);

                    for(int i = 0; i < N_As2T - m; i++)
                    {
                        A_ConnectionPolynomial[m + i] ^= mul(discrepancy, B[i]);
                    }

                    if(2 * L_Length <= k)
                    {
                        L_Length = k + 1 - L_Length;
                        B = T_Temp;
                        m = 0;
                    }
                }
            }
            m++;
        }
        return Arrays.copyOf(A_ConnectionPolynomial, L_Length + 1);
    }

    /**
     * Applies Berlekamp-Massey algorithm to calculate the error roots as the Error Locator Polynomial (elp).
     * @param S_syndromes to process
     * @return elp array of coefficients.
     */
    private int[] berlekampMassey2(int[] s)
    {
        int[] tempS = new int[N - K + 1];
        Arrays.fill(tempS, -1);
        System.arraycopy(s, 0, tempS, 0, s.length);
        s = tempS;
        /* compute the error location polynomial via the Berlekamp iterative algorithm,
         following the terminology of Lin and Costello :   d[u] is the 'mu'th
         discrepancy, where u='mu'+1 and 'mu' (the Greek letter!) is the step number
         ranging from -1 to 2*tt (see L&C),  l[u] is the
         degree of the elp at that step, and u_l[u] is the difference between the
         step number and the degree of the elp.
         */

        /* initialise table entries */
        int[] d = new int[N - K + 2];
        int[][] elp = new int[N - K + 2][N - K];
        int[] l = new int[N - K + 2];
        int[] u_lu = new int[N - K + 2];
        int q, u;

        d[0] = 0; /* index form */
        d[1] = s[1]; /* index form */
        elp[0][0] = 0; /* index form */
        elp[1][0] = 1; /* polynomial form */

        for(int i = 1; i < N - K; i++)
        {
            elp[0][i] = -1; /* index form */
            elp[1][i] = 0; /* polynomial form */
        }

        l[0] = 0;
        l[1] = 0;
        u_lu[0] = -1;
        u_lu[1] = 0;
        u = 0;

        do
        {
            u++;

            if(d[u] == -1)
            {
                l[u + 1] = l[u];

                for(int i = 0; i <= l[u]; i++)
                {
                    elp[u + 1][i] = elp[u][i];
                    elp[u][i] = INDEX_OF[elp[u][i]];
                }
            }
            else
                /* search for words with greatest u_lu[q] for which d[q]!=0 */
            {
                q = u - 1;

                while((d[q] == -1) && (q > 0))
                {
                    q--;
                }

                /* have found first non-zero d[q]  */
                if(q > 0)
                {
                    int j = q;

                    do
                    {
                        j--;

                        if((d[j] != -1) && (u_lu[q] < u_lu[j]))
                        {
                            q = j;
                        }
                    }
                    while(j > 0);
                }

                /* have now found q such that d[u]!=0 and u_lu[q] is maximum */
                /* store degree of new elp polynomial */
                l[u + 1] = FastMath.max(l[u], l[q] + u - q);

                /* form new elp(x) */
                for(int i = 0; i < N - K; i++)
                {
                    elp[u + 1][i] = 0;
                }

                for(int i = 0; i <= l[q]; i++)
                {
                    if(elp[q][i] != -1)
                    {
                        elp[u + 1][i + u - q] = ALPHA_TO[(d[u] + N - d[q] + elp[q][i]) % N];
                    }
                }
                for(int i = 0; i <= l[u]; i++)
                {
                    elp[u + 1][i] ^= elp[u][i];
                    elp[u][i] = INDEX_OF[elp[u][i]]; /*convert old elp value to index*/
                }
            }

            u_lu[u + 1] = u - l[u + 1];

            /* form (u+1)th discrepancy */
            if(u < N - K) /* no discrepancy computed on last iteration */
            {
                if(s[u + 1] != -1)
                {
                    d[u + 1] = ALPHA_TO[s[u + 1]];
                }
                else
                {
                    d[u + 1] = 0;
                }
                for(int i = 1; i <= l[u + 1]; i++)
                {
                    if((s[u + 1 - i] != -1) && (elp[u + 1][i] != 0))
                    {
                        d[u + 1] ^= ALPHA_TO[(s[u + 1 - i] + INDEX_OF[elp[u + 1][i]]) % N];
                    }
                }

                d[u + 1] = INDEX_OF[d[u + 1]]; /* put d[u+1] into index form */
            }
        }
        while((u < N - K) && (l[u + 1] <= T));

//TODO: this is where it should cut off ...
//        return elp[u];

        //Chien search follows ...
        int[] reg = new int[T + 1];
        int[] root = new int[T];
        int[] loc = new int[T];
        int[] z = new int[T + 1];
        int[] err = new int[N];
        int count;

        u++;

        if(l[u] <= T) /* can correct error */
        {
            /* put elp into index form */
            for(int i = 0; i <= l[u]; i++)
            {
                elp[u][i] = INDEX_OF[elp[u][i]];
            }

            /* find roots of the error location polynomial */
            if(l[u] >= 0)
            {
                System.arraycopy(elp[u], 1, reg, 1, l[u]);
            }

            count = 0;

            for(int i = 1; i <= N; i++)
            {
                q = 1;

                for(int j = 1; j <= l[u]; j++)
                {
                    if(reg[j] != -1)
                    {
                        reg[j] = (reg[j] + j) % N;
                        q ^= ALPHA_TO[reg[j]];
                    }
                }

                if(q == 0) /* store root and error location number indices */
                {
                    root[count] = i;
                    loc[count] = N - i;
                    count++;
                }
            }

            if(count == l[u]) /* no. roots = degree of elp hence <= tt errors */
            {
                /* form polynomial z(x) */
                for(int i = 1; i <= l[u]; i++) /* Z[0] = 1 always - do not need */
                {
                    if((s[i] != -1) && (elp[u][i] != -1))
                    {
                        z[i] = ALPHA_TO[s[i]] ^ ALPHA_TO[elp[u][i]];
                    }
                    else if((s[i] != -1) && (elp[u][i] == -1))
                    {
                        z[i] = ALPHA_TO[s[i]];
                    }
                    else if((s[i] == -1) && (elp[u][i] != -1))
                    {
                        z[i] = ALPHA_TO[elp[u][i]];
                    }
                    else
                    {
                        z[i] = 0;
                    }

                    for(int j = 1; j < i; j++)
                    {
                        if((s[j] != -1) && (elp[u][i - j] != -1))
                        {
                            z[i] ^= ALPHA_TO[(elp[u][i - j] + s[j]) % N];
                        }
                    }

                    z[i] = INDEX_OF[z[i]]; /* put into index form */
                }

                boolean f = false;

                /* evaluate errors at locations given by error location numbers loc[i] */
//                for(int i = 0; i < N; i++)
//                {
//                    err[i] = 0;
//
//                    if(output[i] != -1) /* convert recd[] to polynomial form */
//                    {
//                        output[i] = ALPHA_TO[output[i]];
//                    }
//                    else
//                    {
//                        output[i] = 0;
//                    }
//                }

//                for(int i = 0; i < l[u]; i++) /* compute numerator of error term first */
//                {
//                    err[loc[i]] = 1; /* accounts for z[0] */
//
//                    for(int j = 1; j <= l[u]; j++)
//                    {
//                        if(z[j] != -1)
//                        {
//                            err[loc[i]] ^= alpha_to[(z[j] + j * root[i]) % NN];
//                        }
//                    }
//
//                    if(err[loc[i]] != 0)
//                    {
//                        err[loc[i]] = index_of[err[loc[i]]];
//
//                        q = 0; /* form denominator of error term */
//
//                        for(int j = 0; j < l[u]; j++)
//                        {
//                            if(j != i)
//                            {
//                                q += index_of[1 ^ alpha_to[(loc[j] + root[i]) % NN]];
//                            }
//                        }
//
//                        q = q % NN;
//                        err[loc[i]] = alpha_to[(err[loc[i]] - q + NN) % NN];
//                        output[loc[i]] ^= err[loc[i]]; /*recd[i] must be in polynomial form */
//                    }
                }
            }
            else
            {
                /* no. roots != degree of elp => >tt errors and cannot solve */
//                irrecoverable_error = true;
            }

            return new int[0];
        }


    /**
     * Calculates the bit error index locations from the error roots array.
     * @param elp error roots array.
     * @return array of error indices where the array length indicates the quantity of errors, up to T (11) bit errors.
     */
    private int[] chienSearch(int[] elp)
    {
        System.out.println("ALPHA_TO = " + Arrays.toString(ALPHA_TO));
        System.out.println("INDEX_OF = " + Arrays.toString(INDEX_OF) + "\n");

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

//            System.out.println("\t" + Arrays.toString(elp) + " " + i + " Sum: " + sum);

            if(sum == 0)
            {
                //This is an unrecoverable error ... we'll just keep updating the highest location
                if(count >= T)
                {
                    count = T - 1;
                }

//TODO: can we change this from 'N - i' to just i, so that we don't have to reverse it?  Maybe not.
//                errorLocations[count++] = N - i;
                errorLocations[count++] = i;
            }
        }

        System.out.println("Chien Search Results: " + Arrays.toString(errorLocations));
        return Arrays.copyOf(errorLocations, count);
    }

    /**
     * Calculates the bit error index locations from the error roots array.
     * @param elp error roots array.
     * @return array of error indices where the array length indicates the quantity of errors, up to T (11) bit errors.
     */
    private int[] chienSearch2(int[] elp)
    {
        /* put elp into index form */
//        for(int i = 0; i <= l[u]; i++)
//        {
//            elp[u][i] = index_of[elp[u][i]];
//        }


//        for(int i = 0; i < elp.length; i++)
//        {
//            elp[i] = INDEX_OF[elp[i]];
//        }

        int[] reg = Arrays.copyOf(elp, elp.length + 1);
        int[] root = new int[T];
        int[] loc = new int[T];

        int count = 0, q;

        for(int i = 1; i <= N; i++)
        {
            q = 1;

            for(int j = 1; j <= elp.length; j++)
            {
                if(reg[j] != -1)
                {
                    reg[j] = (reg[j] + j) % N;
                    q ^= ALPHA_TO[reg[j]];
                }
            }

            if(q == 0) /* store root and error location number indices */
            {
                root[count] = i;
                loc[count] = N - i;
                count++;
            }
        }

        return Arrays.copyOf(root, count);
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