/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.edac;

import io.github.dsheirer.bits.BinaryMessage;

public class ReedSolomon_12_9 {
    static final int RS_12_9_DATASIZE = 9;
    static final int RS_12_9_CHECKSUMSIZE = 3;
    static final int RS_12_9_POLY_MAXDEG = RS_12_9_CHECKSUMSIZE * 2;

    public class rs_12_9_roots_t {
        int[] error_locations = new int[256];
        int errors_num;
    }

    ;

    public ReedSolomon_12_9() {

    }

    // See DMR AI. spec. page 138.
    static int[] rs_12_9_galois_exp_table = {
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
            0x2C, 0x58, 0xB0, 0x7D, 0xFA, 0xE9, 0xCF, 0x83, 0x1B, 0x36, 0x6C, 0xD8, 0xAD, 0x47, 0x8E, 0x01,
    };

    // See DMR AI. spec. page 138.
    static int[] rs_12_9_galois_log_table = {
            0, 0, 1, 25, 2, 50, 26, 198, 3, 223, 51, 238, 27, 104, 199, 75,
            4, 100, 224, 14, 52, 141, 239, 129, 28, 193, 105, 248, 200, 8, 76, 113,
            5, 138, 101, 47, 225, 36, 15, 33, 53, 147, 142, 218, 240, 18, 130, 69,
            29, 181, 194, 125, 106, 39, 249, 185, 201, 154, 9, 120, 77, 228, 114, 166,
            6, 191, 139, 98, 102, 221, 48, 253, 226, 152, 37, 179, 16, 145, 34, 136,
            54, 208, 148, 206, 143, 150, 219, 189, 241, 210, 19, 92, 131, 56, 70, 64,
            30, 66, 182, 163, 195, 72, 126, 110, 107, 58, 40, 84, 250, 133, 186, 61,
            202, 94, 155, 159, 10, 21, 121, 43, 78, 212, 229, 172, 115, 243, 167, 87,
            7, 112, 192, 247, 140, 128, 99, 13, 103, 74, 222, 237, 49, 197, 254, 24,
            227, 165, 153, 119, 38, 184, 180, 124, 17, 68, 146, 217, 35, 32, 137, 46,
            55, 63, 209, 91, 149, 188, 207, 205, 144, 135, 151, 178, 220, 252, 190, 97,
            242, 86, 211, 171, 20, 42, 93, 158, 132, 60, 57, 83, 71, 109, 65, 162,
            31, 45, 67, 216, 183, 123, 164, 118, 196, 23, 73, 236, 127, 12, 111, 246,
            108, 161, 59, 82, 41, 157, 85, 170, 251, 96, 134, 177, 187, 204, 62, 90,
            203, 89, 95, 176, 156, 169, 160, 81, 11, 245, 22, 235, 122, 117, 44, 215,
            79, 174, 213, 233, 230, 231, 173, 232, 116, 214, 244, 234, 168, 80, 88, 175
    };

    static int rs_12_9_galois_exp_table_get(int pos) {
        return rs_12_9_galois_exp_table[pos];
    }

    static int rs_12_9_galois_multiplication(int a, int b) {
        if (a == 0 || b == 0)
            return 0;

        return rs_12_9_galois_exp_table[(rs_12_9_galois_log_table[a] + rs_12_9_galois_log_table[b]) % 255];
    }

    static int rs_12_9_galois_inv(int elt) {
        return rs_12_9_galois_exp_table[255 - rs_12_9_galois_log_table[elt]];
    }

    // Multiply by z (shift right by 1).
    static void rs_12_9_multiply_poly_z(int[] poly) {
        int i;

        for (i = RS_12_9_POLY_MAXDEG - 1; i > 0; i--)
            poly[i] = poly[i - 1];
        poly[0] = 0;
    }

    static void rs_12_9_multiplicate_polys(int dst[], int[] p1, int[] p2) {
        byte i;
        byte j;
        int[] tmp1 = new int[RS_12_9_POLY_MAXDEG * 2];

        for (i = 0; i < RS_12_9_POLY_MAXDEG * 2; i++)
            dst[i] = 0;

        for (i = 0; i < RS_12_9_POLY_MAXDEG; i++) {
            for (j = RS_12_9_POLY_MAXDEG; j < (RS_12_9_POLY_MAXDEG * 2); j++)
                tmp1[j] = 0;

            // Scale tmp1 by p1[i]
            for (j = 0; j < RS_12_9_POLY_MAXDEG; j++)
                tmp1[j] = rs_12_9_galois_multiplication(p2[j], p1[i]);

            // Shift (multiply) tmp1 right by i
            for (j = (RS_12_9_POLY_MAXDEG * 2) - 1; j >= i; j--)
                tmp1[j] = tmp1[j - i];
            for (j = 0; j < i; j++)
                tmp1[j] = 0;

            // Add into partial product.
            for (j = 0; j < (RS_12_9_POLY_MAXDEG * 2); j++)
                dst[j] ^= tmp1[j];
        }
    }

