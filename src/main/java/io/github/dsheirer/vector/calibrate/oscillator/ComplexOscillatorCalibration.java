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

import io.github.dsheirer.dsp.oscillator.IComplexOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * Complex oscillator calibration
 */
public class ComplexOscillatorCalibration extends Calibration
{
    private static final double FREQUENCY = 5.0;
    private static final double SAMPLE_RATE = 100.0;
    private static final int BUFFER_SIZE = 2048;

    /**
     * Constructs an instance
     */
    public ComplexOscillatorCalibration()
    {
        super(CalibrationType.OSCILLATOR_COMPLEX);
    }

    @Override protected List<Candidate> getCandidates()
    {
        return List.of(
            candidate(Implementation.SCALAR, () -> {
                IComplexOscillator operation = OscillatorFactory.getComplexOscillator(
                    Implementation.SCALAR, FREQUENCY, SAMPLE_RATE);
                return () -> operation.generateComplexSamples(BUFFER_SIZE, 0L);
            }),
            candidate(Implementation.VECTOR_SIMD_PREFERRED, () -> {
                IComplexOscillator operation = OscillatorFactory.getComplexOscillator(
                    Implementation.VECTOR_SIMD_PREFERRED, FREQUENCY, SAMPLE_RATE);
                return () -> operation.generateComplexSamples(BUFFER_SIZE, 0L);
            }));
    }

    /**
     * Verifies the candidate against the scalar output after removing a constant phase rotation.  The vector
     * oscillator starts each SIMD lane one sample further into the rotation than the scalar oscillator, which is a
     * constant phase offset that doesn't affect mixing or tuning.
     */
    @Override protected Verification verify(Implementation implementation, float[][] reference, float[][] output)
    {
        float[] referenceI = reference[0];
        float[] referenceQ = reference[1];
        float[] outputI = output[0];
        float[] outputQ = output[1];

        //Estimate the rotation from the reference to the output: sum of output * conjugate(reference)
        double rotationI = 0.0;
        double rotationQ = 0.0;

        for(int x = 0; x < referenceI.length; x++)
        {
            rotationI += outputI[x] * referenceI[x] + outputQ[x] * referenceQ[x];
            rotationQ += outputQ[x] * referenceI[x] - outputI[x] * referenceQ[x];
        }

        double magnitude = Math.hypot(rotationI, rotationQ);

        if(magnitude == 0.0 || Double.isNaN(magnitude))
        {
            return new Verification(implementation, Double.POSITIVE_INFINITY, getTolerance(), "no correlation");
        }

        rotationI /= magnitude;
        rotationQ /= magnitude;

        float[][] rotated = new float[2][referenceI.length];

        for(int x = 0; x < referenceI.length; x++)
        {
            rotated[0][x] = (float)(referenceI[x] * rotationI - referenceQ[x] * rotationQ);
            rotated[1][x] = (float)(referenceI[x] * rotationQ + referenceQ[x] * rotationI);
        }

        Verification verification = super.verify(implementation, rotated, output);
        return new Verification(implementation, verification.error(), verification.tolerance(),
            "after rotating " + Math.round(Math.toDegrees(Math.atan2(rotationQ, rotationI))) + " degrees" +
                (verification.detail() != null ? " " + verification.detail() : ""));
    }

    /**
     * The vector oscillator accumulates rounding error differently than the scalar oscillator, so their outputs drift
     * apart slightly over time.
     */
    @Override protected double getTolerance()
    {
        return 1e-3;
    }
}
