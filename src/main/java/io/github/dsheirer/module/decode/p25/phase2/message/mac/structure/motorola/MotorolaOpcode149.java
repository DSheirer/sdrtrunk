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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Unknown Opcode 149 (0x95)
 *
 * This was observed at the end of a Group Regroup call sequence during HANGTIME, so it seems more of an announcement
 * type message and less as something relevant to the call.
 *
 * Examples:
 * 959011 018E58B82BAB4D3B70E9A8457F9D C67
 * 959011 0287000000000000000000000000 E26
 *
 * The opcode, vendor and length octets are consistent.  The first octet seems to be an identifier, 01, 02, etc.
 * Nothing else seems to match any of the other identifiers that were active at call time.
 */
public class MotorolaOpcode149 extends MacStructureVendor
{
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaOpcode149(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA UNKNOWN OPCODE 149");
        sb.append(" MSG:").append(getMessage().get(getOffset(), getMessage().length()).toHexString());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
        }

        return mIdentifiers;
    }
}
