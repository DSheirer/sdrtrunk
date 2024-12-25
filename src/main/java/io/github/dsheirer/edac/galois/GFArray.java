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
import org.apache.commons.lang3.Validate;

public class GFArray extends Array<GF>
{
    private GF[] mData;

    public GFArray(int size)
    {
        super(size);
    }

    @Override
    public int size()
    {
        return mData.length;
    }

    @Override
    public GF get(int index)
    {
        if(index < mData.length)
        {
            return mData[index];
        }
        else
        {
            throw new IllegalArgumentException("Index [" + index + "] out of bounds for length [" + mData.length + "]");
        }
    }

    @Override
    public void set(int index, GF value)
    {
        if(index < mData.length)
        {
            mData[index] = value;
        }
        else
        {
            throw new IllegalArgumentException("Index [" + index + "] out of bounds for length [" + mData.length + "]");
        }
    }

    @Override
    protected void allocate(int size)
    {
        mData = new GF[size];

        for(int x = 0; x < size; x++)
        {
            mData[x] = new GF();
        }
    }

    @Override
    public void set_size(int size, boolean copy)
    {
        Validate.isTrue(size >= 0, "size must be greater than or equal to zero");

        if(size() == size)
        {
            return;
        }

        if(copy)
        {
            mData = Arrays.copyOf(mData, size);

            if(size > size())
            {
                for(int i = size(); i < size; i++)
                {
                    mData[i] = new GF();
                }
            }
        }
        else
        {
            allocate(size);
        }
    }
}
