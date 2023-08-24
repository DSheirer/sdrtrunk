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

package io.github.dsheirer.vector.calibrate.demodulator;

import io.github.dsheirer.dsp.psk.dqpsk.DQPSKDemodulator;
import io.github.dsheirer.dsp.psk.dqpsk.DQPSKDemodulatorScalar;
import io.github.dsheirer.dsp.psk.dqpsk.DQPSKDemodulatorVector128;
import io.github.dsheirer.dsp.psk.dqpsk.DQPSKDemodulatorVector256;
import io.github.dsheirer.dsp.psk.dqpsk.DQPSKDemodulatorVector512;
import io.github.dsheirer.dsp.psk.dqpsk.DQPSKDemodulatorVector64;
import io.github.dsheirer.dsp.psk.dqpsk.DQPSKSoftSymbolListener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calibrates DQPSK demodulator options
 */
public class DqpskDemodulatorCalibration extends Calibration
{
    private static final int BUFFER_SIZE = 2048;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;
    private DQPSKDemodulator mScalarDemodulator = new DQPSKDemodulatorScalar(4800);
    private final DQPSKDemodulator mVectorDemodulator64 = new DQPSKDemodulatorVector64(4800);
    private final DQPSKDemodulator mVectorDemodulator128 = new DQPSKDemodulatorVector128(4800);
    private final DQPSKDemodulator mVectorDemodulator256 = new DQPSKDemodulatorVector256(4800);
    private final DQPSKDemodulator mVectorDemodulator512 = new DQPSKDemodulatorVector512(4800);
    private final OutputListener mListener = new OutputListener();

    /**
     * Constructs an instance
     */
    public DqpskDemodulatorCalibration()
    {
        super(CalibrationType.DQPSK_DEMODULATOR);
        mScalarDemodulator.setListener(mListener);
        mVectorDemodulator64.setListener(mListener);
        mVectorDemodulator128.setListener(mListener);
        mVectorDemodulator256.setListener(mListener);
        mVectorDemodulator512.setListener(mListener);

        float sampleRate = 25000.0f;
        mScalarDemodulator.setSampleRate(sampleRate);
        mVectorDemodulator64.setSampleRate(sampleRate);
        mVectorDemodulator128.setSampleRate(sampleRate);
        mVectorDemodulator256.setSampleRate(sampleRate);
        mVectorDemodulator512.setSampleRate(sampleRate);
    }

    @Override public void calibrate() throws CalibrationException
    {
        float[] i = getFloatSamples(BUFFER_SIZE);
        float[] q = getFloatSamples(BUFFER_SIZE);
        ComplexSamples samples = new ComplexSamples(i, q, System.currentTimeMillis());

        Mean scalarMean = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testScalar(samples);
            scalarMean.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR WARMUP - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        Mean vectorMean64 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector64(samples);
            vectorMean64.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR WARMUP - VECTOR 64: " + DECIMAL_FORMAT.format(vectorMean64.getResult()));

        Mean vectorMean128 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector128(samples);
            vectorMean128.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR WARMUP - VECTOR 128: " + DECIMAL_FORMAT.format(vectorMean128.getResult()));

        Mean vectorMean256 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector256(samples);
            vectorMean256.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR WARMUP - VECTOR 256: " + DECIMAL_FORMAT.format(vectorMean256.getResult()));

        Mean vectorMean512 = new Mean();

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            long score = testVector512(samples);
            vectorMean512.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR WARMUP - VECTOR 512: " + DECIMAL_FORMAT.format(vectorMean512.getResult()));

        //Start tests
        scalarMean.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testScalar(samples);
            scalarMean.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR - SCALAR: " + DECIMAL_FORMAT.format(scalarMean.getResult()));

        vectorMean64.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector64(samples);
            vectorMean64.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR - VECTOR 64: " + DECIMAL_FORMAT.format(vectorMean64.getResult()));

        vectorMean128.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector128(samples);
            vectorMean128.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR - VECTOR 128: " + DECIMAL_FORMAT.format(vectorMean128.getResult()));

        vectorMean256.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector256(samples);
            vectorMean256.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR - VECTOR 256: " + DECIMAL_FORMAT.format(vectorMean256.getResult()));

        vectorMean512.clear();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            long score = testVector512(samples);
            vectorMean512.increment(score);
        }

        mLog.info("DQPSK DEMODULATOR - VECTOR 512: " + DECIMAL_FORMAT.format(vectorMean512.getResult()));

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

        mListener.finish();

        mLog.info("DQPSK DEMODULATOR - SET OPTIMAL IMPLEMENTATION TO:" + getImplementation());
    }

    private long testScalar(ComplexSamples samples)
    {
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            mScalarDemodulator.receive(samples);
            count++;
        }

        return count;
    }

    private long testVector64(ComplexSamples samples)
    {
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            mVectorDemodulator64.receive(samples);
            count++;
        }

        return count;
    }

    private long testVector128(ComplexSamples samples)
    {
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            mVectorDemodulator128.receive(samples);
            count++;
        }

        return count;
    }

    private long testVector256(ComplexSamples samples)
    {
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            mVectorDemodulator256.receive(samples);
            count++;
        }

        return count;
    }

    private long testVector512(ComplexSamples samples)
    {
        long count = 0;
        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            mVectorDemodulator512.receive(samples);
            count++;
        }

        return count;
    }

    /**
     * Listener to receive the demodulated samples
     */
    public class OutputListener implements DQPSKSoftSymbolListener
    {
        private float mAccumulator;

        public void finish()
        {
            if(mAccumulator == 0)
            {
                System.out.println("");
            }
        }

        @Override
        public void receive(float[] samples)
        {
            mAccumulator += samples[0];
        }

        @Override
        public void setTimestamp(long timestamp)
        {
            //Ignore
        }

        @Override
        public void setSamplesPerSymbol(float samplesPerSymbol)
        {
            //Ignore
        }
    }

    public static void main(String[] args)
    {
        DqpskDemodulatorCalibration calibration = new DqpskDemodulatorCalibration();

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
