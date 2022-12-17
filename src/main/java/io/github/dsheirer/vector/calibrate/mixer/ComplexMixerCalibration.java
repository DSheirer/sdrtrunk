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

package io.github.dsheirer.vector.calibrate.mixer;

import io.github.dsheirer.dsp.mixer.ScalarComplexMixer;
import io.github.dsheirer.dsp.mixer.VectorComplexMixer;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calibrates the complex mixer implementations to determine the optimal instance.
 */
public class ComplexMixerCalibration extends Calibration
{
    private static final double FREQUENCY = 2.0;
    private static final double SAMPLE_RATE = 10.0;
    private static final int SAMPLE_SIZE = 2048;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;
    private ScalarComplexMixer mScalarComplexMixer = new ScalarComplexMixer(FREQUENCY, SAMPLE_RATE);
    private VectorComplexMixer mVectorComplexMixer = new VectorComplexMixer(FREQUENCY, SAMPLE_RATE);

    /**
     * Constructs an instance
     */
    public ComplexMixerCalibration()
    {
        super(CalibrationType.MIXER_COMPLEX);
    }

    @Override public void calibrate() throws CalibrationException
    {
        float[] i = getFloatSamples(SAMPLE_SIZE);
        float[] q = getFloatSamples(SAMPLE_SIZE);

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testScalar(i, q);
            scalarMean.increment(score);
        }

        mLog.info("COMPLEX MIXER WARMUP - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vectorMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector(i, q);
            vectorMean.increment(score);
        }

        mLog.info("COMPLEX MIXER WARMUP - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testScalar(i, q);
            scalarMean.increment(score);
        }

        mLog.info("COMPLEX MIXER - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        vectorMean.clear();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector(i, q);
            vectorMean.increment(score);
        }

        mLog.info("COMPLEX MIXER - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        if(scalarMean.getResult() > vectorMean.getResult())
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("COMPLEX MIXER - IMPLEMENTATION SET TO:" + getImplementation());
    }

    private long testScalar(float[] i, float[] q)
    {
        long start = System.currentTimeMillis();
        double accumulator = 0.0;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            ComplexSamples mixed = mScalarComplexMixer.mix(i, q, start);
            accumulator += mixed.i()[2];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector(float[] i, float[] q)
    {
        long start = System.currentTimeMillis();
        double accumulator = 0.0;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            ComplexSamples mixed = mVectorComplexMixer.mix(i, q, start);
            accumulator += mixed.i()[2];
            count++;
        }

        return count + (long)(accumulator * 0);
    }
}
