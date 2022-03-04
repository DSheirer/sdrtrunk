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

package io.github.dsheirer.dsp.filter.hilbert;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory class for creating an optimal implementation of a Hilbert Transform as determined by the
 * Calibration manager.
 */
public class HilbertTransformFactory
{
    public static HilbertTransform getHilbertTransform()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.HILBERT_TRANSFORM);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorHilbertTransformDefaultBits();
            case VECTOR_SIMD_64:
                return new VectorHilbertTransform64Bits();
            case VECTOR_SIMD_128:
                return new VectorHilbertTransform128Bits();
            case VECTOR_SIMD_256:
                return new VectorHilbertTransform256Bits();
            case VECTOR_SIMD_512:
                return new VectorHilbertTransform512Bits();
            case SCALAR:
            case UNCALIBRATED:
            default:
                return new ScalarHilbertTransform();
        }
    }
}
