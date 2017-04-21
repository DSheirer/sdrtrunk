/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package source.tuner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.complex.ComplexBuffer;
import source.ComplexSource;
import source.ISourceEventProcessor;
import source.SourceEvent;
import source.SourceException;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Complex source that provides a complex sample stream at the channel's required sample rate
 */
public abstract class ComplexChannelSource extends ComplexSource implements ISourceEventProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(ComplexChannelSource.class);
    private SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private Listener<SourceEvent> mSourceEventListener;
    private Listener<ComplexBuffer> mComplexBufferListener;
    private ISourceEventProcessor mUpstreamSourceEventProcessor;

    public ComplexChannelSource(ISourceEventProcessor upstreamSourceEventProcessor)
    {
        mUpstreamSourceEventProcessor = upstreamSourceEventProcessor;
    }

    /**
     * Sets the source event listener to receive source events from this source
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Removes the source event listener from receiving source events from this source
     */
    @Override
    public void removeSourceEventListener()
    {
        mSourceEventListener = null;
    }

    /**
     * Broadcasts the source event to the registered source event listener
     */
    protected void broadcast(SourceEvent sourceEvent)
    {
        if(mSourceEventListener != null)
        {
            mSourceEventListener.receive(sourceEvent);
        }
    }

    /**
     * Registers the listener to receive complex sample buffers from this channel source
     */
    @Override
    public void setListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferListener = listener;
    }

    @Override
    public void removeListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferListener = null;
    }

    /**
     * Dispatches the processed complex buffer to the downstream consumer.
     * @param complexBuffer containing complex samples
     */
    protected void broadcast(ComplexBuffer complexBuffer)
    {
        if(mComplexBufferListener != null)
        {
            mComplexBufferListener.receive(complexBuffer);
        }
    }

    @Override
    public void start(ScheduledExecutorService executor)
    {
        //TODO: start a runnable to process downstream buffer dispatching

        if(mUpstreamSourceEventProcessor != null)
        {
            try
            {
                mUpstreamSourceEventProcessor.process(SourceEvent.startSampleStream(ComplexChannelSource.this));
            }
            catch(SourceException se)
            {
                mLog.error("Error starting complex channel source");
            }
        }
    }

    @Override
    public void stop()
    {
        if(mUpstreamSourceEventProcessor != null)
        {
            try
            {
                mUpstreamSourceEventProcessor.process(SourceEvent.stopSampleStream(ComplexChannelSource.this));
            }
            catch(SourceException se)
            {
                mLog.error("Error starting complex channel source");
            }
        }

        //TODO: cancel a runnable to process downstream buffer dispatching
    }


    /**
     * Source Event processor interface for this source to receive source events
     */
    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventProcessor;
    }

    /**
     * Adapter for implementing ISourceEventProcessor and dispatching source events to subclasses via the
     * ISourceEventProcessor.process() method.
     */
    public class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            try
            {
                process(sourceEvent);
            }
            catch(SourceException se)
            {
                mLog.error("Error processing source event", se);
            }
        }
    }
}
