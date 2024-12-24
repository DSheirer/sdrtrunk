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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Confirmed Data Header
 */
public class ConfirmedDataHeader extends OctetDataHeader
{
    private static final int RE_SYNCHRONIZE_FLAG = 72;
    private static final int[] SEND_SEQUENCE_NUMBER = new int[]{73, 74, 75};

    private List<Identifier> mIdentifiers;

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
    public ConfirmedDataHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CONFIRMED DATA HEADER");
        sb.append(" FM:").append(getSourceLLID());
        sb.append(" TO:").append(getDestinationLLID());
        sb.append(" ").append(getServiceAccessPoint());
        sb.append(" BLOCKS TO FOLLOW:").append(getBlocksToFollow());
        if(isReSynchronize())
        {
            sb.append(" (RESYNC)");
        }
        sb.append(" SEND SEQUENCE NUMBER:").append(getSendSequenceNumber());
        sb.append(" FRAGMENT SEQUENCE NUMBER:").append(getFragmentSequenceNumber());
        if(isFinalFragment())
        {
            sb.append("(FINAL)");
        }
        sb.append(" PAD OCTETS:").append(getPadOctetCount());
        return sb.toString();
    }

    /**
     * Send sequence number
     */
    public int getSendSequenceNumber()
    {
        return getMessage().getInt(SEND_SEQUENCE_NUMBER);
    }

    /**
     * Indicates if the receiver should re-synchronize by accepting this packet and its fragment sequence number as a
     * valid fragment and avoid ignoring this packet as a duplicate when the fragment sequence number was previously
     * received.
     * @return true if resynchronized is asserted
     */
    public boolean isReSynchronize()
    {
        return getMessage().get(RE_SYNCHRONIZE_FLAG);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getDestinationLLID());
            mIdentifiers.add(getSourceLLID());
        }

        return mIdentifiers;
    }
}
