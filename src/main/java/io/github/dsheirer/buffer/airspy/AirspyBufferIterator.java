/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import io.github.dsheirer.dsp.filter.hilbert.HilbertTransform;
import java.util.Iterator;

/**
 * Base complex samples iterator for raw Airspy sample buffers.  Incorporates a Hilbert transform filter
 * for converting the real sample array to complex samples.
 * @param <T> either ComplexSamples or InterleavedComplexSamples
 */
public abstract class AirspyBufferIterator<T> implements Iterator<T>
{
    protected static final float SCALE_SIGNED_12_BIT_TO_FLOAT = 1.0f / 2048.0f;
    protected static final int FRAGMENT_SIZE = 4096;
    protected static final float[] COEFFICIENTS =
            HilbertTransform.convertHalfBandToHilbert(HilbertTransform.HALF_BAND_FILTER_47_TAP);

    public static final int I_OVERLAP = 11;
    public static final int Q_OVERLAP = 23;

    protected float[] mIBuffer = new float[FRAGMENT_SIZE + I_OVERLAP];
    protected float[] mQBuffer = new float[FRAGMENT_SIZE + Q_OVERLAP];
    protected short[] mSamples;
    protected int mSamplesPointer = 0;
    protected float mAverageDc;
    private long mTimestamp;
    private float mSamplesPerMillisecond;

    /**
     * Constructs an instance
     * @param samples from the airspy.
     * @param residualI samples from previous buffer
     * @param residualQ samples from previous buffer
     * @param averageDc as measured
     * @param timestamp of the buffer
     * @param samplesPerMillisecond to calculate sub-buffer fragment timestamps
     */
    public AirspyBufferIterator(short[] samples, short[] residualI, short[] residualQ, float averageDc, long timestamp,
                                float samplesPerMillisecond)
    {
        if(residualI.length != I_OVERLAP || residualQ.length != Q_OVERLAP)
        {
            throw new IllegalArgumentException("Residual I length [" + residualI.length +
                    "] must be " + I_OVERLAP + " and Residual Q length [" + residualQ.length +
                    "] must be " + Q_OVERLAP);
        }

        int requiredInterval = FRAGMENT_SIZE * 2; //requires 4 bytes (2 samples) per fragment

        if(samples.length % requiredInterval != 0)
        {
            throw new IllegalArgumentException("Samples short array length [" + mSamples.length +
                    "]must be an integer multiple of " + requiredInterval);
        }

        mAverageDc = averageDc;
        mSamples = samples;
        mTimestamp = timestamp;
        mSamplesPerMillisecond = samplesPerMillisecond;

        //Transfer and scale the residual I & Q samples from the previous buffer
        for(int i = 0; i < residualI.length; i++)
        {
            mIBuffer[i] = scale(residualI[i], averageDc);
        }

        for(int q = 0; q < residualQ.length; q++)
        {
            mQBuffer[q] = scale(residualQ[q], averageDc);
        }
    }

    /**
     * Calculates the timestamp for a samples buffer fragment based on where it is extracted from relative to the
     * native buffer starting timestamp.
     * @param samplesPointer for the start of the fragment.  Note: this value will be divided by 2 to account for I/Q sample pairs.
     * @return timestamp adjusted to the fragment or sub-buffer start sample.
     */
    protected long getFragmentTimestamp(int samplesPointer)
    {
        return mTimestamp + (long)(samplesPointer / 2 / mSamplesPerMillisecond);
    }

    @Override
    public boolean hasNext()
    {
        return mSamplesPointer < mSamples.length;
    }

    /**
     * Scales the short sample value to a 12-bit floating point and subtracts the average DC offset
     * @param value to scale
     * @param averageDc detected
     * @return scaled and normalized sample
     */
    public static float scale(short value, float averageDc)
    {
        return ((float)value - 2048) * SCALE_SIGNED_12_BIT_TO_FLOAT - averageDc;
    }
}
