/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
import io.github.dsheirer.source.tuner.frequency.TunerFrequencyErrorManager;
import io.github.dsheirer.util.Dispatcher;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
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

    //Decimation rate where a 2048 sample fragment produces too few samples for the consumer, so the decimated samples
    //are assembled into longer buffers.  Decoders apply additional decimation that requires a buffer length that's a
    //multiple of their decimation.
    private static final int ASSEMBLED_OUTPUT_DECIMATION = 1024;
    private static final int ASSEMBLED_OUTPUT_LENGTH = 64;

    //Decimation rate where sample fragments can be too short for the filter, which requires a buffer length that's a
    //multiple of the decimation rate, so the mixed samples are assembled before decimation.
    private static final int ASSEMBLED_INPUT_DECIMATION = 2048;

    private final Dispatcher<T> mBufferDispatcher;
    private final ComplexMixer mFrequencyCorrectionMixer;
    private final IRealDecimationFilter mIDecimationFilter;
    private final IRealDecimationFilter mQDecimationFilter;
    private final boolean mReuseMixerOutput;
    private float[] mIMixerOutput;
    private float[] mQMixerOutput;
    private Listener<ComplexSamples> mSamplesListener;
    private final double mChannelSampleRate;
    private volatile long mChannelFrequency;
    private final long mFrequencyOffset;
    private volatile long mFrequencyCorrection;
    private final AtomicBoolean mPendingMixerUpdate = new AtomicBoolean();
    private final boolean mAssembleOutput;
    private final float[] mInputI;
    private final float[] mInputQ;
    private int mInputPointer;
    private long mInputTimestamp;
    private float[] mOutputI;
    private float[] mOutputQ;
    private int mOutputPointer;
    private long mOutputTimestamp;

    /**
     * Constructs a frequency translating and CIC decimating channel source.
     *
     * @param producerSourceEventListener to receive sample stream start/stop requests
     * @param tunerChannel that details the desired channel frequency and bandwidth
     * @param sampleRate of the incoming sample stream
     * @param channelSpecification for the requested channel.
     * @param threadName for the dispatcher
     * @param tunerFrequencyErrorManager to connect with the channel error manager
     * @throws FilterDesignException if a final cleanup filter cannot be designed using the remez filter
     *                               designer and the filter parameters.
     */
    public HalfBandTunerChannelSource(Listener<SourceEvent> producerSourceEventListener, TunerChannel tunerChannel,
                                      double sampleRate, ChannelSpecification channelSpecification, String threadName,
                                      TunerFrequencyErrorManager tunerFrequencyErrorManager)
                                            throws FilterDesignException
    {
        super(producerSourceEventListener, tunerChannel, threadName, tunerFrequencyErrorManager);

        int desiredDecimation = (int)(sampleRate / channelSpecification.getMinimumSampleRate());
        int decimation = DecimationFilterFactory.getDecimationRate(desiredDecimation);

        mIDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimation);
        mQDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimation);

        //The decimation filters copy the mixed samples, unless there's no decimation and the mixed samples are the output
        mReuseMixerOutput = decimation > 1;
        mAssembleOutput = decimation >= ASSEMBLED_OUTPUT_DECIMATION;
        mInputI = decimation >= ASSEMBLED_INPUT_DECIMATION ? new float[decimation] : null;
        mInputQ = decimation >= ASSEMBLED_INPUT_DECIMATION ? new float[decimation] : null;

        //Set dispatcher to process 1/10 of estimated sample arrival rate, 20 times per second (up to 200% per interval)
        mBufferDispatcher = new Dispatcher(threadName, 50, getHeartbeatManager(), BUFFER_MAX_CAPACITY);
        mBufferDispatcher.setListener(new NativeBufferProcessor());

        //Setup the frequency mixer to the current source frequency
        mChannelSampleRate = sampleRate / (double)decimation;
        mChannelFrequency = tunerChannel.getFrequency();
        mFrequencyOffset = mChannelFrequency - getTunerChannel().getFrequency();
        mFrequencyCorrectionMixer = ComplexMixerFactory.getMixer(mFrequencyOffset, sampleRate);
    }

    /**
     * Sets the channel decoder requested frequency correction, which is applied on the sample processing thread.
     * @param correction requested by the downstream processors.
     */
    @Override
    public void setFrequencyCorrection(long correction)
    {
        mFrequencyCorrection = correction;
        mPendingMixerUpdate.set(true);
    }

    @Override
    public void start()
    {
        //The dispatcher has to be running to accept delayed sample buffers preloaded in response to the start request
        mBufferDispatcher.start();
        super.start();
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
     * Sets/updates the center frequency for the sample streaming being sent from the producer, which is applied on
     * the sample processing thread.
     *
     * @param frequency in hertz
     */
    @Override
    public void setFrequency(long frequency)
    {
        mChannelFrequency = frequency;
        mPendingMixerUpdate.set(true);
    }

    /**
     * Calculates the local mixer frequency offset from the tuned frequency,
     * channel's requested frequency, and channel frequency correction.  This is only invoked on the sample processing
     * thread so that the latest values from the tuner and channel frequency correction threads are applied together
     * and the mixer is never updated while it is generating samples.
     */
    private void updateMixerFrequencyOffset()
    {
        long offset = mChannelFrequency - getTunerChannel().getFrequency() + mFrequencyCorrection;
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
     * Mixes the samples to baseband, reusing the mixer output arrays when they aren't passed to the consumer.
     * @param samples to mix, which aren't modified since they can be shared with other channels.
     * @return mixed samples
     */
    private ComplexSamples mix(ComplexSamples samples)
    {
        if(!mReuseMixerOutput)
        {
            return mFrequencyCorrectionMixer.mix(samples);
        }

        if(mIMixerOutput == null || mIMixerOutput.length != samples.i().length)
        {
            mIMixerOutput = new float[samples.i().length];
            mQMixerOutput = new float[samples.q().length];
        }

        return mFrequencyCorrectionMixer.mix(samples, mIMixerOutput, mQMixerOutput);
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
                    if(mPendingMixerUpdate.compareAndSet(true, false))
                    {
                        updateMixerFrequencyOffset();
                    }

                    ComplexSamples basebanded = mix(iterator.next());

                    if(mInputI != null)
                    {
                        assembleInput(basebanded);
                    }
                    else
                    {
                        decimate(basebanded.i(), basebanded.q(), basebanded.timestamp());
                    }
                }
            }
        }

        /**
         * Assembles the mixed samples into buffers of the decimation rate length and decimates each full buffer.
         * @param samples that were mixed, which are copied since the mixer output arrays can be reused.
         */
        private void assembleInput(ComplexSamples samples)
        {
            int pointer = 0;

            while(pointer < samples.i().length)
            {
                if(mInputPointer == 0)
                {
                    mInputTimestamp = samples.timestamp();
                }

                int length = Math.min(samples.i().length - pointer, mInputI.length - mInputPointer);
                System.arraycopy(samples.i(), pointer, mInputI, mInputPointer, length);
                System.arraycopy(samples.q(), pointer, mInputQ, mInputPointer, length);
                pointer += length;
                mInputPointer += length;

                if(mInputPointer == mInputI.length)
                {
                    mInputPointer = 0;
                    decimate(mInputI, mInputQ, mInputTimestamp);
                }
            }
        }

        /**
         * Decimates the samples and sends them to the listener.
         */
        private void decimate(float[] i, float[] q, long timestamp)
        {
            output(mIDecimationFilter.decimateReal(i), mQDecimationFilter.decimateReal(q), timestamp);
        }

        /**
         * Sends the decimated samples to the listener, assembling them into larger buffers at the maximum decimation.
         * @param i decimated samples
         * @param q decimated samples
         * @param timestamp of the first sample
         */
        private void output(float[] i, float[] q, long timestamp)
        {
            if(!mAssembleOutput)
            {
                dispatch(new ComplexSamples(i, q, timestamp));
                return;
            }

            int pointer = 0;

            while(pointer < i.length)
            {
                if(mOutputPointer == 0)
                {
                    //New arrays each time since the assembled arrays are passed to the consumer
                    mOutputI = new float[ASSEMBLED_OUTPUT_LENGTH];
                    mOutputQ = new float[ASSEMBLED_OUTPUT_LENGTH];
                    mOutputTimestamp = timestamp;
                }

                int length = Math.min(i.length - pointer, ASSEMBLED_OUTPUT_LENGTH - mOutputPointer);
                System.arraycopy(i, pointer, mOutputI, mOutputPointer, length);
                System.arraycopy(q, pointer, mOutputQ, mOutputPointer, length);
                pointer += length;
                mOutputPointer += length;

                if(mOutputPointer == ASSEMBLED_OUTPUT_LENGTH)
                {
                    mOutputPointer = 0;
                    dispatch(new ComplexSamples(mOutputI, mOutputQ, mOutputTimestamp));
                }
            }
        }

        /**
         * Sends the samples to the listener.
         */
        private void dispatch(ComplexSamples samples)
        {
            try
            {
                mSamplesListener.receive(samples);
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
