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
    private GFX mG = new GFX();
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
        mG.set(mN + 1, exponents);
    }

    public boolean decode(CorrectedBinaryMessage coded_bits)
    {
        CorrectedBinaryMessage decoded_message = new CorrectedBinaryMessage(coded_bits.size());

        boolean decoderfailure = false, no_dec_failure = true;
        int j, i, degree, kk, foundzeros;
        int[] errorpos;
        int iterations = (int)Math.floor((double)coded_bits.size() / mN);
        BinaryMessage rbin = new BinaryMessage(mN);
        BinaryMessage mbin = new BinaryMessage(mK);
        decoded_message.setSize(iterations * mK); //Shouldn't iterations always be a 1 and this should be mK??
        BinaryMessage cw_isvalid = new BinaryMessage(iterations); //This also can be reduced to just a simple boolean
        GFX r = new GFX(mN + 1, mN - 1);
        GFX c = new GFX(mN + 1, mN - 1);
        GFX m = new GFX(mN + 1, mK - 1);
        GFX S = new GFX(mN + 1, 2 * mT);
        GFX Lambda = new GFX(mN + 1);
        GFX OldLambda = new GFX(mN + 1);
        GFX T = new GFX(mN + 1);
        GFX Omega = new GFX(mN + 1);
        GFX One = new GFX(mN + 1, 0);
        GF delta = new GF(mN + 1);

        for(i = 0; i < iterations; i++)
        {
            decoderfailure = false;

            //Fix the received polynomial r(x)
            rbin = coded_bits.get(i * mN, mN);

            for(j = 0; j < mN; j++)
            {
                degree = rbin.get(mN - j - 1) ? 0 : -1; //0 or -1
                r.set(j, new GF(mN  + 1, degree));
            }

            //Fix the syndrome polynomial S(x)
            S.clear();
            for(j = 1; j <= 2 * mT; j++)
            {
                S.set(j, r.evaluate(new GF(mN + 1, j)));
            }

            if(S.get_true_degree() >= 1)  //Errors in the received word
            {
                //Iterate to find Lambda(x)
                kk = 0;
                Lambda = new GFX(mN + 1, 0);
                T = new GFX(mN + 1, 0);
                while(kk < mT) //Step 5
                {
                    Omega = Lambda.multiply(S.add(One));
                    delta = Omega.get(2 * kk + 1);
                    OldLambda = Lambda;  //Line 246

                    //Berlekamp - p.212, Step 3
//TODO:  fix this                  Lambda = OldLambda.add(delta.multiply(new GFX(mN + 1, new int[]{-1, 0}).multiply(T)));

                    //Berlekamp - p.212, Step 4
                    if((delta.equals(new GF(mN + 1, -1)) || (OldLambda.get_true_degree() > kk)))
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
                        rbin.set(mN - errorpos[j] - 1); //reverse mapping
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
        //P25 Generator Polynomial: g(x) = 6331 1413 6723 5453 in octal
        //As Binary: 11001101 10010011 00001011 11011101 00111011 00101011

        BinaryMessage bits = BinaryMessage.load("110011011001001100001011110111010011101100101011");
        int[] exponents = new int[48];
        for(int i = 0; i < exponents.length; i++)
        {
            //Load the exponents in reverse where a binary 1 is represented as an integer zero and a binary 0 as a -1'
            exponents[i] = bits.get(48 - i - 1) ? 0 : -1;
        }

        return exponents;
    }
}
