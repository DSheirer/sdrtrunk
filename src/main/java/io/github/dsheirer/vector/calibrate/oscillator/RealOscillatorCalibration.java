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

package io.github.dsheirer.vector.calibrate.oscillator;

import io.github.dsheirer.dsp.oscillator.IRealOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * Real oscillator calibration
 */
public class RealOscillatorCalibration extends Calibration
{
    private static final double FREQUENCY = 5.0;
    private static final double SAMPLE_RATE = 100.0;
    private static final int BUFFER_SIZE = 2048;

    /**
     * Constructs an instance
     */
    public RealOscillatorCalibration()
    {
        super(CalibrationType.OSCILLATOR_REAL);
    }

    @Override protected List<Candidate> getCandidates()
    {
        return List.of(
            candidate(Implementation.SCALAR, () -> {
                IRealOscillator operation = OscillatorFactory.getRealOscillator(
                    Implementation.SCALAR, FREQUENCY, SAMPLE_RATE);
                return () -> operation.generate(BUFFER_SIZE);
            }),
            candidate(Implementation.VECTOR_SIMD_PREFERRED, () -> {
                IRealOscillator operation = OscillatorFactory.getRealOscillator(
                    Implementation.VECTOR_SIMD_PREFERRED, FREQUENCY, SAMPLE_RATE);
                return () -> operation.generate(BUFFER_SIZE);
            }));
    }

    /**
     * The vector oscillator accumulates phase differently than the scalar oscillator, so their outputs drift apart
     * slightly over time.
     */
    @Override protected double getTolerance()
    {
        return 1e-3;
    }
}
