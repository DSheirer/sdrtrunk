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

package io.github.dsheirer.dsp.c4fm;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.symbol.Dibit;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the DQPSK differential demodulator and differential integrator filter design.
 */
public class DQPSKDifferentialDemodulatorTest
{
    private static final int SAMPLE_RATE = 24000;
    private static final int SAMPLES_PER_SYMBOL = 5;
    private static final int BUFFER_LENGTH = 1024;
    private static final Dibit[] DIBITS = {Dibit.D00_PLUS_1, Dibit.D01_PLUS_3, Dibit.D10_MINUS_1, Dibit.D11_MINUS_3};

    /**
     * Synthetic differential QPSK signal generator.  Ramps the phase across each symbol period by the symbol's ideal
     * phase so that the phase change across each symbol period is the ideal symbol phase, plus a carrier offset.
     */
    private static class SignalGenerator
    {
        private final Random mRandom = new Random(1);
        private final double mOffset; //Radians per sample
        private final double mNoise;
        private double mPhase = 0.0;
        private Dibit mSymbol = Dibit.D00_PLUS_1;
        private int mSampleCounter = 0;
        private Dibit mPreviousSymbol;

        SignalGenerator(double offsetHertz, double noise)
        {
            mOffset = 2.0 * Math.PI * offsetHertz / SAMPLE_RATE;
            mNoise = noise;
        }

        /**
         * Generates the next buffer of samples, recording the symbol completed at each sample (or null).
         */
        float[][] next(Dibit[] completedSymbols)
        {
            float[] i = new float[BUFFER_LENGTH];
            float[] q = new float[BUFFER_LENGTH];

            for(int x = 0; x < BUFFER_LENGTH; x++)
            {
                if(mSampleCounter % SAMPLES_PER_SYMBOL == 0)
                {
                    mPreviousSymbol = mSymbol;
                    mSymbol = DIBITS[mRandom.nextInt(4)];
                    completedSymbols[x] = mSampleCounter > 0 ? mPreviousSymbol : null;
                }
                else
                {
                    completedSymbols[x] = null;
                }

                i[x] = (float)(Math.cos(mPhase) + mRandom.nextGaussian() * mNoise);
                q[x] = (float)(Math.sin(mPhase) + mRandom.nextGaussian() * mNoise);
                mPhase += mSymbol.getIdealPhase() / SAMPLES_PER_SYMBOL + mOffset;
                mSampleCounter++;
            }

            return new float[][]{i, q};
        }
    }

    private static double toHertz(float radiansPerSample)
    {
        return radiansPerSample * SAMPLE_RATE / (2.0 * Math.PI);
    }

    /**
     * Test: the integrator taps span one symbol period with the fractional remainder in the final tap and unity DC gain.
     */
    @Test
    void testDifferentialIntegratorDesign()
    {
        float[] taps = FilterFactory.getDifferentialIntegrator(5.2);
        assertEquals(6, taps.length, "Tap count");
        float sum = 0;

        for(float tap : taps)
        {
            sum += tap;
        }

        assertEquals(1.0, sum, 1e-5, "DC gain");
        assertEquals(1.0 / 5.2, taps[0], 1e-6, "Whole tap");
        assertEquals(0.2 / 5.2, taps[5], 1e-6, "Fractional tap");
        assertEquals(5, FilterFactory.getDifferentialIntegrator(5.0).length, "Integral samples per symbol");
    }

    /**
     * Test: the carrier offset estimate converges and each symbol period's demodulated sample, scaled to radians per
     * symbol, decides the transmitted symbol with the offset removed.
     */
    @Test
    void testOffsetRemovalAndSymbolRecovery()
    {
        SignalGenerator generator = new SignalGenerator(300, 0.0);
        DQPSKDifferentialDemodulator demodulator = new DQPSKDifferentialDemodulator();
        demodulator.setSamplesPerSymbol(SAMPLES_PER_SYMBOL);
        Dibit[] completed = new Dibit[BUFFER_LENGTH];
        int symbolCount = 0;
        int errorCount = 0;

        for(int buffer = 0; buffer < 400; buffer++)
        {
            float[][] samples = generator.next(completed);
            float[] demodulated = demodulator.demodulate(samples[0], samples[1], true);

            //Check symbol decisions after the estimator converges
            if(buffer >= 300)
            {
                for(int x = 0; x < BUFFER_LENGTH; x++)
                {
                    if(completed[x] != null)
                    {
                        symbolCount++;

                        if(Dibit.fromSample(demodulated[x] * SAMPLES_PER_SYMBOL) != completed[x])
                        {
                            errorCount++;
                        }
                    }
                }
            }
        }

        assertEquals(300.0, toHertz(demodulator.getFrequencyOffset()), 5.0, "Offset estimate");
        assertTrue(symbolCount > 10000, "Symbol count");
        assertEquals(0, errorCount, "Symbol errors");
    }

