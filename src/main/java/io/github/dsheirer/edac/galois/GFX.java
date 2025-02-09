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

public class GFX
{
    private int mDegree = -1;
    private int mQ = 0;
    private GFArray mCoefficients;

    public GFX(int q, int[] values)
    {
        set(q, values);
    }

    public GFX(int q, int degree)
    {
        this(q);
        Validate.isTrue(degree >= 0, "degree must be greater than or equal to 0");
        mDegree = degree;
        mCoefficients.set_size(degree + 1);

        for(int i = 0; i < degree + 1; i++)
        {
            mCoefficients.get(i).set(q, -1);
        }
    }

    public GFX(int q)
    {
        Validate.isTrue(q >= 0, "Q must be greater than or equal to 0");
        mQ = q;
        mCoefficients = new GFArray(0, mQ);
    }

    public GFArray getCoefficients()
    {
        GFArray coefficients = new GFArray(mDegree + 1, mQ);

        for(int i = 0; i < mDegree + 1; i++)
        {
            coefficients.set(i, mCoefficients.get(i).copyOf());
        }

        return coefficients;
    }

    public int getDegree()
    {
        return mDegree;
    }

    public void setDegree(int degree)
    {
        Validate.isTrue(degree >= -1, "degree must be greater than or equal to -1");
        mDegree = degree;
        mCoefficients.set_size(degree + 1, true);
    }

    public int get_true_degree()
    {
        int i = mDegree;

        while(i > -1 && mCoefficients.get(i).get_value() == -1)
        {
            i--;
        }

        return i;
    }

    public int getSize()
    {
        return mQ;
    }

    public void clear()
    {
        for(int i = 0; i < mCoefficients.size(); i++)
        {
            mCoefficients.get(i).set(mQ, -1);
        }
    }

    public GFX copyOf()
    {
        GFX gfx = new GFX(mQ);
        gfx.mDegree = mDegree;
        gfx.mCoefficients = getCoefficients();
        return gfx;
    }

    public void set(int q, int[] values)
    {
        Validate.isTrue(q >= 0, "Q must be greater than or equal to 0");
        mQ = q;

        if(mCoefficients == null)
        {
            mCoefficients = new GFArray(0, mQ);
        }

        mDegree = values.length - 1;
        mCoefficients.set_size(values.length);

        for(int i = 0; i < values.length; i++)
        {
            mCoefficients.get(i).set(q, values[i]);
        }
    }

    public GF evaluate(GF ingf)
    {
        Validate.isTrue(getSize() == ingf.get_size(), "not the same field");

        GF temp = mCoefficients.get(0).copyOf();
        GF ingfpower = ingf.copyOf();

        for(int i = 1; i < mDegree + 1; i++)
        {
            GF coefficient = mCoefficients.get(i);
            GF multiplied = coefficient.multiply(ingfpower);
            temp.addEquals(multiplied);
            ingfpower.multiplyEquals(ingf);
        }

//        GF clearingOperation = new GF(mQ, mQ - 1 - ingf.get_value());
//        temp.addEquals(clearingOperation);

        return temp;
    }

    /**
     * Gets the coefficient at the specified index
     * @param index of the coefficient
     * @return GF coefficient
     */
    public GF get(int index)
    {
        return mCoefficients.get(index);
    }

    /**
     * Sets the coefficient at the specified index to the argument.
     * @param index of the coefficient
     * @param coefficient to assign or set.
     */
    public void set(int index, GF coefficient)
    {
        mCoefficients.set(index, coefficient);
    }

    /**
     * Assigns or transfers values from argument.  Implements the (=) operator.
     * @param gfx to assign from.
     */
    public void assignFrom(GFX gfx)
    {
        mDegree = gfx.mDegree;
        mQ = gfx.mQ;
        mCoefficients = gfx.mCoefficients;
    }

