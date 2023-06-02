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
package io.github.dsheirer.module.decode.ltrnet;

import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.bits.SyncPattern;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.dsp.fsk.LTRDecoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LTR-Net decoder
 */
public class LTRNetDecoder extends Decoder implements IRealBufferListener, Listener<float[]>
{
    private static final Logger mLog = LoggerFactory.getLogger(LTRNetDecoder.class);
    public static final int LTR_NET_MESSAGE_LENGTH = 40;
    protected LTRDecoder mLTRDecoder;
    private MessageFramer mLTRMessageFramer;
    private LTRNetMessageProcessor mLTRMessageProcessor;

    /**
     * LTR-Net Decoder.  Decodes unfiltered (e.g. demodulated but with no DC or
     * audio filtering) samples and produces LTR-Net messages.
     */
    public LTRNetDecoder(DecodeConfigLTRNet config, LTRDecoder ltrDecoder)
    {
        mLTRDecoder = ltrDecoder;

        if(config.getMessageDirection() == MessageDirection.OSW)
        {
            mLTRMessageFramer = new MessageFramer(SyncPattern.LTR_STANDARD_OSW.getPattern(), LTR_NET_MESSAGE_LENGTH);
        }
        else
        {
            mLTRMessageFramer = new MessageFramer(SyncPattern.LTR_STANDARD_ISW.getPattern(), LTR_NET_MESSAGE_LENGTH);
        }

        mLTRDecoder.setListener(bits -> {
            for(boolean bit: bits)
            {
                mLTRMessageFramer.process(bit);
            }
        });
        mLTRMessageProcessor = new LTRNetMessageProcessor(config.getMessageDirection());
        mLTRMessageFramer.addMessageListener(mLTRMessageProcessor);
        mLTRMessageProcessor.setMessageListener(getMessageListener());
    }

    /**
     * LTR-Net Decoder.  Decodes unfiltered (e.g. demodulated but with no DC or
     * audio filtering) samples and produces LTR-Net messages.
     */
    public LTRNetDecoder(DecodeConfigLTRNet config)
    {
        this(config, new LTRDecoder());
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.LTR_NET;
    }

    @Override
    public Listener<float[]> getBufferListener()
    {
        return mLTRDecoder;
    }

    @Override
    public void receive(float[] demodulatedSamples)
    {
        mLTRDecoder.receive(demodulatedSamples);
    }

    @Override
    public void reset()
    {
        mLTRMessageFramer.reset();
    }
}
