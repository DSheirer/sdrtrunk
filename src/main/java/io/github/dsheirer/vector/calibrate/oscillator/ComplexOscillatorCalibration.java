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

package io.github.dsheirer.vector.calibrate.oscillator;

import io.github.dsheirer.dsp.oscillator.IComplexOscillator;
import io.github.dsheirer.dsp.oscillator.ScalarComplexOscillator;
import io.github.dsheirer.dsp.oscillator.VectorComplexOscillator;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calibration plugin for complex oscillators
 */
public class ComplexOscillatorCalibration extends Calibration
{
    private static final double FREQUENCY = 5.0d;
    private static final double SAMPLE_RATE = 100.0d;
    private static final int BUFFER_SIZE = 2048;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;

    private IComplexOscillator mScalarOscillator = new ScalarComplexOscillator(FREQUENCY, SAMPLE_RATE);
    private IComplexOscillator mVectorOscillator = new VectorComplexOscillator(FREQUENCY, SAMPLE_RATE);


    /**
     * Constructs an instance
     */
    public ComplexOscillatorCalibration()
    {
        super(CalibrationType.OSCILLATOR_COMPLEX);
    }

    /**
     * Performs calibration to determine optimal (Scalar vs Vector) operation type.
     * @throws CalibrationException
     */
    @Override public void calibrate() throws CalibrationException
    {
        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testScalar();
            scalarMean.increment(score);
        }

        mLog.info("COMPLEX OSCILLATOR WARMUP - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vectorMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector();
            vectorMean.increment(score);
        }

        mLog.info("COMPLEX OSCILLATOR WARMUP - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testScalar();
            scalarMean.increment(score);
        }

        mLog.info("COMPLEX OSCILLATOR - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        vectorMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector();
            vectorMean.increment(score);
        }

        mLog.info("COMPLEX OSCILLATOR - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        if(scalarMean.getResult() > vectorMean.getResult())
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("COMPLEX OSCILLATOR - SETTING OPTIMAL OPERATION TO:" + getImplementation());
    }

    /**
     * Calculates the time duration needed to generate the sample buffers of the specified size and iteration count
     * @return time duration in milliseconds
     */
    private long testScalar()
    {
        float accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] generated = mScalarOscillator.generate(BUFFER_SIZE);
            accumulator += generated[0];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    /**
     * Calculates the time duration to process the sample buffer with the filter coefficients.
     * @return time duration in milliseconds
     */
    private long testVector()
    {
        float accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] generated = mVectorOscillator.generate(BUFFER_SIZE);
            accumulator += generated[0];
            count++;
        }

        return count + (long)(accumulator * 0);
    }
}
