/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.vector.calibrate.sample;

import io.github.dsheirer.buffer.sample.SampleBufferIterator;
import io.github.dsheirer.buffer.sample.SampleBufferIteratorScalar;
import io.github.dsheirer.buffer.sample.SampleBufferIteratorVector128Bits;
import io.github.dsheirer.buffer.sample.SampleBufferIteratorVector256Bits;
import io.github.dsheirer.buffer.sample.SampleBufferIteratorVector512Bits;
import io.github.dsheirer.buffer.sample.SampleBufferIteratorVector64Bits;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import jdk.incubator.vector.FloatVector;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calculates optimal implementation (SCALAR vs VECTOR) for non-interleaved sample native buffers.
 */
public class UnpackedSampleConverterCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 131072;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;

    /**
     * Constructs an instance
     */
    public UnpackedSampleConverterCalibration()
    {
        super(CalibrationType.SAMPLE_UNPACKED_ITERATOR);
    }

    @Override
    public void calibrate() throws CalibrationException
    {
        short[] samples = getShortSamples(BUFFER_SIZE);
        short[] residualI = getShortSamples(SampleBufferIterator.I_OVERLAP);
        short[] residualQ = getShortSamples(SampleBufferIterator.Q_OVERLAP);

        mLog.info("UNPACKED SAMPLE CONVERTER - VECTOR SIMD LANES PREFERRED: " + FloatVector.SPECIES_PREFERRED.length());

        //Warm-Up Phase ....
        Mean scalarMean = new Mean();

        for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
        {
            long score = testScalar(samples, residualI, residualQ);
            scalarMean.increment(score);
        }

        mLog.info("UNPACKED SAMPLE CONVERTER WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        switch(FloatVector.SPECIES_PREFERRED.length())
        {
            //Deliberate fall-through of each case statement so that we can test the largest
            //SIMD lane width supported by hardware down to the smallest SIMD lane width.
            case 16:
            {
                Mean vector512Mean = new Mean();
                for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
                {
                    long score = testVector512(samples, residualI, residualQ);
                    vector512Mean.increment(score);
                }

                mLog.info("UNPACKED SAMPLE CONVERTER WARMUP - VECTOR 512: " + DECIMAL_FORMAT.format(vector512Mean.getResult()));
            }
            case 8:
            {
                Mean vector256Mean = new Mean();
                for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
                {
                    long score = testVector256(samples, residualI, residualQ);
                    vector256Mean.increment(score);
                }

                mLog.info("UNPACKED SAMPLE CONVERTER WARMUP - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));
            }
            case 4:
            {
                Mean vector128Mean = new Mean();
                for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
                {
                    long score = testVector128(samples, residualI, residualQ);
                    vector128Mean.increment(score);
                }

                mLog.info("UNPACKED SAMPLE CONVERTER WARMUP - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));
            }
            case 2:
            {
                Mean vector64Mean = new Mean();
                for(int warmup = 0; warmup < WARMUP_ITERATIONS; warmup++)
                {
                    long score = testVector64(samples, residualI, residualQ);
                    vector64Mean.increment(score);
                }

                mLog.info("UNPACKED SAMPLE CONVERTER WARMUP - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));
            }
        }

        //Test Phase ....
        scalarMean.clear();

        for(int test = 0; test < TEST_ITERATIONS; test++)
        {
            long score = testScalar(samples, residualI, residualQ);
            scalarMean.increment(score);
        }

        double bestScore = scalarMean.getResult();
        setImplementation(Implementation.SCALAR);
        mLog.info("UNPACKED SAMPLE CONVERTER - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        switch(FloatVector.SPECIES_PREFERRED.length())
        {
            //Deliberate fall-through of each case statement so that we can test the largest
            //SIMD lane width supported by hardware down to the smallest SIMD lane width.
            case 16:
            {
                Mean vector512Mean = new Mean();
                for(int test = 0; test < TEST_ITERATIONS; test++)
                {
                    long score = testVector512(samples, residualI, residualQ);
                    vector512Mean.increment(score);
                }

                mLog.info("UNPACKED SAMPLE CONVERTER - VECTOR 512: " + DECIMAL_FORMAT.format(vector512Mean.getResult()));
                if(vector512Mean.getResult() > bestScore)
                {
                    bestScore = vector512Mean.getResult();
                    setImplementation(Implementation.VECTOR_SIMD_512);
                }
            }
            case 8:
            {
                Mean vector256Mean = new Mean();
                for(int test = 0; test < TEST_ITERATIONS; test++)
                {
                    long score = testVector256(samples, residualI, residualQ);
                    vector256Mean.increment(score);
                }

                mLog.info("UNPACKED SAMPLE CONVERTER - VECTOR 256: " + DECIMAL_FORMAT.format(vector256Mean.getResult()));
                if(vector256Mean.getResult() > bestScore)
                {
                    bestScore = vector256Mean.getResult();
                    setImplementation(Implementation.VECTOR_SIMD_256);
                }
            }
            case 4:
            {
                Mean vector128Mean = new Mean();
                for(int test = 0; test < TEST_ITERATIONS; test++)
                {
                    long score = testVector128(samples, residualI, residualQ);
                    vector128Mean.increment(score);
                }

                mLog.info("UNPACKED SAMPLE CONVERTER - VECTOR 128: " + DECIMAL_FORMAT.format(vector128Mean.getResult()));
                if(vector128Mean.getResult() > bestScore)
                {
                    bestScore = vector128Mean.getResult();
                    setImplementation(Implementation.VECTOR_SIMD_128);
                }
            }
            case 2:
            {
                Mean vector64Mean = new Mean();
                for(int test = 0; test < TEST_ITERATIONS; test++)
                {
                    long score = testVector64(samples, residualI, residualQ);
                    vector64Mean.increment(score);
                }

                mLog.info("UNPACKED SAMPLE CONVERTER - VECTOR 64: " + DECIMAL_FORMAT.format(vector64Mean.getResult()));
                if(vector64Mean.getResult() > bestScore)
                {
                    setImplementation(Implementation.VECTOR_SIMD_64);
                }
            }
        }

        mLog.info("UNPACKED SAMPLE CONVERTER - SET OPTIMAL IMPLEMENTATION TO: " + getImplementation());
    }

    private long testScalar(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            SampleBufferIteratorScalar iterator = new SampleBufferIteratorScalar(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().i()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }

    private long testVector64(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            SampleBufferIteratorVector64Bits iterator = new SampleBufferIteratorVector64Bits(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().i()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }

    private long testVector128(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;
        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            SampleBufferIteratorVector128Bits iterator = new SampleBufferIteratorVector128Bits(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().i()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }

    private long testVector256(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            SampleBufferIteratorVector256Bits iterator =
                    new SampleBufferIteratorVector256Bits(samples, residualI, residualQ, 0.0f,
                            System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().i()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }

    private long testVector512(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            SampleBufferIteratorVector512Bits iterator =
                    new SampleBufferIteratorVector512Bits(samples, residualI, residualQ, 0.0f,
                            System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().i()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }
}
