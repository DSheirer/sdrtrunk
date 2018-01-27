/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.buffer;

import io.github.dsheirer.sample.Listener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CircularBuffer<T> implements Listener<T>
{
    private List<T> mElements;
    private Listener<T> mListener;
    private int mPointer;
    private int mSize;


    /**
     * Circular Buffer - stores elements in an array list.  Once the buffer is
     * full, new samples replace older samples and older samples are broadcast
     * to an optional listener.
     *
     * @param size - size of buffer
     */
    public CircularBuffer(int size)
    {
        mSize = size;
        mElements = new ArrayList<T>();
    }

    /**
     * Returns the element stored at index.  Note: the index argument refers to
     * the internal storage index and does not indicate a temporal relationship
     * among elements.  For example, if you get indexes 1, 2, and 3, you may be
     * getting ( oldest - 1, oldest, newest ) elements, if the internal pointer
     * is currently pointing to and will insert the next element at index 4.
     *
     * @param index - element index
     * @return element or null
     */
    public T get(int index)
    {
        if(index < mElements.size())
        {
            return mElements.get(index);
        }

        return null;
    }

    public int getSize()
    {
        return mSize;
    }

    public List<T> getElements()
    {
        return Collections.unmodifiableList(mElements);
    }

    @Override
    public void receive(T element)
    {
        T previous = null;

        if(mElements.size() > mPointer)
        {
            previous = mElements.get(mPointer);

            mElements.set(mPointer, element);
        }
        else
        {
            mElements.add(element);
        }

        mPointer++;

        if(mPointer >= mSize)
        {
            mPointer = 0;
        }

        if(previous != null && mListener != null)
        {
            mListener.receive(previous);
        }
    }

    public void setListener(Listener<T> listener)
    {
        mListener = listener;
    }
}
