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
package io.github.dsheirer.dsp.filter.resample;

import com.laszlosystems.libresample4j.Resampler;
import com.laszlosystems.libresample4j.SampleBuffers;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableBuffer;
import io.github.dsheirer.sample.buffer.ReusableBufferQueue;
import io.github.dsheirer.sample.real.RealBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class RealResampler implements Listener<RealBuffer>
{
    protected static final Logger mLog = LoggerFactory.getLogger(RealResampler.class);

    private Resampler mResampler;
    private Listener<ReusableBuffer> mResampledListener;
    private BufferManager mBufferManager = new BufferManager();
    private ReusableBufferQueue mReusableBufferQueue = new ReusableBufferQueue("RealResampler");
    private double mResampleFactor;

    /**
     * Resampler for real sample buffers.
     * @param inputRate
     * @param outputRate
     */
    public RealResampler(double inputRate, double outputRate)
    {
        mResampleFactor = outputRate / inputRate;
        mResampler = new Resampler(true, mResampleFactor, mResampleFactor);
    }

    /**
     * Primary input method to the resampler
     * @param realBuffer to resample
     */
    @Override
    public void receive(RealBuffer realBuffer)
    {
        mBufferManager.load(realBuffer);
        mResampler.process(mResampleFactor, mBufferManager, false);
    }

    /**
     * Registers the listener to receive the resampled buffer output
     * @param resampledBufferListener to receive buffers
     */
    public void setListener(Listener<ReusableBuffer> resampledBufferListener)
    {
        mResampledListener = resampledBufferListener;
    }

    /**
     * Buffer manager for input and output buffers used during the resampling process.  The manager will
     * auto-resize the input and output buffers to meet the size of the incoming sample buffers.
     */
    public class BufferManager implements SampleBuffers
    {
        private int mBufferLength = 1000;
        private FloatBuffer mInputBuffer = ByteBuffer.allocate(mBufferLength).asFloatBuffer();
        private FloatBuffer mOutputBuffer = ByteBuffer.allocate(mBufferLength).asFloatBuffer();

        public void resize(int bufferLength)
        {
            int floatBufferLength = bufferLength * 4;

            FloatBuffer inputBuffer = ByteBuffer.allocate(floatBufferLength).asFloatBuffer();
            inputBuffer.put(mInputBuffer);
            mInputBuffer = inputBuffer;

            FloatBuffer outputBuffer = ByteBuffer.allocate(floatBufferLength).asFloatBuffer();
            outputBuffer.put(mOutputBuffer);
            mOutputBuffer = outputBuffer;

            mBufferLength = bufferLength;
        }

        /**
         * Current length of the input and output buffers
         */
        public int getBufferLength()
        {
            return mBufferLength;
        }

        /**
         * Queues the buffer sample for resampling
         */
        public void load(RealBuffer realBuffer)
        {
            int remaining = mInputBuffer.remaining();

            if(mInputBuffer.remaining() < realBuffer.getSamples().length)
            {
                resize(mInputBuffer.capacity() + (realBuffer.getSamples().length - mInputBuffer.remaining()));
            }

            mInputBuffer.put(realBuffer.getSamples());
        }

        @Override
        public int getInputBufferLength()
        {
            return mInputBuffer.position();
        }

        @Override
        public int getOutputBufferLength()
        {
            return mOutputBuffer.remaining();
        }

        /**
         * Provides input to the resampler
         */
        @Override
        public void produceInput(float[] samples, int offset, int length)
        {
            mInputBuffer.flip();
            mInputBuffer.get(samples, offset, length);
            mInputBuffer.compact();
        }

        /**
         * Receives output from the resampler and dispatches output buffers periodically.
         */
        @Override
        public void consumeOutput(float[] samples, int offset, int length)
        {
            mOutputBuffer.put(samples, offset, length);

            while(mOutputBuffer.position() > 200)
            {
                ReusableBuffer outputBuffer = mReusableBufferQueue.getBuffer(200);

                mOutputBuffer.flip();
                mOutputBuffer.get(outputBuffer.getSamples());
                mOutputBuffer.compact();

                if(mResampledListener != null)
                {
                    outputBuffer.incrementUserCount();
                    mResampledListener.receive(outputBuffer);
                }
            }
        }
    }
}
