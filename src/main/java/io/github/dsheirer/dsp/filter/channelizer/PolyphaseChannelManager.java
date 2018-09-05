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
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.channelizer.output.IPolyphaseChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.OneChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.TwoChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferProvider;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Polyphase Channel Manager is a DDC channel manager and complex buffer queue/processor for a tuner.  This class
 * provides DDC polyphase channel sources and wraps a polyphase channelizer processing sample buffers produced by
 * the tuner and distributing channelized sample buffers to each allocated DDC polyphase channel source.  This
 * class is responsible for monitoring the tuner for changes in center frequency and/or sample rate and updating
 * active DDC polyphase channel sources accordingly.  This class also monitors source event requests and
 * notifications received from active DDC polyphase channel sources to adjust sample streams as required.
 *
 * Channel bandwidth and channel count are determined by the sample rate of the baseband buffer stream provider.  This
 * class is currently designed to provide channels each with a minimum usable bandwidth of 12.5 kHz and oversampled by
 * 2.0 to a minimum of 25.0 kHz channel sample rate.  If the baseband stream provider sample rate is not evenly
 * divisible by 12.5 kHz channels for an even number of channels, the channel bandwidth will be increased.
 *
 * Note: add this channel manager as a source event listener to the complex buffer provider to ensure this manager
 * adapts to changes in source frequency and sample rate.
 */
