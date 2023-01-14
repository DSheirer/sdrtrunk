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

package io.github.dsheirer.dsp.filter.hilbert;

import io.github.dsheirer.sample.complex.ComplexSamples;
import org.apache.commons.math3.util.FastMath;

/**
 * Base Hilbert transform class.
 */
public abstract class HilbertTransform
{
    /*  Half-band filter coefficients retrieved October 2015 from:
     *  https://github.com/airspy/host/libairspy/src/filters.h
     *
     *  Copyright (C) 2014, Youssef Touil <youssef@airspy.com>
     *
     *  Permission is hereby granted, free of charge, to any person obtaining a copy
     *  of this software and associated documentation files (the "Software"), to deal
     *  in the Software without restriction, including without limitation the rights
     *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
     *  copies of the Software, and to permit persons to whom the Software is
     *  furnished to do so, subject to the following conditions:
     *
     *  The above copyright notice and this permission notice shall be included in
     *  all copies or substantial portions of the Software.
     *
     *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
     *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
     *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
     *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
     *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
     *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
     *  THE SOFTWARE.
     ******************************************************************************/
    public static final float[] HALF_BAND_FILTER_47_TAP = new float[]
    {
            -0.000998606272947510f, 0.0f,  0.001695637278417295f, 0.0f,
            -0.003054430179754289f, 0.0f,  0.005055504379767936f, 0.0f,
            -0.007901319195893647f, 0.0f,  0.011873357051047719f, 0.0f,
            -0.017411159379930066f, 0.0f,  0.025304817427568772f, 0.0f,
            -0.037225225204559217f, 0.0f,  0.057533286997004301f, 0.0f,
            -0.102327462004259350f, 0.0f,  0.317034472508947400f, 0.5f,
            0.317034472508947400f, 0.0f, -0.102327462004259350f, 0.0f,
            0.057533286997004301f, 0.0f, -0.037225225204559217f, 0.0f,
            0.025304817427568772f, 0.0f, -0.017411159379930066f, 0.0f,
            0.011873357051047719f, 0.0f, -0.007901319195893647f, 0.0f,
            0.005055504379767936f, 0.0f, -0.003054430179754289f, 0.0f,
            0.001695637278417295f, 0.0f, -0.000998606272947510f
    };

    protected float[] mCoefficients;
    protected int mIOverlap = 11;
    protected int mQOverlap = 23;
    protected float[] mIBuffer = new float[mIOverlap];
    protected float[] mQBuffer = new float[mQOverlap];

    public HilbertTransform()
    {
        mCoefficients = convertHalfBandToHilbert(HALF_BAND_FILTER_47_TAP);
    }

    /**
     * Converts the real sample array to complex samples as half the sample rate
     * @param samples to convert
     * @param timestamp of the first sample
     * @return converted samples
     */
    public abstract ComplexSamples filter(float[] samples, long timestamp);

    /**
     * Converts the half-band filter coefficients for use as hilbert transform filter coefficients.  Sets all
     * even-numbered coefficients left of center tap to negative and all even-numbered coefficients right of
     * center tap to positive and sets the center tap from 0.5 to 0.0, resulting in an overall 2x reduction
     * in gain.  We throw away the odd-number coefficients because they are 0.0 or 0.5 (center).
     *
     * Since we need the filter to be reversed for implementation and since the filter is symmetrical, we
     * flip it during assignment and make the left of center positive and the right of center negative.
     *
     * Note: we apply a 2.0 gain to the coefficients to compensate for the loss, or zero-ing of the original
     * 0.5 center coefficient.
     *
     * As described in Understanding Digital Signal Processing, Lyons, 3e, 2011, sections 13.1.2 and
     * 13.1.3 (p 674-678) and implemented as described in Section 13.37.1 and 13.37.2 (p 802-804)
     */
    public static float[] convertHalfBandToHilbert(float[] coefficients)
    {
        float[] hilbert = new float[coefficients.length / 2 + 1];

        int middle = coefficients.length / 2;

        for(int x = 0; x < coefficients.length; x += 2)
        {
            if(x < middle)
            {
                hilbert[x / 2] = -2.0f * FastMath.abs(coefficients[x]);
            }
            else if(x > middle)
            {
                hilbert[x / 2] = 2.0f * FastMath.abs(coefficients[x]);
            }
        }

        return hilbert;
    }
}
