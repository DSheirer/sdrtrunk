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

import io.github.dsheirer.dsp.filter.channelizer.output.IPolyphaseChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.OneChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.TwoChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.IComplexBufferProvider;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

public class PolyphaseChannelManager
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelManager.class);
    private static final double MINIMUM_CHANNEL_BANDWIDTH = 12500.0;
    private static final double CHANNEL_OVERSAMPLING = 2.0;
    private static final int POLYPHASE_FILTER_TAPS_PER_CHANNEL = 17;

    private Broadcaster<SourceEvent> mSourceEventBroadcaster = new Broadcaster<>();
    private IComplexBufferProvider mComplexBufferProvider;
    private List<PolyphaseChannelSource> mChannelSources = new CopyOnWriteArrayList<>();
    private ChannelCalculator mChannelCalculator;
    private ComplexPolyphaseChannelizerM2 mPolyphaseChannelizer;
    private ChannelSourceEventListener mChannelSourceEventListener = new ChannelSourceEventListener();
    private BufferSourceEventMonitor mBufferSourceEventMonitor = new BufferSourceEventMonitor();
    private ScheduledBufferProcessor mBufferProcessor;

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
     * @param complexBufferProvider that supports register/deregister of this channel manager
     * @param frequency of the provided complex buffer samples
     * @param sampleRate of the provided complex buffer samples
     */
    public PolyphaseChannelManager(IComplexBufferProvider complexBufferProvider, long frequency, double sampleRate)
    {
        if(complexBufferProvider == null)
        {
            throw new IllegalArgumentException("Complex buffer provider argument cannot be null");
        }

        mComplexBufferProvider = complexBufferProvider;

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
            mLog.debug("Getting polyphase channel for: " + tunerChannel + " indexes:" + polyphaseIndexes);

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
                return new TwoChannelOutputProcessor(mChannelCalculator.getChannelSampleRate(), indexes);
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
                mComplexBufferProvider.addComplexBufferListener(mBufferProcessor);
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
                mComplexBufferProvider.removeComplexBufferListener(mBufferProcessor);
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
        if(mPolyphaseChannelizer == null || Math.abs(mPolyphaseChannelizer.getSampleRate() - sampleRate) > 1.0)
        {
            mLog.debug("Creating or Updating Channelizer for sample rate " + sampleRate);
            mChannelCalculator.setSampleRate(sampleRate);

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

            //Broadcast the new channel sample rate
            double updatedChannelSampleRate = mChannelCalculator.getChannelSampleRate();
            updateOutputProcessors(SourceEvent.sampleRateChange(updatedChannelSampleRate));
        }
        else
        {
            mLog.debug("Request to update current sample rate ignored: " + sampleRate);
        }
    }

    private void updateChannelizerCenterFrequency(long frequency)
    {
        mChannelCalculator.setCenterFrequency(frequency);

        mLog.debug("Updating output processors - center frequency is now: " + frequency);

        updateOutputProcessors(null);
    }

    /**
     * Updates each of the output processors for any changes in the tuner's center frequency or sample rate, which
     * would cause the output processors to change the polyphase channelizer results channel(s) that the processor is
     * consuming
     *
     * @param sourceEvent that requires an update to the output processors
     */
    private void updateOutputProcessors(SourceEvent sourceEvent)
    {
        for(PolyphaseChannelSource channelSource: mChannelSources)
        {
            updateOutputProcessor(channelSource);

            //Notify the channel source that the frequency or sample rate has changed so that it can reset
            //any frequency correction value.
            if(sourceEvent != null)
            {
                try
                {
//TODO: this notification event should go to the channelizer to be embedded in the channel buffer stream
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
                mLog.debug("Updating indexes to " + indexes + " for: " + channelSource.getTunerChannel());
                channelSource.getPolyphaseChannelOutputProcessor().setPolyphaseChannelIndices(indexes);
                channelSource.setFrequency(centerFrequency);
            }
            else
            {
                mLog.debug("Creating new output processor for indexes " + indexes);
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
     * Processes the incoming buffer stream from the provider to detect frequency and sample rate change source events
     * and update the channelizer configuration as necessary.
     */
    public class BufferSourceEventMonitor implements Listener<ComplexBuffer>
    {
        @Override
        public void receive(ComplexBuffer complexBuffer)
        {
            if(complexBuffer.hasSourceEvent())
            {
                process(complexBuffer.getSourceEvent());
            }

            if(mPolyphaseChannelizer != null)
            {
                mPolyphaseChannelizer.receive(complexBuffer);
            }
        }

        /**
         * Processes frequency and sample rate change events that are embedded in the complex buffer stream from the
         * tuner or sample source
         */
        public void process(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                case NOTIFICATION_FREQUENCY_CHANGE:
                    //Update the index calculator and each of the output processors to update each channel frequency
                    mChannelCalculator.setCenterFrequency(sourceEvent.getValue().longValue());
                    mLog.debug("Updating output processors - center frequency is now: " + sourceEvent.getValue().longValue());
                    updateOutputProcessors(sourceEvent);
                    break;
                case NOTIFICATION_SAMPLE_RATE_CHANGE:
                    //Update the channelizer (and the output processors) for the new sample rate
                    updateChannelizerSampleRate(sourceEvent.getValue().intValue());
                    break;
                default:
                    mLog.info("Received an unrecognized source event: " + sourceEvent.getEvent());
                    break;
            }
        }
    }
}