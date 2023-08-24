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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * DMR Tier III - Broadcast Local Time
 */
public class LocalTime extends Announcement
{
    //Broadcast Parameters 1: 21-34
    private static final int[] DAY = new int[]{21, 22, 23, 24, 25};
    private static final int[] MONTH = new int[]{26, 27, 28, 39};
    private static final int[] UTC = new int[]{30, 31, 32, 33, 34};

    //Broadcast Parameters 2: 56-79
    private static final int[] HOUR = new int[]{56, 57, 58, 59, 60};
    private static final int[] MINUTE = new int[]{61, 62, 63, 64, 65, 66};
    private static final int[] SECOND = new int[]{67, 68, 69, 70, 71, 72};
    private static final int[] DAY_OF_WEEK = new int[]{73, 74, 75};
    private static final int[] UTC_MINUTES_OFFSET = new int[]{76, 77};
    private static final int[] RESERVED = new int[]{78, 79};

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
    public LocalTime(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" LOCAL TIME:").append(new Date(getLocalTime()).toString());

        sb.append(" ").append(getSystemIdentityCode().getModel());
        sb.append(" NETWORK:").append(getSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getSystemIdentityCode().getSite());

        return sb.toString();
    }

    /**
     * Local time broadcast
     * @return
     */
    public long getLocalTime()
    {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(System.currentTimeMillis());
        int year = calendar.get(Calendar.YEAR);
        int month = getMessage().getInt(MONTH);
        int day = getMessage().getInt(DAY);
        int utcHoursOffset = getMessage().getInt(UTC);
        int hour = (getMessage().getInt(HOUR) + (utcHoursOffset == 31 ? 0 : utcHoursOffset)) % 24;
        int minute = getMessage().getInt(MINUTE);
        int second = getMessage().getInt(SECOND);
        calendar.clear();
        calendar.set(year, month, day, hour, minute, second);
        return calendar.getTimeInMillis();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSystemIdentityCode().getNetwork());
            mIdentifiers.add(getSystemIdentityCode().getSite());
        }

        return mIdentifiers;
    }
}
