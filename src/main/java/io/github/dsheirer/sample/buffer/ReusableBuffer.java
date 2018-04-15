/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.sample.buffer;

import org.apache.commons.lang3.Validate;

import java.nio.FloatBuffer;

public class ReusableBuffer extends AbstractReusableBuffer
{
    private float[] mSamples;

    /**
     * Creates a reusable, timestamped complex buffer using the specified time in milliseconds.
     *
     * NOTE: reusability of this buffer requires strict user count tracking.  Each component that receives this
     * buffer should not modify the buffer contents.  Each component should also increment the user count before
     * sending this buffer to another component and should decrement the user count when finished using this buffer.
     *
     * @param bufferDisposedListener to be notified when all consumers/users are finished using the buffer
     * @param samples of data
     * @param timestamp in millis for the buffer
     */
    ReusableBuffer(IReusableBufferDisposedListener bufferDisposedListener, float[] samples, long timestamp)
    {
        super(bufferDisposedListener, timestamp);
        mSamples = samples;

        Validate.notNull(bufferDisposedListener, "Reusable Buffer Listener cannot be null");
    }

    /**
     * Samples for this buffer
     */
    public float[] getSamples()
    {
        return mSamples;
    }

    /**
     * Creates a copy of the samples from this buffer
     */
    public float[] getSamplesCopy()
    {
        float[] samples = getSamples();
        float[] copy = new float[samples.length];
        System.arraycopy(samples, 0, copy, 0, samples.length);
        return copy;
    }

    /**
     * Number of samples contained in this buffer
     */
    public int getSampleCount()
    {
        return getSamples().length;
    }

    /**
     * Constructs a timestamped complex buffer using the current system time in milliseconds.
     *
     * NOTE: reusability of this buffer requires strict user count tracking.  Each component that receives this
     * buffer should not modify the buffer contents.  Each component should also increment the user count before
     * sending this buffer to another component and should decrement the user count when finished using this buffer.
     *
     * @param bufferDisposedListener to be notified when all consumers are finished using the buffer
     * @param samples of data
     */
    public ReusableBuffer(IReusableBufferDisposedListener bufferDisposedListener, float[] samples)
    {
        this(bufferDisposedListener, samples, System.currentTimeMillis());
    }

    /**
     * Reloads this buffer by copying the sample array into this buffer.  Internal buffer length will be automatically
     * resized to fit the new sample array length.
     *
     * This is convenient if the source of the sample array originates via a native buffer.
     * @param samples to copy into this buffer
     * @param timestamp for the samples in millis
     */
    public void reloadFrom(float[] samples, long timestamp)
    {
        if(mUserCount.get() > 0)
        {
            throw new IllegalStateException("New data cannot be loaded into this reusable buffer while the user count " +
                "is above zero - user count:" + mUserCount.get());
        }

        mUserCount.set(0);

        if(mSamples.length != samples.length)
        {
            mSamples = new float[samples.length];
        }

        System.arraycopy(samples, 0, mSamples, 0, samples.length);

        mTimestamp = timestamp;
    }

    /**
     * Reloads this buffer by filling this buffer from the float buffer (ie copying the buffer contents).  Internal
     * buffer length will be automatically resized to accommodate the float buffer's capacity/size.
     *
     * @param floatBuffer to copy into this buffer
     * @param timestamp for the samples in millis
     */
    public void reloadFrom(FloatBuffer floatBuffer, long timestamp)
    {
        if(mUserCount.get() > 0)
        {
            throw new IllegalStateException("New data cannot be loaded into this reusable buffer while the user count " +
                "is above zero - user count:" + mUserCount.get());
        }

        mUserCount.set(0);

        if(mSamples.length != floatBuffer.capacity())
        {
            mSamples = new float[floatBuffer.capacity()];
        }

        floatBuffer.rewind();
        floatBuffer.get(mSamples);

        mTimestamp = timestamp;
    }

}
