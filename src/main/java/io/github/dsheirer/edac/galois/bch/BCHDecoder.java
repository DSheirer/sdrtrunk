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

package io.github.dsheirer.edac.galois.bch;

import java.util.Arrays;
import java.util.Random;

public class BCHDecoder
{
    private final int MM;
    private final int NN;
    public final int k = 21;
    private final int TT;
    private final int d = 5;
    public int length = 31;
    int[] PRIMITIVE_POLYNOMIAL = new int[6];        /* irreducible polynomial */
    int[] alpha_to = new int[32];
    int[] index_of = new int[32];
    int[] g = new int[11];
    public int[] recd = new int[31];
    public int[] data = new int[21];
    public int[] bb = new int[11];
    int numerr = 0;
    int[] errpos = new int[32];
    int decerror = 0;
    int seed;

    /**
     * Constructs a BCH decoder.
     * @param m size of the Galois Field - GF(2 ^ m)
     * @param n size of the full codeword, generally: 2 ^ m - 1
     * @param t maximum number of errors that can be corrected - this is a design characteristic of the generator
     * polynomial.
     * @param primitivePolynomial minimum polynomial to generate the Galois field, using elements of 0 or 1, in
     * reverse order, e.g.: 1 + x^1 + x^2 ... + x^m
     */
    public BCHDecoder(int m, int n, int t, int[] primitivePolynomial)
    {
        MM = m;
        NN = n;
        TT = t;
        PRIMITIVE_POLYNOMIAL = primitivePolynomial;
        System.out.println("Primitive Polynomial: " + Arrays.toString(PRIMITIVE_POLYNOMIAL) + " x^5 + x^2 + 1 (in reverse order)");
    }

    /*
     * generate GF(2**m) from the irreducible polynomial p(X) in p[0]..p[m]
     * lookup tables:  index->polynomial form   alpha_to[] contains j=alpha**i;
     * polynomial form -> index form  index_of[j=alpha**i] = i alpha=2 is the
     * primitive element of GF(2**m)
     */
    void generate_gf()
    {
        int i, mask;
        mask = 1;
        alpha_to[MM] = 0;
        for(i = 0; i < MM; i++)
        {
            alpha_to[i] = mask;
            index_of[alpha_to[i]] = i;
            if(PRIMITIVE_POLYNOMIAL[i] != 0)
            {
                alpha_to[MM] ^= mask;
            }
            mask <<= 1;
        }
        index_of[alpha_to[MM]] = MM;
        mask >>= 1;
        for(i = MM + 1; i < NN; i++)
        {
            if(alpha_to[i - 1] >= mask)
            {
                alpha_to[i] = alpha_to[MM] ^ ((alpha_to[i - 1] ^ mask) << 1);
            }
            else
            {
                alpha_to[i] = alpha_to[i - 1] << 1;
            }
            index_of[alpha_to[i]] = i;
        }
        index_of[0] = -1;

        System.out.println("ALPHA_TO = " + Arrays.toString(alpha_to));
        System.out.println("INDEX_OF = " + Arrays.toString(index_of));
    }