    // Computes the combined erasure/error evaluator polynomial (error_locator_poly*syndrome mod z^4)
    static void rs_12_9_calc_error_evaluator_poly(int[] error_locator_poly, int[] syndrome, int[] error_evaluator_poly) {
        byte i;
        int[] product = new int[RS_12_9_POLY_MAXDEG * 2];

        rs_12_9_multiplicate_polys(product, error_locator_poly, syndrome);
        for (i = 0; i < RS_12_9_CHECKSUMSIZE; i++)
            error_evaluator_poly[i] = product[i];
        for (; i < RS_12_9_POLY_MAXDEG; i++)
            error_evaluator_poly[i] = 0;
    }

    static int rs_12_9_compute_discrepancy(int[] error_locator_poly, int[] syndrome, int L, int n) {
        byte i;
        int sum = 0;

        for (i = 0; i <= L; i++)
            sum ^= rs_12_9_galois_multiplication(error_locator_poly[i], syndrome[n - i]);

        return sum;
    }

    // This finds the coefficients of the error locator polynomial, and then calculates
// the error evaluator polynomial using the Berlekamp-Massey algorithm.
// From  Cain, Clark, "Error-Correction Coding For Digital Communications", pp. 216.
    static void rs_12_9_calculate(int[] syndrome, int[] error_locator_poly, int[] error_evaluator_poly) {
        byte n;
        int L = 0;
        int L2;
        int k;
        int d;
        byte i;
        int[] psi2 = new int[RS_12_9_POLY_MAXDEG];
        int[] D = {0, 1, 0, 0, 0, 0};

        k = -1;

        error_locator_poly[0] = 1;

        for (n = 0; n < RS_12_9_CHECKSUMSIZE; n++) {
            d = rs_12_9_compute_discrepancy(error_locator_poly, syndrome, L, n);

            if (d != 0) {
                // psi2 = error_locator_poly - d*D
                for (i = 0; i < RS_12_9_POLY_MAXDEG; i++)
                    psi2[i] = error_locator_poly[i] ^ rs_12_9_galois_multiplication(d, D[i]);

                if (L < (n - k)) {
                    L2 = n - k;
                    k = n - L;

                    for (i = 0; i < RS_12_9_POLY_MAXDEG; i++)
                        D[i] = rs_12_9_galois_multiplication(error_locator_poly[i], rs_12_9_galois_inv(d));

                    L = L2;
                }

                // error_locator_poly = psi2
                for (i = 0; i < RS_12_9_POLY_MAXDEG; i++)
                    error_locator_poly[i] = psi2[i];
            }

            rs_12_9_multiply_poly_z(D);
        }

        rs_12_9_calc_error_evaluator_poly(error_locator_poly, syndrome, error_evaluator_poly);
    }

    // The error-locator polynomial's roots are found by looking for the values of a^n where
// evaluating the polynomial yields zero (evaluating rs_12_9_error_locator_poly at
// successive values of alpha (Chien's search)).
    rs_12_9_roots_t rs_12_9_find_roots(int[] error_locator_poly) {
        rs_12_9_roots_t roots = new rs_12_9_roots_t();
        int sum;
        int r;
        int k;


        for (r = 1; r < 256; r++) {
            sum = 0;
            // Evaluate rs_12_9_error_locator_poly at r
            for (k = 0; k < RS_12_9_CHECKSUMSIZE + 1; k++)
                sum ^= rs_12_9_galois_multiplication(rs_12_9_galois_exp_table_get((k * r) % 255), error_locator_poly[k]);

            if (sum == 0)
                roots.error_locations[roots.errors_num++] = (255 - r);
        }
        return roots;
    }

    static void rs_12_9_calc_syndrome(int[] codeword, int[] syndrome) {
        int i, j;

        syndrome[0] = syndrome[1] = syndrome[2] = 0;

        for (j = 0; j < 3; j++) {
            for (i = 0; i < codeword.length; i++)
                syndrome[j] = codeword[i] ^ rs_12_9_galois_multiplication(rs_12_9_galois_exp_table_get(j + 1), syndrome[j]);
        }
    }

