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

public class BCHDecoder
{
    public static final int PRIMITIVE_POLYNOMIAL_GF_3 = 0x7;
    public static final int PRIMITIVE_POLYNOMIAL_GF_7 = 0xB;
    public static final int PRIMITIVE_POLYNOMIAL_GF_15 = 0x13;
    public static final int PRIMITIVE_POLYNOMIAL_GF_31 = 0x25;
    public static final int PRIMITIVE_POLYNOMIAL_GF_63 = 0x43;
    public static final int PRIMITIVE_POLYNOMIAL_GF_127 = 0x83;
    public static final int PRIMITIVE_POLYNOMIAL_GF_255 = 0x11D;

    private int mPrimitivePolynomial = PRIMITIVE_POLYNOMIAL_GF_31;
    private int mM = 5;
    private int mN = 31;
    private int mK = 21;
    private int mT = 2;
    private int[] a_pow_tab;
    private int[] a_log_tab;
    private int[] xi_tab;

    public BCHDecoder(int m, int k, int t, int primitivePolynomial)
    {
        mM = m;
        mK = k;
        mT = t;
        mN = (1 << m) - 1;
        mPrimitivePolynomial = primitivePolynomial;
        initGFTables();

        System.out.println("ALPHA_TO: " + Arrays.toString(a_pow_tab));
        System.out.println("INDEX_OF: " + Arrays.toString(a_log_tab));
        System.out.println("XI_TAB: " + Arrays.toString(xi_tab));
    }

    public BCHDecoder()
    {
        initGFTables();
    }

    /*
     * Creates lookup table for finding the roots of a degree 2 error locator polynomial.
     * See: https://github.com/Parrot-Developers/bch.c build_deg2_base() method.
     */
    private void buildDegree2Base()
    {
        xi_tab = new int[mM];

        int i, j, r, sum, x, y, remaining, ak = 0;
        int[] xi = new int[mM];

        /* find k s.t. Tr(a^k) = 1 and 0 <= k < m */
        for(i = 0; i < mM; i++)
        {
            sum = 0;
            for(j = 0; j < mM; j++)
            {
                sum ^= a_pow(i * (1 << j));
            }

            if(sum != 0)
            {
                ak = a_pow_tab[i];
                break;
            }
        }

        /* find xi, i=0..m-1 such that xi^2+xi = a^i+Tr(a^i).a^k */
        remaining = mM;

        for(x = 0; (x <= mN) && remaining != 0; x++)
        {
            y = gf_sqr(x) ^ x;
            for(i = 0; i < 2; i++)
            {
                r = a_log(y);

                if(y != 0 && (r < mM) && xi[r] == 0)
                {
                    xi_tab[r] = x;
                    xi[r] = 1;
                    remaining--;
                    break;
                }

                y ^= ak;
            }
        }

        if(remaining != 0)
        {
            throw new IllegalStateException("Unexpected remaining value: " + remaining);
        }
    }

    /*
     * find roots of a polynomial, using BTZ algorithm; see the beginning of this
     * file for details
     */
    public int[] find_poly_roots(GFPoly poly)
    {
        int[] roots = new int[0];

        switch(poly.mDegree)
        {
            /* handle low degree polynomials with ad hoc techniques */
            case 1:
                roots = find_poly_deg1_roots(poly);
                break;
            case 2:
                roots = find_poly_deg2_roots(poly);
                break;
            case 3:
                roots = find_poly_deg3_roots(poly);
                break;
            case 4:
                //                cnt = find_poly_deg4_roots(bch, poly, roots);
                break;
            default:
                /* factor polynomial using Berlekamp Trace Algorithm (BTA) */
                //                cnt = 0;
                //                if (poly->deg && (k <= GF_M(bch)))
                //                {
                //                    gf_poly f1, f2;
                //                    factor_polynomial(bch, k, poly, &f1, &f2);
                //                    if (f1)
                //                        cnt += find_poly_roots(bch, k+1, f1, roots);
                //                    if (f2)
                //                        cnt += find_poly_roots(bch, k+1, f2, roots+cnt);
                //                }
                break;
        }

        return roots;
    }

