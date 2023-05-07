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

import io.github.dsheirer.dsp.magnitude.IMagnitudeCalculator;
import io.github.dsheirer.dsp.magnitude.MagnitudeFactory;
import io.github.dsheirer.vector.VectorUtilities;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import org.apache.commons.math3.util.FastMath;

/**
 * Performs AM demodulation on baseband I/Q samples to produce demodulated float output.
 */
public class VectorAMDemodulator256 implements IAmDemodulator
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_256;
    private float mGain;
    private IMagnitudeCalculator mMagnitudeCalculator = MagnitudeFactory.getMagnitudeCalculator();

    /**
     * Constructs this demodulator where the specified gain is applied to demodulated output samples.
     *
     * @param gain to apply to demodulated output samples.
     */
    public VectorAMDemodulator256(float gain)
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
        VectorUtilities.checkArrayLength(magnitude, VECTOR_SPECIES);
        FloatVector gain = FloatVector.broadcast(VECTOR_SPECIES, mGain);
        float[] demodulated = new float[magnitude.length];

        FloatVector vector;
        for(int x = 0; x < magnitude.length; x += VECTOR_SPECIES.length())
        {
            vector = FloatVector.fromArray(VECTOR_SPECIES, magnitude, x);
            vector.sqrt().mul(gain).intoArray(demodulated, x);
        }

        return demodulated;
    }
}
