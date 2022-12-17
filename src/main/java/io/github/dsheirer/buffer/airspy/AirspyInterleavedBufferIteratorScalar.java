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

package io.github.dsheirer.buffer.airspy;

import io.github.dsheirer.sample.complex.InterleavedComplexSamples;

/**
 * Scalar implementation for non-packed Airspy native buffers
 */
public class AirspyInterleavedBufferIteratorScalar extends AirspyBufferIterator<InterleavedComplexSamples>
{
    /**
     * Constructs an instance
     *
     * @param samples from the airspy, either packed or unpacked.
     * @param residualI samples from last buffer
     * @param residualQ samples from last buffer
     * @param averageDc measured
     * @param timestamp of the buffer
     * @param samplesPerMillisecond to calculate sub-buffer fragment timestamps
     */
    public AirspyInterleavedBufferIteratorScalar(short[] samples, short[] residualI, short[] residualQ, float averageDc,
                                                 long timestamp, float samplesPerMillisecond)
    {
        super(samples, residualI, residualQ, averageDc, timestamp, samplesPerMillisecond);
    }

    @Override
    public InterleavedComplexSamples next()
    {
        if(mSamplesPointer >= mSamples.length)
        {
            throw new IllegalStateException("End of buffer exceeded");
        }

        long timestamp = getFragmentTimestamp(mSamplesPointer);
        int offset = mSamplesPointer;

        for(int x = 0; x < FRAGMENT_SIZE; x++)
        {
            mIBuffer[x + I_OVERLAP] = scale(mSamples[offset++], mAverageDc);
            mQBuffer[x + Q_OVERLAP] = scale(mSamples[offset++], mAverageDc);
        }

        mSamplesPointer = offset;

        float[] samples = new float[FRAGMENT_SIZE * 2];

        float accumulator;

        for(int x = 0; x < FRAGMENT_SIZE; x++)
        {
            accumulator = 0;

            for(int tap = 0; tap < COEFFICIENTS.length; tap++)
            {
                accumulator += COEFFICIENTS[tap] * mQBuffer[x + tap];
            }

            //Perform FS/2 frequency translation on final filtered values ... multiply sequence by 1, -1, etc.
            if(x % 2 == 0)
            {
                samples[2 * x] = mIBuffer[x];
                samples[2 * x + 1] = accumulator;
            }
            else
            {
                samples[2 * x] = -mIBuffer[x];
                samples[2 * x + 1] = -accumulator;
            }
        }

        //Copy residual end samples to beginning of buffers for the next iteration
        System.arraycopy(mIBuffer, FRAGMENT_SIZE, mIBuffer, 0, I_OVERLAP);
        System.arraycopy(mQBuffer, FRAGMENT_SIZE, mQBuffer, 0, Q_OVERLAP);

        return new InterleavedComplexSamples(samples, timestamp);
    }
}
