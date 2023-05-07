/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.dsp.am;

import io.github.dsheirer.dsp.magnitude.IMagnitudeCalculator;
import io.github.dsheirer.dsp.magnitude.MagnitudeFactory;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calibrates AM demodulator options
 */
public class AmDemodulatorCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;
    private IMagnitudeCalculator mMagnitudeCalculator = MagnitudeFactory.getMagnitudeCalculator();
    private IAmDemodulator mScalarDemodulator = new ScalarAMDemodulator(500.0f);
    private IAmDemodulator mVectorDemodulator64 = new VectorAMDemodulator64(500.0f);
    private IAmDemodulator mVectorDemodulator128 = new VectorAMDemodulator128(500.0f);
    private IAmDemodulator mVectorAMDemodulator256 = new VectorAMDemodulator256(500.0f);
    private IAmDemodulator mVectorAMDemodulator512 = new VectorAMDemodulator512(500.0f);


    /**
     * Constructs an instance
     */
    public AmDemodulatorCalibration()
    {
        super(CalibrationType.AM_DEMODULATOR);
    }

    @Override public void calibrate() throws CalibrationException
    {
        float[] magnitude = getFloatSamples(BUFFER_SIZE);

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testScalar(magnitude);
            scalarMean.increment(score);
        }

        mLog.info("AM DEMODULATOR WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vector64Mean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector64(magnitude);
            vector64Mean.increment(score);
        }

        mLog.info("AM DEMODULATOR WARMUP - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));

        Mean vector128Mean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector128(magnitude);
            vector128Mean.increment(score);
        }

        mLog.info("AM DEMODULATOR WARMUP - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));

        Mean vector256Mean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector256(magnitude);
            vector256Mean.increment(score);
        }

        mLog.info("AM DEMODULATOR WARMUP - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));

        Mean vector512Mean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector512(magnitude);
            vector512Mean.increment(score);
        }

        mLog.info("AM DEMODULATOR WARMUP - VECTOR 512: " + DECIMAL_FORMAT.format(vector512Mean.getResult()));

        //Start tests
        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testScalar(magnitude);
            scalarMean.increment(score);
        }

        mLog.info("AM DEMODULATOR - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        vector64Mean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector64(magnitude);
            vector64Mean.increment(score);
        }

        mLog.info("AM DEMODULATOR - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));

        vector128Mean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector128(magnitude);
            vector128Mean.increment(score);
        }

        mLog.info("AM DEMODULATOR - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));

        vector256Mean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector256(magnitude);
            vector256Mean.increment(score);
        }

        mLog.info("AM DEMODULATOR - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));

        vector512Mean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector512(magnitude);
            vector512Mean.increment(score);
        }

        mLog.info("AM DEMODULATOR - VECTOR 512: " + DECIMAL_FORMAT.format(vector512Mean.getResult()));

        Implementation optimal = Implementation.SCALAR;
        double optimalResult = scalarMean.getResult();

        if(vector64Mean.getResult() > optimalResult)
        {
            optimal = Implementation.VECTOR_SIMD_64;
            optimalResult = vector64Mean.getResult();
        }

        if(vector128Mean.getResult() > optimalResult)
        {
            optimal = Implementation.VECTOR_SIMD_128;
            optimalResult = vector128Mean.getResult();
        }

        if(vector256Mean.getResult() > optimalResult)
        {
            optimal = Implementation.VECTOR_SIMD_256;
            optimalResult = vector256Mean.getResult();
        }

        if(vector512Mean.getResult() > optimalResult)
        {
            optimal = Implementation.VECTOR_SIMD_512;
        }

        setImplementation(optimal);

        mLog.info("AM DEMODULATOR - SET OPTIMAL IMPLEMENTATION TO:" + getImplementation());
    }

    private long testScalar(float[] magnitude)
    {

        double accumulator = 0.0;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mScalarDemodulator.demodulateMagnitude(magnitude);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector64(float[] magnitude)
    {
        double accumulator = 0.0;
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mVectorDemodulator64.demodulateMagnitude(magnitude);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector128(float[] magnitude)
    {
        double accumulator = 0.0;
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mVectorDemodulator128.demodulateMagnitude(magnitude);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector256(float[] magnitude)
    {
        double accumulator = 0.0;
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mVectorAMDemodulator256.demodulateMagnitude(magnitude);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector512(float[] magnitude)
    {
        double accumulator = 0.0;
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mVectorAMDemodulator512.demodulateMagnitude(magnitude);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    public static void main(String[] args)
    {
        AmDemodulatorCalibration calibration = new AmDemodulatorCalibration();

        try
        {
            calibration.calibrate();
        }
        catch(Exception e)
        {
            mLog.error("Error during calibration", e);
        }
    }
}
