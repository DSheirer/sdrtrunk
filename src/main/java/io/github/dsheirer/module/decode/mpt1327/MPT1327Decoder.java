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
package io.github.dsheirer.module.decode.mpt1327;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.IBinarySymbolProcessor;
import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.dsp.afsk.AFSK1200Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.afsk.AbstractAFSKDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MPT1327Decoder extends AbstractAFSKDecoder implements IBinarySymbolProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(MPT1327Decoder.class);

    /* Message length -- longest possible message is:
     *   4xREVS + 16xSYNC + 64xADD1 + 64xDCW1 + 64xDCW2 + 64xDCW3 + 64xDCW4 */
    private static final int MESSAGE_LENGTH = 350;

    private MessageFramer mControlMessageFramer;
    private MessageFramer mTrafficMessageFramer;
    private MPT1327MessageProcessor mMessageProcessor;

    public MPT1327Decoder(AliasList aliasList, Sync sync)
    {
        super((sync == Sync.NORMAL ? AFSK1200Decoder.Output.NORMAL : AFSK1200Decoder.Output.INVERTED));

        getDecoder().setSymbolProcessor(this);

        //Message framer for control channel messages
        mControlMessageFramer = new MessageFramer(sync.getControlSyncPattern().getPattern(), MESSAGE_LENGTH);

        //Message framer for traffic channel massages
        mTrafficMessageFramer = new MessageFramer(sync.getTrafficSyncPattern().getPattern(), MESSAGE_LENGTH);

        //Fully decoded and framed messages processor
        mMessageProcessor = new MPT1327MessageProcessor(aliasList);
        mMessageProcessor.setMessageListener(getMessageListener());

        mControlMessageFramer.addMessageListener(mMessageProcessor);
        mTrafficMessageFramer.addMessageListener(mMessageProcessor);
    }

    public void receive(boolean symbol)
    {
        mControlMessageFramer.receive(symbol);
        mTrafficMessageFramer.receive(symbol);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.MPT1327;
    }

    @Override
    public void reset()
    {
        mControlMessageFramer.reset();
        mTrafficMessageFramer.reset();
    }

    /**
     * Cleanup method
     */
    public void dispose()
    {
        super.dispose();

        mMessageProcessor.dispose();
        mControlMessageFramer.dispose();
        mTrafficMessageFramer.dispose();
    }

}
