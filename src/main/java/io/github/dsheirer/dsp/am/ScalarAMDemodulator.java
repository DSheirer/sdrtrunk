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
package io.github.dsheirer.dsp.am;

import org.apache.commons.math3.util.FastMath;

/**
 * Performs AM demodulation on baseband I/Q samples to produce demodulated float output.
 */
public class ScalarAMDemodulator implements IAmDemodulator
{
    private float mGain;

    /**
     * Constructs this demodulator where the specified gain is applied to demodulated output samples.
     *
     * @param gain to apply to demodulated output samples.
     */
    public ScalarAMDemodulator(float gain)
    {
        mGain = gain;
    }

    /**
     * Sets the gain to the specified level.
     */
    public void setGain(float gain)
    {
        mGain = gain;
    }

    /**
     * Demodulates the comples I/Q sample and returns the demodulated output sample.
     *
     * @param inphase sample
     * @param quadrature sample
     * @return AM demodulated sample
     */
    public float demodulate(float inphase, float quadrature)
    {
        return (float) FastMath.sqrt((inphase * inphase) + (quadrature * quadrature)) * mGain;
    }

    @Override
    public float[] demodulateMagnitude(float[] magnitude)
    {
        float[] demodulated = new float[magnitude.length];
        for(int x = 0; x < magnitude.length; x++)
        {
            demodulated[x] = (float)FastMath.sqrt(magnitude[x]) * mGain;
        }

        return demodulated;
    }
}
