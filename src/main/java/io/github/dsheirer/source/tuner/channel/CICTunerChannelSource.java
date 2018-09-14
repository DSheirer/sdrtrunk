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
package io.github.dsheirer.source.tuner.channel;

import io.github.dsheirer.dsp.filter.cic.ComplexPrimeCICDecimate;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.mixer.IOscillator;
import io.github.dsheirer.dsp.mixer.LowPhaseNoiseOscillator;
import io.github.dsheirer.sample.IOverflowListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.OverflowableReusableBufferTransferQueue;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.source.SourceEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Complex sample source that provides a frequency-translated and decimated sample buffer stream.  Uses a
 * CIC decimation filter that requires the decimation rate to be an integer multiple.  Sample buffer processing
 * occurs on a scheduled runnable thread.
 */
public class CICTunerChannelSource extends TunerChannelSource implements Listener<ReusableComplexBuffer>
{
//    private final static Logger mLog = LoggerFactory.getLogger(CICTunerChannelSource.class);

    //Maximum number of filled buffers for the blocking queue
    private static final int BUFFER_MAX_CAPACITY = 300;

    //Threshold for resetting buffer overflow condition
    private static final int BUFFER_OVERFLOW_RESET_THRESHOLD = 100;

    private OverflowableReusableBufferTransferQueue<ReusableComplexBuffer> mBuffer;
    private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("CICTunerChannelSource");
    private IOscillator mFrequencyCorrectionMixer;
    private ComplexPrimeCICDecimate mDecimationFilter;
    private List<ReusableComplexBuffer> mSampleBuffers = new ArrayList<>();
    private double mChannelSampleRate;
    private long mChannelFrequencyCorrection = 0;
    private long mTunerFrequency;

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

        mDecimationFilter = new ComplexPrimeCICDecimate(sampleRate, decimation, channelSpecification.getPassFrequency(),
            channelSpecification.getStopFrequency());

        mBuffer = new OverflowableReusableBufferTransferQueue<>(BUFFER_MAX_CAPACITY, BUFFER_OVERFLOW_RESET_THRESHOLD);

        //Setup the frequency mixer to the current source frequency
        mChannelSampleRate = sampleRate / (double)decimation;
        mTunerFrequency = tunerChannel.getFrequency();
        long frequencyOffset = mTunerFrequency - getTunerChannel().getFrequency();

        mFrequencyCorrectionMixer = new LowPhaseNoiseOscillator(frequencyOffset, sampleRate);
    }

    /**
     * Registers an overflow listener to be notified in the internal buffer is in an
     * overflow state.
     *
     * @param listener to receiver updates to the source overflow state
     */
    @Override
    public void setOverflowListener(IOverflowListener listener)
    {
        mBuffer.setOverflowListener(listener);
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
    public void receive(ReusableComplexBuffer buffer)
    {
        mBuffer.offer(buffer);
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
     * @param complexBufferListener to receive complex buffers
     */
    @Override
    public void setListener(Listener<ReusableComplexBuffer> complexBufferListener)
    {
        mDecimationFilter.setListener(complexBufferListener);
    }

    @Override
    public void removeListener(Listener<ReusableComplexBuffer> listener)
    {
        mDecimationFilter.removeListener();
    }


    @Override
    public double getSampleRate()
    {
        return mChannelSampleRate;
    }

    /**
     * Primary processing method that is invoked on a recurring basis to process any queued complex buffers.
     *
     * Mixes the target frequency to baseband and then passes the buffer to the CIC decimation filter
     */
    protected void processSamples()
    {
        mBuffer.drainTo(mSampleBuffers);

        for(ReusableComplexBuffer complexBuffer : mSampleBuffers)
        {
            float[] samples = complexBuffer.getSamples();

            ReusableComplexBuffer translatedComplexBuffer = mReusableComplexBufferQueue.getBuffer(samples.length);
            float[] translatedSamples = translatedComplexBuffer.getSamples();

            /* Perform frequency translation */
            for(int x = 0; x < samples.length; x += 2)
            {
                mFrequencyCorrectionMixer.rotate();

                translatedSamples[x] = Complex.multiplyInphase(samples[x], samples[x + 1],
                    mFrequencyCorrectionMixer.inphase(), mFrequencyCorrectionMixer.quadrature());

                translatedSamples[x + 1] = Complex.multiplyQuadrature(samples[x], samples[x + 1],
                    mFrequencyCorrectionMixer.inphase(), mFrequencyCorrectionMixer.quadrature());
            }

            mDecimationFilter.receive(translatedComplexBuffer);
            complexBuffer.decrementUserCount();
        }

        mSampleBuffers.clear();
    }
}
