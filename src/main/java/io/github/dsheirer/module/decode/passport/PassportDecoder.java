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
package io.github.dsheirer.module.decode.passport;

import io.github.dsheirer.bits.MessageFramer;
import io.github.dsheirer.bits.SyncPattern;
import io.github.dsheirer.module.decode.Decoder;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.dsp.fsk.LTRDecoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;

/**
 * LTR Passport decoder
 */
public class PassportDecoder extends Decoder implements IRealBufferListener, Listener<float[]>
{
    public static final int PASSPORT_MESSAGE_LENGTH = 68;
    private LTRDecoder mLTRDecoder;
    private MessageFramer mPassportMessageFramer;
    private PassportMessageProcessor mPassportMessageProcessor;

    /**
     * Passport Decoder.  Decodes unfiltered (e.g. demodulated but with no DC or
     * audio filtering) samples and produces Passport messages.
     */
    public PassportDecoder(DecodeConfiguration config)
    {
        mLTRDecoder = new LTRDecoder();

        mPassportMessageFramer = new MessageFramer(SyncPattern.PASSPORT.getPattern(), PASSPORT_MESSAGE_LENGTH);

        mLTRDecoder.setListener(bits -> {
            for(boolean bit: bits)
            {
                mPassportMessageFramer.process(bit);
            }
        });
        mPassportMessageProcessor = new PassportMessageProcessor();
        mPassportMessageFramer.addMessageListener(mPassportMessageProcessor);
        mPassportMessageProcessor.setMessageListener(getMessageListener());
    }

    @Override
    public Listener<float[]> getBufferListener()
    {
        return mLTRDecoder;
    }

    @Override
    public void receive(float[] realBuffer)
    {
        mLTRDecoder.receive(realBuffer);
    }

    @Override
    public DecoderType getDecoderType()
    {
        return DecoderType.PASSPORT;
    }

    @Override
    public void reset()
    {
        mPassportMessageFramer.reset();
    }
}
