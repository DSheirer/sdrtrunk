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

package io.github.dsheirer.dsp.filter.fir.complex;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.window.WindowType;
import org.apache.commons.lang3.ArrayUtils;

import java.text.DecimalFormat;
import java.util.Random;

public class ScalarComplexFIRFilter implements IComplexFilter
{
    private float[] mBuffer;
    private float[] mCoefficients;
    private int mBufferOverlap;

    /**
     * Float sample FIR filter base class.
     *
     * @param coefficients - filter coefficients in normal order.
     */
    public ScalarComplexFIRFilter(float[] coefficients)
    {
        //Reverse the order of the coefficients.
        ArrayUtils.reverse(coefficients);

        //Double the size of the coefficients array to align with the complex samples array
        mCoefficients = new float[coefficients.length * 2];
        for(int x = 0; x < coefficients.length; x++)
        {
            mCoefficients[2 * x] = coefficients[x];
        }

        mBufferOverlap = mCoefficients.length - 2;

        //We'll resize this later when we get the first sample buffer.  For now, make it non-null.
        mBuffer = new float[mCoefficients.length];
    }

    /**
     * Filters the sample array
     * @param samples to filter
     * @return filtered samples
     */
    public float[] filter(float[] samples)
    {
        int bufferLength = samples.length + mBufferOverlap;

        //Resize the data buffer if needed.  This shouldn't happen more than once since all buffers should be same size
        if(mBuffer.length != bufferLength)
        {
            float[] temp = new float[bufferLength];
            //Move residual samples from end of previous buffer to the beginning of the new temp buffer and reassign
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

        float[] filtered = new float[samples.length];

        int offset = 0;

        float iAccumulator, qAccumulator;

        for(int bufferPointer = 0; bufferPointer < samples.length; bufferPointer += 2)
        {
            iAccumulator = 0.0f;
            qAccumulator = 0.0f;

            for(int coefficientPointer = 0; coefficientPointer < mCoefficients.length; coefficientPointer += 2)
            {
                offset = bufferPointer + coefficientPointer;
                iAccumulator += mBuffer[offset] * mCoefficients[coefficientPointer];
                qAccumulator += mBuffer[offset + 1] * mCoefficients[coefficientPointer];
            }

            filtered[bufferPointer] = iAccumulator;
            filtered[bufferPointer + 1] = qAccumulator;
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

        float[] coefficients = FilterFactory.getLowPass(1000, 250, 99, WindowType.BLACKMAN);

//        ComplexFIRFilter2 legacyFilter = new ComplexFIRFilter2(coefficients);
        ScalarComplexFIRFilter filter = new ScalarComplexFIRFilter(coefficients);
        VectorComplexFIRFilter128Bit vectorFilter = new VectorComplexFIRFilter128Bit(coefficients);

        double accumulator = 0.0d;

        int iterations = 1_000_000;

        long start = System.currentTimeMillis();

        for(int x = 0; x < iterations; x++)
        {
//            float[] filtered = legacyFilter.filter(samples);
//            float[] filtered = filter.filter(samples);
            float[] filtered = vectorFilter.filter(samples);
//            float[] vfiltered = vectorFilter.filter(samples);
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
