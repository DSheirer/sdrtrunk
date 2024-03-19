/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import java.util.Collections;
import java.util.List;

/**
 * Multi-Fragment Continuation Message
 */
public class MultiFragmentContinuationMessage extends MacStructure
{
    private static final int[] LENGTH = {10, 11, 12, 13, 14, 15}; //Of this packet

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MultiFragmentContinuationMessage(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MULTI-FRAGMENT CONTINUATION MESSAGE LENGTH:").append(getLength());
        return sb.toString();
    }

    /**
     * Access the message field integer value from this continuation message.
     * @param indexes for the field.
     * @return integer value.
     */
    public int getInt(int[] indexes)
    {
        return getMessage().getInt(indexes, getOffset());
    }

    /**
     * Access the boolean field value.
     * @param index of the value in this message.  Note: this will be adjusted to the offset of this message.
     * @return boolean value.
     */
    public boolean get(int index)
    {
        return getMessage().get(index + getOffset());
    }

    /**
     * Length of this packet.
     * @return length in bytes.
     */
    public int getLength()
    {
        return getMessage().getInt(LENGTH, getOffset());
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
