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

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import org.apache.commons.lang3.Validate;

public class BCH
{
    private int mN;
    private int mK;
    private int mT;
    private GFX mG;
    private boolean mSystematic;

    /**
     * Constructs a (n/k) BCH decoder capable of correcting t errors.
     * @param n codeword length
     * @param k parity bit count
     * @param t maximum correctable errors
     * @param systematic if the
     */
    public BCH(int n, int k, int t, boolean systematic)
    {
        //TODO: validate size of n as a power of two minus 1.
        mN = n;
        mK = k;
        mT = t;
        mSystematic = systematic;
        int[] exponents = getGeneratorPolynomialExponents();
        Validate.isTrue(exponents.length == (mN - mK + 1));
        mG = new GFX(mN + 1, exponents);
    }

    public boolean decode(CorrectedBinaryMessage coded_bits)
    {
        CorrectedBinaryMessage decoded_message = new CorrectedBinaryMessage(coded_bits.size());

        boolean decoderfailure = false, no_dec_failure = true;
        int j, i, degree, foundzeros;
        int iterations = (int)Math.floor((double)coded_bits.size() / mN);
        BinaryMessage rbin = new BinaryMessage(mN);
        BinaryMessage mbin = new BinaryMessage(mK);
        decoded_message.setSize(iterations * mK); //Shouldn't iterations always be a 1 and this should be mK??
        BinaryMessage cw_isvalid = new BinaryMessage(iterations); //This also can be reduced to just a simple boolean
        GFX c = new GFX(mN + 1, mN - 1);
        GFX m = new GFX(mN + 1, mK - 1);
        GFX r = new GFX(mN + 1, mN - 1);
        GFX S = new GFX(mN + 1, 2 * mT);

        for(i = 0; i < iterations; i++)
        {
            decoderfailure = false;

            //Fix the received polynomial r(x)
            rbin = coded_bits.get(i * mN, mN);

            for(j = 0; j < mN; j++)
            {
                degree = rbin.get(mN - j - 1) ? 0 : -1; //0 or -1 from the index form
                r.set(j, new GF(mN  + 1, degree));
            }

            //Fix the syndrome polynomial S(x)
            S.clear();
            for(j = 1; j <= 2 * mT; j++)
            {
                GF gf1 = new GF(mN + 1, j);
                GF syndromeCoefficient = r.evaluate(gf1);
                S.set(j, syndromeCoefficient);
            }

            int sTrueDegree = S.get_true_degree();

            if(S.get_true_degree() >= 1)  //Errors in the received word
            {
                GFX Lambda = new GFX(mN + 1);
                GFX OldLambda = new GFX(mN + 1);
                GFX T = new GFX(mN + 1);
                GFX Omega = new GFX(mN + 1);
                GFX One = new GFX(mN + 1, new int[]{0});
                GF delta = new GF(mN + 1);
                int[] errorpos;

                //Iterate to find Lambda(x)
                int kk = 0;
                Lambda = new GFX(mN + 1, new int[]{0});
                T = new GFX(mN + 1, new int[]{0});
                while(kk < mT) //Step 5
                {
                    GFX syndromeAddOne = S.add(One);
                    GFX oneAddSyndrome = One.add(S);

//                    Omega = Lambda.multiply(syndromeAddOne); //Berlekemp - p.212, Step 2
                    Omega = Lambda.multiply(oneAddSyndrome); //Berlekemp - p.212, Step 2
                    int deltaIndex = 2 * kk + 1;
                    delta = Omega.get(deltaIndex); //Berlekemp - p.212, Step 2
                    OldLambda = Lambda;  //Line 246

                    //Berlekamp - p.212, Step 3
                    GFX step3a = new GFX(mN + 1, new int[]{-1, 0});
                    GFX step3b = step3a.multiply(T);
                    GFX step3c = delta.multiply(step3b);
                    GFX step3d = OldLambda.add(step3c);
                    Lambda = OldLambda.add(delta.multiply(new GFX(mN + 1, new int[]{-1, 0}).multiply(T)));

                    //Berlekamp - p.212, Step 4
//                    boolean caseA = delta.eq(new GF(mN + 1, -1));  //Original: this seems like it always evaluates to true?
                    boolean caseA = delta.get_value() == -1;
                    boolean caseB = OldLambda.get_true_degree() > kk;

//                    if(caseA || caseB)
                    if((delta.eq(new GF(mN + 1, -1)) || (OldLambda.get_true_degree() > kk)))
                    {
                        T = new GFX(mN + 1, new int[]{-1, -1, 0}).multiply(T);
                    }
                    else
                    {
                        T = new GFX(mN + 1, new int[]{-1, 0}).multiply(OldLambda).divide(delta);
                    }

                    kk++; //Step 5
                }

                //Find the zeros to Lambda(x)
                errorpos = new int[Lambda.get_true_degree()];
                foundzeros = 0;

                for(j = 0; j <= mN - 1; j++)
                {
                    if(Lambda.evaluate(new GF(mN + 1, j)).eq(new GF(mN + 1, -1)))
                    {
                        errorpos[foundzeros] = (mN - j) % mN;
                        foundzeros++;

                        if(foundzeros >= Lambda.get_true_degree())
                        {
                            break;
                        }
                    }
                }

                if(foundzeros != Lambda.get_true_degree())
                {
                    decoderfailure = true;
                }
                else
                {
                    //Correct the codeword
                    for(j = 0; j < foundzeros; j++)
                    {
                        //Changed this from set to flip
                        rbin.flip(mN - errorpos[j] - 1); //reverse mapping
                    }

                    //Reconstruct the corrected codeword
                    for(j = 0; j < mN; j++)
                    {
                        degree = rbin.get(mN - j - 1) ? 0 : -1;
                        c.set(j, new GF(mN + 1, degree));
                    }

                    //Codeword validation
                    S.clear();
                    for(j = 1; j <= (2 * mT); j++)
                    {
                        S.set(j, c.evaluate(new GF(mN + 1, j)));
                    }

                    decoderfailure = S.get_true_degree() > 0;
                }
            }
            else
            {
                c = r;
            }

            //Construct the message bit vector
            if(!decoderfailure) //c(x) is a valid codeword
            {
                if(c.get_true_degree() > 1)
                {
                    if(mSystematic)
                    {
                        for(j = 0; j < mK; j++)
                        {
                            m.set(j, c.get(mN - mK + j));
                        }
                    }
                    else
                    {
                        m = GFX.divide(c, mG);
                    }

                    mbin.clear();

                    for(j = 0; j <= m.get_true_degree(); j++)
                    {
                        if(m.get(j).eq(new GF(mN + 1, 0)))
                        {
                            mbin.set(mK - j - 1);
                        }
                    }
                }
            }
            else //decoder failure
            {
                if(mSystematic)
                {
                    mbin = coded_bits;
                }
                else
                {
                    mbin.clear();
                }

                no_dec_failure = false;
            }

//TODO: reassign mbin to the decoded message to replace the errors.
//            decoded_message.replace_mid(i * k, mbin);
            cw_isvalid.set(i, !decoderfailure);
        }

        return no_dec_failure;
    }

