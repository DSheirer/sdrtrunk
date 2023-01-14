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

package io.github.dsheirer.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Implements a factory for creating SignedByteNativeBuffer instances
 */
public class SignedByteNativeBufferFactory extends AbstractNativeBufferFactory
{
    /**
     * DC removal calculations will run once a minute
     */
    private static final long DC_PROCESSING_INTERVAL = TimeUnit.SECONDS.toMillis(60);

    /**
     * Number of buffers to process per DC calculation processing interval.
     */
    private static final int DC_CALCULATIONS_PER_INTERVAL = 5;

    /**
     * DC offset processing residual threshold for ending DC processing interval
      */
    private static final float TARGET_DC_OFFSET_REMAINING = 0.0002f;

    /**
     * Number of DC calculations remaining in the current interval
     */
    private int mDcCalculationsRemaining = DC_CALCULATIONS_PER_INTERVAL; //Initial value for coarse correction

    /**
     * Last time DC offset calculation interval ran
     */
    private long mLastDcCalculationTimestamp = 0;

    /**
     * DC offset calculation gain.
     */
    private static final float DC_FILTER_GAIN = 0.05f;

    /**
     * Current DC offset correction for Inphase samples
     */
    private float mIAverageDc = 0.0f;

    /**
     * Current DC offset correction for Quadrature samples
     */
    private float mQAverageDc = 0.0f;

    @Override
    public INativeBuffer getBuffer(ByteBuffer samples, long timestamp)
    {
        byte[] copy = new byte[samples.capacity()];
        samples.get(copy);

        if(shouldCalculateDc())
        {
            calculateDc(copy);
        }

        return new SignedByteNativeBuffer(copy, timestamp, mIAverageDc, mQAverageDc, getSamplesPerMillisecond());
    }

    /**
     * Indicates if a DC offset calculation for a buffer should be performed.
     */
    private boolean shouldCalculateDc()
    {
        if(System.currentTimeMillis() > (mLastDcCalculationTimestamp + DC_PROCESSING_INTERVAL))
        {
            if(mDcCalculationsRemaining > 0)
            {
                return true;
            }
            else
            {
                //Reset the number of buffers to calculate and the timestamp
                mDcCalculationsRemaining = DC_CALCULATIONS_PER_INTERVAL;
                mLastDcCalculationTimestamp = System.currentTimeMillis();
            }
        }

        return false;
    }

    /**
     * Calculates the average DC in the sample stream so that it can be subtracted from the samples when the
     * native buffer is used.
     * @param samples containing DC offset
     */
    private void calculateDc(byte[] samples)
    {
        float iDcAccumulator = 0;
        float qDcAccumulator = 0;

        for(int x = 0; x < samples.length; x += 2)
        {
            iDcAccumulator += samples[x];
            qDcAccumulator += samples[x + 1];
        }

        iDcAccumulator /= (samples.length / 2);
        iDcAccumulator /= 128.0f;
        iDcAccumulator -= mIAverageDc;
        mIAverageDc += (iDcAccumulator * DC_FILTER_GAIN);

        qDcAccumulator /= (samples.length / 2);
        qDcAccumulator /= 128.0f;
        qDcAccumulator -= mQAverageDc;
        mQAverageDc += (qDcAccumulator * DC_FILTER_GAIN);

        if(Math.abs(iDcAccumulator) < TARGET_DC_OFFSET_REMAINING && Math.abs(qDcAccumulator) < TARGET_DC_OFFSET_REMAINING)
        {
            mDcCalculationsRemaining--;
        }
    }
}
