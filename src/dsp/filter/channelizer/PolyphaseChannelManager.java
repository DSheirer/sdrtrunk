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

import dsp.filter.channelizer.output.IPolyphaseChannelOutputProcessor;
import dsp.filter.channelizer.output.OneChannelOutputProcessor;
import dsp.filter.channelizer.output.TwoChannelOutputProcessor;
import dsp.filter.design.FilterDesignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import source.Source;
import source.SourceEvent;
import source.SourceException;
import source.tuner.Tuner;
import source.tuner.TunerChannel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PolyphaseChannelManager implements Listener<SourceEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelManager.class);
    private static final int CHANNEL_BANDWIDTH = 12500;
    private static final double CHANNEL_OVERSAMPLING = 2.0;
    private static final int CHANNEL_SAMPLE_RATE = (int)(CHANNEL_BANDWIDTH * CHANNEL_OVERSAMPLING);
    private static final int POLYPHASE_FILTER_TAPS_PER_CHANNEL = 17;

    private Tuner mTuner;
    private List<PolyphaseChannelSource> mChannelSources = new CopyOnWriteArrayList<>();
    private ChannelCalculator mChannelCalculator;
    private ComplexPolyphaseChannelizerM2 mPolyphaseChannelizer;
    private ChannelSourceEventListener mChannelSourceEventListener = new ChannelSourceEventListener();
    private ScheduledBufferProcessor mBufferProcessor;

    /**
     * Polyphase Channel Manager is a DDC channel manager and complex buffer queue/processor for a tuner.  This class
     * provides DDC polyphase channel sources and wraps a polyphase channelizer processing sample buffers produced by
     * the tuner and distributing channelized sample buffers to each allocated DDC polyphase channel source.  This
     * class is responsible for monitoring the tuner for changes in center frequency and/or sample rate and updating
     * active DDC polyphase channel sources accordingly.  This class also monitors source event requests and
     * notifications received from active DDC polyphase channel sources to adjust sample streams as required.
     *
     * @param tuner providing complex sample buffers
     */
    public PolyphaseChannelManager(Tuner tuner)
    {
        if(tuner == null)
        {
            throw new IllegalArgumentException("Tuner argument cannot be null");
        }

        mTuner = tuner;

        int sampleRate = mTuner.getTunerController().getSampleRate();

        //Ensure that tuner sample rate is a multiple of channel sample rate since polyphase channelizer is M2
        if(sampleRate % (CHANNEL_SAMPLE_RATE) != 0)
        {
            throw new IllegalArgumentException("Tuner sample rate [" + sampleRate + "] must be an even multiple of " +
                CHANNEL_SAMPLE_RATE + " Hz");
        }

        long frequency = mTuner.getTunerController().getFrequency();
        int channelCount = sampleRate / CHANNEL_BANDWIDTH;

        mChannelCalculator = new ChannelCalculator(sampleRate, channelCount, frequency, CHANNEL_OVERSAMPLING);

        mBufferProcessor = new ScheduledBufferProcessor(500, 100, 50, 50);

        updateChannelizer(sampleRate, null);

        mTuner.addSourceEventListener(this);

    }

    /**
     * Provides a Digital Drop Channel (DDC) for the specified tuner channel or returns null if the channel can't be
     * sourced due to the current center frequency and/or sample rate.
     * @param tunerChannel specifying center frequency and bandwidth.
     * @return source or null.
     */
    public Source getChannel(TunerChannel tunerChannel)
    {
        PolyphaseChannelSource channelSource = null;

        try
        {
            List<Integer> polyphaseIndexes = mChannelCalculator.getChannelIndexes(tunerChannel);

            IPolyphaseChannelOutputProcessor outputProcessor = getOutputProcessor(polyphaseIndexes);

            if(outputProcessor != null)
            {
                channelSource = new PolyphaseChannelSource(tunerChannel, outputProcessor, mChannelSourceEventListener,
                    CHANNEL_SAMPLE_RATE);

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
                return new OneChannelOutputProcessor(CHANNEL_SAMPLE_RATE, indexes);
            case 2:
                return new TwoChannelOutputProcessor(CHANNEL_SAMPLE_RATE, indexes);
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

            if(mPolyphaseChannelizer.getRegisteredChannelCount() == 1)
            {
                mTuner.addListener(mBufferProcessor);
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

            if(mPolyphaseChannelizer.getRegisteredChannelCount() == 0)
            {
                mTuner.removeListener(mBufferProcessor);
                mBufferProcessor.stop();
            }
        }
    }

    /**
     * Implements the Listener<SourceEvent> interface for receiving notifications of frequency and sample rate
     * changes from the tuner.
     */
    @Override
    public void receive(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_FREQUENCY_CHANGE:
                //Update the index calculator and each of the output processors to update each channel frequency
                mChannelCalculator.setCenterFrequency(sourceEvent.getValue().longValue());
                updateOutputProcessors(sourceEvent);
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                //Update the channelizer (and the output processors) for the new sample rate
                updateChannelizer(sourceEvent.getValue().intValue(), sourceEvent);
                break;
        }
    }

    /**
     * Creates or updates the channelizer to process the incoming sample rate and updates any channel processors.
     *
     * @param sampleRate to use for the channelizer
     * @param sourceEvent containing a notification of sample rate change (optional - can be null).  This source
     * event will be passed to all output processors to ensure the change is fully promulgated before restarting the
     * buffer processor if it was already running.
     */
    private void updateChannelizer(int sampleRate, SourceEvent sourceEvent)
    {
        mChannelCalculator.setSampleRate(sampleRate);

        //Lock on the buffer processor to block any channel create/start/stop requests while we configure
        synchronized(mBufferProcessor)
        {
            //Get current processor state so we can restart it after we update the channelizer
            boolean bufferProcessorWasRunning = mBufferProcessor.isRunning();

            //Stop will be ignored if it is not currently running
            mBufferProcessor.stop();

            try
            {
                if(mPolyphaseChannelizer == null)
                {
                    mPolyphaseChannelizer = new ComplexPolyphaseChannelizerM2(sampleRate,
                        POLYPHASE_FILTER_TAPS_PER_CHANNEL);

                    mBufferProcessor.setListener(mPolyphaseChannelizer);
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

            if(sourceEvent != null && sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
            {
                updateOutputProcessors(sourceEvent);
            }

            if(bufferProcessorWasRunning)
            {
                mBufferProcessor.start();
            }
        }
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
            List<Integer> indexes = mChannelCalculator.getChannelIndexes(channelSource.getTunerChannel());

            //If the indexes size is the same then update the current processor, otherwise create a new one
            if(channelSource.getPolyphaseChannelOutputProcessor().getInputChannelCount() == indexes.size())
            {
                channelSource.getPolyphaseChannelOutputProcessor().setPolyphaseChannelIndices(indexes);
            }
            else
            {
                channelSource.setPolyphaseChannelOutputProcessor(getOutputProcessor(indexes));
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
}
