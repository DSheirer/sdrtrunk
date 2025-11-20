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

package io.github.dsheirer.module.decode.nxdn;

import java.util.Arrays;

/**
 * Sample averaging filter provides averaging of sample values over the length of the window size.
 */
public class AveragingFilter
{
    private float[] mBuffer;
    private int mWindowSize;
    private float mAccumulator;

    /**
     * Creates an instance
     * @param windowSize for averaging.
     */
    public AveragingFilter(int windowSize)
    {
        mWindowSize = windowSize;

        //Initialize the working buffer to window size and then we'll resize on the first incoming buffer.
        mBuffer = new float[mWindowSize];
    }

    /**
     * Filters the sample array using moving average filter.
     * @param samples
     * @return
     */
    public float[] filter(float[] samples)
    {
        float accumulator = mAccumulator;
        int windowSize = mWindowSize;

        //Transfer the residual samples from the end of the buffer to the front
        System.arraycopy(mBuffer, mBuffer.length - mWindowSize, mBuffer, 0, mWindowSize);

        //Resize the buffer if necessary
        if(mBuffer.length != (samples.length) + mWindowSize)
        {
            mBuffer = Arrays.copyOf(mBuffer, samples.length + mWindowSize);
        }

        //Copy the incoming samples to the buffer
        System.arraycopy(samples, 0, mBuffer, mWindowSize, samples.length);

        for(int x = 0; x < samples.length; x++)
        {
            accumulator -= mBuffer[x];
            accumulator += mBuffer[x + windowSize];
            samples[x] = accumulator / windowSize;
        }

        mAccumulator = accumulator;

        return samples;
    }

    static void main()
    {
        AveragingFilter av = new AveragingFilter(5);
        float[] samples = new float[25];
        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = x;
        }

        System.out.println("Samples  In:" + Arrays.toString(samples));
        samples = av.filter(samples);
        System.out.println("Samples Out:" + Arrays.toString(samples));

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = 25 + x;
        }

        System.out.println("Samples  In:" + Arrays.toString(samples));
        samples = av.filter(samples);
        System.out.println("Samples Out:" + Arrays.toString(samples));
    }
}