    /**
     * Test: an offset beyond the +/- 600 Hz fourth power estimator range is found by the ambiguity search while
     * unsynchronized, and is held once synchronized.
     */
    @Test
    void testAmbiguitySearch()
    {
        SignalGenerator generator = new SignalGenerator(-750, 0.1);
        DQPSKDifferentialDemodulator demodulator = new DQPSKDifferentialDemodulator();
        demodulator.setSamplesPerSymbol(SAMPLES_PER_SYMBOL);
        Dibit[] completed = new Dibit[BUFFER_LENGTH];
        boolean resolved = false;

        for(int buffer = 0; buffer < 1000 && !resolved; buffer++)
        {
            float[][] samples = generator.next(completed);
            demodulator.demodulate(samples[0], samples[1], false);

            //Emulate the symbol processor synchronizing once the offset is resolved
            resolved = Math.abs(toHertz(demodulator.getFrequencyOffset()) + 750) < 20;
        }

        assertTrue(resolved, "Search resolves the offset");

        for(int buffer = 0; buffer < 200; buffer++)
        {
            float[][] samples = generator.next(completed);
            demodulator.demodulate(samples[0], samples[1], true);
        }

        assertEquals(-750.0, toHertz(demodulator.getFrequencyOffset()), 10.0, "Offset held while synchronized");
    }

    /**
     * Test: the ambiguity search holds the resolved offset range across a gap with no signal while unsynchronized (ie
     * between transmissions on a traffic channel) so that the next transmission starts in the correct offset range.
     */
    @Test
    void testSearchHoldsAcrossSignalGap()
    {
        SignalGenerator generator = new SignalGenerator(-750, 0.1);
        DQPSKDifferentialDemodulator demodulator = new DQPSKDifferentialDemodulator();
        demodulator.setSamplesPerSymbol(SAMPLES_PER_SYMBOL);
        Dibit[] completed = new Dibit[BUFFER_LENGTH];
        boolean resolved = false;

        for(int buffer = 0; buffer < 1000 && !resolved; buffer++)
        {
            float[][] samples = generator.next(completed);
            demodulator.demodulate(samples[0], samples[1], false);
            resolved = Math.abs(toHertz(demodulator.getFrequencyOffset()) + 750) < 20;
        }

        assertTrue(resolved, "Search resolves the offset");

        //Emulate the symbol processor synchronizing to the signal and the framer decoding a valid NID once the offset is
        //resolved
        for(int buffer = 0; buffer < 20; buffer++)
        {
            float[][] samples = generator.next(completed);
            demodulator.demodulate(samples[0], samples[1], true);
            demodulator.offsetVerified();
        }

        //Gap of ~20,000 symbols (10x the search dwell period) with no signal and no synchronization
        Random random = new Random(3);

        for(int buffer = 0; buffer < 100; buffer++)
        {
            float[] i = new float[BUFFER_LENGTH];
            float[] q = new float[BUFFER_LENGTH];

            for(int x = 0; x < BUFFER_LENGTH; x++)
            {
                i[x] = (float)random.nextGaussian();
                q[x] = (float)random.nextGaussian();
            }

            demodulator.demodulate(i, q, false);
        }

        assertEquals(-750.0, toHertz(demodulator.getFrequencyOffset()), 20.0, "Offset held across the gap");
    }

