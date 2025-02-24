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

import org.apache.commons.lang3.Validate;

/**
 * Galois Field implementation.
 */
public class GF
{
    private static final int[] q = new int[]{1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536};
    private static final int[] reducetable = new int[]{3, 3, 3, 5, 3, 9, 29, 17, 9, 5, 83, 27, 43, 3, 4107};
    private int mM;
    private int mQ;
    private int mValue;
    private static final IntArrayArray alphapow = new IntArrayArray(0);
    private static final IntArrayArray logalpha = new IntArrayArray(0);

    /**
     * Private copy/clone constructor creates a new instance with the provided values.
     */
    private GF(int m, int q, int value)
    {
        mM = m;
        mQ = q;
        mValue = value;
    }

    public GF(int q, int value)
    {
        set(q, value);
    }

    public GF(int q)
    {
        if(q == 0)
        {
            mValue = -1; // qvalue==0 gives the zeroth element
        }
        else
        {
            set_size(q);
        }
    }

    public void set_size(int q)
    {
        mQ = q;
        mM = (int) Math.round(Math.log(q) / Math.log(2));
        Validate.isTrue((1 << mM) == q, "q [" + mQ + "] must be a power of 2 and m[" + mM + "] where (2 ^ m) == q");
        Validate.isTrue(mM > 0 && mM <= 16, "q must be positive and less than or equal to 2^16");

        if(alphapow.size() < (mM + 1))
        {
            alphapow.set_size(mM + 1, true);
            logalpha.set_size(mM + 1, true);

            if(alphapow.get(mM).size() == 0)
            {
                IntArray ap = alphapow.get(mM);
                IntArray la = logalpha.get(mM);
                ap.set_size(q);
                la.set_size(q);

                if(mM == 1) //GF(2), special case
                {
                    ap.set(0, 1);
                    la.set(0, -1);
                    la.set(1, 0);
                }
                else
                {
                    int temp, n;
                    int reduce = reducetable[mM - 2];
                    ap.set(0, 1);
                    int qMask = 1 << mM;
                    for(n = 1; n < qMask - 1; n++)
                    {
                        temp = ap.get(n - 1);
                        temp = (temp << 1);
                        if((temp & qMask) > 0)
                        {
                            ap.set(n, (temp & ~qMask) ^ reduce);
                        }
                        else
                        {
                            ap.set(n, temp); //if no alpha**m term, store as is
                        }
                    }

                    la.set(0, -1); //special case, actually log(0)=-inf

                    for(n = 0; n < qMask - 1; n++)
                    {
                        la.set(ap.get(n), n);
                    }
                }
            }
        }
    }

    public int get_size()
    {
        return mQ;
    }

    public void set(int q, int value)
    {
        set_size(q);
        mValue = value;
    }

    public int get_value()
    {
        return mValue;
    }

    /**
     * Performs add operation and returns a new GF
     * @param gf to add with current GF to produce a new GF
     * @return new gf
     */
    public GF add(GF gf)
    {
        GF temp = copyOf();
        temp.addEquals(gf);
        return temp;
    }

    /**
     *inline void GF::operator+=(const GF &ingf)
     * {
     *   if (value == -1) {
     *     value = ingf.value;
     *     m = ingf.m;
     *   }
     *   else if (ingf.value != -1) {
     *     it_assert_debug(ingf.m == m, "GF::op+=, not same field");
     *     value = logalpha(m)(alphapow(m)(value) ^ alphapow(m)(ingf.value));
     *   }
     * }
     *
     * Adds the gf argument to this GF.
     * @param ingf to add.
     * @return this.
     */
    public void addEquals(GF ingf)
    {
        if(mValue == -1)
        {
            mValue = ingf.mValue;
            mM = ingf.mM;
        }
        else if(ingf.mValue != -1)
        {
            Validate.isTrue(mM == ingf.mM, "Not the same field");

            //TODO: remove debug ..
            int a = alphapow.get(mM).get(mValue);
            int b = alphapow.get(mM).get(ingf.mValue);
            int x = a ^ b;
            int la = logalpha.get(mM).get(x);


            mValue = logalpha.get(mM).get(alphapow.get(mM).get(mValue) ^ alphapow.get(mM).get(ingf.mValue));
        }
    }

    /**
     * Performs multiply operation and returns a new GF
     * @param gf to multiply with current GF to produce a new GF
     * @return new gf
     */
    public GF multiply(GF gf)
    {
        GF temp = copyOf();
        temp.multiplyEquals(gf);
        return temp;
    }

    public void multiplyEquals(GF ingf)
    {
        if(mValue == -1 || ingf.get_value() == -1)
        {
            mValue = -1;
        }
        else
        {
            mValue = (mValue + ingf.mValue) % (q[mM] - 1);
        }
    }

    public GFX multiply(GFX ingfx)
    {
        Validate.isTrue(get_size() == ingfx.getSize(), "Not the same field");
        GFX temp = ingfx.copyOf();

        for(int i = 0; i < ingfx.getDegree() + 1; i++)
        {
            GF coefficient = temp.get(i);
            coefficient.multiplyEquals(this);
            temp.set(i, coefficient);
        }

        return temp;
    }

    public GF divide(GF gf)
    {
        GF temp = copyOf();
        temp.divideEquals(gf);
        return temp;
    }

    public void divideEquals(GF ingf)
    {
        Validate.isTrue(ingf.mValue != -1, "Division by zero element"); //No division by the zeroth element

        if(mValue != -1)
        {
            Validate.isTrue(mM == ingf.mM, "Not the same field");
            mValue = (mValue - ingf.mValue + q[mM] - 1) % (q[mM] - 1);
        }
    }

    /**
     * Tests for equality
     * @param ingf to test against this GF
     * @return
     */
    public boolean eq(GF ingf)
    {
        if(mValue == -1 || ingf.mValue == -1)
        {
            return true;
        }

        if(mM == ingf.mM && mValue == ingf.mValue)
        {
            return true;
        }

        return false;
    }

    public GF copyOf()
    {
        return new GF(mM, mQ, mValue);
    }


    @Override
    public String toString()
    {
        return "GF m:" + mM + " q:" + mQ + " value:" + mValue;
    }

}
