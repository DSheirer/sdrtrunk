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
import io.github.dsheirer.module.decode.SquelchControlDecoder;
import io.github.dsheirer.module.decode.squelchDecoder.ctcss.CTCSSMessage;
import io.github.dsheirer.module.decode.squelchDecoder.dcs.DCSDecoder;
import io.github.dsheirer.module.decode.squelchDecoder.dcs.DCSEncode;
import io.github.dsheirer.module.decode.squelchDecoder.dcs.DCSMessage;
import io.github.dsheirer.module.decode.squelchDecoder.squelchDecoderConfig;
import io.github.dsheirer.module.decode.squelchDecoder.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.squelchDecoder.ctcss.CTCSSDetector;
import io.github.dsheirer.module.decode.squelchDecoder.dcs.DCSCode;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.sample.real.IRealBufferProvider;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * NBFM decoder with integrated noise squelch, channel-level squelch tone decoders.
 *
 */
public class NBFMDecoder extends SquelchControlDecoder implements ISourceEventListener, IComplexSamplesListener,
        Listener<ComplexSamples>, IRealBufferProvider, IDecoderStateEventProvider, INoiseSquelchController
{
    private final static Logger mLog = LoggerFactory.getLogger(NBFMDecoder.class);
    private static final double DEMODULATED_AUDIO_SAMPLE_RATE = 8000.0;
    private final IDemodulator mDemodulator = FmDemodulatorFactory.getFmDemodulator();
    private final SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private final NoiseSquelch mNoiseSquelch;
    private IRealFilter mIBasebandFilter;
    private IRealFilter mQBasebandFilter;
    private IRealDecimationFilter mIDecimationFilter;
    private IRealDecimationFilter mQDecimationFilter;
    private Listener<float[]> mResampledBufferListener;
    private Listener<DecoderStateEvent> mDecoderStateEventListener;
    private RealResampler mResampler;
    private final double mChannelBandwidth;
    private boolean mSquelchDecoderEnabled = false;
    private List<CTCSSCode> mConfiguredCTCSSCodes = new ArrayList<>();
    private Set<DCSCode> mConfiguredDCSCodes = new HashSet<>();
    private CTCSSDetector mCTCSSDetector = null;
    private DCSDecoder mDCSDetector = null;
    private boolean mMute = true;

    /**
     * Constructs an instance
     *
     * @param config to setup the NBFM decoder and noise squelch control.
     */
    public NBFMDecoder(DecodeConfigNBFM config)
    {
        super(config);

        //Save channel bandwidth to setup channel baseband filter.
        mChannelBandwidth = config.getBandwidth().getValue();
        mNoiseSquelch = new NoiseSquelch(config.getSquelchNoiseOpenThreshold(), config.getSquelchNoiseCloseThreshold(),
                config.getSquelchHysteresisOpenThreshold(), config.getSquelchHysteresisCloseThreshold());

         // Configure squelch code decoders
        configureSquelchDecoders(config);

        // Audio pipeline: NoiseSquelch -> Resampler -> CTCSS or DCS -> 1200 baud decoders -> HPF -> De-emphasis -> gain -> Output

        mNoiseSquelch.setAudioListener(audio -> {
            // if squelch is closing (it hasn't propagated yet to mute the audio)
            //  call the resampler with lastBatch set to true. This will zero pad the input buffer and ensure
            //  the output buffer gets emptied.
            if(mNoiseSquelch.isSquelched())
            {
                mResampler.resample(audio, true);
            }
            else
            {
                mResampler.resample(audio);     // this method will set lastBatch to false
                if (!mSquelchDecoderEnabled)
                {
                    notifyCallContinuation();
                }
            }
        });

        //Notify the decoder state of call starts and ends
        mNoiseSquelch.setSquelchStateListener(squelchState -> {
            if(squelchState == SquelchState.SQUELCH && !mSquelchDecoderEnabled)
            {
                notifyCallEnd();
            }
            else
            {
                // When tone filtering is enabled, delay call start until tone is matched.
                // The call start will be triggered from ctcssDetected/dcsDetected instead.
                if(!mSquelchDecoderEnabled)
                {
                    notifyCallStart();
                }
            }
        });

        int dummy = DCSEncode.encode(DCSEncode.OctStr2Int("036"));

    }

    /**
     * Configures the set of allowed codes from the channel decode configuration
     */
    private void configureSquelchDecoders(DecodeConfigNBFM config)
    {
        mSquelchDecoderEnabled = config.isSquelchDecoderEnabled();

        if(mSquelchDecoderEnabled)
        {
            // at the present time, only a single decoder per channel is configured, however the playlist and other
            //  storage allows for multiple decoders per channel
            List<squelchDecoderConfig> decoders = config.getSquelchDecoders();
            for(squelchDecoderConfig decoder : decoders)
            {
                if(!decoder.isValid())
                {
                    continue;
                }
                switch(decoder.getSquelchType())
                {
                    case CTCSS:
                        CTCSSCode ctcss = decoder.getCTCSSCode();
                        if(ctcss != null && ctcss != CTCSSCode.UNKNOWNH)
                        {
                            mConfiguredCTCSSCodes.add(ctcss);
                        }
                        break;
                    case DCS:
                        DCSCode dcs = decoder.getDCSCode();
                        if(dcs != null)
                        {
                            mConfiguredDCSCodes.add(dcs);
                        }
                        break;
                }
            }

            // If we configured tone filtering but have no valid tones, disable it
            if(mConfiguredCTCSSCodes.isEmpty() && mConfiguredDCSCodes.isEmpty())
            {
                mLog.warn("Tone filtering enabled but no valid CTCSS/DCS codes configured — disabling tone filter");
                mSquelchDecoderEnabled = false;
            }
            else
            {
                // Create CTCSS detector if we have CTCSS codes to detect
                if(!mConfiguredCTCSSCodes.isEmpty())
                {
                    mCTCSSDetector = new CTCSSDetector(mConfiguredCTCSSCodes);
                }

                // Create DCS detector if we have DCS codes to detect
                if(!mConfiguredDCSCodes.isEmpty())
                {
                    mDCSDetector = new DCSDecoder(mConfiguredDCSCodes);
                }
            }
        }
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.NBFM;
    }

    /**
     * Decode configuration for this decoder.
     * @return configuration
     */
    @Override
    public DecodeConfigNBFM getDecodeConfiguration()
    {
        return (DecodeConfigNBFM)super.getDecodeConfiguration();
    }

    /**
     * Register the noise squelch state listener.  This will normally be a GUI noise squelch state view/controller.
     * @param listener to receive states or pass null to de-register a listener.
     */
    @Override
    public void setNoiseSquelchStateListener(Listener<NoiseSquelchState> listener)
    {
        mNoiseSquelch.setNoiseSquelchStateListener(listener);
    }

    /**
     * Applies new open and close noise threshold values for the noise squelch.
     * @param open for the open noise variance calculation in range 0.1 - 0.5 where open <= close value.
     * @param close for the close noise variance calculation. in range 0.1 - 0.5 where close >= open.
     */
    @Override
    public void setNoiseThreshold(float open, float close)
    {
        mNoiseSquelch.setNoiseThreshold(open, close);

        //Update the channel configuration and schedule a playlist save.
        getDecodeConfiguration().setSquelchNoiseOpenThreshold(open);
        getDecodeConfiguration().setSquelchNoiseCloseThreshold(close);
    }

    /**
     * Sets the open and close hysteresis thresholds in units of 10 milliseconds.
     * @param open in range 1-10, recommend: 4 where open <= close
     * @param close in range 1-10, recommend: 6 where close >= open.
     */
    @Override
    public void setHysteresisThreshold(int open, int close)
    {
        mNoiseSquelch.setHysteresisThreshold(open, close);
        getDecodeConfiguration().setSquelchHysteresisOpenThreshold(open);
        getDecodeConfiguration().setSquelchHysteresisCloseThreshold(close);
    }

    /**
     * Sets the squelch override state to temporarily bypass/override squelch control and pass all audio.
     * @param override (true) or (false) to turn off squelch override.
     */
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
    public void reset() {}

    @Override
    public void start() {}

    @Override
    public void stop()  {}

    /**
     * Broadcasts the demodulated, resampled to 8 kHz audio samples to the registered listener.
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
     * @param listener to receive demodulated, resampled audio sample buffers.
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
     * Implements the Listener<ComplexSample> interface to receive a stream of complex I/Q sample buffers
     */
    @Override
    public void receive(ComplexSamples samples)
    {
        if(mIDecimationFilter == null || mQDecimationFilter == null)
        {
            throw new IllegalStateException("NBFM demodulator module must receive a sample rate change source event " +
                    "before it can process complex sample buffers");
        }

        float[] decimatedI = mIDecimationFilter.decimateReal(samples.i());
        float[] decimatedQ = mQDecimationFilter.decimateReal(samples.q());

        float[] filteredI = mIBasebandFilter.filter(decimatedI);
        float[] filteredQ = mQBasebandFilter.filter(decimatedQ);

        float[] demodulated = mDemodulator.demodulate(filteredI, filteredQ);
        /**
         * The following call completes a series of tasks:
         *  - determines noise squelch states
         *  - resamples audio via the squelch listener
         *  - processes resample audio for squelch decoders via the resampler listener
         *  - broadcasts audio to downstream registered listeners, such as audioModule as determined by DecoderFactory
         */
        mNoiseSquelch.process(demodulated);

        //Once we process the sample buffer, if the ending state is squelch closed, update the decoder state that we
        // are idle.
        if(mNoiseSquelch.isSquelched())
        {
            notifyIdle();
        }
    }

    /**
     * Broadcasts a call start state event
     */
    private void notifyCallStart()
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
     * Updates the decoder to process complex sample buffers at the specified sample rate.
     * @param sampleRate of the incoming complex sample buffer stream.
     */
    private void setSampleRate(double sampleRate)
    {
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

        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate(decimatedSampleRate * 2)
                .gridDensity(16)
                .oddLength(true)
                .passBandCutoff(passBandStop)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandStart(stopBandStart)
                .stopBandAmplitude(0.0)
                .stopBandRipple(0.005)
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

        mResampler.setListener(NBFMDecoder.this::processResampledAudio);

    }

    /**
     * Process the Resampled audio for squelch decoding.  This also where audio is muted if no tone code match.
     * @param resampled audio buffer
     */

    private void processResampledAudio(float [] resampled)
    {
        if(!mNoiseSquelch.isSquelched())
        {
            if(mCTCSSDetector != null)
            {
                CTCSSMessage ctcssMessage = mCTCSSDetector.process(resampled);
                getMessageListener().receive(ctcssMessage);     // sending: one of the listeners is NBFMDecoderState
                if(ctcssMessage.getCallEvent() != null)
                {
                    // handles START, CONTINUATION, END.
                    broadcast(new DecoderStateEvent(this, ctcssMessage.getCallEvent(), State.CALL, 0));
                }
                mMute = ctcssMessage.getMutedStatus();
            }
            /*
             * While the CTCSS decoder can determine a tone in a single buffer of resampled audio, the DCS decoder
             * requires 2.6 buffers to determine a code. DCSMessage will be null for audio buffers where there is
             * no state change.
             */
            if(mDCSDetector != null)
            {
                DCSMessage dcsMessage = mDCSDetector.process(resampled);
                if (dcsMessage != null)
                {
                    getMessageListener().receive(dcsMessage);     // sending: one of the listeners is NBFMDecoderState
                    if (dcsMessage.getCallEvent() != null)
                    {
                        // handles START, CONTINUATION, END.
                        broadcast(new DecoderStateEvent(this, dcsMessage.getCallEvent(), State.CALL, 0));
                    }
                    mMute = dcsMessage.getMutedStatus();
                }
            }
        }
        else
        {
            // there can still be one buffer's worth of resampled audio after .isSquelched()
            // which may result in two consectutive paths here. While the squelch decoders will mute here,
            // the noise squelch will mute audio in AudioModule.
            if(mCTCSSDetector != null)
            {
                CTCSSMessage ctcssMessage = mCTCSSDetector.reset();
                getMessageListener().receive(ctcssMessage);     // sending: one of the listeners is NBFMDecoderState
                notifyCallEnd();
                mMute = true;
            }
            if(mDCSDetector != null)
            {
                DCSMessage dcsMessage = mDCSDetector.inlineReset();
                getMessageListener().receive(dcsMessage);       // sending: one of the listeners is NBFMDecoderState
                notifyCallEnd();
                mMute = true;
            }
        }

        if(mSquelchDecoderEnabled && mMute)
        {
            return;     // drop audio buffer here for muting
        }

        broadcast(resampled);
    }

    /**
     * Monitors sample rate change source event(s) to set up the filters, decimation, and demodulator.
     */
    public class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
            {
                setSampleRate(sourceEvent.getValue().doubleValue());
            }
        }
    }
}
