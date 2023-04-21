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

package io.github.dsheirer.dsp.filter.iir;

/**
 * Single pole IIR filter
 */
public class SinglePoleIirFilter
{
    /**
     * Decay value, in range: 0 <-> 1.0
     */
    private float mAlpha;

    /**
     * 1.0 - alpha
     */
    private float mOneMinusAlpha;

    /**
     * Current filter output value.
     */
    private float mOutput;

    /**
     * Constructs a single pole IIR filter
     * @param alpha decay value in range (0.0 - 1.0)
     */
    public SinglePoleIirFilter(float alpha)
    {
        if(alpha < 0.0 || alpha > 1.0)
        {
            throw new IllegalArgumentException("alpha decay value must be in range: 0.0 - 1.0");
        }

        mAlpha = alpha;
        mOneMinusAlpha = 1.0f - alpha;
    }

    /**
     * Processes the specified sample and returns the filter output
     * @param sample to process/filter
     * @return filtered output
     */
    public float filter(float sample)
    {
        mOutput = (mOutput * mOneMinusAlpha) + (mAlpha * sample);
        return mOutput;
    }

    /**
     * Current output value
     */
    public float getValue()
    {
        return mOutput;
    }
}
