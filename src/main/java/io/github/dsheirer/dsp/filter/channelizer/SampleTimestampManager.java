/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.dsp.filter.channelizer;

public class SampleTimestampManager
{
    private long mReferenceTimestamp = System.currentTimeMillis();
    private double mSamplesPerMilliSecond;
    private long mSampleCount;

    /**
     * Tracks the number of samples produced relative to a reference timestamp to derive a current reference timestamp
     * for use in generating sample buffers.
     *
     * @param sampleRate per second
     */
    public SampleTimestampManager(double sampleRate)
    {
        setSampleRate(sampleRate);
    }

    /**
     * Updates the internal reference timestamp and resets the sample counter to zero.
     *
     * @param referenceTimestamp in milli-seconds since epoch
     */
    public void setReferenceTimestamp(long referenceTimestamp)
    {
        mReferenceTimestamp = referenceTimestamp;
        mSampleCount = 0;
    }

    /**
     * Updates the sample rate that is used in calculating the reference timestamp
     *
     * @param sampleRate
     */
    public void setSampleRate(double sampleRate)
    {
        //Convert to samples per milli-second for compatibility with reference timestamp
        mSamplesPerMilliSecond = sampleRate / 1000.0d;
    }

    /**
     * Increments the internal count of dispatched samples to use in calculating an accurate reference timestamp.
     */
    public void increment()
    {
        mSampleCount++;
    }

    /**
     * Increments the internal count of dispatched samples to use in calculating an accurate reference timestamp.
     *
     * @param value to add to the sample count
     */
    public void increment(int value)
    {
        mSampleCount += value;
    }

    public long getCurrentTimestamp()
    {
        if(mSampleCount > 0)
        {
            return mReferenceTimestamp + (long)((double)mSampleCount / mSamplesPerMilliSecond);
        }

        return mReferenceTimestamp;
    }
}
