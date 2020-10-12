/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.ip;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

public abstract class Packet implements IPacket
{
    private CorrectedBinaryMessage mMessage;
    private int mOffset;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public Packet(CorrectedBinaryMessage message, int offset)
    {
        mMessage = message;
        mOffset = offset;
    }

    /**
     * Underlying message that contains this packet
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Offset to this packet in the underlying message
     */
    public int getOffset()
    {
        return mOffset;
    }

    @Override
    public boolean hasPayload()
    {
        return getPayload() != null;
    }
}
