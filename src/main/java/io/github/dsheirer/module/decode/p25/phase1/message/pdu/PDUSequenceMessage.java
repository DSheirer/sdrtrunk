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

package io.github.dsheirer.module.decode.p25.phase1.message.pdu;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import java.util.Collections;
import java.util.List;

public class PDUSequenceMessage extends P25P1Message
{
    private PDUSequence mPDUSequence;

    public PDUSequenceMessage(PDUSequence PDUSequence, int nac, long timestamp)
    {
        super(null, nac, timestamp);
        mPDUSequence = PDUSequence;
    }

    public PDUSequence getPDUSequence()
    {
        return mPDUSequence;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getPDUSequence().toString());
        return sb.toString();
    }

    @Override
    public P25P1DataUnitID getDUID()
    {
        return P25P1DataUnitID.PACKET_DATA_UNIT;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
