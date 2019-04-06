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

import io.github.dsheirer.audio.convert.thumbdv.message.AmbeMessage;
import io.github.dsheirer.audio.convert.thumbdv.message.PacketField;

/**
 * AMBE-3000R Request Packet
 */
public abstract class AmbeRequest extends AmbeMessage
{
    private static final byte PACKET_START_BYTE = (byte)0x61;
    protected static final int PACKET_TYPE_INDEX = 3;
    protected static final int PAYLOAD_START_INDEX = 4;

    public abstract PacketField getType();
    public abstract byte[] getData();

    /**
     * Creates a byte array of the specified length plus 4 packet header bytes with packet start, length and control
     * bytes filled in.
     *
     * @param length of the message (not including the 4 byte packet header)
     * @return byte array with packet header pre-filled.
     */
    protected byte[] createMessage(int length, PacketField type)
    {
        byte[] data = new byte[length + 4];

        data[0] = PACKET_START_BYTE;
        data[1] = (byte)((length >> 8 & 0xFF));
        data[2] = (byte)(length & 0xFF);

        if(type == PacketField.PACKET_TYPE_ENCODE_SPEECH || type == PacketField.PACKET_TYPE_DECODE_SPEECH)
        {
            data[PACKET_TYPE_INDEX] = type.getCode();
        }
        else
        {
            data[PACKET_TYPE_INDEX] = PacketField.PACKET_TYPE_CONTROL.getCode();
            data[PAYLOAD_START_INDEX] = type.getCode();
        }

        return data;
    }
}
