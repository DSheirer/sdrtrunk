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
package io.github.dsheirer.dsp.filter.decimate;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.halfband.RealHalfBandDecimationFilter;
import io.github.dsheirer.dsp.mixer.ComplexMixer;
import io.github.dsheirer.dsp.mixer.ScalarComplexMixer;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that reusing arrays for intermediate results doesn't change the results.
 */
public class IntermediateOutputReuseTest
{
    private static float[] samples(int length, long seed)
    {
        Random random = new Random(seed);
        float[] samples = new float[length];

        for(int x = 0; x < length; x++)
        {
            samples[x] = random.nextFloat() - 0.5f;
        }

        return samples;
    }

    /**
     * Test: a half-band filter uses the output array and produces the same results as an allocating filter.
     */
    @Test
    void halfBandOutputArray()
    {
        float[] coefficients = FilterFactory.getHalfBand(23, WindowType.BLACKMAN);
        RealHalfBandDecimationFilter allocating = new RealHalfBandDecimationFilter(coefficients);
        RealHalfBandDecimationFilter reusing = new RealHalfBandDecimationFilter(coefficients);
        float[] output = new float[1024];

        for(int block = 0; block < 4; block++)
        {
            float[] samples = samples(2048, block);
            assertSame(output, reusing.decimateReal(samples, output));
            assertArrayEquals(allocating.decimateReal(samples), output);
        }
    }

    /**
     * Test: consecutive results from a decimation cascade are independent arrays, since consumers retain them.
     */
    @Test
    void cascadeOutputIsNotReused()
    {
        IRealDecimationFilter filter = DecimationFilterFactory.getRealDecimationFilter(16);
        float[] first = filter.decimateReal(samples(2048, 1));
        float[] copy = first.clone();
        float[] second = filter.decimateReal(samples(2048, 2));

        assertNotSame(first, second);
        assertArrayEquals(copy, first);
    }

    /**
     * Test: mixing into reused arrays matches the allocating mix, and the input isn't modified.
     */
    @Test
    void mixerOutputArrays()
    {
        ComplexMixer allocating = new ScalarComplexMixer(100000, 2400000);
        ComplexMixer reusing = new ScalarComplexMixer(100000, 2400000);
        float[] i = new float[2048];
        float[] q = new float[2048];

        for(int block = 0; block < 4; block++)
        {
            ComplexSamples samples = new ComplexSamples(samples(2048, block), samples(2048, block + 10), block);
            float[] iOriginal = samples.i().clone();
            ComplexSamples expected = allocating.mix(samples);
            ComplexSamples mixed = reusing.mix(samples, i, q);

            assertSame(i, mixed.i());
            assertSame(q, mixed.q());
            assertEquals(block, mixed.timestamp());
            assertArrayEquals(expected.i(), mixed.i());
            assertArrayEquals(expected.q(), mixed.q());
            assertArrayEquals(iOriginal, samples.i());
        }
    }
}
