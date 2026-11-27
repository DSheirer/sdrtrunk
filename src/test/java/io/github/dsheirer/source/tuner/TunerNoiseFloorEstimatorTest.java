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

package io.github.dsheirer.source.tuner;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.ComplexSamplesNativeBufferAdapter;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import io.github.dsheirer.vector.calibrate.CalibrationType;
import io.github.dsheirer.vector.calibrate.Implementation;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Feeds white noise of a known density to the tuner noise floor estimator and checks the channel noise density.
 */
public class TunerNoiseFloorEstimatorTest
{
    private static final long CENTER = 100_000_000L;
    private static final double NOISE_VARIANCE = 1E-4;

    /**
     * Noise with optional tones, one buffer per 50 ms processing interval.
     */
    private static class Generator
    {
        private final Random mRandom = new Random(1234);
        private final double mSampleRate;
        private final double[] mToneOffsets;
        private final double mToneMagnitude;
        private long mSample;

        Generator(double sampleRate, double toneMagnitude, double... toneOffsets)
        {
            mSampleRate = sampleRate;
            mToneMagnitude = toneMagnitude;
            mToneOffsets = toneOffsets;
        }

        ComplexSamplesNativeBufferAdapter next(int length)
        {
            float[] i = new float[length];
            float[] q = new float[length];
            double sigma = Math.sqrt(NOISE_VARIANCE / 2.0);

            for(int x = 0; x < length; x++)
            {
                double vi = mRandom.nextGaussian() * sigma;
                double vq = mRandom.nextGaussian() * sigma;

                for(double offset: mToneOffsets)
                {
                    double phase = 2.0 * Math.PI * offset * (mSample + x) / mSampleRate;
                    vi += mToneMagnitude * Math.cos(phase);
                    vq += mToneMagnitude * Math.sin(phase);
                }

                i[x] = (float)vi;
                q[x] = (float)vq;
            }

            mSample += length;
            return new ComplexSamplesNativeBufferAdapter(new ComplexSamples(i, q, 0));
        }
    }

    /**
     * Runs 2 seconds of buffers through the estimator and returns the noise density in dBFS/Hz for the channel.
     */
    private static double run(double sampleRate, Generator generator, long channelFrequency, int bandwidth)
    {
        TunerNoiseFloorEstimator estimator = new TunerNoiseFloorEstimator(() -> CENTER, () -> sampleRate,
            () -> (int)(sampleRate * 0.98), false);
        assertTrue(Double.isNaN(estimator.getNoiseDensity(channelFrequency, bandwidth)), "no estimate before measuring");
        assertTrue(estimator.isActive(), "request turns on the measurement");
        int bufferLength = TunerNoiseFloorEstimator.getFftSize(sampleRate) * 8;

        for(long now = 0; now <= 2000; now += 50)
        {
            estimator.receive(generator.next(bufferLength));
            estimator.process(now);
        }

        return 10.0 * Math.log10(estimator.getNoiseDensity(channelFrequency, bandwidth));
    }

    private static double expected(double sampleRate)
    {
        return 10.0 * Math.log10(NOISE_VARIANCE / sampleRate);
    }

    /**
     * Test: noise density matches the injected noise at several tuner sample rates.
     */
    /**
     * Runs the test with the estimator's FFT using each implementation, independent of this computer's calibration.
     */
    private static void forEachImplementation(Runnable test)
    {
        CalibrationManager manager = CalibrationManager.getInstance();

        for(Implementation implementation: new Implementation[]{Implementation.SCALAR,
            Implementation.VECTOR_SIMD_PREFERRED})
        {
            try
            {
                manager.setImplementationOverride(CalibrationType.FFT_COMPLEX, implementation);
                test.run();
            }
            finally
            {
                manager.setImplementationOverride(CalibrationType.FFT_COMPLEX, null);
            }
        }
    }

    @Test
    void noiseDensity()
    {
        forEachImplementation(TunerNoiseFloorEstimatorTest::noiseDensityTest);
    }

