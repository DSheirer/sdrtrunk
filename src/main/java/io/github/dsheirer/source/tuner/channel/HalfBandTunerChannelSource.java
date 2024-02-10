/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.mixer.ComplexMixer;
import io.github.dsheirer.dsp.mixer.ComplexMixerFactory;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.util.Dispatcher;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Complex sample source that provides a frequency-translated and decimated sample buffer stream.  Uses a
 * CIC decimation filter that requires the decimation rate to be an integer multiple.  Sample buffer processing
 * occurs on a scheduled runnable thread.
 */
public class HalfBandTunerChannelSource<T extends INativeBuffer> extends TunerChannelSource implements Listener<T>
{
    private static final Logger mLog = LoggerFactory.getLogger(HalfBandTunerChannelSource.class);

    //Maximum number of filled buffers for the blocking queue
    private static final int BUFFER_MAX_CAPACITY = 600;

    private Dispatcher<T> mBufferDispatcher;
    private ComplexMixer mFrequencyCorrectionMixer;
    private IRealDecimationFilter mIDecimationFilter;
    private IRealDecimationFilter mQDecimationFilter;
    private Listener<ComplexSamples> mSamplesListener;
    private double mChannelSampleRate;
    private long mTunerFrequency;

    /**
     * Constructs a frequency translating and CIC decimating channel source.
     *
     * @param producerSourceEventListener to receive sample stream start/stop requests
     * @param tunerChannel that details the desired channel frequency and bandwidth
     * @param sampleRate of the incoming sample stream
     * @param channelSpecification for the requested channel.
     * @param threadName for the dispatcher
     * @throws FilterDesignException if a final cleanup filter cannot be designed using the remez filter
     *                               designer and the filter parameters.
     */
    public HalfBandTunerChannelSource(Listener<SourceEvent> producerSourceEventListener, TunerChannel tunerChannel,
                                      double sampleRate, ChannelSpecification channelSpecification, String threadName)
                                            throws FilterDesignException
    {
        super(producerSourceEventListener, tunerChannel, threadName);

        int desiredDecimation = (int)(sampleRate / channelSpecification.getMinimumSampleRate());
        int decimation = DecimationFilterFactory.getDecimationRate(desiredDecimation);

        mIDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimation);
        mQDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimation);

        //Set dispatcher to process 1/10 of estimated sample arrival rate, 20 times per second (up to 200% per interval)
        mBufferDispatcher = new Dispatcher(threadName, 50, getHeartbeatManager());
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
     * Calculates the local mixer frequency offset from the tuned frequency,
     * channel's requested frequency, and channel frequency correction.
     */
    private void updateMixerFrequencyOffset()
    {
        long offset = mTunerFrequency - getTunerChannel().getFrequency();
        mFrequencyCorrectionMixer.setFrequency(offset);
    }

    /**
     * Mixer frequency applying offset to the samples.
     * @return mixer frequency
     */
    public long getMixerFrequency()
    {
        return (long)mFrequencyCorrectionMixer.getFrequency();
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
     * Sets the listener to receive the complex buffer sample output from this channel
     *
     * @param listener to receive complex buffers
     */
    @Override
    public void setListener(Listener<ComplexSamples> listener)
    {
        mSamplesListener = listener;
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
            if(mSamplesListener != null)
            {
                Iterator<ComplexSamples> iterator = nativeBuffer.iterator();

                while(iterator.hasNext())
                {
                    ComplexSamples basebanded = mFrequencyCorrectionMixer.mix(iterator.next());
                    float[] i = mIDecimationFilter.decimateReal(basebanded.i());
                    float[] q = mQDecimationFilter.decimateReal(basebanded.q());

                    try
                    {
                        mSamplesListener.receive(new ComplexSamples(i, q, basebanded.timestamp()));
                    }
                    catch(Throwable t)
                    {
                        //The listener can be made null and cause the error - only log if we have a non-null listener
                        if(mSamplesListener != null)
                        {
                            mLog.error("Error dispatching complex samples to listener [" + mSamplesListener + "]");
                        }
                    }
                }
            }
        }
    }
}
