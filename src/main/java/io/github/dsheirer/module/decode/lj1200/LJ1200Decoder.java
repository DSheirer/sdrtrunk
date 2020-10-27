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
package io.github.dsheirer.module.decode.lj1200;

import io.github.dsheirer.bits.IBinarySymbolProcessor;
import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.bits.SyncPattern;
import io.github.dsheirer.dsp.afsk.AFSK1200Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.afsk.AbstractAFSKDecoder;

/**
 * LJ1200 - 1200 baud AFSK decoder
 */
public class LJ1200Decoder extends AbstractAFSKDecoder implements IBinarySymbolProcessor
{
    /* Message length - 16-bit sync plus 64 bit message */
    private static final int MESSAGE_LENGTH = 80;

    private MessageFramer mTowerMessageFramer;
    private MessageFramer mTransponderMessageFramer;
    private LJ1200MessageProcessor mMessageProcessor;

    protected LJ1200Decoder(AFSK1200Decoder decoder)
    {
        super(decoder);
        init();
    }

    public LJ1200Decoder()
    {
        super(AFSK1200Decoder.Output.NORMAL);
        init();
    }


    private void init()
    {
        getDecoder().setSymbolProcessor(this);

        mTowerMessageFramer = new MessageFramer(SyncPattern.LJ1200.getPattern(), MESSAGE_LENGTH);
        mTransponderMessageFramer = new MessageFramer(SyncPattern.LJ1200_TRANSPONDER.getPattern(), MESSAGE_LENGTH);
        mMessageProcessor = new LJ1200MessageProcessor();
        mTowerMessageFramer.addMessageListener(mMessageProcessor);
        mTransponderMessageFramer.addMessageListener(mMessageProcessor);
        mMessageProcessor.setMessageListener(getMessageListener());
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.LJ_1200;
    }

    @Override
    public void process(boolean symbol)
    {
        mTowerMessageFramer.process(symbol);
        mTransponderMessageFramer.process(symbol);
    }

    public MessageFramer getTowerMessageFramer()
    {
        return mTowerMessageFramer;
    }

    public MessageFramer getTransponderMessageFramer()
    {
        return mTransponderMessageFramer;
    }
}
