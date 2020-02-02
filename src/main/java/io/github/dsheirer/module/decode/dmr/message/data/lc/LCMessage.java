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

package io.github.dsheirer.module.decode.dmr.message.data.lc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.DMRMessage;
import io.github.dsheirer.module.decode.dmr.message.type.Vendor;

import java.util.List;

/**
 * Base Link Control class
 */
public abstract class LCMessage extends DMRMessage
{
    /**
     * Constructs an instance
     * @param message containing link control data
     */
    public LCMessage(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    /**
     * Link control opcode
     */
    public abstract LCOpcode getOpcode();

    /**
     * Link control vendor (feature ID)
     */
    public abstract Vendor getVendor();

    /**
     * Access the identifiers contained in the link control message
     */
    public abstract List<Identifier> getIdentifiers();

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(isValid())
        {
            sb.append("LINK CONTROL ");
            sb.append(" ").append(getOpcode());
            sb.append(" ").append(getMessage().toHexString());
        }
        else
        {
            sb.append("INVALID LINK CONTROL");
        }

        return sb.toString();
    }
}
