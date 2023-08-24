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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.MBCContinuationBlock;
import io.github.dsheirer.module.decode.dmr.message.type.AbsoluteChannelParameters;
import io.github.dsheirer.module.decode.dmr.message.type.DataType;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.List;

/**
 * Tier III Announcement - Logical channel:frequency relationship.
 */
public class AnnounceChannelFrequency extends Announcement
{
    private AbsoluteChannelParameters mAbsoluteChannelParameters;
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
    public AnnounceChannelFrequency(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                                    long timestamp, int timeslot, MBCContinuationBlock multiBlock)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);

        if(multiBlock != null)
        {
            //Timeslot hard-coded to one
            mAbsoluteChannelParameters = new AbsoluteChannelParameters(multiBlock.getMessage(), 0, 1);
        }
    }

    /**
     * Checks CRC and sets the message valid flag according to the results.
     */
    public void checkCRC()
    {
        if(getSlotType().getDataType() == DataType.MBC_HEADER)
        {
            checkMultiBlockCRC(getAbsoluteChannelParameters());
        }
        else
        {
            //Use the standard CSBK CRC check
            super.checkCRC();
        }
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
        sb.append(" ANNOUNCE CHANNEL");
        if(hasAbsoluteChannelParameters())
        {
            sb.append(getAbsoluteChannelParameters().getChannel());
            sb.append(" CC:").append(getAbsoluteChannelParameters().getColorCode());
        }

        return sb.toString();
    }

    /**
     * Absolute channel parameters structure
     */
    public AbsoluteChannelParameters getAbsoluteChannelParameters()
    {
        return mAbsoluteChannelParameters;
    }

    /**
     * Indicates if this message has an absolute channel parameters structure
     */
    public boolean hasAbsoluteChannelParameters()
    {
        return getAbsoluteChannelParameters() != null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(hasAbsoluteChannelParameters())
            {
                mIdentifiers.add(getAbsoluteChannelParameters().getChannel());
            }
        }

        return mIdentifiers;
    }
}
