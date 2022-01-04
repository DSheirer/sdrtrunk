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

package io.github.dsheirer.dsp.fm;

import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating FM demodulators
 */
public class FmDemodulatorFactory
{
    private static final Logger mLog = LoggerFactory.getLogger(FmDemodulatorFactory.class);

    /**
     * Creates the optimal FM demodulator using calibration data to select the optimal
     * implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static IFmDemodulator getFmDemodulator()
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.FM_DEMODULATOR);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorFMDemodulator();
            case SCALAR:
                return new ScalarFMDemodulator();
            default:
                mLog.warn("Unrecognized optimal operation for FM demodulator: " + implementation.name());
                return new ScalarFMDemodulator();
        }
    }

    /**
     * Creates the optimal Squelching FM demodulator using calibration data to select the optimal
     * implementation from scalar and vector options.
     * @return demodulator instance
     */
    public static ISquelchingFmDemodulator getSquelchingFmDemodulator(float alpha, float threshold, int ramp)
    {
        Implementation implementation = CalibrationManager.getInstance().getImplementation(CalibrationType.FM_DEMODULATOR);

        switch(implementation)
        {
            case VECTOR_SIMD_PREFERRED:
                return new VectorSquelchingFMDemodulator(alpha, threshold, ramp);
            case SCALAR:
                return new ScalarSquelchingFMDemodulator(alpha, threshold, ramp);
            default:
                mLog.warn("Unrecognized optimal operation for Squelching FM demodulator: " + implementation.name());
                return new ScalarSquelchingFMDemodulator(alpha, threshold, ramp);
        }
    }
}
