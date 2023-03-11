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
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.buffer.INativeBufferProvider;
import io.github.dsheirer.buffer.NativeBufferPoisonPill;
import io.github.dsheirer.controller.channel.event.ChannelStopProcessingRequest;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.util.Dispatcher;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelManager.class);
    private static final double MINIMUM_CHANNEL_BANDWIDTH = 25000.0;
    private static final double CHANNEL_OVERSAMPLING = 2.0;
    private static final int POLYPHASE_CHANNELIZER_TAPS_PER_CHANNEL = 9;

    private Broadcaster<SourceEvent> mSourceEventBroadcaster = new Broadcaster<>();
    private INativeBufferProvider mNativeBufferProvider;
    private List<PolyphaseChannelSource> mChannelSources = new CopyOnWriteArrayList<>();
    private ChannelCalculator mChannelCalculator;
    private SynthesisFilterManager mFilterManager = new SynthesisFilterManager();
    private ComplexPolyphaseChannelizerM2 mPolyphaseChannelizer;
    private ChannelSourceEventListener mChannelSourceEventListener = new ChannelSourceEventListener();
    private NativeBufferReceiver mNativeBufferReceiver = new NativeBufferReceiver();
    private Dispatcher mBufferDispatcher;
    private Map<Integer,float[]> mOutputProcessorFilters = new HashMap<>();
    private boolean mRunning = true;

    /**
     * Creates a polyphase channel manager instance.
     *
     * @param nativeBufferProvider (ie tuner) that supports register/deregister for reusable baseband sample buffer
     * streams
     * @param frequency of the baseband complex buffer sample stream (ie center frequency)
     * @param sampleRate of the baseband complex buffer sample stream
     */
    public PolyphaseChannelManager(INativeBufferProvider nativeBufferProvider, long frequency, double sampleRate)
    {
        if(nativeBufferProvider == null)
        {
            throw new IllegalArgumentException("Complex buffer provider argument cannot be null");
        }

        mNativeBufferProvider = nativeBufferProvider;

        int channelCount = (int)(sampleRate / MINIMUM_CHANNEL_BANDWIDTH);

        //Ensure channel count is an even integer since we're using a 2x oversampling polyphase channelizer
        if(channelCount % 2 != 0)
        {
            channelCount--;
        }

        mChannelCalculator = new ChannelCalculator(sampleRate, channelCount, frequency, CHANNEL_OVERSAMPLING);
        mBufferDispatcher = new Dispatcher(500, "sdrtrunk polyphase buffer processor",
                new NativeBufferPoisonPill());
        mBufferDispatcher.setListener(mNativeBufferReceiver);
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
     * Provides a description of the state of this manager.
     */
    public String getStateDescription()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Polyphase Channel Manager Providing [").append(mChannelSources.size()).append("] Channels");
        sb.append("\n\t").append(mChannelCalculator);
        for(PolyphaseChannelSource pcs: mChannelSources)
        {
            List<Integer> indexes = pcs.getOutputProcessorIndexes();
            double sampleRate = pcs.getSampleRate();
            long indexCenterFrequency = pcs.getIndexCenterFrequency();
            long appliedFrequencyOffset = pcs.getFrequencyOffset();
            long requestedCenterFrequency = pcs.getFrequency();

            sb.append("\n\tPolyphase | Tuner SR:").append(FREQUENCY_FORMAT.format(pcs.getTunerSampleRate() / 1E6d));
            sb.append(" CF:").append(FREQUENCY_FORMAT.format(pcs.getTunerCenterFrequency() / 1E6d));
            sb.append(" BW: ").append(FREQUENCY_FORMAT.format(sampleRate / 1E6d));
            sb.append(" | Channel CF: ").append(FREQUENCY_FORMAT.format(indexCenterFrequency / 1E6d));
            sb.append(" REQUESTED CF: ").append(FREQUENCY_FORMAT.format(requestedCenterFrequency / 1E6d));
            sb.append(" MIXER:").append(FREQUENCY_FORMAT.format(appliedFrequencyOffset / 1E6d));
            sb.append(" | Polyphase Indices: ").append(indexes);
        }

        return sb.toString();
    }

    public void stopAllChannels()
    {
        mRunning = false;

        List<TunerChannelSource> toStop = new ArrayList<>(mChannelSources);

        for(TunerChannelSource tunerChannelSource: toStop)
        {
            MyEventBus.getGlobalEventBus().post(new ChannelStopProcessingRequest(tunerChannelSource));
        }
    }

    /**
     * Signals to all provisioned tuner channel sources that the source complex buffer provider has an error and can
     * no longer provide channels, so that the tuner channel source can notify the consumer of the error state.
     */
    public void setErrorMessage(String errorMessage)
    {
        for(TunerChannelSource tunerChannelSource: mChannelSources)
        {
            tunerChannelSource.setError(errorMessage);
        }
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

        if(mRunning)
        {
            try
            {
                channelSource = new PolyphaseChannelSource(tunerChannel, mChannelCalculator, mFilterManager,
                        mChannelSourceEventListener);

                mChannelSources.add(channelSource);
            }
            catch(IllegalArgumentException iae)
            {
                mLog.debug("Couldn't design final output low pass filter for polyphase channel source");
            }
        }

        return channelSource;
    }

    /**
     * Starts/adds the channel source to receive channelized sample buffers, registering with the tuner to receive
     * sample buffers when this is the first channel.
     *
     * @param channelSource to start
     */
    private void startChannelSource(PolyphaseChannelSource channelSource)
    {
        synchronized(mBufferDispatcher)
        {
            //Note: the polyphase channel source has already been added to the mChannelSources in getChannel() method
            checkChannelizerConfiguration();

            mPolyphaseChannelizer.addChannel(channelSource);
            mSourceEventBroadcaster.broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));

            //If this is the first channel, register to start the sample buffers flowing
            if(mPolyphaseChannelizer.getRegisteredChannelCount() == 1)
            {
                mNativeBufferProvider.addBufferListener(mBufferDispatcher);
                mPolyphaseChannelizer.start();
                mBufferDispatcher.start();
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
        synchronized(mBufferDispatcher)
        {
            mChannelSources.remove(channelSource);
            mPolyphaseChannelizer.removeChannel(channelSource);
            mSourceEventBroadcaster.broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));

            //If this is the last/only channel, deregister to stop the sample buffers
            if(mPolyphaseChannelizer.getRegisteredChannelCount() == 0)
            {
                mNativeBufferProvider.removeBufferListener(mBufferDispatcher);
                mBufferDispatcher.stop();
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
                mNativeBufferReceiver.receive(sourceEvent);
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
            case NOTIFICATION_RECORDING_FILE_LOADED:
                //no-op
                break;
            default:
                mLog.info("Unrecognized source event: " + sourceEvent);
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
        if(mPolyphaseChannelizer == null || FastMath.abs(mPolyphaseChannelizer.getSampleRate() - tunerSampleRate) > 0.5)
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
     */
    private void updateOutputProcessors()
    {
        for(PolyphaseChannelSource channelSource: mChannelSources)
        {
            try
            {
                channelSource.updateOutputProcessor(mChannelCalculator, mFilterManager);
            }
            catch(IllegalArgumentException iae)
            {
                mLog.error("Error updating polyphase channel source output processor following tuner frequency or " +
                        "sample rate change");
                stopChannelSource(channelSource);
            }
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
                case REQUEST_SOURCE_DISPOSE:
                    Source source = sourceEvent.getSource();

                    if(source instanceof PolyphaseChannelSource)
                    {
                        source.dispose();
                    }
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
     * This monitor incorporates a source event handler that queues a center frequency update so that it can be
     * handled on the buffer processing thread, avoiding having to lock on the output processor thread.  Since we
     * anticipate that these two threads will contend for access to this update required flag, we use an update lock
     * to protect access to the flag.
     */
    public class NativeBufferReceiver implements Listener<INativeBuffer>
    {
        private boolean mOutputProcessorUpdateRequired = false;

        /**
         * Processes tuner center frequency change source events to flag when output processors need updating.
         * @param event that affects configuration of the channelizer (frequency or sample rate change events)
         */
        public void receive(SourceEvent event)
        {
            long frequency = event.getValue().longValue();

            if(mChannelCalculator.getCenterFrequency() != frequency)
            {
                //Update the channel calculator frequency so that it's ready when the output processor update occurs
                mChannelCalculator.setCenterFrequency(frequency);
                mOutputProcessorUpdateRequired = true;
            }
        }

        /**
         * Process native buffer streams and update polyphase output channels when the parent tuner center
         * frequency changes.
         * @param nativeBuffer of sample to process.
         */
        @Override
        public void receive(INativeBuffer nativeBuffer)
        {
            if(mOutputProcessorUpdateRequired)
            {
                try
                {
                    updateOutputProcessors();
                }
                catch(Exception e)
                {
                    mLog.error("Error updating polyphase channel output processors");
                }
                mOutputProcessorUpdateRequired = false;
            }

            if(mPolyphaseChannelizer != null)
            {
                Iterator<InterleavedComplexSamples> iterator = nativeBuffer.iteratorInterleaved();

                while(iterator.hasNext())
                {
                    try
                    {
                        mPolyphaseChannelizer.receive(iterator.next());
                    }
                    catch(Throwable throwable)
                    {
                        mLog.error("Error", throwable);
                    }
                }
            }
        }
    }
}