/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
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
    private long mChannelFrequencyCorrection;
    private long mCurrentSamplesTimestamp;
    private ReentrantLock mOutputProcessorLock = new ReentrantLock();

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
        updateOutputProcessor(channelCalculator, filterManager);
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
        mCurrentSamplesTimestamp = complexSamples.timestamp();
    }

    /**
     * Updates the output processor whenever the source tuner's center frequency changes.
     * @param channelCalculator providing access to updated tuner center frequency, sample rate, etc.
     * @param filterManager for designing and caching synthesis filters
     */
    public void updateOutputProcessor(ChannelCalculator channelCalculator, SynthesisFilterManager filterManager) throws IllegalArgumentException
    {
        String errorMessage = null;

        mOutputProcessorLock.lock();

        try
        {
            //If a change in sample rate or center frequency makes this channel no longer viable, then the channel
            //calculator will throw an IllegalArgException ... handled below
            List<Integer> indexes = channelCalculator.getChannelIndexes(getTunerChannel());

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
                        float[] filter = filterManager.getFilter(channelCalculator.getSampleRate(),
                                channelCalculator.getChannelBandwidth(), indexes.size());
                        mPolyphaseChannelOutputProcessor.setSynthesisFilter(filter);
                    }
                    catch(FilterDesignException fde)
                    {
                        mLog.error("Error creating an updated synthesis filter for the channel output processor");
                        errorMessage ="Cannot update output processor - unable to design synthesis filter for [" +
                                indexes.size() + "] channel indices";
                    }
                }

                mPolyphaseChannelOutputProcessor.setFrequencyOffset(getFrequencyOffset());
            }
            else //Create a new output processor.
            {
                if(mPolyphaseChannelOutputProcessor != null)
                {
                    mPolyphaseChannelOutputProcessor.setListener(null);
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
        }
        finally
        {
            mOutputProcessorLock.unlock();
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
     */
    public void receiveChannelResults(List<float[]> channelResultsList)
    {
        mOutputProcessorLock.lock();

        try
        {
            if(mPolyphaseChannelOutputProcessor != null)
            {
                mPolyphaseChannelOutputProcessor.receiveChannelResults(channelResultsList, mCurrentSamplesTimestamp);
            }
        }
        finally
        {
            mOutputProcessorLock.unlock();
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

    @Override
    public long getChannelFrequencyCorrection()
    {
        return mChannelFrequencyCorrection;
    }

    /**
     * Adjusts the frequency correction value that is being applied to the channelized output stream by the
     * polyphase channel output processor.
     *
     * @param value to apply for frequency correction in hertz
     */
    protected void setChannelFrequencyCorrection(long value)
    {
        mChannelFrequencyCorrection = value;
        updateFrequencyOffset();
        broadcastConsumerSourceEvent(SourceEvent.frequencyCorrectionChange(mChannelFrequencyCorrection));
    }

    /**
     * Sets the center frequency for this channel.frequency for the incoming sample stream channel results and resets frequency correction to zero.
     * @param frequency in hertz
     */
    @Override
    public void setFrequency(long frequency)
    {
        mIndexCenterFrequency = frequency;

        //Set frequency correction to zero to trigger an update to the mixer and allow downstream monitors to
        //recalculate the frequency error correction again
        setChannelFrequencyCorrection(0);
    }

    /**
     * Calculates the frequency offset required to mix the incoming signal to center the desired frequency
     * within the channel
     */
    private long getFrequencyOffset()
    {
        return mIndexCenterFrequency - getTunerChannel().getFrequency() + mChannelFrequencyCorrection;
    }

    /**
     * Updates the frequency offset being applied by the output processor.  Calculates the offset using the center
     * frequency of the incoming polyphase channel results and the desired output channel frequency adjusted by any
     * requested frequency correction value.
     */
    private void updateFrequencyOffset()
    {
        if(mPolyphaseChannelOutputProcessor != null)
        {
            mPolyphaseChannelOutputProcessor.setFrequencyOffset(getFrequencyOffset());
        }
    }

    @Override
    public String toString()
    {
        return "POLYPHASE [" + mPolyphaseChannelOutputProcessor.getInputChannelCount() + "] " +
            getTunerChannel().getFrequency();
    }
}