    /*
     * compute root r of a degree 1 polynomial over GF(2^m) (returned as log(1/r))
     */
    public int[] find_poly_deg1_roots(GFPoly poly)
    {
        int[] roots = new int[1];

        if(poly.mC[0] != 0)
        {
            /* poly[X] = bX+c with c!=0, root=c/b */
            roots[0] = mod_s(mN - a_log_tab[poly.mC[0]] + a_log_tab[poly.mC[1]]);
        }

        return roots;
    }

    /*
     * compute roots of a degree 2 polynomial over GF(2^m)
     */
    public int[] find_poly_deg2_roots(GFPoly poly)
    {
        int[] roots = new int[poly.mDegree];

        int n = 0, i, l0, l1, l2;
        int u, v, r;

        if(poly.mC[0] > 0 && poly.mC[1] > 0)
        {
            l0 = a_log_tab[poly.mC[0]];
            l1 = a_log_tab[poly.mC[1]];
            l2 = a_log_tab[poly.mC[2]];

            /* using z=a/bX, transform aX^2+bX+c into z^2+z+u (u=ac/b^2) */
            u = a_pow(l0 + l2 + 2 * (mN - l1));
            /*
             * let u = sum(li.a^i) i=0..m-1; then compute r = sum(li.xi):
             * r^2+r = sum(li.(xi^2+xi)) = sum(li.(a^i+Tr(a^i).a^k)) =
             * u + sum(li.Tr(a^i).a^k) = u+a^k.Tr(sum(li.a^i)) = u+a^k.Tr(u)
             * i.e. r and r+1 are roots iff Tr(u)=0
             */
            r = 0;
            v = u;
            while(v != 0)
            {
                i = deg(v);
                r ^= xi_tab[i];
                v ^= (1 << i);
            }

            /* verify root */
            if((gf_sqr(r) ^ r) == u)
            {
                /* reverse z=a/bX transformation and compute log(1/r) */
                roots[n++] = modulo(2 * mN - l1 - a_log_tab[r] + l2);
                roots[n++] = modulo(2 * mN - l1 - a_log_tab[r ^ 1] + l2);
            }
        }

        return roots;
    }

    /*
     * compute roots of a degree 3 polynomial over GF(2^m)
     */
    public int[] find_poly_deg3_roots(GFPoly poly)
    {
        int[] roots = new int[poly.mDegree];

        int i, n = 0;
        int a, b, c, a2, b2, c2, e3;
        int[] tmp = new int[4];

        if(poly.mC[0] != 0)
        {
            /* transform polynomial into monic X^3 + a2X^2 + b2X + c2 */
            e3 = poly.mC[3];
            c2 = gf_div(poly.mC[0], e3);
            b2 = gf_div(poly.mC[1], e3);
            a2 = gf_div(poly.mC[2], e3);

            /* (X+a2)(X^3+a2X^2+b2X+c2) = X^4+aX^2+bX+c (affine) */
            c = gf_mul(a2, c2);           /* c = a2c2      */
            b = gf_mul(a2, b2) ^ c2;        /* b = a2b2 + c2 */
            a = gf_sqr(a2) ^ b2;            /* a = a2^2 + b2 */

            /* find the 4 roots of this affine polynomial */
            if(find_affine4_roots(a, b, c, tmp) == 4)
            {
                /* remove a2 from final list of roots */
                for(i = 0; i < 4; i++)
                {
                    if(tmp[i] != a2)
                    {
                        roots[n++] = a_ilog(tmp[i]);
                    }
                }
            }
        }

        return roots;
    }

