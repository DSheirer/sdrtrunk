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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Processes I & Q sample arrays into uniform length complex samples buffers.
 */
public class ComplexSamplesRepackager
{
    private int mSize;
    private float[] mIResidual = new float[0];
    private float[] mQResidual = new float[0];

    /**
     * Constructs an instance.
     * @param size or length of the output complex samples.
     */
    public ComplexSamplesRepackager(int size)
    {
        mSize = size;
    }

    /**
     * Combines the I & Q sample arrays with any residual leftover from the previous invocation and
     * chunks the arrays up into uniform length complex sample buffers, storing any leftover samples
     * as residual to use in the next processing invocation.  If the length of the sample arguments
     * combined with any residual is not enough to create at least one buffer, those samples will be
     * added to the residual and used in the next operation, and no complex sample buffer will be
     * returned.
     *
     * @param i samples array to repackage
     * @param q samples array to repackage
     * @return zero or more complex samples of uniform length.
     */
    public List<ComplexSamples> process(float[] i, float[] q)
    {
        float[] iCombined = new float[mIResidual.length + i.length];
        float[] qCombined = new float[mQResidual.length + q.length];

        if(mIResidual.length > 0)
        {
            System.arraycopy(mIResidual, 0, iCombined, 0, mIResidual.length);
            System.arraycopy(mQResidual, 0, qCombined, 0, mQResidual.length);
        }

        System.arraycopy(i, 0, iCombined, mIResidual.length, i.length);
        System.arraycopy(q, 0, qCombined, mQResidual.length, q.length);

        int processed = 0;

        List<ComplexSamples> samples = new ArrayList<>();

        while((processed + mSize) < iCombined.length)
        {
            float[] iChunk = Arrays.copyOfRange(iCombined, processed, processed + mSize);
            float[] qChunk = Arrays.copyOfRange(qCombined, processed, processed + mSize);
            samples.add(new ComplexSamples(iChunk, qChunk));
            processed += mSize;
        }

        if(processed < iCombined.length)
        {
            mIResidual = Arrays.copyOfRange(iCombined, processed, iCombined.length);
            mQResidual = Arrays.copyOfRange(qCombined, processed, qCombined.length);
        }
        else
        {
            mIResidual = new float[0];
            mQResidual = new float[0];
        }

        return samples;
    }
}
