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

import java.util.Arrays;

public class BCHDecoder {
    private static final int PRIMITIVE_POLYNOMIAL_BCH_63_61_1 = 0x43; //x^6 + x + 1
    private static final int PRIMITIVE_POLYNOMIAL_BCH_63_47_3 = 0x57; //x^6 + x^4 + x^2 + x + 1
    private static final int PRIMITIVE_POLYNOMIAL_BCH_63_16_23 = 0x73; //x^6 + x^5 + x^4 + x + 1
    private static final int N = 63; // Codeword length
    private static final int K = 16; // Message length
    private static final int T = 11; // Error-correcting capability

    private int[] alpha_to;
    private int[] index_of;
    private int[] g;

    public BCHDecoder() {
        initializeGaloisField();
        generatePolynomial();
    }

    private void initializeGaloisField() {
        // Initialize Galois Field GF(2^6)
        int m = 6;
//        int primitive_poly = 0x43; // x^6 + x + 1
        int primitive_poly = PRIMITIVE_POLYNOMIAL_BCH_63_16_23; // x^6 + x + 1
        alpha_to = new int[N + 1];
        index_of = new int[N + 1];
        int mask = 1;
        alpha_to[m] = 0;
        for (int i = 0; i < m; i++) {
            alpha_to[i] = mask;
            index_of[alpha_to[i]] = i;
            if ((primitive_poly & (1 << i)) != 0)
                alpha_to[m] ^= mask;
            mask <<= 1;
        }
        index_of[alpha_to[m]] = m;
        mask >>= 1;
        for (int i = m + 1; i < N; i++)
        {
            if (alpha_to[i - 1] >= mask)
            {
                alpha_to[i] = alpha_to[m] ^ ((alpha_to[i - 1] ^ mask) << 1);
            }
            else
            {
                alpha_to[i] = alpha_to[i - 1] << 1;
            }

            index_of[alpha_to[i]] = i;
        }

        index_of[0] = -1;
    }

    private void generatePolynomial() {
        // Generate generator polynomial
        g = new int[N - K + 1];
        g[0] = 2;
        g[1] = 1;
        for (int i = 2; i <= N - K; i++) {
            g[i] = 1;
            for (int j = i - 1; j > 0; j--)
                if (g[j] != 0)
                    g[j] = g[j - 1] ^ alpha_to[(index_of[g[j]] + i) % N];
                else
                    g[j] = g[j - 1];
            g[0] = alpha_to[(index_of[g[0]] + i) % N];
        }
    }

    public int[] decode(int[] received) {
        int[] s = calculateSyndromes(received);
        if (isZero(s)) {
            return Arrays.copyOf(received, K);
        }
        int[] elp = berlekampMassey(s);
        int[] errorLocations = chienSearch(elp);
        int[] errorValues = forney(s, errorLocations);
        int[] corrected = correctErrors(received, errorLocations, errorValues);
        return Arrays.copyOf(corrected, K);
    }

    private int[] calculateSyndromes(int[] received) {
        int[] s = new int[2 * T];
        for (int i = 1; i <= 2 * T; i++) {
            s[i - 1] = 0;
            for (int j = 0; j < N; j++)
                if (received[j] != 0)
                    s[i - 1] ^= alpha_to[(i * j) % N];
        }
        return s;
    }

    private boolean isZero(int[] polynomial) {
        for (int coeff : polynomial) {
            if (coeff != 0) return false;
        }
        return true;
    }

    private int[] berlekampMassey(int[] s) {
        // Implement Berlekamp-Massey algorithm
        // This is a simplified version and may need to be expanded for full functionality
        int[] C = new int[N];
        int[] B = new int[N];
        C[0] = 1;
        B[0] = 1;
        int L = 0;
        int m = 1;
        int b = 1;
        for (int n = 0; n < 2 * T; n++) {
            int d = s[n];
            for (int i = 1; i <= L; i++) {
                d ^= mul(C[i], s[n - i]);
            }
            if (d != 0) {
                int[] T = Arrays.copyOf(C, N);
                for (int i = 0; i < N - m; i++) {
                    C[m + i] ^= mul(d, B[i]);
                }
                if (2 * L <= n) {
                    L = n + 1 - L;
                    B = T;
                    b = d;
                    m = 0;
                }
            }
            m++;
        }
        return Arrays.copyOf(C, L + 1);
    }

    private int[] chienSearch(int[] elp) {
        // Implement Chien search to find error locations
        int[] errorLocations = new int[T];
        int count = 0;
        for (int i = 1; i <= N; i++) {
            int sum = 1;
            for (int j = 1; j < elp.length; j++) {
                sum ^= mul(elp[j], alpha_to[(j * i) % N]);
            }
            if (sum == 0) {
                errorLocations[count++] = N - i;
            }
        }
        return Arrays.copyOf(errorLocations, count);
    }

    private int[] forney(int[] s, int[] errorLocations) {
        // Implement Forney algorithm to find error values
        int[] errorValues = new int[errorLocations.length];
        for (int i = 0; i < errorLocations.length; i++) {
            int xi = alpha_to[errorLocations[i]];
            int yi = 0;
            for (int j = 0; j < 2 * T; j++) {
                yi ^= mul(s[j], alpha_to[(j * errorLocations[i]) % N]);
            }
            errorValues[i] = mul(yi, inverse(xi));
        }
        return errorValues;
    }

    private int[] correctErrors(int[] received, int[] errorLocations, int[] errorValues) {
        int[] corrected = Arrays.copyOf(received, N);
        for (int i = 0; i < errorLocations.length; i++) {
            corrected[errorLocations[i]] ^= errorValues[i];
        }
        return corrected;
    }

    private int mul(int a, int b) {
        if (a == 0 || b == 0) return 0;
        return alpha_to[(index_of[a] + index_of[b]) % N];
    }

    private int inverse(int x) {
        return alpha_to[N - index_of[x]];
    }

    public static void main(String[] args) {
        BCHDecoder decoder = new BCHDecoder();
        int[] received = new int[N]; // Fill this with your received codeword
        int[] decoded = decoder.decode(received);
        System.out.println("Decoded message: " + Arrays.toString(decoded));
    }
}