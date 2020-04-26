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
package io.github.dsheirer.dsp.filter.dc;

import io.github.dsheirer.sample.buffer.FloatBuffer;

/**
 * DC removal filter for removing any DC offset that may be present when a signal is not aligned within the
 * demodulated channel.
 */
public class DCRemovalFilter
{
    private Object mReusableBufferQueue = new Object();
    protected float mAverage;
    protected float mRatio;

    /**
     * Creates a DC removal filter.
     *
     * @param ratio
     */
    public DCRemovalFilter(float ratio)
    {
        mRatio = ratio;
    }

    /**
     * Resets the running average to zero.
     */
    public void reset()
    {
        mAverage = 0.0f;
    }

    /**
     * Filters the sample and returns a filtered (ie DC-removed) sample.
     * @param sample to filter
     * @return filtered sample
     */
    public float filter(float sample)
    {
        float filtered = sample - mAverage;
        mAverage += mRatio * filtered;
        return filtered;
    }

    public float[] filter( float[] samples )
    {
        for( int x = 0; x < samples.length; x++ )
        {
            samples[ x ] = filter( samples[ x ] );
        }

        return samples;
    }

    /**
     * Filters the buffer samples and returns a new reusable buffer with the filtered samples.
     *
     * Note: this method automatically manages the user accounting between the buffer argument and the new returned
     * filtered sample buffer.
     *
     * @param unfilteredBuffer containing samples to filter
     * @return a new reusable buffer containing the filtered samples with the user count set to 1.
     */
    public FloatBuffer filter(FloatBuffer unfilteredBuffer)
    {
        FloatBuffer buffer = new FloatBuffer(new float[unfilteredBuffer.getSampleCount()]);
        FloatBuffer filteredBuffer = buffer;

        float[] unfilteredSamples = unfilteredBuffer.getSamples();
        float[] filteredSamples = filteredBuffer.getSamples();

        for(int x = 0; x < unfilteredSamples.length; x++)
        {
            filteredSamples[x] = filter(unfilteredSamples[x]);
        }

        return filteredBuffer;
    }
}