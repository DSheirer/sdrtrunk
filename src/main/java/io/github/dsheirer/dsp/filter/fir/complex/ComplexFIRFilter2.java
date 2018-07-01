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
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter2;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import io.github.dsheirer.sample.complex.Complex;

public class ComplexFIRFilter2 extends FIRFilter
{
    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("Complex FIR Filter");
    private RealFIRFilter2 mIFilter;
    private RealFIRFilter2 mQFilter;

    /**
     * Complex FIR Filter for processing complex sample pairs that internally uses two RealFIRFilter
     * instances.
     *
     * @param coefficients - filter taps
     * @param gain - gain to apply to filtered outputs - use 1.0f for no gain
     */
    public ComplexFIRFilter2(float[] coefficients, float gain)
    {
        mIFilter = new RealFIRFilter2(coefficients, gain);
        mQFilter = new RealFIRFilter2(coefficients, gain);
    }

    /**
     * Complex FIR Filter for processing complex sample pairs that internally uses two RealFIRFilter
     * instances.  This constructor uses a default gain of 1.0f.
     *
     * @param coefficients - filter taps
     */
    public ComplexFIRFilter2(float[] coefficients)
    {
        this(coefficients, 1.0f);
    }

    /**
     * Filters the inphase sample value.
     * @param sample to filter
     * @return filtered sample
     */
    public float filterInphase(float sample)
    {
        return mIFilter.filter(sample);
    }

    /**
     * Current filtered inphase sample value after invoking the filterInphase() method.
     */
    public float currentInphaseValue()
    {
        return mIFilter.currentValue();
    }

    /**
     * Filters the quadrature sample value.
     * @param sample to filter
     * @return filtered sample
     */
    public float filterQuadrature(float sample)
    {
        return mQFilter.filter(sample);
    }

    /**
     * Current filtered quadrature sample value after invoking the filterInphase() method.
     */
    public float currentQuadratureValue()
    {
        return mQFilter.currentValue();
    }

    /**
     * Filters the complex sample.
     * @param sample to filter
     * @return filtered sample
     */
    public Complex filter(Complex sample)
    {
        float i = filterInphase(sample.inphase());
        float q = filterQuadrature(sample.quadrature());

        return new Complex(i, q);
    }

    /**
     * Filters the complex samples from the reusable buffer and returns a new complex buffer with the filtered output
     *
     * Note: the original reusable buffer user count is decremented and a new reusable buffer is returned
     * with the user count already incremented to one.
     *
     * @param originalBuffer with complex samples to filter
     * @return new buffer containing filtered complex samples
     */
    public ReusableComplexBuffer filter(ReusableComplexBuffer originalBuffer)
    {
        ReusableComplexBuffer filteredBuffer = mReusableComplexBufferQueue.getBuffer(originalBuffer.getSamples().length);
        filteredBuffer.setTimestamp(originalBuffer.getTimestamp());

        float[] samples = originalBuffer.getSamples();
        float[] filteredSamples = filteredBuffer.getSamples();

        for(int x = 0; x < originalBuffer.getSamples().length; x += 2)
        {
            filteredSamples[x] = filterInphase(samples[x]);
            filteredSamples[x + 1] = filterQuadrature(samples[x + 1]);
        }

        originalBuffer.decrementUserCount();

        return filteredBuffer;
    }

    @Override
    public void dispose()
    {
        mIFilter.dispose();
        mQFilter.dispose();
        mReusableComplexBufferQueue.dispose();
    }
}
