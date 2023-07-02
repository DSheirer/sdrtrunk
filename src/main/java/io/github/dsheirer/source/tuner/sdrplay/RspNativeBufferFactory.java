/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Native buffer factory for SDRPlay RSP tuners.
 *
 * The SDRPlay API automatically changes the length of sample buffers according to the sample rate.  This causes
 * problems for down-stream processing components that may be optimized for vector operations and depend on the
 * sample arrays being a power-of-2 length.  This class repackages the incoming sample stream into arrays of
 * power-of-2 length.
 */
public class RspNativeBufferFactory
{
    private RspSampleRate mRspSampleRate;
    private short[] mIResidual = new short[0];
    private short[] mQResidual = new short[0];
    private long mResidualTimestamp = System.currentTimeMillis();
    private int mIncomingBufferLength = 0;
    private int mOptimalBufferLength = 128;
    private float mSamplesPerMillisecond;

    /**
     * Constructs an instance.
     * @param sampleRate of the tuner
     */
    public RspNativeBufferFactory(RspSampleRate sampleRate)
    {
        setSampleRate(sampleRate);
    }

    /**
     * Set or update the sample rate.
     * @param sampleRate to set.
     */
    public void setSampleRate(RspSampleRate sampleRate)
    {
        mRspSampleRate = sampleRate;
        mSamplesPerMillisecond = sampleRate.getSamplesPerMillisecond();
    }

    /**
     * Repackages the samples into optimal length buffers and returns zero or more RSP native buffers.
     * @param i samples
     * @param q samples
     * @param timestamp of samples
     * @return zero or more repackaged RSP native buffers
     */
    public List<RspNativeBuffer> get(short[] i, short[] q, long timestamp)
    {
        updateBufferLength(i.length);

        short[] iCombined = new short[mIResidual.length + i.length];
        System.arraycopy(mIResidual, 0, iCombined, 0, mIResidual.length);
        System.arraycopy(i, 0, iCombined, mIResidual.length, i.length);

        short[] qCombined = new short[mQResidual.length + q.length];
        System.arraycopy(mQResidual, 0, qCombined, 0, mQResidual.length);
        System.arraycopy(q, 0, qCombined, mQResidual.length, q.length);

        if(iCombined.length < mOptimalBufferLength)
        {
            mIResidual = iCombined;
            mQResidual = qCombined;
            return Collections.emptyList();
        }

        List<RspNativeBuffer> buffers = new ArrayList<>();

        while(iCombined.length >= mOptimalBufferLength)
        {
            short[] iOptimal = Arrays.copyOf(iCombined, mOptimalBufferLength);
            iCombined = Arrays.copyOfRange(iCombined, mOptimalBufferLength, iCombined.length);

            short[] qOptimal = Arrays.copyOf(qCombined, mOptimalBufferLength);
            qCombined = Arrays.copyOfRange(qCombined, mOptimalBufferLength, qCombined.length);

            RspNativeBuffer buffer = new RspNativeBuffer(iOptimal, qOptimal, mResidualTimestamp, mSamplesPerMillisecond);
            buffers.add(buffer);
            mResidualTimestamp += (long)(mOptimalBufferLength / mSamplesPerMillisecond);
        }

        mIResidual = iCombined;
        mQResidual = qCombined;

        //For simplicity, just update the residual timestamp to be the timestamp for this latest update
        mResidualTimestamp = timestamp;

        return buffers;
    }

    /**
     * Updates the optimal native buffer length based on the incoming buffer size.  Optimal length is a power-of-2
     * value that is closest to the buffer length to minimize the quantity of residual samples from each arriving buffer.
     * @param length
     */
    private void updateBufferLength(int length)
    {
        if(mIncomingBufferLength != length)
        {
            int optimal = 128;

            while((optimal) < length)
            {
                optimal *= 2;
            }

            mOptimalBufferLength = optimal;
            mIncomingBufferLength = length;
        }
    }
}
