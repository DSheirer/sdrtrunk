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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import source.ISourceEventProcessor;
import source.Source;
import source.SourceEvent;
import source.tuner.Tuner;
import source.tuner.TunerChannel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class PolyphaseChannelManager implements ISourceEventProcessor, Listener<SourceEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelManager.class);
    private static final int CHANNEL_BANDWIDTH = 12500;
    private static final int CHANNEL_SAMPLE_RATE = CHANNEL_BANDWIDTH * 2;
    private static final double CHANNEL_OVERSAMPLING = 2.0;

    private Tuner mTuner;
    private List<PolyphaseChannelSource> mChannelSources = new CopyOnWriteArrayList<>();
    private ChannelizerConfiguration mChannelizerConfiguration;
    private ComplexPolyphaseChannelizerM2 mPolyphaseChannelizer;

    /**
     * Provides access to Digital Drop Channel (DDC) sources from a tuner.  Incorporates a polyphase channelizer
     * with downstream channel and upstream tuner management.
     *
     * @param tuner providing broadband sample buffers
     */
    public PolyphaseChannelManager(Tuner tuner)
    {
        if(tuner == null)
        {
            throw new IllegalArgumentException("Tuner argument cannot be null");
        }

        int sampleRate = mTuner.getTunerController().getSampleRate();

        //Ensure that tuner sample rate is a multiple of channel sample rate since polyphase channelizer is M2
        if(sampleRate % (CHANNEL_SAMPLE_RATE) != 0)
        {
            throw new IllegalArgumentException("Tuner sample rate [" + sampleRate + "] must be a multiple of " +
                CHANNEL_SAMPLE_RATE + " Hz");
        }

        mTuner = tuner;

        int channelCount = sampleRate / CHANNEL_BANDWIDTH;

        long frequency = mTuner.getTunerController().getFrequency();

        mChannelizerConfiguration = new ChannelizerConfiguration(frequency, channelCount,
            CHANNEL_BANDWIDTH, CHANNEL_OVERSAMPLING);

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

        IPolyphaseChannelOutputProcessor outputProcessor = null;

        try
        {
            List<Integer> polyphaseIndexes = mChannelizerConfiguration.getPolyphaseChannelIndexes(tunerChannel);

            switch(polyphaseIndexes.size())
            {
                case 1:
                    outputProcessor = new OneChannelOutputProcessor(CHANNEL_SAMPLE_RATE, polyphaseIndexes);
                    break;
                case 2:
                    outputProcessor = new TwoChannelOutputProcessor(CHANNEL_SAMPLE_RATE, polyphaseIndexes);
                    break;
                default:
                    //TODO: create output processor for greater than 2 input channels
                    break;
            }
        }
        catch(IllegalArgumentException iae)
        {
            mLog.info("Can't provide DDC for " + tunerChannel.toString() + " due to channelizer frequency [" +
                mChannelizerConfiguration.getCenterFrequency() + "] and sample rate [" +
                (mChannelizerConfiguration.getChannelCount() * mChannelizerConfiguration.getChannelBandwidth()) + "]");
        }

        if(outputProcessor != null)
        {
            channelSource = new PolyphaseChannelSource(tunerChannel, outputProcessor, this);
            mChannelSources.add(channelSource);
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
        mPolyphaseChannelizer.addChannel(channelSource);

        if(mPolyphaseChannelizer.getRegisteredChannelCount() == 1)
        {
            mTuner.addListener(mPolyphaseChannelizer);
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
        mChannelSources.remove(channelSource);

        if(mPolyphaseChannelizer.getRegisteredChannelCount() == 0)
        {
            mTuner.removeListener(mPolyphaseChannelizer);
        }
    }

    @Override
    public void process(SourceEvent sourceEvent)
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
                    mLog.error("Request to stop sample stream for unrecognized source: " +
                        (sourceEvent.hasSource() ? sourceEvent.getSource().getClass() : "null source"));
                }
                //TODO: add the channel and register for samples from the tuner
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
            case NOTIFICATION_FREQUENCY_CHANGE:
                mChannelizerConfiguration.setCenterFrequency(sourceEvent.getValue().longValue());
                updateChannelResultsProcessors();
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                int sampleRate = sourceEvent.getValue().intValue();
                int channelCount = sampleRate / mChannelizerConfiguration.getChannelBandwidth();
                mChannelizerConfiguration.setChannelCount(channelCount);
                updateChannelResultsProcessors();
                break;
        }



    }

    @Override
    public void receive(SourceEvent sourceEvent)
    {
        process(sourceEvent);
    }

    private void updateChannelResultsProcessors()
    {
        //Update the processors anytime that the center frequency or sample rate changes
    }
}
