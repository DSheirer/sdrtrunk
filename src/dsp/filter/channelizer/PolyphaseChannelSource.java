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
import sample.OverflowableTransferQueue;
import sample.complex.ComplexBuffer;
import sample.real.IOverflowListener;
import source.ISourceEventProcessor;
import source.SourceEvent;
import source.SourceException;
import source.tuner.ComplexChannelSource;
import source.tuner.TunerChannel;

public class PolyphaseChannelSource extends ComplexChannelSource
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelSource.class);

    private TunerChannel mTunerChannel;
    private long mTunerCenterFrequency;
    private int mUpstreamSampleRate;
    private int mChannelSampleRate;
    private int mChannelCount;
    private int mOutputFrequencyCorrection;
    private ChannelBufferProcessor mProcessor = new ChannelBufferProcessor();

    /**
     * Polyphase channelizer channel implementation.  Adapts the channel array output samples from the polyphase
     * channelizer into a single channel, or a channel synthesized from two adjacent channels that is frequency
     * translated and decimated to a single channel.
     *
     * @param tunerChannel - requested output tuner channel frequency and bandwidth.
     */
    public PolyphaseChannelSource(ISourceEventProcessor upstreamSourceEventProcessor, TunerChannel tunerChannel)
    {
        super(upstreamSourceEventProcessor);

        mTunerChannel = tunerChannel;
    }

    /**
     * Process source event notifications received from upstream source provider and source event requests
     * received from downstream consumers.
     *
     * @param sourceEvent containing a request or notification
     * @throws SourceException if there are any errors while processing the source event
     */
    @Override
    public void process(SourceEvent sourceEvent) throws SourceException
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_CHANNEL_SAMPLE_RATE_CHANGE:
                setChannelSampleRate(sourceEvent.getValue().intValue());
                break;
            case NOTIFICATION_FREQUENCY_CHANGE:
                mTunerCenterFrequency = sourceEvent.getValue().longValue();
                updateInputConfiguration();
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                setUpstreamSampleRate(sourceEvent.getValue().intValue());
                updateInputConfiguration();
                break;
            case REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE:
                setFrequencyCorrection(sourceEvent.getValue().intValue());
                break;
        }
    }

    /**
     * Process the array samples for each of the output channels from the polyphase channelizer.
     * @param channelsBuffer
     */
    public void processChannels(ComplexBuffer channelsBuffer)
    {
        mProcessor.process(channelsBuffer);
    }

    /**
     * Updates current processing configuration any time there is a change in tuner center frequency
     * or tuner sample rate.
     */
    private void updateInputConfiguration()
    {

    }

    /**
     * Downstream sample rate
     *
     * @return sample rate in Hertz
     * @throws SourceException if this method is accessed before the channel count is established
     */
    @Override
    public int getSampleRate() throws SourceException
    {
        if(mChannelSampleRate > 0)
        {
            return mChannelSampleRate;
        }

        throw new SourceException("Channel sample rate has not been set.");
    }

    /**
     * Sets the downstream channel sample rate.  Since oversampling may occur in the upstream channelizer, we can't
     * rely on simply dividing the upstream sample rate by the number of channels.  Thus, we required the upstream
     * channelizer to tell us the exact channel sample rate.
     *
     * @param channelSampleRate in hertz
     */
    private void setChannelSampleRate(int channelSampleRate)
    {
        mChannelSampleRate = channelSampleRate;

        //Translate the upstream channel sample rate event into a downstream sample rate change event
        broadcast(SourceEvent.sampleRateChange(mChannelSampleRate));
    }

    /**
     * Sets the input sample rate and broadcasts a source change event to the downstream listener
     * @param sampleRate from the upstream source
     */
    private void setUpstreamSampleRate(int sampleRate)
    {
        mUpstreamSampleRate = sampleRate;

        try
        {
            broadcast(SourceEvent.sampleRateChange(getSampleRate()));
        }
        catch(SourceException se)
        {
            mLog.error("Upstream sample rate changed - couldn't broadcast channel sample rate change event " +
                "to downstream channel", se);
        }
    }

    /**
     * Sets the polyphase channel count.
     * @param channelCount number of channels being processed by the polyphase channelizer
     */
    private void setChannelCount(int channelCount)
    {
        mChannelCount = channelCount;

        try
        {
            broadcast(SourceEvent.sampleRateChange(getSampleRate()));
        }
        catch(SourceException se)
        {
            mLog.error("Polyphase channel count changed - couldn't broadcast channel sample rate change event " +
                "to downstream channel", se);
        }
    }

    @Override
    public long getFrequency() throws SourceException
    {
        return mTunerChannel.getFrequency();
    }

    private void setFrequencyCorrection(int correction)
    {
        //TODO:
        mLog.debug("Request for frequency correction: " + correction + " -- not yet implemented");
    }

    @Override
    public void reset()
    {

    }


    @Override
    public void dispose()
    {

    }

    public class ChannelBufferProcessor implements Runnable
    {
        private OverflowableTransferQueue<ComplexBuffer> mComplexChannelBuffers;

        public ChannelBufferProcessor()
        {
            mComplexChannelBuffers = new OverflowableTransferQueue<>(500, 10);
            mComplexChannelBuffers.setOverflowListener(new IOverflowListener()
            {
                @Override
                public void sourceOverflow(boolean overflow)
                {
                    broadcastOverflowState(overflow);
                }
            });
        }

        /**
         * Enqueues the polyphase analysis filter's channel output for post channelizer mixing, filtering
         * and resampling operations and buffers output for dispatch to downstream processing.
         *
         * @param channelsBuffer to enqueue for processing
         */
        public void process(ComplexBuffer channelsBuffer)
        {
            mComplexChannelBuffers.offer(channelsBuffer);
        }

        @Override
        public void run()
        {
            try
            {

            }
            catch(Throwable throwable)
            {
                mLog.error("Error while processing polyphase channel samples", throwable);
            }
        }
    }
}
