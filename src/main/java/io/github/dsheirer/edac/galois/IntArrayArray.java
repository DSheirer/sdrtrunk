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

/**
 * Array of an array of integers.
 */
public class IntArrayArray extends Array<IntArray>
{
    private IntArray[] mData;

    /**
     * Constructs an instance of the specified size with all members instantiated to zero length arrays.
     * @param size of array.
     */
    public IntArrayArray(int size)
    {
        super(size);
    }

    public IntArrayArray copyOf()
    {
        IntArrayArray copy = new IntArrayArray(mData.length);
        for(int i = 0; i < mData.length; i++)
        {
            copy.set(get(i).copyOf());
        }

        return copy;
    }

    @Override
    public IntArray get(int index)
    {
        if(index < mData.length)
        {
            return mData[index];
        }

        throw new IllegalArgumentException("Index [" + index + "] out of bounds for size [" + size() + "]");
    }

    @Override
    public void set(int index, IntArray value)
    {
        if(index < mData.length)
        {
            mData[index] = value;
        }
        else
        {
            throw new IllegalArgumentException("Index [" + index + "] out of bounds for size [" + size() + "] and data length [" + mData.length + "]");
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

        if(copy && size() > 0)
        {
            int currentSize = size();

            mData = Arrays.copyOf(mData, size);

            if(currentSize < size())
            {
                for(int i = currentSize; i < size(); i++)
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

    @Override
    public String toString()
    {
        return Arrays.toString(mData);
    }
}
