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

package io.github.dsheirer.dsp.gain.complex;

import io.github.dsheirer.sample.complex.ComplexSamples;

/**
 * Gain control to apply a fixed level of gain to complex samples.
 */
public abstract class ComplexGain
{
    protected float mGain;

    /**
     * Constructs an instance
     * @param gain to apply to complex samples
     */
    public ComplexGain(float gain)
    {
        mGain = gain;
    }

    /**
     * Gain value that will be applied to complex samples
     * @return gain value
     */
    public float getGain()
    {
        return mGain;
    }

    /**
     * Sets the gain value to apply to the complex samples
     * @param gain value
     */
    protected void setGain(float gain)
    {
        mGain = gain;
    }

    /**
     * Applies a gain value to the complex samples.
     *
     * Note: the gain is applied directly to the buffer, modifying and returning the
     * original sample buffer.
     * @param samples to amplify
     * @return amplified samples
     */
    public ComplexSamples apply(ComplexSamples samples)
    {
        return apply(samples.i(), samples.q(), samples.timestamp());
    }

    /**
     * Applies a gain value to the complex samples.
     *
     * Note: the gain is applied directly to the buffer, modifying and returning the
     * original sample buffer.
     * @param i samples to amplify
     * @param q samples to amplify
     * @param timestamp of the first sample
     * @return amplified samples
     */
    public abstract ComplexSamples apply(float[] i, float q[], long timestamp);
}