    /*
     * Compute generator polynomial of BCH code of length = 31, redundancy = 10
     * (OK, this is not very efficient, but we only do it once, right? :)
     */
    void gen_poly()
    {
        int ii, jj, ll, kaux;
        boolean test;
        int aux, nocycles, root, noterms, rdncy;
        int[][] cycle = new int[15][6];
        int[] size = new int[15];
        int[] min = new int[11];
        int[] zeros = new int[11];
        /* Generate cycle sets modulo 31 */
        cycle[0][0] = 0;
        size[0] = 1;
        cycle[1][0] = 1;
        size[1] = 1;
        jj = 1;            /* cycle set index */
        do
        {
            /* Generate the jj-th cycle set */
            ii = 0;
            do
            {
                ii++;
                cycle[jj][ii] = (cycle[jj][ii - 1] * 2) % NN;
                size[jj]++;
                aux = (cycle[jj][ii] * 2) % NN;
            }
            while(aux != cycle[jj][0]);
            /* Next cycle set representative */
            ll = 0;
            do
            {
                ll++;
                test = false;
                for(ii = 1; ((ii <= jj) && (!test)); ii++)
                    /* Examine previous cycle sets */
                {
                    for(kaux = 0; ((kaux < size[ii]) && (!test)); kaux++)
                    {
                        if(ll == cycle[ii][kaux])
                        {
                            test = true;
                        }
                    }
                }
            }
            while((test) && (ll < (NN - 1)));
            if(!(test))
            {
                jj++;    /* next cycle set index */
                cycle[jj][0] = ll;
                size[jj] = 1;
            }
        }
        while(ll < (NN - 1));
        nocycles = jj;        /* number of cycle sets modulo n */
        /* Search for roots 1, 2, ..., d-1 in cycle sets */
        kaux = 0;
        rdncy = 0;
        for(ii = 1; ii <= nocycles; ii++)
        {
            min[kaux] = 0;
            for(jj = 0; jj < size[ii]; jj++)
            {
                for(root = 1; root < d; root++)
                {
                    if(root == cycle[ii][jj])
                    {
                        min[kaux] = ii;
                    }
                }
            }
            if(min[kaux] > 0)
            {
                rdncy += size[min[kaux]];
                kaux++;
            }
        }
        noterms = kaux;
        kaux = 1;
        for(ii = 0; ii < noterms; ii++)
        {
            for(jj = 0; jj < size[min[ii]]; jj++)
            {
                zeros[kaux] = cycle[min[ii]][jj];
                kaux++;
            }
        }
//        System.out.println(String.format("This is a (%d, %d, %d) binary BCH code\n", length, k, d));
        /* Compute generator polynomial */
        g[0] = alpha_to[zeros[1]];
        g[1] = 1;        /* g(x) = (X + zeros[1]) initially */
        for(ii = 2; ii <= rdncy; ii++)
        {
            g[ii] = 1;
            for(jj = ii - 1; jj > 0; jj--)
            {
                if(g[jj] != 0)
                {
                    g[jj] = g[jj - 1] ^ alpha_to[(index_of[g[jj]] + zeros[ii]) % NN];
                }
                else
                {
                    g[jj] = g[jj - 1];
                }
            }
            g[0] = alpha_to[(index_of[g[0]] + zeros[ii]) % NN];
        }

        for(ii = 0; ii <= rdncy; ii++)
        {
            if(ii > 0 && ((ii % 70) == 0))
            {
//                printf("\n");
            }
        }

        System.out.println("Generator Polynomial g(x) = " + Arrays.toString(g) + " - Used Only For Encoding");
    }

    /*
     * Calculate redundant bits bb[], codeword is c(X) = data(X)*X**(n-k)+ bb(X)
     */
    void encode_bch()
    {
        int i, j;
        int feedback;
        for(i = 0; i < length - k; i++)
        {
            bb[i] = 0;
        }
        for(i = k - 1; i >= 0; i--)
        {
            feedback = data[i] ^ bb[length - k - 1];
            if(feedback != 0)
            {
                for(j = length - k - 1; j > 0; j--)
                {
                    if(g[j] != 0)
                    {
                        bb[j] = bb[j - 1] ^ feedback;
                    }
                    else
                    {
                        bb[j] = bb[j - 1];
                    }
                }
                bb[0] = g[0] & feedback;
            }
            else
            {
                for(j = length - k - 1; j > 0; j--)
                {
                    bb[j] = bb[j - 1];
                }
                bb[0] = 0;
            }
        }

        System.out.println("Encoded Parity Bits (bb): " + Arrays.toString(bb));
    }

