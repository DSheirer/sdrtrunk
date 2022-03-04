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

package io.github.dsheirer.dsp.gain.complex;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for selecting and creating the optimal implementation of complex gain
 * control for this hardware.
 */
public class ComplexGainFactory
{
    /**
     * Instantiates the optimal complex gain control implementation for this hardware,
     * as determined by the Calibration Manager.
     */
    public static IComplexGainControl getComplexGainControl()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.GAIN_CONTROL_COMPLEX);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
            {
                return new VectorComplexGainControl();
            }
            case SCALAR:
            default:
                return new ComplexGainControl();
        }
    }

    /**
     * Instantiates the optimal complex gain implementation for this hardware,
     * as determined by the Calibration Manager.
     */
    public static ComplexGain getComplexGain(float gain)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.GAIN_CONTROL_COMPLEX);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
            {
                return new VectorComplexGain(gain);
            }
            case SCALAR:
            default:
                return new ScalarComplexGain(gain);
        }
    }
}
