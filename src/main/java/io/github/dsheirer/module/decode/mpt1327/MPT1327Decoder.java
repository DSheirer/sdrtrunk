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
package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.bits.IBinarySymbolProcessor;
import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.dsp.afsk.AFSK1200Decoder;
import io.github.dsheirer.dsp.symbol.BinaryToByteBufferAssembler;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.afsk.AbstractAFSKDecoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IByteBufferProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Instrumented version of the MPT1327 decoder.  Exposes properties for instrumented manual decoding of a signal.
 */
public class MPT1327Decoder extends AbstractAFSKDecoder implements IBinarySymbolProcessor, IByteBufferProvider
{
    private final static Logger mLog = LoggerFactory.getLogger(MPT1327Decoder.class);

    /* Message length -- longest possible message is:
     *   4xREVS + 16xSYNC + 64xADD1 + 64xDCW1 + 64xDCW2 + 64xDCW3 + 64xDCW4 */
    private static final int MESSAGE_LENGTH = 350;

    private MessageFramer mControlMessageFramer;
    private MessageFramer mTrafficMessageFramer;
    private MPT1327MessageProcessor mMessageProcessor;
    private BinaryToByteBufferAssembler mBinaryToByteBufferAssembler = new BinaryToByteBufferAssembler(512);

    protected MPT1327Decoder(AFSK1200Decoder decoder, Sync sync)
    {
        super(decoder);
        init(sync);
    }

    public MPT1327Decoder(Sync sync)
    {
        super((sync == Sync.NORMAL ? AFSK1200Decoder.Output.NORMAL : AFSK1200Decoder.Output.INVERTED));
        init(sync);
    }

    private void init(Sync sync)
    {
        getDecoder().setSymbolProcessor(this);

        //Message framer for control channel messages
        mControlMessageFramer = new MessageFramer(sync.getControlSyncPattern().getPattern(), MESSAGE_LENGTH);

        //Message framer for traffic channel massages
        mTrafficMessageFramer = new MessageFramer(sync.getTrafficSyncPattern().getPattern(), MESSAGE_LENGTH);

        //Fully decoded and framed messages processor
        mMessageProcessor = new MPT1327MessageProcessor();
        mMessageProcessor.setMessageListener(getMessageListener());

        mControlMessageFramer.addMessageListener(mMessageProcessor);
        mTrafficMessageFramer.addMessageListener(mMessageProcessor);
    }

    public void process(boolean symbol)
    {
        mControlMessageFramer.process(symbol);
        mTrafficMessageFramer.process(symbol);
        mBinaryToByteBufferAssembler.process(symbol);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MPT1327;
    }

    @Override
    public void reset()
    {
    }

    @Override
    public void start()
    {
        super.start();
        mControlMessageFramer.reset();
        mTrafficMessageFramer.reset();
    }

    public MessageFramer getControlMessageFramer()
    {
        return mControlMessageFramer;
    }

    public MessageFramer getTrafficMessageFramer()
    {
        return mTrafficMessageFramer;
    }

    @Override
    public void setBufferListener(Listener<ByteBuffer> listener)
    {
        mBinaryToByteBufferAssembler.setBufferListener(listener);
    }

    @Override
    public void removeBufferListener(Listener<ByteBuffer> listener)
    {
        mBinaryToByteBufferAssembler.removeBufferListener(listener);
    }

    @Override
    public boolean hasBufferListeners()
    {
        return mBinaryToByteBufferAssembler.hasBufferListeners();
    }
}
