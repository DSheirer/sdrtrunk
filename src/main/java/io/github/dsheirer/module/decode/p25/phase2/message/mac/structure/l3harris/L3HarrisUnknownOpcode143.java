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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import java.util.ArrayList;
import java.util.List;

/**
 * L3Harris Unknown Opcode 143 (0x8F).
 *
 * Observed on L3Harris control channel, continuously transmitted and the message content doesn't change for the
 * same control channel.
 *
 * 8FA4070A44800A on WACN:0x91F14, SYS 0x2D7, SITE 0x0A, LRA 0x0A (recorded late in ~2020)
 * 8FA4070A43800A on WACN:0x91F14, SYS 0x201, SITE 0x18, LRA 0x18 (recorded in Feb 2024)
 *          ^
 */
public class L3HarrisUnknownOpcode143 extends MacStructureVendor
{
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public L3HarrisUnknownOpcode143(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("L3HARRIS UNKNOWN OPCODE 143 MSG:").append(getMessage()
                .getSubMessage(getOffset(), getOffset() + (getLength() * 8)).toHexString());
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