    /*
     * this function builds and solves a linear system for finding roots of a degree
     * 4 affine monic polynomial X^4+aX^2+bX+c over GF(2^m).
     */
    public int find_affine4_roots(int a, int b, int c, int[] roots)
    {
        int i, j, k;
        int m = mM;
        int mask = 0xff, t;
        int[] rows = new int[16];

        j = a_log(b);
        k = a_log(a);
        rows[0] = c;

        /* buid linear system to solve X^4+aX^2+bX+c = 0 */
        for(i = 0; i < m; i++)
        {
            rows[i + 1] = a_pow_tab[4 * i] ^ (a != 0 ? a_pow_tab[mod_s(k)] : 0) ^ (b != 0 ? a_pow_tab[mod_s(j)] : 0);
            j++;
            k += 2;
        }

        /*
         * transpose 16x16 matrix before passing it to linear solver
         * warning: this code assumes m < 16
         */
        for(j = 8; j != 0; j >>= 1, mask ^= (mask << j))
        {
            for(k = 0; k < 16; k = (k + j + 1) & ~j)
            {
                t = ((rows[k] >> j) ^ rows[k + j]) & mask;
                rows[k] ^= (t << j);
                rows[k + j] ^= t;
            }
        }

        return solve_linear_system(rows, roots, 4);
    }

    /*
     * solve a m x m linear system in GF(2) with an expected number of solutions,
     * and return the number of found solutions
     */
    public int solve_linear_system(int[] rows, int[] sol, int nsol)
    {
        int m = mM;
        int tmp, mask;
        int rem, c, r, p, k;
        int[] param = new int[m];

        k = 0;
        mask = 1 << m;

        /* Gaussian elimination */
        for(c = 0; c < m; c++)
        {
            rem = 0;
            p = c - k;

            /* find suitable row for elimination */
            for(r = p; r < m; r++)
            {
                if((rows[r] & mask) != 0)
                {
                    if(r != p)
                    {
                        tmp = rows[r];
                        rows[r] = rows[p];
                        rows[p] = tmp;
                    }

                    rem = r + 1;
                    break;
                }
            }
            if(rem != 0)
            {
                /* perform elimination on remaining rows */
                tmp = rows[p];
                for(r = rem; r < m; r++)
                {
                    if((rows[r] & mask) != 0)
                    {
                        rows[r] ^= tmp;
                    }
                }
            }
            else
            {
                /* elimination not needed, store defective row index */
                param[k++] = c;
            }
            mask >>= 1;
        }
        /* rewrite system, inserting fake parameter rows */
        if(k > 0)
        {
            p = k;
            for(r = m - 1; r >= 0; r--)
            {
                if((r > m - 1 - k) && rows[r] != 0)
                {
                    /* system has no solution */
                    return 0;
                }

                if(p != 0 && (r == param[p - 1]))
                {
                    p--;
                    rows[r] = 1 << (m - r);
                }
                else
                {
                    rows[r] = rows[r - p];
                }
            }
        }

        if(nsol != (1 << k))
        {
            /* unexpected number of solutions */
            return 0;
        }

        for(p = 0; p < nsol; p++)
        {
            /* set parameters for p-th solution */
            for(c = 0; c < k; c++)
            {
                rows[param[c]] = (rows[param[c]] & ~1) | ((p >> c) & 1);
            }

            /* compute unique solution */
            tmp = 0;
            for(r = m - 1; r >= 0; r--)
            {
                mask = rows[r] & (tmp | 1);
                tmp |= parity(mask) << (m - r);
            }

            sol[p] = tmp >> 1;
        }

        return nsol;
    }

    public static int parity(int x)
    {
        //        /*
        //         * public domain code snippet, lifted from
        //         * http://www-graphics.stanford.edu/~seander/bithacks.html
        //         */
        //        x ^= x >> 1;
        //        x ^= x >> 2;
        //        x = (x & 0x11111111U) * 0x11111111U;
        //        return (x >> 28) & 1;
        return Integer.bitCount(x) % 2;
    }

