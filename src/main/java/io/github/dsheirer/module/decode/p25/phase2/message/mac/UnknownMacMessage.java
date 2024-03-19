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

package io.github.dsheirer.module.decode.p25.phase2.message.mac;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.UnknownMacStructure;
import java.util.Collections;
import java.util.List;

/**
 * Unknown MAC Message.
 */
public class UnknownMacMessage extends MacMessage
{

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param timestamp of the final bit of the message
     */
    public UnknownMacMessage(int timeslot, DataUnitID dataUnitID, CorrectedBinaryMessage message, long timestamp)
    {
        super(timeslot, dataUnitID, message, timestamp, new UnknownMacStructure(message, 0));
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());
        sb.append(" ").append(getDataUnitID());

        if(!isValid())
        {
            sb.append(" [CRC ERROR]");
        }

        sb.append(" ").append(getMacPduType().toString());
        sb.append(" ").append(getMacStructure().toString());

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
