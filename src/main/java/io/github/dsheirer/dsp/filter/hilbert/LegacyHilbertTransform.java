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
package io.github.dsheirer.dsp.filter.hilbert;

import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class LegacyHilbertTransform
{
    private static final Logger mLog = LoggerFactory.getLogger(LegacyHilbertTransform.class);

    private float[] mHilbertFilter;
    private float[] mBuffer;
    private int mBufferSize;
    private int mBufferPointer;
    private int[][] mIndexMap;
    private int mMapHeight;
    private int mCenterTapIndex;
    private float mCenterCoefficent;

    private boolean mInvertFlag = false;

    /**
     * Hilbert transform filter used for converting real-valued samples into
     * complex valued samples using frequency translation (FS/4) and a half-band
     * filter.
     *
     * This filter uses a circular sample buffer and a pre-calculated
     * index map to correctly map the filter coefficients to the circular delay
     * buffer contents as each new sample is added to the buffer and processed.
     *
     * Half-band filter coefficients used in this filter must be of length N
     * where (N + 1) is a multiple of 4.  This filter uses a pre-defined half
     * band filter with 47 coefficients.
     *
     * This transform process is described in Understanding Digital Signal
     * Processing, Lyons, 3e, 2011, sections 13.1.2 and 13.1.3 (p 674-678) and
     * implemented as described in Section 13.37.1 and 13.37.2 (p 802-804)
     */
    public LegacyHilbertTransform()
    {
        convertHalfBandToHilbert(HilbertTransform.HALF_BAND_FILTER_47_TAP);

        mBufferSize = mHilbertFilter.length + 1;
        mBuffer = new float[mBufferSize];

        mCenterCoefficent = mHilbertFilter[mHilbertFilter.length / 2];

        generateIndexMap(mHilbertFilter.length);
    }

    /**
     * Filters the continuous real-sample stream by treating the stream as a
     * set of interleaved values and applying two hilbert filters against each
     * interleaved set (I & Q).  Frequency translation has already been
     * pre-applied to the hilbert filter coefficients.
     *
     * Filtering of the inphase component is a simple multiply by the center
     * coefficient (0.5) since all of the other coefficients are zero-valued.
     *
     * Filtering of the quadrature component uses a folded FIR structure and
     * expects the hilbert coefficients to be symmetric with all negative valued
     * coefficients below the center coefficient and all positive-valued coefficients
     * above the center coefficient. So, convolution only applies the filter
     * coefficient against the difference of the two samples.
     *
     * ( coefficient * value1 ) + (-coefficient * value2 ) = coefficient * ( value1 - value2 )
     *
     * Performs frequency translation by FS/2 on the final filtered values by
     * applying a sequence of 1,-1 (sign change) to each I/Q output sample.
     */
    public float[] filter(float[] samples)
    {
        float accumulator;

        for(int y = 0; y < samples.length; y += 2)
        {
            insert(samples[y]);
            insert(samples[y + 1]);

            accumulator = 0.0f;

            int index = mBufferPointer / 2;

            for(int x = 0; x < mHilbertFilter.length / 2; x += 2)
            {
                accumulator += mHilbertFilter[x] *
                    (mBuffer[mIndexMap[index][x + 1]] -
                        mBuffer[mIndexMap[index][x]]);
            }

            //Perform FS/2 frequency translation on the final filtered values
            if(mInvertFlag)
            {
                //inphase
                samples[y] = -(mBuffer[mIndexMap[index][mCenterTapIndex]]);

                //quadrature
                samples[y + 1] = -accumulator;
            }
            else
            {
                //inphase
                samples[y] = mBuffer[mIndexMap[index][mCenterTapIndex]];

                //quadrature
                samples[y + 1] = accumulator;
            }

            mInvertFlag = !mInvertFlag;
        }

        return samples;
    }

    /**
     * Inserts the sample into the circular buffer, overwriting the oldest value
     */
    private void insert(float sample)
    {
        mBuffer[mBufferPointer++] = sample;

        mBufferPointer = mBufferPointer % mBufferSize;
    }

    /**
     * Creates an index map to support efficient lookup of sample indexes from
     * the circular buffer.
     *
     * The first index of the map corresponds to the current buffer pointer
     * setting.
     *
     * The second index is structured in sample pairs to align with
     * the hilbert filter coefficients.  For example, the first iteration of
     * the filter would use filter coefficient 0 and the index map values 0 and
     * 1 would be pointing to samples 46 and 0.
     *
     * The final element of each index set is the index of the center sample.
     */
    private void generateIndexMap(int size)
    {
        mMapHeight = size / 2 + 1;
        int mapWidth = mMapHeight + 1;

        mIndexMap = new int[mMapHeight][mapWidth];

        //Setup the first row with buffer pointer index 0 as a starting point
        for(int x = 0; x < mapWidth - 1; x += 2)
        {
            mIndexMap[0][x] = size - 1 - x;
            mIndexMap[0][x + 1] = x;
        }

        //Place the center index at the end of the array
        mCenterTapIndex = mapWidth - 1;

        mIndexMap[0][mCenterTapIndex] = size / 2;

        //For each subsequent row, increment the previous row's value by 2,
        //wrapping as needed, to keep the values between 0 and size - 1
        for(int x = 1; x < mMapHeight; x++)
        {
            for(int y = 0; y < mapWidth; y++)
            {
                mIndexMap[x][y] = mIndexMap[x - 1][y] + 2;

                if(mIndexMap[x][y] >= size)
                {
                    mIndexMap[x][y] -= size + 1;

                    //Special handling for center tap wrap around
                    if(y == mCenterTapIndex && mIndexMap[x][y] < 0)
                    {
                        mIndexMap[x][y] = size;
                    }
                }
            }
        }
    }

    /**
     * Logs the index map contents
     */
    public void logIndexMap()
    {
        for(int[] indexSet : mIndexMap)
        {
            mLog.debug("Row:" + Arrays.toString(indexSet));
        }
    }

    /**
     * Converts the half-band filter coefficients for use as hilbert transform
     * filter coefficients.  Sets all even-numbered coefficients left of center
     * to negative and all even-numbered coefficients right of center to positive
     * which applies a FS/4 frequency translation to the coefficients.
     *
     * Note: even though the coefficients above the center coefficient are being
     * defined, they are unused during the convolution process since we're using
     * a folded FIR structure to exploit the symmetric nature of the half-band
     * filter coefficients.  The polarity difference between the upper and lower
     * halves is accounted for in the filter() method.
     *
     * Note: we apply a 2.0 gain to the coefficients to compensate for the loss
     * of splitting the signal via two filters.
     */
    private void convertHalfBandToHilbert(float[] coefficients)
    {
        mHilbertFilter = new float[coefficients.length];

        int middle = coefficients.length / 2;

        for(int x = 0; x < coefficients.length; x++)
        {
            if(x < middle)
            {
                mHilbertFilter[x] = 2.0f * -FastMath.abs(coefficients[x]);
            }
            else if(x > middle)
            {
                mHilbertFilter[x] = 2.0f * FastMath.abs(coefficients[x]);
            }
            else
            {
                mHilbertFilter[x] = 2.0f * coefficients[x];
            }
        }
    }
}
