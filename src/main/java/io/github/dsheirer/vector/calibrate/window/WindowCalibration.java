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

package io.github.dsheirer.vector.calibrate.window;

import io.github.dsheirer.dsp.window.ScalarWindow;
import io.github.dsheirer.dsp.window.VectorWindow;
import io.github.dsheirer.dsp.window.Window;
import io.github.dsheirer.dsp.window.WindowFactory;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Calculates the optimal (scalar vs vector) implementation for windowing samples.
 */
public class WindowCalibration extends Calibration
{
    private static final int WINDOW_SIZE = 8192;
    private static final int ITERATION_DURATION_MS = 1000;
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 5;
    private Window mScalarWindow;
    private Window mVectorWindow;

    /**
     * Constructs an instance
     */
    public WindowCalibration()
    {
        super(CalibrationType.WINDOW);
        float[] window = WindowFactory.getBlackman(WINDOW_SIZE);
        mScalarWindow = new ScalarWindow(window);
        mVectorWindow = new VectorWindow(window);
    }

    @Override public void calibrate() throws CalibrationException
    {
        Mean scalarMean = new Mean();

        float[] samples = getFloatSamples(WINDOW_SIZE);

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            scalarMean.increment(testScalar(samples));
        }

        mLog.info("WINDOW WARMUP - SCALAR: " + scalarMean.getResult());

        Mean vectorMean = new Mean();

        samples = getFloatSamples(WINDOW_SIZE);

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            vectorMean.increment(testVector(samples));
        }

        mLog.info("WINDOW WARMUP - VECTOR: " + vectorMean.getResult());

        //Test starts ...
        scalarMean.clear();

        samples = getFloatSamples(WINDOW_SIZE);

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            scalarMean.increment(testScalar(samples));
        }

        mLog.info("WINDOW - SCALAR: " + scalarMean.getResult());

        vectorMean.clear();

        samples = getFloatSamples(WINDOW_SIZE);

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            vectorMean.increment(testVector(samples));
        }

        mLog.info("WINDOW - VECTOR: " + vectorMean.getResult());

        if(scalarMean.getResult() > vectorMean.getResult())
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("WINDOW - OPTIMAL IMPLEMENTATION SET TO: " + getImplementation());
    }

    private long testScalar(float[] samples)
    {
        long start = System.currentTimeMillis();
        double accumulator = 0.0d;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            mScalarWindow.apply(samples);
            accumulator += samples[3];
            count++;
        }

        return count + (long)(accumulator * 0);
    }
    private long testVector(float[] samples)
    {
        long start = System.currentTimeMillis();
        double accumulator = 0.0d;
        long count = 0;

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            mVectorWindow.apply(samples);
            accumulator += samples[3];
            count++;
        }

        return count + (long)(accumulator * 0);
    }
}
