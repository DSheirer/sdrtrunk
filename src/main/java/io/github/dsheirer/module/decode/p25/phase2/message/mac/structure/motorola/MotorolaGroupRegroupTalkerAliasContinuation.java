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
 * Motorola Unknown Opcode 149 (0x95) Talker Alias continuation message.
 *
 * This was observed at the end of a Group Regroup call sequence during HANGTIME.  Each radio doesn't know the alias
 * until it receives it once and then queues the alias for subsequent use.
 *
 * This opcode is observed following an Opcode 145 message and seems to be a continuation message.
 *
 * Examples:
 * 959011 018E58B82BAB4D3B70E9A8457F9D C67
 * 959011 0287000000000000000000000000 E26
 *
 * Example 2:
 * 9190110BCD02010026BEE004A403151724FD9 Opcode 145
 * 959011012436C4C022EC902A9943EDAA69E76 Opcode 149
 * 95901102298FAA77683FAE0000000000000AB Opcode 149
 * (immediately followed in the same call sequence by a slight variation)
 * 9190110BCD02010036BEE004A403151724513 Opcode 145
 * 959011013436C4C022EC902A9943EDAA699F6 Opcode 149
 * 95901102398FAA77683FAE00000000000072B Opcode 149
 *
 * The opcode, vendor and length octets are consistent.  The first octet seems to be a sequence identifier, 01, 02, etc.
 * Nothing else seems to match any of the other identifiers that were active at call time.
 */
public class MotorolaGroupRegroupTalkerAliasContinuation extends MacStructureVendor
{
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaGroupRegroupTalkerAliasContinuation(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA TALKER ALIAS CONTINUATION");
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
