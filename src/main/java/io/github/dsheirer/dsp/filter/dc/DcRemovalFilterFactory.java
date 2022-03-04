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

package io.github.dsheirer.dsp.filter.dc;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;

/**
 * Factory for constructing the optimal DC removal filter.
 */
public class DcRemovalFilterFactory
{
    /**
     * Creates the optimal DC removal filter for this computer
     * @param gain to apply to DC removal
     * @return optimal implementation.
     */
    public static IDcRemovalFilter getFilter(float gain)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.DC_REMOVAL_REAL);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorDcRemovalFilter(gain);
            case SCALAR:
            default:
                return new ScalarDcRemovalFilter(gain);
        }
    }
}
