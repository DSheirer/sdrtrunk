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

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.RealSampleListener;

/**
 * Assembles single float samples into a ReusableFloatBuffer containing an array of floats
 */
public class ReusableBufferAssembler implements RealSampleListener
{
    private int mBufferSize;
    private int mBufferPointer;
    private ReusableFloatBuffer mCurrentBuffer;
    private ReusableBufferQueue mReusableBufferQueue = new ReusableBufferQueue("Real");

    private Listener<ReusableFloatBuffer> mListener;

    public ReusableBufferAssembler(int bufferSize)
    {
        mBufferSize = bufferSize;
    }

    public void dispose()
    {
        mListener = null;
    }

    public void reset()
    {
        mCurrentBuffer.decrementUserCount();
        mCurrentBuffer = null;
        mBufferPointer = 0;
    }

    @Override
    public void receive(float sample)
    {
        if(mCurrentBuffer == null)
        {
            mCurrentBuffer = mReusableBufferQueue.getBuffer(mBufferSize);
            mBufferPointer = 0;
        }

        mCurrentBuffer.getSamples()[mBufferPointer++] = sample;

        if (mBufferPointer >= mBufferSize)
        {
            if (mListener != null)
            {
                mListener.receive(mCurrentBuffer);
            }
            else
            {
                mCurrentBuffer.decrementUserCount();
            }

            mCurrentBuffer = null;
        }
    }

    public void setListener(Listener<ReusableFloatBuffer> listener)
    {
        mListener = listener;
    }
}
