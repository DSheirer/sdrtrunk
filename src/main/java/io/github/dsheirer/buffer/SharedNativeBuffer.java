/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.buffer;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Native buffer wrapper that lets multiple channels share one conversion of the native samples.  Each channel's
 * iterator returns the same complex samples instances, on the channel's own thread, so consumers must not modify the
 * samples.  The converted samples are released when the channels release this wrapper.
 */
public class SharedNativeBuffer implements INativeBuffer
{
    private final INativeBuffer mNativeBuffer;
    private final List<ComplexSamples> mConverted = new ArrayList<>();
    private Iterator<ComplexSamples> mConverter;

    /**
     * Constructs an instance
     * @param nativeBuffer to convert
     */
    public SharedNativeBuffer(INativeBuffer nativeBuffer)
    {
        mNativeBuffer = nativeBuffer;
    }

    /**
     * Indicates if the converted samples include the index, converting the next samples as needed.
     */
    private synchronized boolean hasConverted(int index)
    {
        if(mConverter == null)
        {
            mConverter = mNativeBuffer.iterator();
        }

        if(index == mConverted.size() && mConverter.hasNext())
        {
            mConverted.add(mConverter.next());
        }

        return index < mConverted.size();
    }

    private synchronized ComplexSamples getConverted(int index)
    {
        return mConverted.get(index);
    }

    @Override
    public Iterator<ComplexSamples> iterator()
    {
        return new Iterator<>()
        {
            private int mIndex = 0;

            @Override
            public boolean hasNext()
            {
                return hasConverted(mIndex);
            }

            @Override
            public ComplexSamples next()
            {
                if(!hasNext())
                {
                    throw new NoSuchElementException("No more samples");
                }

                return getConverted(mIndex++);
            }
        };
    }

    @Override
    public Iterator<InterleavedComplexSamples> iteratorInterleaved()
    {
        return mNativeBuffer.iteratorInterleaved();
    }

    @Override
    public int sampleCount()
    {
        return mNativeBuffer.sampleCount();
    }

    @Override
    public long getTimestamp()
    {
        return mNativeBuffer.getTimestamp();
    }
}
