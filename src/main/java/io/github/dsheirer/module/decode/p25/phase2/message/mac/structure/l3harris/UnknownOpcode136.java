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
 * Unknown Opcode 136 (0x88)
 *
 * Observed on L3Harris control channel.  Opcode is in the vendor partition, but the message indicates a
 * standard (0x00) vendor ID.
 *
 * Doesn't seem to correlate with any activity.  Observed the following sequence of 4 messages repeatedly, normally
 * occurring in either the 1/3 or 3/3 superframe fragment, but this may be due to the MAC scheduler in the transmitter:
 *
 * 8800AB7BE
 * 8800A8952
 * 8800A6BBF
 * 8800A0F7B
 *
 * Observed on Duke Energy Ohio Site 0x0A
 */
public class UnknownOpcode136 extends MacStructureVendor
{
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public UnknownOpcode136(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("UNKNOWN OPCODE 136 VENDOR:STANDARD MSG:").append(getMessage().getSubMessage(getOffset(), getOffset() + 36).toHexString());
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
