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
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.dsp.fm.FmDemodulatorFactory;
import io.github.dsheirer.dsp.fm.IDemodulator;
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
     * Broadcasts the demodulated audio samples to the registered listener.
     * @param demodulatedSamples to broadcast
     */
    protected void broadcast(float[] demodulatedSamples)
    {
        if(mResampledBufferListener != null)
        {
            mResampledBufferListener.receive(demodulatedSamples);
        }
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
            }
        }
    }
}
