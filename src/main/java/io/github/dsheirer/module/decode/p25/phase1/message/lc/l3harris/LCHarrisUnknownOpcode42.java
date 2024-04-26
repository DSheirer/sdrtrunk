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

package io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import java.util.Collections;
import java.util.List;

/**
 * L3Harris Unknown Opcode 42 (0x2A)
 * <p>
 * Observed in a voice traffic channel carried by an LDU1.  This was transmitted once followed by an Opcode 43 (0x2B)
 * which is also unknown.
 */
public class LCHarrisUnknownOpcode42 extends LinkControlWord
{
    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCHarrisUnknownOpcode42(CorrectedBinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("**CRC-FAILED** ");
        }

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        else
        {
            sb.append("L3HARRIS UNKNOWN OPCODE 42 MSG:").append(getMessage().toHexString());
        }
        return sb.toString();
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