    /*
     * We do not need the Berlekamp algorithm to decode.
     * We solve before hand two equations in two variables.
     */
    public void decode()
    {
        int i, j, q;
        int[] elp = new int[TT + 1];
        int[] SYNDROMES = new int[MM];
        int s3;
        int count = 0;
        boolean syn_error = false;
        int[] loc = new int[TT + 1];
        int[] err = new int[TT + 1];
        int[] reg = new int[TT + 1];
        int aux;

        //Calculate the syndromes
        for(i = 1; i <= 2 * TT; i++)
        {
            SYNDROMES[i] = 0;
            for(j = 0; j < NN; j++)
            {
                if(recd[j] != 0)
                {
                    SYNDROMES[i] ^= alpha_to[(i * j) % NN];
                }
            }
            if(SYNDROMES[i] != 0)
            {
                syn_error = true;    /* set flag if non-zero syndrome */
            }

            /* convert syndrome from polynomial form to index form  */
            SYNDROMES[i] = index_of[SYNDROMES[i]];
        }

        System.out.println("Syndromes: " + Arrays.toString(SYNDROMES));
        if(syn_error)
        {    /* If there are errors, try to correct them */
            if(SYNDROMES[1] != -1)
            {
                s3 = (SYNDROMES[1] * 3) % NN;
                if(SYNDROMES[3] == s3)  /* Was it a single error ? */
                {
                    System.out.println("One error at " + SYNDROMES[1]);
                    recd[SYNDROMES[1]] ^= 1;        /* Yes: Correct it */
                }
                else
                {                /* Assume two errors occurred and solve
                 * for the coefficients of sigma(x), the
                 * error locator polynomail
                 */
                    if(SYNDROMES[3] != -1)
                    {
                        aux = alpha_to[s3] ^ alpha_to[SYNDROMES[3]];
                    }
                    else
                    {
                        aux = alpha_to[s3];
                    }
                    elp[0] = 0;
                    elp[1] = (SYNDROMES[2] - index_of[aux] + NN) % NN;
                    elp[2] = (SYNDROMES[1] - index_of[aux] + NN) % NN;
//                    printf("sigma(x) = ");
//                    for(i = 0; i <= 2; i++)
//                    {
//                        printf("%3d ", elp[i]);
//                    }
//                    printf("\n");
                    System.out.println("ELP: " + Arrays.toString(elp));

//                    printf("Roots: ");
                    /* find roots of the error location polynomial */
                    for(i = 1; i <= 2; i++)
                    {
                        reg[i] = elp[i];
                    }

                    /* Chien search */
                    count = 0;
                    for(i = 1; i <= NN; i++)
                    {
                        q = 1;
                        for(j = 1; j <= 2; j++)
                        {
                            if(reg[j] != -1)
                            {
                                reg[j] = (reg[j] + j) % NN;
                                q ^= alpha_to[reg[j]];
                            }
                        }
                        if(q == 0)
                        {    /* store error location number indices */
                            loc[count] = i % NN;
                            count++;
                            System.out.println("Root found at: " + (i % NN));
//                            printf("%3d ", (i % n));
                        }
                    }
//                    printf("\n");
                    if(count == 2)/* no. roots = degree of elp hence 2 errors */
                    {
                        for(i = 0; i < 2; i++)
                        {
                            recd[loc[i]] ^= 1;
                        }
                    }
                    else    /* Cannot solve: Error detection */
                    {
                        System.out.println("incomplete decoding\n");
                    }
                }
            }
            else if(SYNDROMES[2] != -1) /* Error detection */
            {
                System.out.println("incomplete decoding\n");
            }
        }
    }

    public static void main(String[] args)
    {
        BCHDecoder31_21_2 bch = new BCHDecoder31_21_2();
//        BCH31_21_OriginalWorkingExample bch = new BCH31_21_OriginalWorkingExample();
        bch.generate_gf();            /* generate the Galois Field GF(2**m) */
        bch.gen_poly();                /* Compute the generator polynomial of BCH code */

        int i;
        int seed = 1;
        Random random = new Random(seed);
        /* Randomly generate DATA */
        for(i = 0; i < bch.k; i++)
        {
            bch.data[i] = (random.nextInt() & 67108864) >> 26;
        }

        /* ENCODE */
        bch.encode_bch();            /* encode data */

        for(i = 0; i < bch.length - bch.k; i++)
        {
            bch.recd[i] = bch.bb[i];    /* first (length-k) bits are redundancy */
        }
        for(i = 0; i < bch.k; i++)
        {
            bch.recd[i + bch.length - bch.k] = bch.data[i];    /* last k bits are data */
        }

        System.out.println("Code Word: " + Arrays.toString(bch.recd));

        /* ERRORS */

        //        printf("Enter the number of errors and their positions: ");
        //        scanf("%d", & numerr)
        //        for(i = 0; i < numerr; i++)
        //        {
        //            scanf("%d", & errpos[i])
        //            recd[errpos[i]] ^= 1;
        //        } printf("r(x) = ");
        //        for(i = 0; i < length; i++)
        //        {
        //            printf("%1d", recd[i]);
        //        }
        //        printf("\n");

        int[] r = {0, 3, 4, 5, 6, 8, 10, 14, 16, 17, 18, 20, 21, 23, 24, 25};

        bch.recd = new int[31];
        for(int bit: r)
        {
            bch.recd[bit] = 1;
        }

        System.out.println("Loaded Codeword: " + Arrays.toString(bch.recd));

        int[] errorPositions = {4, 18};

        for(int error: errorPositions)
        {
            bch.recd[error] ^= 1;
        }

        System.out.println("Codeword With Errors: " + Arrays.toString(bch.recd) + " Errors At: " + Arrays.toString(errorPositions));

        /* DECODE */
        bch.decode();
        /*
         * print out original and decoded data
         */
        System.out.println("Results");
        //        System.out.println("Original: " + Arrays.toString(bch.data));
        System.out.println("Recovered: " + Arrays.toString(bch.recd));

        /* decoding errors: we compare only the data portion */
        //        for(i = length - k; i < length; i++)
        //        {
        //            if(data[i - length + k] != recd[i])
        //            {
        //                decerror++;
        //            }
        //        }
        //        if(decerror)
        //        {
        //            printf("%d message decoding errors\n", decerror);
        //        }
        //        else
        //        {
        //            printf("Succesful decoding\n");
        //        }
    }
}
