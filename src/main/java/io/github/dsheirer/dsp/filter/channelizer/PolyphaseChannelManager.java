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
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.channelizer.output.IPolyphaseChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.OneChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.TwoChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.reusable.IReusableComplexBufferProvider;
import io.github.dsheirer.sample.complex.reusable.ReusableComplexBuffer;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class PolyphaseChannelManager implements ISourceEventProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelManager.class);
    private static final double MINIMUM_CHANNEL_BANDWIDTH = 12500.0;
    private static final double CHANNEL_OVERSAMPLING = 2.0;
    private static final int POLYPHASE_FILTER_TAPS_PER_CHANNEL = 17;

    private Broadcaster<SourceEvent> mSourceEventBroadcaster = new Broadcaster<>();
    private IReusableComplexBufferProvider mReusableBufferProvider;
    private List<PolyphaseChannelSource> mChannelSources = new CopyOnWriteArrayList<>();
    private ChannelCalculator mChannelCalculator;
    private ComplexPolyphaseChannelizerM2 mPolyphaseChannelizer;
    private ChannelSourceEventListener mChannelSourceEventListener = new ChannelSourceEventListener();
    private BufferSourceEventMonitor mBufferSourceEventMonitor = new BufferSourceEventMonitor();
    private ScheduledBufferProcessor mBufferProcessor;
    private Map<Integer,float[]> mOutputProcessorFilters = new HashMap<>();

    /**
     * Polyphase Channel Manager is a DDC channel manager and complex buffer queue/processor for a tuner.  This class
     * provides DDC polyphase channel sources and wraps a polyphase channelizer processing sample buffers produced by
     * the tuner and distributing channelized sample buffers to each allocated DDC polyphase channel source.  This
     * class is responsible for monitoring the tuner for changes in center frequency and/or sample rate and updating
     * active DDC polyphase channel sources accordingly.  This class also monitors source event requests and
     * notifications received from active DDC polyphase channel sources to adjust sample streams as required.
     *
     * Note: add this channel manager as a source event listener to the complex buffer provider to ensure this manager
     * adapts to changes in source frequency and sample rate.
     *
     * @param reusableBufferProvider that supports register/deregister of this channel manager
     * @param frequency of the provided complex buffer samples
     * @param sampleRate of the provided complex buffer samples
     */
    public PolyphaseChannelManager(IReusableComplexBufferProvider reusableBufferProvider, long frequency, double sampleRate)
    {
        if(reusableBufferProvider == null)
        {
            throw new IllegalArgumentException("Complex buffer provider argument cannot be null");
        }

        mReusableBufferProvider = reusableBufferProvider;

        int channelCount = (int)(sampleRate / MINIMUM_CHANNEL_BANDWIDTH);

        //Ensure channel count is an even integer since we're using a 2x oversampling polyphase channelizer
        if(channelCount % 2 != 0)
        {
            channelCount--;
        }

        mChannelCalculator = new ChannelCalculator(sampleRate, channelCount, frequency, CHANNEL_OVERSAMPLING);

        mBufferProcessor = new ScheduledBufferProcessor(500, 100, 50, 50);
        mBufferProcessor.setListener(mBufferSourceEventMonitor);

        updateChannelizerSampleRate(sampleRate);
    }

    /**
     * Creates a polyphase channel manager for the tuner controller
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
                channelSource = new PolyphaseChannelSource(tunerChannel, outputProcessor, mChannelSourceEventListener,
                    mChannelCalculator.getChannelSampleRate());

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

    public TunerChannelSource getSpecialChannel(int index)
    {
        PolyphaseChannelSource channelSource = null;

        try
        {
            List<Integer> polyphaseIndexes = new ArrayList<>();
            polyphaseIndexes.add(index);

            IPolyphaseChannelOutputProcessor outputProcessor = getOutputProcessor(polyphaseIndexes);

            TunerChannel tunerChannel = new TunerChannel(100000000, 12500);

            if(outputProcessor != null)
            {
                channelSource = new PolyphaseChannelSource(tunerChannel, outputProcessor, mChannelSourceEventListener,
                    mChannelCalculator.getChannelSampleRate());
            }
        }
        catch(IllegalArgumentException iae)
        {
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
                return new OneChannelOutputProcessor(mChannelCalculator.getChannelSampleRate(), indexes);
            case 2:
                try
                {
                    float[] filter = getOutputProcessorFilter(2);
                    return new TwoChannelOutputProcessor(mChannelCalculator.getChannelSampleRate(), indexes, filter);
                }
                catch(FilterDesignException fde)
                {
                    mLog.error("Error designing 2 channel synthesis filter for output processor");
                }
            default:
                //TODO: create output processor for greater than 2 input channels
                mLog.error("Request to create an output processor for unexpected channel index size:" + indexes.size());
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
            mPolyphaseChannelizer.addChannel(channelSource);
            mSourceEventBroadcaster.broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));

            if(mPolyphaseChannelizer.getRegisteredChannelCount() == 1)
            {
                mReusableBufferProvider.addBufferListener(mBufferProcessor);
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
            mSourceEventBroadcaster.broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));

            if(mPolyphaseChannelizer.getRegisteredChannelCount() == 0)
            {
                mReusableBufferProvider.removeBufferListener(mBufferProcessor);
                mBufferProcessor.stop();
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

                mLog.debug("Tuner frequency change: " + sourceEvent.getValue().longValue());
                //Defer channelizer configuration changes to be handled on the buffer processor thread
                mBufferSourceEventMonitor.receive(sourceEvent);
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                //Update channel calculator immediately so that channels can be allocated
                mChannelCalculator.setSampleRate(sourceEvent.getValue().doubleValue());

                //Defer channelizer configuration changes to be handled on the buffer processor thread
                mBufferSourceEventMonitor.receive(sourceEvent);
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
     *
     * @param sampleRate to use for the channelizer
     */
    private void updateChannelizerSampleRate(double sampleRate)
    {
        if(mPolyphaseChannelizer == null || Math.abs(mPolyphaseChannelizer.getSampleRate() - sampleRate) > 0.5)
        {
            mLog.debug("Creating or Updating Channelizer for sample rate " + sampleRate);

            try
            {
                if(mPolyphaseChannelizer == null)
                {
                    mPolyphaseChannelizer = new ComplexPolyphaseChannelizerM2(sampleRate,
                        POLYPHASE_FILTER_TAPS_PER_CHANNEL);
                }
                else
                {
                    mPolyphaseChannelizer.setSampleRate(sampleRate);
                }
            }
            catch(IllegalArgumentException iae)
            {
                mLog.error("Could not create polyphase channelizer for sample rate [" + sampleRate + "]", iae);
            }
            catch(FilterDesignException fde)
            {
                mLog.error("Could not create filter for polyphase channelizer for sample rate [" + sampleRate + "]", fde);
            }

            //Clear the previous channel synthesis filters so they can be recreated for the new channel sample rate
            mOutputProcessorFilters.clear();

            //Broadcast the new channel sample rate
            double updatedChannelSampleRate = mChannelCalculator.getChannelSampleRate();
            updateOutputProcessors(SourceEvent.sampleRateChange(updatedChannelSampleRate));
        }
        else
        {
            mLog.debug("Request to update current sample rate ignored: " + sampleRate);
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
                    mLog.debug("**Sending source event to channel: " + sourceEvent.toString());
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
            if(channelSource.getPolyphaseChannelOutputProcessor().getInputChannelCount() == indexes.size())
            {
                mLog.debug("Updating existing output processor - indexes:" + indexes + " freq:" + centerFrequency);
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
                mLog.debug("Setting new output processor - indexes:" + indexes + " freq:" + centerFrequency);
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
            taps = FilterFactory.getSincM2Synthesizer(mChannelCalculator.getChannelBandwidth(), channels,
                POLYPHASE_FILTER_TAPS_PER_CHANNEL, Window.WindowType.BLACKMAN_HARRIS_7, true);

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
                default:
                    mLog.error("Received unrecognized source event from polyphase channel source [" +
                        sourceEvent.getEvent() + "]");
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
    public class BufferSourceEventMonitor implements Listener<ReusableComplexBuffer>
    {
        private Queue<SourceEvent> mQueuedSourceEvents = new ConcurrentLinkedQueue<>();

        /**
         * Queues the source event for deferred execution on the buffer processing thread.
         * @param event that affects configuration of the channelizer (frequency or sample rate change events)
         */
        public void receive(SourceEvent event)
        {
            mLog.debug("Got a source event: " + event.toString());
            mQueuedSourceEvents.offer(event);
        }

        @Override
        public void receive(ReusableComplexBuffer reusableComplexBuffer)
        {
            try
            {
                //Process any queued source events before processing the buffer
                SourceEvent queuedSourceEvent = mQueuedSourceEvents.poll();

                while(queuedSourceEvent != null)
                {
                    switch(queuedSourceEvent.getEvent())
                    {
                        case NOTIFICATION_FREQUENCY_CHANGE:
                            //Don't send the tuner's frequency change event down to the channels - it would cause chaos
                            updateOutputProcessors(null);
                            break;
                        case NOTIFICATION_SAMPLE_RATE_CHANGE:
                            updateChannelizerSampleRate(queuedSourceEvent.getValue().intValue());
                            break;
                    }

                    queuedSourceEvent = mQueuedSourceEvents.poll();
                }

                if(mPolyphaseChannelizer != null)
                {
                    mPolyphaseChannelizer.receive(reusableComplexBuffer.incrementUserCount());
                }
            }
            catch(Throwable throwable)
            {
                mLog.error("Error", throwable);
            }

            reusableComplexBuffer.decrementUserCount();
        }
    }
}