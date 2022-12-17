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

package io.github.dsheirer.dsp.filter.hilbert;

import io.github.dsheirer.dsp.oscillator.IRealOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.sample.SampleUtils;
import io.github.dsheirer.sample.complex.ComplexSamples;
import java.util.Arrays;

/**
 * Implements a Hilbert transform using scalar operations against a pre-defined 47-tap filter.
 * As described in Understanding Digital Signal Processing, Lyons, 3e, 2011, Section 13.37.2 (p 804-805)
 */
public class ScalarHilbertTransform extends HilbertTransform
{
    /**
     * Filters the real samples array into complex samples at half the sample rate.
     * @param realSamples to filter
     * @param timestamp of the first sample
     * @return complex samples at half the sample rate
     */
    public ComplexSamples filter(float[] realSamples, long timestamp)
    {
        int bufferLength = realSamples.length / 2;

        //Resize the I and Q delay buffers to incoming buffer length plus overlap, if necessary.
        if(mIBuffer.length != (bufferLength + mIOverlap))
        {
            float[] iTemp = new float[bufferLength + mIOverlap];
            float[] qTemp = new float[bufferLength + mQOverlap];
            System.arraycopy(mIBuffer, 0, iTemp, 0, mIOverlap);
            System.arraycopy(mQBuffer, 0, qTemp, 0, mQOverlap);
            mIBuffer = iTemp;
            mQBuffer = qTemp;
        }

        ComplexSamples deinterleaved = SampleUtils.deinterleave(realSamples, timestamp);
        System.arraycopy(deinterleaved.i(), 0, mIBuffer, mIOverlap, deinterleaved.i().length);
        System.arraycopy(deinterleaved.q(), 0, mQBuffer, mQOverlap, deinterleaved.q().length);

        float[] i = new float[bufferLength];
        float[] q = new float[bufferLength];

        float accumulator;

        for(int x = 0; x < bufferLength; x++)
        {
            accumulator = 0.0f;

            for(int y = 0; y < mCoefficients.length; y++)
            {
                accumulator += mCoefficients[y] * mQBuffer[x + y];
            }

            i[x] = mIBuffer[x]; //Simple delay assignment
            q[x] = accumulator;
        }

        //Copy residual from end of delay buffers to beginning for next iteration
        System.arraycopy(mIBuffer, mIBuffer.length - mIOverlap, mIBuffer, 0, mIOverlap);
        System.arraycopy(mQBuffer, mQBuffer.length - mQOverlap, mQBuffer, 0, mQOverlap);

        return new ComplexSamples(i, q, timestamp);
    }

    public static void main(String[] args)
    {
        int iterations = 1_000_000;
        int bufferSize = 2048;

        IRealOscillator realOscillator = OscillatorFactory.getRealOscillator(2, 40);
        float[] realSamples = realOscillator.generate(bufferSize);
        ScalarHilbertTransform hilbertTransform = new ScalarHilbertTransform();
        VectorHilbertTransformDefaultBits vectorDefault = new VectorHilbertTransformDefaultBits();
        VectorHilbertTransform512Bits vector512 = new VectorHilbertTransform512Bits();
        VectorHilbertTransform256Bits vector256 = new VectorHilbertTransform256Bits();
        VectorHilbertTransform128Bits vector128 = new VectorHilbertTransform128Bits();
        VectorHilbertTransform64Bits vector64 = new VectorHilbertTransform64Bits();

        System.out.println("Starting ...");

        float accumulator = 0.0f;
        long start = System.currentTimeMillis();

        boolean validate = true;

        if(validate)
        {
            ComplexSamples scalar = hilbertTransform.filter(realSamples, 0l);
            ComplexSamples vector = vector512.filter(realSamples, start);

            System.out.println("SI: " + Arrays.toString(scalar.i()));
            System.out.println("VI: " + Arrays.toString(vector.i()));

            System.out.println("SQ: " + Arrays.toString(scalar.q()));
            System.out.println("VQ: " + Arrays.toString(vector.q()));
        }
        else
        {
            for(int count = 0; count < iterations; count++)
            {
//            ComplexSamples filtered = hilbertTransform.filter(realSamples);
//            ComplexSamples filtered = vectorDefault.filter(realSamples);
                ComplexSamples filtered = vector512.filter(realSamples, start);
//            ComplexSamples filtered = vector256.filter(realSamples);
//            ComplexSamples filtered = vector128.filter(realSamples);
//            ComplexSamples filtered = vector64.filter(realSamples);

                accumulator += filtered.q()[2];
            }

            System.out.println("Accumulator: " + accumulator);
            System.out.println("Elapsed: " + ((System.currentTimeMillis() - start) / 1000.0f) + " seconds");
        }

        System.out.println("Finished!");

    }
}
