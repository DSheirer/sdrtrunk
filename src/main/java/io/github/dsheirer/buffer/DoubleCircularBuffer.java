/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.buffer;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;

import java.util.Arrays;

/**
 * Circular sample buffer - allocates a buffer and stores samples in a circular fashion, overwriting older samples with
 * newly arrived samples.  Initially fills buffer with 0-valued (default) samples or with a specified value.
 */
public class DoubleCircularBuffer
{
    private StandardDeviation mStandardDeviation;
    private Variance mVariance;
    private Max mMax;
    private Mean mMean;
    private Min mMin;
    private double[] mBuffer;
    private int mBufferPointer = 0;

    /**
     * Creates a circular double buffer of the specified size and all entries filled with the specified initial value.
     * @param size of the buffer
     * @param initialFillValue to fill the buffer entries
     */
    public DoubleCircularBuffer(int size, double initialFillValue)
    {
        mBuffer = new double[size];
        Arrays.fill(mBuffer, initialFillValue);
    }

    /**
     * Creates a circular double buffer of the specified size and all entries filled with an initial value of zero.
     * @param size of the buffer
     */
    public DoubleCircularBuffer(int size)
    {
        this(size, 0.0);
    }

    /**
     * Puts the new value into the buffer and returns the oldest buffer value that it replaced
     *
     * @param newValue to add to the buffer
     * @return oldest value that was overwritten by the new value
     */
    public double get(double newValue)
    {
        double oldestSample = mBuffer[mBufferPointer];

        mBuffer[mBufferPointer] = newValue;

        mBufferPointer++;

        if(mBufferPointer >= mBuffer.length)
        {
            mBufferPointer = 0;
        }

        return oldestSample;
    }

    public double[] getAll()
    {
        int valuePointer = 0;
        double[] values = new double[mBuffer.length];
        int bufferPointer = mBufferPointer;

        while(valuePointer < values.length)
        {
            values[valuePointer++] = mBuffer[bufferPointer++];

            if(bufferPointer >= mBuffer.length)
            {
                bufferPointer = 0;
            }
        }

        return values;
    }

    /**
     * Places the new value into the buffer, overwriting the oldest value
     * @param newValue to add to the buffer
     */
    public void put(double newValue)
    {
        mBuffer[mBufferPointer] = newValue;

        mBufferPointer++;

        if(mBufferPointer >= mBuffer.length)
        {
            mBufferPointer = 0;
        }
    }

    /**
     * Returns the maximum from the values currently in the buffer
     */
    public double max()
    {
        if(mMax == null)
        {
            mMax = new Max();
        }

        return mMax.evaluate(mBuffer, 0, mBuffer.length);
    }

    /**
     * Returns the minimum from the values currently in the buffer
     */
    public double min()
    {
        if(mMin == null)
        {
            mMin = new Min();
        }

        return mMin.evaluate(mBuffer, 0, mBuffer.length);
    }

    public double standardDeviation()
    {
        if(mStandardDeviation == null)
        {
            mStandardDeviation = new StandardDeviation();
        }

        return mStandardDeviation.evaluate(mBuffer);
    }

    /**
     * Calculates the variance of the values contained in this buffer
     */
    public double variance()
    {
        if(mVariance == null)
        {
            mVariance = new Variance();
        }

        return mVariance.evaluate(mBuffer);
    }

    /**
     * Calculates the average/mean of the values contained in this buffer
     */
    public double mean()
    {
        if(mMean == null)
        {
            mMean = new Mean();
        }

        return mMean.evaluate(mBuffer, 0, mBuffer.length);
    }
}
