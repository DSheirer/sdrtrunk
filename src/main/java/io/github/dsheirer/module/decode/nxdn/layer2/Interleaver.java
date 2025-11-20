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

package io.github.dsheirer.module.decode.nxdn.layer2;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Base NXDN interleaver configuration.
 */
public class Interleaver
{
    private int[] mInterleaveIndexes;
    private int mWidth;
    private int mDepth;

    /**
     * Constructs an instance
     * @param width of the interleave
     * @param depth of the interleave
     */
    public Interleaver(int width, int depth)
    {
        mWidth = width;
        mDepth = depth;
        mInterleaveIndexes = new int[width * depth];

        int pointer = 0;

        for(int column = 0; column < width; column++)
        {
            for(int row = 0; row < depth; row++)
            {
                mInterleaveIndexes[pointer++] = (row * mWidth) + column;
            }
        }
    }

    /**
     * Performs interleave of the original message
     * @param message original
     * @param offset into the output message where the interleaved data should be placed
     * @return output message with interleaved payload positioned starting at the offset.
     */
    public CorrectedBinaryMessage interleave(CorrectedBinaryMessage message, int offset)
    {
        CorrectedBinaryMessage interleaved = new CorrectedBinaryMessage(mInterleaveIndexes.length + offset);

        for(int index = 0; index < mInterleaveIndexes.length; index++)
        {
            if(message.get(index + offset))
            {
                interleaved.set(offset + mInterleaveIndexes[index]);
            }
        }


        return interleaved;
    }

    /**
     * Performs deinterleave of the interleaved message
     * @param interleaved containing the interleaved payload starting at the offset
     * @param offset into the message to the start of the interleaved payload.
     * @return deinterleaved message starting at index 0
     */
    public CorrectedBinaryMessage deinterleave(CorrectedBinaryMessage interleaved, int offset)
    {
        CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(mInterleaveIndexes.length);

        for(int index = 0; index < mInterleaveIndexes.length; index++)
        {
            deinterleaved.set(mInterleaveIndexes[index], interleaved.get(index + offset));
        }

        return deinterleaved;
    }
}
