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
package io.github.dsheirer.module.decode.tait;

import io.github.dsheirer.bits.IBinarySymbolProcessor;
import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.bits.SyncPattern;
import io.github.dsheirer.dsp.afsk.AFSK1200Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.afsk.AbstractAFSKDecoder;

/**
 * TAIT 1200 - 1200 baud AFSK decoder
 */
public class Tait1200Decoder extends AbstractAFSKDecoder implements IBinarySymbolProcessor
{
    private static final int MESSAGE_LENGTH = 440;

    private MessageFramer mMessageFramerGPS;
    private MessageFramer mMessageFramerANI;
    private Tait1200GPSMessageProcessor mMessageAProcessor;
    private Tait1200ANIMessageProcessor mMessageBProcessor;

    protected Tait1200Decoder(AFSK1200Decoder decoder)
    {
        super(decoder);
        init();
    }

    public Tait1200Decoder()
    {
        super(AFSK1200Decoder.Output.NORMAL);
        init();
    }

    private void init()
    {
        getDecoder().setSymbolProcessor(this);

        mMessageFramerGPS = new MessageFramer(SyncPattern.TAIT_CCDI_GPS_MESSAGE.getPattern(), MESSAGE_LENGTH);
        mMessageFramerANI = new MessageFramer(SyncPattern.TAIT_SELCAL_MESSAGE.getPattern(), MESSAGE_LENGTH);

        mMessageAProcessor = new Tait1200GPSMessageProcessor();
        mMessageBProcessor = new Tait1200ANIMessageProcessor();

        mMessageFramerGPS.addMessageListener(mMessageAProcessor);
        mMessageFramerANI.addMessageListener(mMessageBProcessor);

        mMessageAProcessor.setMessageListener(getMessageListener());
        mMessageBProcessor.setMessageListener(getMessageListener());
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.TAIT_1200;
    }

    @Override
    public void process(boolean symbol)
    {
        mMessageFramerANI.process(symbol);
        mMessageFramerGPS.process(symbol);
    }

    public MessageFramer getANIMessageFramer()
    {
        return mMessageFramerANI;
    }

    public MessageFramer getGPSMessageFramer()
    {
        return mMessageFramerGPS;
    }
}
