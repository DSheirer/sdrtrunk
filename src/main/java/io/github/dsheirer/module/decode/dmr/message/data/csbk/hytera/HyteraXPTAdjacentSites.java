/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
 * Hytera XPT - Adjacent Site State
 */
public class HyteraXPTAdjacentSites extends CSBKMessage
{
    private static final int[] SEQUENCE_NUMBER = new int[]{0, 1};
    private static final int[] SITE_1 = new int[]{16, 17, 18, 19, 20}; //Unknown: 21-23
    private static final int[] FREE_1 = new int[]{24, 25, 26, 27}; //Unknown: 28-31
    private static final int[] SITE_2 = new int[]{32, 33, 34, 35, 36}; //Unknown: 37-39
    private static final int[] FREE_2 = new int[]{40, 41, 42, 43}; //Unknown: 44-47
    private static final int[] SITE_3 = new int[]{48, 49, 50, 51, 52}; //Unknown: 53-55
    private static final int[] FREE_3 = new int[]{56, 57, 58, 59}; //Unknown: 60-63
    private static final int[] SITE_4 = new int[]{64, 65, 66, 67, 68}; //Unknown: 69-71
    private static final int[] FREE_4 = new int[]{72, 73, 74, 75}; //Unknown: 76-79

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
    public HyteraXPTAdjacentSites(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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

        sb.append(" HYTERA XPT NEIGHBORS");

        if(hasSite1())
        {
            sb.append(" SITE:").append(getSite1());
            sb.append(" FREE:").append(getFree1());
        }

        if(hasSite2())
        {
            sb.append(" SITE:").append(getSite2());
            sb.append(" FREE:").append(getFree2());
        }

        if(hasSite3())
        {
            sb.append(" SITE:").append(getSite3());
            sb.append(" FREE:").append(getFree3());
        }

        if(hasSite4())
        {
            sb.append(" SITE:").append(getSite4());
            sb.append(" FREE:").append(getFree4());
        }

        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    /**
     * Site ID for site number 1.
     */
    public int getSite1()
    {
        return getMessage().getInt(SITE_1);
    }

    /**
     * Indicates if site number 1 is valid (ie non-zero).
     */
    public boolean hasSite1()
    {
        return getSite1() > 0;
    }

    /**
     * Free repeater for site 1.
     */
    public int getFree1()
    {
        return getMessage().getInt(FREE_1);
    }

    /**
     * Site ID for site number 2.
     */
    public int getSite2()
    {
        return getMessage().getInt(SITE_2);
    }

    /**
     * Indicates if site number 2 is valid (ie non-zero).
     */
    public boolean hasSite2()
    {
        return getSite2() > 0;
    }

    /**
     * Free repeater for site 2.
     */
    public int getFree2()
    {
        return getMessage().getInt(FREE_2);
    }

    /**
     * Site ID for site number 3.
     */
    public int getSite3()
    {
        return getMessage().getInt(SITE_3);
    }

    /**
     * Indicates if site number 3 is valid (ie non-zero).
     */
    public boolean hasSite3()
    {
        return getSite3() > 0;
    }

    /**
     * Free repeater for site 3.
     */
    public int getFree3()
    {
        return getMessage().getInt(FREE_3);
    }

    /**
     * Site ID for site number 4.
     */
    public int getSite4()
    {
        return getMessage().getInt(SITE_4);
    }

    /**
     * Indicates if site number 4 is valid (ie non-zero).
     */
    public boolean hasSite4()
    {
        return getSite4() > 0;
    }

    /**
     * Free repeater for site 4.
     */
    public int getFree4()
    {
        return getMessage().getInt(FREE_4);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
