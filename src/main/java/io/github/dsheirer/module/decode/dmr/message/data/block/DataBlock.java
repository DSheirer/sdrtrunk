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

package io.github.dsheirer.module.decode.dmr.message.data.block;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.DataMessage;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import java.util.Collections;
import java.util.List;

/**
 * Base class for data blocks using 1/2, 3/4, and 1/1 trellis coded data payloads
 */
public abstract class DataBlock extends DataMessage
{
    private static final int[] DATA_BLOCK_SERIAL_NUMBER = new int[]{0, 1, 2, 3, 4, 5, 6};

    /**
     * Constructs an instance.
     *
     * @param syncPattern either BASE_STATION_DATA or MOBILE_STATION_DATA
     * @param message containing extracted 196-bit payload.
     * @param cach for the DMR burst
     * @param slotType for this data message
     * @param timestamp message was received
     * @param timeslot for the DMR burst
     */
    public DataBlock(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                     long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Data block payload for confirmed delivery.
     */
    public abstract CorrectedBinaryMessage getConfirmedPayload();

    /**
     * Data block payload for unconfirmed delivery
     */
    public abstract CorrectedBinaryMessage getUnConfirmedPayload();

    /**
     * Serial number for a confirmed data block
     */
    public int getDataBlockSerialNumber()
    {
        return getMessage().getInt(DATA_BLOCK_SERIAL_NUMBER);
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(" ").append(getSlotType());
        sb.append(" ").append(getMessage().toHexString());

        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
