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

package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.gain.complex.ComplexGainFactory;
import io.github.dsheirer.dsp.gain.complex.IComplexGainControl;
import io.github.dsheirer.dsp.psk.DQPSKDecisionDirectedDemodulator;
import io.github.dsheirer.dsp.psk.InterpolatingSampleBuffer;
import io.github.dsheirer.dsp.psk.pll.CostasLoop;
import io.github.dsheirer.dsp.psk.pll.FrequencyCorrectionSyncMonitor;
import io.github.dsheirer.dsp.psk.pll.PLLBandwidth;
import io.github.dsheirer.dsp.squelch.PowerMonitor;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IByteBufferProvider;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * DMR decoder module.
 */
public class DMRDecoder extends FeedbackDecoder implements ISourceEventListener, ISourceEventProvider,
        IComplexSamplesListener, Listener<ComplexSamples>, IByteBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRDecoder.class);
    protected static final float SAMPLE_COUNTER_GAIN = 0.4f;
    private static final double SYMBOL_RATE = 4800.0;
    private double mSampleRate;
    private Broadcaster<Dibit> mDibitBroadcaster = new Broadcaster<>();
    private DibitToByteBufferAssembler mByteBufferAssembler = new DibitToByteBufferAssembler(300);
    private DMRMessageProcessor mMessageProcessor;
    protected IComplexGainControl mAGC = ComplexGainFactory.getComplexGainControl();
    private Map<Double,float[]> mBasebandFilters = new HashMap<>();
    protected IRealFilter mIBasebandFilter;
    protected IRealFilter mQBasebandFilter;

    protected InterpolatingSampleBuffer mInterpolatingSampleBuffer;
    protected DQPSKDecisionDirectedDemodulator mQPSKDemodulator;
    protected CostasLoop mCostasLoop;
    protected FrequencyCorrectionSyncMonitor mFrequencyCorrectionSyncMonitor;
    protected DMRMessageFramer mMessageFramer;
    protected PowerMonitor mPowerMonitor = new PowerMonitor();

    /**
     * Constructs an instance
     */
    public DMRDecoder(DecodeConfigDMR config)
    {
        mMessageProcessor = new DMRMessageProcessor(config);
        mMessageProcessor.setMessageListener(getMessageListener());
        getDibitBroadcaster().addListener(mByteBufferAssembler);
        setSampleRate(25000.0);
    }

    /**
     * DMR decoder type.
     */
    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.DMR;
    }

    /**
     * Message processor
     */
    protected DMRMessageProcessor getMessageProcessor()
    {
        return mMessageProcessor;
    }

    /**
     * Sets the sample rate and configures internal decoder components.
     * @param sampleRate of the channel to decode
     */
    public void setSampleRate(double sampleRate)
    {
        if(sampleRate <= getSymbolRate() * 2)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be >9600 (2 * " +
                getSymbolRate() + " symbol rate)");
        }

        mPowerMonitor.setSampleRate((int)sampleRate);
        mSampleRate = sampleRate;
        mIBasebandFilter = FilterFactory.getRealFilter(getBasebandFilter());
        mQBasebandFilter = FilterFactory.getRealFilter(getBasebandFilter());
        mCostasLoop = new CostasLoop(getSampleRate(), getSymbolRate());
        mCostasLoop.setPLLBandwidth(PLLBandwidth.BW_300);
        mFrequencyCorrectionSyncMonitor = new FrequencyCorrectionSyncMonitor(mCostasLoop, this);
        mInterpolatingSampleBuffer = new InterpolatingSampleBuffer(getSamplesPerSymbol(), SAMPLE_COUNTER_GAIN);

        mQPSKDemodulator = new DQPSKDecisionDirectedDemodulator(mCostasLoop, mInterpolatingSampleBuffer);

        if(mMessageFramer != null)
        {
            getDibitBroadcaster().removeListener(mMessageFramer);
        }

        //The Costas Loop receives symbol-inversion correction requests when detected.
        //The PLL gain monitor receives sync detect/loss signals from the message framer
        mMessageFramer = new DMRMessageFramer(mCostasLoop);
        mMessageFramer.setSyncDetectListener(mFrequencyCorrectionSyncMonitor);
        mMessageFramer.setListener(getMessageProcessor());

        mQPSKDemodulator.setSymbolListener(getDibitBroadcaster());
        getDibitBroadcaster().addListener(mMessageFramer);
    }

    /**
     * Primary method for processing incoming complex sample buffers
     * @param samples containing channelized complex samples
     */
    @Override
    public void receive(ComplexSamples samples)
    {
        mMessageFramer.setCurrentTime(System.currentTimeMillis());

        float[] i = mIBasebandFilter.filter(samples.i());
        float[] q = mQBasebandFilter.filter(samples.q());

        //Process buffer for power measurements
        mPowerMonitor.process(i, q);

        ComplexSamples amplified = mAGC.process(i, q);
        mQPSKDemodulator.receive(amplified);
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
                filter = FilterFactory.getTaps(specification);//
            }
            catch(Exception fde) //FilterDesignException
            {
                mLog.error("Couldn't design low pass baseband filter for sample rate: " + getSampleRate());
            }

            if(filter != null)
            {
                mBasebandFilters.put(getSampleRate(), filter);
            }
            else
            {
                throw new IllegalStateException("Couldn't design a DMR baseband filter for sample rate: " + getSampleRate());
            }
        }

        return filter;
    }

    /**
     * Process source events
     */
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
        mFrequencyCorrectionSyncMonitor.reset();
    }

    /**
     * Assembler for packaging Dibit stream into reusable byte buffers.
     */
    protected Broadcaster<Dibit> getDibitBroadcaster()
    {
        return mDibitBroadcaster;
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the byte buffer assembler
     */
    @Override
    public void setBufferListener(Listener<ByteBuffer> listener)
    {
        mByteBufferAssembler.setBufferListener(listener);
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the byte buffer assembler
     */
    @Override
    public void removeBufferListener(Listener<ByteBuffer> listener)
    {
        mByteBufferAssembler.removeBufferListener(listener);
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the byte buffer assembler
     */
    @Override
    public boolean hasBufferListeners()
    {
        return mByteBufferAssembler.hasBufferListeners();
    }

    protected double getSymbolRate()
    {
        return SYMBOL_RATE;
    }

    /**
     * Current sample rate for this decoder
     */
    protected double getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Samples per symbol based on current sample rate and symbol rate.
     */
    public float getSamplesPerSymbol()
    {
        return (float)(getSampleRate() / getSymbolRate());
    }

    /**
     * Sets the source event listener to receive source events from this decoder.
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        super.setSourceEventListener(listener);
        mPowerMonitor.setSourceEventListener(listener);
    }

    /**
     * Removes a registered source event listener from receiving source events from this decoder
     */
    @Override
    public void removeSourceEventListener()
    {
        super.removeSourceEventListener();
        mPowerMonitor.setSourceEventListener(null);
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return sourceEvent -> process(sourceEvent);
    }

    /**
     * Listener interface to receive reusable complex buffers
     */
    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return DMRDecoder.this;
    }

    /**
     * Starts the decoder
     */
    @Override
    public void start()
    {
        //No-op
    }

    /**
     * Stops the decoder
     */
    @Override
    public void stop()
    {
        //No-op
    }
}
