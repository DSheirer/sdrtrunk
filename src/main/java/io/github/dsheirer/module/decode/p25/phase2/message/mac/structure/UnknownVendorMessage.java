/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.Collections;
import java.util.List;

/**
 * Unknown MAC Opcode Structure.
 */
public class UnknownVendorMessage extends MacStructure
{
    private static final int[] VENDOR = {8, 9, 10, 11, 12, 13, 14, 15};

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset bit index to the start of this structure
     */
    public UnknownVendorMessage(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    public Vendor getVendor()
    {
        return Vendor.fromValue(getMessage().getInt(VENDOR, getOffset()));
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" VENDOR:").append(getVendor());
        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
