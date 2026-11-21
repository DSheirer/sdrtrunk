/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.module.demodulate.fm;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IRealDecimationFilter;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.equalizer.GraphicEqualizer;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.dsp.fm.IDemodulator;
import io.github.dsheirer.dsp.squelch.NoiseSquelch;
import io.github.dsheirer.dsp.squelch.PowerMonitor;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.module.Module;
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
 * FM Demodulator with integrated fractional resampler.
 *
 * Note: no filtering is applied to the demodulated audio.
 */
public class FMDemodulatorModule extends Module implements ISourceEventListener, ISourceEventProvider,
        IComplexSamplesListener, Listener<ComplexSamples>, IRealBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(FMDemodulatorModule.class);
    private static final double DEMODULATED_AUDIO_SAMPLE_RATE = 8000.0;
    private IRealFilter mIBasebandFilter;
    private IRealFilter mQBasebandFilter;
    private IRealDecimationFilter mIDecimationFilter;
    private IRealDecimationFilter mQDecimationFilter;
    private IDemodulator mDemodulator = FmDemodulatorFactory.getFmDemodulator();
    private PowerMonitor mPowerMonitor = new PowerMonitor();
    private RealResampler mResampler;
    private SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private Listener<float[]> mResampledBufferListener;
    private double mChannelBandwidth;
    private NoiseSquelch mNoiseSquelch;
    private GraphicEqualizer mGraphicEqualizer;
    private IRealFilter mOutputFilter;
    private boolean mCarrierSquelch;
    private double mCarrierReferenceDb = Double.NEGATIVE_INFINITY;
    private double mDecimatedSampleRate;

    /** Carrier squelch mutes when the carrier is this far below the call's carrier level */
    public static final double CARRIER_DROP_DB = 20.0;
    /** How fast the carrier reference follows a weakening signal */
    private static final double CARRIER_REFERENCE_DECAY_DB_PER_SECOND = 3.0;

    /**
     * Creates an FM demodulator for the specified channel bandwidth and output sample rate.
     *
     * A baseband I/Q filter will be constructed to low pass filter the incoming sample buffer stream to the desired
     * channel bandwidth and the demodulated output will be resampled to the desired output sample rate.  The input
     * low pass filter is constructed at runtime based on receiving a sample rate notification source event.
     */
    public FMDemodulatorModule(double channelBandwidth)
    {
        mChannelBandwidth = channelBandwidth;
    }

    /**
     * FM demodulator whose audio passes through a noise squelch before resampling, so noise
     * (no carrier) is dropped and only received audio is broadcast.  Used on trunked voice channels, which stay
     * open a few seconds after the transmitter drops.
     *
     * @param channelBandwidth as above
     * @param noiseSquelch applied to the demodulated audio
     */
    public FMDemodulatorModule(double channelBandwidth, NoiseSquelch noiseSquelch)
    {
        this(channelBandwidth);
        mNoiseSquelch = noiseSquelch;
    }

    /**
     * Broadcasts the demodulated audio samples to the registered listener.
     * @param demodulatedSamples to broadcast
     */
    protected void broadcast(float[] demodulatedSamples)
    {
        IRealFilter outputFilter = mOutputFilter;

        if(outputFilter != null)
        {
            demodulatedSamples = outputFilter.filter(demodulatedSamples);
        }

        GraphicEqualizer equalizer = mGraphicEqualizer;

        if(equalizer != null && equalizer.isEnabled())
        {
            demodulatedSamples = equalizer.process(demodulatedSamples);
        }

        if(mResampledBufferListener != null)
        {
            mResampledBufferListener.receive(demodulatedSamples);
        }
    }

    /**
     * Filter applied to the 8 kHz audio before the equalizer, or null for none
     */
    public void setOutputFilter(IRealFilter filter)
    {
        mOutputFilter = filter;
    }

    /**
     * Carrier squelch for trunked voice channels.  The channel is only open while a call is on it,
     * so the audio is dropped whenever the received carrier falls CARRIER_DROP_DB below the call's carrier level - the
     * transmitter has stopped.  Works with the noise squelch: audio passes only when both are open.
     */
    public void setCarrierSquelch(boolean enabled)
    {
        mCarrierSquelch = enabled;
    }

    /**
     * Carrier squelch decision for one block of filtered samples - true to drop the block.  Package-private for testing.
     */
    boolean carrierSquelched(float[] i, float[] q)
    {
        if(!mCarrierSquelch || i.length == 0)
        {
            return false;
        }

        double power = 0;

        for(int x = 0; x < i.length; x++)
        {
            power += i[x] * i[x] + q[x] * q[x];
        }

        double db = 10 * Math.log10(power / i.length + 1e-20);
        double seconds = mDecimatedSampleRate > 0 ? i.length / mDecimatedSampleRate : 0.01;
        mCarrierReferenceDb = Math.max(db, mCarrierReferenceDb - CARRIER_REFERENCE_DECAY_DB_PER_SECOND * seconds);
        return db < mCarrierReferenceDb - CARRIER_DROP_DB;
    }

    /**
     * Graphic equalizer applied to the 8 kHz demodulated audio, or null for none
     */
    public void setGraphicEqualizer(GraphicEqualizer equalizer)
    {
        mGraphicEqualizer = equalizer;
    }

    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventProcessor;
    }

    @Override
    public void dispose()
    {
        mDemodulator = null;
    }

    @Override
    public void reset() {}
    @Override
    public void start() {}
    @Override
    public void stop() {}

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
    public void receive(ComplexSamples samples)
    {
        if(mIBasebandFilter == null || mIDecimationFilter == null)
        {
            throw new IllegalStateException("FM demodulator module must receive a sample rate change source " +
                    "event before it can process complex sample buffers");
        }

        float[] decimatedI = mIDecimationFilter.decimateReal(samples.i());
        float[] decimatedQ = mQDecimationFilter.decimateReal(samples.q());
        float[] filteredI = mIBasebandFilter.filter(decimatedI);
        float[] filteredQ = mQBasebandFilter.filter(decimatedQ);
        float[] demodulated = mDemodulator.demodulate(filteredI, filteredQ);
        mPowerMonitor.process(filteredI, filteredQ);

        if(carrierSquelched(filteredI, filteredQ))
        {
            return; //Carrier gone - the transmission is over
        }

        if(mNoiseSquelch != null)
        {
            mNoiseSquelch.process(demodulated);
        }
        else if(mResampler != null)
        {
            mResampler.resample(demodulated);
        }
    }

    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mPowerMonitor.setSourceEventListener(listener);
    }

    @Override
    public void removeSourceEventListener()
    {
        mPowerMonitor.setSourceEventListener(null);
    }

    /**
     * Monitors sample rate change source event(s) to setup the initial I/Q filter
     */
    public class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
            {
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

                mDecimatedSampleRate = decimatedSampleRate;
                mIDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimationRate);
                mQDecimationFilter = DecimationFilterFactory.getRealDecimationFilter(decimationRate);

                if((decimatedSampleRate < (2.0 * mChannelBandwidth)))
                {
                    throw new IllegalStateException("FM demodulator with channel bandwidth [" +
                            mChannelBandwidth + "] requires a channel sample rate of [" + (2.0 * mChannelBandwidth +
                            "] - sample rate of [" + decimatedSampleRate + "] is not supported"));
                }

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
                        .stopBandRipple(0.005) //Approximately 90 dB attenuation
                        .build();

                try
                {
                    coefficients = FilterFactory.getTaps(specification);
                }
                catch(FilterDesignException fde)
                {
                    mLog.error("Couldn't design demodulator remez filter for sample rate [" + sampleRate +
                            "] pass frequency [" + passBandStop + "] and stop frequency [" + stopBandStart +
                            "] - will proceed using sinc (low-pass) filter");
                }

                if(coefficients == null)
                {
                    mLog.info("Unable to use remez filter designer for sample rate [" + decimatedSampleRate +
                            "] pass band stop [" + passBandStop +
                            "] and stop band start [" + stopBandStart + "] - will proceed using simple low pass filter design");
                    coefficients = FilterFactory.getLowPass(decimatedSampleRate, passBandStop, stopBandStart, 60,
                            WindowType.HAMMING, true);
                }

                mIBasebandFilter = FilterFactory.getRealFilter(coefficients);
                mQBasebandFilter = FilterFactory.getRealFilter(coefficients);

                mResampler = new RealResampler(decimatedSampleRate, DEMODULATED_AUDIO_SAMPLE_RATE, 4192, 512);
                mResampler.setListener(resampled -> broadcast(resampled));

                if(mNoiseSquelch != null)
                {
                    final RealResampler resampler = mResampler;
                    mNoiseSquelch.setSampleRate(decimatedSampleRate);
                    mNoiseSquelch.setAudioListener(audio -> resampler.resample(audio));
                }
            }
        }
    }
}
