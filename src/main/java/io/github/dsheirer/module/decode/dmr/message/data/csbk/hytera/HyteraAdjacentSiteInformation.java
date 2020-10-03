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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.hytera;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement.AdjacentSiteInformation;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.MBCContinuationBlock;

/**
 * Hytera Neighbor/Adjacent Site Information
 */
public class HyteraAdjacentSiteInformation extends AdjacentSiteInformation
{
    /**
     * Constructs a multi-block MBC instance
     * @param syncPattern for the burst
     * @param message from the burst
     * @param cach from the burst
     * @param slotType for the burst
     * @param timestamp of the burst
     * @param timeslot of the burst
     * @param multiBlock containing absolute frequency parameters
     */
    public HyteraAdjacentSiteInformation(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach,
                                         SlotType slotType, long timestamp, int timeslot, MBCContinuationBlock multiBlock)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot, multiBlock);
    }

    /**
     * Constructs a single-block CSBK instance
     * @param syncPattern for the burst
     * @param message from the burst
     * @param cach from the burst
     * @param slotType for the burst
     * @param timestamp of the burst
     * @param timeslot of the burst
     */
    public HyteraAdjacentSiteInformation(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach,
                                         SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" HYTERA NEIGHBOR ").append(getSystemIdentityCode().getModel());
        sb.append(" NETWORK:").append(getNeighborSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getNeighborSystemIdentityCode().getSite());
        if(hasAbsoluteChannelParameters())
        {
            sb.append(" CHAN:").append(getAbsoluteChannelParameters().getChannel());
            sb.append(" CC:").append(getAbsoluteChannelParameters().getColorCode());
        }
        else
        {
            sb.append(" CHAN:").append(getNeighborChannelNumber());
        }
        sb.append(" THIS NETWORK:").append(getSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getSystemIdentityCode().getSite());
        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }
}
