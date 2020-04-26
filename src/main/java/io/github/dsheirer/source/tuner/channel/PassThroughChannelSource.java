/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2019 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/

package io.github.dsheirer.source.tuner.channel;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.OverflowableBufferTransferQueue;
import io.github.dsheirer.sample.buffer.ComplexBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.TunerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Pass-through channel source that simply passes complex sample buffers from the tuner controller
 * directly to the registered listener
 */
public class PassThroughChannelSource extends TunerChannelSource implements ISourceEventListener,
        Listener<ComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(PassThroughChannelSource.class);
    private TunerController mTunerController;
    private OverflowableBufferTransferQueue<ComplexBuffer> mBufferQueue =
            new OverflowableBufferTransferQueue<>(500, 100);
    private List<ComplexBuffer> mBuffersToProcess = new ArrayList<>();
    private Listener<ComplexBuffer> mComplexBufferListener;

    /**
     * Constructs an instance
     *
     * @param listener to receive source events
     * @param tunerController that provides current sample rate
     * @param tunerChannel requested for this channel
     */
    public PassThroughChannelSource(Listener<SourceEvent> listener, TunerController tunerController,
                                    TunerChannel tunerChannel)
    {
        super(listener, tunerChannel);
        mTunerController = tunerController;
    }

    @Override
    public void setFrequency(long frequency)
    {
        mLog.debug("Request to set frequency: " + frequency);
    }

    @Override
    protected void setSampleRate(double sampleRate)
    {
        mLog.debug("Request to set sample rate: " + sampleRate);
    }

    @Override
    protected void setChannelFrequencyCorrection(long correction)
    {
        mLog.debug("Request to set frequency correction: " + correction);
    }

    @Override
    public long getChannelFrequencyCorrection()
    {
        return 0;
    }

    @Override
    public void setListener(Listener<ComplexBuffer> complexBufferListener)
    {
        mComplexBufferListener = complexBufferListener;
    }

    @Override
    public void removeListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferListener = null;
    }

    @Override
    protected void processSamples()
    {
        mBufferQueue.drainTo(mBuffersToProcess);

        for(ComplexBuffer buffer: mBuffersToProcess)
        {
            if(mComplexBufferListener != null)
            {
                mComplexBufferListener.receive(buffer);
            }
            else
            {

                }
        }

        mBuffersToProcess.clear();
    }

    @Override
    public double getSampleRate()
    {
        return mTunerController.getSampleRate();
    }

    @Override
    public void receive(ComplexBuffer reusableComplexBuffer)
    {
        mBufferQueue.offer(reusableComplexBuffer);
    }
}
