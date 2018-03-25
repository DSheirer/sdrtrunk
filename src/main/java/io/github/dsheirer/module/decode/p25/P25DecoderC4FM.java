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
import io.github.dsheirer.dsp.psk.DQPSKDecisionDirectedDemodulator;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBuffer;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class P25DecoderC4FM extends P25Decoder
{
    private final static Logger mLog = LoggerFactory.getLogger(P25DecoderC4FM.class);
    protected static final float SAMPLE_COUNTER_GAIN = 0.5f;
    protected InterpolatingSampleBuffer mInterpolatingSampleBuffer;
    protected DQPSKDecisionDirectedDemodulator mQPSKDemodulator;
    protected CostasLoop mCostasLoop;
    protected P25MessageFramer mMessageFramer;
    private ComplexFeedForwardGainControl mAGC = new ComplexFeedForwardGainControl(32);
    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    private ComplexFIRFilter mBasebandFilter;

    /**
     * P25 Phase 1 - standard C4FM modulation decoder.  Uses Differential QPSK decoding with a Costas PLL and a
     * decision-directed phase and timing error detector.
     *
     * @param aliasList
     */
    public P25DecoderC4FM(AliasList aliasList)
    {
        super(4800.0, aliasList);
        setSampleRate(48000.0);
    }

    public void setSampleRate(double sampleRate)
    {
        super.setSampleRate(sampleRate);

        mBasebandFilter = new ComplexFIRFilter(getBasebandFilter(), 1.0f);

        mCostasLoop = new CostasLoop(getSampleRate(), getSymbolRate());
        mInterpolatingSampleBuffer = new InterpolatingSampleBuffer(getSamplesPerSymbol(), SAMPLE_COUNTER_GAIN);

        mQPSKDemodulator = new DQPSKDecisionDirectedDemodulator(mCostasLoop, mInterpolatingSampleBuffer);

        //Message framer can issue a symbol-inversion correction request to the PLL when detected
        mMessageFramer = new P25MessageFramer(getAliasList(), mCostasLoop);
        mMessageFramer.setListener(getMessageProcessor());
        mQPSKDemodulator.setSymbolListener(mMessageFramer);
    }

    /**
     * Primary method for processing incoming complex sample buffers
     * @param reusableComplexBuffer containing channelized complex samples
     */
    @Override
    public void receive(ReusableComplexBuffer reusableComplexBuffer)
    {
        //User accounting of the incoming buffer is handled by the filter
        ReusableComplexBuffer basebandFiltered = filter(reusableComplexBuffer);

        //User accounting of the incoming buffer is handled by the gain filter
        ReusableComplexBuffer gainApplied = mAGC.filter(basebandFiltered);

        //User accounting of the filtered buffer is handled by the demodulator
        mQPSKDemodulator.receive(gainApplied);
    }

    /**
     * Filters the complex buffer and returns a new reusable complex buffer with the filtered contents.
     * @param reusableComplexBuffer to filter
     * @return filtered complex buffer
     */
    protected ReusableComplexBuffer filter(ReusableComplexBuffer reusableComplexBuffer)
    {
        //User accounting of the incoming buffer is handled by the filter
        return mBasebandFilter.filter(reusableComplexBuffer);
    }

    /**
     * Constructs a baseband filter for this decoder using the current sample rate
     */
    private float[] getBasebandFilter()
    {
        //Attempt to reuse a cached (ie already-designed) filter if available
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
                throw new IllegalStateException("Couldn't design a C4FM baseband filter for sample rate: " + getSampleRate());
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
    protected void process(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_FREQUENCY_CHANGE:
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
            case NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_CHANGE:
                mCostasLoop.reset();
                break;
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                mCostasLoop.reset();
                setSampleRate(sourceEvent.getValue().doubleValue());
                break;
        }
    }

    /**
     * P25 modulation supported by this decoder
     */
    public Modulation getModulation()
    {
        return Modulation.C4FM;
    }

    /**
     * Resets this decoder to prepare for processing a new channel
     */
    @Override
    public void reset()
    {
        mCostasLoop.reset();
    }
}
