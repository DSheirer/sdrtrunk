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
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter;
import io.github.dsheirer.dsp.gain.ComplexFeedForwardGainControl;
import io.github.dsheirer.dsp.psk.DQPSKGardnerDemodulator;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBuffer;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.sample.complex.reusable.ReusableComplexBuffer;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class P25DecoderLSM extends P25Decoder
{
    private final static Logger mLog = LoggerFactory.getLogger(P25DecoderLSM.class);
    protected static final float SAMPLE_COUNTER_GAIN = 0.5f;

    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    private ComplexFIRFilter mBasebandFilter;
    private ComplexFeedForwardGainControl mAGC = new ComplexFeedForwardGainControl(32);
    protected DQPSKGardnerDemodulator mQPSKDemodulator;
    protected P25MessageFramer mMessageFramer;
    protected CostasLoop mCostasLoop;
    protected InterpolatingSampleBuffer mInterpolatingSampleBuffer;

    /**
     * P25 Phase 1 - linear simulcast modulation (LSM) decoder.  Uses Differential QPSK decoding with a Costas PLL and
     * a gardner timing error detector.
     *
     * @param aliasList
     */
    public P25DecoderLSM(AliasList aliasList)
    {
        super(4800.0, aliasList);
        setSampleRate(48000.0);
    }

    /**
     * Sets or changes the channel sample rate
     * @param sampleRate in hertz
     */
    public void setSampleRate(double sampleRate)
    {
        super.setSampleRate(sampleRate);

        mBasebandFilter = new ComplexFIRFilter(getBasebandFilter(), 1.0f);

        mCostasLoop = new CostasLoop(getSampleRate(), getSymbolRate());
        mInterpolatingSampleBuffer = new InterpolatingSampleBuffer(getSamplesPerSymbol(), SAMPLE_COUNTER_GAIN);

        mQPSKDemodulator = new DQPSKGardnerDemodulator(mCostasLoop, mInterpolatingSampleBuffer);

        //Message framer can issue a symbol-inversion correction request to the PLL when detected
        mMessageFramer = new P25MessageFramer(getAliasList(), mCostasLoop);
        mMessageFramer.setListener(getMessageProcessor());
        mQPSKDemodulator.setSymbolListener(mMessageFramer);
    }

    /**
     * Primary method for receiving incoming channel samples
     */
    @Override
    public void receive(ReusableComplexBuffer reusableComplexBuffer)
    {
        //The filter will decrement the user count when finished
        ReusableComplexBuffer basebandFiltered = filter(reusableComplexBuffer);

        //AGC will decrement the user count when finished
        ReusableComplexBuffer gainApplied = mAGC.filter(basebandFiltered);

        //Decoder will decrement the user count when finished
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
     * Processes source events - updates buffer and decoder when sample rate is established/changed and resets the
     * PLL after frequency changes/corrections
     */
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
     * Resets this decoder to prepare for processing a new channel
     */
    @Override
    public void reset()
    {
        mCostasLoop.reset();
    }

    public void dispose()
    {
        super.dispose();

        mBasebandFilter.dispose();
        mBasebandFilter = null;

        mAGC.dispose();
        mAGC = null;

        mQPSKDemodulator.dispose();
        mQPSKDemodulator = null;

        mMessageFramer.dispose();
        mMessageFramer = null;
    }

    public Modulation getModulation()
    {
        return Modulation.CQPSK;
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
            filter = FilterFactory.getLowPass(48000, 7250, 8000, 60,
                WindowType.HANN, true);

            mBasebandFilters.put(getSampleRate(), filter);
        }

        return filter;
    }
}
