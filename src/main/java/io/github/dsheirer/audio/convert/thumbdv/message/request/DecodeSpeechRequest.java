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
import io.github.dsheirer.audio.convert.thumbdv.message.VocoderRate;

/**
 * Decode speech request, used to request decode of an encoded audio frame.
 */
public class DecodeSpeechRequest extends AmbeRequest
{
    private static final int CHANNEL_DATA_IDENTIFIER_INDEX = 4;
    private static final byte SAMPLE_COUNT = (byte)(0xFF & 160);  //8 kHz Audio sample count for 20ms frame

    private byte[] mAudioFrame;
    private VocoderRate mVocoderRate;

    /**
     * Constructs an audio frame decode request using the specified vocoder rate.
     * @param audioFrame of encoded audio samples
     * @param vocoderRate to use when decoding
     */
    public DecodeSpeechRequest(byte[] audioFrame, VocoderRate vocoderRate)
    {
        mAudioFrame = audioFrame;
        mVocoderRate = vocoderRate;
    }

    /**
     * Constructs an audio frame decode request using the current vocoder rate.
     * @param audioFrame of encoded audio samples
     */
    public DecodeSpeechRequest(byte[] audioFrame)
    {
        this(audioFrame, null);
    }

    @Override
    public PacketField getType()
    {
        return PacketField.PACKET_TYPE_ENCODE_SPEECH;
    }

    private boolean hasVocoderRate()
    {
        return mVocoderRate != null;
    }

    @Override
    public byte[] getData()
    {
        if(hasVocoderRate())
        {
            int length = mAudioFrame.length + 9;

            byte[] data = createMessage(length, getType());
            int offset = CHANNEL_DATA_IDENTIFIER_INDEX;

            data[offset++] = PacketField.VOCODER.getCode();
            data[offset++] = mVocoderRate.getCode();

            data[offset++] = PacketField.CHANNEL_DATA_HARD_SYMBOL.getCode();
            data[offset++] = (byte)(0xFF & (mAudioFrame.length * 8));
            System.arraycopy(mAudioFrame, 0, data, offset, mAudioFrame.length);
            offset += mAudioFrame.length;
            data[offset++] = PacketField.SAMPLE_COUNT.getCode();
            data[offset++] = (byte)0xA0;
            data[offset++] = (byte)0x02;
            data[offset++] = (byte)0x00;
            data[offset] = (byte)0x00;

            return data;
        }
        else
        {
            int length = mAudioFrame.length + 2;
            byte[] data = createMessage(length, getType());

            int offset = CHANNEL_DATA_IDENTIFIER_INDEX;
            data[offset++] = PacketField.CHANNEL_DATA_HARD_SYMBOL.getCode();
            data[offset++] = (byte)(0xFF & (mAudioFrame.length * 8));
            System.arraycopy(mAudioFrame, 0, data, offset, mAudioFrame.length);

            return data;
        }
    }
}
