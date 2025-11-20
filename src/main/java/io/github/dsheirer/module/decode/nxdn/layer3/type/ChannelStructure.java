/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.type;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;

/**
 * Channel Structure Information field.
 */
public class ChannelStructure
{
    private static final IntField BN_BCCH_FRAMES_PER_SUPERFRAME = IntField.length2(0);
    private static final IntField GN_GROUPS_PER_RCCH = IntField.length3(2);
    private static final IntField PN_PAGING_FRAMES = IntField.length4(5);
    private static final IntField MN_MULTIPURPOSE_FRAMES = IntField.length3(9);
    private static final IntField IN_GROUP_ITERATIONS_IN_SUPERFRAME = IntField.length4(12);

    private final CorrectedBinaryMessage mMessage;
    private final int mOffset;

    /**
     * Constructs an instance
     * @param message containing the field
     * @param offset to the start of the field.
     */
    public ChannelStructure(CorrectedBinaryMessage message, int offset)
    {
        mMessage = message;
        mOffset = offset;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CHANNEL STRUCTURE BN:").append(getNumberOfBCCHFramesPerSuperFrame());
        sb.append(" GN:").append(getNumberOfGroupsPerRCCH());
        sb.append(" PN:").append(getNumberOfPagingFrames());
        sb.append(" MN:").append(getNumberOfMultiPurposeFrames());
        sb.append(" IN:").append(getNumberOfGroupIterationsPerSuperframe());
        return sb.toString();
    }

    /**
     * Number of BCCH frames per superframe.
     * @return number
     */
    public int getNumberOfBCCHFramesPerSuperFrame()
    {
        return mMessage.getInt(BN_BCCH_FRAMES_PER_SUPERFRAME, mOffset);
    }

    /**
     * Number of groups per RCCH
     * @return number
     */
    public int getNumberOfGroupsPerRCCH()
    {
        return mMessage.getInt(GN_GROUPS_PER_RCCH, mOffset);
    }

    /**
     * Number of paging frames
     * @return number
     */
    public int getNumberOfPagingFrames()
    {
        return mMessage.getInt(PN_PAGING_FRAMES, mOffset);
    }

    /**
     * Number of multipurpose frames
     * @return number
     */
    public int getNumberOfMultiPurposeFrames()
    {
        return mMessage.getInt(MN_MULTIPURPOSE_FRAMES, mOffset);
    }

    /**
     * Number of iterations of groups within one superframe
     * @return number
     */
    public int getNumberOfGroupIterationsPerSuperframe()
    {
        return mMessage.getInt(IN_GROUP_ITERATIONS_IN_SUPERFRAME, mOffset);
    }
}
