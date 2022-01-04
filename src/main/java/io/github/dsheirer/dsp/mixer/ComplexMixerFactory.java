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

package io.github.dsheirer.dsp.mixer;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating complex mixer instances from scalar and vector implementations.
 */
public class ComplexMixerFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(ComplexMixerFactory.class);

    /**
     * Creates an instance of the optimal implementation of a complex mixer for this hardware.
     * @param frequency of the mixing oscillator
     * @param sampleRate of the mixing oscillator
     * @return optimal instance
     */
    public static ComplexMixer getMixer(double frequency, double sampleRate)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.MIXER_COMPLEX);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                {
                    return new VectorComplexMixer(frequency, sampleRate);
                }
            case SCALAR:
                {
                    return new ScalarComplexMixer(frequency, sampleRate);
                }
            default:
                {
                    mLog.warn("Unrecognized complex mixer implementation: " + implementation);
                    return new ScalarComplexMixer(frequency, sampleRate);
                }
        }
    }
}
