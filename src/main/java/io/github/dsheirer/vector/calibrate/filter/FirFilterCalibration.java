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

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter128Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter256Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter512Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilter64Bit;
import io.github.dsheirer.dsp.filter.fir.real.VectorRealFIRFilterDefaultBit;
import io.github.dsheirer.dsp.window.WindowType;
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
public class FirFilterCalibration extends Calibration
{
    private static final VectorSpecies<Float> VECTOR_SPECIES = FloatVector.SPECIES_PREFERRED;
    private static final int BUFFER_SIZE = 2048;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;

    private IRealFilter mScalar;
    private IRealFilter mVectorPreferred;
    private IRealFilter mVector512;
    private IRealFilter mVector256;
    private IRealFilter mVector128;
    private IRealFilter mVector64;


    /**
     * Constructs an instance
     */
    public FirFilterCalibration()
    {
        super(CalibrationType.FILTER_FIR);

        float[] coefficients;

        try
        {
            coefficients = FilterFactory.getSinc(0.25, 31, WindowType.BLACKMAN);
            mScalar = new RealFIRFilter(coefficients);
            mVectorPreferred = new VectorRealFIRFilterDefaultBit(coefficients);
            mVector64 = new VectorRealFIRFilter64Bit(coefficients);
            mVector128 = new VectorRealFIRFilter128Bit(coefficients);
            mVector256 = new VectorRealFIRFilter256Bit(coefficients);
            mVector512 = new VectorRealFIRFilter512Bit(coefficients);
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Error creating sinc filter for real FIR filter calibration");
        }
    }

    /**
     * Performs calibration to determine optimal (Scalar vs Vector) operation type.
     * @throws CalibrationException
     */
    @Override public void calibrate() throws CalibrationException
    {
        if(mScalar == null)
        {
            return;
        }

        float[] samples = getFloatSamples(BUFFER_SIZE);

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testScalar(samples);
            scalarMean.increment(score);
        }

        mLog.info("FIR FILTER WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vectorPreferredMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVectorPreferred(samples);
            vectorPreferredMean.increment(score);
        }

        mLog.info("FIR FILTER WARMUP - VECTOR PREFERRED: " + DECIMAL_FORMAT.format(vectorPreferredMean.getResult()));

        Mean vector512Mean = new Mean();

        if(VECTOR_SPECIES.length() >= 16)
        {
            for(int x = 0; x < WARMUP_ITERATIONS; x++)
            {
                long score = testVector512(samples);
                vector512Mean.increment(score);
            }

            mLog.info("FIR FILTER WARMUP - VECTOR 512: " + DECIMAL_FORMAT.format(vector512Mean.getResult()));
        }

        Mean vector256Mean = new Mean();

        if(VECTOR_SPECIES.length() >= 8)
        {
            for(int x = 0; x < WARMUP_ITERATIONS; x++)
            {
                long score = testVector256(samples);
                vector256Mean.increment(score);
            }

            mLog.info("FIR FILTER WARMUP - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));
        }

        Mean vector128Mean = new Mean();

        if(VECTOR_SPECIES.length() >= 4)
        {
            for(int x = 0; x < WARMUP_ITERATIONS; x++)
            {
                long score = testVector128(samples);
                vector128Mean.increment(score);
            }

            mLog.info("FIR FILTER WARMUP - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));
        }

        Mean vector64Mean = new Mean();

        if(VECTOR_SPECIES.length() >= 2)
        {
            for(int x = 0; x < WARMUP_ITERATIONS; x++)
            {
                long score = testVector64(samples);
                vector64Mean.increment(score);
            }

            mLog.info("FIR FILTER WARMUP - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));
        }

        //Test starts ...
        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testScalar(samples);
            scalarMean.increment(score);
        }

        mLog.info("FIR FILTER - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        double bestScore = scalarMean.getResult();
        setImplementation(Implementation.SCALAR);

        vectorPreferredMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVectorPreferred(samples);
            vectorPreferredMean.increment(score);
        }

        mLog.info("FIR FILTER - VECTOR PREFERRED: " + DECIMAL_FORMAT.format(vectorPreferredMean.getResult()));

        if(vectorPreferredMean.getResult() > bestScore)
        {
            bestScore = vectorPreferredMean.getResult();
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        if(VECTOR_SPECIES.length() >= 16)
        {
            vector512Mean.clear();

            for(int x = 0; x < TEST_ITERATIONS; x++)
            {
                long score = testVector512(samples);
                vector512Mean.increment(score);
            }

            mLog.info("FIR FILTER - VECTOR 512: " + DECIMAL_FORMAT.format(vector512Mean.getResult()));

            if(vector512Mean.getResult() > bestScore)
            {
                bestScore = vector512Mean.getResult();
                setImplementation(Implementation.VECTOR_SIMD_512);
            }
        }

        if(VECTOR_SPECIES.length() >= 8)
        {
            vector256Mean.clear();

            for(int x = 0; x < TEST_ITERATIONS; x++)
            {
                long score = testVector256(samples);
                vector256Mean.increment(score);
            }

            mLog.info("FIR FILTER - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));

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
                long score = testVector128(samples);
                vector128Mean.increment(score);
            }

            mLog.info("FIR FILTER - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));

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
                long score = testVector64(samples);
                vector64Mean.increment(score);
            }

            mLog.info("FIR FILTER - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));

            if(vector64Mean.getResult() > bestScore)
            {
                setImplementation(Implementation.VECTOR_SIMD_64);
            }
        }

        mLog.info("FIR FILTER - SET OPTIMAL IMPLEMENTATION TO: " + getImplementation());
    }

    private long testScalar(float[] samples)
    {
        double accumulator = 0.0f;
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

    private long testVector64(float[] samples)
    {
        double accumulator = 0.0f;

        long start = System.currentTimeMillis();
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] filtered = mVector64.filter(samples);
            accumulator += filtered[0];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector128(float[] samples)
    {
        double accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] filtered = mVector128.filter(samples);
            accumulator += filtered[0];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector256(float[] samples)
    {
        double accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] filtered = mVector256.filter(samples);
            accumulator += filtered[0];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector512(float[] samples)
    {
        double accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] filtered = mVector512.filter(samples);
            accumulator += filtered[0];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVectorPreferred(float[] samples)
    {
        double accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] filtered = mVectorPreferred.filter(samples);
            accumulator += filtered[0];
            count++;
        }

        return count + (long)(accumulator * 0);
    }
}
