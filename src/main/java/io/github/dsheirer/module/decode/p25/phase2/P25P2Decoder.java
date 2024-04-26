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
package io.github.dsheirer.module.decode.p25.phase2;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.dsp.squelch.PowerMonitor;
import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.p25.P25FrequencyBandPreloadDataContent;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IByteBufferProvider;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.sample.complex.IComplexSamplesListener;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import java.nio.ByteBuffer;

/**
 * Base P25 Phase 2 Decoder
 */
public abstract class P25P2Decoder extends FeedbackDecoder implements ISourceEventListener, ISourceEventProvider,
        IComplexSamplesListener, Listener<ComplexSamples>, IByteBufferProvider
{
    private double mSampleRate;
    private Broadcaster<Dibit> mDibitBroadcaster = new Broadcaster<>();
    private DibitToByteBufferAssembler mByteBufferAssembler = new DibitToByteBufferAssembler(300);
    private P25P2MessageProcessor mMessageProcessor;
    private double mSymbolRate;
    protected PowerMonitor mPowerMonitor = new PowerMonitor();

    public P25P2Decoder(double symbolRate)
    {
        mSymbolRate = symbolRate;
        mMessageProcessor = new P25P2MessageProcessor();
        mMessageProcessor.setMessageListener(getMessageListener());
        getDibitBroadcaster().addListener(mByteBufferAssembler);
    }

    /**
     * Preloads P25 frequency bands when this is a traffic channel, since they are not transmitted on the traffic
     * channel and the decoder state needs accurate frequency values to pass to the traffic channel manager for
     * call event tracking.
     * @param preLoadDataContent with known frequency bands.
     */
    @Subscribe
    public void process(P25FrequencyBandPreloadDataContent preLoadDataContent)
    {
        mMessageProcessor.preload(preLoadDataContent);
    }

    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener )
    {
        super.setSourceEventListener(listener);
        mPowerMonitor.setSourceEventListener(listener);
    }

    @Override
    public void removeSourceEventListener()
    {
        super.removeSourceEventListener();
        mPowerMonitor.setSourceEventListener(null);
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
        return mSymbolRate;
    }

    /**
     * Current sample rate for this decoder
     */
    protected double getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Sets current sample rate for this decoder
     */
    public void setSampleRate(double sampleRate)
    {
        if(sampleRate <= getSymbolRate() * 2)
        {
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be > 12000 (2 * " +
                getSymbolRate() + " symbol rate)");
        }

        mPowerMonitor.setSampleRate((int)sampleRate);
        mSampleRate = sampleRate;
    }

    /**
     * Samples per symbol based on current sample rate and symbol rate.
     */
    public float getSamplesPerSymbol()
    {
        return (float)(getSampleRate() / getSymbolRate());
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return new Listener<SourceEvent>()
        {
            @Override
            public void receive(SourceEvent sourceEvent)
            {
                process(sourceEvent);
            }
        };
    }

    /**
     * Sub-class processing of received source events
     *
     * @param sourceEvent
     */
    protected abstract void process(SourceEvent sourceEvent);

    /**
     * Listener interface to receive reusable complex buffers
     */
    @Override
    public Listener<ComplexSamples> getComplexSamplesListener()
    {
        return P25P2Decoder.this;
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE2;
    }

    protected P25P2MessageProcessor getMessageProcessor()
    {
        return mMessageProcessor;
    }
}
