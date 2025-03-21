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

package io.github.dsheirer.edac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements Reed Solomon RS(12,9,4) error detection and correction.  The RS code is based on a shortened GF(8)
 * RS(255,252,4) code which can detect and correct up to 1x symbol error.  Since each symbol is 8-bits long, we can
 * correct up to 8-bit errors, as long as the bit errors are contained within the same 8-bit symbol.
 *
 * The DMR full link control (FLC) message is composed of 96 bits representing 12x 8-bit symbols where the first
 * 9x symbols are message codewords and the 3x trailing are parity symbols.
 *
 * This class was ported from the original implementation at:
 * https://github.com/nonoo/dmrshark/blob/master/libs/coding/rs-12-9.c
 * License: dmrshark library uses the same GPL3 license used by sdrtrunk.
 *
 * Usage: all methods are static.  Use the correct(CorrectedBinaryMessage cbm) method to detect and correct errors and
 * inspect the cbm.getCorrectedBitCount().  A non-negative value indicates success and the count of bits corrected.
 */
public class RS_12_9_DMR
{
    /**
     * Primitive polynomial used to generate the EXPONENTS_TABLE and LOG_TABLE values. See p.142, B.14
     */
    private static final int PRIMITIVE_POLYNOMIAL = 0x11D;

    // See DMR AI. spec. page 142, Table B.19
    private static final int[] EXPONENTS_TABLE = {
        0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1D, 0x3A, 0x74, 0xE8, 0xCD, 0x87, 0x13, 0x26,
        0x4C, 0x98, 0x2D, 0x5A, 0xB4, 0x75, 0xEA, 0xC9, 0x8F, 0x03, 0x06, 0x0C, 0x18, 0x30, 0x60, 0xC0,
        0x9D, 0x27, 0x4E, 0x9C, 0x25, 0x4A, 0x94, 0x35, 0x6A, 0xD4, 0xB5, 0x77, 0xEE, 0xC1, 0x9F, 0x23,
        0x46, 0x8C, 0x05, 0x0A, 0x14, 0x28, 0x50, 0xA0, 0x5D, 0xBA, 0x69, 0xD2, 0xB9, 0x6F, 0xDE, 0xA1,
        0x5F, 0xBE, 0x61, 0xC2, 0x99, 0x2F, 0x5E, 0xBC, 0x65, 0xCA, 0x89, 0x0F, 0x1E, 0x3C, 0x78, 0xF0,
        0xFD, 0xE7, 0xD3, 0xBB, 0x6B, 0xD6, 0xB1, 0x7F, 0xFE, 0xE1, 0xDF, 0xA3, 0x5B, 0xB6, 0x71, 0xE2,
        0xD9, 0xAF, 0x43, 0x86, 0x11, 0x22, 0x44, 0x88, 0x0D, 0x1A, 0x34, 0x68, 0xD0, 0xBD, 0x67, 0xCE,
        0x81, 0x1F, 0x3E, 0x7C, 0xF8, 0xED, 0xC7, 0x93, 0x3B, 0x76, 0xEC, 0xC5, 0x97, 0x33, 0x66, 0xCC,
        0x85, 0x17, 0x2E, 0x5C, 0xB8, 0x6D, 0xDA, 0xA9, 0x4F, 0x9E, 0x21, 0x42, 0x84, 0x15, 0x2A, 0x54,
        0xA8, 0x4D, 0x9A, 0x29, 0x52, 0xA4, 0x55, 0xAA, 0x49, 0x92, 0x39, 0x72, 0xE4, 0xD5, 0xB7, 0x73,
        0xE6, 0xD1, 0xBF, 0x63, 0xC6, 0x91, 0x3F, 0x7E, 0xFC, 0xE5, 0xD7, 0xB3, 0x7B, 0xF6, 0xF1, 0xFF,
        0xE3, 0xDB, 0xAB, 0x4B, 0x96, 0x31, 0x62, 0xC4, 0x95, 0x37, 0x6E, 0xDC, 0xA5, 0x57, 0xAE, 0x41,
        0x82, 0x19, 0x32, 0x64, 0xC8, 0x8D, 0x07, 0x0E, 0x1C, 0x38, 0x70, 0xE0, 0xDD, 0xA7, 0x53, 0xA6,
        0x51, 0xA2, 0x59, 0xB2, 0x79, 0xF2, 0xF9, 0xEF, 0xC3, 0x9B, 0x2B, 0x56, 0xAC, 0x45, 0x8A, 0x09,
        0x12, 0x24, 0x48, 0x90, 0x3D, 0x7A, 0xF4, 0xF5, 0xF7, 0xF3, 0xFB, 0xEB, 0xCB, 0x8B, 0x0B, 0x16,
        0x2C, 0x58, 0xB0, 0x7D, 0xFA, 0xE9, 0xCF, 0x83, 0x1B, 0x36, 0x6C, 0xD8, 0xAD, 0x47, 0x8E, 0x01
    };

