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
import org.apache.commons.lang3.StringUtils;

/**
 * Controller (Inbound) Programming Command
 */
public class ProgrammingCommandMessage extends MCGPPacket
{
    //This field is byte reversed (ie big endian)
    private static final int[] TARGET_UNIT_ID = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 16, 17, 18, 19, 20, 21, 22,
        23, 8, 9, 10, 11, 12, 13, 14, 15, 0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] COMMAND_NUMERATOR = new int[]{32, 33, 34, 35, 36, 37, 38, 39};
    //This field is (possibly) byte reversed (ie big endian)
    private static final int[] AUTHENTICATION_CODE = new int[]{64, 65, 66, 67, 68, 69, 70, 71, 56, 57, 58, 59, 60, 61,
        62, 63, 48, 49, 50, 51, 52, 53, 54, 55, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] BLOCK_CODE = new int[]{72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] BITMASK = new int[]{80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};
    private static final int DATA_BLOCK_START = 96;
    private static final int DATA_BLOCK_END = 224;

    private CellocatorRadioIdentifier mTargetRadioId;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param header for this message
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public ProgrammingCommandMessage(MCGPHeader header, CorrectedBinaryMessage message, int offset)
    {
        super(header, message, offset);
    }

    public CellocatorRadioIdentifier getRadioId()
    {
        if(mTargetRadioId == null)
        {
            mTargetRadioId = CellocatorRadioIdentifier.createTo(getMessage().getInt(TARGET_UNIT_ID, getOffset()));
        }

        return mTargetRadioId;
    }

    public int getCommandNumerator()
    {
        return getMessage().getInt(COMMAND_NUMERATOR, getOffset());
    }

    public String getAuthenticationCode()
    {
        return Integer.toHexString(getMessage().getInt(AUTHENTICATION_CODE, getOffset())).toUpperCase();
    }

    public int getBlockCode()
    {
        return getMessage().getInt(BLOCK_CODE, getOffset());
    }

    public String getBitMask()
    {
        return StringUtils.leftPad(Integer.toHexString(getMessage().getInt(BITMASK, getOffset())).toUpperCase(), 4, "0");
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
        sb.append(" PROGRAM SEQUENCE NUMBER:").append(getCommandNumerator());
        sb.append(" AUTH:").append(getAuthenticationCode());
        sb.append(" BLOCK:").append(getBlockCode());
        sb.append(" BITMASK:").append(getBitMask());
        sb.append(" DATA:").append(getDataBlock());
        return sb.toString();
    }
}
