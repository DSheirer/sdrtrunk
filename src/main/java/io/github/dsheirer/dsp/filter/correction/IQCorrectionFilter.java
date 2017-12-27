/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.correction;

import io.github.dsheirer.sample.complex.ComplexBuffer;

public class IQCorrectionFilter
{
    private float mRatio = 0.00001f;
    private float mAverageInphase = 0.0f;
    private float mAverageQuadrature = 0.0f;

    public IQCorrectionFilter(float ratio)
    {
        mRatio = ratio;
    }

    public IQCorrectionFilter()
    {
    }

    public void filter(ComplexBuffer buffer)
    {
        float[] samples = buffer.getSamples();

        for(int x = 0; x < samples.length; x += 2)
        {
            mAverageInphase = mAverageInphase + (mRatio * (samples[x] - mAverageInphase));
            samples[x] -= mAverageInphase;

            mAverageQuadrature = mAverageQuadrature + (mRatio * (samples[x + 1] - mAverageQuadrature));
            samples[x + 1] -= mAverageQuadrature;
        }
    }
}