    // See DMR AI. spec. page 143, Table B.20.
    private static final int[] LOG_TABLE = {0, 0, 1, 25, 2, 50, 26, 198, 3, 223, 51, 238, 27, 104, 199, 75, 4, 100, 224,
        14, 52, 141, 239, 129, 28, 193, 105, 248, 200, 8, 76, 113, 5, 138, 101, 47, 225, 36, 15, 33, 53, 147, 142, 218,
        240, 18, 130, 69, 29, 181, 194, 125, 106, 39, 249, 185, 201, 154, 9, 120, 77, 228, 114, 166, 6, 191, 139, 98,
        102, 221, 48, 253, 226, 152, 37, 179, 16, 145, 34, 136, 54, 208, 148, 206, 143, 150, 219, 189, 241, 210, 19, 92,
        131, 56, 70, 64, 30, 66, 182, 163, 195, 72, 126, 110, 107, 58, 40, 84, 250, 133, 186, 61, 202, 94, 155, 159, 10,
        21, 121, 43, 78, 212, 229, 172, 115, 243, 167, 87, 7, 112, 192, 247, 140, 128, 99, 13, 103, 74, 222, 237, 49,
        197, 254, 24, 227, 165, 153, 119, 38, 184, 180, 124, 17, 68, 146, 217, 35, 32, 137, 46, 55, 63, 209, 91, 149,
        188, 207, 205, 144, 135, 151, 178, 220, 252, 190, 97, 242, 86, 211, 171, 20, 42, 93, 158, 132, 60, 57, 83, 71,
        109, 65, 162, 31, 45, 67, 216, 183, 123, 164, 118, 196, 23, 73, 236, 127, 12, 111, 246, 108, 161, 59, 82, 41,
        157, 85, 170, 251, 96, 134, 177, 187, 204, 62, 90, 203, 89, 95, 176, 156, 169, 160, 81, 11, 245, 22, 235, 122,
        117, 44, 215, 79, 174, 213, 233, 230, 231, 173, 232, 116, 214, 244, 234, 168, 80, 88, 175
    };

    // See DMR AI. spec. page 141, B.10 for these coefficients.
    private static final int[] GENERATOR_POLYNOMIAL = {0x40, 0x38, 0x0e, 0x01};
    private static final int CODEWORD_SIZE = 12;
    private static final int CHECKSUM_SIZE = 3; //L
    private static final int POLYNOMIAL_MAXIMUM_DEGREE = (CHECKSUM_SIZE * 2);
    public static final int ERRORS_CANT_BE_CORRECTED = -1;

