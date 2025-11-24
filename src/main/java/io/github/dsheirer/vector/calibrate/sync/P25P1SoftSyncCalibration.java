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

package io.github.dsheirer.vector.calibrate.sync;

import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetector;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorScalar;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorVector128;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorVector256;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorVector512;
import io.github.dsheirer.module.decode.p25.phase1.sync.P25P1SoftSyncDetectorVector64;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * P25 Phase 1 Soft Sync Detector calibration
 */
public class P25P1SoftSyncCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;
    private final P25P1SoftSyncDetector mScalarDetector = new P25P1SoftSyncDetectorScalar();
    private final P25P1SoftSyncDetector mVectorDetector64 = new P25P1SoftSyncDetectorVector64();
    private final P25P1SoftSyncDetector mVectorDetector128 = new P25P1SoftSyncDetectorVector128();
    private final P25P1SoftSyncDetector mVectorDetector256 = new P25P1SoftSyncDetectorVector256();
    private final P25P1SoftSyncDetector mVectorDetector512 = new P25P1SoftSyncDetectorVector512();

    /**
     * Constructs an instance
     */
    public P25P1SoftSyncCalibration()
    {
        super(CalibrationType.P25P1_SOFT_SYNC_DETECTOR);
    }

    @Override public void calibrate() throws CalibrationException
    {
        float[] samples = getFloatSamples(BUFFER_SIZE);

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testScalar(samples);
            scalarMean.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vectorMean64 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector64(samples);
            vectorMean64.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR WARMUP - VECTOR 64: " + DECIMAL_FORMAT.format(vectorMean64.getResult()));

        Mean vectorMean128 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector128(samples);
            vectorMean128.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR WARMUP - VECTOR 128: " + DECIMAL_FORMAT.format(vectorMean128.getResult()));

        Mean vectorMean256 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector256(samples);
            vectorMean256.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR WARMUP - VECTOR 256: " + DECIMAL_FORMAT.format(vectorMean256.getResult()));

        Mean vectorMean512 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector512(samples);
            vectorMean512.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR WARMUP - VECTOR 512: " + DECIMAL_FORMAT.format(vectorMean512.getResult()));

        //Start tests
        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testScalar(samples);
            scalarMean.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        vectorMean64.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector64(samples);
            vectorMean64.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR - VECTOR 64: " + DECIMAL_FORMAT.format(vectorMean64.getResult()));

        vectorMean128.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector128(samples);
            vectorMean128.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR - VECTOR 128: " + DECIMAL_FORMAT.format(vectorMean128.getResult()));

        vectorMean256.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector256(samples);
            vectorMean256.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR - VECTOR 256: " + DECIMAL_FORMAT.format(vectorMean256.getResult()));

        vectorMean512.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector512(samples);
            vectorMean512.increment(score);
        }

        mLog.info("P25P1 SOFT SYNC DETECTOR - VECTOR 512: " + DECIMAL_FORMAT.format(vectorMean512.getResult()));

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

        mLog.info("P25P1 SOFT SYNC DETECTOR - SET OPTIMAL IMPLEMENTATION TO:" + getImplementation());
    }

    private long testScalar(float[] samples)
    {
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            for(float sample : samples)
            {
                mScalarDetector.process(sample);
            }
            count++;
        }

        return count;
    }

    private long testVector64(float[] samples)
    {
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            for(float sample : samples)
            {
                mVectorDetector64.process(sample);
            }
            count++;
        }

        return count;
    }

    private long testVector128(float[] samples)
    {
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            for(float sample : samples)
            {
                mVectorDetector128.process(sample);
            }
            count++;
        }

        return count;
    }

    private long testVector256(float[] samples)
    {
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            for(float sample : samples)
            {
                mVectorDetector256.process(sample);
            }
            count++;
        }

        return count;
    }

    private long testVector512(float[] samples)
    {
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            for(float sample : samples)
            {
                mVectorDetector512.process(sample);
            }
            count++;
        }

        return count;
    }

    public static void main(String[] args)
    {
        P25P1SoftSyncCalibration calibration = new P25P1SoftSyncCalibration();

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
