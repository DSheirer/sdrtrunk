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

package io.github.dsheirer.dsp.filter.channelizer.output;

import io.github.dsheirer.sample.complex.ComplexSamples;

public class SampleAssembler
{
    private final int mBufferSize;
    private float[] mI;
    private float[] mQ;
    private int mPointer = 0;

    /**
     * Creates a sample assembler to produce buffers of the specified size.
     * @param bufferSize for a full buffer
     */
    public SampleAssembler(int bufferSize)
    {
        mBufferSize = bufferSize;
        reset();
    }

    /**
     * Resets the buffer pointer and creates new (empty) I and Q arrays.
     */
    private void reset()
    {
        mPointer = 0;
        mI = new float[mBufferSize];
        mQ = new float[mBufferSize];
    }

    /**
     * Adds the I & Q samples to this assembler.
     * @param i inphase sample
     * @param q quadrature sample.
     */
    public void receive(float i, float q)
    {
        if(mPointer >= mBufferSize)
        {
            throw new IllegalStateException("Buffer is full.  Use flushBuffer() to reset the assembler.");
        }

        mI[mPointer] = i;
        mQ[mPointer++] = q;
    }

    /**
     * Indicates if this assembler has a full buffer.
     * Note: use getBufferAndReset() to access the full buffer.
     */
    public boolean hasBuffer()
    {
        return mPointer >= mBufferSize;
    }

    /**
     * Extracts the full buffer from this assembler and resets the assembler to accept more samples.
     */
    public ComplexSamples getBufferAndReset(long timestamp)
    {
        ComplexSamples buffer = new ComplexSamples(mI, mQ, timestamp);
        reset();
        return buffer;
    }
}
