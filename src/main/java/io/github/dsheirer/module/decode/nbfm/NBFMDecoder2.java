/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.module.decode.nbfm;

import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventProvider;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.dsp.fm.IDemodulator;
import io.github.dsheirer.dsp.squelch.INoiseSquelchController;
import io.github.dsheirer.dsp.squelch.NoiseSquelch;
import io.github.dsheirer.dsp.squelch.NoiseSquelchState;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.PrimaryDecoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.sample.real.IRealBufferProvider;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract analog decoder module with integrated squelch control
 */
public class NBFMDecoder2 extends PrimaryDecoder implements ISourceEventListener, ISourceEventProvider, IComplexSamplesListener, Listener<ComplexSamples>, IRealBufferProvider, IDecoderStateEventProvider, INoiseSquelchController
{
    private final static Logger mLog = LoggerFactory.getLogger(NBFMDecoder2.class);
    private static final double DEMODULATED_AUDIO_SAMPLE_RATE = 8000.0;
    private final IDemodulator mDemodulator = FmDemodulatorFactory.getFmDemodulator();
    private final SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private final NoiseSquelch mNoiseSquelch;
    private IRealFilter mIBasebandFilter;
    private IRealFilter mQBasebandFilter;
    private IRealDecimationFilter mIDecimationFilter;
    private IRealDecimationFilter mQDecimationFilter;
    private RealResampler mResampler;
    private Listener<float[]> mResampledBufferListener;
    private Listener<DecoderStateEvent> mDecoderStateEventListener;
    private final double mChannelBandwidth;

    /**
     * Constructs an instance
     *
     * @param config to setup the NBFM decoder
     */
    public NBFMDecoder2(DecodeConfigNBFM config)
    {
        super(config);

        //Save channel bandwidth to setup channel baseband filter.
        mChannelBandwidth = config.getBandwidth().getValue();
        mNoiseSquelch = new NoiseSquelch(config.getSquelchNoiseOpenThreshold(), config.getSquelchNoiseCloseThreshold(),
                config.getSquelchHysteresisOpenThreshold(), config.getSquelchHysteresisCloseThreshold());

        //Send squelch controlled audio to the resampler and notify the decoder state that the call continues.
        mNoiseSquelch.setAudioListener(audio -> {
            mResampler.resample(audio);
            notifyCallContinuation();
        });

        //Notify the decoder state of call starts and ends
        mNoiseSquelch.setSquelchStateListener(squelchState -> {
            if(squelchState == SquelchState.SQUELCH)
            {
                notifyCallEnd();
            }
            else
            {
                notifyCallStart();
            }
        });
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NBFM;
    }

    @Override
    public void setNoiseSquelchStateListener(Listener<NoiseSquelchState> listener)
    {
        System.out.println("NBFM decoder ... registering listener: " + listener);
        mNoiseSquelch.setNoiseSquelchStateListener(listener);
    }

    @Override
    public void setNoiseThreshold(float open, float close)
    {
        mNoiseSquelch.setNoiseThreshold(open, close);
    }

    @Override
    public void setHysteresisThreshold(int open, int close)
    {
        mNoiseSquelch.setHysteresisThreshold(open, close);
    }

    @Override
    public void setSquelchOverride(boolean override)
    {
        mNoiseSquelch.setSquelchOverride(override);
    }