    public static int[] toArray(CorrectedBinaryMessage message)
    {
        int[] array = new int[message.length()];

        for(int i = 0; i < message.size(); i++)
        {
            array[i] = message.get(i) ? 1 : 0;
        }

        return array;
    }

    /**
     * Access the generator polynomial as an integer array of
     * @return
     */
    public int[] getGeneratorPolynomialExponents()
    {
        //P25 Generator Polynomial: g(x)
        // Octal:   6  3  3  1  1  4  1  3  6  7  2  3  5  4  5  3
        //Binary: 110011011001001100001011110111010011101100101011

        BinaryMessage bits = BinaryMessage.load("110011011001001100001011110111010011101100101011");
        int[] exponents = new int[48];
        for(int i = 0; i < exponents.length; i++)
        {
            //Load the exponents in reverse order
            exponents[i] = bits.get(48 - i - 1) ? 1 : 0;
        }

        return exponents;
    }

    public static void main(String[] args)
    {
        /**
         * NAC: 1, DUID:0 (HDU), line 12 in the FDMA Table 19 NID Generator matrix.
         *
         * Octal: 00 0020 1430 2667 7236 1044
         *   Bin: 0 000 000 000 010 000 001 100 011 000 010 110 110 111 111 010 011 110 001 000 100 100
         *   Bin: 0000 0000 0001 0000 0011 0001 1000 0101 1011 0111 1110 1001 1110 0010 0010 0100
         *   Hex:    0    0    1    0    3    1    8    5    B    7    E    9    E    2    2    4
         *   Hex:    00103185B7E9E224
         */

        long[] matrix = new long[16];
        matrix[0] = Long.parseLong("6331141367235452", 8);
        matrix[1] = Long.parseLong("5265521614723276", 8);
        matrix[2] = Long.parseLong("4603711461164164", 8);
        matrix[3] = Long.parseLong("2301744630472072", 8);
        matrix[4] = Long.parseLong("7271623073000466", 8);
        matrix[5] = Long.parseLong("5605650752635660", 8);
        matrix[6] = Long.parseLong("2702724365316730", 8);
        matrix[7] = Long.parseLong("1341352172547354", 8);
        matrix[8] = Long.parseLong("0560565075263566", 8);
        matrix[9] = Long.parseLong("6141333751704220", 8);
        matrix[10] = Long.parseLong("3060555764742110", 8);
        matrix[11] = Long.parseLong("1430266772361044", 8);
        matrix[12] = Long.parseLong("0614133375170422", 8);
        matrix[13] = Long.parseLong("6037114611641642", 8);
        matrix[14] = Long.parseLong("5326507063515373", 8);
        matrix[15] = Long.parseLong("4662302756473127", 8);

        int[][] nadDUID = new int[][]{{1, 0}, {2, 3}, {3, 5}, {4, 7}, {5, 10}, {6, 12}, {7, 15}}; //NAC + DUID
        int[] nacIndices = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
        int[] duidIndices = new int[]{12, 13, 14, 15};

        BCH bch = new BCH(63, 16, 11, true);

        for(int[] combo : nadDUID)
        {
            CorrectedBinaryMessage cbm = new CorrectedBinaryMessage(64);
            cbm.setInt(combo[0], nacIndices);
            cbm.setInt(combo[1], duidIndices);

            long parity = 0;
            for(int x = 0; x < 16; x++)
            {
                if(cbm.get(x))
                {
                    parity ^= matrix[x];
                }
            }

            cbm.load(16, 48, parity);

            int errorIndex = 0;
            cbm.flip(errorIndex);

            String before = cbm.toHexString();
            boolean decoded = bch.decode(cbm);
            System.out.println("NAC:" + combo[0] + " DUID:" + combo[1] + " CODEWORD: " + before + " AFTER:" + cbm.toHexString() + " DECODED: " + decoded);

            boolean decoded1BitError = bch.decode(cbm);
            System.out.println("\t1-BitError: " + decoded1BitError);

        }
    }
}
