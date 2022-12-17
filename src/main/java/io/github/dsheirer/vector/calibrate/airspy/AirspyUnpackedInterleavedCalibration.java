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

package io.github.dsheirer.vector.calibrate.airspy;

import io.github.dsheirer.buffer.airspy.AirspyBufferIterator;
import io.github.dsheirer.buffer.airspy.AirspyInterleavedBufferIteratorScalar;
import io.github.dsheirer.buffer.airspy.AirspyInterleavedBufferIteratorVector128Bits;
import io.github.dsheirer.buffer.airspy.AirspyInterleavedBufferIteratorVector256Bits;
import io.github.dsheirer.buffer.airspy.AirspyInterleavedBufferIteratorVector512Bits;
import io.github.dsheirer.buffer.airspy.AirspyInterleavedBufferIteratorVector64Bits;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import jdk.incubator.vector.FloatVector;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calculates optimal implementation (SCALAR vs VECTOR) for interleaved airspy native buffers.
 */
public class AirspyUnpackedInterleavedCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 131072;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;

    /**
     * Constructs an instance
     */
    public AirspyUnpackedInterleavedCalibration()
    {
        super(CalibrationType.AIRSPY_UNPACKED_INTERLEAVED_ITERATOR);
    }

    @Override
    public void calibrate() throws CalibrationException
    {
        short[] samples = getShortSamples(BUFFER_SIZE);
        short[] residualI = getShortSamples(AirspyBufferIterator.I_OVERLAP);
        short[] residualQ = getShortSamples(AirspyBufferIterator.Q_OVERLAP);

        //Warm-Up Phase ....
        mLog.info("AIRSPY UNPACKED INTERLEAVED - VECTOR SIMD LANES PREFERRED: " + FloatVector.SPECIES_PREFERRED.length());

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = calibrateScalar(samples, residualI, residualQ);
            scalarMean.increment(score);
        }

        mLog.info("AIRSPY UNPACKED INTERLEAVED WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        switch(FloatVector.SPECIES_PREFERRED.length())
        {
            //Deliberate fall-through of each case statement so that we can test the largest
            //SIMD lane width supported by hardware down to the smallest SIMD lane width.
            case 16:
            {
                Mean vectorMean = new Mean();

                for(int x = 0; x < WARMUP_ITERATIONS; x++)
                {
                    long score = calibrateVector512(samples, residualI, residualQ);
                    vectorMean.increment(score);
                }

                mLog.info("AIRSPY UNPACKED INTERLEAVED WARMUP - VECTOR 512: " + DECIMAL_FORMAT.format(vectorMean.getResult()));
            }
            case 8:
            {
                Mean vectorMean = new Mean();

                for(int x = 0; x < WARMUP_ITERATIONS; x++)
                {
                    long score = calibrateVector256(samples, residualI, residualQ);
                    vectorMean.increment(score);
                }

                mLog.info("AIRSPY UNPACKED INTERLEAVED WARMUP - VECTOR 256: " + DECIMAL_FORMAT.format(vectorMean.getResult()));
            }
            case 4:
            {
                Mean vectorMean = new Mean();

                for(int x = 0; x < WARMUP_ITERATIONS; x++)
                {
                    long score = calibrateVector128(samples, residualI, residualQ);
                    vectorMean.increment(score);
                }

                mLog.info("AIRSPY UNPACKED INTERLEAVED WARMUP - VECTOR 128: " + DECIMAL_FORMAT.format(vectorMean.getResult()));
            }
            case 2:
            {
                Mean vectorMean = new Mean();

                for(int x = 0; x < WARMUP_ITERATIONS; x++)
                {
                    long score = calibrateVector64(samples, residualI, residualQ);
                    vectorMean.increment(score);
                }

                mLog.info("AIRSPY UNPACKED INTERLEAVED WARMUP - VECTOR 64: " + DECIMAL_FORMAT.format(vectorMean.getResult()));
            }

            //Test Phase ....
            scalarMean.clear();
            for(int x = 0; x < TEST_ITERATIONS; x++)
            {
                long score = calibrateScalar(samples, residualI, residualQ);
                scalarMean.increment(score);
            }

            double bestScore = scalarMean.getResult();
            setImplementation(Implementation.SCALAR);

            mLog.info("AIRSPY UNPACKED INTERLEAVED - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

            switch(FloatVector.SPECIES_PREFERRED.length())
            {
                //Deliberate fall-through of each case statement so that we can test the largest
                //SIMD lane width supported by hardware down to the smallest SIMD lane width.
                case 16:
                {
                    Mean vectorMean = new Mean();

                    for(int x = 0; x < TEST_ITERATIONS; x++)
                    {
                        long score = calibrateVector512(samples, residualI, residualQ);
                        vectorMean.increment(score);
                    }

                    if(vectorMean.getResult() > bestScore)
                    {
                        bestScore = vectorMean.getResult();
                        setImplementation(Implementation.VECTOR_SIMD_512);
                    }

                    mLog.info("AIRSPY UNPACKED INTERLEAVED - VECTOR 512: " + DECIMAL_FORMAT.format(vectorMean.getResult()));
                }
                case 8:
                {
                    Mean vectorMean = new Mean();

                    for(int x = 0; x < TEST_ITERATIONS; x++)
                    {
                        long score = calibrateVector256(samples, residualI, residualQ);
                        vectorMean.increment(score);
                    }

                    if(vectorMean.getResult() > bestScore)
                    {
                        bestScore = vectorMean.getResult();
                        setImplementation(Implementation.VECTOR_SIMD_256);
                    }

                    mLog.info("AIRSPY UNPACKED INTERLEAVED - VECTOR 256: " + DECIMAL_FORMAT.format(vectorMean.getResult()));
                }
                case 4:
                {
                    Mean vectorMean = new Mean();

                    for(int x = 0; x < TEST_ITERATIONS; x++)
                    {
                        long score = calibrateVector128(samples, residualI, residualQ);
                        vectorMean.increment(score);
                    }

                    if(vectorMean.getResult() > bestScore)
                    {
                        bestScore = vectorMean.getResult();
                        setImplementation(Implementation.VECTOR_SIMD_128);
                    }

                    mLog.info("AIRSPY UNPACKED INTERLEAVED - VECTOR 128: " + DECIMAL_FORMAT.format(vectorMean.getResult()));
                }
                case 2:
                {
                    Mean vectorMean = new Mean();

                    for(int x = 0; x < TEST_ITERATIONS; x++)
                    {
                        long score = calibrateVector64(samples, residualI, residualQ);
                        vectorMean.increment(score);
                    }

                    if(vectorMean.getResult() > bestScore)
                    {
                        setImplementation(Implementation.VECTOR_SIMD_64);
                    }

                    mLog.info("AIRSPY UNPACKED INTERLEAVED - VECTOR 64: " + DECIMAL_FORMAT.format(vectorMean.getResult()));
                }
            }
        }

        mLog.info("AIRSPY UNPACKED INTERLEAVED - SET OPTIMAL IMPLEMENTATION TO: " + getImplementation());
    }

    private long calibrateScalar(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;
        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            AirspyInterleavedBufferIteratorScalar iterator = new AirspyInterleavedBufferIteratorScalar(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().samples()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }

    private long calibrateVector64(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            AirspyInterleavedBufferIteratorVector64Bits iterator = new AirspyInterleavedBufferIteratorVector64Bits(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().samples()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }

    private long calibrateVector128(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;

        for(int x = 0; x < ITERATION_DURATION_MS; x++)
        {
            AirspyInterleavedBufferIteratorVector128Bits iterator = new AirspyInterleavedBufferIteratorVector128Bits(samples, residualI,
                    residualQ, 0.0f, System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().samples()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }

    private long calibrateVector256(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            AirspyInterleavedBufferIteratorVector256Bits iterator =
                    new AirspyInterleavedBufferIteratorVector256Bits(samples, residualI, residualQ, 0.0f, System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().samples()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }

    private long calibrateVector512(short[] samples, short[] residualI, short[] residualQ)
    {
        long start = System.currentTimeMillis();
        long accumulator = 0;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            AirspyInterleavedBufferIteratorVector512Bits iterator =
                    new AirspyInterleavedBufferIteratorVector512Bits(samples, residualI, residualQ, 0.0f,
                            System.currentTimeMillis(), 0.0f);

            while(iterator.hasNext() && ((System.currentTimeMillis() - start) < ITERATION_DURATION_MS))
            {
                accumulator += iterator.next().samples()[2];
                count++;
            }
        }

        return count + (accumulator * 0);
    }
}
