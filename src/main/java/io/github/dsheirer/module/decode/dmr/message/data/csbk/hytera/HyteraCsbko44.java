/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.Collections;
import java.util.List;

/**
 * Hytera DMR Tier III - CSBKO 44 -
 *
 * Analysis: ... this was transmitted on the Tier III traffic channel during call termination.  This CSBK was
 * interleaved with every other Terminator message, starting after the first 2x terminator messages.  This continued
 * through the final teardown which was 3x Clear messages.
 *
 * On a second example, there were 8 distinct configurations of this message that were repeated in-order, multiple times
 * during the traffic channel shutdown ... a total of 51x of these messages were transmitted.
 */
public class HyteraCsbko44 extends CSBKMessage
{
    private static final int[] UNKNOWN_1 = new int[]{16, 17, 18, 19, 20, 21, 22, 23};

    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public HyteraCsbko44(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());
        if(hasRAS())
        {
            sb.append(" RAS:").append(getBPTCReservedBits());
        }
        sb.append(" HYTERA UNKNOWN CSBKO=44");
        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }
    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