    /**
     * Performs error detection and correction on the Full Link Control (FLC) message.  The message should be 96-bits
     * long containing 9x 8-bit message symbols and 3x 8-bit parity symbols.  The RS(12,9,4) algorithm can detect and
     * correct up to 1x 8-bit symbol, or up to 8 bit errors if they are all contained in the same symbol.
     *
     * Error detection and correction results will be stored in the message.getCorrectedBitCount() and the message will
     * be corrected in place, if able to correct.  A corrected bit count of ERRORS_CANT_BE_CORRECTED (-1) indicates that
     * the message could not be corrected.
     *
     * @param cbm to correct.
     * @param mask to apply/remove before error detection and correction.
     */
    public static int correct(CorrectedBinaryMessage cbm, int mask)
    {
        int[] codeword = new int[12];

        for(int index = 0; index < 12; index++)
        {
            codeword[index] = (0xFF & cbm.getByte(index * 8));

            //Apply the mask to parity symbols 9-11
            if(index >= 9)
            {
                codeword[index] ^= (mask & 0xFF);
            }
        }

        int[] syndrome = calculateSyndrome(codeword);

        if(hasErrors(syndrome))
        {
            int correctedSymbolIndex = correctErrors(codeword, syndrome);

            if(correctedSymbolIndex >= 0)
            {
                //Get the previous symbol value and xor to calculate the bit error correction pattern
                int previousValue = (0xFF & cbm.getByte(correctedSymbolIndex * 8));
                int correctionPattern = previousValue ^ codeword[correctedSymbolIndex];

                //Apply the correction to the original message
                cbm.setByte(8 * correctedSymbolIndex, (byte)(0xFF & codeword[correctedSymbolIndex]));

                //Set the corrected bit count to the number of ones in the correction pattern from the corrected symbol.
                cbm.setCorrectedBitCount(Integer.bitCount(correctionPattern));
                return 0;
            }
            else
            {
                //Message can't be corrected - too many errors.
                cbm.setCorrectedBitCount(correctedSymbolIndex);

                //Calculate the checksum residual and return that as the observed alternate mask value.  Note: the
                //DMR ICD checksum has already been applied to the codeword and this calculated residual is the
                //'other' masking value that is being used on both Headers and Terminators.
                int[] checksum = calculateChecksum(codeword);

                for(int index = 0; index < 3; index++)
                {
                    checksum[index] ^= codeword[index + 9];
                }

                return checksum[0] << 16 | checksum[1] << 8 | checksum[2];
            }
        }
        else
        {
            cbm.setCorrectedBitCount(0);
            return 0;
        }
    }

    /**
     * Performs GF(8) multiplication (a + b % 255)
     * @param factorA factor
     * @param factorB factor
     * @return product
     */
    private static int galoisMultiplication(int factorA, int factorB)
    {
        if (factorA == 0 || factorB == 0)
        {
            return 0;
        }

        return EXPONENTS_TABLE[((LOG_TABLE[factorA & 0xFF] + LOG_TABLE[factorB & 0xFF]) % 255)];
    }

    /**
     * Calculates the inverse.
     * @param elt value
     * @return inverse
     */
    private static int galoisInverse(int elt)
    {
        return EXPONENTS_TABLE[255 - LOG_TABLE[elt]];
    }

    /**
     * Multiplies the polynomial by z, resulting in a shift right by 1 operation.
     * @param poly to multiply.
     */
    private static void multiplyPolynomialByZ(int[] poly)
    {
        for(int i = POLYNOMIAL_MAXIMUM_DEGREE - 1; i > 0; i--)
        {
            poly[i] = poly[i - 1];
        }

        poly[0] = 0;
    }

    /**
     * Multiplies the polynomials and returns the result.
     * @param poly1 argument
     * @param poly2 argument
     * @return result
     */
    private static int[] multiplyPolynomials(int[] poly1, int[] poly2)
    {
        int[] product = new int[POLYNOMIAL_MAXIMUM_DEGREE * 2];

        int i;
        int j;
        int[] tmp1 = new int[POLYNOMIAL_MAXIMUM_DEGREE * 2];

        for (i = 0; i < POLYNOMIAL_MAXIMUM_DEGREE * 2; i++)
        {
            product[i] = 0;
        }

        for (i = 0; i < POLYNOMIAL_MAXIMUM_DEGREE; i++)
        {
            for(j = POLYNOMIAL_MAXIMUM_DEGREE; j < (POLYNOMIAL_MAXIMUM_DEGREE *2); j++)
            {
                tmp1[j] = 0;
            }

            // Scale tmp1 by p1[i]
            for(j = 0; j < POLYNOMIAL_MAXIMUM_DEGREE; j++)
            {
                tmp1[j] = galoisMultiplication(poly2[j], poly1[i]);
            }

            // Shift (multiply) tmp1 right by i
            for (j = (POLYNOMIAL_MAXIMUM_DEGREE * 2) - 1; j >= i; j--)
            {
                tmp1[j] = tmp1[j-i];
            }

            for (j = 0; j < i; j++)
            {
                tmp1[j] = 0;
            }

            // Add into partial product.
            for (j = 0; j < (POLYNOMIAL_MAXIMUM_DEGREE *2); j++)
            {
                product[j] ^= tmp1[j];
            }
        }

        return product;
    }

