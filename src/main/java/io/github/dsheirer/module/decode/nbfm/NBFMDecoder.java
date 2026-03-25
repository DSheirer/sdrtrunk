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
import io.github.dsheirer.dsp.gain.AudioGainAndDcFilter;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.SquelchControlDecoder;
import io.github.dsheirer.module.decode.squelchFilter.SquelchFilterConfig;
import io.github.dsheirer.module.decode.squelchFilter.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.squelchFilter.ctcss.CTCSSDetector;
import io.github.dsheirer.module.decode.squelchFilter.dcs.DCSCode;
import io.github.dsheirer.module.decode.squelchFilter.dcs.DCSDetector;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.sample.real.IRealBufferProvider;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * NBFM decoder with integrated noise squelch, channel-level tone filtering, FM de-emphasis,
 * and squelch tail removal.
 *
 *
 * When tone filtering is enabled, audio only passes when noise squelch is open AND a
 * matching CTCSS/DCS tone is detected.
 */
public class NBFMDecoder extends SquelchControlDecoder implements ISourceEventListener, IComplexSamplesListener,
        Listener<ComplexSamples>, IRealBufferProvider, IDecoderStateEventProvider, INoiseSquelchController
{
    private final static Logger mLog = LoggerFactory.getLogger(NBFMDecoder.class);
    private NBFMDecoderState mDecoderState;
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

    // === TODO: FM de-emphasis and gain will need to be moved downstream of the 1200 baud decoders
    private float mDeemphasisAlpha = 0;
    private float mPreviousDeemphasis = 0;
    private boolean mDeemphasisEnabled = false;
    private AudioGainAndDcFilter mAudioGain;

     // === TODO: might need to be reorganized and removed from this file
    private boolean mCTCSSSquelchEnabled = false;
    private Set<CTCSSCode> mAllowedCTCSSCodes = EnumSet.noneOf(CTCSSCode.class);
    private Set<DCSCode> mAllowedDCSCodes = new HashSet<>();
    private volatile CTCSSCode mDetectedCTCSS = null;
    private volatile DCSCode mDetectedDCS = null;
    private volatile boolean mToneMatched = false;
    private int mSquelchClosedSamples = 0;
    private int mSquelchHoldoverSamples = 0; // Set in setSampleRate()
    private CTCSSDetector mCTCSSDetector = null;
    private DCSDetector mDCSDetector = null;
    private int mToneDetectorSkipCounter = 0;

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

        // Configure de-emphasis
        configureDeemphasis(config.getDeemphasis());
        mAudioGain = new AudioGainAndDcFilter(.5F, 5F, 0.7F);
        mAudioGain.setDecayRate(2);     // set a 2 percent decay rate

         // Configure tone filtering
        configureToneFilters(config);

        // Audio pipeline: NoiseSquelch -> Resampler -> CTCSS -> De-emphasis -> gain -> Output
        // TODO: other decoders need unfiltered audio, so de-emphasis and gain need to be addressed somewhere else

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
                notifyCallContinuation();
            }
        });

        //Notify the decoder state of call starts and ends
        mNoiseSquelch.setSquelchStateListener(squelchState -> {
            if(squelchState == SquelchState.SQUELCH)
            {
                notifyCallEnd();
            }
            else
            {
                // When tone filtering is enabled, delay call start until tone is matched.
                // The call start will be triggered from ctcssDetected/dcsDetected instead.
                if(!mCTCSSSquelchEnabled)
                {
                    notifyCallStart();
                }
            }
        });
    }

    /**
     * Sets the decoder state reference so the decoder can push detected tone updates.
     * @param decoderState the NBFM decoder state to receive tone notifications
     */
    public void setDecoderState(NBFMDecoderState decoderState)
    {
        mDecoderState = decoderState;
    }

    /**
     * Configures FM de-emphasis filter parameters based on the selected mode
     */
    private void configureDeemphasis(DecodeConfigNBFM.DeemphasisMode mode)
    {
        if(mode != null && mode != DecodeConfigNBFM.DeemphasisMode.NONE && mode.getMicroseconds() > 0)
        {
            mDeemphasisEnabled = true;
            double tau = mode.getMicroseconds() / 1_000_000.0; // Convert µs to seconds
            double dt = 1.0 / DEMODULATED_AUDIO_SAMPLE_RATE;
            mDeemphasisAlpha = (float)(dt / (tau + dt));
        }
        else
        {
            mDeemphasisEnabled = false;
        }
    }

    /**
     * Applies single-pole IIR de-emphasis filter to demodulated audio.
     * This restores flat frequency response from pre-emphasized FM transmission.
     */
    private float[] applyDeemphasis(float[] samples)
    {
        if(!mDeemphasisEnabled || mDeemphasisAlpha <= 0)
        {
            return samples;
        }

        float[] output = new float[samples.length];
        float prev = mPreviousDeemphasis;

        for(int i = 0; i < samples.length; i++)
        {
            output[i] = mDeemphasisAlpha * samples[i] + (1.0f - mDeemphasisAlpha) * prev;
            prev = output[i];
        }

        mPreviousDeemphasis = prev;
        return output;
    }

    /**
     * Configures the set of allowed tones from the channel decode configuration
     */
    private void configureToneFilters(DecodeConfigNBFM config)
    {
        mCTCSSSquelchEnabled = config.isSquelchFilterEnabled();

        if(mCTCSSSquelchEnabled)
        {
            List<SquelchFilterConfig> filters = config.getSquelchFilters();
            for(SquelchFilterConfig filter : filters)
            {
                if(!filter.isValid())
                {
                    continue;
                }

                switch(filter.getSquelchType())
                {
                    case CTCSS:
                        CTCSSCode ctcss = filter.getCTCSSCode();
                        if(ctcss != null && ctcss != CTCSSCode.UNKNOWNH)
                        {
                            mAllowedCTCSSCodes.add(ctcss);
                        }
                        break;
                    case DCS:
                        DCSCode dcs = filter.getDCSCode();
                        if(dcs != null)
                        {
                            mAllowedDCSCodes.add(dcs);
                        }
                        break;
                }
            }

            // If we configured tone filtering but have no valid tones, disable it
            if(mAllowedCTCSSCodes.isEmpty() && mAllowedDCSCodes.isEmpty())
            {
                mLog.warn("Tone filtering enabled but no valid CTCSS/DCS codes configured — disabling tone filter");
                mCTCSSSquelchEnabled = false;
            }
            else
            {
                mLog.info("NBFM tone filtering enabled: {} CTCSS codes, {} DCS codes",
                        mAllowedCTCSSCodes.size(), mAllowedDCSCodes.size());

                // Create CTCSS detector if we have CTCSS codes to detect
                // Note: detector is initialized with 8000 Hz sample rate; it will be
                // recreated in setSampleRate() if the actual rate differs
                if(!mAllowedCTCSSCodes.isEmpty())
                {
                    createCTCSSDetector();
                }

                // Create DCS detector if we have DCS codes to detect
                if(!mAllowedDCSCodes.isEmpty())
                {
                    createDCSDetector(8000.0f);
                }
            }
        }
    }

    /**
     * Creates the CTCSS Goertzel detector at the specified sample rate.
     */
    private void createCTCSSDetector()
    {
        mCTCSSDetector = new CTCSSDetector(mAllowedCTCSSCodes, (float) DEMODULATED_AUDIO_SAMPLE_RATE);
        // Goertzel now comes after resampler which is 8000 Hz sample rate (fixed)
        //mCTCSSDetector = new CTCSSDetector();
        mCTCSSDetector.setListener(new CTCSSDetector.CTCSSDetectorListener()
        {
            @Override
            public void ctcssDetected(CTCSSCode code)
            {
                NBFMDecoder.this.ctcssDetected(code);
            }

            @Override
            public void ctcssRejected(CTCSSCode code)
            {
                if(mDecoderState != null && code != null)
                {
                    mDecoderState.setRejectedCTCSS(code);
                }
            }

            @Override
            public void ctcssLost()
            {
                NBFMDecoder.this.toneLost();
            }
        });
    }

    /**
     * Creates the DCS detector at the specified sample rate.
     * @param sampleRate of the demodulated audio
     */
    private void createDCSDetector(float sampleRate)
    {
        mDCSDetector = new DCSDetector(mAllowedDCSCodes, sampleRate);
        mDCSDetector.setListener(new DCSDetector.DCSDetectorListener()
        {
            @Override
            public void dcsDetected(DCSCode code)
            {
                NBFMDecoder.this.dcsDetected(code);
            }

            @Override
            public void dcsLost()
            {
                NBFMDecoder.this.toneLost();
            }
        });
    }

    /**
     * Called by CTCSS decoder when a tone is detected. If tone filtering is enabled,
     * this updates the tone match state.
     * @param code the detected CTCSS tone code
     */
    public void ctcssDetected(CTCSSCode code)
    {
        mDetectedCTCSS = code;
        if(mCTCSSSquelchEnabled && code != null && mAllowedCTCSSCodes.contains(code))
        {
            if(!mToneMatched)
            {
                mToneMatched = true;
                // Tone just matched — now fire the deferred call start
                notifyCallStart();
            }
        }

        // Push to decoder state for activity summary display
        if(mDecoderState != null && code != null)
        {
            mDecoderState.setDetectedCTCSS(code);
        }
    }

    /**
     * Called by DCS decoder when a code is detected. If tone filtering is enabled,
     * this updates the tone match state.
     * @param code the detected DCS code
     */
    public void dcsDetected(DCSCode code)
    {
        mDetectedDCS = code;
        if(mCTCSSSquelchEnabled && code != null && mAllowedDCSCodes.contains(code))
        {
            if(!mToneMatched)
            {
                mToneMatched = true;
                notifyCallStart();
            }
        }

        // Push to decoder state for activity summary display
        if(mDecoderState != null && code != null)
        {
            mDecoderState.setDetectedDCS(code);
        }
    }

    /**
     * Called when tone is lost (no longer detected). Resets tone match state.
     */
    public void toneLost()
    {
        mDetectedCTCSS = null;
        mDetectedDCS = null;
        mToneMatched = false;

        // Notify decoder state
        if(mDecoderState != null)
        {
            mDecoderState.setToneLost();
        }
    }

    /**
     * Indicates if a matching tone is currently detected
     */
    public boolean isToneMatched()
    {
        return !mCTCSSSquelchEnabled || mToneMatched;
    }

    /**
     * Returns the currently detected CTCSS code, or null
     */
    public CTCSSCode getDetectedCTCSS()
    {
        return mDetectedCTCSS;
    }

    /**
     * Returns the currently detected DCS code, or null
     */
    public DCSCode getDetectedDCS()
    {
        return mDetectedDCS;
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

        mNoiseSquelch.process(demodulated);

        //Once we process the sample buffer, if the ending state is squelch closed, update the decoder state that we
        // are idle.
        if(mNoiseSquelch.isSquelched())
        {
//            // Don't immediately clear tone match — squelch may briefly close during
//            // a transmission (noise spikes, signal fading). Track how long squelch
//            // has been closed and only clear after sustained silence.
//            // TODO: this is probably not needed. If the squelch is closed, no tone detection is taking place
//            if(mCTCSSSquelchEnabled)
//            {
//                mSquelchClosedSamples += demodulated.length;
//
//                // Clear tone match after ~500ms of sustained squelch (transmission truly ended)
//                if(mSquelchClosedSamples > mSquelchHoldoverSamples)
//                {
//                    mToneMatched = false;
//                }
//            }
            notifyIdle();
        }
//        else
//        {
//            // Squelch is open — reset the closed counter
//            mSquelchClosedSamples = 0;
//        }
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

//        // === NEW: Calculate de-emphasis alpha for this sample rate ===
//        if(mDeemphasisEnabled)
//        {
//            DecodeConfigNBFM.DeemphasisMode mode = getDecodeConfiguration().getDeemphasis();
//            if(mode != null && mode.getMicroseconds() > 0)
//            {
//                double tau = mode.getMicroseconds() / 1_000_000.0; // Convert µs to seconds
//                double dt = 1.0 / decimatedSampleRate;
//                mDeemphasisAlpha = (float)(dt / (tau + dt));
//                mLog.info("FM de-emphasis configured: τ={}µs, α={}, sample rate={}",
//                        mode.getMicroseconds(), String.format("%.6f", mDeemphasisAlpha), decimatedSampleRate);
//            }
//        }

//        // Recreate CTCSS detector at the actual decimated sample rate
//        if(mCTCSSSquelchEnabled && !mAllowedCTCSSCodes.isEmpty())
//        {
//            createCTCSSDetector((float) decimatedSampleRate);
//        }
//
//        // Recreate DCS detector at the actual decimated sample rate
//        if(mCTCSSSquelchEnabled && !mAllowedDCSCodes.isEmpty())
//        {
//            createDCSDetector((float) decimatedSampleRate);
//        }

//        // Tone match holdover: 500ms of sustained squelch before clearing tone match
//        mSquelchHoldoverSamples = (int)(decimatedSampleRate * 0.5);

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

        //mResampler.setListener(NBFMDecoder.this::broadcast);
        mResampler.setListener(NBFMDecoder.this::processResampledAudio);

    }

    private void processResampledAudio(float [] resampled)
    {
        // CTCSS detection
        //
        // Only skip when squelch is closed (no signal = no tone to detect).
        if(!mNoiseSquelch.isSquelched())
        {
            if(mCTCSSDetector != null)
            {
                mCTCSSDetector.process(resampled);
            }
            if(mDCSDetector != null)
            {
                mDCSDetector.process(resampled);
            }
        }
        else
        {
            if(mToneMatched)
                mToneMatched = false;
        }
        if(mCTCSSSquelchEnabled && !mToneMatched)
        {
            // Tone filtering enabled but no match - drop audio buffer here
            return;
        }
        // deemphasis filter
        float[] audio = resampled;
        if(mDeemphasisEnabled)
        {
            audio = applyDeemphasis(resampled);
            audio = mAudioGain.process(audio);      // usually need some gain after de-emphasis
        }
        // audio gain
        // send audio to registered listeners
        // TODO: need to do something about listeners that need unfiltered audio or move audio filters
        broadcast(audio);
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
