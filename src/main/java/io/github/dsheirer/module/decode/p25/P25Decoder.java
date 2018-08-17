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
import io.github.dsheirer.dsp.symbol.DibitToByteBufferAssembler;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableByteBufferListener;
import io.github.dsheirer.sample.buffer.IReusableByteBufferProvider;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferListener;
import io.github.dsheirer.sample.buffer.ReusableByteBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;

public abstract class P25Decoder extends Decoder
    implements ISourceEventListener, ISourceEventProvider, IReusableComplexBufferListener, Listener<ReusableComplexBuffer>,
    IReusableByteBufferProvider, IReusableByteBufferListener
{
    private double mSampleRate;
    private DibitToByteBufferAssembler mByteBufferAssembler = new DibitToByteBufferAssembler(300);
    private P25MessageProcessor mMessageProcessor;
    private AliasList mAliasList;
    private Listener<SourceEvent> mSourceEventListener;
    private double mSymbolRate;

    public P25Decoder(double symbolRate, AliasList aliasList)
    {
        mSymbolRate = symbolRate;
        mAliasList = aliasList;
        mMessageProcessor = new P25MessageProcessor(mAliasList);
        mMessageProcessor.setMessageListener(getMessageListener());
    }

    /**
     * Assembler for packaging Dibit stream into reusable byte buffers.
     */
    protected DibitToByteBufferAssembler getByteBufferAssembler()
    {
        return mByteBufferAssembler;
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the byte buffer assembler
     */
    @Override
    public void setBufferListener(Listener<ReusableByteBuffer> listener)
    {
        getByteBufferAssembler().setBufferListener(listener);
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the byte buffer assembler
     */
    @Override
    public void removeBufferListener(Listener<ReusableByteBuffer> listener)
    {
        getByteBufferAssembler().removeBufferListener(listener);
    }

    /**
     * Implements the IByteBufferProvider interface - delegates to the byte buffer assembler
     */
    @Override
    public boolean hasBufferListeners()
    {
        return getByteBufferAssembler().hasBufferListeners();
    }

    protected double getSymbolRate()
    {
        return mSymbolRate;
    }

    protected AliasList getAliasList()
    {
        return mAliasList;
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
            throw new IllegalArgumentException("Sample rate [" + sampleRate + "] must be >9600 (2 * " +
                getSymbolRate() + " symbol rate)");
        }

        mSampleRate = sampleRate;
    }

    /**
     * Samples per symbol based on current sample rate and symbol rate.
     */
    public float getSamplesPerSymbol()
    {
        return (float)(getSampleRate() / getSymbolRate());
    }

    public void dispose()
    {
        super.dispose();

        mMessageProcessor.dispose();
        mMessageProcessor = null;
    }

    /**
     * Sets the source event listener to receive source events from this decoder.
     */
    @Override
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Removes a registered source event listener from receiving source events from this decoder
     */
    @Override
    public void removeSourceEventListener()
    {
        mSourceEventListener = null;
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
     * @param sourceEvent
     */
    protected abstract void process(SourceEvent sourceEvent);

    /**
     * Broadcasts the source event to an optional registered listener.  This method should primarily be used to
     * issue frequency correction requests to the channel source.
     * @param sourceEvent to broadcast
     */
    public void broadcast(SourceEvent sourceEvent)
    {
        if(mSourceEventListener != null)
        {
            mSourceEventListener.receive(sourceEvent);
        }
    }

    /**
     * Listener interface to receive reusable complex buffers
     */
    @Override
    public Listener<ReusableComplexBuffer> getReusableComplexBufferListener()
    {
        return P25Decoder.this;
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

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.P25_PHASE1;
    }

    protected P25MessageProcessor getMessageProcessor()
    {
        return mMessageProcessor;
    }

    public abstract Modulation getModulation();

    public enum Modulation
    {
        CQPSK("Simulcast (LSM)", "LSM"),
        C4FM("Normal (C4FM)", "C4FM");

        private String mLabel;
        private String mShortLabel;

        private Modulation(String label, String shortLabel)
        {
            mLabel = label;
            mShortLabel = shortLabel;
        }

        public String getLabel()
        {
            return mLabel;
        }

        public String getShortLabel()
        {
            return mShortLabel;
        }

        public String toString()
        {
            return getLabel();
        }
    }
}
