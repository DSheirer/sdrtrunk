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
package io.github.dsheirer.module.decode.dmr.message.data.mbc;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import java.util.Collections;
import java.util.List;

public class MBCContinuationBlock extends DataMessage
{
    private static final int LAST_BLOCK = 0;

    /**
     * DMR message frame.  This message is comprised of a 24-bit prefix and a 264-bit message frame.  Outbound base
     * station frames transmit a Common Announcement Channel (CACH) in the 24-bit prefix, whereas Mobile inbound frames
     * do not use the 24-bit prefix.
     *
     * @param syncPattern
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public MBCContinuationBlock(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                                long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("CC:").append(getSlotType().getColorCode());

        if(!isValid())
        {
            sb.append(" [CRC ERROR]");
        }

        sb.append(" MULTI-BLOCK CSBK CONTINUATION");

        if(isLastBlock())
        {
            sb.append("-FINAL");
        }

        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Indicates if this is the last/final block of a multi-block message fragment sequence.
     */
    public boolean isLastBlock()
    {
        return getMessage().get(LAST_BLOCK);
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