    /**
     * Calculate the error locator polynomial from the syndromes.
     *
     * @param syn
     * @return
     */
    public GFPoly compute_error_locator_polynomial(int[] syn)
    {
        int i, j, tmp, l, pd = 1, d = syn[0];
        int k, pp = -1;

        GFPoly elp = new GFPoly(2 * mT + 1);
        GFPoly pelp = new GFPoly(2 * mT + 1);
        GFPoly elp_copy = new GFPoly(2 * mT + 1);

        pelp.mDegree = 0;
        pelp.mC[0] = 1;
        elp.mDegree = 0;
        elp.mC[0] = 1;

        /* use simplified binary Berlekamp-Massey algorithm */
        for(i = 0; (i < mT) && (elp.mDegree <= mT); i++)
        {
            if(d != 0)
            {
                k = 2 * i - pp;
                //                gf_poly_copy(elp_copy, elp);
                elp.copyTo(elp_copy); // reimplemented
                /* e[i+1](X) = e[i](X)+di*dp^-1*X^2(i-p)*e[p](X) */
                tmp = a_log(d) + mN - a_log(pd);
                for(j = 0; j <= pelp.mDegree; j++)
                {
                    if(pelp.mC[j] > 0)
                    {
                        l = a_log(pelp.mC[j]);
                        elp.mC[j + k] ^= a_pow(tmp + l);
                    }
                }
                /* compute l[i+1] = max(l[i]->c[l[p]+2*(i-p]) */
                tmp = pelp.mDegree + k;
                if(tmp > elp.mDegree)
                {
                    elp.mDegree = tmp;
                    elp_copy.copyTo(pelp); //reimplemented
                    //                    gf_poly_copy(pelp, elp_copy);
                    pd = d;
                    pp = 2 * i;
                }
            }
            /* di+1 = S(2i+3)+elp[i+1].1*S(2i+2)+...+elp[i+1].lS(2i+3-l) */
            if(i < mT - 1)
            {
                d = syn[2 * i + 2];
                for(j = 1; j <= elp.mDegree; j++)
                {
                    d ^= mul(elp.mC[j], syn[2 * i + 2 - j]);
                }
            }
        }

        System.out.println("ELP: " + Arrays.toString(elp.mC) + " Degree:" + elp.mDegree);

        //        dbg("elp=%s\n", gf_poly_str(elp));
        //        return (elp->deg > t) ? -1 : (int)elp->deg;
        return elp;
    }

    /**
     * Multiplies two integer values using mod N (63) polynomial math.
     *
     * @param a first value
     * @param b second value
     * @return product of (a * b) % N
     */
    private int mul(int a, int b)
    {
        if(a == 0 || b == 0)
        {
            return 0;
        }

        return a_pow_tab[(a_log_tab[a] + a_log_tab[b]) % mN];
    }

    private int gf_sqr(int a)
    {
        return a > 0 ? a_pow_tab[mod_s(2 * a_log_tab[a])] : 0;
    }

    private int gf_mul(int a, int b)
    {
        return (a != 0 && b != 0) ? a_pow_tab[mod_s(a_log_tab[a] + a_log_tab[b])] : 0;
    }

    private int gf_div(int a, int b)
    {
        return a != 0 ? a_pow_tab[mod_s(a_log_tab[a] + mN - a_log_tab[b])] : 0;
    }

    public static class GFPoly
    {
        public int mDegree = 0;
        public int[] mC; //Coefficients

        public GFPoly(int size)
        {
            mC = new int[size];
        }

        public void copyTo(GFPoly copyTo)
        {
            copyTo.mDegree = mDegree;
            copyTo.mC = Arrays.copyOf(mC, mC.length);
        }

        @Override
        public String toString()
        {
            return "Polynomial Degree: " + mDegree + " Coefficients: " + Arrays.toString(mC);
        }
    }

    /**
     * Computes the syndromes for the message.
     * @param message to check
     * @return array of syndromes of size (2 * T)
     */
    public int[] computeSyndromes(CorrectedBinaryMessage message)
    {
        int j;
        int twoT = 2 * mT;
        int[] syndromes = new int[twoT];

        //Calculate for each set bin in message from bit position 0 to N - 1 across 2*T syndromes
        for(int i = message.nextSetBit(0); i >= 0 && i < mN; i = message.nextSetBit(i + 1))
        {
            for(j = 0; j < twoT; j += 2)
            {
                syndromes[j] ^= a_pow((j + 1) * i);
            }
        }

        System.out.println("Syndromes Before Squaring: " + Arrays.toString(syndromes));

        //Calculate the even syndromes as squaring of the odd syndromes: v(a^(2j)) = v(a^j)^2
        for(j = 0; j < mT; j++)
        {
            syndromes[2 * j + 1] = gf_sqr(syndromes[j]);
        }

        return syndromes;
    }

