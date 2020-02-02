/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Unit (Outbound) Programming Status/Response
 */
public class ProgrammingStatusMessage extends MCGPPacket
{
    //This field is byte reversed (ie big endian)
    private static final int[] SOURCE_UNIT_ID = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 16, 17, 18, 19, 20, 21, 22,
            23, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] COMMUNICATION_CONTROL = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45,
            46, 47};
    private static final int[] MESSAGE_NUMERATOR = new int[]{48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SPARE = new int[]{56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] BLOCK_CODE = new int[]{64, 65, 66, 67, 68, 69, 70, 71};
    private static final int DATA_BLOCK_START = 72;
    private static final int DATA_BLOCK_END = DATA_BLOCK_START + 128;

    private CellocatorRadioIdentifier mSourceRadioId;
    private CommunicationControl mCommunicationControl;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param header for this message
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public ProgrammingStatusMessage(MCGPHeader header, CorrectedBinaryMessage message, int offset)
    {
        super(header, message, offset);
    }

    public CellocatorRadioIdentifier getRadioId()
    {
        if(mSourceRadioId == null)
        {
            mSourceRadioId = CellocatorRadioIdentifier.createTo(getMessage().getInt(SOURCE_UNIT_ID, getOffset()));
        }

        return mSourceRadioId;
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

    public int getBlockCode()
    {
        return getMessage().getInt(BLOCK_CODE, getOffset());
    }

    public String getDataBlock()
    {
        return getMessage().getSubMessage(DATA_BLOCK_START + getOffset(), DATA_BLOCK_END + getOffset()).toHexString();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CELLOCATOR RADIO:");
        sb.append(getRadioId());
        sb.append(" PROGRAM MESSAGE NUMBER:").append(getMessageNumerator());
        sb.append(" BLOCK:").append(getBlockCode());
        sb.append(" DATA:").append(getDataBlock());
        return sb.toString();
    }
}
