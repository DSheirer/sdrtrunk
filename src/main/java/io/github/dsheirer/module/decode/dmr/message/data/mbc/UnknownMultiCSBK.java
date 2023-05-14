/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.mbc;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.Opcode;
import io.github.dsheirer.module.decode.dmr.message.data.header.MBCHeader;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;
import java.util.Collections;
import java.util.List;

/**
 * Unknown Multi-Block CSBK Message
 */
public class UnknownMultiCSBK extends MultiCSBK
{
    /**
     * Constructs an instance
     *
     * @param header
     * @param continuationBlocks of the message
     */
    public UnknownMultiCSBK(MBCHeader header, List<MBCContinuationBlock> continuationBlocks)
    {
        super(header, continuationBlocks);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());
        if(hasRAS())
        {
            sb.append(" RAS:").append(getBPTCReservedBits());
        }
        sb.append(" CSBK *UNKNOWN*");

        Vendor vendor = getVendor();

        if(vendor == Vendor.UNKNOWN)
        {
            sb.append(" VENDOR:").append(getVendorID());
        }
        else if(vendor != Vendor.STANDARD)
        {
            sb.append(" ").append(vendor);
        }

        Opcode opcode = getOpcode();

        if(opcode != Opcode.UNKNOWN)
        {
            sb.append(" ").append(opcode);
        }
        else
        {
            sb.append(" UNKNOWN OPCODE:").append(getOpcodeValue());
        }

        sb.append(" MSG:").append(getMessage().toHexString());

        for(MBCContinuationBlock block: mBlocks)
        {
            sb.append(block.getMessage().toHexString());
        }

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
