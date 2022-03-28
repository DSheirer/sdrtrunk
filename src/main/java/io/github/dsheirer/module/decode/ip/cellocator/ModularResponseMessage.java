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

package io.github.dsheirer.module.decode.ip.cellocator;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Modular Message
 */
public class ModularResponseMessage extends MCGPPacket
{
    //This field is byte reversed (ie big endian)
    private static final int[] UNIT_ID = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 16, 17, 18, 19, 20, 21, 22,
            23, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] COMMUNICATION_CONTROL = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45,
            46, 47};
    private static final int[] MESSAGE_NUMERATOR = new int[]{48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] PACKET_CONTROL = new int[]{56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] TOTAL_MESSAGE_LENGTH = new int[]{64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] SUB_DATA_1_MESSAGE_TYPE = new int[]{72, 73, 74, 75, 76, 77, 78, 79};

    private CellocatorRadioIdentifier mRadioId;
    private CommunicationControl mCommunicationControl;
    private PacketControl mPacketControl;

    /**
     * Constructs a parser for the message.
     *
     * @param header for this message
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public ModularResponseMessage(MCGPHeader header, CorrectedBinaryMessage message, int offset)
    {
        super(header, message, offset);
    }

    /**
     * Static access to message total length (bytes) field.
     * @param message containing modular response message
     * @param offset to start of modular response message
     * @return value from the total message length field
     */
    public static int getTotalMessageLength(BinaryMessage message, int offset)
    {
        return message.getInt(TOTAL_MESSAGE_LENGTH, offset);
    }

    /**
     * Radio/Unit identifier, either source or target as specified in the PacketControl field.
     */
    public CellocatorRadioIdentifier getRadioId()
    {
        if(mRadioId == null)
        {
            int radioId = getMessage().getInt(UNIT_ID, getOffset());

            if(getPacketControl().getDirection() == PacketControl.Direction.FROM_UNIT)
            {
                mRadioId = CellocatorRadioIdentifier.createFrom(radioId);
            }
            else
            {
                mRadioId = CellocatorRadioIdentifier.createTo(radioId);
            }
        }

        return mRadioId;
    }

    public PacketControl getPacketControl()
    {
        if(mPacketControl == null)
        {
            mPacketControl = new PacketControl(getMessage().getInt(PACKET_CONTROL, getOffset()));
        }

        return mPacketControl;
    }

    public CommunicationControl getCommunicationControl()
    {
        if(mCommunicationControl == null)
        {
            mCommunicationControl = new CommunicationControl(getMessage().getInt(COMMUNICATION_CONTROL, getOffset()));
        }

        return mCommunicationControl;
    }

    public int getMessageNumerator()
    {
        return getMessage().getInt(MESSAGE_NUMERATOR, getOffset());
    }

    /**
     * Modular data type for the first sub-chunk
     */
    public ModularDataType getModularDataType()
    {
        return ModularDataType.fromValue(getMessage().getInt(SUB_DATA_1_MESSAGE_TYPE, getOffset()));
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CELLOCATOR RADIO:");
        if(getPacketControl().getDirection() == PacketControl.Direction.FROM_UNIT)
        {
            sb.append(" FROM:");
        }
        else
        {
            sb.append(" TO:");
        }
        sb.append(getRadioId());
        sb.append(" MODULAR TYPE:").append(getModularDataType());
        sb.append(" MESSAGE NUMBER:").append(getMessageNumerator());
        return sb.toString();
    }
}
