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
import io.github.dsheirer.module.decode.p25.reference.Vendor;

/**
 * Base class for custom vendor implementations
 */
public abstract class MacStructureVendor extends MacStructureVariableLength
{
    private static final IntField VENDOR = IntField.length8(OCTET_2_BIT_8);
    private static final IntField LENGTH = IntField.range(18, 23);

    /**
     * Constructs a MAC structure parser
     *
     * @param message containing a MAC structure
     * @param offset in the message to the start of the structure
     */
    protected MacStructureVendor(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Overrides the primary method to ensure we get the correct vendor mac opcode representation
     */
    @Override
    public MacOpcode getOpcode()
    {
        return MacOpcode.fromValue(getOpcodeNumber(), getVendor());
    }

    /**
     * Utility method to lookup vendor specified for this message
     * @param message bits
     * @param offset to the start of the structure
     * @return vendor or UNKNOWN.
     */
    public static Vendor getVendor(CorrectedBinaryMessage message, int offset)
    {
        return Vendor.fromValue(message.getInt(VENDOR, offset));
    }

    /**
     * Vendor specified for this message
     * @return vendor or UNKNOWN.
     */
    public Vendor getVendor()
    {
        return getVendor(getMessage(), getOffset());
    }

    /**
     * ID number for the vendor.
     */
    public int getVendorID()
    {
        return getInt(VENDOR);
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
}
