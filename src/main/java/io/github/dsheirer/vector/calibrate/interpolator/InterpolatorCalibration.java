/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.vector.calibrate.interpolator;

import io.github.dsheirer.dsp.filter.interpolator.Interpolator;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorScalar;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorVector128;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorVector256;
import io.github.dsheirer.dsp.filter.interpolator.InterpolatorVector64;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calibration plugin for FIR filters
 */
public class InterpolatorCalibration extends Calibration
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;
    private static final int BUFFER_SIZE = 8;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;

    private Interpolator mScalar = new InterpolatorScalar();
    private Interpolator mVector256 = new InterpolatorVector256();
    private Interpolator mVector128 = new InterpolatorVector128();
    private Interpolator mVector64 = new InterpolatorVector64();

    /**
     * Constructs an instance
     */
    public InterpolatorCalibration()
    {
        super(CalibrationType.INTERPOLATOR);
    }

    /**
     * Performs calibration to determine optimal (Scalar vs Vector) operation type.
     * @throws CalibrationException
     */
    @Override public void calibrate() throws CalibrationException
    {
        float[] samples = getFloatSamples(BUFFER_SIZE);
        float[] interpolationPoints = getPositiveFloatSamples(2048);

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testScalar(samples, interpolationPoints);
            scalarMean.increment(score);
        }

        mLog.info("INTERPOLATOR WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vector256Mean = new Mean();

        if(VECTOR_SPECIES.length() >= 8)
        {
            for(int x = 0; x < WARMUP_ITERATIONS; x++)
            {
                long score = testVector256(samples, interpolationPoints);
                vector256Mean.increment(score);
            }

            mLog.info("INTERPOLATOR WARMUP - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));
        }

        Mean vector128Mean = new Mean();

        if(VECTOR_SPECIES.length() >= 4)
        {
            for(int x = 0; x < WARMUP_ITERATIONS; x++)
            {
                long score = testVector128(samples, interpolationPoints);
                vector128Mean.increment(score);
            }

            mLog.info("INTERPOLATOR WARMUP - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));
        }

        Mean vector64Mean = new Mean();

        if(VECTOR_SPECIES.length() >= 2)
        {
            for(int x = 0; x < WARMUP_ITERATIONS; x++)
            {
                long score = testVector64(samples, interpolationPoints);
                vector64Mean.increment(score);
            }

            mLog.info("INTERPOLATOR WARMUP - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));
        }

        //Test starts ...
        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testScalar(samples, interpolationPoints);
            scalarMean.increment(score);
        }

        mLog.info("INTERPOLATOR - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        double bestScore = scalarMean.getResult();
        setImplementation(Implementation.SCALAR);

        if(VECTOR_SPECIES.length() >= 8)
        {
            vector256Mean.clear();

            for(int x = 0; x < TEST_ITERATIONS; x++)
            {
                long score = testVector256(samples, interpolationPoints);
                vector256Mean.increment(score);
            }

            mLog.info("INTERPOLATOR - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));

            if(vector256Mean.getResult() > bestScore)
            {
                bestScore = vector256Mean.getResult();
                setImplementation(Implementation.VECTOR_SIMD_256);
            }
        }

        if(VECTOR_SPECIES.length() >= 4)
        {
            vector128Mean.clear();

            for(int x = 0; x < TEST_ITERATIONS; x++)
            {
                long score = testVector128(samples, interpolationPoints);
                vector128Mean.increment(score);
            }

            mLog.info("INTERPOLATOR - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));

            if(vector128Mean.getResult() > bestScore)
            {
                bestScore = vector128Mean.getResult();
                setImplementation(Implementation.VECTOR_SIMD_128);
            }
        }

        if(VECTOR_SPECIES.length() >= 2)
        {
            vector64Mean.clear();

            for(int x = 0; x < TEST_ITERATIONS; x++)
            {
                long score = testVector64(samples, interpolationPoints);
                vector64Mean.increment(score);
            }

            mLog.info("INTERPOLATOR - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));

            if(vector64Mean.getResult() > bestScore)
            {
                setImplementation(Implementation.VECTOR_SIMD_64);
            }
        }

        mLog.info("INTERPOLATOR - SET OPTIMAL IMPLEMENTATION TO: " + getImplementation());
    }

    private long testScalar(float[] samples, float[] interpolationPoints)
    {
        double accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            for(int x = 0; x < interpolationPoints.length; x++)
            {
                accumulator += mScalar.filter(samples, 0, interpolationPoints[x]);
            }

            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector64(float[] samples, float[] interpolationPoints)
    {
        double accumulator = 0.0f;

        long start = System.currentTimeMillis();
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            for(int x = 0; x < interpolationPoints.length; x++)
            {
                accumulator += mVector64.filter(samples, 0, interpolationPoints[x]);
            }

            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector128(float[] samples, float[] interpolationPoints)
    {
        double accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            for(int x = 0; x < interpolationPoints.length; x++)
            {
                accumulator += mVector128.filter(samples, 0, interpolationPoints[x]);
            }

            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector256(float[] samples, float[] interpolationPoints)
    {
        double accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            for(int x = 0; x < interpolationPoints.length; x++)
            {
                accumulator += mVector256.filter(samples, 0, interpolationPoints[x]);
            }
            count++;
        }

        return count + (long)(accumulator * 0);
    }
}
