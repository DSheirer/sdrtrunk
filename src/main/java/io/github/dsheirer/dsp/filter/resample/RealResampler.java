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
package io.github.dsheirer.dsp.filter.resample;

import com.laszlosystems.libresample4j.Resampler;
import com.laszlosystems.libresample4j.SampleBuffers;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Real sample buffer resampler
 */
public class RealResampler
{
    protected static final Logger mLog = LoggerFactory.getLogger(RealResampler.class);

    private Resampler mResampler;
    private Listener<float[]> mResampledListener;
    private BufferManager mBufferManager;
    private double mResampleFactor;

    /**
     * Constructs an instance.
     * @param inputRate sample rate
     * @param outputRate sample rate
     * @param bufferSize for the input and output float buffers sized large enough to hold at least two of the
     * input or output buffer lengths, whichever is larger
     * @param outputArrayLength to create uniform length output arrays
     */
    public RealResampler(double inputRate, double outputRate, int bufferSize, int outputArrayLength)
    {
        mResampleFactor = outputRate / inputRate;
        mResampler = new Resampler(true, mResampleFactor, mResampleFactor);
        mBufferManager = new BufferManager(bufferSize, outputArrayLength);
    }

    /**
     * Resample factor (output rate / input rate)
     */
    public double getResampleFactor()
    {
        return mResampleFactor;
    }

    /**
     * Resamples all of the audio packets
     * @param audioPackets to resample
     * @return resampled audio packets
     */
    public List<float[]> resample(List<float[]> audioPackets)
    {
        List<float[]> resampled = new ArrayList<>();

        mResampledListener = resampledAudio -> resampled.add(resampledAudio);

        for(int x = 0; x < audioPackets.size(); x++)
        {
            if(x == audioPackets.size() - 1)
            {
                resample(audioPackets.get(x), true);
            }
            else
            {
                resample(audioPackets.get(x));
            }
        }

        mResampledListener = null;
        return resampled;
    }

    /**
     * Primary input method to the resampler
     * @param samples to resample
     */
    public void resample(float[] samples)
    {
        resample(samples, false);
    }

    /**
     * Primary input method to the resampler
     * @param samples to resample
     * @param lastBatch set to true if this is the last set of samples
     */
    public void resample(float[] samples, boolean lastBatch)
    {
        mBufferManager.load(samples);
        mResampler.process(mResampleFactor, mBufferManager, lastBatch);
    }

    /**
     * Registers the listener to receive the resampled output buffers
     * @param listener to receive buffers
     */
    public void setListener(Listener<float[]> listener)
    {
        mResampledListener = listener;
    }

    /**
     * Buffer manager for input and output buffers used during the resampling process.
     */
    public class BufferManager implements SampleBuffers
    {
        private int mOutputArrayLength;
        private FloatBuffer mInputBuffer;
        private FloatBuffer mOutputBuffer;

        /**
         * Constructs an instance
         * @param bufferSize for the input and output float buffers
         * @param outputArrayLength to create uniform length output sample arrays
         */
        public BufferManager(int bufferSize, int outputArrayLength)
        {
            mOutputArrayLength = outputArrayLength;

            //The size of the underlying byte buffer in bytes is 4 times the requested input buffer size in floats
            mInputBuffer = ByteBuffer.allocate(bufferSize * 4).asFloatBuffer();
            mOutputBuffer = ByteBuffer.allocate(bufferSize * 4).asFloatBuffer();
        }

        /**
         * Queues the buffer samples for resampling
         */
        public void load(float[] samples)
        {
            mInputBuffer.put(samples);
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

            while(mOutputBuffer.position() > mOutputArrayLength)
            {
                float[] resampled = new float[mOutputArrayLength];

                mOutputBuffer.flip();
                mOutputBuffer.get(resampled);
                mOutputBuffer.compact();

                if(mResampledListener != null)
                {
                    mResampledListener.receive(resampled);
                }
            }
        }
    }
}
