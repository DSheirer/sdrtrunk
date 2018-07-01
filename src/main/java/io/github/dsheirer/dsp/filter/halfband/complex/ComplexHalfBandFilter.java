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
package io.github.dsheirer.dsp.filter.halfband.complex;

import io.github.dsheirer.dsp.filter.halfband.real.HalfBandFilter;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;

public class ComplexHalfBandFilter
{
    private HalfBandFilter mIFilter;
    private HalfBandFilter mQFilter;
    private boolean mDecimate;
    private boolean mDecimateFlag;

    /**
     * @param coefficients
     * @param gain
     * @param decimate
     */
    public ComplexHalfBandFilter(float[] coefficients, float gain, boolean decimate)
    {
        mIFilter = new HalfBandFilter(coefficients, gain);
        mQFilter = new HalfBandFilter(coefficients, gain);
        mDecimate = decimate;
    }

    public void dispose()
    {
        mIFilter.dispose();
        mQFilter.dispose();
    }

    public ReusableComplexBuffer filter(ReusableComplexBuffer buffer)
    {
        throw new IllegalStateException("This filter has to be updated to work with reusable buffers");

//        if(mDecimate)
//        {
//            float[] samples = buffer.getSamples();
//
//            int half = samples.length / 2;
//
//            float[] decimated;
//
//            if(half % 2 == 0 || mDecimateFlag)
//            {
//                decimated = new float[half];
//            }
//            else
//            {
//                /* If inbound buffer size is odd-length, then we have to
//                 * adjust when the first operation is non-decimation, since
//                 * that will produce an outbound buffer 1 sample larger */
//                decimated = new float[half + 1];
//            }
//
//            int decimatedPointer = 0;
//
//            for(int x = 0; x < samples.length; x += 2)
//            {
//                /* Insert the sample but don't filter */
//                if(mDecimateFlag)
//                {
//                    mIFilter.insert(samples[x]);
//                    mQFilter.insert(samples[x + 1]);
//                }
//                else
//                {
//                    decimated[decimatedPointer++] = mIFilter.filter(samples[x]);
//                    decimated[decimatedPointer++] = mQFilter.filter(samples[x + 1]);
//                }
//
//                /* Toggle the decimation flag for every sample */
//                mDecimateFlag = !mDecimateFlag;
//            }
//
//            //return buffer
//        }
//        else
//        {
//            /* Non Decimate - filter each of the values and return the buffer */
//            float[] samples = buffer.getSamples();
//
//            for(int x = 0; x < samples.length; x += 2)
//            {
//                samples[x] = mIFilter.filter(samples[x]);
//                samples[x + 1] = mQFilter.filter(samples[x + 1]);
//            }
//
//            //return buffer
//        }
    }
}