    // Returns 1 if syndrome differs from all zeroes.
    static boolean rs_12_9_check_syndrome(int[] syndrome) {
        int i;

        for (i = 0; i < 3; i++) {
            if (syndrome[i] != 0)
                return true;
        }

        return false;
    }

    // Returns 1 if errors have been found and corrected, returns 0 if
// no errors found or errors can't be corrected.
    int rs_12_9_correct_errors(int[] codeword, int[] syndrome) {
        int r;
        int i;
        int j;
        int err;//RS_12_9_DATASIZE+RS_12_9_CHECKSUMSIZE
        int[] error_locator_poly = new int[RS_12_9_POLY_MAXDEG];
        int[] error_evaluator_poly = new int[RS_12_9_POLY_MAXDEG];
        int num, denom;
        int errors_found = 0;
        rs_12_9_calculate(syndrome, error_locator_poly, error_evaluator_poly);
        rs_12_9_roots_t roots = rs_12_9_find_roots(error_locator_poly);
        errors_found = roots.errors_num;

        if (roots.errors_num == 0)
            return 0;

        // Error correction is done using the error-evaluator equation on pp 207.
        if (roots.errors_num > 0 && roots.errors_num <= RS_12_9_CHECKSUMSIZE) {
            // First check for illegal error locations.
            for (r = 0; r < roots.errors_num; r++) {
                if (roots.error_locations[r] >= RS_12_9_DATASIZE + RS_12_9_CHECKSUMSIZE)
                    return 2;
            }

            // Evaluates rs_12_9_error_evaluator_poly/rs_12_9_error_locator_poly' at the roots
            // alpha^(-i) for error locs i.
            for (r = 0; r < roots.errors_num; r++) {
                i = roots.error_locations[r];

                // Evaluate rs_12_9_error_evaluator_poly at alpha^(-i)
                num = 0;
                for (j = 0; j < RS_12_9_POLY_MAXDEG; j++)
                    num ^= rs_12_9_galois_multiplication(error_evaluator_poly[j], rs_12_9_galois_exp_table_get(((255 - i) * j) % 255));

                // Evaluate rs_12_9_error_evaluator_poly' (derivative) at alpha^(-i). All odd powers disappear.
                denom = 0;
                for (j = 1; j < RS_12_9_POLY_MAXDEG; j += 2)
                    denom ^= rs_12_9_galois_multiplication(error_locator_poly[j], rs_12_9_galois_exp_table_get(((255 - i) * (j - 1)) % 255));

                err = rs_12_9_galois_multiplication(num, rs_12_9_galois_inv(denom));
                codeword[RS_12_9_DATASIZE + RS_12_9_CHECKSUMSIZE - i - 1] ^= err;
            }
            return 1;
        }

        return 2;
    }

    // Simulates an LFSR with the generator polynomial and calculates checksum bytes for the given data.
    int[] rs_12_9_calc_checksum(int[] codeword) {
        // See DMR AI. spec. page 136 for these coefficients.
        int genpoly[] = {0x40, 0x38, 0x0e, 0x01};
        int[] rs_12_9_checksum = new int[3];
        int i;
        int feedback;

        rs_12_9_checksum[0] = rs_12_9_checksum[1] = rs_12_9_checksum[2] = 0;

        for (i = 0; i < 9; i++) {
            feedback = codeword[i] ^ rs_12_9_checksum[0];

            rs_12_9_checksum[0] = rs_12_9_checksum[1] ^ rs_12_9_galois_multiplication(genpoly[2], feedback);
            rs_12_9_checksum[1] = rs_12_9_checksum[2] ^ rs_12_9_galois_multiplication(genpoly[1], feedback);
            rs_12_9_checksum[2] = rs_12_9_galois_multiplication(genpoly[0], feedback);
        }
        return rs_12_9_checksum;
    }
    public static ReedSolomon_12_9 instance = null;
    public static int checkReedSolomon(BinaryMessage message, int messageStart, int CRCstart, int mode) {
        if(instance == null) {
            instance = new ReedSolomon_12_9();
        }
        int[] code = message.toIntegerArray();
        code[9] ^= mode;
        code[10] ^= mode;
        code[11] ^= mode;
        int[] syndrome = new int[RS_12_9_POLY_MAXDEG];
        rs_12_9_calc_syndrome(code, syndrome);
        if(rs_12_9_check_syndrome(syndrome)) {
            return instance.rs_12_9_correct_errors(code, syndrome);
        }
        return 2;
    }
}
