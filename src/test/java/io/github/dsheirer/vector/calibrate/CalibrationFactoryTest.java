/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.vector.calibrate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.halfband.RealHalfBandDecimationFilter;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter11Tap256Bit;
import io.github.dsheirer.dsp.filter.halfband.VectorRealHalfBandDecimationFilter256Bit;
import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.dsp.fm.ScalarFMDemodulator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.dsp.window.WindowType;
import org.junit.jupiter.api.Test;

/**
 * Factory methods that create a specific implementation, which calibration uses for its candidates.  Each calibration
 * candidate's implementation is also exercised by CalibrationVerificationTest.
 */
public class CalibrationFactoryTest
{
    @Test
    void uncalibratedCreatesScalar()
    {
        assertInstanceOf(ScalarFMDemodulator.class, FmDemodulatorFactory.getFmDemodulator(Implementation.UNCALIBRATED));
    }

    @Test
    void unsupportedImplementationThrows()
    {
        assertThrows(IllegalArgumentException.class,
            () -> FmDemodulatorFactory.getFmDemodulator(Implementation.VECTOR_SIMD_PREFERRED));
        assertThrows(IllegalArgumentException.class,
            () -> OscillatorFactory.getRealOscillator(Implementation.VECTOR_SIMD_256, 1.0, 10.0));
    }

    @Test
    void halfBandTypeSelectsImplementations()
    {
        float[] coefficients = FilterFactory.getHalfBand(11, WindowType.BLACKMAN);
        assertInstanceOf(VectorRealHalfBandDecimationFilter11Tap256Bit.class, FilterFactory.getRealDecimationFilter(
            CalibrationType.FILTER_HALF_BAND_REAL_11_TAP, Implementation.VECTOR_SIMD_256, coefficients));
        assertInstanceOf(VectorRealHalfBandDecimationFilter256Bit.class, FilterFactory.getRealDecimationFilter(
            CalibrationType.FILTER_HALF_BAND_REAL_DEFAULT, Implementation.VECTOR_SIMD_256, coefficients));
        assertInstanceOf(RealHalfBandDecimationFilter.class, FilterFactory.getRealDecimationFilter(
            CalibrationType.FILTER_HALF_BAND_REAL_DEFAULT, Implementation.UNCALIBRATED, coefficients));
        assertThrows(IllegalArgumentException.class, () -> FilterFactory.getRealDecimationFilter(
            CalibrationType.FM_DEMODULATOR, Implementation.SCALAR, coefficients));
    }

    @Test
    void halfBandLengthSelectsType()
    {
        assertEquals(CalibrationType.FILTER_HALF_BAND_REAL_11_TAP, FilterFactory.getRealHalfBandCalibrationType(11));
        assertEquals(CalibrationType.FILTER_HALF_BAND_REAL_63_TAP, FilterFactory.getRealHalfBandCalibrationType(63));
        assertEquals(CalibrationType.FILTER_HALF_BAND_REAL_DEFAULT, FilterFactory.getRealHalfBandCalibrationType(47));
    }
}
