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
package io.github.dsheirer.sample.adapter;

import io.github.dsheirer.source.mixer.MixerChannel;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Adapter for converting stereo/2-channel short sample data to single channel float samples.
 */
public class RealChannelShortAdapter extends RealSampleAdapter
{
    private final static Logger mLog = LoggerFactory.getLogger(RealChannelShortAdapter.class);
    private ByteOrder mByteOrder = ByteOrder.LITTLE_ENDIAN;
    private MixerChannel mMixerChannel;
    private ByteBuffer mByteBuffer;

    public RealChannelShortAdapter(MixerChannel channel)
    {
        /* Only use with LEFT/RIGHT channels */
        Validate.isTrue(channel != MixerChannel.MONO);

        mMixerChannel = channel;
    }

    @Override
    public float[] convert(byte[] samples)
    {
        float[] convertedSamples = new float[samples.length / 4];

        mByteBuffer = ByteBuffer.wrap(samples);

        /* Set endian to correct byte ordering */
        mByteBuffer.order(mByteOrder);

        int pointer = 0;

        while(mByteBuffer.hasRemaining())
        {
            if(mMixerChannel == MixerChannel.LEFT)
            {
                convertedSamples[pointer++] = (float) mByteBuffer.getShort() / 32768.0f;

                //Throw away the right channel
                mByteBuffer.getShort();
            }
            else
            {
                //Throw away the left channel
                mByteBuffer.getShort();

                convertedSamples[pointer++] = (float) mByteBuffer.getShort() / 32768.0f;
            }
        }

        return convertedSamples;
    }

    /**
     * Set byte interpretation to little or big endian.  Defaults to LITTLE
     * endian
     */
    public void setByteOrder(ByteOrder order)
    {
        mByteOrder = order;
    }
}
