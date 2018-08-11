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
package io.github.dsheirer.dsp.filter.dc;

import io.github.dsheirer.sample.buffer.ReusableBufferQueue;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import io.github.dsheirer.sample.real.RealSampleListener;

public class IIRSinglePoleDCRemovalFilter implements RealSampleListener
{
    private ReusableBufferQueue mReusableBufferQueue = new ReusableBufferQueue("IIR DC Filter");
    private float mAlpha;
    private float mPreviousInput = 0.0f;
    private float mPreviousOutput = 0.0f;
    private float mCurrentOutput = 0.0f;

    private RealSampleListener mListener;

    /**
     * IIR single-pole DC removal filter, as described by J M de Freitas in
     * 29Jan2007 paper at:
     *
     * http://www.mathworks.com/matlabcentral/fileexchange/downloads/72134/download
     *
     * @param alpha 0.0 - 1.0 float - the closer alpha is to unity, the closer
     * the cutoff frequency is to DC.
     */
    public IIRSinglePoleDCRemovalFilter(float alpha)
    {
        mAlpha = alpha;
    }

    /**
     * Filters the sample
     * @param sample to filter
     * @return filtered output sample
     */
    public float filter(float sample)
    {
        mCurrentOutput = (sample - mPreviousInput) + (mAlpha * mPreviousOutput);

        mPreviousInput = sample;
        mPreviousOutput = mCurrentOutput;

        return mCurrentOutput;
    }

    /**
     * Filters the input buffer and returns a new filtered output buffer.
     *
     * Buffer user accounting is handled by this method where the input buffer user count is decremented and the
     * returned output buffer has the user count incremented to one.
     *
     * @param inputBuffer to filter
     * @return filtered samples in a new (ie reused) output buffer
     */
    public ReusableFloatBuffer filter(ReusableFloatBuffer inputBuffer)
    {
        ReusableFloatBuffer outputBuffer = mReusableBufferQueue.getBuffer(inputBuffer.getSampleCount());
        float[] inputSamples = inputBuffer.getSamples();
        float[] outputSamples = outputBuffer.getSamples();

        for(int x = 0; x < inputSamples.length; x++)
        {
            outputSamples[x] = filter(inputSamples[x]);
        }

        inputBuffer.decrementUserCount();

        return outputBuffer;
    }

    /**
     * Implements the RealSampleListener interface for single sample streams.
     */
    @Override
    public void receive(float sample)
    {
        if(mListener != null)
        {
            mListener.receive(filter(sample));
        }
    }

    /**
     * Assigns the listener to receive filtered samples processed by the RealSampleListener interface
     */
    public void setListener(RealSampleListener listener)
    {
        mListener = listener;
    }
}