    /**
     * Calculates the error evaluator polynomial (EEP). Computes the combined erasure/error evaluator polynomial
     * (error_locator_poly*syndrome mod z^4
     * @param elp error locator polynomial
     * @param syndrome calculated from the codeword
     * @return error evaluator polynomial (EEP)
     */
    private static int[] calculateEEP(int[] elp, int[] syndrome)
    {
        int[] eep = new int[POLYNOMIAL_MAXIMUM_DEGREE];
        int[] product = multiplyPolynomials(elp, syndrome);
        int i;

        for (i = 0; i < CHECKSUM_SIZE; i++)
        {
            eep[i] = product[i];
        }

        for (; i < POLYNOMIAL_MAXIMUM_DEGREE; i++)
        {
            eep[i] = 0;
        }

        return eep;
    }

    /**
     * Computes the discrepancy.
     * @param elp error locator polynomial
     * @param syndrome from the codeword
     * @param L maximum capacity to correct.
     * @param n argument
     * @return discrepancy
     */
    private static int computeDiscrepancy(int[] elp, int[] syndrome, int L, int n)
    {
        int sum = 0;

        for (int i = 0; i <= L; i++)
        {
            sum ^= galoisMultiplication(elp[i], syndrome[n - i]);
        }

        return sum;
    }

    /**
     * Finds the coefficients of the error locator polynomial (ELP) and then calculates the error evaluator
     * (EEP) using the Berlekamp-Massey algorithm.  From Cain, Clark, "Error-Correction Coding For Digital
     * Communications", pp. 216.
     * @param syndrome calculated from the codeword
     * @param elp calculated.
     * @return EEP
     */
    private static int[] calculateELP(int[] syndrome)
    {
        int[] elp = new int[POLYNOMIAL_MAXIMUM_DEGREE];
        int[] psi2 = new int[POLYNOMIAL_MAXIMUM_DEGREE];
        int[] bigD = new int[POLYNOMIAL_MAXIMUM_DEGREE];

        int L = 0;
        int L2;
        int k = -1;
        int d;
        int i;

        bigD[1] = 1;
        elp[0] = 1;

        for(int n = 0; n < CHECKSUM_SIZE; n++)
        {
            d = computeDiscrepancy(elp, syndrome, L, n);

            if (d != 0)
            {
                // psi2 = error_locator_poly - d*D
                for (i = 0; i < POLYNOMIAL_MAXIMUM_DEGREE; i++)
                {
                    psi2[i] =  (elp[i] ^ galoisMultiplication(d, bigD[i]));
                }

                if (L < (n - k))
                {
                    L2 = (n - k);
                    k = (n - L);

                    for (i = 0; i < POLYNOMIAL_MAXIMUM_DEGREE; i++)
                    {
                        bigD[i] = galoisMultiplication(elp[i], galoisInverse(d));
                    }

                    L = L2;
                }

                // error_locator_poly = psi2
                for (i = 0; i < POLYNOMIAL_MAXIMUM_DEGREE; i++)
                {
                    elp[i] = psi2[i];
                }
            }

            multiplyPolynomialByZ(bigD);
        }

        return elp;
    }

    /**
     * The error-locator polynomial's roots are found by looking for the values of a^n where evaluating the polynomial
     * yields zero (evaluating rs_12_9_error_locator_poly at successive values of alpha (Chien's search)).
     * @param elp error locator polynomial.
     * @return error location roots.
     */
    private static List<Integer> findRoots(int[] elp)
    {
        List<Integer> roots = new ArrayList<>();
        int sum;
        int k;

        for(int r = 1; r < 256; r++)
        {
            sum = 0;

            // Evaluate ELP at r
            for(k = 0; k < CHECKSUM_SIZE + 1; k++)
            {
                sum ^= galoisMultiplication(EXPONENTS_TABLE[(k * r) % 255], elp[k]);
            }

            if(sum == 0)
            {
                roots.add(255 - r);
            }
        }

        return roots;
    }

