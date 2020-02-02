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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCOpcode;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;

import java.util.Collections;
import java.util.List;

/**
 * Unknown full link control message
 */
public class UnknownFullLCMessage extends FullLCMessage
{
    /**
     * Constructs an instance
     *
     * @param message containing link control data
     */
    public UnknownFullLCMessage(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("FLC UNKNOWN");
        LCOpcode opcode = getOpcode();

        if(opcode != LCOpcode.FULL_STANDARD_UNKNOWN)
        {
            sb.append(" ").append(opcode);
        }
        else
        {
            sb.append(" OPCODE:").append(getOpcodeValue());
        }

        Vendor vendor = getVendor();

        if(vendor != Vendor.STANDARD)
        {
            if(vendor == Vendor.UNKNOWN)
            {
                sb.append(" VENDOR:").append(getVendorValue());
            }
            else
            {
                sb.append(" VENDOR:").append(vendor);
            }
        }

        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
