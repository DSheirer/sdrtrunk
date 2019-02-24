/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter2;
import io.github.dsheirer.dsp.gain.ComplexFeedForwardGainControl;
import io.github.dsheirer.dsp.psk.DQPSKDecisionDirectedDemodulator;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBuffer;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.PLLGain;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * P25 Phase 2 HDQPSK 2-timeslot Decoder
 */
public class P25P2DecoderHDQPSK extends P25P2Decoder
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2DecoderHDQPSK.class);

//    protected static final float SYMBOL_TIMING_GAIN = 0.3f;
    protected static final float SYMBOL_TIMING_GAIN = 0.01f;
    protected InterpolatingSampleBuffer mInterpolatingSampleBuffer;
    protected DQPSKDecisionDirectedDemodulator mQPSKDemodulator;
    protected CostasLoop mCostasLoop;
    protected P25P2MessageFramer mMessageFramer;
    private ComplexFeedForwardGainControl mAGC = new ComplexFeedForwardGainControl(32);
    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    private ComplexFIRFilter2 mBasebandFilter;

    public P25P2DecoderHDQPSK()
    {
        super(6000.0);
        setSampleRate(25000.0);
    }

    public void setSampleRate(double sampleRate)
    {
        super.setSampleRate(sampleRate);

        mLog.debug("Setting sample rate to [" + sampleRate + "] and samples per symbol [" + getSamplesPerSymbol() + "]");
        mBasebandFilter = new ComplexFIRFilter2(getBasebandFilter());

        mCostasLoop = new CostasLoop(getSampleRate(), getSymbolRate());
        mCostasLoop.setPLLGain(PLLGain.LEVEL_11);
        mInterpolatingSampleBuffer = new InterpolatingSampleBuffer(getSamplesPerSymbol(), SYMBOL_TIMING_GAIN);

        mQPSKDemodulator = new DQPSKDecisionDirectedDemodulator(mCostasLoop, mInterpolatingSampleBuffer);

        if(mMessageFramer != null)
        {
            getDibitBroadcaster().removeListener(mMessageFramer);
        }

        //The Costas Loop receives symbol-inversion correction requests when detected.
        //The PLL gain monitor receives sync detect/loss signals from the message framer
        mMessageFramer = new P25P2MessageFramer(mCostasLoop, DecoderType.P25_PHASE2.getProtocol().getBitRate());
        mMessageFramer.setListener(getMessageProcessor());
        mMessageFramer.setSampleRate(sampleRate);

        mQPSKDemodulator.setSymbolListener(getDibitBroadcaster());
        getDibitBroadcaster().addListener(mMessageFramer);
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

        mMessageFramer.setCurrentTime(reusableComplexBuffer.getTimestamp());

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
                .passBandCutoff(6500)
                .passBandAmplitude(1.0)
                .passBandRipple(0.01)
                .stopBandAmplitude(0.0)
                .stopBandStart(7200)
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

        mMessageFramer = null;
    }

    @Override
    protected void process(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                mCostasLoop.reset();
                setSampleRate(sourceEvent.getValue().doubleValue());
                break;
            case NOTIFICATION_FREQUENCY_CORRECTION_CHANGE:
                //Reset the PLL if/when the tuner PPM changes so that we can re-lock
                mCostasLoop.reset();
                break;
        }
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
