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
import io.github.dsheirer.dsp.gain.ComplexFeedForwardGainControl;
import io.github.dsheirer.dsp.psk.DQPSKSymbolPhaseErrorCalculator;
import io.github.dsheirer.dsp.psk.IQPSKSymbolDecoder;
import io.github.dsheirer.dsp.psk.ISymbolPhaseErrorCalculator;
import io.github.dsheirer.dsp.psk.QPSKDemodulator;
import io.github.dsheirer.dsp.psk.QPSKSymbolDecoder;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.IPhaseLockedLoop;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.Complex;
import io.github.dsheirer.sample.complex.ComplexBuffer;
import io.github.dsheirer.sample.complex.ComplexBufferToStreamConverter;
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
    private static final double SYMBOL_RATE = 4800;
    private double mSampleRate;

    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    private ComplexFIRFilter mBasebandFilter;
    private QPSKDemodulator mQPSKDemodulator;
    private IPhaseLockedLoop mPhaseLockedLoop;
    private ComplexBufferToStreamConverter mStreamConverter = new ComplexBufferToStreamConverter();
    private ComplexFeedForwardGainControl mAGC = new ComplexFeedForwardGainControl(32);
    private QPSKSymbolDecoder mQPSKSlicer = new QPSKSymbolDecoder();
    private P25MessageFramer mMessageFramer;
    private ISymbolPhaseErrorCalculator mSymbolPhaseErrorCalculator = new DQPSKSymbolPhaseErrorCalculator();
    private IQPSKSymbolDecoder mSymbolDecoder = new QPSKSymbolDecoder();
    private Listener<Complex> mSymbolListener;
    private Listener<Double> mPLLPhaseErrorListener;

    public P25_C4FMDecoder2(AliasList aliasList)
    {
        super(aliasList);

        setSampleRate(48000.0);

        mStreamConverter.setListener(mAGC);
    }

    public void setSampleRate(double sampleRate)
    {
        if(sampleRate <= SYMBOL_RATE * 2)
        {
            throw new IllegalArgumentException("Sample rate must be at least twice the symbol rate [4800]");
        }

        mSampleRate = sampleRate;
        mBasebandFilter = new ComplexFIRFilter(getBasebandFilter(), 1.0f);

        mPhaseLockedLoop = new CostasLoop(mSampleRate, SYMBOL_RATE);
        mQPSKDemodulator = new QPSKDemodulator(mPhaseLockedLoop, mSymbolPhaseErrorCalculator, mSymbolDecoder,
            mSampleRate, SYMBOL_RATE);
        mQPSKDemodulator.setSymbolListener(mSymbolListener);
        mQPSKDemodulator.setPLLErrorListener(mPLLPhaseErrorListener);

        //Message framer can trigger a symbol-inversion correction to the PLL when detected
        mMessageFramer = new P25MessageFramer(getAliasList(), mPhaseLockedLoop);
        mMessageFramer.setListener(getMessageProcessor());
        mQPSKDemodulator.setDibitListener(mMessageFramer);
    }

    public void setSymbolListener(Listener<Complex> listener)
    {
        mSymbolListener = listener;
        mQPSKDemodulator.setSymbolListener(listener);
    }

    public void setPLLPhaseErrorListener(Listener<Double> listener)
    {
        mPLLPhaseErrorListener = listener;
        mQPSKDemodulator.setPLLErrorListener(listener);
    }

    @Override
    public void receive(ComplexBuffer complexBuffer)
    {
        ComplexBuffer filtered = mBasebandFilter.filter(complexBuffer);
        ComplexBuffer amplified = mAGC.filter(filtered);
        mQPSKDemodulator.receive(amplified);
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
                .passBandCutoff(6000)
                .passBandAmplitude(1.0)
                .passBandRipple(0.1)
                .stopBandAmplitude(0.0)
                .stopBandStart(6250)
                .stopBandRipple(0.01)
                .build();

            try
            {
                filter = FilterFactory.getTaps(specification);
                mBasebandFilters.put(getSampleRate(), filter);

            }
            catch(FilterDesignException fde)
            {
                mLog.error("Couldn't design low pass baseband filter for sample rate: " + getSampleRate());
            }
        }

        return filter;
    }

    public void dispose()
    {
        super.dispose();

        mBasebandFilter.dispose();
        mBasebandFilter = null;

        mStreamConverter.dispose();
        mStreamConverter = null;

        mAGC.dispose();
        mAGC = null;

        mQPSKSlicer.dispose();
        mQPSKSlicer = null;

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
                //Ignored
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
        // TODO Auto-generated method stub

    }

    @Override
    public void start(ScheduledExecutorService executor)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop()
    {
        // TODO Auto-generated method stub
    }
}
