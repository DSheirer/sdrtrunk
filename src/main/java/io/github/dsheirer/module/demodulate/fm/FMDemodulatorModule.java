/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.demodulate.fm;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter2;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.dsp.fm.FMDemodulator;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableBufferProvider;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferListener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FM Demodulator with integrated fractional resampler.
 *
 * Note: no filtering is applied to the demodulated audio.
 */
public class FMDemodulatorModule extends Module implements ISourceEventListener, IReusableComplexBufferListener,
    Listener<ReusableComplexBuffer>, IReusableBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(FMDemodulatorModule.class);

    private ComplexFIRFilter2 mIQFilter;
    private FMDemodulator mDemodulator = new FMDemodulator();
    private RealResampler mResampler;
    private SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private Listener<ReusableFloatBuffer> mResampledReusableBufferListener;
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
    public Listener<ReusableComplexBuffer> getReusableComplexBufferListener()
    {
        return this;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventProcessor;
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
    public void setBufferListener(Listener<ReusableFloatBuffer> listener)
    {
        mResampledReusableBufferListener = listener;
    }

    @Override
    public void removeBufferListener()
    {
        mResampledReusableBufferListener = null;
    }

    @Override
    public void receive(ReusableComplexBuffer reusableComplexBuffer)
    {
        if(mIQFilter == null)
        {
            reusableComplexBuffer.decrementUserCount();
            throw new IllegalStateException("FM demodulator module must receive a sample rate change source " +
                "event before it can process complex sample buffers");
        }

        ReusableComplexBuffer basebandFilteredBuffer = mIQFilter.filter(reusableComplexBuffer);
        ReusableFloatBuffer demodulatedBuffer = mDemodulator.demodulate(basebandFilteredBuffer);

        if(mResampler != null)
        {
            mResampler.resample(demodulatedBuffer);
        }
        else
        {
            demodulatedBuffer.decrementUserCount();
        }
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
                if(mIQFilter != null)
                {
                    mIQFilter.dispose();
                    mIQFilter = null;
                }

                double sampleRate = sourceEvent.getValue().doubleValue();

                if((sampleRate < (2.0 * mChannelBandwidth)))
                {
                    throw new IllegalStateException("FM Demodulator with channel bandwidth [" + mChannelBandwidth +
                        "] requires a channel sample rate of [" + (2.0 * mChannelBandwidth + "] - sample rate of [" +
                        sampleRate + "] is not supported"));
                }

                double cutoff = sampleRate / 4.0;
                int passBandStop = (int)cutoff - 500;
                int stopBandStart = (int)cutoff + 500;

                float[] filterTaps = null;

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
                    filterTaps = FilterFactory.getTaps(specification);
                }
                catch(FilterDesignException fde)
                {
                    mLog.error("Couldn't design FM demodulator remez filter for sample rate [" + sampleRate +
                        "] pass frequency [" + passBandStop + "] and stop frequency [" + stopBandStart +
                        "] - using sinc filter");
                }

                if(filterTaps == null)
                {
                    filterTaps = FilterFactory.getLowPass(sampleRate, passBandStop, stopBandStart, 60,
                        Window.WindowType.HAMMING, true);
                }

                mIQFilter = new ComplexFIRFilter2(filterTaps);

                mResampler = new RealResampler(sampleRate, mOutputSampleRate, 2000, 1000);

                mResampler.setListener(new Listener<ReusableFloatBuffer>()
                {
                    @Override
                    public void receive(ReusableFloatBuffer reusableFloatBuffer)
                    {
                        if(mResampledReusableBufferListener != null)
                        {
                            mResampledReusableBufferListener.receive(reusableFloatBuffer);
                        }
                        else
                        {
                            reusableFloatBuffer.decrementUserCount();
                        }
                    }
                });
            }
        }
    }
}
