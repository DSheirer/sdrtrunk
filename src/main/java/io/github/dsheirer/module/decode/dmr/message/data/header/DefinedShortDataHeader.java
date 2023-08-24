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

package io.github.dsheirer.module.decode.dmr.message.data.header;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.DefinedDataFormat;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Defined Short Data Header
 */
public class DefinedShortDataHeader extends PacketSequenceHeader
{
    private static final int[] BLOCKS_TO_FOLLOW = new int[]{2, 3, 12, 13, 14, 15};
    private static final int[] DEFINED_DATA_FORMAT = new int[]{64, 65, 66, 67, 68, 69};
    private static final int RESYNCHRONIZE_FLAG = 70;
    private static final int FULL_MESSAGE_FLAG = 71;
    private static final int[] BIT_PADDING = new int[]{72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] HEADER_CRC = new int[]{80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};

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
    public DefinedShortDataHeader(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(isResponseRequested() ? " CONFIRMED" : " UNCONFIRMED");
        sb.append(" DEFINED SHORT DATA HEADER");
        sb.append(" FM:").append(getSourceLLID());
        sb.append(" TO:").append(getDestinationLLID());
        sb.append(" FORMAT:").append(getDefinedDataFormat());
        sb.append(" ").append(getServiceAccessPoint());
        if(isFullMessage())
        {
            sb.append(" FULL MESSAGE");
        }
        if(isResynchronize())
        {
            sb.append(" RESYNCHRONIZE");
        }
        sb.append(" BIT PADDING:").append(getBitPadding());
        sb.append(" BLOCKS TO FOLLOW:").append(getBlocksToFollow());
        return sb.toString();
    }

    /**
     * Format for the data payload for this packet sequence.
     */
    public DefinedDataFormat getDefinedDataFormat()
    {
        return DefinedDataFormat.fromValue(getMessage().getInt(DEFINED_DATA_FORMAT));
    }

    /**
     * Indicates if this is a full message
     */
    public boolean isFullMessage()
    {
        return getMessage().get(FULL_MESSAGE_FLAG);
    }

    /**
     * Indicates if this is resynchronized data
     */
    public boolean isResynchronize()
    {
        return getMessage().get((RESYNCHRONIZE_FLAG));
    }

    /**
     * Bit padding value.
     */
    public int getBitPadding()
    {
        return getMessage().getInt(BIT_PADDING);
    }

    /**
     * Number of appended blocks
     */
    @Override
    public int getBlocksToFollow()
    {
        return getMessage().getInt(BLOCKS_TO_FOLLOW);
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
