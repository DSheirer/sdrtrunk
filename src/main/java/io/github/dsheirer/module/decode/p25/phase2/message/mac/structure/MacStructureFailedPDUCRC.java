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
import java.util.List;

/**
 * MAC structure to indicate that the MAC PDU CRC-12 or CRC-16 check failed and the contents of the MAC PDU are invalid
 * or untrustworthy.
 */
public class MacStructureFailedPDUCRC extends MacStructure
{
    /**
     * Constructs a MAC structure parser
     *
     * @param message containing a MAC structure
     * @param offset in the message to the start of the structure
     */
    public MacStructureFailedPDUCRC(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    @Override
    public String toString()
    {
        return "[CRC-FAILED] MAC MESSAGE PDU CONTENT FAILED CRC CHECK";
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return null;
    }
}
