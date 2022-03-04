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
package io.github.dsheirer.module.demodulate.fm;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.dsp.fm.ScalarFMDemodulator;
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

    private IRealFilter mIBasebandFilter;
    private IRealFilter mQBasebandFilter;
    private ScalarFMDemodulator mDemodulator = new ScalarFMDemodulator();
    private PowerMonitor mPowerMonitor = new PowerMonitor();
    private RealResampler mResampler;
    private SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private Listener<float[]> mResampledBufferListener;
    private double mChannelBandwidth;
    private double mOutputSampleRate;

    /**
     * Creates an FM demodulator for the specified channel bandwidth and output sample rate.
     *
     * A baseband I/Q filter will be constructed to low pass filter the incoming sample buffer stream to the desired
     * channel bandwidth and the demodulated output will be resampled to the desired output sample rate.  The input
     * low pass filter is constructed at runtime based on receiving a sample rate notification source event.
     */
    public FMDemodulatorModule(double channelBandwidth, double outputSampleRate)
    {
        mChannelBandwidth = channelBandwidth;
        mOutputSampleRate = outputSampleRate;
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
        mDemodulator.dispose();
        mDemodulator = null;
    }

    @Override
    public void reset()
    {
        mDemodulator.reset();
    }

    @Override
    public void start()
    {
    }

    @Override
    public void stop()
    {
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
    public void receive(ComplexSamples samples)
    {
        if(mIBasebandFilter == null || mQBasebandFilter == null)
        {
            throw new IllegalStateException("FM demodulator module must receive a sample rate change source " +
                    "event before it can process complex sample buffers");
        }

        float[] i = mIBasebandFilter.filter(samples.i());
        float[] q = mQBasebandFilter.filter(samples.q());

        mPowerMonitor.process(i, q);
        float[] demodulated = mDemodulator.demodulate(i, q);

        if(mResampler != null)
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

                if((sampleRate < (2.0 * mChannelBandwidth)))
                {
                    throw new IllegalStateException("FM Demodulator with channel bandwidth [" + mChannelBandwidth +
                            "] requires a channel sample rate of [" + (2.0 * mChannelBandwidth + "] - sample rate of [" +
                            sampleRate + "] is not supported"));
                }

                mPowerMonitor.setSampleRate((int)sampleRate);

                double cutoff = sampleRate / 4.0;
                int passBandStop = (int)cutoff - 500;
                int stopBandStart = (int)cutoff + 500;

                float[] coefficients = null;

                FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                        .sampleRate(sampleRate)
                        .gridDensity(16)
                        .oddLength(true)
                        .passBandCutoff(passBandStop)
                        .passBandAmplitude(1.0)
                        .passBandRipple(0.01)
                        .stopBandStart(stopBandStart)
                        .stopBandAmplitude(0.0)
                        .stopBandRipple(0.028) //Approximately 60 dB attenuation
                        .build();

                try
                {
                    coefficients = FilterFactory.getTaps(specification);
                }
                catch(FilterDesignException fde)
                {
                    mLog.error("Couldn't design FM demodulator remez filter for sample rate [" + sampleRate +
                            "] pass frequency [" + passBandStop + "] and stop frequency [" + stopBandStart +
                            "] - using sinc filter");
                }

                if(coefficients == null)
                {
                    coefficients = FilterFactory.getLowPass(sampleRate, passBandStop, stopBandStart, 60,
                            WindowType.HAMMING, true);
                }

                mIBasebandFilter = FilterFactory.getRealFilter(coefficients);
                mQBasebandFilter = FilterFactory.getRealFilter(coefficients);
                mResampler = new RealResampler(sampleRate, mOutputSampleRate, 8192, 512);

                mResampler.setListener(resampledBuffer -> {
                    if(mResampledBufferListener != null)
                    {
                        mResampledBufferListener.receive(resampledBuffer);
                    }
                });
            }
        }
    }
}
