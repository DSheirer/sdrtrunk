/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
package io.github.dsheirer.vector.calibrate.channelizer;

import io.github.dsheirer.dsp.filter.channelizer.ChannelDFT;
import io.github.dsheirer.dsp.filter.channelizer.ScalarChannelDFT;
import io.github.dsheirer.dsp.filter.channelizer.VectorChannelDFT;
import io.github.dsheirer.vector.calibrate.Calibration;
import io.github.dsheirer.vector.calibrate.CalibrationException;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import org.apache.commons.math3.stat.descriptive.moment.Mean;

/**
 * Determines the optimal scalar vs vector implementation of the polyphase channel DFT.
 */
public class ChannelDFTCalibration extends Calibration
{
    private static final int CHANNEL_COUNT = 96;
    private static final int[] CHANNELS = {5, 6, 40, 90};
    private static final int ITERATION_DURATION_MS = 500;
    private static final int WARMUP_ITERATIONS = 3;
    private static final int TEST_ITERATIONS = 3;

    /**
     * Constructs an instance
     */
    public ChannelDFTCalibration()
    {
        super(CalibrationType.CHANNEL_DFT);
    }

    @Override public void calibrate() throws CalibrationException
    {
        ChannelDFT scalar = new ScalarChannelDFT(CHANNEL_COUNT);
        ChannelDFT vector = new VectorChannelDFT(CHANNEL_COUNT);

        for(int x = 0; x < WARMUP_ITERATIONS; x++)
        {
            test(scalar);
            test(vector);
        }

        Mean scalarMean = new Mean();
        Mean vectorMean = new Mean();

        for(int x = 0; x < TEST_ITERATIONS; x++)
        {
            scalarMean.increment(test(scalar));
            vectorMean.increment(test(vector));
        }

        mLog.info("CHANNEL DFT - SCALAR:" + DECIMAL_FORMAT.format(scalarMean.getResult()));
        mLog.info("CHANNEL DFT - VECTOR:" + DECIMAL_FORMAT.format(vectorMean.getResult()));

        if(scalarMean.getResult() > vectorMean.getResult())
        {
            setImplementation(Implementation.SCALAR);
        }
        else
        {
            setImplementation(Implementation.VECTOR_SIMD_PREFERRED);
        }

        mLog.info("CHANNEL DFT - SET IMPLEMENTATION TO:" + getImplementation());
    }

    private long test(ChannelDFT channelDFT)
    {
        float[] samples = getFloatSamples(CHANNEL_COUNT * 2);
        float[] input = samples.clone();
        float accumulator = 0.0f;
        long count = 0;

        long start = System.currentTimeMillis();

        while((System.currentTimeMillis() - start) < ITERATION_DURATION_MS)
        {
            System.arraycopy(input, 0, samples, 0, samples.length);
            channelDFT.process(samples, CHANNELS);
            accumulator += samples[10];
            count++;
        }

        return count + (long)(accumulator * 0);
    }
}
