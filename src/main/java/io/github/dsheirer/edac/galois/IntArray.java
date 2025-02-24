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
 * Array of integers.
 */
public class IntArray extends Array<Integer>
{
    private int[] mData;

    /**
     * Constructs an instance of the specified size with data defaulting to zeroes.
     * @param size of array
     */
    public IntArray(int size)
    {
        super(size);
    }

    public IntArray copyOf()
    {
        IntArray clone = new IntArray(mData.length);
        clone.mData = Arrays.copyOf(mData, mData.length);
        return clone;
    }

    @Override
    public int size()
    {
        return mData.length;
    }

    @Override
    public Integer get(int index)
    {
        if(index < mData.length)
        {
            return mData[index];
        }

        throw new IllegalArgumentException("Index [" + index + "] out of bounds for size [" + size() + "]");
    }

    @Override
    public void set(int index, Integer value)
    {
        if(index < mData.length)
        {
            mData[index] = value;
        }
        else
        {
            throw new IllegalArgumentException("Index [" + index + "] out of bounds for size [" + size() + "]");
        }

    }

    @Override
    protected void allocate(int size)
    {
        mData = new int[size];
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
        }
        else
        {
            allocate(size);
        }
    }

    @Override
    public String toString()
    {
        return Arrays.toString(mData);
    }
}
