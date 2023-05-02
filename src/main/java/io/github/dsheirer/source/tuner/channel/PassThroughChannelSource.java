/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.channel;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.util.Dispatcher;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pass-through channel source that simply passes complex sample buffers from the tuner controller
 * directly to the registered listener
 */
public class PassThroughChannelSource extends TunerChannelSource implements ISourceEventListener,
        Listener<INativeBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(PassThroughChannelSource.class);
    private TunerController mTunerController;
    private Dispatcher<INativeBuffer> mBufferDispatcher;
    private Listener<ComplexSamples> mBufferListener;

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
        mBufferDispatcher = new Dispatcher<>("sdrtrunk pass-through channel " + tunerChannel.getFrequency(),
                250, 50, getHeartbeatManager());
        mBufferDispatcher.setListener(new BufferProcessor());
    }

    @Override
    public void start()
    {
        super.start();
        mBufferDispatcher.start();
    }

    @Override
    public void stop()
    {
        super.stop();
        mBufferDispatcher.stop();
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
    public void setListener(Listener<ComplexSamples> listener)
    {
        mBufferListener = listener;
    }

    @Override
    public double getSampleRate()
    {
        return mTunerController.getSampleRate();
    }

    @Override
    public void receive(INativeBuffer buffer)
    {
        mBufferDispatcher.receive(buffer);
    }

    public class BufferProcessor implements Listener<INativeBuffer>
    {
        @Override
        public void receive(INativeBuffer nativeBuffer)
        {
            if(mBufferListener != null)
            {
                Iterator<ComplexSamples> iterator = nativeBuffer.iterator();

                while(iterator.hasNext())
                {
                    try
                    {
                        mBufferListener.receive(iterator.next());
                    }
                    catch(Throwable t)
                    {
                        mLog.error("Error dispatching complex sample buffers to listener [" + mBufferListener + "]", t);
                    }
                }
            }
        }
    }
}
