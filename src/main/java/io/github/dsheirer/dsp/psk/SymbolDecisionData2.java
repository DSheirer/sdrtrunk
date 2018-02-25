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
package io.github.dsheirer.dsp.psk;

import io.github.dsheirer.buffer.ComplexCircularBuffer;
import io.github.dsheirer.sample.complex.Complex;

public class SymbolDecisionData2
{
    //45 degrees rotation to orient the symbol to a polar axis to make error calculation easy/efficient
    public static final Complex DIFFERENTIAL_OFFSET = Complex.fromAngle(Math.PI / 4.0d);

    private ComplexCircularBuffer mBuffer;
    private float mSamplingPoint;

    public SymbolDecisionData2(int samplesPerSymbol)
    {
        mBuffer = new ComplexCircularBuffer(2 * samplesPerSymbol);
    }

    public void receive(float inphase, float quadrature)
    {
        mBuffer.put(new Complex(inphase, quadrature));
    }

    public void receive(Complex complex)
    {
        mBuffer.put(complex.copy());
    }

    public void setSamplingPoint(float samplingPoint)
    {
        mSamplingPoint = samplingPoint;
    }

    public float getSamplingPoint()
    {
        return mSamplingPoint;
    }

    public Complex[] getDemodulated()
    {
        Complex[] samples = mBuffer.getAll();

        int length = samples.length / 2;

        Complex[] demodulated = new Complex[length];

        for(int x = 0; x < samples.length / 2; x++)
        {
            Complex copy = samples[length + x].copy();
            copy.multiply(samples[x].conjugate());

            //Test - rotate the demodulated symbol by 45 degrees to see the impact on the eye diagram
//            copy.multiply(DIFFERENTIAL_OFFSET);
            demodulated[x] = copy;
        }

        return demodulated;
    }
}
