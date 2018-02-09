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
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter_CB_CB;
import io.github.dsheirer.dsp.fm.FMDemodulator_CB;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.reusable.IReusableComplexBufferListener;
import io.github.dsheirer.sample.complex.reusable.ReusableComplexBuffer;
import io.github.dsheirer.sample.real.IUnFilteredRealBufferProvider;
import io.github.dsheirer.sample.real.RealBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FMDemodulatorModule extends Module implements ISourceEventListener, IReusableComplexBufferListener,
    IUnFilteredRealBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(FMDemodulatorModule.class);

    private ComplexFIRFilter_CB_CB mIQFilter;
    private FMDemodulator_CB mDemodulator;
    private ReusableBufferProcessor mReusableBufferProcessor = new ReusableBufferProcessor();
    private SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private int mPassFrequency;
    private int mStopFrequency;

    /**
     * FM Demodulator with I/Q filter.  Demodulated output is unfiltered and may contain a DC component.
     *
     * @param pass - pass frequency for IQ filtering prior to demodulation.  This
     * frequency should be less than or equal to half of the signal bandwidth since the filter will
     * be applied against each of the inphase and quadrature signals and the
     * combined pass bandwidth will be twice this value.
     */
    public FMDemodulatorModule(int pass, int stop)
    {
        mDemodulator = new FMDemodulator_CB(1.0f);
        mPassFrequency = pass;
        mStopFrequency = stop;
    }

    @Override
    public Listener<ReusableComplexBuffer> getReusableComplexBufferListener()
    {
        return mReusableBufferProcessor;
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventProcessor;
    }

    @Override
    public void dispose()
    {
        if(mIQFilter != null)
        {
            mIQFilter.dispose();
            mIQFilter = null;
        }

        mDemodulator.dispose();
        mDemodulator = null;
    }

    @Override
    public void reset()
    {
        mDemodulator.reset();
    }

    @Override
    public void setUnFilteredRealBufferListener(Listener<RealBuffer> listener)
    {
        mDemodulator.setListener(listener);
    }

    @Override
    public void removeUnFilteredRealBufferListener()
    {
        mDemodulator.removeListener();
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
     * Receives complex buffers and sends them through the filtering and demodulation process.  Manages accounting for
     * the reusable buffers.
     */
    public class ReusableBufferProcessor implements Listener<ReusableComplexBuffer>
    {
        @Override
        public void receive(ReusableComplexBuffer reusableComplexBuffer)
        {
            if(mIQFilter == null)
            {
                reusableComplexBuffer.decrementUserCount();
                throw new IllegalStateException("FM demodulator module must receive a sample rate change source " +
                    "event before it can process complex sample buffers");
            }

            float[] samples = reusableComplexBuffer.getSamplesCopy();

            mIQFilter.receive(samples);

            reusableComplexBuffer.decrementUserCount();
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
                double sampleRate = sourceEvent.getValue().doubleValue();

                float[] filterTaps = null;

                FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                    .sampleRate(sampleRate)
                    .gridDensity(16)
                    .oddLength(true)
                    .passBandCutoff(mPassFrequency)
                    .passBandAmplitude(1.0)
                    .passBandRipple(0.01)
                    .stopBandStart(mStopFrequency)
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
                        "] pass frequency [" + mPassFrequency + "] and stop frequency [" + mStopFrequency +
                        "] - using sinc filter");
                }

                if(filterTaps == null)
                {
                    filterTaps = FilterFactory.getLowPass(sampleRate, mPassFrequency, mStopFrequency, 60,
                        Window.WindowType.HAMMING, true);
                }

                mIQFilter = new ComplexFIRFilter_CB_CB(filterTaps, 1.0f);
                mIQFilter.setListener(mDemodulator);
            }
        }
    }
}
