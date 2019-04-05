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

/**
 * Set Vocoder parameters request packet
 */
public class SetVocoderParametersRequest extends AmbeRequest
{
    private int mWord0;
    private int mWord1;
    private int mWord2;
    private int mWord3;
    private int mWord4;
    private int mWord5;

    public SetVocoderParametersRequest(int word0, int word1, int word2, int word3, int word4, int word5)
    {
        mWord0 = word0;
        mWord1 = word1;
        mWord2 = word2;
        mWord3 = word3;
        mWord4 = word4;
        mWord5 = word5;
    }

    @Override
    public PacketField getType()
    {
        return PacketField.PKT_RATE_PARAMETER;
    }

    @Override
    public byte[] getData()
    {
        byte[] data = createMessage(14, getType());
        data[PAYLOAD_START_INDEX + 1] = (byte)(mWord0 >> 8 & 0xFF);
        data[PAYLOAD_START_INDEX + 2] = (byte)(mWord0 & 0xFF);
        data[PAYLOAD_START_INDEX + 3] = (byte)(mWord1 >> 8 & 0xFF);
        data[PAYLOAD_START_INDEX + 4] = (byte)(mWord1 & 0xFF);
        data[PAYLOAD_START_INDEX + 5] = (byte)(mWord2 >> 8 & 0xFF);
        data[PAYLOAD_START_INDEX + 6] = (byte)(mWord2 & 0xFF);
        data[PAYLOAD_START_INDEX + 7] = (byte)(mWord3 >> 8 & 0xFF);
        data[PAYLOAD_START_INDEX + 8] = (byte)(mWord3 & 0xFF);
        data[PAYLOAD_START_INDEX + 9] = (byte)(mWord4 >> 8 & 0xFF);
        data[PAYLOAD_START_INDEX + 10] = (byte)(mWord4 & 0xFF);
        data[PAYLOAD_START_INDEX + 11] = (byte)(mWord5 >> 8 & 0xFF);
        data[PAYLOAD_START_INDEX + 12] = (byte)(mWord5 & 0xFF);

        return data;
    }
}
