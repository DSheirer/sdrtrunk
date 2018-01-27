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
package io.github.dsheirer.dsp.filter.fir.complex;

import io.github.dsheirer.dsp.filter.fir.FIRFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.sample.complex.Complex;

public class ComplexFIRFilter extends FIRFilter
{
    private RealFIRFilter mIFilter;
    private RealFIRFilter mQFilter;

    /**
     * Complex FIR Filter for processing complex sample pairs.  Wraps two real
     * FIR filters for processing each of the inphase and quadrature samples.
     *
     * @param coefficients - filter taps
     * @param gain - gain to apply to filtered outputs - use 1.0f for no gain
     */
    public ComplexFIRFilter(float[] coefficients, float gain)
    {
        mIFilter = new RealFIRFilter(coefficients, gain);
        mQFilter = new RealFIRFilter(coefficients, gain);
    }

    /**
     * Coefficients used in each of the I and Q filters
     */
    public float[] getCoefficients()
    {
        return mIFilter.getCoefficients();
    }

    /**
     * Filters the inphase sample independent of the quadrature side of the filter
     * @param sample to filter
     * @return filtered inphase sample
     */
    public float filterInphase(float sample)
    {
        return mIFilter.filter(sample);
    }

    /**
     * Filters the quadrature sample independent of the inphase side of the filter
     * @param sample to filter
     * @return filtered quadrature sample
     */
    public float filterQuadrature(float sample)
    {
        return mQFilter.filter(sample);
    }

    /**
     * Loads the complex sample and provides a filtered output
     * @param sample to load
     * @return filtered complex sample
     */
    public Complex filter(Complex sample)
    {
        return filter(sample.inphase(), sample.quadrature());
    }

    /**
     * Loads the complex sample and provides a filtered output
     * @param i inphase sample to load
     * @param q quadrature sample to load
     * @return filtered complex sample
     */
    public Complex filter(float i, float q)
    {
        float filteredI = filterInphase(i);
        float filteredQ = filterQuadrature(q);

        return new Complex(filteredI, filteredQ);
    }

    /**
     * Filters the complex sample array where the samples are loaded as i0,q0,i1,q1 ... iN-2,qN-1
     *
     * @param samples to filter.
     */
    public float[] filter(float[] samples)
    {
        for(int x = 0; x < samples.length; x += 2)
        {
            samples[x] = filterInphase(samples[x]);
            samples[x + 1] = filterInphase(samples[x + 1]);
        }

        return samples;
    }

    /**
     * Loads the complex sample into the filter but does not calculate a filtered output sample
     * @param i inphase sample
     * @param q quadrature sample
     */
    public void load(float i, float q)
    {
        mIFilter.load(i);
        mQFilter.load(q);
    }

    @Override
    public void dispose()
    {
        mIFilter.dispose();
        mQFilter.dispose();
    }
}