    /*
     * compute 2t syndromes of ecc polynomial, i.e. ecc(a^j) for j=1..2t ... this only works for 32-bit codeword
     */
    public int[] compute_syndromes(int codeword)
    {
        int i, j;
        int poly;
        int[] syn = new int[2 * mT];

        int t = mT;
        int s = mN - mK;  //Should be 31 - 21 = 10, which is also <=m*t or 5 * 2;

        /* compute v(a^j) for j=1 .. 2t-1 */
        do
        {
            poly = codeword;
            s -= 32;

            s = 0;

            while(poly != 0)
            {
                i = deg(poly);

                for(j = 0; j < 2 * t; j += 2)
                {
                    syn[j] ^= a_pow((j + 1) * (i + s));
                }

                poly ^= (1 << i);
            }
        }
        while(s > 0);

        System.out.println("Syndromes Before Squaring: " + Arrays.toString(syn));
        /* v(a^(2j)) = v(a^j)^2 */
        for(j = 0; j < t; j++)
        {
            syn[2 * j + 1] = gf_sqr(syn[j]);
        }

        return syn;
    }

    /**
     * Calculates the degree of the polynomial, represented as the most significant bit index
     *
     * @param poly to inspect
     * @return degree
     */
    public static int deg(int poly)
    {
        /* polynomial degree is the most-significant bit index */
        int highestSetBit = Integer.highestOneBit(poly);
        return Integer.numberOfTrailingZeros(highestSetBit);
        //return fls(poly)-1;  << why minus 1?
    }

    /**
     * Initializes the Galois field lookup tables.
     *
     * Modeled on bch.1 build_gf_tables() method.
     */
    public void initGFTables()
    {
        int i, x = 1;
        int k = 1 << mM;

        a_pow_tab = new int[k];
        a_log_tab = new int[k];

        for(i = 0; i < k - 1; i++)
        {
            a_pow_tab[i] = x;
            a_log_tab[x] = i;
            x <<= 1;
            if((x & k) != 0)
            {
                x ^= mPrimitivePolynomial;
            }
        }
        a_pow_tab[k - 1] = 1;
        a_log_tab[0] = 0;

        System.out.println("a_pow_tab = " + Arrays.toString(a_pow_tab));
        System.out.println("a_log_tab = " + Arrays.toString(a_log_tab));

        buildDegree2Base();
    }

    public int a_log(int value)
    {
        return a_log_tab[value];
    }

    public int a_ilog(int x)
    {
        return mod_s(mN - a_log_tab[x]);
    }


    public int a_pow(int value)
    {
        return a_pow_tab[modulo(value)];
    }

    public int modulo(int v)
    {
        while(v >= mN)
        {
            v -= mN;
            v = (v & mN) + (v >> mM);
        }

        return v;
    }

    public int mod_s(int v)
    {
        return (v < mN ? v : v - mN);
    }

    /**
     * Decodes the BCH protected message.
     * @param message where the BCH protected codeword is located at bit indices 0 to (N-1)
     */
    public void decode(CorrectedBinaryMessage message)
    {
        int[] syndromes = computeSyndromes(message);
        GFPoly elp = compute_error_locator_polynomial(syndromes);
        System.out.println(elp);
        int[] roots = find_poly_roots(elp);
        System.out.println("Error Roots:" + Arrays.toString(roots));

        //TODO: correct the roots.
    }

    public static void main(String[] args)
    {
        BCHDecoder bch = new BCHDecoder();
        //        build_gf_tables();

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

        //***********************************
        int[] errors = {4, 18};
        //***********************************

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
