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

package io.github.dsheirer.dsp.oscillator;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for creating real and complex oscillators.
 *
 * Uses the CalibrationManager to determine the optimal (scalar vs vector) oscillator implementation type.
 */
public class OscillatorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(OscillatorFactory.class);

    /**
     * Constructs an optimal implementation of a real sample oscillator using calibration data when available.
     * @param frequency of the oscillator
     * @param sampleRate of the oscillator
     * @return constructed oscillator
     */
    public static IRealOscillator getRealOscillator(double frequency, double sampleRate)
    {
        Implementation operation = CalibrationManager.getInstance().getImplementation(CalibrationType.OSCILLATOR_REAL);

        switch(operation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorRealOscillator(frequency, sampleRate);
            case SCALAR:
            default:
                return new ScalarRealOscillator(frequency, sampleRate);
        }
    }

    /**
     * Constructs an optimal implementation of a complex sample oscillator using calibration data when available.
     * @param frequency of the oscillator
     * @param sampleRate of the oscillator
     * @return constructed oscillator
     */
    public static IComplexOscillator getComplexOscillator(double frequency, double sampleRate)
    {
        Implementation operation = CalibrationManager.getInstance().getImplementation(CalibrationType.OSCILLATOR_COMPLEX);

        switch(operation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorComplexOscillator(frequency, sampleRate);
            case SCALAR:
            default:
                return new ScalarComplexOscillator(frequency, sampleRate);
        }
    }
}
