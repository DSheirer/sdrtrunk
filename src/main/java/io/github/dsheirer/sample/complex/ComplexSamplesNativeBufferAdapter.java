/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

package io.github.dsheirer.sample.complex;

import io.github.dsheirer.buffer.INativeBuffer;
import java.util.Iterator;

/**
 * Adapts a single complex samples buffer to be compatible as an INativeBuffer instance.
 */
public class ComplexSamplesNativeBufferAdapter implements INativeBuffer
{
    private ComplexSamples mComplexSamples;

    /**
     * Constructs an instance
     * @param complexSamples to adapt/convert to an INativeBuffer
     */
    public ComplexSamplesNativeBufferAdapter(ComplexSamples complexSamples)
    {
        mComplexSamples = complexSamples;
    }

    @Override
    public Iterator<ComplexSamples> iterator()
    {
        return new ComplexSamplesIterator();
    }

    @Override
    public Iterator<InterleavedComplexSamples> iteratorInterleaved()
    {
        return new InterleavedComplexSamplesIterator();
    }

    @Override
    public int sampleCount()
    {
        return mComplexSamples.i().length;
    }

    @Override
    public long getTimestamp()
    {
        return mComplexSamples.timestamp();
    }

    /**
     * Simple iterator implementation.
     */
    public class ComplexSamplesIterator implements Iterator<ComplexSamples>
    {
        public boolean mHasNext = true;

        @Override
        public boolean hasNext()
        {
            return mHasNext;
        }

        @Override
        public ComplexSamples next()
        {
            mHasNext = false;
            return mComplexSamples;
        }
    }

    /**
     * Simple iterator implementation.
     */
    public class InterleavedComplexSamplesIterator implements Iterator<InterleavedComplexSamples>
    {
        public boolean mHasNext = true;

        @Override
        public boolean hasNext()
        {
            return mHasNext;
        }

        @Override
        public InterleavedComplexSamples next()
        {
            mHasNext = false;
            return mComplexSamples.toInterleaved();
        }
    }
}
