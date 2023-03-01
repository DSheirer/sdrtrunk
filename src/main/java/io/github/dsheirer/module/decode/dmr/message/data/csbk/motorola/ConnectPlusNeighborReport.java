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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola;

import com.google.common.base.Joiner;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.site.SiteIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRSite;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Connect Plus - Neighbor Report
 */
public class ConnectPlusNeighborReport extends CSBKMessage
{
    private static final int SITE_ARRAY_START = 16;
    private static final int[] UNKNOWN = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
        73, 74, 75, 76, 77, 78, 79};

    private List<SiteIdentifier> mNeighbors;
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
    public ConnectPlusNeighborReport(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK CON+ NEIGHBORS:");
        List<SiteIdentifier> neighbors = getNeighbors();

        if(neighbors.isEmpty())
        {
            sb.append("NONE");
        }
        else
        {
            sb.append(Joiner.on(",").join(getNeighbors()));
        }

        sb.append(" UNK:").append(getUnknownField());
//        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    public String getUnknownField()
    {
        return getMessage().getHex(UNKNOWN, 6);
    }

    /**
     * List of neighbor site identifiers
     */
    public List<SiteIdentifier> getNeighbors()
    {
        if(mNeighbors == null)
        {
            mNeighbors = new ArrayList<>();

            for(int x = 0; x <= 5; x++)
            {
                int site = getSite(x);

                if(site > 0)
                {
                    mNeighbors.add(DMRSite.create(site));
                }
            }
        }

        return mNeighbors;
    }

    /**
     * Retrieves the specified neighbor site value.
     *
     * @param index 0 - 6
     * @return site id value.  A value of zero indicates the field is empty.
     */
    private int getSite(int index)
    {
        int start = SITE_ARRAY_START + (index * 8);
        int end = start + 7;
        return getMessage().getInt(start, end);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.addAll(getNeighbors());
        }

        return mIdentifiers;
    }
}
