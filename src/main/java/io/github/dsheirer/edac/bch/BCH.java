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

package io.github.dsheirer.edac.bch;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Bose-Chaudhuri-Hocquenghem (BCH) decoder.  Note: does NOT include the encoder from the original implementation.
 *
 * Based on the GPL2 Linux BCH decoder implementation here:
 * https://github.com/Parrot-Developers/bch/blob/master/include/linux/bch.h
 *
 * Note: original implementation method and variable names were largely used/retained in porting to Java.
 */
public abstract class BCH
{
    public static final int MESSAGE_NOT_CORRECTED = -1;
    public static final int PRIMITIVE_POLYNOMIAL_GF_3 = 0x7;
    public static final int PRIMITIVE_POLYNOMIAL_GF_7 = 0xB;
    public static final int PRIMITIVE_POLYNOMIAL_GF_15 = 0x13;
    public static final int PRIMITIVE_POLYNOMIAL_GF_31 = 0x25;
    public static final int PRIMITIVE_POLYNOMIAL_GF_63 = 0x43;
    public static final int PRIMITIVE_POLYNOMIAL_GF_127 = 0x83;
    public static final int PRIMITIVE_POLYNOMIAL_GF_255 = 0x11D;

    /**
     * Primitive polynomials for GF(2) to GF(8) are defined in this class via the PRIMITIVE_POLYNOMIAL_GF_xx constants.
     * This is different from the generator polynomial that is used to create message codewords for the BCH code, where
     * the generator polynomial dictates the error correcting capacity value T for the BCH finite set.  The base
     * primitive polynomial is used to calculate the syndromes for the BCH code regardless of the generator polynomial
     * that is used to generate the codewords and the specified value of T dictates how many syndromes are calculated
     * and therefore how many bit errors can be detected and corrected.
     */
    private int mPrimitivePolynomial;

    /**
     * Galois Field size: GF(2^m)
     */
    private int mM;

    /**
     * Codeword size where: N = 2 ^ M - 1
     */
    private int mN;

    /**
     * Codeword message content size or number of data bits.
     */
    private int mK;

    /**
     * Error detection and correction capacity of the BCH code.  This is the maximum number of bit errors that can
     * be detected and corrected.
     */
    private int mT;

    /**
     * Lookup tables.  Note: naming convention is from the original C++ implementation.
     */
    public int[] a_pow_tab;
    public int[] a_log_tab;
    private int[] xi_tab;

    /**
     * Constructs an instance of a BCH decoder with the following design parameters:
     *
     * @param m Galois Field (2^m) where the BCH code is 2^m-1
     * @param k message data bits
     * @param t maximum correctable errors.  This is a design parameter of the generator polynomial used to form the
     * codewords
     * @param primitivePolynomial for the GF(2).  Note: use one of the PRIMITIVE_POLYNOMIAL_GF_xxx constants defined in
     * this class which cover use cases for M: 2-8.
     */
    public BCH(int m, int k, int t, int primitivePolynomial)
    {
        mM = m;
        mN = (1 << m) - 1;
        mK = k;
        mT = t;
        mPrimitivePolynomial = primitivePolynomial;
        initTables();
    }

    /**
     * Maximum bit error detection and correction capacity (T) of this decoder.
     * @return maximum bit error correction.
     */
    public int getMaxErrorCorrection()
    {
        return mT;
    }

    /*
     * Creates lookup table for finding the roots of a degree 2 error locator polynomial.
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
     * Finds the error roots of the error locator polynomial, using BTZ algorithm
     */
    public int[] find_poly_roots(GFPoly poly, int k)
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
                roots = find_poly_deg4_roots(poly);
                break;
            default:
                /* factor polynomial using Berlekamp Trace Algorithm (BTA) */
                if (poly.mDegree != 0 && (k <= mM))
                {
                    GFPoly[] factors = factor_polynomial(k, poly);

                    if (factors != null && factors.length == 2)
                    {
                        int[] aRoots = find_poly_roots(factors[0], k + 1);
                        int[] bRoots = find_poly_roots(factors[1], k + 1);

                        roots = new int[aRoots.length + bRoots.length];

                        System.arraycopy(aRoots, 0, roots, 0, aRoots.length);
                        System.arraycopy(bRoots, 0, roots, aRoots.length, bRoots.length);
                    }
                    else if (factors != null && factors.length > 0)
                    {
                        roots = find_poly_roots(factors[0], k + 1);
                    }
                }

