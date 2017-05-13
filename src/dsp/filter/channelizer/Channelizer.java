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

public class Channelizer implements ISourceEventProcessor, Listener<SourceEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(Channelizer.class);
    private static final int CHANNEL_BANDWIDTH = 12500;
    private static final double CHANNEL_OVERSAMPLING = 2.0;

    private Tuner mTuner;
    private List<PolyphaseChannelSource> mChannelSources = new CopyOnWriteArrayList<>();
    private ChannelizerConfiguration mChannelizerConfiguration;

    /**
     * Provides access to Digital Drop Channel (DDC) sources from the tuner.  Incorporates a polyphase channelizer
     * with downstream channel and upstream tuner management.
     *
     * @param tuner providing broadband sample buffers
     */
    public Channelizer(Tuner tuner)
    {
        mTuner = tuner;

        int sampleRate = mTuner.getTunerController().getSampleRate();

        if(sampleRate % 25000 != 0)
        {
            throw new IllegalArgumentException("Tuner sample rate [" + sampleRate + "] must be a multiple of 25 kHz");
        }

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
        try
        {
            List<Integer> polyphaseIndexes = mChannelizerConfiguration.getPolyphaseChannelIndexes(tunerChannel);



        }
        catch(IllegalArgumentException iae)
        {
            mLog.info("Can't provide DDC for " + tunerChannel.toString() + " due to channelizer frequency [" +
                " due to channelizer frequency [" + mChannelizerConfiguration.getCenterFrequency() +
                "] and sample rate [" + (mChannelizerConfiguration.getChannelCount() *
                mChannelizerConfiguration.getChannelBandwidth()) + "]");
        }

        return null;
    }

    /**
     * Starts/adds the channel source to receive channelized sample buffers, registering with the tuner to receive
     * sample buffers when this is the first channel.
     *
     * @param channelSource to start
     */
    private void startChannelSource(PolyphaseChannelSource channelSource)
    {
        if(mChannelSources.isEmpty())
        {
            //TODO: start the tuner
        }

        mChannelSources.add(channelSource);
    }

    /**
     * Stops/removes the channel source from receiving channelized sample buffers and deregisters from the tuner
     * if this is the last channel being sourced.
     *
     * @param channelSource to stop
     */
    private void stopChannelSource(PolyphaseChannelSource channelSource)
    {
        mChannelSources.remove(channelSource);

        if(mChannelSources.isEmpty())
        {
            //TODO: stop the tuner
        }

        if(mTuner != null)
        {
            //TODO: change the tuner to accept a remove(TunerChannel) method
//            mTuner.remove(channel.getTunerChannel());
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