    private static void noiseDensityTest()
    {
        for(double sampleRate: new double[]{2_400_000.0, 10_000_000.0, 6_000_000.0, 250_000.0})
        {
            long offset = (long)Math.min(150_000, 0.2 * sampleRate);
            double measured = run(sampleRate, new Generator(sampleRate, 0.0), CENTER + offset, 12500);
            assertEquals(expected(sampleRate), measured, 0.3, "sample rate " + sampleRate);
        }
    }

    /**
     * Test: strong carriers on the channel and on a few neighbors, including adjacent 12.5 kHz channels, don't move
     * the noise estimate.
     */
    @Test
    void occupiedNeighbors()
    {
        double sampleRate = 2_400_000.0;
        long channel = CENTER + 150_000;
        Generator generator = new Generator(sampleRate, 0.03, 150_000, 137_500, 162_500, 250_000, 50_000, 300_000);
        assertEquals(expected(sampleRate), run(sampleRate, generator, channel, 12500), 0.5);
    }

    /**
     * Test: channels near the tuner band edges, where the channel source managers can place them after a retune for a
     * traffic channel, still get a noise estimate.  Offsets up to 49% of the sample rate (98% of the bandwidth).
     */
    @Test
    void channelNearBandEdge()
    {
        forEachImplementation(TunerNoiseFloorEstimatorTest::channelNearBandEdgeTest);
    }

    private static void channelNearBandEdgeTest()
    {
        for(double sampleRate: new double[]{2_400_000.0, 10_000_000.0})
        {
            for(double fraction: new double[]{0.38, 0.42, 0.45, 0.47, 0.49, -0.45, -0.47, -0.49})
            {
                long channel = CENTER + (long)(fraction * sampleRate);
                double measured = run(sampleRate, new Generator(sampleRate, 0.0), channel, 12500);
                assertEquals(expected(sampleRate), measured, 0.3, "sample rate " + sampleRate + " channel at " +
                    fraction + " of the sample rate");
            }
        }
    }

    /**
     * Test: a tuner frequency change invalidates the estimate until the next one.
     */
    @Test
    void frequencyChange()
    {
        double sampleRate = 2_400_000.0;
        AtomicLong frequency = new AtomicLong(CENTER);
        TunerNoiseFloorEstimator estimator = new TunerNoiseFloorEstimator(frequency::get, () -> sampleRate,
            () -> (int)(sampleRate * 0.98), false);
        estimator.getNoiseDensity(CENTER, 12500);
        Generator generator = new Generator(sampleRate, 0.0);

        for(long now = 0; now <= 1100; now += 50)
        {
            estimator.receive(generator.next(1024));
            estimator.process(now);
        }

        assertFalse(Double.isNaN(estimator.getNoiseDensity(CENTER + 100_000, 12500)));
        frequency.set(CENTER + 1_000_000);
        assertTrue(Double.isNaN(estimator.getNoiseDensity(CENTER + 1_100_000, 12500)));
    }

    /**
     * Test: buffers aren't kept until a request turns on the measurement.
     */
    @Test
    void inactiveUntilRequested()
    {
        TunerNoiseFloorEstimator estimator = new TunerNoiseFloorEstimator(() -> CENTER, () -> 2_400_000.0, () -> 0,
            false);
        assertFalse(estimator.isActive());
        estimator.receive(new Generator(2_400_000.0, 0.0).next(1024));
        estimator.process(2000);
        assertFalse(estimator.isActive());
    }

    /**
     * Test: FFT sizes give bins of about 25 kHz, with a minimum size for low sample rates.
     */
    @Test
    void fftSize()
    {
        assertEquals(96, TunerNoiseFloorEstimator.getFftSize(2_400_000.0));
        assertEquals(400, TunerNoiseFloorEstimator.getFftSize(10_000_000.0));
        assertEquals(64, TunerNoiseFloorEstimator.getFftSize(250_000.0));
    }
}
