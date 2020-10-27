/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.mdc1200;

import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.bits.SyncPattern;
import io.github.dsheirer.dsp.NRZDecoder;
import io.github.dsheirer.dsp.afsk.AFSK1200Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.afsk.AbstractAFSKDecoder;

/**
 * MDC1200 Decoder - 1200 baud 2FSK decoder
 */
public class MDCDecoder extends AbstractAFSKDecoder
{
    private static final int MESSAGE_LENGTH = 304;

    private NRZDecoder mNRZDecoder;
    private MessageFramer mMessageFramer;
    private MDCMessageProcessor mMessageProcessor;

    public MDCDecoder()
    {
        super(AFSK1200Decoder.Output.INVERTED);
        init();
    }

    protected MDCDecoder(AFSK1200Decoder decoder)
    {
        super(decoder);
        init();
    }

    private void init()
    {
        mNRZDecoder = new NRZDecoder(NRZDecoder.MODE_INVERTED);
        getDecoder().setSymbolProcessor(mNRZDecoder);
        mMessageFramer = new MessageFramer(SyncPattern.MDC1200.getPattern(), MESSAGE_LENGTH);
        mNRZDecoder.setListener(mMessageFramer);
        mMessageProcessor = new MDCMessageProcessor();
        mMessageFramer.addMessageListener(mMessageProcessor);
        mMessageProcessor.addMessageListener(getMessageListener());
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MDC1200;
    }

    public MessageFramer getMessageFramer()
    {
        return mMessageFramer;
    }
}
