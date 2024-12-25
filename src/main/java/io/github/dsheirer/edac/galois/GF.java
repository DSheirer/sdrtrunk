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
    private Array<Array<Integer>> alphapow = new IntArrayArray(0);
    private Array<Array<Integer>> logalpha = new IntArrayArray(0);

    public GF()
    {
    }

    public GF(int qvalue)
    {
        if(qvalue == 0)
        {
            mValue = -1; // qvalue==0 gives the zeroth element
        }
        else
        {
            set_size(qvalue);
        }
    }

    public GF(int qvalue, int inexp)
    {
        set(qvalue, inexp);
    }

    public void set_size(int qValue)
    {
        mQ = qValue;
        int m = (int) Math.round(Math.log(qValue));
        Validate.isTrue((1 << m) == qValue, "q must be a power of 2 where (2 ^ m) == q");
        Validate.isTrue(m > 0 && m <= 16, "q must be positive and less than or equal to 2^16");

        int reduce, temp, n;

        if(alphapow.size() < (m + 1))
        {
            alphapow.set_size(m + 1, true);
            logalpha.set_size(m + 1, true);
        }

        if(alphapow.get(m).size() == 0)
        {
            alphapow.get(m).set_size(qValue);
            logalpha.get(m).set_size(qValue);
            alphapow.get(m).set(0);
            logalpha.get(m).set(0);

            if(m == 1) //GF(2), special case
            {
                alphapow.get(1).set(0, 1);
                logalpha.get(1).set(0, -1);
                logalpha.get(1).set(1, 0);
            }
        }
        else
        {
            reduce = reducetable[m - 2];
            alphapow.get(m).set(0, 1);
            for(n = 1; n < (1 << m) - 1; n++)
            {
                temp = alphapow.get(m).get(n - 1);
                temp = (temp << 1);
                if((temp & (1 << m)) > 0)
                {
                    alphapow.get(m).set(n, (temp & ~(1 << m)) ^ reduce);
                }
                else
                {
                    alphapow.get(m).set(n, temp); //if no alpha**m termp, store as is
                }

                logalpha.get(m).set(0, -1); //special case, actually log(0)=-inf
            }

            for(n = 0; n < (1 << m) - 1; n++)
            {
                logalpha.get(m).set(alphapow.get(m).get(n), n);
            }
        }
    }

    public int size()
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
     * @param gf to add.
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

    /**
     * Multiplies this GF with the gf artument.
     *
     *
     * inline void GF::operator*=(const GF &ingf)
     * {
     *   if (value == -1 || ingf.value == -1)
     *     value = -1;
     *   else {
     *     it_assert_debug(ingf.m == m, "GF::op+=, not same field");
     *     value = (value + ingf.value) % (q[m] - 1);
     *   }
     * }
     *
     * @param gf
     */
    public void multiplyEquals(GF ingf)
    {
        if(mValue == -1 || ingf.get_value() == -1)
        {
            mValue = -1;
        }
        else
        {
            Validate.isTrue(mM == ingf.mM, "Not the same field");
            mValue = (mValue + ingf.mValue) % (q[mM] - 1);
        }
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
            mValue = (mValue - ingf.get_value() + q[mM] - 1) % (q[mM] - 1);
        }
    }

    /**
     * Tests for equality
     * @param ingf to test against this GF
     * @return
     */
    public boolean eq(GF ingf)
    {
        if(mValue == -1 || ingf.get_value() == -1)
        {
            return true;
        }

        if(mM == ingf.mM && mValue == ingf.get_value())
        {
            return true;
        }

        return false;
    }

    public GF copyOf()
    {
        return new GF(mQ, mValue);
    }
}
