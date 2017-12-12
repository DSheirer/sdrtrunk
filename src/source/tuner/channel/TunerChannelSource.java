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
package source.tuner.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.complex.ComplexBuffer;
import source.ComplexSource;
import source.ISourceEventProcessor;
import source.SourceEvent;
import source.SourceEventListenerToProcessorAdapter;
import source.SourceException;
import util.ThreadPool;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class TunerChannelSource extends ComplexSource implements ISourceEventProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(TunerChannelSource.class);
    private static final long BUFFER_PROCESSOR_RUN_INTERVAL_MILLISECONDS = 50;
    private SourceEventListenerToProcessorAdapter mConsumerSourceEventListenerAdapter;
    private TunerChannel mTunerChannel;
    private Listener<SourceEvent> mProducerSourceEventListener;
    private Listener<SourceEvent> mConsumerSourceEventListener;
    private BufferProcessor mBufferProcessor = new BufferProcessor();
    private boolean mExpended = false;

    /**
     * Tuner Channel Source is a Digital Drop Channel (DDC) abstract class that defines the minimum functionality
     * requirements for a channel sample source.
     *
     * @param producerSourceEventListener to receive source event requests (e.g. start/stop sample stream)
     * @param tunerChannel describing the desired channel frequency and bandwidth/minimum sample rate
     */
    public TunerChannelSource(Listener<SourceEvent> producerSourceEventListener, TunerChannel tunerChannel)
    {
        mProducerSourceEventListener = producerSourceEventListener;
        mTunerChannel = tunerChannel;
        mConsumerSourceEventListenerAdapter = new SourceEventListenerToProcessorAdapter(this);
    }

    @Override
    public long getFrequency()
    {
        return mTunerChannel.getFrequency();
    }

    /**
     * Sets the center frequency for the sample streaming being sent from the producer.
     * @param frequency in hertz
     */
    protected abstract void setFrequency(long frequency);

    /**
     * Sets the sample rate of the incoming sample stream from the producer
     * @param sampleRate in hertz
     */
    protected abstract void setSampleRate(double sampleRate);

    /**
     * Sets the frequency correction for the outbound sample stream to the consumer
     * @param correction in hertz
     */
    protected abstract void setFrequencyCorrection(long correction);

    /**
     * Frequency correction value currently being applied to the outbound sample stream
     */
    public abstract long getFrequencyCorrection();

    /**
     * Sets the listener to receive the complex buffer sample output from this channel
     * @param complexBufferListener to receive complex buffers
     */
    public abstract void setListener(Listener<ComplexBuffer> complexBufferListener);

    /**
     * Commands sub-class to process queued samples and distribute them to the consumer.  This method will be invoked
     * by an interval timer.
     */
    protected abstract void processSamples();

    /**
     * Tuner channel for this tuner channel source
     */
    public TunerChannel getTunerChannel()
    {
        return mTunerChannel;
    }

    @Override
    public void dispose()
    {
        stop();
    }

    /**
     * Starts this tuner channel source producing sample stream.
     */
    public void start()
    {
        if(mExpended)
        {
            throw new IllegalStateException("Cannot start an expended tuner channel source.  You must request a new" +
                " tuner channel source each time.  Tuner channel sources are one-use only");
        }

        //Broadcast current frequency and sample rate so consumer can configure correctly
        broadcastConsumerSourceEvent(SourceEvent.frequencyChange(getFrequency()));
        broadcastConsumerSourceEvent(SourceEvent.sampleRateChange(getSampleRate()));

        if(mProducerSourceEventListener != null)
        {
            mProducerSourceEventListener.receive(SourceEvent.startSampleStream(this));
        }

        mBufferProcessor.start();
    }

    /**
     * Stops this tuner channel source from producing a sample stream.  Note: tuner channel sources are one-time usage
     * only.  Invoking this method will also tell the source manager to dispose of this source.
     */
    public void stop()
    {
        mExpended = true;

        if(mProducerSourceEventListener != null)
        {
            mProducerSourceEventListener.receive(SourceEvent.stopSampleStream(this));
            mProducerSourceEventListener.receive(SourceEvent.sourceDisposeRequest(this));
        }

        mBufferProcessor.stop();
    }

    @Override
    public void reset()
    {
        if(mExpended)
        {
            throw new IllegalStateException("Cannot reset an expended tuner channel source.  You must request a new" +
                " tuner channel source each time.  Tuner channel sources are one-use only");
        }
    }

    /**
     * This method is invoked after the buffer processor is completely shutdown so that this instance can perform any
     * cleanup operations needed to dispose of this instance.
     */
    protected void performDisposal()
    {
        mProducerSourceEventListener = null;
        mConsumerSourceEventListener = null;
        mConsumerSourceEventListenerAdapter.dispose();
    }

    /**
     * Processes source event notifications received from the producer/parent and source event requests received from
     * the consumer.
     *
     * @param sourceEvent to process
     * @throws SourceException if there is an error while processing the source event
     */
    @Override
    public void process(SourceEvent sourceEvent) throws SourceException
    {
        switch(sourceEvent.getEvent())
        {
            //Notification events from the producer
            case NOTIFICATION_FREQUENCY_CHANGE:
                setFrequency(sourceEvent.getValue().longValue());
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                setSampleRate(sourceEvent.getValue().doubleValue());
                break;

            //Request events from the consumer
            case REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE:
                setFrequencyCorrection(sourceEvent.getValue().longValue());
                break;

            default:
                mLog.error("Ignoring unrecognized source event: " + sourceEvent.getEvent() + " from [" +
                    sourceEvent.getSource().getClass() + "]");
                break;
        }
    }

    /**
     * Broadcasts the source event to the incoming sample stream producer.
     * @param sourceEvent to broadcast
     */
    protected void broadcastProducerSourceEvent(SourceEvent sourceEvent)
    {
        if(mProducerSourceEventListener != null)
        {
            mProducerSourceEventListener.receive(sourceEvent);
        }
    }

    /**
     * Broadcasts the source event to the outbound sample stream consumer.
     * @param sourceEvent to broadcast
     */
    protected void broadcastConsumerSourceEvent(SourceEvent sourceEvent)
    {
        if(mConsumerSourceEventListener != null)
        {
            mConsumerSourceEventListener.receive(sourceEvent);
        }
    }

    /**
     * Implements the ISourceEventListener interface for receiving source events (ie requests) from a consumer.
     *
     * Note: source events are simply transferred to the process() method in this class.
     */
    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mConsumerSourceEventListenerAdapter;
    }

    /**
     * Implements the ISourceEventProvider interface to set the consumer listener for source events.
     * @param listener to receive consumer source events
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mConsumerSourceEventListener = listener;
    }

    /**
     * Implements the ISourceEventProvider interface to remove the consumer listener for source events.
     */
    @Override
    public void removeSourceEventListener()
    {
        mConsumerSourceEventListener = null;
    }

    /**
     * Processor to periodically invoke buffer sample processing on an interval timer.  At each interval this processor
     * sends a heartbeat to the registered consumer and then commands the sub-class implementation to process any
     * queued buffers and distribute complex buffer sample(s) to the registered consumer.
     */
    public class BufferProcessor implements Runnable
    {
        private ScheduledFuture<?> mScheduledFuture;
        private boolean mStopped = false;

        /**
         * Commands this processor to do a shutdown at the end of this or the next iteration.  Once successfully
         * shutdown, it will invoke the performDisposal() method to cleanup this instance.
         */
        public void stop()
        {
            mStopped = true;
        }

        /**
         * Starts buffer processing on a fixed interval using a scheduled thread pool
         */
        public void start()
        {
            if(mScheduledFuture == null)
            {
                mScheduledFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this, 0,
                    BUFFER_PROCESSOR_RUN_INTERVAL_MILLISECONDS, TimeUnit.MILLISECONDS);
            }
        }

        /**
         * Implementation of the Runnable interface to periodically send a heartbeat and then process buffer samples.
         */
        @Override
        public void run()
        {
            if(!mStopped)
            {
                getHeartbeatManager().broadcast();
                processSamples();
            }

            if(mStopped)
            {
                if(mScheduledFuture != null)
                {
                    //Set may-interrupt to false so that we can complete this iteration
                    mScheduledFuture.cancel(false);
                }

                mScheduledFuture = null;

                performDisposal();
            }
        }
    }
}
