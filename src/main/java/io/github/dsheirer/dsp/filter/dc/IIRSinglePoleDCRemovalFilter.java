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
package io.github.dsheirer.dsp.filter.dc;

public class IIRSinglePoleDCRemovalFilter
{
    private float mAlpha;
    private float mPreviousInput = 0.0f;
    private float mPreviousOutput = 0.0f;
    private float mCurrentOutput = 0.0f;

    /**
     * IIR single-pole DC removal filter, as described by J M de Freitas in
     * 29Jan2007 paper at:
     *
     * http://www.mathworks.com/matlabcentral/fileexchange/downloads/72134/download
     *
     * @param alpha 0.0 - 1.0 float - the closer alpha is to unity, the closer
     * the cutoff frequency is to DC.
     */
    public IIRSinglePoleDCRemovalFilter(float alpha)
    {
        mAlpha = alpha;
    }

    /**
     * Filters the sample
     * @param sample to filter
     * @return filtered output sample
     */
    public float filter(float sample)
    {
        mCurrentOutput = (sample - mPreviousInput) + (mAlpha * mPreviousOutput);

        mPreviousInput = sample;
        mPreviousOutput = mCurrentOutput;

        return mCurrentOutput;
    }

    /**
     * Filters the input buffer and returns a new filtered output buffer.
     *
     * Buffer user accounting is handled by this method where the input buffer user count is decremented and the
     * returned output buffer has the user count incremented to one.
     *
     * @param samples to filter
     * @return filtered samples in a new (ie reused) output buffer
     */
    public float[] filter(float[] samples)
    {
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = filter(samples[x]);
        }

        return samples;
    }
}
