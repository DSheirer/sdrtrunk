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
import io.github.dsheirer.sample.complex.ComplexBuffer;

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

    public float[] getCoefficients()
    {
        return mIFilter.getCoefficients();
    }

    public float filterInphase(float sample)
    {
        return mIFilter.filter(sample);
    }

    public float filterQuadrature(float sample)
    {
        return mQFilter.filter(sample);
    }

    public Complex filter(Complex sample)
    {
        float i = filterInphase(sample.inphase());
        float q = filterQuadrature(sample.quadrature());

        return new Complex(i, q);
    }

    /**
     * Filters the complex samples from the buffer and returns a new complex buffer with the filtered output
     *
     * @param buffer with complex samples to filter
     * @return new buffer containing filtered complex samples
     */
    public ComplexBuffer filter(ComplexBuffer buffer)
    {
        float[] samples = buffer.getSamples();
        float[] filtered = new float[samples.length];

        for(int x = 0; x < samples.length; x += 2)
        {
            filtered[x] = filterInphase(samples[x]);
            filtered[x + 1] = filterQuadrature(samples[x + 1]);
        }

        return new ComplexBuffer(filtered);
    }

    @Override
    public void dispose()
    {
        mIFilter.dispose();
        mQFilter.dispose();
    }
}
