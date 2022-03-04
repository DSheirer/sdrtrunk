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
package io.github.dsheirer.dsp.filter.halfband;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.window.WindowType;

import java.text.DecimalFormat;
import java.util.Random;

/**
 * Implements a half-band filter that produces one filtered output for every two input samples.
 *
 * This filter is structured for Java 8+ compiler JIT optimization for SIMD instructions when
 * supported by the host CPU.
 *
 * Note: this class is structured to process an entire float array, versus processing one sample at a time from the
 * array.
 */
public class RealHalfBandDecimationFilter implements IRealDecimationFilter
{
    private static final float CENTER_COEFFICIENT = 0.5f;
    private float[] mCoefficients;
    private float[] mBuffer;
    private int mBufferOverlap;

    /**
     * Creates a half band filter with inherent decimation by two.
     *
     * @param coefficients of the half-band filter that is odd-length where all odd index coefficients are
     * zero valued except for the middle odd index coefficient which should be valued 0.5
     */
    public RealHalfBandDecimationFilter(float[] coefficients)
    {
        if((coefficients.length + 1) % 4 != 0)
        {
            throw new IllegalArgumentException("Half-band filter coefficients must be odd-length and symmetrical (length = [x * 4 + 1]");
        }

        mCoefficients = coefficients;
        mBufferOverlap = mCoefficients.length - 1;
    }

    public float[] decimateReal(float[] samples)
    {
        if(samples.length % 2 != 0)
        {
            throw new IllegalArgumentException("Samples array length must be an integer multiple of 2");
        }

        int bufferLength = samples.length + mBufferOverlap;

        if(mBuffer == null)
        {
            mBuffer = new float[bufferLength];
        }
        else if(mBuffer.length != bufferLength)
        {
            float[] temp = new float[bufferLength];
            //Move residual samples from end of old buffer to the beginning of the new temp buffer
            System.arraycopy(mBuffer, mBuffer.length - mBufferOverlap, temp, 0, mBufferOverlap);
            mBuffer = temp;
        }
        else
        {
            //Move residual samples from end of buffer to the beginning of the buffer
            System.arraycopy(mBuffer, samples.length, mBuffer, 0, mBufferOverlap);
        }

        //Copy new sample array into end of buffer
        System.arraycopy(samples, 0, mBuffer, mBufferOverlap, samples.length);

        float[] filtered = new float[samples.length / 2];

        float accumulator = 0.0f;
        int half = mBufferOverlap / 2;

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 2)
        {
            accumulator = 0.0f;

            for(int coefficientPointer = 0; coefficientPointer < half; coefficientPointer += 2)
            {
                //Half band filter coefficients are mirrored, so we add the mirrored samples and then multiply by
                //one of the coefficients to achieve the same effect.
                accumulator += mCoefficients[coefficientPointer] *
                        (mBuffer[bufferPointer + coefficientPointer] +
                                mBuffer[bufferPointer + (mBufferOverlap - coefficientPointer)]);
            }

            accumulator += mBuffer[bufferPointer + half] * CENTER_COEFFICIENT;

            filtered[bufferPointer / 2] = accumulator;
        }

        return filtered;
    }

    public static void main(String[] args)
    {
        Random random = new Random();

        int sampleSize = 2048;

        float[] samples = new float[sampleSize];
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = random.nextFloat() * 2.0f - 1.0f;
        }

        float[] coefficients = FilterFactory.getHalfBand(63, WindowType.BLACKMAN);

//        RealHalfBandDecimationFilter filter = new RealHalfBandDecimationFilter(coefficients);
//        VectorRealHalfBandDecimationFilter63Tap64Bit vectorFilter64 = new VectorRealHalfBandDecimationFilter63Tap64Bit(coefficients);
//        VectorRealHalfBandDecimationFilter63Tap128Bit vectorFilter128 = new VectorRealHalfBandDecimationFilter63Tap128Bit(coefficients);
//        VectorRealHalfBandDecimationFilter63Tap256Bit vectorFilter256 = new VectorRealHalfBandDecimationFilter63Tap256Bit(coefficients);
//        VectorRealHalfBandDecimationFilter23Tap512Bit vectorFilter512 = new VectorRealHalfBandDecimationFilter23Tap512Bit(coefficients);
        VectorRealHalfBandDecimationFilter256Bit vectorFilter = new VectorRealHalfBandDecimationFilter256Bit(coefficients);

        double accumulator = 0.0d;

        int iterations = 10_000_000;

        long start = System.currentTimeMillis();

        for(int x = 0; x < iterations; x++)
        {
//            float[] filtered = filter.decimateReal(samples);
//            float[] filtered = vectorFilter256.decimateReal(samples);
            float[] filtered = vectorFilter.decimateReal(samples);
//            float[] vfiltered = vectorFilterGeneric.decimateReal(samples);
            accumulator += filtered[3];
        }

//        System.out.println("REG:" + Arrays.toString(filtered));
//        System.out.println("VEC:" + Arrays.toString(vfiltered));
        double elapsed = System.currentTimeMillis() - start;

        DecimalFormat df = new DecimalFormat("0.000");
        System.out.println("Accumulator: " + accumulator);
        System.out.println("Test Complete.  Elapsed Time: " + df.format(elapsed / 1000.0d) + " seconds");
    }
}
