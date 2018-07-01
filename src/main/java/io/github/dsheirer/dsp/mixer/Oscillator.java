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
package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.sample.complex.Complex;

public class Oscillator extends AbstractOscillator
{
    private Complex mAnglePerSample;
    private Complex mCurrentAngle = new Complex(0.0f, -1.0f);

    /**
     * Oscillator that produces complex or float samples corresponding to a sine wave oscillating at the specified
     * frequency and sample rate
     *
     * @param frequency - positive or negative frequency in hertz
     * @param sampleRate - in hertz
     */
    public Oscillator(double frequency, double sampleRate)
    {
        super(frequency, sampleRate);
    }

    /**
     * Steps the current angle by the angle per sample amount
     */
    @Override
    public void rotate()
    {
        mCurrentAngle.multiply(mAnglePerSample);
        mCurrentAngle.fastNormalize();
    }

    @Override
    public float inphase()
    {
        return mCurrentAngle.inphase();
    }

    @Override
    public float quadrature()
    {
        return mCurrentAngle.quadrature();
    }

    /**
     * Updates the internal values after a frequency or sample rate change
     */
    @Override
    protected void update()
    {
        float anglePerSample = (float)(2.0d * Math.PI * getFrequency() / getSampleRate());
        mAnglePerSample = Complex.fromAngle(anglePerSample);
    }
}