    /**
     * Implements the ISourceEventListener interface
     */
    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventProcessor;
    }

    /**
     * Module interface methods - unused.
     */
    @Override
    public void reset()
    {
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
    }

    /**
     * Broadcasts the demodulated audio samples to the registered listener.
     *
     * @param demodulatedSamples to broadcast
     */
    protected void broadcast(float[] demodulatedSamples)
    {
        if(mResampledBufferListener != null)
        {
            mResampledBufferListener.receive(demodulatedSamples);
        }
    }

    /**
     * Implements the IRealBufferProvider interface to register a listener for demodulated audio samples.
     *
     * @param listener to receive demodulated audio sample buffers.
     */
    @Override
    public void setBufferListener(Listener<float[]> listener)
    {
        mResampledBufferListener = listener;
    }

    /**
     * Implements the IRealBufferProvider interface to deregister a listener from receiving demodulated audio samples.
     */
    @Override
    public void removeBufferListener()
    {
        mResampledBufferListener = null;
    }

    /**
     * Implements the IComplexSampleListener interface to receive a stream of complex sample buffers.
     */
    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    /**
     * Implements the Listener<ComplexSample> interface to receive a stream of complex sample buffers
     */
    @Override
    public void receive(ComplexSamples samples)
    {
        if(mIDecimationFilter == null || mQDecimationFilter == null)
        {
            throw new IllegalStateException("NBFM demodulator module must receive a sample rate change source " + "event before it can process complex sample buffers");
        }

        float[] decimatedI = mIDecimationFilter.decimateReal(samples.i());
        float[] decimatedQ = mQDecimationFilter.decimateReal(samples.q());

        float[] filteredI = mIBasebandFilter.filter(decimatedI);
        float[] filteredQ = mQBasebandFilter.filter(decimatedQ);

        float[] demodulated = mDemodulator.demodulate(filteredI, filteredQ);

        mNoiseSquelch.process(demodulated);

        //Once we process the samples, if the ending state is squelch closed, update the decoder state that we are idle.
        if(mNoiseSquelch.isSquelched())
        {
            notifyIdle();
        }
    }

    /**
     * Broadcasts a call start state event
     */
    protected void notifyCallStart()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.START, State.CALL, 0));
    }

    /**
     * Broadcasts a call continuation state event
     */
    private void notifyCallContinuation()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION, State.CALL, 0));
    }

    /**
     * Broadcasts a call end state event
     */
    private void notifyCallEnd()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.END, State.CALL, 0));
    }

    /**
     * Broadcasts an idle notification
     */
    private void notifyIdle()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION, State.IDLE, 0));
    }

    /**
     * Broadcasts the decoder state event to an optional registered listener
     */
    private void broadcast(DecoderStateEvent event)
    {
        if(mDecoderStateEventListener != null)
        {
            mDecoderStateEventListener.receive(event);
        }
    }

    /**
     * Sets the decoder state listener
     */
    @Override
    public void setDecoderStateListener(Listener<DecoderStateEvent> listener)
    {
        mDecoderStateEventListener = listener;
    }

    /**
     * Removes the decoder state event listener
     */
    @Override
    public void removeDecoderStateListener()
    {
        mDecoderStateEventListener = null;
    }

    /**
     * Registers the listener to receive squelch change events from the demodulator/squelch controller.
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        //        mDemodulator.setSourceEventListener(listener);
    }

    /**
     * De-registers the listener
     */
    @Override
    public void removeSourceEventListener()
    {
        //        mDemodulator.setSourceEventListener(null);
    }

    /**
     * Monitors sample rate change source event(s) to setup the initial I/Q filter and passes squelch threshold
     * change requests down to the demodulator.
     */
    public class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                //Forward these requests to the sub-class implementation (AM or FM demod) to handle
                case REQUEST_CURRENT_SQUELCH_THRESHOLD:
                case REQUEST_CHANGE_SQUELCH_THRESHOLD:
                case REQUEST_CURRENT_SQUELCH_AUTO_TRACK:
                case REQUEST_CHANGE_SQUELCH_AUTO_TRACK:
                    //                    mDemodulator.receive(sourceEvent);
                    break;
                case NOTIFICATION_SAMPLE_RATE_CHANGE:
                    if(mIBasebandFilter != null)
                    {
                        mIBasebandFilter = null;
                        mQBasebandFilter = null;
                    }

                    double sampleRate = sourceEvent.getValue().doubleValue();

                    int decimationRate = 0;
                    double decimatedSampleRate = sampleRate;

                    if(sampleRate / 2 >= (mChannelBandwidth * 2))
                    {
                        decimationRate = 2;

                        while(sampleRate / decimationRate / 2 >= (mChannelBandwidth * 2))
                        {
                            decimationRate *= 2;
                        }
                    }

                    if(decimationRate > 0)
                    {
                        decimatedSampleRate /= decimationRate;
                    }

                    mIDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimationRate);
                    mQDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimationRate);

                    if((decimatedSampleRate < (2.0 * mChannelBandwidth)))
                    {
                        throw new IllegalStateException(getDecoderType().name() + " demodulator with channel bandwidth [" + mChannelBandwidth + "] requires a channel sample rate of [" + (2.0 * mChannelBandwidth + "] - sample rate of [" + decimatedSampleRate + "] is not supported"));
                    }

                    mNoiseSquelch.setSampleRate(decimatedSampleRate);

                    int passBandStop = (int) (mChannelBandwidth * .8);
                    int stopBandStart = (int) mChannelBandwidth;

                    float[] coefficients = null;

                    FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder().sampleRate(decimatedSampleRate * 2).gridDensity(16).oddLength(true).passBandCutoff(passBandStop).passBandAmplitude(1.0).passBandRipple(0.01).stopBandStart(stopBandStart).stopBandAmplitude(0.0).stopBandRipple(0.005) //Approximately 90 dB attenuation
                            .build();

                    try
                    {
                        coefficients = FilterFactory.getTaps(specification);
                    }
                    catch(FilterDesignException fde)
                    {
                        mLog.error("Couldn't design demodulator remez filter for sample rate [" + sampleRate + "] pass frequency [" + passBandStop + "] and stop frequency [" + stopBandStart + "] - will proceed using sinc (low-pass) filter");
                    }

                    if(coefficients == null)
                    {
                        mLog.info("Unable to use remez filter designer for sample rate [" + decimatedSampleRate + "] pass band stop [" + passBandStop + "] and stop band start [" + stopBandStart + "] - will proceed using simple low pass filter design");
                        coefficients = FilterFactory.getLowPass(decimatedSampleRate, passBandStop, stopBandStart, 60, WindowType.HAMMING, true);
                    }

                    mIBasebandFilter = FilterFactory.getRealFilter(coefficients);
                    mQBasebandFilter = FilterFactory.getRealFilter(coefficients);

                    mResampler = new RealResampler(decimatedSampleRate, DEMODULATED_AUDIO_SAMPLE_RATE, 4192, 512);
                    mResampler.setListener(NBFMDecoder2.this::broadcast);
                    break;
            }
        }
    }
}
