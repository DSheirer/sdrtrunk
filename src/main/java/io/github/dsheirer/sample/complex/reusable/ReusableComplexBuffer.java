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
package io.github.dsheirer.sample.complex.reusable;

import io.github.dsheirer.sample.complex.ComplexBuffer;
import org.apache.commons.lang3.Validate;

import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class ReusableComplexBuffer extends ComplexBuffer
{
    private IReusableBufferDisposedListener mDisposalListener;
    private long mTimestamp;
    private AtomicInteger mUserCount = new AtomicInteger();

    /**
     * Creates a reusable, timestamped complex buffer using the specified time in milliseconds.
     *
     * NOTE: reusability of this buffer requires strict user count tracking.  Each component that receives this
     * buffer should not modify the buffer contents.  Each component should also increment the user count before
     * sending this buffer to another component and should decrement the user count when finished using this buffer.
     *
     * @param disposalListener to be notified when all consumers/users are finished using the buffer
     * @param samples of data
     * @param timestamp in millis for the buffer
     */
    public ReusableComplexBuffer(IReusableBufferDisposedListener disposalListener, float[] samples, long timestamp)
    {
        super(samples);

        Validate.notNull(disposalListener, "Timestamped Buffer Listener cannot be null");
        mDisposalListener = disposalListener;
        mTimestamp = timestamp;
    }

    /**
     * Constructs a timestamped complex buffer using the current system time in milliseconds.
     *
     * NOTE: reusability of this buffer requires strict user count tracking.  Each component that receives this
     * buffer should not modify the buffer contents.  Each component should also increment the user count before
     * sending this buffer to another component and should decrement the user count when finished using this buffer.
     *
     * @param disposalListener to be notified when all consumers are finished using the buffer
     * @param samples of data
     */
    public ReusableComplexBuffer(IReusableBufferDisposedListener disposalListener, float[] samples)
    {
        this(disposalListener, samples, System.currentTimeMillis());
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
     * Number of complex samples contained in this buffer
     */
    public int getSampleLength()
    {
        return getSamples().length / 2;
    }

    /**
     * Reference timestamp for this complex buffer.  Timestamp is relative to the first sample in the buffer.
     * @return timestamp as milliseconds since epoch
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }

    public void setTimestamp(long timestamp)
    {
        mTimestamp = timestamp;
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

    /**
     * Sets the number of users that will receive this buffer.  Each user is expected to invoke the
     * decrementUserCount() method to indicate that they are finished using this buffer so that this buffer can be
     * reused once the user count is zero.
     *
     * @param userCount that will receive this reusable buffer
     */
    public void setUserCount(int userCount)
    {
        mUserCount.set(userCount);
    }

    /**
     * Decrements the user count for this buffer to indicate that the user/receiver of this buffer is finished using
     * the buffer.
     *
     * User count is established prior to sending this buffer to each listener and each listener invokes this method to
     * indicate that they have finished processing the buffer so that when the user count reaches zero, this buffer
     * can be reused.
     *
     * This method is thread-safe.
     */
    public ReusableComplexBuffer decrementUserCount()
    {
        int currentCount = mUserCount.decrementAndGet();

        if(currentCount == 0)
        {
            mDisposalListener.disposed(this);
        }
        else if(currentCount < 0)
        {
            throw new IllegalStateException("User count is below zero.  This indicates that this buffer's decrement" +
                " user count was invoked by more than the expected user count");
        }

        return this;
    }

    /**
     * Increments the user count to indicate that this buffer will be sent to another user.
     *
     * Users/receivers of this buffer that wish to forward the buffer to additional users should increment the expected
     * user count prior to invoking the decrementUserCount() to indicate that the original receiver of the buffer is
     * finished to avoid the user count reaching zero and the buffer being flagged as ready for reuse.
     *
     * This method is thread-safe.
     */
    public ReusableComplexBuffer incrementUserCount()
    {
        mUserCount.incrementAndGet();
        return this;
    }

    /**
     * Increments the user count by the specified count to indicate that this buffer will be sent to additional users.
     *
     * Users/receivers of this buffer that wish to forward the buffer to additional users should increment the expected
     * user count prior to invoking the decrementUserCount() to indicate that the original receiver of the buffer is
     * finished to avoid the user count reaching zero and the buffer being flagged as ready for reuse.
     *
     * This method is thread-safe.
     *
     * @param additionalUserCount to add to the current user count
     */
    public ReusableComplexBuffer incrementUserCount(int additionalUserCount)
    {
        mUserCount.addAndGet(additionalUserCount);
        return this;
    }
}
