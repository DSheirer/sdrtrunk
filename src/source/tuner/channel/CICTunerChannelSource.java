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
package source.tuner.channel;

import dsp.filter.FilterFactory;
import dsp.filter.Window;
import dsp.filter.cic.ComplexPrimeCICDecimate;
import dsp.mixer.Oscillator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Buffer;
import sample.Listener;
import sample.OverflowableTransferQueue;
import sample.complex.Complex;
import sample.complex.ComplexBuffer;
import source.SourceEvent;
import source.tuner.Tuner;

import java.util.ArrayList;
import java.util.List;

public class CICTunerChannelSource extends TunerChannelSource
{
    private final static Logger mLog = LoggerFactory.getLogger(CICTunerChannelSource.class);

    //Maximum number of filled buffers for the blocking queue
    private static final int BUFFER_MAX_CAPACITY = 300;

    //Threshold for resetting buffer overflow condition
    private static final int BUFFER_OVERFLOW_RESET_THRESHOLD = 100;

    private static double CHANNEL_RATE = 48000.0;
    private static int CHANNEL_PASS_FREQUENCY = 12000;

    private OverflowableTransferQueue<ComplexBuffer> mBuffer;

    private Oscillator mFrequencyCorrectionMixer;
    private ComplexPrimeCICDecimate mDecimationFilter;
    private Listener<ComplexBuffer> mListener;
    private List<ComplexBuffer> mSampleBuffers = new ArrayList<ComplexBuffer>();
    private long mTunerFrequency = 0;
    private double mTunerSampleRate;
    private long mChannelFrequencyCorrection = 0;

    /**
     * Implements a heterodyne/decimate Digital Drop Channel (DDC) to decimate the IQ output from a tuner down to a
     * fixed 48 kHz IQ channel rate.
     *
     * Note: this class can only be used once (started and stopped) and a new tuner channel source must be requested
     * from the tuner once this object has been stopped.  This is because channels are managed dynamically and center
     * tuned frequency may have changed since this source was obtained and thus the tuner might no longer be able to
     * source this channel once it has been stopped.
     *
     * @param tunerChannel specifying the center frequency for the DDC
     */
    public CICTunerChannelSource(Tuner tuner, TunerChannel tunerChannel)
    {
        super(tuner.getTunerController().getSourceEventListener(), tunerChannel);

        mBuffer = new OverflowableTransferQueue<>(BUFFER_MAX_CAPACITY, BUFFER_OVERFLOW_RESET_THRESHOLD);
        mBuffer.setSourceOverflowListener(this);

        //Setup the frequency translator to the current source frequency
        mTunerFrequency = tuner.getTunerController().getFrequency();
        long frequencyOffset = mTunerFrequency - getTunerChannel().getFrequency();
        mFrequencyCorrectionMixer = new Oscillator(frequencyOffset, tuner.getTunerController().getSampleRate());

        //Setup the decimation filter chain
        setSampleRate(tuner.getTunerController().getSampleRate());
    }

    /**
     * Sets the center frequency for the incoming sample buffers
     * @param frequency in hertz
     */
    @Override
    protected void setFrequency(long frequency)
    {
        mTunerFrequency = frequency;

        //Reset frequency correction so that consumer components can adjust
        setFrequencyCorrection(0);

        updateMixerFrequencyOffset();
    }

    /**
     * Current frequency correction being applied to the channel sample stream
     * @return correction in hertz
     */
    @Override
    public long getFrequencyCorrection()
    {
        return mChannelFrequencyCorrection;
    }

    /**
     * Changes the frequency correction value and broadcasts the change to the registered downstream listener.
     * @param correction current frequency correction value.
     */
    protected void setFrequencyCorrection(long correction)
    {
        mChannelFrequencyCorrection = correction;

        updateMixerFrequencyOffset();

        broadcastConsumerSourceEvent(SourceEvent.frequencyCorrectionChange(mChannelFrequencyCorrection));
    }

    /**
     * Primary input method for receiving complex sample buffers from the wideband source (ie tuner)
     * @param buffer to receive and eventually process
     */
    public void receive(ComplexBuffer buffer)
    {
        mBuffer.offer(buffer);
    }

    @Override
    public void setListener(Listener<ComplexBuffer> listener)
    {
		/* Save a pointer to the listener so that if we have to change the
		 * decimation filter, we can re-add the listener */
        mListener = listener;

        mDecimationFilter.setListener(listener);
    }

    @Override
    public void removeListener(Listener<ComplexBuffer> listener)
    {
        mDecimationFilter.removeListener();
    }

    /**
     * Updates the sample rate to the requested value and notifies any downstream components of the change
     * @param sampleRate to set
     */
    protected void setSampleRate(double sampleRate)
    {
        if(mTunerSampleRate != sampleRate)
        {
            mFrequencyCorrectionMixer.setSampleRate(sampleRate);

            /* Get new decimation filter */
            mDecimationFilter = FilterFactory.getDecimationFilter((int)sampleRate, (int)CHANNEL_RATE, 1,
                CHANNEL_PASS_FREQUENCY, 60, Window.WindowType.HAMMING);

            /* re-add the original output listener */
            mDecimationFilter.setListener(mListener);

            mTunerSampleRate = sampleRate;

            broadcastConsumerSourceEvent(SourceEvent.channelSampleRateChange(getSampleRate()));
        }
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

    public double getSampleRate()
    {
        return CHANNEL_RATE;
    }

    @Override
    protected void processSamples()
    {
        mBuffer.drainTo(mSampleBuffers, 20);

        for(Buffer buffer : mSampleBuffers)
        {
            float[] samples = buffer.getSamples();

            /* We make a copy of the buffer so that we don't affect
             * anyone else that is using the same buffer, like other
             * channels or the spectral display */
            float[] translated = new float[samples.length];

            /* Perform frequency translation */
            for(int x = 0; x < samples.length; x += 2)
            {
                mFrequencyCorrectionMixer.rotate();

                translated[x] = Complex.multiplyInphase(
                    samples[x], samples[x + 1], mFrequencyCorrectionMixer.inphase(), mFrequencyCorrectionMixer.quadrature());

                translated[x + 1] = Complex.multiplyQuadrature(
                    samples[x], samples[x + 1], mFrequencyCorrectionMixer.inphase(), mFrequencyCorrectionMixer.quadrature());
            }

            final ComplexPrimeCICDecimate filter = mDecimationFilter;

            if(filter != null)
            {
                filter.receive(new ComplexBuffer(translated));
            }
        }

        mSampleBuffers.clear();
    }
}
