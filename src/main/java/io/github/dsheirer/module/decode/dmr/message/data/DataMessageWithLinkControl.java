/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.data;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.lc.LCMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.Collections;
import java.util.List;

/**
 * Data Message with Full Link Control Payload.
 */
public abstract class DataMessageWithLinkControl extends DataMessage
{
    protected LCMessage mLCMessage;

    /**
     * Constructs an instance.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message containing extracted 196-bit payload.
     * @param cach for the DMR burst
     * @param slotType for this data message
     * @param timestamp message was received
     * @param timeslot for the DMR burst
     * @param linkControl message
     */
    public DataMessageWithLinkControl(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach,
                                      SlotType slotType, long timestamp, int timeslot, LCMessage linkControl)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
        mLCMessage = linkControl;
    }

    /**
     * Access the embedded link control message
     */
    public LCMessage getLCMessage()
    {
        return mLCMessage;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(hasRAS())
        {
            sb.append("RAS:").append(getBPTCReservedBits()).append(" ");
        }
        sb.append(getSlotType());
        sb.append(" ").append(getLCMessage());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(getLCMessage() != null)
        {
            return getLCMessage().getIdentifiers();
        }

        return Collections.emptyList();
    }
}
