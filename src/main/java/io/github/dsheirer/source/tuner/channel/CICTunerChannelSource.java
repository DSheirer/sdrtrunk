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
package io.github.dsheirer.source.tuner.channel;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.buffer.NativeBufferPoisonPill;
import io.github.dsheirer.dsp.filter.cic.PrimeCicDecimationFilter;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.mixer.ComplexMixer;
import io.github.dsheirer.dsp.mixer.ComplexMixerFactory;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.ComplexSamplesRepackager;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.Dispatcher;

import java.util.Iterator;
import java.util.List;

/**
 * Complex sample source that provides a frequency-translated and decimated sample buffer stream.  Uses a
 * CIC decimation filter that requires the decimation rate to be an integer multiple.  Sample buffer processing
 * occurs on a scheduled runnable thread.
 */
public class CICTunerChannelSource<T extends INativeBuffer> extends TunerChannelSource implements Listener<T>
{
    //Maximum number of filled buffers for the blocking queue
    private static final int BUFFER_MAX_CAPACITY = 300;

    //Threshold for resetting buffer overflow condition
    private static final int BUFFER_OVERFLOW_RESET_THRESHOLD = 100;

    //Samples repackager is set to 2048 (a power of 2) to align with channel processing chain requirements.
    private final ComplexSamplesRepackager mSamplesRepackager = new ComplexSamplesRepackager(1024);
    private Dispatcher<T> mBufferDispatcher;
    private ComplexMixer mFrequencyCorrectionMixer;
    private PrimeCicDecimationFilter mIDecimationFilter;
    private PrimeCicDecimationFilter mQDecimationFilter;
    private double mChannelSampleRate;
    private long mChannelFrequencyCorrection = 0;
    private long mTunerFrequency;
    private Listener<ComplexSamples> mListener;

    /**
     * Constructs a frequency translating and CIC decimating channel source.
     *
     * @param producerSourceEventListener to receive sample stream start/stop requests
     * @param tunerChannel that details the desired channel frequency and bandwidth
     * @param sampleRate of the incoming sample stream
     * @param channelSpecification for the requested channel.
     * @throws FilterDesignException if a final cleanup filter cannot be designed using the remez filter
     *                               designer and the filter parameters.
     */
    public CICTunerChannelSource(Listener<SourceEvent> producerSourceEventListener, TunerChannel tunerChannel,
                 double sampleRate, ChannelSpecification channelSpecification) throws FilterDesignException
    {
        super(producerSourceEventListener, tunerChannel);

        int decimation = (int)(sampleRate / channelSpecification.getMinimumSampleRate());

        mIDecimationFilter = new PrimeCicDecimationFilter(sampleRate, decimation, channelSpecification.getPassFrequency(),
                channelSpecification.getStopFrequency());
        mQDecimationFilter = new PrimeCicDecimationFilter(sampleRate, decimation, channelSpecification.getPassFrequency(),
                channelSpecification.getStopFrequency());

        mBufferDispatcher = new Dispatcher(BUFFER_MAX_CAPACITY, "sdrtrunk cic channel " +
                tunerChannel.getFrequency(), new NativeBufferPoisonPill());
        mBufferDispatcher.setListener(new NativeBufferProcessor());

        //Setup the frequency mixer to the current source frequency
        mChannelSampleRate = sampleRate / (double)decimation;
        mTunerFrequency = tunerChannel.getFrequency();
        long frequencyOffset = mTunerFrequency - getTunerChannel().getFrequency();

        mFrequencyCorrectionMixer = ComplexMixerFactory.getMixer(frequencyOffset, sampleRate);
    }

    @Override
    public void start()
    {
        super.start();
        mBufferDispatcher.start();
    }

    @Override
    public void stop()
    {
        super.stop();
        mBufferDispatcher.stop();
    }

    @Override
    public void dispose()
    {
        //
    }

    /**
     * Primary interface for receiving incoming complex sample buffers to be frequency translated and decimated.
     */
    @Override
    public void receive(T complexSamples)
    {
        mBufferDispatcher.receive(complexSamples);
    }

    /**
     * Sets/updates the center frequency for the sample streaming being sent from the producer.
     *
     * @param frequency in hertz
     */
    @Override
    public void setFrequency(long frequency)
    {
        mTunerFrequency = frequency;
        updateMixerFrequencyOffset();
    }

    /**
     * Current frequency correction being applied to the channel sample stream
     *
     * @return correction in hertz
     */
    public long getFrequencyCorrection()
    {
        return mChannelFrequencyCorrection;
    }

    /**
     * Changes the frequency correction value and broadcasts the change to the registered downstream listener.
     *
     * @param correction current frequency correction value.
     */
    public void setFrequencyCorrection(long correction)
    {
        mChannelFrequencyCorrection = correction;
        updateMixerFrequencyOffset();
        broadcastConsumerSourceEvent(SourceEvent.frequencyCorrectionChange(mChannelFrequencyCorrection));
    }

    /**
     * Calculates the local mixer frequency offset from the tuned frequency,
     * channel's requested frequency, and channel frequency correction.
     */
    private void updateMixerFrequencyOffset()
    {
        long offset = mTunerFrequency - getTunerChannel().getFrequency() - mChannelFrequencyCorrection;
        mFrequencyCorrectionMixer.setFrequency(offset);
    }

    /**
     * Sets the sample rate of the incoming sample stream from the producer
     *
     * @param sampleRate in hertz
     */
    @Override
    protected void setSampleRate(double sampleRate)
    {
        //Not implemented.  Sample rate changes are not permitted once sample stream starts
    }

    /**
     * Sets the frequency correction for the outbound sample stream to the consumer
     *
     * @param correction in hertz
     */
    @Override
    protected void setChannelFrequencyCorrection(long correction)
    {
        mChannelFrequencyCorrection = correction;
        broadcastConsumerSourceEvent(SourceEvent.channelFrequencyCorrectionChange(correction));
        updateMixerFrequencyOffset();
    }

    /**
     * Frequency correction value currently being applied to the outbound sample stream
     */
    @Override
    public long getChannelFrequencyCorrection()
    {
        return mChannelFrequencyCorrection;
    }

    /**
     * Sets the listener to receive the complex buffer sample output from this channel
     *
     * @param listener to receive complex buffers
     */
    @Override
    public void setListener(Listener<ComplexSamples> listener)
    {
        mListener = listener;
    }

    @Override
    public double getSampleRate()
    {
        return mChannelSampleRate;
    }

    /**
     * Processes native buffers received from the dispatcher and sends to the registered listener
     */
    public class NativeBufferProcessor implements Listener<T>
    {
        @Override
        public void receive(T nativeBuffer)
        {
            getHeartbeatManager().broadcast();

            if(mListener != null)
            {
                Iterator<ComplexSamples> iterator = nativeBuffer.iterator();

                while(iterator.hasNext())
                {
                    ComplexSamples basebanded = mFrequencyCorrectionMixer.mix(iterator.next());
                    float[] i = mIDecimationFilter.decimate(basebanded.i());
                    float[] q = mQDecimationFilter.decimate(basebanded.q());

                    List<ComplexSamples> repackaged = mSamplesRepackager.process(i, q);

                    for(ComplexSamples samples: repackaged)
                    {
                        mListener.receive(samples);
                    }
                }
            }
        }
    }
}
