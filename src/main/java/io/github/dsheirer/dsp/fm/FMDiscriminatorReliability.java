/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.dsp.fm;

import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;

/**
 * Calculates the soft symbol reliability (inverse of the relative noise variance) of each sample produced by an FM
 * discriminator followed by a post-discriminator FIR filter (ie a C4FM receive filter), from the complex samples that
 * are input to the discriminator.
 *
 * The discriminator output is the phase difference between consecutive samples: d[n] = phase(x[n]) - phase(x[n - 1]),
 * so the filtered output y[n] = sum(h[k] * d[n - k]) = sum(g[k] * phase(x[n - k])) where g[k] = h[k] - h[k - 1] (with
 * h[-1] = h[L] = 0).  The phase noise variance of each sample is inversely proportional to the sample power, so the
 * noise variance of the filtered output is proportional to sum(g[k]^2 / |x[n - k]|^2), and the reliability is the
 * inverse of that sum.  The sum is calculated as an FIR filter of the inverse sample power with taps g[k]^2.
 *
 * Testing against a P25 LSM simulcast recording found that most of the symbol errors occur in the small fraction of
 * symbols with the lowest envelope, so weighting the soft decision decoding by the reliability recovers many of the
 * blocks that fail unweighted soft decision decoding.
 */
public class FMDiscriminatorReliability
{
    //Minimum sample power, avoiding division by zero for samples with zero power
    private static final float MINIMUM_POWER = 1e-30f;
    private final IRealFilter mInversePowerFilter;

    /**
     * Constructs an instance
     * @param postDiscriminatorFilterTaps for the FIR filter applied to the FM discriminator output.
     */
    public FMDiscriminatorReliability(float[] postDiscriminatorFilterTaps)
    {
        mInversePowerFilter = new RealFIRFilter(getInversePowerFilterTaps(postDiscriminatorFilterTaps));
    }

    /**
     * Calculates the inverse sample power filter taps: g[k]^2 where g[k] = h[k] - h[k - 1] for k = 0 to L, with
     * h[-1] = h[L] = 0.
     * @param taps of the post-discriminator filter (h)
     * @return inverse power filter taps, one longer than the post-discriminator filter.
     */
    public static float[] getInversePowerFilterTaps(float[] taps)
    {
        float[] squared = new float[taps.length + 1];

        for(int k = 0; k <= taps.length; k++)
        {
            float current = k < taps.length ? taps[k] : 0.0f;
            float previous = k > 0 ? taps[k - 1] : 0.0f;
            float difference = current - previous;
            squared[k] = difference * difference;
        }

        return squared;
    }

    /**
     * Calculates the reliability of each filtered discriminator output sample from the discriminator input samples.
     * @param i inphase samples input to the FM discriminator
     * @param q quadrature samples input to the FM discriminator
     * @return reliabilities, parallel to the filtered discriminator output samples.
     */
    public float[] calculate(float[] i, float[] q)
    {
        float[] inversePower = new float[i.length];

        for(int x = 0; x < i.length; x++)
        {
            inversePower[x] = 1.0f / Math.max(i[x] * i[x] + q[x] * q[x], MINIMUM_POWER);
        }

        float[] reliabilities = mInversePowerFilter.filter(inversePower);

        for(int x = 0; x < reliabilities.length; x++)
        {
            reliabilities[x] = reliabilities[x] > 0.0f ? 1.0f / reliabilities[x] : 0.0f;
        }

        return reliabilities;
    }
}
