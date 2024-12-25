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

public class IntArrayArray extends Array<Array<Integer>>
{
    private Array<Integer>[] mData;

    public IntArrayArray(int size)
    {
        super(size);
    }

    @Override
    public Array<Integer> get(int index)
    {
        if(index < mData.length)
        {
            return mData[index];
        }

        throw new IllegalArgumentException("Index [" + index + "] out of bounds for size [" + size() + "]");
    }

    @Override
    public void set(int index, Array<Integer> value)
    {
        if(index < mData.length)
        {
            mData[index] = value;
        }

        throw new IllegalArgumentException("Index [" + index + "] out of bounds for size [" + size() + "]");
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
                    mData[i] = new IntArray(0);
                }
            }
        }
        else
        {
            allocate(size);
        }
    }

    @Override
    public int size()
    {
        return mData.length;
    }

    @Override
    protected void allocate(int size)
    {
        mData = new IntArray[size];

        for(int i = 0; i < mData.length; i++)
        {
            mData[i] = new IntArray(0);
        }
    }
}
