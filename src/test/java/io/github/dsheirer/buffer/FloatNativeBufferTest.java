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

package io.github.dsheirer.buffer;

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the float native buffer.
 */
public class FloatNativeBufferTest
{
    private static final int SAMPLE_COUNT = 5000;

    /**
     * Creates interleaved complex samples where each value is unique and non-zero.
     */
    private static float[] getSamples()
    {
        float[] samples = new float[SAMPLE_COUNT * 2];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = x + 1;
        }

        return samples;
    }

    /**
     * Test: the complex samples iterator returns every sample exactly once, with a final chunk that holds the remaining
     * samples rather than padding them to the full chunk length with zero valued samples.
     */
    @Test
    void testComplexSamplesIterator()
    {
        float[] samples = getSamples();
        List<Integer> lengths = new ArrayList<>();
        int pointer = 0;

        Iterator<ComplexSamples> iterator = new FloatNativeBuffer(samples, 0L, 0.0f).iterator();

        while(iterator.hasNext())
        {
            ComplexSamples chunk = iterator.next();
            lengths.add(chunk.i().length);

            for(int x = 0; x < chunk.i().length; x++)
            {
                assertEquals(samples[pointer++], chunk.i()[x], "Inphase sample " + (pointer / 2));
                assertEquals(samples[pointer++], chunk.q()[x], "Quadrature sample " + (pointer / 2));
            }
        }

        assertEquals(samples.length, pointer, "Sample count");
        assertEquals(List.of(2048, 2048, 904), lengths, "Chunk lengths");
    }

    /**
     * Test: the interleaved complex samples iterator returns every sample exactly once, without zero padding.
     */
    @Test
    void testInterleavedComplexSamplesIterator()
    {
        float[] samples = getSamples();
        List<Integer> lengths = new ArrayList<>();
        int pointer = 0;

        Iterator<InterleavedComplexSamples> iterator = new FloatNativeBuffer(samples, 0L, 0.0f).iteratorInterleaved();

        while(iterator.hasNext())
        {
            float[] chunk = iterator.next().samples();
            lengths.add(chunk.length / 2);

            for(float sample : chunk)
            {
                assertEquals(samples[pointer], sample, "Sample " + pointer);
                pointer++;
            }
        }

        assertEquals(samples.length, pointer, "Sample count");
        assertEquals(List.of(2048, 2048, 904), lengths, "Chunk lengths");
    }
}
