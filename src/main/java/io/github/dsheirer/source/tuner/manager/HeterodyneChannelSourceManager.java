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
package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexDelayBuffer;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.CICTunerChannelSource;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Channel provider for heterodyne and decimate method of channel provisioning.
 */
public class HeterodyneChannelSourceManager extends ChannelSourceManager
{
    private final static Logger mLog = LoggerFactory.getLogger(HeterodyneChannelSourceManager.class);

    private final static int DELAY_BUFFER_DURATION_MILLISECONDS = 2000;

    private List<CICTunerChannelSource> mChannelSources = new CopyOnWriteArrayList<>();
    private SortedSet<TunerChannel> mTunerChannels = new TreeSet<>();
    private TunerController mTunerController;
    private ChannelSourceEventProcessor mChannelSourceEventProcessor = new ChannelSourceEventProcessor();
    private ReusableComplexDelayBuffer mSampleDelayBuffer;

    public HeterodyneChannelSourceManager(TunerController tunerController)
    {
        mTunerController = tunerController;
        mTunerController.addListener(this);
    }

    @Override
    public SortedSet<TunerChannel> getTunerChannels()
    {
        return mTunerChannels;
    }

    @Override
    public int getTunerChannelCount()
    {
        return mTunerChannels.size();
    }

    @Override
    public TunerChannelSource getSource(TunerChannel tunerChannel, ChannelSpecification channelSpecification)
    {
        if(CenterFrequencyCalculator.canTune(tunerChannel, mTunerController, mTunerChannels))
        {
            try
            {
                //Attempt to create the channel source first, in case we get a filter design exception
                CICTunerChannelSource tunerChannelSource = new CICTunerChannelSource(mChannelSourceEventProcessor,
                    tunerChannel, mTunerController.getSampleRate(), channelSpecification);

                //Add to the list of channel sources so that it will receive the tuner frequency change
                mChannelSources.add(tunerChannelSource);

                //Set the current tuner frequency
                tunerChannelSource.setFrequency(mTunerController.getFrequency());

                //Add to the channel list and update the tuner center frequency as needed
                mTunerChannels.add(tunerChannel);
                updateTunerFrequency();

                //Lock the tuner controller frequency and sample rate
                mTunerController.setLocked(true);

                broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));

                return tunerChannelSource;
            }
            catch(FilterDesignException fde)
            {
                mLog.error("Error creating CIC tuner channel source - couldn't design cleanup filter", fde);
            }
        }

        return null;
    }

    /**
     * Calculates a new center frequency and updates the tuner center frequency
     */
    private void updateTunerFrequency()
    {
        if(!mTunerController.isTunedFor(getTunerChannels()))
        {
            long centerFrequency = CenterFrequencyCalculator.getCenterFrequency(mTunerController, getTunerChannels());

            if(centerFrequency == CenterFrequencyCalculator.INVALID_FREQUENCY)
            {
                mLog.error("Couldn't calculate center frequency for tuner and tuner channels");
                return;
            }

            if(centerFrequency != mTunerController.getFrequency())
            {
                try
                {
                    mTunerController.setFrequency(centerFrequency);
                }
                catch(SourceException se)
                {
                    mLog.error("Couldn't update tuner center frequency to " + centerFrequency, se);
                }
            }
        }
    }

    @Override
    public void process(SourceEvent tunerSourceEvent) throws SourceException
    {
        switch(tunerSourceEvent.getEvent())
        {
            case NOTIFICATION_FREQUENCY_CHANGE:
                //Tuner center frequency has changed - update channels
                updateTunerFrequency(tunerSourceEvent.getValue().longValue());

                //Clear the delay buffer since any delayed samples will be centered on the previous frequency
                if(mSampleDelayBuffer != null)
                {
                    mSampleDelayBuffer.clear();
                }
                break;
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                //The tuner is self-correcting for PPM error - relay to channels
                broadcastToChannels(tunerSourceEvent);
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_LOCKED:
            case NOTIFICATION_FREQUENCY_AND_SAMPLE_RATE_UNLOCKED:
                //no-op
                break;
            default:
                mLog.info("Unrecognized Source Event received from tuner: " + tunerSourceEvent);
        }
    }

    /**
     * Broadcasts the source event to any channel
     */
    private void broadcastToChannels(SourceEvent sourceEvent)
    {
        for(CICTunerChannelSource channelSource : mChannelSources)
        {
            try
            {
                channelSource.process(sourceEvent);
            }
            catch(Exception e)
            {
                mLog.error("Error broadcasting source event to channel: " + sourceEvent);
            }
        }
    }

    /**
     * Updates all channel sources to use the new tuner center frequency
     *
     * @param tunerFrequency in hertz
     */
    private void updateTunerFrequency(long tunerFrequency)
    {
        for(CICTunerChannelSource channelSource : mChannelSources)
        {
            channelSource.setFrequency(tunerFrequency);
        }
    }

    /**
     * Creates a complex sample delay buffer and registers it with the tuner controller to start the flow
     * of complex sample buffers from the tuner.
     */
    private void startDelayBuffer()
    {
        if(mSampleDelayBuffer == null)
        {
            int delayBufferSize = (int)(DELAY_BUFFER_DURATION_MILLISECONDS / mTunerController.getBufferDuration());
            mSampleDelayBuffer = new ReusableComplexDelayBuffer(delayBufferSize, mTunerController.getBufferDuration());

            mLog.debug("Created/registered complex sample delay buffer of size [" + delayBufferSize +
                "] buffers and delay duration [" + DELAY_BUFFER_DURATION_MILLISECONDS +
                "ms] for tuner controller [" + mTunerController.getClass().getName() +
                "] with buffer duration [" + mTunerController.getBufferDuration() + "]");

            mTunerController.addBufferListener(mSampleDelayBuffer);
        }
    }

    /**
     * Deregisters the complex sample delay buffer and disposes of any queued reusable buffers.
     */
    private void stopDelayBuffer()
    {
        if(mSampleDelayBuffer != null && !mSampleDelayBuffer.hasListeners())
        {
            mTunerController.removeBufferListener(mSampleDelayBuffer);
            mSampleDelayBuffer.dispose();
            mSampleDelayBuffer = null;
        }
    }

    /**
     * Processes channel source events
     */
    public class ChannelSourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                case REQUEST_START_SAMPLE_STREAM:
                    if(sourceEvent.getSource() instanceof CICTunerChannelSource)
                    {
                        startDelayBuffer();

                        //The start sample stream request contains a start timestamp and the delay buffer
                        //will preload the channel with delayed sample buffers that either contain the
                        //timestamp or occur later/newer than the timestamp.
                        mSampleDelayBuffer.addListener((CICTunerChannelSource)sourceEvent.getSource(),
                            sourceEvent.getValue().longValue());
                    }
                    break;
                case REQUEST_STOP_SAMPLE_STREAM:
                    if(sourceEvent.getSource() instanceof CICTunerChannelSource)
                    {
                        mSampleDelayBuffer.removeListener((CICTunerChannelSource)sourceEvent.getSource());
                        stopDelayBuffer();
                    }
                    break;
                case REQUEST_SOURCE_DISPOSE:
                    if(sourceEvent.getSource() instanceof CICTunerChannelSource)
                    {
                        CICTunerChannelSource channelSource = (CICTunerChannelSource)sourceEvent.getSource();
                        mChannelSources.remove(channelSource);
                        mTunerChannels.remove(channelSource.getTunerChannel());
                        channelSource.dispose();

                        //Unlock the tuner controller if there are no more channels
                        if(getTunerChannelCount() == 0)
                        {
                            mTunerController.setLocked(false);
                        }
                        broadcast(SourceEvent.channelCountChange(getTunerChannelCount()));
                    }
                    break;
                case NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED:
                    //Rebroadcast so that the tuner source can process this event
                    broadcast(sourceEvent);
                    break;
                case NOTIFICATION_CHANNEL_COUNT_CHANGE:
                    //Lock the tuner controller frequency & sample rate when we're processing channels
                    break;
                default:
                    mLog.info("Unrecognized Source Event received from channel: " + sourceEvent);
                    break;
            }
        }
    }
}
