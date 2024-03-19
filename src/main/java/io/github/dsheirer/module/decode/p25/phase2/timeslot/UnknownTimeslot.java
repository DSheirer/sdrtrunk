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

package io.github.dsheirer.module.decode.p25.phase2.timeslot;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import java.util.Collections;
import java.util.List;

/**
 * Unknown timeslot - usually happens when we decode the Data Unit ID incorrectly.
 */
public class UnknownTimeslot extends Timeslot
{
    public UnknownTimeslot(CorrectedBinaryMessage message, int timeslot,  long timestamp, DataUnitID dataUnitID)
    {
        super(message, dataUnitID, timeslot, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());
        sb.append(" UNKNOWN TIMESLOT - DATA UNIT ID: ").append(getDataUnitID());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
