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

package io.github.dsheirer.module.decode.dmr.message.data.header;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;

/**
 * Packet sequence header that contains octet (byte) data
 *
 * Note: this is the base header for Confirmed, Unconfirmed and Short Data
 */
public abstract class OctetDataHeader extends PacketSequenceHeader
{
    private static final int[] PAD_OCTET_COUNT = new int[]{3, 12, 13, 14, 15};
    private static final int FINAL_FRAGMENT_FLAG = 64;
    private static final int[] BLOCKS_TO_FOLLOW = new int[]{65, 66, 67, 68, 69, 70, 71};
    private static final int[] FRAGMENT_SEQUENCE_NUMBER = new int[]{76, 77, 78, 79};

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
    public OctetDataHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Fragment sequence number
     */
    public int getFragmentSequenceNumber()
    {
        return getMessage().getInt(FRAGMENT_SEQUENCE_NUMBER);
    }


    /**
     * Number of data blocks to follow
     */
    public int getBlocksToFollow()
    {
        return getMessage().getInt(BLOCKS_TO_FOLLOW);
    }

    /**
     * Indicates if this is the final fragment in a fragment sequence
     */
    public boolean isFinalFragment()
    {
        return getMessage().get(FINAL_FRAGMENT_FLAG);
    }

    /**
     * Number of octets (bytes) that are padded onto the last packet fragment to form a complete fragment.
     */
    public int getPadOctetCount()
    {
        return getMessage().getInt(PAD_OCTET_COUNT);
    }
}
