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
package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.gain.complex.ComplexGainFactory;
import io.github.dsheirer.dsp.gain.complex.IComplexGainControl;
import io.github.dsheirer.dsp.psk.DQPSKGardnerDemodulator;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBuffer;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.FrequencyCorrectionSyncMonitor;
import io.github.dsheirer.dsp.psk.pll.PLLBandwidth;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.SourceEvent;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class P25P1DecoderLSM extends P25P1Decoder
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1DecoderLSM.class);

    protected static final float SAMPLE_COUNTER_GAIN = 0.3f;

    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    protected IRealFilter mIBasebandFilter;
    protected IRealFilter mQBasebandFilter;
    protected IComplexGainControl mAGC = ComplexGainFactory.getComplexGainControl();
    protected DQPSKGardnerDemodulator mQPSKDemodulator;
    protected P25P1MessageFramer mMessageFramer;
    protected CostasLoop mCostasLoop;
    protected InterpolatingSampleBuffer mInterpolatingSampleBuffer;
    protected FrequencyCorrectionSyncMonitor mFrequencyCorrectionSyncMonitor;

    /**
     * P25 Phase 1 - linear simulcast modulation (LSM) decoder.  Uses Differential QPSK decoding with a Costas PLL and
     * a gardner timing error detector.
     */
    public P25P1DecoderLSM()
    {
        super(4800.0);
        setSampleRate(25000.0);
    }

    @Override
    public void start()
    {
        super.start();
        mQPSKDemodulator.start();
    }

    @Override
    public void stop()
    {
        super.stop();
        mQPSKDemodulator.stop();
    }

    /**
     * Sets or changes the channel sample rate
     *
     * @param sampleRate in hertz
     */
    public void setSampleRate(double sampleRate)
    {
        super.setSampleRate(sampleRate);

        mIBasebandFilter = FilterFactory.getRealFilter(getBasebandFilter());
        mQBasebandFilter = FilterFactory.getRealFilter(getBasebandFilter());

        mCostasLoop = new CostasLoop(getSampleRate(), getSymbolRate());
        mCostasLoop.setPLLBandwidth(PLLBandwidth.BW_200);
        mFrequencyCorrectionSyncMonitor = new FrequencyCorrectionSyncMonitor(mCostasLoop, this);

        mInterpolatingSampleBuffer = new InterpolatingSampleBuffer(getSamplesPerSymbol(), SAMPLE_COUNTER_GAIN);

        mQPSKDemodulator = new DQPSKGardnerDemodulator(mCostasLoop, mInterpolatingSampleBuffer);

        //The Costas Loop receives symbol-inversion correction requests when detected.
        //The PLL gain monitor receives sync detect/loss signals from the message framer
        if(mMessageFramer != null)
        {
            getDibitBroadcaster().removeListener(mMessageFramer);
        }

        mMessageFramer = new P25P1MessageFramer(mCostasLoop, DecoderType.P25_PHASE1.getProtocol().getBitRate());
        mMessageFramer.setSyncDetectListener(mFrequencyCorrectionSyncMonitor);
        mMessageFramer.setListener(getMessageProcessor());
        mMessageFramer.setSampleRate(sampleRate);
        mQPSKDemodulator.setSymbolListener(getDibitBroadcaster());
        getDibitBroadcaster().addListener(mMessageFramer);
    }

    /**
     * Primary method for receiving incoming channel samples
     */
    @Override
    public void receive(ComplexSamples samples)
    {
        mMessageFramer.setCurrentTime(samples.timestamp());

        //The filter will decrement the user count when finished
        float[] i = mIBasebandFilter.filter(samples.i());
        float[] q = mQBasebandFilter.filter(samples.q());

        //Process the buffer for power measurements
        mPowerMonitor.process(i, q);

        ComplexSamples amplified = mAGC.process(i, q, samples.timestamp());
        mQPSKDemodulator.receive(amplified);
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
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                setSampleRate(sourceEvent.getValue().doubleValue());
                mCostasLoop.reset();
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
        mFrequencyCorrectionSyncMonitor.reset();
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
            filter = FilterFactory.getLowPass(getSampleRate(), 7250, 8000, 60,
                WindowType.HANN, true);

            mBasebandFilters.put(getSampleRate(), filter);
        }

        return filter;
    }
}
