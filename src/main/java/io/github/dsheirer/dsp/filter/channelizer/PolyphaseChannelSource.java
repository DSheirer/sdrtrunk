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

import io.github.dsheirer.dsp.filter.channelizer.output.IPolyphaseChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.OneChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.channelizer.output.TwoChannelOutputProcessor;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.StreamProcessorWithHeartbeat;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Polyphase Channelizer's Tuner Channel Source implementation.  Wraps a ChannelOutputProcessor instance and
 * provides tuner channel source functionality.  Supports dynamic swapout of the underlying channel output processor
 * as the upstream polyphase channelizer reconfigures during tuned center frequency changes.
 */
public class PolyphaseChannelSource extends TunerChannelSource implements Listener<ComplexSamples>
{
    private Logger mLog = LoggerFactory.getLogger(PolyphaseChannelSource.class);
    private IPolyphaseChannelOutputProcessor mPolyphaseChannelOutputProcessor;
    private StreamProcessorWithHeartbeat<ComplexSamples> mStreamHeartbeatProcessor;
    private double mChannelSampleRate;
    private long mIndexCenterFrequency;
    private List<Integer> mOutputProcessorIndexes = new ArrayList<>();
    private double mTunerSampleRate;
    private double mTunerCenterFrequency;
    private PendingOutputProcessorUpdate mPendingOutputProcessorUpdate;

    /**
     * Constructs an instance
     *
     * @param tunerChannel describing the desired channel frequency and bandwidth/minimum sample rate
     * @param channelCalculator for current channel center frequency and sample rate and index calculations
     * @param filterManager for access to new or cached synthesis filters
     * @param producerSourceEventListener to receive source event requests (e.g. start/stop sample stream)
     * @throws IllegalArgumentException if a channel low pass filter can't be designed to the channel specification
     */
    public PolyphaseChannelSource(TunerChannel tunerChannel, ChannelCalculator channelCalculator, SynthesisFilterManager filterManager,
                                  Listener<SourceEvent> producerSourceEventListener) throws IllegalArgumentException
    {
        super(producerSourceEventListener, tunerChannel);
        mChannelSampleRate = channelCalculator.getChannelSampleRate();
        mStreamHeartbeatProcessor = new StreamProcessorWithHeartbeat<>(getHeartbeatManager(), HEARTBEAT_INTERVAL_MS);
        doUpdateOutputProcessor(channelCalculator, filterManager);
    }

    /**
     * Description of the state or configuration of the output processor.
     */
    public String getStateDescription()
    {
        return mPolyphaseChannelOutputProcessor.getStateDescription();
    }

    /**
     * Current output processor indexes.
     * @return indexes
     */
    public List<Integer> getOutputProcessorIndexes()
    {
        return mOutputProcessorIndexes;
    }

    /**
     * Sample rate or bandwidth of the tuner providing input to the channelizer.
     * @return sample rate
     */
    public double getTunerSampleRate()
    {
        return mTunerSampleRate;
    }

    /**
     * Center tuned frequency of the tuner providing input to the channelizer.
     * @return center frequency
     */
    public double getTunerCenterFrequency()
    {
        return mTunerCenterFrequency;
    }

    @Override
    public void start()
    {
        super.start();

        mStreamHeartbeatProcessor.start();

        if(mPolyphaseChannelOutputProcessor != null)
        {
            mPolyphaseChannelOutputProcessor.start();
        }
    }

    @Override
    public void stop()
    {
        super.stop();

        if(mPolyphaseChannelOutputProcessor != null)
        {
            mPolyphaseChannelOutputProcessor.stop();
        }

        mStreamHeartbeatProcessor.stop();
    }

    /**
     * Registers the listener to receive complex sample buffers from this channel source
     */
    @Override
    public void setListener(final Listener<ComplexSamples> listener)
    {
        mStreamHeartbeatProcessor.setListener(listener);
        mPolyphaseChannelOutputProcessor.setListener(this);
    }

    @Override
    public void receive(ComplexSamples complexSamples)
    {
        mStreamHeartbeatProcessor.receive(complexSamples);
    }

