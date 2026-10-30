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
package io.github.dsheirer.dsp.oscillator;

import io.github.dsheirer.sample.complex.ComplexSamples;
import jdk.incubator.vector.FloatVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the vector complex oscillator produces a clean tone, including across frequency changes.
 */
public class VectorComplexOscillatorTest
{
    private static final double SAMPLE_RATE = 2_400_000.0;
    private static final int BUFFER_SIZE = FloatVector.SPECIES_PREFERRED.length() * 256;
    private static final double MAXIMUM_ERROR_RADIANS = 0.001;

    /**
     * Largest deviation of the sample to sample rotation from the rotation expected for the frequency.
     */
    private static double rotationError(ComplexSamples samples, double frequency)
    {
        double expected = 2.0 * Math.PI * frequency / SAMPLE_RATE;
        float[] i = samples.i();
        float[] q = samples.q();
        double error = 0.0;

        for(int x = 1; x < i.length; x++)
        {
            double rotation = Math.atan2(q[x], i[x]) - Math.atan2(q[x - 1], i[x - 1]);
            error = Math.max(error, Math.abs(Math.IEEEremainder(rotation - expected, 2.0 * Math.PI)));
        }

        return error;
    }

    /**
     * Test: output is a unit magnitude tone at the requested frequency, for the mixer's sample generation method.
     */
    @Test
    void generatesTone()
    {
        VectorComplexOscillator oscillator = new VectorComplexOscillator(400_000, SAMPLE_RATE);
        ComplexSamples samples = oscillator.generateComplexSamples(BUFFER_SIZE, 0);

        assertTrue(rotationError(samples, 400_000) < MAXIMUM_ERROR_RADIANS);

        for(int x = 0; x < samples.i().length; x++)
        {
            assertEquals(1.0, Math.hypot(samples.i()[x], samples.q()[x]), 0.001);
        }
    }

    /**
     * Test: the SIMD lanes follow a frequency change, without a phase discontinuity at the change.
     */
    @Test
    void frequencyChangeIsPhaseContinuous()
    {
        VectorComplexOscillator oscillator = new VectorComplexOscillator(600_000, SAMPLE_RATE);
        ComplexSamples before = oscillator.generateComplexSamples(BUFFER_SIZE, 0);
        oscillator.setFrequency(-300_000);
        ComplexSamples after = oscillator.generateComplexSamples(BUFFER_SIZE, 0);

        assertTrue(rotationError(after, -300_000) < MAXIMUM_ERROR_RADIANS);

        int last = before.i().length - 1;
        double step = Math.atan2(after.q()[0], after.i()[0]) - Math.atan2(before.q()[last], before.i()[last]);
        double error = Math.abs(Math.IEEEremainder(step, 2.0 * Math.PI));
        assertTrue(error <= (2.0 * Math.PI * 600_000 / SAMPLE_RATE) * FloatVector.SPECIES_PREFERRED.length() + 0.001);
    }

    /**
     * Test: the SIMD lanes stay aligned over a long run at frequencies where the rotation for a set of lanes is nearly
     * a whole turn or half turn, which is small enough for rounding to affect each lane differently.  The frequencies
     * that are affected depend on the lane width of the hardware.
     */
    @Test
    void lanesRemainAlignedAtRationalFrequencies()
    {
        int buffers = 30_000_000 / BUFFER_SIZE;

        for(double divisor: new double[]{8.0, 16.0, 32.0})
        {
            double frequency = SAMPLE_RATE / divisor;
            VectorComplexOscillator oscillator = new VectorComplexOscillator(frequency, SAMPLE_RATE);

            for(int x = 0; x < buffers; x++)
            {
                assertTrue(rotationError(oscillator.generateComplexSamples(BUFFER_SIZE, 0), frequency) < MAXIMUM_ERROR_RADIANS);
            }
        }
    }

    /**
     * Test: a frequency change from another thread during sample generation doesn't leave the SIMD lanes spaced for
     * the previous frequency.
     */
    @Test
    void concurrentFrequencyChangeUpdatesLanes() throws Exception
    {
        for(int trial = 0; trial < 500; trial++)
        {
            VectorComplexOscillator oscillator = new VectorComplexOscillator(600_000, SAMPLE_RATE);
            oscillator.generateComplexSamples(BUFFER_SIZE, 0);

            Thread thread = new Thread(() -> oscillator.setFrequency(-300_000));
            thread.start();

            while(thread.isAlive())
            {
                oscillator.generateComplexSamples(BUFFER_SIZE, 0);
            }

            //The first buffer can overlap the change, so that it is the second buffer that must be clean
            oscillator.generateComplexSamples(BUFFER_SIZE, 0);
            assertTrue(rotationError(oscillator.generateComplexSamples(BUFFER_SIZE, 0), -300_000) < MAXIMUM_ERROR_RADIANS);
        }
    }
}