                break;
        }

        return roots;
    }

    /*
     * Factors a polynomial using Berlekamp Trace algorithm (BTA)
     */
    public GFPoly[] factor_polynomial(int k, GFPoly f)
    {
        GFPoly f2 = new GFPoly(2 * mT);
        GFPoly q = new GFPoly(2 * mT);
        GFPoly z = new GFPoly(2 * mT);
        GFPoly tk;
        GFPoly gcd;

        GFPoly g = new GFPoly(1);
        f.copyTo(g);

        /* tk = Tr(a^k.X) mod f */
        tk = compute_trace_bk_mod(k, f, z);

        if (tk.mDegree > 0)
        {
            f.copyTo(f2);

            /* compute g = gcd(f, tk) (destructive operation) */
            gcd = gf_poly_gcd(f2, tk);

            if (gcd.mDegree < f.mDegree)
            {
                /* compute h=f/gcd(f,tk); this will modify f and q */
                gf_poly_div(f, gcd, q);
                GFPoly[] results = new GFPoly[2];
                results[0] = gcd;
                results[1] = q;
                return results;
            }
        }

        GFPoly[] results = new GFPoly[1];
        results[0] = g;
        return results;
    }

    /*
     * Compute polynomial Euclidean division quotient in GF(2^m)[X]
     */
    public void gf_poly_div(GFPoly a, GFPoly b, GFPoly q)
    {
        if (a.mDegree >= b.mDegree)
        {
            q.mDegree = a.mDegree - b.mDegree;
            /* compute a mod b (modifies a) */
            gf_poly_mod(a, b);
            /* quotient is stored in upper part of polynomial a */
            System.arraycopy(a.mC, b.mDegree, q.mC, 0, 1 + q.mDegree);
        }
        else
        {
            q.mDegree = 0;
            q.mC[0] = 0;
        }
    }


    /*
     * Compute polynomial GCD (Greatest Common Divisor) in GF(2^m)[X]
     */
    public GFPoly gf_poly_gcd(GFPoly a, GFPoly b)
    {
        GFPoly tmp;

        if (a.mDegree < b.mDegree)
        {
            tmp = b;
            b = a;
            a = tmp;
        }

        while (b.mDegree > 0)
        {
            gf_poly_mod(a, b);
            tmp = b;
            b = a;
            a = tmp;
        }

        return a;
    }


    /*
     * Given a polynomial f and an integer k, compute Tr(a^kX) mod f
     * This is used in Berlekamp Trace algorithm for splitting polynomials
     */
    public GFPoly compute_trace_bk_mod(int k, GFPoly f, GFPoly z)
    {
        int m = mM;
        int i, j;

        /* z contains z^2j mod f */
        z.mDegree = 1;
        z.mC[0] = 0;
        z.mC[1] = a_pow_tab[k];

        GFPoly outTK = new GFPoly(f.mC.length);
        outTK.mDegree = 0;

        for (i = 0; i < m; i++)
        {
            /* add a^(k*2^i)(z^(2^i) mod f) and compute (z^(2^i) mod f)^2 */
            for (j = z.mDegree; j >= 0; j--)
            {
                outTK.mC[j] ^= z.mC[j];
                z.mC[2 * j] = gf_sqr(z.mC[j]);
                z.mC[2 * j + 1] = 0;
            }

            if (z.mDegree > outTK.mDegree)
            {
                outTK.mDegree = z.mDegree;
            }

            if (i < m-1)
            {
                z.mDegree *= 2;
                /* z^(2(i+1)) mod f = (z^(2^i) mod f)^2 mod f */
                gf_poly_mod(z, f);
            }
        }

        while (outTK.mC[outTK.mDegree] == 0 && outTK.mDegree != 0)
        {
            outTK.mDegree--;
        }

        return outTK;
    }

    /*
     * compute polynomial Euclidean division remainder in GF(2^m)[X]
     */
    public void gf_poly_mod(GFPoly a, GFPoly b)
    {
        if (a.mDegree < b.mDegree)
        {
            return;
        }

        int la, p, m;
        int i, j;
        int[] c = Arrays.copyOf(a.mC, a.mC.length);
        int d = b.mDegree;

        int[] rep = gf_poly_logrep(b);

        for (j = a.mDegree; j >= d; j--)
        {
            if (c[j] != 0)
            {
                la = a_log(c[j]);
                p = j-d;

                for (i = 0; i < d; i++, p++)
                {
                    m = rep[i];

                    if (m >= 0)
                    {
                        c[p] ^= a_pow_tab[mod_s(m + la)];
                    }
                }
            }
        }

        a.mDegree = d - 1;

        while (c[a.mDegree] == 0 && a.mDegree != 0)
        {
            a.mDegree--;
        }

        //Reassign c back to a's polynomial
        a.mC = c;
    }


    /*
     * Build monic, log-based representation of a polynomial
     */
    public int[] gf_poly_logrep(GFPoly a)
    {
        int i;
        int d = a.mDegree;
        int l = mN - a_log(a.mC[a.mDegree]);

        int[] rep = new int[d];

        /* represent 0 values with -1; warning, rep[d] is not set to 1 */
        for (i = 0; i < d; i++)
        {
            rep[i] = a.mC[i] != 0 ? mod_s(a_log(a.mC[i]) + l) : -1;
        }

        return rep;
    }

    /*
     * Compute root r of a degree 1 polynomial over GF(2^m) (returned as log(1/r))
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
     * Compute roots of a degree 2 polynomial over GF(2^m)
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
     * Compute roots of a degree 3 polynomial over GF(2^m)
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
     * Compute roots of a degree 4 polynomial over GF(2^m)
     */
    public int[] find_poly_deg4_roots(GFPoly poly)
    {
        int[] roots = new int[poly.mDegree];

        int i, l, n = 0;
        int a, b, c, d, e = 0, f, a2, b2, c2, e4;

        if (poly.mC[0] == 0)
        {
            return new int[0];
        }

        /* transform polynomial into monic X^4 + aX^3 + bX^2 + cX + d */
        e4 = poly.mC[4];
        d = gf_div(poly.mC[0], e4);
        c = gf_div(poly.mC[1], e4);
        b = gf_div(poly.mC[2], e4);
        a = gf_div(poly.mC[3], e4);

        /* use Y=1/X transformation to get an affine polynomial */
        if (a != 0)
        {
            /* first, eliminate cX by using z=X+e with ae^2+c=0 */
            if (c != 0)
            {
                /* compute e such that e^2 = c/a */
                f = gf_div(c, a);
                l = a_log(f);
                l += ((l & 1) != 0) ? mN : 0;
                e = a_pow(l / 2);

                /*
                 * use transformation z=X+e:
                 * z^4+e^4 + a(z^3+ez^2+e^2z+e^3) + b(z^2+e^2) +cz+ce+d
                 * z^4 + az^3 + (ae+b)z^2 + (ae^2+c)z+e^4+be^2+ae^3+ce+d
                 * z^4 + az^3 + (ae+b)z^2 + e^4+be^2+d
                 * z^4 + az^3 +     b'z^2 + d'
                 */
                d = a_pow(2 * l) ^ gf_mul(b, f) ^ d;
                b = gf_mul(a, e) ^ b;
            }

            /* now, use Y=1/X to get Y^4 + b/dY^2 + a/dY + 1/d */
            if (d == 0)
            {
                /* assume all roots have multiplicity 1 */
                return new int[0];
            }

            c2 = gf_inv(d);
            b2 = gf_div(a, d);
            a2 = gf_div(b, d);
        } else {
            /* polynomial is already affine */
            c2 = d;
            b2 = c;
            a2 = b;
        }
        /* find the 4 roots of this affine polynomial */
        if (find_affine4_roots(a2, b2, c2, roots) == 4)
        {
            for (i = 0; i < 4; i++)
            {
                /* post-process roots (reverse transformations) */
                f = a != 0 ? gf_inv(roots[i]) : roots[i];
                roots[i] = a_ilog(f ^ e);
            }
        }

        return roots;
    }

    /*
     * This function builds and solves a linear system for finding roots of a degree
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
     * Solve an m x m linear system in GF(2) with an expected number of solutions,
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

    /**
     * Calculates the parity of an integer as the number of set bits mod 2.
     * @param x to calculate
     * @return parity of x
     */
    public static int parity(int x)
    {
        return Integer.bitCount(x) % 2;
    }

    /**
     * Calculate the error locator polynomial from the syndromes.
     *
     * @param syn syndromes
     * @return error locator polynomial (elp)
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

        return elp;
    }

    /**
     * Multiplies two integer values using mod N polynomial math.
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

    private int gf_inv(int a)
    {
        return a_pow_tab[mN - a_log_tab[a]];
    }

    /**
     * Polynomial representation.
     */
    public static class GFPoly
    {
        public int mDegree = 0;
        public int[] mC; //Coefficients

        /**
         * Constructs an instance with the specified size, or number of coefficients.
         * @param size of the coefficients array
         */
        public GFPoly(int size)
        {
            mC = new int[size];
        }

        /**
         * Copies the degree and coefficients of this polynomial onto the argument polynomial.
         * @param copyTo target of the copy operation.
         */
        public void copyTo(GFPoly copyTo)
        {
            copyTo.mDegree = mDegree;
            copyTo.mC = Arrays.copyOf(mC, mC.length);
        }

        /**
         * Pretty print of the contents of this polynomial.
         */
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
        int nMinus1 = mN - 1;
        int[] syndromes = new int[twoT];

        //Use the binary message feature to iterate across the set bit positions to calculate the syndromes for each
        //set bit.  We iterate the bit positions in order from MSB to LSB, however we calculate the syndrome as if each
        //set bit is accessed in reverse order by subtracting the iterated bit position from (N-1).
        for(int i = message.nextSetBit(0); i >= 0 && i < mN; i = message.nextSetBit(i + 1))
        {
            for(j = 0; j < twoT; j += 2)
            {
                syndromes[j] ^= a_pow((j + 1) * (nMinus1 - i));
            }
        }

        //Calculate the even syndromes as squaring of the odd syndromes: v(a^(2j)) = v(a^j)^2
        for(j = 0; j < mT; j++)
        {
            syndromes[2 * j + 1] = gf_sqr(syndromes[j]);
        }

        return syndromes;
    }

    /*
     * compute 2t syndromes of ecc polynomial, i.e. ecc(a^j) for j=1..2t ... this only works for 32-bit codeword
     *
     * Note: this is ported from the original implementation that used an array of one or more 32-bit integers to
     * represent the codeword and was used during development and testing of the ported code.  This method only works
     * with a single 32-bit codeword formed for a BCH(31) finite set and does not work for larger BCH codes, so don't
     * use this method.
     */
    private int[] compute_syndromes(int codeword)
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

        //        System.out.println("Syndromes Before Squaring: " + Arrays.toString(syn));
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
    public void initTables()
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

        //        System.out.println("a_pow_tab = " + Arrays.toString(a_pow_tab));
        //        System.out.println("a_log_tab = " + Arrays.toString(a_log_tab));

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
        int elpDegree = elp.mDegree;
        //        System.out.println(elp);
        int k = 1; //Recursive call argument
        int[] roots = find_poly_roots(elp, k);
        //        System.out.println(elp);

        if(roots.length != elpDegree)
        {
            //            System.out.println("Error roots " + Arrays.toString(roots) + " do not match ELP degree [" + elpDegree + "]");
            message.setCorrectedBitCount(MESSAGE_NOT_CORRECTED);
            return;
        }

        //Create a set from the roots to eliminate and then detect if there are duplicates
        Set<Integer> rootSet = new HashSet<>(Arrays.asList(ArrayUtils.toObject(roots)));

        if(rootSet.size() != roots.length)
        {
            //            System.out.println("Error roots were not distinct: " + Arrays.toString(roots));
            message.setCorrectedBitCount(MESSAGE_NOT_CORRECTED);
            return;
        }

        //Invert the error roots because we process the message backwards/inverted, so the calculated roots then also
        //need to be un-inverted to reference the correct message indices.
        for(int x = 0; x < roots.length; x++)
        {
            roots[x] = mN - 1 - roots[x];
        }

        //        System.out.println("Error Roots:" + Arrays.toString(roots));

        //Correct the errors in the original message.
        for(int error: roots)
        {
            message.flip(error);
        }

        //Set the number of errors corrected on the original message.
        message.setCorrectedBitCount(roots.length);
    }
}