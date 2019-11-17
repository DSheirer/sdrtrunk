/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.audio.convert.thumbdv.message.request;

import io.github.dsheirer.audio.convert.thumbdv.message.PacketField;

public class EncodeSpeechRequest extends AmbeRequest
{
    private static final int CHANNEL_IDENTIFIER_INDEX = 4;
    private static final int SPEECH_DATA_IDENTIFIER_INDEX = 5;
    private static final int SAMPLE_COUNT_INDEX = 6;
    private static final int SPEECH_DATA_START_INDEX = 7;

    private short[] mSamples;

    public EncodeSpeechRequest(short[] samples)
    {
        mSamples = samples;
    }

    public EncodeSpeechRequest(float[] samples)
    {
        mSamples = new short[samples.length];

        for(int x = 0; x < samples.length; x++)
        {
            if(samples[x] > 1.0f)
            {
                mSamples[x] = Short.MAX_VALUE;
            }
            else if(samples[x] < -1.0f)
            {
                mSamples[x] = Short.MIN_VALUE;
            }
            else
            {
                mSamples[x] = (short)(samples[x] * Short.MAX_VALUE);
            }
        }
    }

    @Override
    public PacketField getType()
    {
        return PacketField.PACKET_TYPE_DECODE_SPEECH;
    }

    @Override
    public byte[] getData()
    {
        int length = (mSamples.length * 2) + 3;
        byte[] data = createMessage(length, getType());

        data[CHANNEL_IDENTIFIER_INDEX] = PacketField.PKT_CHANNEL_0.getCode();
        data[SPEECH_DATA_IDENTIFIER_INDEX] = (byte)0x00;
        data[SAMPLE_COUNT_INDEX] = (byte)(0xFF & mSamples.length);

        int pointer = SPEECH_DATA_START_INDEX;

        for(short sample: mSamples)
        {
            data[pointer++] = (byte)((sample >> 8 & 0xFF));
            data[pointer++] = (byte)(sample & 0xFF);
        }

        return data;
    }
}
