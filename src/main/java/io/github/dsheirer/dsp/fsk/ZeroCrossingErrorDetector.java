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
package io.github.dsheirer.dsp.fsk;

import org.apache.commons.math3.util.FastMath;

/**
 * Zero crossing error detector.  Provides symbol timing error correction values by detecting zero-crossings within
 * each symbol period.  Ideally there should be only 0 or 1 zero crossings within each symbol period.  However, when
 * there is a slight DC offset in the demodulated samples, there can be up to 2 zero crossings within a symbol
 * period.
 *
 * This detector will only provide error corrections when there are 1 or 2 zero crossings detected.  If the number
 * of zero crossings is greater than 2, then it is assumed that there is no signal present and the returned timing
 * error value is zero.  This detector provides an error correction value relative to (approximately) the middle of
 * a symbol period and therefore continuously attempts to align zero crossings to the middle of the period.
 *
 * Since this detector does NOT use interpolation, the error signal will contain minor levels of symbol timing
 * jitter.  Error signal values are reported as integrals of the sampling period.
 */
public class ZeroCrossingErrorDetector
{
    protected boolean[] mBuffer;
    protected float mZeroCrossingIdeal;
    protected float mZeroCrossingOne;
    protected float mZeroCrossingTwo;
    protected float mDetectedZeroCrossing;
    private int mZeroCrossingCount;

    /**
     * Constructs the detector for the specified samples per symbol.
     *
     * @param samplesPerSymbol
     */
    public ZeroCrossingErrorDetector(float samplesPerSymbol)
    {
        mBuffer = new boolean[(int)FastMath.ceil(samplesPerSymbol)];
//        mZeroCrossingIdeal = mBuffer.length - 1.5f - (samplesPerSymbol / 2.0f);  //Preserve this for LTR
        mZeroCrossingIdeal = (samplesPerSymbol / 2.0f);
    }

    /**
     * Stores the sample in the buffer.  Use the getError() method once enough samples have been processed to determine
     * the error for a symbol period.
     *
     * @param sample
     */
    public void receive(boolean sample)
    {
        //Shift all samples left and store new sample at the end
        System.arraycopy(mBuffer, 1, mBuffer, 0, mBuffer.length - 1);
        mBuffer[mBuffer.length - 1] = sample;
    }

    /**
     * Determines the symbol timing error for the samples processed thus far.  This method should be invoked once per
     * symbol period, triggered via an external samples per symbol tracking process.
     *
     * @return symbol timing error calculated as the distance between the approximate symbol center and the next closest
     * zero crossing index.  An error value of zero is returned if the number of zero crossings is less than one or
     * greater than two.
     */
    public float getError()
    {
        mZeroCrossingCount = 0;

        for(int x = 0; x < mBuffer.length - 1; x++)
        {
            if(mBuffer[x] ^ mBuffer[x + 1])
            {
                if(mZeroCrossingCount == 0)
                {
                    mZeroCrossingCount++;
                    mZeroCrossingOne = x + 0.5f;
                }
                else if(mZeroCrossingCount == 1)
                {
                    mZeroCrossingCount++;
                    mZeroCrossingTwo = x + 0.5f;
                }
                else
                {
                    //Max zero crossings exceeded - no error can be calculated - abort early
                    return 0.0f;
                }
            }
        }

        if(mZeroCrossingCount == 1)
        {
            mDetectedZeroCrossing = mZeroCrossingOne;

            return mZeroCrossingIdeal - mZeroCrossingOne;
        }
        else if(mZeroCrossingCount == 2)
        {
            float errorDistanceOne = mZeroCrossingIdeal - mZeroCrossingOne;
            float errorDistanceTwo = mZeroCrossingCount - mZeroCrossingTwo;

            if(FastMath.abs(errorDistanceOne) < FastMath.abs(errorDistanceTwo))
            {
                mDetectedZeroCrossing = mZeroCrossingOne;
            }
            else
            {
                mDetectedZeroCrossing = mZeroCrossingTwo;
            }

            return mZeroCrossingIdeal - mDetectedZeroCrossing;
        }

        return 0.0f;
    }
}
