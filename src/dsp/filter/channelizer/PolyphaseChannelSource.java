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
package dsp.filter.channelizer;

import source.heartbeat.Heartbeat;
import dsp.filter.channelizer.output.IPolyphaseChannelOutputProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.ComplexToComplexBufferAssembler;
import source.ComplexSource;
import source.ISourceEventProcessor;
import source.SourceEvent;
import source.SourceException;
import source.tuner.channel.TunerChannel;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PolyphaseChannelSource extends ComplexSource implements ISourceEventProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelSource.class);

    private TunerChannel mTunerChannel;
    private int mChannelSampleRate;

    private DownstreamSourceEventHandler mDownstreamSourceEventHandler = new DownstreamSourceEventHandler();
    private UpstreamSourceEventHandler mUpstreamSourceEventHandler;

    private ComplexToComplexBufferAssembler mComplexBufferAssembler = new ComplexToComplexBufferAssembler(2500);
    private IPolyphaseChannelOutputProcessor mPolyphaseChannelOutputProcessor;
    private ChannelResultsProcessor mChannelResultsProcessor;
    private ScheduledFuture<?> mChannelResultsProcessorFuture;

    /**
     * Polyphase channelizer channel implementation.  Adapts the channel array output samples from the polyphase
     * channelizer into a single channel, or a channel synthesized from two adjacent channels that is frequency
     * translated and decimated to a single channel.
     *
     * @param tunerChannel - requested output tuner channel frequency and bandwidth.
     * @param outputProcessor - to process polyphase channelizer channel results into a channel stream
     * @param externalEventListener to process sample stream start/stop requests
     */
    public PolyphaseChannelSource(TunerChannel tunerChannel, IPolyphaseChannelOutputProcessor outputProcessor,
                                  Listener<SourceEvent> externalEventListener, int channelSampleRate)
    {
        mTunerChannel = tunerChannel;
        mPolyphaseChannelOutputProcessor = outputProcessor;
        mUpstreamSourceEventHandler = new UpstreamSourceEventHandler(externalEventListener);;
        mChannelSampleRate = channelSampleRate;

    }

    /**
     * Registers the listener to receive complex sample buffers from this channel source
     */
    @Override
    public void setListener(final Listener<ComplexBuffer> listener)
    {
//        mComplexBufferAssembler.setListener(listener);

        mComplexBufferAssembler.setListener(new Listener<ComplexBuffer>()
        {
            @Override
            public void receive(ComplexBuffer complexBuffer)
            {
                if(mTunerChannel.getFrequency() == 99981250)
                {
//                    mLog.debug(Arrays.toString(complexBuffer.getSamples()));
                }
                listener.receive(complexBuffer);
            }
        });
    }

    /**
     * Removes the listener from receiving complex sample buffers from this channel source
     */
    @Override
    public void removeListener(Listener<ComplexBuffer> listener)
    {
        mComplexBufferAssembler.setListener(null);
    }

    /**
     * Starts scheduled thread pool processing of the inbound polyphase channel results buffer to produce channelized
     * complex sample buffer output for broadcast to a registered complex sample buffer listener.
     *
     * @param executor to use when scheduling a buffer processor
     */
    @Override
    public void start(ScheduledExecutorService executor)
    {
        if(mUpstreamSourceEventHandler != null)
        {
            mUpstreamSourceEventHandler.broadcast(SourceEvent.startSampleStream(PolyphaseChannelSource.this));
        }

        mChannelResultsProcessor = new ChannelResultsProcessor();

        if(mChannelResultsProcessorFuture != null)
        {
            mChannelResultsProcessorFuture.cancel(true);
            mLog.error("An existing channel results processor scheduled future was cancelled");
        }

        //Schedule the results processor to run every 100 milliseconds
        mChannelResultsProcessorFuture =
            executor.scheduleAtFixedRate(mChannelResultsProcessor, 0, 100, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops scheduled thread pool processing of the inbound polyphase channel results buffer to produce channelized
     * complex sample buffer output for broadcast to a registered complex sample buffer listener.
     */
    @Override
    public void stop()
    {
        if(mUpstreamSourceEventHandler != null)
        {
            mUpstreamSourceEventHandler.broadcast(SourceEvent.stopSampleStream(PolyphaseChannelSource.this));
        }

        if(mChannelResultsProcessorFuture != null)
        {
            mChannelResultsProcessorFuture.cancel(true);
            mChannelResultsProcessorFuture = null;
        }

        mChannelResultsProcessor = null;
    }


    /**
     * Module-level ISourceEventListener interface implementation for processing downstream events
     */
    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mDownstreamSourceEventHandler;
    }

    /**
     * Module-level ISourceEventProvider interface implementation, sets the listener to receive downstream module
     * level source events from this source
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mDownstreamSourceEventHandler.setDownstreamEventListener(listener);
    }

    /**
     * Module-level ISourceEventProvider interface implementation, removes the listener from receiving downstream module
     * level source events from this source
     */
    @Override
    public void removeSourceEventListener()
    {
        mDownstreamSourceEventHandler.removeDownstreamEventListener();
    }

    /**
     * ISourceEventProcessor implementation for processing source events from the external polyphase channel manager
     * and/or tuner providing samples to the polphase channelizer implementation.
     *
     * @param sourceEvent containing a request or notification
     * @throws SourceException if there are any errors while processing the source event
     */
    @Override
    public void process(SourceEvent sourceEvent) throws SourceException
    {
        mUpstreamSourceEventHandler.receive(sourceEvent);
    }

    /**
     * Channel output processor used by this channel source to convert polyphase channel results into a specific
     * channel complex buffer output stream.
     */
    public IPolyphaseChannelOutputProcessor getPolyphaseChannelOutputProcessor()
    {
        return mPolyphaseChannelOutputProcessor;
    }

    /**
     * Sets/updates the output processor for this channel source, replacing the existing output processor.
     */
    public void setPolyphaseChannelOutputProcessor(IPolyphaseChannelOutputProcessor outputProcessor)
    {
        //Lock on the channel output processor to block the channel results processor thread from servicing the
        //channel output processor's buffer while we change out the processors.
        synchronized(mPolyphaseChannelOutputProcessor)
        {
            IPolyphaseChannelOutputProcessor existingProcessor = mPolyphaseChannelOutputProcessor;

            //Swap out the processor so that incoming samples can accumulate in the new channel output processor
            mPolyphaseChannelOutputProcessor = outputProcessor;

            //Fully process the residual channel results buffer of the previous channel output processor
            if(existingProcessor != null)
            {
                existingProcessor.processChannelResults(mComplexBufferAssembler);
            }
        }
    }

    /**
     * Tuner channel that is being sourced by this polyphase channel source.
     */
    public TunerChannel getTunerChannel()
    {
        return mTunerChannel;
    }

    /**
     * Primary method for receiving channel results output from a polyphase channelizer.  The results array will be
     * queued for processing to extract the target channel samples, process them for frequency correction and/or
     * channel aggregation, and dispatch the results to the downstream sample listener/consumer.
     *
     * @param channelResultsBuffer containing the polyphase channelizer output for a single complex sample period.
     */
    public void receiveChannelResults(float[] channelResultsBuffer)
    {
        mPolyphaseChannelOutputProcessor.receiveChannelResults(channelResultsBuffer);
    }

    /**
     * Downstream sample rate
     *
     * @return sample rate in Hertz
     * @throws SourceException never
     */
    @Override
    public int getSampleRate() throws SourceException
    {
        return mChannelSampleRate;
    }

    /**
     * Center tuned frequency for this channel.
     *
     * @return frequency in hertz.
     * @throws SourceException never
     */
    @Override
    public long getFrequency() throws SourceException
    {
        return mTunerChannel.getFrequency();
    }

    @Override
    public void setHeartbeatListener(Listener<Heartbeat> listener)
    {
        mLog.info("Polyphase channel source received request to set heartbeat listener");
        //TODO: implement this
    }

    @Override
    public void removeHeartbeatListener()
    {
        mLog.info("Polyphase channel source received request to remove heartbeat listener");
        //TODO: implement this
    }

    @Override
    public void reset()
    {
        mLog.info("Polyphase channel source received request to reset");
    }


    @Override
    public void dispose()
    {
        mLog.info("Polyphase channel source received request to dispose");
    }

    /**
     * Adjusts the frequency correction value that is being applied to the channelized output stream by the
     * polyphase channel output processor.
     * @param value to apply for frequency correction in hertz
     */
    private void setFrequencyCorrection(long value)
    {
        if(mPolyphaseChannelOutputProcessor != null)
        {
            mPolyphaseChannelOutputProcessor.setFrequencyCorrection(value);
            mDownstreamSourceEventHandler.broadcast(SourceEvent.frequencyCorrectionChange(value));
        }
    }

    /**
     * Runnable for processing the incoming polyphase channelizer results queue and distributing the channel
     * results to the registered complex sample listener
     */
    public class ChannelResultsProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                //Lock on the output processor so that it can't be changed out in the middle of processing
                synchronized(mPolyphaseChannelOutputProcessor)
                {
                    mPolyphaseChannelOutputProcessor.processChannelResults(mComplexBufferAssembler);
                }
            }
            catch(Throwable throwable)
            {
                mLog.error("Error while processing polyphase channel samples", throwable);
            }
        }
    }

    public class UpstreamSourceEventHandler implements Listener<SourceEvent>
    {
        private Listener<SourceEvent> mExternalSourceEventListener;

        /**
         * Upstream source event processor for handling any source events generated by the polyphase channelizer that
         * provides this polyphase channel source.
         *
         * @param externalSourceEventListener to receive requests for start/stop sample streams
         */
        public UpstreamSourceEventHandler(Listener<SourceEvent> externalSourceEventListener)
        {
            mExternalSourceEventListener = externalSourceEventListener;
        }

        /**
         * Broadcasts the request source event to the external source event listener.  This listener is normally the
         * polyphase channel manager.
         *
         * @param sourceEvent to broadcast
         */
        public void broadcast(SourceEvent sourceEvent)
        {
            if(mExternalSourceEventListener != null)
            {
                mExternalSourceEventListener.receive(sourceEvent);
            }
        }

        /**
         * Process source events received from the upstream (ie tuner or channelizer) sample provider.
         * @param sourceEvent to process
         */
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                //Reset frequency correction anytime the upstream frequency or sample rate changes so that the
                //downstream modules can reset and recalculate any needed frequency correction
                case NOTIFICATION_FREQUENCY_CHANGE:
                case NOTIFICATION_SAMPLE_RATE_CHANGE:
                    setFrequencyCorrection(0);
                    break;
            }
        }
    }

    /**
     * Downstream source event handler implements the ISourceEventProvider and ISourceEventProcessor interfaces on
     * for this module to handle broadcasting source events to other downstream modules and receiving requests from
     * any downstream modules.
     */
    public class DownstreamSourceEventHandler implements Listener<SourceEvent>
    {
        private Listener<SourceEvent> mDownstreamEventListener;

        public void broadcast(SourceEvent sourceEvent)
        {
            if(mDownstreamEventListener != null)
            {
                mDownstreamEventListener.receive(sourceEvent);
            }
        }

        /**
         * Sets the downstream event listener to receive source events from this channel source module
         * @param listener to receive events
         */
        public void setDownstreamEventListener(Listener<SourceEvent> listener)
        {
            mDownstreamEventListener = listener;
        }

        public void removeDownstreamEventListener()
        {
            mDownstreamEventListener = null;
        }

        /**
         * Process source events received from the downstream channel modules.
         * @param sourceEvent to process
         */
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                case REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE:
                    setFrequencyCorrection(sourceEvent.getValue().intValue());
                    break;
            }
        }
    }
}
