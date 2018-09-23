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
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter2;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableChannelResultsBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferAssembler;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class PolyphaseChannelSource extends TunerChannelSource
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelSource.class);

    private ReusableComplexBufferAssembler mReusableComplexBufferAssembler;
    private IPolyphaseChannelOutputProcessor mPolyphaseChannelOutputProcessor;
    private IPolyphaseChannelOutputProcessor mReplacementPolyphaseChannelOutputProcessor;
    private long mReplacementFrequency;
    private ComplexFIRFilter2 mLowPassFilter;
    private static Map<Integer,float[]> sLowPassFilters = new HashMap();

    private double mChannelSampleRate;
    private long mIndexCenterFrequency;
    private long mChannelFrequencyCorrection;

    /**
     * Polyphase channelizer tuner channel source implementation.  Adapts the channel array output samples from the
     * polyphase channelizer into a single channel, or a channel synthesized from two adjacent channels that is
     * frequency translated and decimated to a single channel.
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
                                  long centerFrequency, ChannelSpecification channelSpecification)
        throws FilterDesignException
    {
        super(producerSourceEventListener, tunerChannel);
        mPolyphaseChannelOutputProcessor = outputProcessor;
        mChannelSampleRate = channelSampleRate;
        mReusableComplexBufferAssembler = new ReusableComplexBufferAssembler(2500, mChannelSampleRate);

        float[] filterCoefficients = getLowPassFilter(channelSampleRate, channelSpecification.getPassFrequency(),
            channelSpecification.getStopFrequency());
        mLowPassFilter = new ComplexFIRFilter2(filterCoefficients, 1.0f);

        setFrequency(centerFrequency);
    }

    /**
     * Registers the listener to receive complex sample buffers from this channel source
     */
    @Override
    public void setListener(final Listener<ReusableComplexBuffer> listener)
    {
        mReusableComplexBufferAssembler.setListener(listener);
   }

    /**
     * Removes the listener from receiving complex sample buffers from this channel source
     */
    @Override
    public void removeListener(Listener<ReusableComplexBuffer> listener)
    {
        mReusableComplexBufferAssembler.setListener(null);
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

            //Swap out the processor so that incoming samples can accumulate in the new channel output processor
            mPolyphaseChannelOutputProcessor = mReplacementPolyphaseChannelOutputProcessor;
            mReplacementPolyphaseChannelOutputProcessor = null;

            //Remove the buffer overflow listener and clear any previous buffer overflow state
            existingProcessor.setSourceOverflowListener(null);
            broadcastOverflowState(false);

            //Register to receive buffer overflow notifications so we can push them up through the source chain
            mPolyphaseChannelOutputProcessor.setSourceOverflowListener(this);

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
     * @param channelResultsBuffer containing an array of polyphase channelizer outputs.
     */
    public void receiveChannelResults(ReusableChannelResultsBuffer channelResultsBuffer)
    {
        if(mReplacementPolyphaseChannelOutputProcessor != null)
        {
            swapOutputProcessor();
        }

        mReusableComplexBufferAssembler.updateTimestamp(channelResultsBuffer.getTimestamp());
        mPolyphaseChannelOutputProcessor.receiveChannelResults(channelResultsBuffer);
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
        if(mReusableComplexBufferAssembler != null)
        {
            mReusableComplexBufferAssembler.dispose();
        }

        if(mPolyphaseChannelOutputProcessor != null)
        {
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

    @Override
    protected void processSamples()
    {
        mPolyphaseChannelOutputProcessor.processChannelResults(mReusableComplexBufferAssembler);
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

    /**
     * Creates a low-pass filter to use as the final cleanup filter for the decimated output stream
     *
     * @param sampleRate for the final output channel rate
     * @param passFrequency for half of the desired channel rate
     * @param stopFrequency for the attenuated band
     * @return a newly designed filter or a previously designed (cached) filter
     * @throws FilterDesignException
     */
    private float[] getLowPassFilter(double sampleRate, double passFrequency, double stopFrequency) throws FilterDesignException
    {
        //Use existing filter if we've already designed one
        if(sLowPassFilters.containsKey((int)sampleRate))
        {
            return sLowPassFilters.get((int)sampleRate);
        }

        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
            .sampleRate(sampleRate)
            .gridDensity(16)
            .passBandCutoff(passFrequency)
            .passBandAmplitude(1.0)
            .passBandRipple(0.01)
            .stopBandStart(stopFrequency)
            .stopBandAmplitude(0.0)
            .stopBandRipple(0.01)
            .build();

        RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

        //This will throw an exception if the filter cannot be designed
        float[] taps = designer.getImpulseResponse();

        sLowPassFilters.put((int)sampleRate, taps);

        return taps;
    }
}
