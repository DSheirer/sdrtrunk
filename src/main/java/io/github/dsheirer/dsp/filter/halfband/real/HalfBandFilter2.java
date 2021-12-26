/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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
package io.github.dsheirer.dsp.filter.halfband.real;

import io.github.dsheirer.buffer.FloatCircularBuffer;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;

import java.util.Random;

/**
 * Implements a half-band filter that produces one filtered output for every two input samples.
 *
 * This filter is structured for Java 8+ compiler JIT optimization for SIMD instructions when
 * supported by the host CPU.
 */
public class HalfBandFilter2
{
    private static final float CENTER_COEFFICIENT = 0.5f;
    private float[] mCoefficients;
    private float[] mEvenSamples;
    private float mAccumulator;
    private int mPointer;
    private FloatCircularBuffer mCircularBuffer;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public HalfBandFilter2(float[] coefficients)
    {
        if((coefficients.length + 1) % 4 != 0)
        {
            throw new IllegalArgumentException("Half-band filter coefficients must be odd-length and symmetrical (length = [x * 4 + 1]");
        }

        int half = coefficients.length / 2 + 1;

        mCircularBuffer = new FloatCircularBuffer(half / 2);
        mCoefficients = new float[half];
        mEvenSamples = new float[half];

        //Use only the even coefficients since the odd coefficients are all zero-valued
        for(int x = 0; x < coefficients.length; x += 2)
        {
            mCoefficients[x / 2] = coefficients[x];
        }
    }

    public float filter(float sample1, float sample2)
    {
        //Structured for SIMD array copy optimization when supported by host CPU
        System.arraycopy(mEvenSamples, 0, mEvenSamples, 1, mEvenSamples.length - 1);

        mEvenSamples[0] = sample1;

        mAccumulator = 0.0f;

        //Structured for SIMD dot.product optimization when supported by host CPU
        for(mPointer = 0; mPointer < mEvenSamples.length; mPointer++)
        {
            mAccumulator += (mEvenSamples[mPointer] * mCoefficients[mPointer]);
        }

        mAccumulator += (mCircularBuffer.getAndPut(sample2) * CENTER_COEFFICIENT);

        return mAccumulator;
    }

    public float[] filter(float[] samples)
    {
        float[] filtered = new float[samples.length / 2];
        for(int x = 0; x < samples.length; x += 2)
        {
            filtered[x / 2] = filter(samples[x], samples[x + 1]);
        }

        return filtered;
    }

    public static void main(String[] args)
    {
        System.out.println("Starting ...");
        float[] taps = FilterFactory.getHalfBand(99, Window.WindowType.HAMMING);

        HalfBandFilter2 halfband = new HalfBandFilter2(taps);

        Random random = new Random();
        float[] samples = new float[1024];
        float[] filtered = new float[512];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = (2.0f * random.nextFloat()) - 1.0f;
        }

        System.out.println("Testing ...");
        long current = System.currentTimeMillis();

        int iterations = 10_000_000;

        for(int x = 0; x < iterations; x++)
        {
            for(int y = 0; y < samples.length; y += 2)
            {
                filtered[y / 2] = halfband.filter(samples[y], samples[y + 1]);
            }
        }

        long elapsed = System.currentTimeMillis() - current;

        System.out.println("Elapsed: " + (elapsed / 1000d) + " seconds");
    }
}
