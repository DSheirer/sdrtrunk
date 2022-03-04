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
package io.github.dsheirer.module.demodulate.am;

import io.github.dsheirer.dsp.am.AMDemodulator;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.dsp.gain.AutomaticGainControl;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.sample.real.IRealBufferProvider;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AM Demodulator Module for demodulating complex baseband channel sample buffers and producing demodulated audio
 * at a specified output sample rate, normally 8 kHz.
 * <p>
 * This module requires a sample rate SourceEvent prior to processing baseband sample buffers in order to configure
 * internal filters and the resampler.
 */
public class AMDemodulatorModule extends Module
        implements ISourceEventListener, IComplexSamplesListener, IRealBufferProvider, Listener<ComplexSamples>
{
    private final static Logger mLog = LoggerFactory.getLogger(AMDemodulatorModule.class);
    private IRealFilter mIBasebandFilter;
    private IRealFilter mQBasebandFilter;

    private AMDemodulator mDemodulator;
    private IRealFilter mLowPassFilter;
    private AutomaticGainControl mAGC = new AutomaticGainControl();
    private double mChannelBandwidth;
    private double mOutputSampleRate;
    private RealResampler mResampler;
    private Listener<float[]> mResampledReusableBufferListener;
    private SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();

    /**
     * Constructs an AM demodulator module
     *
     * @param channelBandwidth to use in filtering the baseband input buffers
     * @param outputSampleRate specifies the resampled output sample rate for demodulated audio
     */
    public AMDemodulatorModule(double channelBandwidth, double outputSampleRate)
    {
        mChannelBandwidth = channelBandwidth;
        mOutputSampleRate = outputSampleRate;

        mDemodulator = new AMDemodulator(500.0f);

        float[] lowPassCoefficients = null;

        FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder().sampleRate(mOutputSampleRate)
                .gridDensity(16).oddLength(true).passBandCutoff(3000).passBandAmplitude(1.0).passBandRipple(0.01)
                .stopBandStart(3500).stopBandAmplitude(0.0).stopBandRipple(0.028).build();

        try
        {
            lowPassCoefficients = FilterFactory.getTaps(specification);
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Couldn't design AM demodulator remez filter for sample rate [" + mOutputSampleRate
                    + "] pass frequency [3000] and stop frequency [3500] - using sinc filter");
        }

        if(lowPassCoefficients == null)
        {
            lowPassCoefficients = FilterFactory.getLowPass(mOutputSampleRate, 3000, 3500, 60, WindowType.HAMMING, true);
        }

        mLowPassFilter = FilterFactory.getRealFilter(lowPassCoefficients);
    }

    @Override public void receive(ComplexSamples samples)
    {
        float[] i = mIBasebandFilter.filter(samples.i());
        float[] q = mQBasebandFilter.filter(samples.q());
        float[] demodulated = mDemodulator.demodulate(i, q);
        mResampler.resample(demodulated);
    }

    /**
     * Processes resampled audio buffers produced by the resampler.
     *
     * @param resampledAudio buffer to process.
     */
    private void processResampledAudio(float[] resampledAudio)
    {
        if(mResampledReusableBufferListener != null)
        {
            float[] filtered = mLowPassFilter.filter(resampledAudio);
            float[] amplified = mAGC.process(filtered);
            mResampledReusableBufferListener.receive(amplified);
        }
    }

    @Override public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return this;
    }

    @Override public void reset()
    {
    }

    @Override public void start()
    {
    }

    @Override public void stop()
    {
    }

    @Override public void setBufferListener(Listener<float[]> listener)
    {
        mResampledReusableBufferListener = listener;
    }

    @Override public void removeBufferListener()
    {
        mResampledReusableBufferListener = null;
    }

    @Override public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventProcessor;
    }

    /**
     * Monitors sample rate change source event(s) to setup the initial I/Q filter and resampler
     */
    public class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
            {
                if(mIBasebandFilter != null || mQBasebandFilter != null)
                {
                    mIBasebandFilter = null;
                    mQBasebandFilter = null;
                }

                double sampleRate = sourceEvent.getValue().doubleValue();

                if((sampleRate < (2.0 * mChannelBandwidth)))
                {
                    throw new IllegalStateException("AM Demodulator with channel bandwidth [" + mChannelBandwidth
                            + "] requires a channel sample rate of [" + (2.0 * mChannelBandwidth
                            + "] - sample rate of [" + sampleRate + "] is not supported"));
                }

                double cutoff = sampleRate / 4.0;
                int passBandStop = (int)cutoff - 500;
                int stopBandStart = (int)cutoff + 500;

                float[] coefficients = null;

                FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder().sampleRate(sampleRate)
                        .gridDensity(16).oddLength(true).passBandCutoff(passBandStop).passBandAmplitude(1.0)
                        .passBandRipple(0.01).stopBandStart(stopBandStart).stopBandAmplitude(0.0)
                        .stopBandRipple(0.028) //Approximately 60 dB attenuation
                        .build();

                try
                {
                    coefficients = FilterFactory.getTaps(specification);
                }
                catch(FilterDesignException fde)
                {
                    mLog.error("Couldn't design AM demodulator remez filter for sample rate [" + sampleRate
                            + "] pass frequency [" + passBandStop + "] and stop frequency [" + stopBandStart
                            + "] - using sinc filter");
                }

                if(coefficients == null)
                {
                    coefficients = FilterFactory.getLowPass(sampleRate, passBandStop, stopBandStart, 60,
                            WindowType.HAMMING, true);
                }

                mIBasebandFilter = FilterFactory.getRealFilter(coefficients);
                mQBasebandFilter = FilterFactory.getRealFilter(coefficients);
                mResampler = new RealResampler(sampleRate, mOutputSampleRate, 8192, 512);
                mResampler.setListener(resampledBuffer -> processResampledAudio(resampledBuffer));
            }
        }
    }
}