    /**
     * Calculates the syndrome for a codeword.
     * @param codeword to check.
     * @return syndrome.
     */
    private static int[] calculateSyndrome(int[] codeword)
    {
        if(codeword.length != CODEWORD_SIZE)
        {
            throw new IllegalArgumentException("Codeword length [" + codeword.length + "] should be " + CODEWORD_SIZE +
                    " symbols");
        }

        int[] syndrome = new int[POLYNOMIAL_MAXIMUM_DEGREE];

        int i, temp;

        for(int j = 0; j < 3;  j++)
        {
            for (i = 0; i < codeword.length; i++)
            {
                temp = galoisMultiplication(EXPONENTS_TABLE[j + 1], syndrome[j]);
                temp ^= codeword[i];
                syndrome[j] = (0xFF & temp);
            }
        }

        return syndrome;
    }

    /**
     * Checks the syndrome array for non-zero values indicating an error.
     * @param syndrome to check
     * @return true if the syndrome array has any non-zero values.
     */
    private static boolean hasErrors(int[] syndrome)
    {
        for(int symbol: syndrome)
        {
            if(symbol != 0)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Corrects errors in the codeword using the calculated syndrome.
     * @param codeword to correct.  The codeword will be modified with corrections, if able.
     * @param syndrome calculated from the codeword.
     * @return -1 if the codeword can't be corrected or the index of the symbol that was corrected.
     */
    private static int correctErrors(int[] codeword, int[] syndrome)
    {
        int r;
        int i;
        int j;
        int errorMask;
        int num, denom;

        //Error Locator Polynomial (ELP)
        int[] elp = calculateELP(syndrome);

        //Error Evaluator Polynomial (EEP)
        int[] eep = calculateEEP(elp, syndrome);

        List<Integer> roots = findRoots(elp);

        if (roots.isEmpty())
        {
            return ERRORS_CANT_BE_CORRECTED;
        }

        // Error correction is done using the error-evaluator equation on pp 207.
        if (roots.size() <= CHECKSUM_SIZE)
        {
            // First check for illegal error locations.
            for (r = 0; r < roots.size(); r++)
            {
                if (roots.get(r) >= CODEWORD_SIZE)
                {
                    return ERRORS_CANT_BE_CORRECTED;
                }
            }

            int errorsCorrected = 0;

            // Evaluates rs_12_9_error_evaluator_poly/rs_12_9_error_locator_poly' at the roots
            // alpha^(-i) for error locs i.
            for (r = 0; r < roots.size(); r++)
            {
                i = roots.get(r);

                // Evaluate rs_12_9_error_evaluator_poly at alpha^(-i)
                num = 0;
                for (j = 0; j < POLYNOMIAL_MAXIMUM_DEGREE; j++)
                {
                    num ^= galoisMultiplication(eep[j], EXPONENTS_TABLE[((255 - i) * j) % 255]);
                }

                // Evaluate rs_12_9_error_evaluator_poly' (derivative) at alpha^(-i). All odd powers disappear.
                denom = 0;

                for (j = 1; j < POLYNOMIAL_MAXIMUM_DEGREE; j += 2)
                {
                    denom ^= galoisMultiplication(elp[j], EXPONENTS_TABLE[((255-i)*(j-1)) % 255]);
                }

                errorMask = galoisMultiplication(num, galoisInverse(denom));
                int index = CODEWORD_SIZE - i - 1;
                codeword[index] ^= errorMask;
                return index;
            }

            return errorsCorrected;
        }

        return ERRORS_CANT_BE_CORRECTED;
    }

    /**
     * Calculates the checksum parity symbols for the codeword using the DMR generator polynomial.
     * @param codeword to calculate.
     * @return checksum bytes (3)
     */
    private static int[] calculateChecksum(int[] codeword)
    {
        int[] checksum = new int[CHECKSUM_SIZE];

        int i;
        int feedback;

        for (i = 0; i < 9; i++)
        {
            feedback = (codeword[i] ^ checksum[0]);
            checksum[0] = (checksum[1] ^ galoisMultiplication(GENERATOR_POLYNOMIAL[2], feedback));
            checksum[1] = (checksum[2] ^ galoisMultiplication(GENERATOR_POLYNOMIAL[1], feedback));
            checksum[2] = galoisMultiplication(GENERATOR_POLYNOMIAL[0], feedback);
        }

        return checksum;
    }
}
