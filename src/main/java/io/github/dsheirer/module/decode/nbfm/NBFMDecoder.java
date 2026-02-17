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
import io.github.dsheirer.dsp.squelch.SquelchTailRemover;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.SquelchControlDecoder;
import io.github.dsheirer.module.decode.config.ChannelToneFilter;
import io.github.dsheirer.module.decode.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.dcs.DCSCode;
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
 * Audio chain:
 *   ComplexSamples → Decimate → Baseband Filter → FM Demod → De-emphasis → NoiseSquelch
 *   → ToneGate → SquelchTailRemover → Resample(8kHz) → Output
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

    // === NEW: FM de-emphasis ===
    private float mDeemphasisAlpha = 0;
    private float mPreviousDeemphasis = 0;
    private boolean mDeemphasisEnabled = false;

    // === NEW: Squelch tail remover ===
    private SquelchTailRemover mSquelchTailRemover;
    private boolean mSquelchTailRemovalEnabled = false;

    // === NEW: Tone gating ===
    private boolean mToneFilterEnabled = false;
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

        mChannelBandwidth = config.getBandwidth().getValue();
        mNoiseSquelch = new NoiseSquelch(config.getSquelchNoiseOpenThreshold(), config.getSquelchNoiseCloseThreshold(),
                config.getSquelchHysteresisOpenThreshold(), config.getSquelchHysteresisCloseThreshold());

        // Configure de-emphasis
        configureDeemphasis(config.getDeemphasis());

        // Configure squelch tail removal
        if(config.isSquelchTailRemovalEnabled())
        {
            mSquelchTailRemovalEnabled = true;
            mSquelchTailRemover = new SquelchTailRemover(
                    config.getSquelchTailRemovalMs(),
                    config.getSquelchHeadRemovalMs()
            );
        }

        // Configure tone filtering
        configureToneFilters(config);

        // Audio pipeline: NoiseSquelch → De-emphasis → (ToneGate) → (TailRemover) → Resampler → Output
        mNoiseSquelch.setAudioListener(audio -> {
            if(mToneFilterEnabled && !mToneMatched)
            {
                // Tone filtering enabled but no match — block audio
                return;
            }

            // Apply de-emphasis AFTER squelch (de-emphasis before squelch would
            // attenuate high-frequency noise and prevent squelch from closing)
            if(mDeemphasisEnabled)
            {
                audio = applyDeemphasis(audio);
            }

            if(mSquelchTailRemovalEnabled && mSquelchTailRemover != null)
            {
                mSquelchTailRemover.process(audio);
            }
            else
            {
                mResampler.resample(audio);
            }

            notifyCallContinuation();
        });

        // Squelch state changes → notify decoder state + tail remover
        mNoiseSquelch.setSquelchStateListener(squelchState -> {
            if(squelchState == SquelchState.SQUELCH)
            {
                if(mSquelchTailRemovalEnabled && mSquelchTailRemover != null)
                {
                    mSquelchTailRemover.squelchClose();
                }
                notifyCallEnd();
            }
            else
            {
                if(mSquelchTailRemovalEnabled && mSquelchTailRemover != null)
                {
                    mSquelchTailRemover.squelchOpen();
                }
                // When tone filtering is enabled, delay call start until tone is matched.
                // The call start will be triggered from ctcssDetected/dcsDetected instead.
                if(!mToneFilterEnabled)
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
            // Alpha will be recalculated when sample rate is known
            // For now, store the time constant
            mDeemphasisAlpha = 0; // Will be set in setSampleRate()
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
        mToneFilterEnabled = config.isToneFilterEnabled();

        if(mToneFilterEnabled)
        {
            List<ChannelToneFilter> filters = config.getToneFilters();
            for(ChannelToneFilter filter : filters)
            {
                if(!filter.isValid())
                {
                    continue;
                }

                switch(filter.getToneType())
                {
                    case CTCSS:
                        CTCSSCode ctcss = filter.getCTCSSCode();
                        if(ctcss != null && ctcss != CTCSSCode.UNKNOWN)
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
                    case NAC:
                        // NAC filters are for P25, not NBFM — ignore here
                        break;
                }
            }

            // If we configured tone filtering but have no valid tones, disable it
            if(mAllowedCTCSSCodes.isEmpty() && mAllowedDCSCodes.isEmpty())
            {
                mLog.warn("Tone filtering enabled but no valid CTCSS/DCS codes configured — disabling tone filter");
                mToneFilterEnabled = false;
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
                    createCTCSSDetector(8000.0f);
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
     * @param sampleRate of the demodulated audio
     */
    private void createCTCSSDetector(float sampleRate)
    {
        mCTCSSDetector = new CTCSSDetector(mAllowedCTCSSCodes, sampleRate);
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
        if(mToneFilterEnabled && code != null && mAllowedCTCSSCodes.contains(code))
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
        if(mToneFilterEnabled && code != null && mAllowedDCSCodes.contains(code))
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
        return !mToneFilterEnabled || mToneMatched;
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

    @Override
    public DecodeConfigNBFM getDecodeConfiguration()
    {
        return (DecodeConfigNBFM)super.getDecodeConfiguration();
    }

    @Override
    public void setNoiseSquelchStateListener(Listener<NoiseSquelchState> listener)
    {
        mNoiseSquelch.setNoiseSquelchStateListener(listener);
    }

    @Override
    public void setNoiseThreshold(float open, float close)
    {
        mNoiseSquelch.setNoiseThreshold(open, close);
        getDecodeConfiguration().setSquelchNoiseOpenThreshold(open);
        getDecodeConfiguration().setSquelchNoiseCloseThreshold(close);
    }

    @Override
    public void setHysteresisThreshold(int open, int close)
    {
        mNoiseSquelch.setHysteresisThreshold(open, close);
        getDecodeConfiguration().setSquelchHysteresisOpenThreshold(open);
        getDecodeConfiguration().setSquelchHysteresisCloseThreshold(close);
    }

    @Override
    public void setSquelchOverride(boolean override)
    {
        mNoiseSquelch.setSquelchOverride(override);
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventProcessor;
    }

    @Override
    public void reset() {}

    @Override
    public void start() {}

    @Override
    public void stop()
    {
        if(mSquelchTailRemover != null)
        {
            mSquelchTailRemover.reset();
        }
    }

    protected void broadcast(float[] demodulatedSamples)
    {
        if(mResampledBufferListener != null)
        {
            mResampledBufferListener.receive(demodulatedSamples);
        }
    }

    @Override
    public void setBufferListener(Listener<float[]> listener)
    {
        mResampledBufferListener = listener;
    }

    @Override
    public void removeBufferListener()
    {
        mResampledBufferListener = null;
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

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

        // Run tone detectors on demodulated audio BEFORE noise squelch processing
        // (squelch high-pass filter removes sub-audible tones).
        // Only skip when squelch is closed (no signal = no tone to detect).
        if((mCTCSSDetector != null || mDCSDetector != null) && !mNoiseSquelch.isSquelched())
        {
            if(mCTCSSDetector != null)
            {
                mCTCSSDetector.process(demodulated);
            }
            if(mDCSDetector != null)
            {
                mDCSDetector.process(demodulated);
            }
        }

        mNoiseSquelch.process(demodulated);

        if(mNoiseSquelch.isSquelched())
        {
            // Don't immediately clear tone match — squelch may briefly close during
            // a transmission (noise spikes, signal fading). Track how long squelch
            // has been closed and only clear after sustained silence.
            if(mToneFilterEnabled)
            {
                mSquelchClosedSamples += demodulated.length;

                // Clear tone match after ~500ms of sustained squelch (transmission truly ended)
                if(mSquelchClosedSamples > mSquelchHoldoverSamples)
                {
                    mToneMatched = false;
                }
            }
            notifyIdle();
        }
        else
        {
            // Squelch is open — reset the closed counter
            mSquelchClosedSamples = 0;
        }
    }

    private void notifyCallStart()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.START, State.CALL, 0));
    }

    private void notifyCallContinuation()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION, State.CALL, 0));
    }

    private void notifyCallEnd()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.END, State.CALL, 0));
    }

    private void notifyIdle()
    {
        broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION, State.IDLE, 0));
    }

    private void broadcast(DecoderStateEvent event)
    {
        if(mDecoderStateEventListener != null)
        {
            mDecoderStateEventListener.receive(event);
        }
    }

    @Override
    public void setDecoderStateListener(Listener<DecoderStateEvent> listener)
    {
        mDecoderStateEventListener = listener;
    }

    @Override
    public void removeDecoderStateListener()
    {
        mDecoderStateEventListener = null;
    }

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

        // === NEW: Calculate de-emphasis alpha for this sample rate ===
        if(mDeemphasisEnabled)
        {
            DecodeConfigNBFM.DeemphasisMode mode = getDecodeConfiguration().getDeemphasis();
            if(mode != null && mode.getMicroseconds() > 0)
            {
                double tau = mode.getMicroseconds() / 1_000_000.0; // Convert µs to seconds
                double dt = 1.0 / decimatedSampleRate;
                mDeemphasisAlpha = (float)(dt / (tau + dt));
                mLog.info("FM de-emphasis configured: τ={}µs, α={}, sample rate={}",
                        mode.getMicroseconds(), String.format("%.6f", mDeemphasisAlpha), decimatedSampleRate);
            }
        }

        // Recreate CTCSS detector at the actual decimated sample rate
        if(mToneFilterEnabled && !mAllowedCTCSSCodes.isEmpty())
        {
            createCTCSSDetector((float) decimatedSampleRate);
        }

        // Recreate DCS detector at the actual decimated sample rate
        if(mToneFilterEnabled && !mAllowedDCSCodes.isEmpty())
        {
            createDCSDetector((float) decimatedSampleRate);
        }

        // Tone match holdover: 500ms of sustained squelch before clearing tone match
        mSquelchHoldoverSamples = (int)(decimatedSampleRate * 0.5);

        int passBandStop = (int) (mChannelBandwidth * .8);
        int stopBandStart = (int) mChannelBandwidth;

        float[] coefficients = null;

        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder().sampleRate(decimatedSampleRate * 2).gridDensity(16).oddLength(true).passBandCutoff(passBandStop).passBandAmplitude(1.0).passBandRipple(0.01).stopBandStart(stopBandStart).stopBandAmplitude(0.0).stopBandRipple(0.005)
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
        mResampler.setListener(NBFMDecoder.this::broadcast);

        // === NEW: Connect squelch tail remover to resampler ===
        if(mSquelchTailRemovalEnabled && mSquelchTailRemover != null)
        {
            mSquelchTailRemover.setOutputListener(audio -> mResampler.resample(audio));
        }
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
