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

package io.github.dsheirer.module.decode.p25.phase2.message.isch;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.message.P25P2Message;
import java.util.Collections;
import java.util.List;

/**
 * Synchronizing Inter-Slot Signaling Channel (S-ISCH)
 */
public class SISCH extends P25P2Message
{
    /**
     * Constructs the message
     *
     * @param message
     * @param offset
     * @param timeslot
     * @param timestamp of the final bit of the message
     */
    public SISCH(CorrectedBinaryMessage message, int offset, int timeslot, long timestamp)
    {
        super(message, offset, timeslot, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());
        sb.append(" S-ISCH ").append(getMessage().toHexString());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
