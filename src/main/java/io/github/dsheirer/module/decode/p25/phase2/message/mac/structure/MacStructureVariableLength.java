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
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;

/**
 * Base class for a variable length message.
 */
public abstract class MacStructureVariableLength extends MacStructure
{
    private static final IntField LENGTH = IntField.range(10, 15);

    /**
     * Constructs a MAC structure parser
     *
     * @param message containing a MAC structure
     * @param offset in the message to the start of the structure
     */
    protected MacStructureVariableLength(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Utility method to discover the length of this mac structure
     * @param message containing one or more mac structures
     * @param offset to the start of the mac structure
     * @return data length for the multi-fragment message
     */
    public static int getLength(CorrectedBinaryMessage message, int offset)
    {
        MacOpcode opcode = MacStructure.getOpcode(message, offset);

        switch(opcode)
        {
            case TDMA_11_INDIRECT_GROUP_PAGING_WITHOUT_PRIORITY:
                return IndirectGroupPagingWithoutPriority.getLength(message, offset);
            case TDMA_12_INDIVIDUAL_PAGING_WITH_PRIORITY:
                return IndividualPagingWithPriority.getLength(message, offset);
            default:
                return message.getInt(LENGTH, offset);
        }
    }

    /**
     * Length of this mac structure.
     * @return data length for this multi-fragment message
     */
    public int getLength()
    {
        return getLength(getMessage(), getOffset());
    }

    /**
     * Message fragment for this variable length message.
     */
    public CorrectedBinaryMessage getSubMessage()
    {
        return getMessage().getSubMessage(getOffset(), getOffset() + (getLength() * 8));
    }
}
