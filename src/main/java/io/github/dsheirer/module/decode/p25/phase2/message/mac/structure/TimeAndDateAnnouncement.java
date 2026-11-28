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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * Time and date announcement.
 *
 * Layout per TIA-102.BBAD-A 3.23 (Figure 66), with the field definitions of TIA-102.AABC-D 6.2.27: the date and time
 * are UTC, the year counts from 2000, and the 12-bit local time offset is a sign bit (1 = subtract from UTC) followed
 * by 11 bits of minutes.  The VD, VT and VL flags say whether the date, time and local time offset are valid.
 */
public class TimeAndDateAnnouncement extends MacStructure
{
    private static final int VD_FLAG = 8;
    private static final int VT_FLAG = 9;
    private static final int VL_FLAG = 10;
    private static final int LOCAL_TIME_OFFSET_SIGN = 12;
    private static final IntField LOCAL_TIME_OFFSET = IntField.range(13, 23);
    private static final IntField MONTH = IntField.range(24, 27);
    private static final IntField DAY = IntField.range(28, 32);
    private static final IntField YEAR = IntField.range(33, 45);
    private static final IntField HOURS = IntField.range(48, 52);
    private static final IntField MINUTES = IntField.range(53, 58);
    private static final IntField SECONDS = IntField.range(59, 64);
    private static final int MAX_OFFSET_MINUTES = 18 * 60;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public TimeAndDateAnnouncement(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message.  Only the fields flagged as valid are shown.
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());

        if(isDateValid())
        {
            sb.append(String.format(" DATE:%04d-%02d-%02d", getYear(), getMonth(), getDay()));
        }

        if(isTimeValid())
        {
            sb.append(String.format(" TIME:%02d:%02d:%02d UTC", getHours(), getMinutes(), getSeconds()));
        }

        if(isLocalTimeOffsetValid())
        {
            int minutes = Math.abs(getLocalTimeOffsetMinutes());
            sb.append(String.format(" LOCAL TIME OFFSET:%s%02d:%02d", getLocalTimeOffsetMinutes() < 0 ? "-" : "+",
                    minutes / 60, minutes % 60));
        }

        return sb.toString();
    }

    /**
     * Date and time, at the local time offset when it is valid, otherwise at UTC.
     *
     * @return date and time, or null when the date or the time is not valid or is not a real date and time.
     */
    public OffsetDateTime getDateAndTime()
    {
        if(!isDateValid() || !isTimeValid())
        {
            return null;
        }

        try
        {
            return LocalDateTime.of(getYear(), getMonth(), getDay(), getHours(), getMinutes(), getSeconds())
                    .atOffset(ZoneOffset.UTC).withOffsetSameInstant(getLocalTimeOffset());
        }
        catch(DateTimeException dte)
        {
            return null;
        }
    }

    /**
     * Indicates if the date field (year, month and day) is valid.
     */
    public boolean isDateValid()
    {
        return getMessage().get(VD_FLAG + getOffset());
    }

    /**
     * Indicates if the time field (hours, minutes and seconds) is valid.
     */
    public boolean isTimeValid()
    {
        return getMessage().get(VT_FLAG + getOffset());
    }

    /**
     * Indicates if the local time offset field is valid.
     */
    public boolean isLocalTimeOffsetValid()
    {
        return getMessage().get(VL_FLAG + getOffset());
    }

    /**
     * Local time offset from UTC in minutes, negative when local time is behind UTC.  Check isLocalTimeOffsetValid().
     */
    public int getLocalTimeOffsetMinutes()
    {
        int minutes = getInt(LOCAL_TIME_OFFSET);
        return getMessage().get(LOCAL_TIME_OFFSET_SIGN + getOffset()) ? -minutes : minutes;
    }

    /**
     * Local time offset from UTC when it is valid and within +/-18 hours, otherwise UTC.
     */
    public ZoneOffset getLocalTimeOffset()
    {
        int minutes = getLocalTimeOffsetMinutes();

        if(isLocalTimeOffsetValid() && Math.abs(minutes) <= MAX_OFFSET_MINUTES)
        {
            return ZoneOffset.ofTotalSeconds(minutes * 60);
        }

        return ZoneOffset.UTC;
    }

    /**
     * Year (UTC), 2000-10191.  Check isDateValid().
     */
    public int getYear()
    {
        return 2000 + getInt(YEAR);
    }

    /**
     * Month (UTC), 1-12 where 1 is January.  Check isDateValid().
     */
    public int getMonth()
    {
        return getInt(MONTH);
    }

    /**
     * Day of the month (UTC), 1-31.  Check isDateValid().
     */
    public int getDay()
    {
        return getInt(DAY);
    }

    /**
     * Hours (UTC), 0-23.  Check isTimeValid().
     */
    public int getHours()
    {
        return getInt(HOURS);
    }

    /**
     * Minutes, 0-59.  Check isTimeValid().
     */
    public int getMinutes()
    {
        return getInt(MINUTES);
    }

    /**
     * Seconds, 0-59.  Check isTimeValid().
     */
    public int getSeconds()
    {
        return getInt(SECONDS);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
