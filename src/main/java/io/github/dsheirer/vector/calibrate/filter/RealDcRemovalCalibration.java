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

package io.github.dsheirer.vector.calibrate.filter;

import io.github.dsheirer.dsp.filter.dc.IDcRemovalFilter;
import io.github.dsheirer.dsp.filter.dc.ScalarDcRemovalFilter;
import io.github.dsheirer.dsp.filter.dc.VectorDcRemovalFilter;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Determines optimal DC removal implementation
 */
public class RealDcRemovalCalibration extends Calibration
{
    private static final float GAIN = 0.15f;
    private static final int BUFFER_SIZE = 8192;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int TEST_ITERATIONS = 5;
    private static final int WARMUP_ITERATIONS = 5;

    private IDcRemovalFilter mScalar = new ScalarDcRemovalFilter(GAIN);
    private IDcRemovalFilter mVector = new VectorDcRemovalFilter(GAIN);

    /**
     * Constructs an instance
     */
    public RealDcRemovalCalibration()
    {
        super(CalibrationType.DC_REMOVAL_REAL);
    }

    @Override
    public void calibrate() throws CalibrationException
    {
        float[] samples = getFloatSamples(BUFFER_SIZE);

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testScalar(samples);
            scalarMean.increment(score);
        }

        mLog.info("REAL DC REMOVAL WARMUP - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vectorMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector(samples);
            vectorMean.increment(score);
        }

        mLog.info("REAL DC REMOVAL WARMUP - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testScalar(samples);
            scalarMean.increment(score);
        }

        mLog.info("REAL DC REMOVAL - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));

        vectorMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector(samples);
            vectorMean.increment(score);
        }

        mLog.info("REAL DC REMOVAL - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));


        if(scalarMean.getResult() > vectorMean.getResult())
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("REAL DC REMOVAL - SET OPTIMAL IMPLEMENTATION TO: " + getImplementation());
    }

    private long testScalar(float[] samples)
    {
        double accumulator = 0.0;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] filtered = mScalar.filter(samples);
            accumulator += filtered[0];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector(float[] samples)
    {
        double accumulator = 0.0;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] filtered = mVector.filter(samples);
            accumulator += filtered[0];
            count++;
        }

        return count + (long)(accumulator * 0);
    }
}