public class PolyphaseChannelManager implements ISourceEventProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelManager.class);
    private static final double MINIMUM_CHANNEL_BANDWIDTH = 12500.0;
    private static final double CHANNEL_OVERSAMPLING = 2.0;
    private static final int POLYPHASE_CHANNELIZER_TAPS_PER_CHANNEL = 9;
    private static final int POLYPHASE_SYNTHESIZER_TAPS_PER_CHANNEL = 9;

    private Broadcaster<SourceEvent> mSourceEventBroadcaster = new Broadcaster<>();
    private IReusableComplexBufferProvider mReusableBufferProvider;
    private List<PolyphaseChannelSource> mChannelSources = new CopyOnWriteArrayList<>();
    private ChannelCalculator mChannelCalculator;
    private ComplexPolyphaseChannelizerM2 mPolyphaseChannelizer;
    private ChannelSourceEventListener mChannelSourceEventListener = new ChannelSourceEventListener();
    private BufferSourceEventMonitor mBufferSourceEventMonitor = new BufferSourceEventMonitor();
    private ContinuousBufferProcessor<ReusableComplexBuffer> mBufferProcessor;
    private Map<Integer,float[]> mOutputProcessorFilters = new HashMap<>();

    /**
     * Creates a polyphase channel manager instance.
     *
     * @param reusableComplexBufferProvider (ie tuner) that supports register/deregister for reusable baseband sample buffer
     * streams
     * @param frequency of the baseband complex buffer sample stream (ie center frequency)
     * @param sampleRate of the baseband complex buffer sample stream
     */
    public PolyphaseChannelManager(IReusableComplexBufferProvider reusableComplexBufferProvider,
                                   long frequency, double sampleRate)
    {
        if(reusableComplexBufferProvider == null)
        {
            throw new IllegalArgumentException("Complex buffer provider argument cannot be null");
        }

        mReusableBufferProvider = reusableComplexBufferProvider;

        int channelCount = (int)(sampleRate / MINIMUM_CHANNEL_BANDWIDTH);

        //Ensure channel count is an even integer since we're using a 2x oversampling polyphase channelizer
        if(channelCount % 2 != 0)
        {
            channelCount--;
        }

        mChannelCalculator = new ChannelCalculator(sampleRate, channelCount, frequency, CHANNEL_OVERSAMPLING);

        mBufferProcessor = new ContinuousBufferProcessor(200, 50);
        mBufferProcessor.setListener(mBufferSourceEventMonitor);
    }

    /**
     * Creates a polyphase channel manager for the tuner controller
     *
     * @param tunerController for a tuner that provides a baseband complex buffer stream.
     */
    public PolyphaseChannelManager(TunerController tunerController)
    {
        this(tunerController, tunerController.getFrequency(), tunerController.getSampleRate());
    }

    /**
     * Current channel sample rate which is (2 * channel bandwidth).
     */
    public double getChannelSampleRate()
    {
        return mChannelCalculator.getChannelSampleRate();
    }

    /**
     * Current channel bandwidth/spacing.
     */
    public double getChannelBandwidth()
    {
        return mChannelCalculator.getChannelBandwidth();
    }

    /**
     * Provides a Digital Drop Channel (DDC) for the specified tuner channel or returns null if the channel can't be
     * sourced due to the current center frequency and/or sample rate.
     * @param tunerChannel specifying center frequency and bandwidth.
     * @return source or null.
     */
    public TunerChannelSource getChannel(TunerChannel tunerChannel)
    {
        PolyphaseChannelSource channelSource = null;

        try
        {
            List<Integer> polyphaseIndexes = mChannelCalculator.getChannelIndexes(tunerChannel);

            IPolyphaseChannelOutputProcessor outputProcessor = getOutputProcessor(polyphaseIndexes);

            if(outputProcessor != null)
            {
                long centerFrequency = mChannelCalculator.getCenterFrequencyForIndexes(polyphaseIndexes);

                channelSource = new PolyphaseChannelSource(tunerChannel, outputProcessor, mChannelSourceEventListener,
                    mChannelCalculator.getChannelSampleRate(), centerFrequency);

                mChannelSources.add(channelSource);
            }
        }
        catch(IllegalArgumentException iae)
        {
            mLog.info("Can't provide DDC for " + tunerChannel.toString() + " due to channelizer frequency [" +
                mChannelCalculator.getCenterFrequency() + "] and sample rate [" +
                (mChannelCalculator.getChannelCount() * mChannelCalculator.getChannelBandwidth()) + "]");
        }

        return channelSource;
    }

    /**
     * Creates a processor to process the channelizer channel indexes into a composite output stream providing
     * channelized complex sample buffers to a registered source listener.
     * @param indexes to target by the output processor
     * @return output processor compatible with the number of indexes to monitor
     */
    private IPolyphaseChannelOutputProcessor getOutputProcessor(List<Integer> indexes)
    {
        switch(indexes.size())
        {
            case 1:
                return new OneChannelOutputProcessor(mChannelCalculator.getChannelSampleRate(), indexes,
                    mChannelCalculator.getChannelCount());
            case 2:
                try
                {
                    float[] filter = getOutputProcessorFilter(2);
                    return new TwoChannelOutputProcessor(mChannelCalculator.getChannelSampleRate(), indexes, filter,
                        mChannelCalculator.getChannelCount());
                }
                catch(FilterDesignException fde)
                {
                    mLog.error("Error designing 2 channel synthesis filter for output processor");
                }
            default:
                //TODO: create output processor for greater than 2 input channels
                mLog.error("Request to create an output processor for unexpected channel index size:" + indexes.size());
                mLog.info(mChannelCalculator.toString());
                return null;
        }
    }

    /**
     * Starts/adds the channel source to receive channelized sample buffers, registering with the tuner to receive
     * sample buffers when this is the first channel.
     *
     * @param channelSource to start
     */
    private void startChannelSource(PolyphaseChannelSource channelSource)
    {
        synchronized(mBufferProcessor)
        {
            //Note: the polyphase channel source has already been added to the mChannelSources in getChannel() method

            checkChannelizerConfiguration();

            mPolyphaseChannelizer.addChannel(channelSource);
            mSourceEventBroadcaster.broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));

            //If this is the first channel, register to start the sample buffers flowing
            if(mPolyphaseChannelizer.getRegisteredChannelCount() == 1)
            {
                mReusableBufferProvider.addBufferListener(mBufferProcessor);
                mPolyphaseChannelizer.start();
                mBufferProcessor.start();
            }
        }
    }

    /**
     * Stops/removes the channel source from receiving channelized sample buffers and deregisters from the tuner
     * when this is the last channel being sourced.
     *
     * @param channelSource to stop
     */
    private void stopChannelSource(PolyphaseChannelSource channelSource)
    {
        synchronized(mBufferProcessor)
        {
            mChannelSources.remove(channelSource);
            mPolyphaseChannelizer.removeChannel(channelSource);
            mSourceEventBroadcaster.broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));

            //If this is the last/only channel, deregister to stop the sample buffers
            if(mPolyphaseChannelizer.getRegisteredChannelCount() == 0)
            {
                mReusableBufferProvider.removeBufferListener(mBufferProcessor);
                mBufferProcessor.stop();
                mPolyphaseChannelizer.stop();
            }
        }

        try
        {
            //Broadcast a stop sample stream notification in case this was a forced-stop so consumers are aware
            channelSource.process(SourceEvent.stopSampleStreamNotification(channelSource));
        }
        catch(SourceException se)
        {
            //Do nothing
        }
    }

    /**
     * Process source events received from the source (ie tuner controller) for frequency and sample rate change
     * notifications.
     * @param sourceEvent to process
     * @throws SourceException
     */
    @Override
    public void process(SourceEvent sourceEvent) throws SourceException
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_FREQUENCY_CHANGE:
                //Update channel calculator immediately so that channels can be allocated
                mChannelCalculator.setCenterFrequency(sourceEvent.getValue().longValue());

                //Defer channelizer configuration changes to be handled on the buffer processor thread
                mBufferSourceEventMonitor.receive(sourceEvent);
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                //Update channel calculator immediately so that channels can be allocated
                double sampleRate = sourceEvent.getValue().doubleValue();
                int channelCount = ComplexPolyphaseChannelizerM2.getChannelCount(sampleRate);
                mChannelCalculator.setRates(sampleRate, channelCount);
                break;
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_LOCKED:
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_UNLOCKED:
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                //no-op
                break;
            default:
                mLog.info("Unrecognized source event: " + sourceEvent.toString());
                break;
        }
    }

    /**
     * Creates or updates the channelizer to process the incoming sample rate and updates any channel processors.
     *
     * Note: this method should only be invoked on the mBufferProcessor thread or prior to starting the mBufferProcessor.
     * Sample rate source events will normally arrive via the incoming complex buffer stream from the mBufferProcessor
     * and will be handled as they arrive.
     */
    private void checkChannelizerConfiguration()
    {
        //Channel calculator is always in sync with the tuner's current sample rate
        double tunerSampleRate = mChannelCalculator.getSampleRate();

        //If the channelizer is not setup, or setup to the wrong sample rate, recreate it
        if(mPolyphaseChannelizer == null || Math.abs(mPolyphaseChannelizer.getSampleRate() - tunerSampleRate) > 0.5)
        {
            if(mPolyphaseChannelizer != null && mPolyphaseChannelizer.getRegisteredChannelCount() > 0)
            {
                throw new IllegalStateException("Polyphase Channelizer cannot be changed to a new sample rate while " +
                    "channels are currently sourced.  Ensure you remove all tuner channels before changing tuner " +
                    "sample rate.  Current channel count:" +
                    (mPolyphaseChannelizer != null ? mPolyphaseChannelizer.getRegisteredChannelCount() : "0"));
            }

            try
            {
                mPolyphaseChannelizer = new ComplexPolyphaseChannelizerM2(tunerSampleRate,
                    POLYPHASE_CHANNELIZER_TAPS_PER_CHANNEL);
            }
            catch(IllegalArgumentException iae)
            {
                mLog.error("Could not create polyphase channelizer for sample rate [" + tunerSampleRate + "]", iae);
            }
            catch(FilterDesignException fde)
            {
                mLog.error("Could not create filter for polyphase channelizer for sample rate [" + tunerSampleRate + "]", fde);
            }

            //Clear any previous channel synthesis filters so they can be recreated for the new channel sample rate
            mOutputProcessorFilters.clear();
        }
    }

    /**
     * Updates each of the output processors for any changes in the tuner's center frequency or sample rate, which
     * would cause the output processors to change the polyphase channelizer results channel(s) that the processor is
     * consuming
     *
     * @param sourceEvent (optional-can be null) to broadcast to each output processor following the update
     */
    private void updateOutputProcessors(SourceEvent sourceEvent)
    {
        for(PolyphaseChannelSource channelSource: mChannelSources)
        {
            updateOutputProcessor(channelSource);

            //Send the non-null source event to each channel source
            if(sourceEvent != null)
            {
                try
                {
                    channelSource.process(sourceEvent);
                }
                catch(SourceException se)
                {
                    mLog.error("Error while notifying polyphase channel source of a source event", se);
                }
            }
        }
    }

    /**
     * Updates the polyphase channel source's output processor due to a change in the center frequency or sample
     * rate for the source providing sample buffers to the polyphase channelizer, or whenever the DDC channel's
     * center tuned frequency changes.
     *
     * @param channelSource that requires an update to its output processor
     */
    private void updateOutputProcessor(PolyphaseChannelSource channelSource)
    {
        try
        {
            //If a change in sample rate or center frequency makes this channel no longer viable, then the channel
            //calculator will throw an IllegalArgException ... handled below
            List<Integer> indexes = mChannelCalculator.getChannelIndexes(channelSource.getTunerChannel());

            long centerFrequency = mChannelCalculator.getCenterFrequencyForIndexes(indexes);

            //If the indexes size is the same then update the current processor, otherwise create a new one
            IPolyphaseChannelOutputProcessor outputProcessor = channelSource.getPolyphaseChannelOutputProcessor();

            if(outputProcessor != null && outputProcessor.getInputChannelCount() == indexes.size())
            {
                channelSource.getPolyphaseChannelOutputProcessor().setPolyphaseChannelIndices(indexes);
                channelSource.setFrequency(centerFrequency);

                if(indexes.size() > 1)
                {
                    try
                    {
                        float[] filter = getOutputProcessorFilter(indexes.size());
                        channelSource.getPolyphaseChannelOutputProcessor().setSynthesisFilter(filter);
                    }
                    catch(FilterDesignException fde)
                    {
                        mLog.error("Error creating an updated synthesis filter for the channel output processor");
                    }
                }
            }
            else
            {
                channelSource.setPolyphaseChannelOutputProcessor(getOutputProcessor(indexes), centerFrequency);
            }
        }
        catch(IllegalArgumentException iae)
        {
            mLog.error("Error updating polyphase channel source - can't determine output channel indexes for " +
                "updated tuner center frequency and sample rate.  Stopping channel source", iae);

            stopChannelSource(channelSource);
        }
    }

    /**
     * Sorted set of currently sourced tuner channels being provided by this channel manager.  The set is ordered by
     * frequency (low to high).
     */
    public SortedSet<TunerChannel> getTunerChannels()
    {
        SortedSet<TunerChannel> tunerChannels = new TreeSet<>();

        for(PolyphaseChannelSource channelSource: mChannelSources)
        {
            tunerChannels.add(channelSource.getTunerChannel());
        }

        return tunerChannels;
    }

    /**
     * Count of currently sourced tuner channels
     */
    public int getTunerChannelCount()
    {
        return mChannelSources.size();
    }

    /**
     * Adds the listener to receive source events
     */
    public void addSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving source events
     */
    public void removeSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventBroadcaster.removeListener(listener);
    }

    /**
     * Generates (or reuses) an output processor filter for the specified number of channels.  Each
     * filter is created only once and stored in a map for reuse.  This map is cleared anytime that the
     * input sample rate changes, so that the filters can be recreated with the new channel sample rate.
     * @param channels count
     * @return filter
     * @throws FilterDesignException if the filter cannot be designed to specification (-6 dB band edge)
     */
    private float[] getOutputProcessorFilter(int channels) throws FilterDesignException
    {
        float[] taps = mOutputProcessorFilters.get(channels);

        if(taps == null)
        {
            taps = FilterFactory.getSincM2Synthesizer(mChannelCalculator.getChannelSampleRate(),
                mChannelCalculator.getChannelBandwidth(), channels, POLYPHASE_SYNTHESIZER_TAPS_PER_CHANNEL);

            mOutputProcessorFilters.put(channels, taps);
        }

        return taps;
    }

    /**
     * Internal class for handling requests for start/stop sample stream from polyphase channel sources
     */
    private class ChannelSourceEventListener implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                case REQUEST_START_SAMPLE_STREAM:
                    if(sourceEvent.hasSource() && sourceEvent.getSource() instanceof PolyphaseChannelSource)
                    {
                        startChannelSource((PolyphaseChannelSource)sourceEvent.getSource());
                    }
                    else
                    {
                        mLog.error("Request to start sample stream for unrecognized source: " +
                            (sourceEvent.hasSource() ? sourceEvent.getSource().getClass() : "null source"));
                    }
                    break;
                case REQUEST_STOP_SAMPLE_STREAM:
                    if(sourceEvent.hasSource() && sourceEvent.getSource() instanceof PolyphaseChannelSource)
                    {
                        stopChannelSource((PolyphaseChannelSource)sourceEvent.getSource());
                    }
                    else
                    {
                        mLog.error("Request to stop sample stream for unrecognized source: " +
                            (sourceEvent.hasSource() ? sourceEvent.getSource().getClass() : "null source"));
                    }
                    break;
                case REQUEST_SOURCE_DISPOSE:
                    Source source = sourceEvent.getSource();

                    if(source instanceof PolyphaseChannelSource)
                    {
                        source.dispose();
                    }
                    break;
                case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                    //ignore
                    break;
                case NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED:
                    //Rebroadcast so that the tuner source can process this event
                    mSourceEventBroadcaster.broadcast(sourceEvent);
                    break;
                default:
                    mLog.error("Received unrecognized source event from polyphase channel source [" +
                        sourceEvent.getEvent() + "]");
                    break;
            }
        }
    }

    /**
     * Processes the incoming buffer stream from the provider and transfers the buffers to the polyphase channelizer.
     *
     * This monitor incorporates a queue for receiving source events that affect configuration of the channelizer so
     * that they can be processed on the buffer processor calling thread, avoiding unnecessary locks on the channelizer
     * and/or the channel sources and output processors.
     */
    public class BufferSourceEventMonitor implements Listener<List<ReusableComplexBuffer>>
    {
        private Queue<SourceEvent> mQueuedSourceEvents = new ConcurrentLinkedQueue<>();

        /**
         * Queues the source event for deferred execution on the buffer processing thread.
         * @param event that affects configuration of the channelizer (frequency or sample rate change events)
         */
        public void receive(SourceEvent event)
        {
            mQueuedSourceEvents.offer(event);
        }

        @Override
        public void receive(List<ReusableComplexBuffer> reusableComplexBuffers)
        {
            try
            {
                //Process any queued source events before processing the buffers
                SourceEvent queuedSourceEvent = mQueuedSourceEvents.poll();

                while(queuedSourceEvent != null)
                {
                    switch(queuedSourceEvent.getEvent())
                    {
                        case NOTIFICATION_FREQUENCY_CHANGE:
                            //Don't send the tuner's frequency change event down to the channels - it would cause chaos
                            updateOutputProcessors(null);
                            break;
                    }

                    queuedSourceEvent = mQueuedSourceEvents.poll();
                }

                for(ReusableComplexBuffer reusableComplexBuffer: reusableComplexBuffers)
                {
                    if(mPolyphaseChannelizer != null)
                    {
                        //User count management is handled by the channelizer
                        mPolyphaseChannelizer.receive(reusableComplexBuffer);
                    }
                    else
                    {
                        reusableComplexBuffer.decrementUserCount();
                    }
                }
            }
            catch(Throwable throwable)
            {
                mLog.error("Error", throwable);
                for(ReusableComplexBuffer buffer: reusableComplexBuffers)
                {
                    buffer.decrementUserCount();
                }
            }
        }
    }
}