    /**
     * Test: symbol processor synchronization without a valid NID decode (ie a false sync detection in an incorrect
     * offset range) doesn't hold the ambiguity search in the incorrect offset range for the longer dwell period, while
     * a verified offset range is held.
     */
    @Test
    void testUnverifiedSynchronizationDoesNotHoldSearch()
    {
        SignalGenerator generatorUnverified = new SignalGenerator(-750, 0.1);
        SignalGenerator generatorVerified = new SignalGenerator(-750, 0.1);
        DQPSKDifferentialDemodulator unverified = new DQPSKDifferentialDemodulator();
        DQPSKDifferentialDemodulator verified = new DQPSKDifferentialDemodulator();
        unverified.setSamplesPerSymbol(SAMPLES_PER_SYMBOL);
        verified.setSamplesPerSymbol(SAMPLES_PER_SYMBOL);
        Dibit[] completed = new Dibit[BUFFER_LENGTH];

        //Emulate a sync detection in the initial (incorrect) offset range, with and without a valid NID decode
        for(int buffer = 0; buffer < 5; buffer++)
        {
            float[][] samples = generatorUnverified.next(completed);
            unverified.demodulate(samples[0], samples[1], true);
            samples = generatorVerified.next(completed);
            verified.demodulate(samples[0], samples[1], true);
            verified.offsetVerified();
        }

        //Unsynchronized for ~10,000 symbols: longer than the search dwell and shorter than the verified dwell
        for(int buffer = 0; buffer < 50; buffer++)
        {
            float[][] samples = generatorUnverified.next(completed);
            unverified.demodulate(samples[0], samples[1], false);
            samples = generatorVerified.next(completed);
            verified.demodulate(samples[0], samples[1], false);
        }

        //The unverified search steps to an adjacent (1200 Hz) offset range while the verified search holds its range
        double difference = Math.abs(toHertz(unverified.getFrequencyOffset()) - toHertz(verified.getFrequencyOffset()));
        assertTrue(difference > 600, "Unverified offset range search stepped away [" + difference + " Hz]");
    }

    /**
     * Test: each sample's reliability is (|x[n]|^2 * |x[n - sps]|^2) / (|x[n]|^2 + |x[n - sps]|^2), including across the
     * sample buffer boundary.
     */
    @Test
    void testReliabilities()
    {
        DQPSKDifferentialDemodulator demodulator = new DQPSKDifferentialDemodulator();
        demodulator.setSamplesPerSymbol(SAMPLES_PER_SYMBOL);
        float[] amplitudes = new float[2 * BUFFER_LENGTH];

        for(int x = 0; x < amplitudes.length; x++)
        {
            amplitudes[x] = 0.1f + 0.9f * (float)Math.abs(Math.sin(x * 0.37));
        }

        float[] reliabilities = new float[amplitudes.length];

        for(int buffer = 0; buffer < 2; buffer++)
        {
            float[] i = new float[BUFFER_LENGTH];
            float[] q = new float[BUFFER_LENGTH];

            for(int x = 0; x < BUFFER_LENGTH; x++)
            {
                //Rotate the phase to show that the reliability depends only on the sample power
                double phase = x * 0.5;
                float amplitude = amplitudes[buffer * BUFFER_LENGTH + x];
                i[x] = (float)(amplitude * Math.cos(phase));
                q[x] = (float)(amplitude * Math.sin(phase));
            }

            demodulator.demodulate(i, q, true);
            System.arraycopy(demodulator.getReliabilities(), 0, reliabilities, buffer * BUFFER_LENGTH, BUFFER_LENGTH);
        }

        for(int x = SAMPLES_PER_SYMBOL; x < amplitudes.length; x++)
        {
            double current = amplitudes[x] * amplitudes[x];
            double delayed = amplitudes[x - SAMPLES_PER_SYMBOL] * amplitudes[x - SAMPLES_PER_SYMBOL];
            assertEquals(current * delayed / (current + delayed), reliabilities[x], 1e-5, "Reliability at " + x);
        }
    }

    /**
     * Test: the offset estimate doesn't drift when the input is noise.
     */
    @Test
    void testNoiseDoesNotMoveEstimate()
    {
        Random random = new Random(2);
        DQPSKDifferentialDemodulator demodulator = new DQPSKDifferentialDemodulator();
        demodulator.setSamplesPerSymbol(SAMPLES_PER_SYMBOL);

        for(int buffer = 0; buffer < 2000; buffer++)
        {
            float[] i = new float[BUFFER_LENGTH];
            float[] q = new float[BUFFER_LENGTH];

            for(int x = 0; x < BUFFER_LENGTH; x++)
            {
                i[x] = (float)random.nextGaussian();
                q[x] = (float)random.nextGaussian();
            }

            demodulator.demodulate(i, q, true);
        }

        assertEquals(0.0, toHertz(demodulator.getFrequencyOffset()), 1e-3, "Offset estimate");
    }
}