    /**
     * Queues a request to update the output processor whenever the source tuner's center frequency changes.
     * @param channelCalculator providing access to updated tuner center frequency, sample rate, etc.
     * @param filterManager for designing and caching synthesis filters
     */
    public void updateOutputProcessor(ChannelCalculator channelCalculator, SynthesisFilterManager filterManager)
            throws IllegalArgumentException
    {
        mPendingOutputProcessorUpdate = new PendingOutputProcessorUpdate(channelCalculator, filterManager);
    }

    /**
     * Performs the output processor update.  This method must be invoked on the channel results calling thread to
     * ensure we don't have thread contention on the output processor.
     * @param channelCalculator for updates
     * @param filterManager for updates
     * @throws IllegalArgumentException if there's an issue.
     */
    public void doUpdateOutputProcessor(ChannelCalculator channelCalculator, SynthesisFilterManager filterManager)
            throws IllegalArgumentException
    {
        String errorMessage = null;

        //If a change in sample rate or center frequency makes this channel no longer viable, then the channel
        //calculator will throw an IllegalArgException ... handled below
        List<Integer> indexes = channelCalculator.getChannelIndexes(getTunerChannel());

        mOutputProcessorIndexes.clear();
        mOutputProcessorIndexes.addAll(indexes);
        mTunerCenterFrequency = channelCalculator.getCenterFrequency();
        mTunerSampleRate = channelCalculator.getSampleRate();

        //The provided channels are necessarily aligned to the center frequency that this source is providing and an
        //oscillator will mix the provided channels to bring the desired center frequency to baseband.
        setFrequency(channelCalculator.getCenterFrequencyForIndexes(indexes));

        if(mPolyphaseChannelOutputProcessor != null && mPolyphaseChannelOutputProcessor.getInputChannelCount() == indexes.size())
        {
            mPolyphaseChannelOutputProcessor.setPolyphaseChannelIndices(indexes);

            if(indexes.size() > 1)
            {
                try
                {
                    float[] filter = filterManager.getFilter(channelCalculator.getChannelSampleRate(),
                            channelCalculator.getChannelBandwidth(), indexes.size());
                    mPolyphaseChannelOutputProcessor.setSynthesisFilter(filter);
                }
                catch(FilterDesignException fde)
                {
                    mLog.error("Error creating an updated synthesis filter for the channel output processor");
                    errorMessage ="Cannot update output processor - unable to design synthesis filter for [" +
                        indexes.size() + "] channel indices - channel sample rate [" + channelCalculator.getChannelSampleRate() +
                        "] channel bandwidth [" + channelCalculator.getChannelBandwidth() + "]";
                }
            }

            mPolyphaseChannelOutputProcessor.setFrequencyOffset(getFrequencyOffset());
        }
        else //Create a new output processor.
        {
            if(mPolyphaseChannelOutputProcessor != null)
            {
                mPolyphaseChannelOutputProcessor.setListener(null);
                mPolyphaseChannelOutputProcessor.stop();
            }

            mPolyphaseChannelOutputProcessor = null;

            switch(indexes.size())
            {
                case 1:
                    mPolyphaseChannelOutputProcessor = new OneChannelOutputProcessor(channelCalculator.getChannelSampleRate(),
                            indexes, channelCalculator.getChannelCount());
                    mPolyphaseChannelOutputProcessor.setListener(this);
                    mPolyphaseChannelOutputProcessor.setFrequencyOffset(getFrequencyOffset());
                    mPolyphaseChannelOutputProcessor.start();
                    break;
                case 2:
                    try
                    {
                        float[] filter = filterManager.getFilter(channelCalculator.getChannelSampleRate(),
                                channelCalculator.getChannelBandwidth(), 2);
                        mPolyphaseChannelOutputProcessor = new TwoChannelOutputProcessor(channelCalculator.getChannelSampleRate(),
                                indexes, filter, channelCalculator.getChannelCount());
                        mPolyphaseChannelOutputProcessor.setListener(this);
                        mPolyphaseChannelOutputProcessor.setFrequencyOffset(getFrequencyOffset());
                        mPolyphaseChannelOutputProcessor.start();
                    }
                    catch(FilterDesignException fde)
                    {
                        errorMessage = "Cannot create new output processor - unable to design synthesis filter for [" +
                                indexes.size() + "] channel indices";
                        mLog.error("Error creating a synthesis filter for a new channel output processor");
                    }
                    break;
                default:
                    mLog.error("Request to create an output processor for unexpected channel index size:" + indexes.size());
                    mLog.info(channelCalculator.toString());
                    errorMessage = "Unable to create new channel output processor - unexpected channel index size: " +
                            indexes.size();
            }
        }

        //Unlikely, but if we had an error designing a synthesis filter, throw an exception
        if(errorMessage != null)
        {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Primary method for receiving channel results output from a polyphase channelizer.  The results buffer will be
     * queued for processing to extract the target channel samples, process them for frequency correction and/or
     * channel aggregation, and dispatch the results to the downstream sample listener/consumer.
     *
     * @param channelResultsList containing a list of polyphase channelizer output arrays.
     * @param currentSamplesTimestamp for the samples
     */
    public void receiveChannelResults(List<float[]> channelResultsList, long currentSamplesTimestamp)
    {
        if(mPendingOutputProcessorUpdate != null)
        {
            ChannelCalculator channelCalculator = mPendingOutputProcessorUpdate.getChannelCalculator();
            SynthesisFilterManager filterManager = mPendingOutputProcessorUpdate.getSynthesisFilterManager();
            mPendingOutputProcessorUpdate = null;
            doUpdateOutputProcessor(channelCalculator, filterManager);
        }

        try
        {
            if(mPolyphaseChannelOutputProcessor != null)
            {
                mPolyphaseChannelOutputProcessor.receiveChannelResults(channelResultsList, currentSamplesTimestamp);
            }
        }
        catch(Exception e)
        {
            mLog.error("Error processing channel results", e);
        }
    }

    /**
     * Downstream channel sample rate
     *
     * @return sample rate in Hertz
     */
    @Override
    public double getSampleRate()
    {
        return mChannelSampleRate;
    }

    /**
     * Sets the downstream channel sample rate
     * @param sampleRate in hertz
     */
    @Override
    protected void setSampleRate(double sampleRate)
    {
        mChannelSampleRate = sampleRate;
    }

    @Override
    public void dispose()
    {
        if(mPolyphaseChannelOutputProcessor != null)
        {
            mPolyphaseChannelOutputProcessor.setListener(null);
            mPolyphaseChannelOutputProcessor.dispose();
        }
    }

    /**
     * Sets the center frequency for this channel.
     * @param frequency in hertz
     */
    @Override
    public void setFrequency(long frequency)
    {
        mIndexCenterFrequency = frequency;
    }

    /**
     * Center frequency from the incoming index channel(s).
     * @return frequency in hertz
     */
    public long getIndexCenterFrequency()
    {
        return mIndexCenterFrequency;
    }

    /**
     * Calculates the frequency offset required to mix the incoming signal to center the desired frequency
     * within the channel
     */
    public long getFrequencyOffset()
    {
        return mIndexCenterFrequency - getTunerChannel().getFrequency();
    }

    @Override
    public String toString()
    {
        return "POLYPHASE [" + mPolyphaseChannelOutputProcessor.getInputChannelCount() + "] " +
            getTunerChannel().getFrequency();
    }

    /**
     * Support classes needed to update the output processor.
     */
    public class PendingOutputProcessorUpdate
    {
        private final ChannelCalculator mChannelCalculator;
        private final SynthesisFilterManager mSynthesisFilterManager;

        /**
         * Constructs an instance
         * @param channelCalculator for support
         * @param synthesisFilterManager for support
         */
        public PendingOutputProcessorUpdate(ChannelCalculator channelCalculator, SynthesisFilterManager synthesisFilterManager)
        {
            mChannelCalculator = channelCalculator;
            mSynthesisFilterManager = synthesisFilterManager;
        }

        /**
         * Channel calculator
         */
        public ChannelCalculator getChannelCalculator()
        {
            return mChannelCalculator;
        }

        /**
         * Channel calculator
         */
        public SynthesisFilterManager getSynthesisFilterManager()
        {
            return mSynthesisFilterManager;
        }
    }
}
