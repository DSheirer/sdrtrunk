/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.module.decode.fleetsync2;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.bits.SyncPattern;
import io.github.dsheirer.dsp.afsk.AFSK1200Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.afsk.AbstractAFSKDecoder;

/**
 * Fleetsync II Decoder
 */
public class Fleetsync2Decoder extends AbstractAFSKDecoder
{
    //Message length - 5 x REVS + 16 x SYNC + 8 x 64Bit Blocks
    private static final int MESSAGE_LENGTH = 537;
    private MessageFramer mMessageFramer;
    private Fleetsync2MessageProcessor mMessageProcessor;

    public Fleetsync2Decoder(AFSK1200Decoder afsk1200Decoder, AliasList aliasList)
    {
        super(afsk1200Decoder);
        mMessageFramer = new MessageFramer(SyncPattern.FLEETSYNC2.getPattern(), MESSAGE_LENGTH);
        getDecoder().setMessageFramer(mMessageFramer);
        mMessageProcessor = new Fleetsync2MessageProcessor(aliasList);
        mMessageFramer.addMessageListener(mMessageProcessor);
        mMessageProcessor.setMessageListener(getMessageListener());
    }

    public Fleetsync2Decoder(AliasList aliasList)
    {
        this(new AFSK1200Decoder(AFSK1200Decoder.Output.NORMAL), aliasList);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.FLEETSYNC2;
    }

    @Override
    public void reset()
    {
        mMessageFramer.reset();
    }
}
