/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.buffer.ComplexCircularBuffer;
import io.github.dsheirer.sample.complex.Complex;
import org.apache.commons.math3.util.FastMath;

public class SymbolDecisionData
{
    //45 degrees rotation to orient the symbol to a polar axis to make error calculation easy/efficient
    public static final Complex DIFFERENTIAL_OFFSET = Complex.fromAngle(FastMath.PI / 4.0d);

    private ComplexCircularBuffer mBuffer;
    private float mSamplingPoint;
    private float mSamplesPerSymbol;

    /**
     * Circular buffer for capturing two symbols worth of sample data for use in instrumentation.
     * @param samplesPerSymbol to size the buffer
     */
    public SymbolDecisionData(float samplesPerSymbol)
    {
        mSamplesPerSymbol = samplesPerSymbol;
        mBuffer = new ComplexCircularBuffer(8);
    }

    /**
     * Stores the complex sample in the buffer
     */
    public void receive(float inphase, float quadrature)
    {
        mBuffer.put(new Complex(inphase, quadrature));
    }

    /**
     * Stores the complex sample in the buffer
     */
    public void receive(Complex complex)
    {
        mBuffer.put(complex.copy());
    }

    /**
     * Sets the fractional sampling point for the current symbol to use in interpolating the current symbol.
     */
    public void setSamplingPoint(float samplingPoint)
    {
        mSamplingPoint = samplingPoint;
    }

    /**
     * Fractional sampling point
     */
    public float getSamplingPoint()
    {
        return mSamplingPoint;
    }

    /**
     * Raw samples
     */
    public Complex[] getSamples()
    {
        return mBuffer.getAll();
    }

    public Complex[] getSamples(int length)
    {
        return mBuffer.get(length);
    }

    /**
     * Array of (FM) demodulated samples representing the current symbol where each current sample is demodulated against
     * the previous symbol's sample to produce the differential demodulated sample.  Samples are in time order.
     *
     * Note: differential decoding is on an integral samples-per-symbol basis and does not use fractional interpolation.
     */
    public Complex[] getDemodulated()
    {
        Complex[] samples = mBuffer.getAll();

        int length = samples.length / 2;

        Complex[] demodulated = new Complex[length];

        for(int x = 0; x < samples.length / 2; x++)
        {
            Complex copy = samples[length + x].copy();
            copy.multiply(samples[x].conjugate());
            demodulated[x] = copy;
        }

        return demodulated;
    }
}
