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

package io.github.dsheirer.vector.calibrate.mixer;

import io.github.dsheirer.dsp.mixer.ComplexMixer;
import io.github.dsheirer.dsp.mixer.ComplexMixerFactory;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.List;

/**
 * Complex mixer calibration
 */
public class ComplexMixerCalibration extends Calibration
{
    private static final double FREQUENCY = 2.0;
    private static final double SAMPLE_RATE = 10.0;
    private static final int BUFFER_SIZE = 2048;
    private final float[] mI;
    private final float[] mQ;

    /**
     * Constructs an instance
     */
    public ComplexMixerCalibration()
    {
        super(CalibrationType.MIXER_COMPLEX);
        mI = getFloatSamples(BUFFER_SIZE);
        mQ = getFloatSamples(BUFFER_SIZE);
    }

    @Override protected List<Candidate> getCandidates()
    {
        return List.of(
            candidate(Implementation.SCALAR, () -> {
                ComplexMixer operation = ComplexMixerFactory.getMixer(Implementation.SCALAR, FREQUENCY, SAMPLE_RATE);
                return () -> operation.mix(mI, mQ, 0L);
            }),
            candidate(Implementation.VECTOR_SIMD_PREFERRED, () -> {
                ComplexMixer operation = ComplexMixerFactory.getMixer(
                    Implementation.VECTOR_SIMD_PREFERRED, FREQUENCY, SAMPLE_RATE);
                return () -> operation.mix(mI, mQ, 0L);
            }));
    }
}
