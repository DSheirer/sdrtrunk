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

package io.github.dsheirer.module.decode.dmr.message.type;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Base structure class for a structure contained within a binary message
 */
public abstract class AbstractStructure
{
    private CorrectedBinaryMessage mMessage;
    private int mOffset;

    /**
     * Constructs an instance
     * @param message that contains this structure
     * @param offset into the message to the start of this structure
     */
    public AbstractStructure(CorrectedBinaryMessage message, int offset)
    {
        mMessage = message;
        mOffset = offset;
    }

    /**
     * Message that contains this structure
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Offset into the message where this structure starts
     */
    public int getOffset()
    {
        return mOffset;
    }
}