    public void addEquals(GFX gfx)
    {
        Validate.isTrue(gfx.mQ == mQ, "Size mismatch - not the same field");

        if(gfx.getDegree() > mDegree)
        {
            for(int j = mDegree + 1; j < mCoefficients.size(); j++)
            {
                GF gf = new GF(mQ, -1);
                mCoefficients.set(j, gf);
            }

            setDegree(gfx.mDegree);
        }

        for(int i = 0; i < gfx.mDegree + 1; i++)
        {
            mCoefficients.get(i).addEquals(gfx.mCoefficients.get(i));
        }
    }

    public GFX add(GFX gfx)
    {
        GFX temp = copyOf();
        temp.addEquals(gfx);
        return temp;
    }

    public void subtractEquals(GFX gfx)
    {
        //Subtraction is same as addition
        addEquals(gfx);
    }

    public GFX subtract(GFX gfx)
    {
        GFX temp = copyOf();
        temp.subtractEquals(gfx);
        return temp;
    }

    public void multiplyEquals(GFX ingfx)
    {
        Validate.isTrue(mQ == ingfx.mQ, "Size mismatch - not the same field");
        int i, j;

        Array<GF> tempcoeffs = getCoefficients();
        mCoefficients.set_size(mDegree + ingfx.mDegree + 1, false);
        for(j = 0; j < mCoefficients.size(); j++)
        {
            mCoefficients.get(j).set(mQ, -1); //set coefficients to the zeroth element (log(0)=-Inf=-1)
        }

        for(i = 0; i < mDegree + 1; i++)
        {
            for(j = 0; j < ingfx.mDegree + 1; j++)
            {
                GF gf = mCoefficients.get(i + j);
                GF gfJ = ingfx.mCoefficients.get(j);
                GF tempI = tempcoeffs.get(i);
                GF product = tempI.multiply(gfJ);
                gf.addEquals(product);
            }
        }

        mDegree = mCoefficients.size() - 1;
    }

    /**
     *
     * @param gfx
     * @return
     */
    public GFX multiply(GFX gfx)
    {
        GFX temp = copyOf();
        temp.multiplyEquals(gfx);
        return temp;
    }

    /**
     * inline GFX  operator/(const GFX &ingfx, const GF &ingf)
     * {
     *   it_assert_debug(ingf.get_size() == ingfx.q, "GFX::op/, Not same field");
     *   GFX temp(ingfx);
     *   for (int i = 0;i < ingfx.degree + 1;i++)
     *     temp.coeffs(i) /= ingf;
     *   return temp;
     * }
     * @param ingf
     */
    public void divideEquals(GF ingf)
    {
        for(int i = 0; i < mDegree; i++)
        {
            GF gf = get(i);
            gf.divide(ingf);
            set(i, gf);
        }
    }

    public GFX divide(GF gf)
    {
        GFX temp = copyOf();
        temp.divideEquals(gf);
        return temp;
    }

    /**
     * Static method to divide two GFX instances.
     * @param c numerator
     * @param g denominator
     * @return result.
     */
    public static GFX divide(GFX c, GFX g)
    {
        int q = c.getSize();
        GFX temp = c.copyOf();
        int tempdegree = temp.get_true_degree();
        int gdegree = g.get_true_degree();
        int degreedif = tempdegree = gdegree;
        if(degreedif < 0)
        {
            return new GFX(q, 0); //denominator larger than nominator.  Return zero polynomial.
        }

        GFX m = new GFX(q, degreedif);
        GFX divisor = new GFX(q);

        for(int i = 0; i < c.get_true_degree(); i++)
        {
            m.set(degreedif, temp.get(tempdegree).divide(g.get(gdegree)));
            divisor.setDegree(degreedif);
            divisor.clear();
            divisor.set(degreedif, m.get(degreedif));
            temp.subtractEquals(divisor.multiply(g));
            tempdegree = temp.get_true_degree();
            degreedif = tempdegree - gdegree;
            if((degreedif < 0) || (temp.get_true_degree() == 0 && temp.get(0).eq(new GF(q, -1))))
            {
                break;
            }
        }

        return m;
    }
}
