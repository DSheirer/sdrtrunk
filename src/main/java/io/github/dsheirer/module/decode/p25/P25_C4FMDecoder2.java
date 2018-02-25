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
package io.github.dsheirer.module.decode.p25;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter;
import io.github.dsheirer.dsp.psk.DQPSKDemodulator;
import io.github.dsheirer.dsp.psk.DQPSKSymbolPhaseErrorCalculator;
import io.github.dsheirer.dsp.psk.IQPSKSymbolDecoder;
import io.github.dsheirer.dsp.psk.ISymbolPhaseErrorCalculator;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBuffer;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBufferInstrumented;
import io.github.dsheirer.dsp.psk.QPSKStarSlicer;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.IComplexBufferListener;
import io.github.dsheirer.source.tuner.frequency.FrequencyChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

public class P25_C4FMDecoder2 extends P25Decoder implements IComplexBufferListener, Listener<ComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(P25_C4FMDecoder2.class);
    protected static final double SYMBOL_RATE = 4800;
    protected ISymbolPhaseErrorCalculator mSymbolPhaseErrorCalculator = new DQPSKSymbolPhaseErrorCalculator();
    protected IQPSKSymbolDecoder mSymbolDecoder = new QPSKStarSlicer();
    protected InterpolatingSampleBuffer mInterpolatingSampleBuffer;
    protected DQPSKDemodulator mQPSKDemodulator;
    protected CostasLoop mCostasLoop;
    protected double mSampleRate;
    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    private ComplexFIRFilter mBasebandFilter;
    private P25MessageFramer mMessageFramer;

    public P25_C4FMDecoder2(AliasList aliasList)
    {
        super(aliasList);
        setSampleRate(48000.0);
    }

    public void setSampleRate(double sampleRate)
    {
        if(sampleRate <= SYMBOL_RATE * 2)
        {
            throw new IllegalArgumentException("Sample rate must be at least twice the symbol rate [4800]");
        }

        mSampleRate = sampleRate;
        mBasebandFilter = new ComplexFIRFilter(getBasebandFilter(), 1.0f);

        mCostasLoop = new CostasLoop(mSampleRate, SYMBOL_RATE);

        mInterpolatingSampleBuffer = new InterpolatingSampleBufferInstrumented((float)(sampleRate / SYMBOL_RATE));
        mQPSKDemodulator = new DQPSKDemodulator(mCostasLoop, mSymbolPhaseErrorCalculator, mSymbolDecoder,
            mInterpolatingSampleBuffer);

        //Message framer can trigger a symbol-inversion correction to the PLL when detected
        mMessageFramer = new P25MessageFramer(getAliasList(), mCostasLoop);
        mMessageFramer.setListener(getMessageProcessor());
        mQPSKDemodulator.setDibitListener(mMessageFramer);
    }

    @Override
    public void receive(ComplexBuffer complexBuffer)
    {
        ComplexBuffer filtered = filter(complexBuffer);
        mQPSKDemodulator.receive(filtered);
    }

    protected ComplexBuffer filter(ComplexBuffer complexBuffer)
    {
        return mBasebandFilter.filter(complexBuffer);
    }

    private double getSampleRate()
    {
        return mSampleRate;
    }

    private float[] getBasebandFilter()
    {
        float[] filter = mBasebandFilters.get(getSampleRate());

        if(filter == null)
        {
            FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
                .sampleRate((int)getSampleRate())
                .passBandCutoff(5100)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandAmplitude(0.0)
                .stopBandStart(6500)
                .stopBandRipple(0.01)
                .build();

            try
            {
                filter = FilterFactory.getTaps(specification);

            }
            catch(FilterDesignException fde)
            {
                mLog.error("Couldn't design low pass baseband filter for sample rate: " + getSampleRate());
            }
            if(filter != null)
            {
                mBasebandFilters.put(getSampleRate(), filter);
            }
            else
            {
                throw new IllegalStateException("Couldn't design a C4FM symbol filter for sample rate: " + mSampleRate);
            }
        }

        return filter;
    }

    public void dispose()
    {
        super.dispose();

        mBasebandFilter.dispose();
        mBasebandFilter = null;

        mMessageFramer.dispose();
        mMessageFramer = null;
    }

    @Override
    public void setFrequencyChangeListener(Listener<FrequencyChangeEvent> listener)
    {
    }

    @Override
    public void removeFrequencyChangeListener()
    {
    }

    @Override
    public Listener<FrequencyChangeEvent> getFrequencyChangeListener()
    {
        return new Listener<FrequencyChangeEvent>()
        {
            @Override
            public void receive(FrequencyChangeEvent frequencyChangeEvent)
            {
                switch(frequencyChangeEvent.getEvent())
                {
                    case NOTIFICATION_FREQUENCY_CHANGE:
                    case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                    case NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE:
                    case NOTIFICATION_SAMPLE_RATE_CHANGE:
                        mCostasLoop.reset();
                        break;
                }
            }
        };
    }

    @Override
    public Listener<ComplexBuffer> getComplexBufferListener()
    {
        return P25_C4FMDecoder2.this;
    }

    public Modulation getModulation()
    {
        return Modulation.C4FM;
    }

    @Override
    public void reset()
    {
        mCostasLoop.reset();
    }

    @Override
    public void start(ScheduledExecutorService executor)
    {
    }

    @Override
    public void stop()
    {
    }
}
