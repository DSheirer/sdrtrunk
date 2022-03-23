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
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.StreamProcessorWithHeartbeat;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;

import java.util.List;

/**
 * Polyphase Channelizer's Tuner Channel Source implementation.  Wraps a ChannelOutputProcessor instance and
 * provides tuner channel source functionality.  Supports dynamic swapout of the underlying channel output processor
 * as the upstream polyphase channelizer reconfigures during tuned center frequency changes.
 */
public class PolyphaseChannelSource extends TunerChannelSource implements Listener<ComplexSamples>
{
    private IPolyphaseChannelOutputProcessor mPolyphaseChannelOutputProcessor;
    private IPolyphaseChannelOutputProcessor mReplacementPolyphaseChannelOutputProcessor;
    private StreamProcessorWithHeartbeat<ComplexSamples> mStreamHeartbeatProcessor;
    private long mReplacementFrequency;
    private double mChannelSampleRate;
    private long mIndexCenterFrequency;
    private long mChannelFrequencyCorrection;

    /**
     * Constructs an instance
     *
     * @param tunerChannel describing the desired channel frequency and bandwidth/minimum sample rate
     * @param outputProcessor - to process polyphase channelizer channel results into a channel stream
     * @param producerSourceEventListener to receive source event requests (e.g. start/stop sample stream)
     * @param channelSampleRate for the downstream sample output
     * @param centerFrequency of the incoming polyphase channel(s)
     * @throws FilterDesignException if a channel low pass filter can't be designed to the channel specification
     */
    public PolyphaseChannelSource(TunerChannel tunerChannel, IPolyphaseChannelOutputProcessor outputProcessor,
                                  Listener<SourceEvent> producerSourceEventListener, double channelSampleRate,
                                  long centerFrequency) throws FilterDesignException
    {
        super(producerSourceEventListener, tunerChannel);
        mPolyphaseChannelOutputProcessor = outputProcessor;
        mPolyphaseChannelOutputProcessor.setListener(this);
        mChannelSampleRate = channelSampleRate;
        mStreamHeartbeatProcessor = new StreamProcessorWithHeartbeat<>(getHeartbeatManager(), HEARTBEAT_INTERVAL_MS);
        setFrequency(centerFrequency);
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
     * Channel output processor used by this channel source to convert polyphase channel results into a specific
     * channel complex buffer output stream.
     */
    public IPolyphaseChannelOutputProcessor getPolyphaseChannelOutputProcessor()
    {
        return mPolyphaseChannelOutputProcessor;
    }

    /**
     * Sets/updates the output processor for this channel source, replacing the existing output processor.
     *
     * @param outputProcessor to use
     * @param frequency center for the channels processed by the output processor
     */
    public void setPolyphaseChannelOutputProcessor(IPolyphaseChannelOutputProcessor outputProcessor, long frequency)
    {
        //If this is the first time, simply assign the output processor
        if(mPolyphaseChannelOutputProcessor == null)
        {
            mPolyphaseChannelOutputProcessor = outputProcessor;
            mPolyphaseChannelOutputProcessor.setListener(this);
        }
        //Otherwise, we have to swap out the processor on the sample processing thread
        else
        {
            mReplacementPolyphaseChannelOutputProcessor = outputProcessor;
            mReplacementFrequency = frequency;
        }
    }

    /**
     * Updates the output processor to use the new output processor provided by the external
     * polyphase channel manager.  This method should only be executed on the sample processing
     * thread, within the processChannelResults() method.
     */
    private void swapOutputProcessor()
    {
        if(mReplacementPolyphaseChannelOutputProcessor != null)
        {
            IPolyphaseChannelOutputProcessor existingProcessor = mPolyphaseChannelOutputProcessor;
            existingProcessor.stop();
            existingProcessor.setListener(null);

            //Swap out the processor so that incoming samples can accumulate in the new channel output processor
            mPolyphaseChannelOutputProcessor = mReplacementPolyphaseChannelOutputProcessor;
            mReplacementPolyphaseChannelOutputProcessor = null;
            mPolyphaseChannelOutputProcessor.setListener(this);
            mPolyphaseChannelOutputProcessor.start();

            //Finally, setup the frequency offset for the output processor.
            mIndexCenterFrequency = mReplacementFrequency;
            mPolyphaseChannelOutputProcessor.setFrequencyOffset(getFrequencyOffset());
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
        if(mReplacementPolyphaseChannelOutputProcessor != null)
        {
            swapOutputProcessor();
        }

        mPolyphaseChannelOutputProcessor.receiveChannelResults(channelResultsList);
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
//TODO: push this frequency correction down to the output processor ...
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
        mPolyphaseChannelOutputProcessor.setFrequencyOffset(getFrequencyOffset());
    }

    @Override
    public String toString()
    {
        return "POLYPHASE [" + mPolyphaseChannelOutputProcessor.getInputChannelCount() + "] " +
            getTunerChannel().getFrequency();
    }
}
