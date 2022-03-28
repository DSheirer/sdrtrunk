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
 * Inbound (from Controller) Modular Request Message
 */
public class ModularRequestMessage extends MCGPPacket
{
    //This field is byte reversed (ie big endian)
    private static final int[] TARGET_UNIT_ID = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 16, 17, 18, 19, 20, 21, 22,
            23, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] COMMAND_NUMERATOR = new int[]{48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] PACKET_CONTROL = new int[]{88, 89, 90, 91, 92, 93, 94, 95};
    private static final int[] TOTAL_MESSAGE_LENGTH = new int[]{96, 97, 98, 99, 100, 101, 102, 103};
    private static final int[] SUB_DATA_1_MESSAGE_TYPE = new int[]{104, 105, 106, 107, 108, 109, 110, 111};

    private CellocatorRadioIdentifier mTargetRadioId;
    private PacketControl mPacketControl;

    /**
     * Constructs a parser for the message.
     *
     * @param header for this message
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public ModularRequestMessage(MCGPHeader header, CorrectedBinaryMessage message, int offset)
    {
        super(header, message, offset);
    }

    /**
     * Static access to message total length (bytes) field.
     * @param message containing modular request message
     * @param offset to start of modular request message
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
        if(mTargetRadioId == null)
        {
            int radioId = getMessage().getInt(TARGET_UNIT_ID, getOffset());
            mTargetRadioId = CellocatorRadioIdentifier.createTo(radioId);
        }

        return mTargetRadioId;
    }

    public PacketControl getPacketControl()
    {
        if(mPacketControl == null)
        {
            mPacketControl = new PacketControl(getMessage().getInt(PACKET_CONTROL, getOffset()));
        }

        return mPacketControl;
    }

    public int getCommandNumerator()
    {
        return getMessage().getInt(COMMAND_NUMERATOR, getOffset());
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
        sb.append(getRadioId());
        sb.append(" MODULAR REQUEST TYPE:").append(getModularDataType());
        sb.append(" MESSAGE NUMBER:").append(getCommandNumerator());
        return sb.toString();
    }
}
