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

package io.github.dsheirer.vector.calibrate.demodulator;

import io.github.dsheirer.dsp.fm.IDemodulator;
import io.github.dsheirer.dsp.fm.ScalarFMDemodulator;
import io.github.dsheirer.dsp.fm.VectorFMDemodulator128;
import io.github.dsheirer.dsp.fm.VectorFMDemodulator256;
import io.github.dsheirer.dsp.fm.VectorFMDemodulator512;
import io.github.dsheirer.dsp.fm.VectorFMDemodulator64;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calibrates FM demodulator options
 */
public class FmDemodulatorCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;
    private IDemodulator mScalarDemodulator = new ScalarFMDemodulator();
    private IDemodulator mVectorDemodulator64 = new VectorFMDemodulator64();
    private IDemodulator mVectorDemodulator128 = new VectorFMDemodulator128();
    private IDemodulator mVectorDemodulator256 = new VectorFMDemodulator256();
    private IDemodulator mVectorDemodulator512 = new VectorFMDemodulator512();

    /**
     * Constructs an instance
     */
    public FmDemodulatorCalibration()
    {
        super(CalibrationType.FM_DEMODULATOR);
    }

    @Override public void calibrate() throws CalibrationException
    {
        float[] i = getFloatSamples(BUFFER_SIZE);
        float[] q = getFloatSamples(BUFFER_SIZE);

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testScalar(i, q);
            scalarMean.increment(score);
        }

        mLog.info("FM DEMODULATOR WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vectorMean64 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector64(i, q);
            vectorMean64.increment(score);
        }

        mLog.info("FM DEMODULATOR WARMUP - VECTOR 64: " + DECIMAL_FORMAT.format(vectorMean64.getResult()));

        Mean vectorMean128 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector128(i, q);
            vectorMean128.increment(score);
        }

        mLog.info("FM DEMODULATOR WARMUP - VECTOR 128: " + DECIMAL_FORMAT.format(vectorMean128.getResult()));

        Mean vectorMean256 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector256(i, q);
            vectorMean256.increment(score);
        }

        mLog.info("FM DEMODULATOR WARMUP - VECTOR 256: " + DECIMAL_FORMAT.format(vectorMean256.getResult()));

        Mean vectorMean512 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector512(i, q);
            vectorMean512.increment(score);
        }

        mLog.info("FM DEMODULATOR WARMUP - VECTOR 512: " + DECIMAL_FORMAT.format(vectorMean512.getResult()));

        //Start tests
        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testScalar(i, q);
            scalarMean.increment(score);
        }

        mLog.info("FM DEMODULATOR - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        vectorMean64.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector64(i, q);
            vectorMean64.increment(score);
        }

        mLog.info("FM DEMODULATOR - VECTOR 64: " + DECIMAL_FORMAT.format(vectorMean64.getResult()));

        vectorMean128.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector128(i, q);
            vectorMean128.increment(score);
        }

        mLog.info("FM DEMODULATOR - VECTOR 128: " + DECIMAL_FORMAT.format(vectorMean128.getResult()));

        vectorMean256.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector256(i, q);
            vectorMean256.increment(score);
        }

        mLog.info("FM DEMODULATOR - VECTOR 256: " + DECIMAL_FORMAT.format(vectorMean256.getResult()));

        vectorMean512.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector512(i, q);
            vectorMean512.increment(score);
        }

        mLog.info("FM DEMODULATOR - VECTOR 512: " + DECIMAL_FORMAT.format(vectorMean512.getResult()));

        Implementation best = Implementation.SCALAR;
        double bestScore = scalarMean.getResult();

        if(vectorMean64.getResult() > bestScore)
        {
            bestScore = vectorMean64.getResult();
            best = Implementation.VECTOR_SIMD_64;
        }

        if(vectorMean128.getResult() > bestScore)
        {
            bestScore = vectorMean128.getResult();
            best = Implementation.VECTOR_SIMD_128;
        }

        if(vectorMean256.getResult() > bestScore)
        {
            bestScore = vectorMean256.getResult();
            best = Implementation.VECTOR_SIMD_256;
        }

        if(vectorMean512.getResult() > bestScore)
        {
            best = Implementation.VECTOR_SIMD_512;
        }

        setImplementation(best);

        mLog.info("FM DEMODULATOR - SET OPTIMAL IMPLEMENTATION TO:" + getImplementation());
    }

    private long testScalar(float[] i, float[] q)
    {
        double accumulator = 0.0;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mScalarDemodulator.demodulate(i, q);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector64(float[] i, float[] q)
    {
        double accumulator = 0.0;
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mVectorDemodulator64.demodulate(i, q);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector128(float[] i, float[] q)
    {
        double accumulator = 0.0;
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mVectorDemodulator128.demodulate(i, q);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector256(float[] i, float[] q)
    {
        double accumulator = 0.0;
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mVectorDemodulator256.demodulate(i, q);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    private long testVector512(float[] i, float[] q)
    {
        double accumulator = 0.0;
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            float[] demodulated = mVectorDemodulator512.demodulate(i, q);
            accumulator += demodulated[1];
            count++;
        }

        return count + (long)(accumulator * 0);
    }

    public static void main(String[] args)
    {
        FmDemodulatorCalibration calibration = new FmDemodulatorCalibration();

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
