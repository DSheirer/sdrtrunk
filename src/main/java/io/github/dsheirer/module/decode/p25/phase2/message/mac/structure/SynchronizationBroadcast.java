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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Synchronization broadcast
 */
public class SynchronizationBroadcast extends MacStructure
{
    public static final int US_TDMA_UNSYNCHRONIZED_TO_FDMA_FLAG = 20;
    public static final int IST_INVALID_SYSTEM_TIME_NOT_LOCKED_TO_EXTERNAL_REFERENCE_FLAG = 21;
    public static final int MM_MICRO_SLOTS_TO_MINUTE_ROLLOVER_UNLOCKED_FLAG = 22;
    private static final IntField MC_LEAP_SECOND_CORRECTION = IntField.range(23, 24);
    public static final int VL_LOCAL_TIME_OFFSET_VALID_FLAG = 25;
    public static final int LOCAL_TIME_OFFSET_SIGN = 26;
    private static final IntField LOCAL_TIME_OFFSET_HOURS = IntField.range(27, 30);
    public static final int LOCAL_TIME_OFFSET_HALF_HOUR = 31;
    private static final IntField YEAR = IntField.range(32, 38);
    private static final IntField MONTH = IntField.range(39, 42);
    private static final IntField DAY = IntField.range(43, 47);
    private static final IntField HOURS = IntField.range(48, 52);
    private static final IntField MINUTES = IntField.range(53, 58);
    private static final IntField MICRO_SLOTS = IntField.range(59, 71);
    private static final DateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
    private static final TimeZone NO_TIME_ZONE = new SimpleTimeZone(0, "NONE");

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public SynchronizationBroadcast(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" SYSTEM TIME");
        if(isSystemTimeNotLockedToExternalReference())
        {
            sb.append(" UNLOCKED");
        }
        sb.append(":");
        TIME_FORMATTER.setTimeZone(getTimeZone());
        sb.append(" ").append(TIME_FORMATTER.format(new Date(getSystemTime())));
        sb.append(" LEAP-SECOND CORRECTION:").append(getLeapSecondCorrection()).append("mS");
        if(isMicroslotsLockedToMinuteRollover())
        {
            sb.append(" MICROSLOT-MINUTE ROLLOVER:SLOW");
        }
        else
        {
            sb.append(" MICROSLOT-MINUTE ROLLOVER:UNLOCKED");
        }
        return sb.toString();
    }

    /**
     * System Time (UTC) in milliseconds since java epoch
     */
    public long getSystemTime()
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(Calendar.YEAR, getYear());
        cal.set(Calendar.MONTH, getMonth() - 1);
        cal.set(Calendar.DAY_OF_MONTH, getDay());
        cal.set(Calendar.HOUR_OF_DAY, getHours());
        cal.set(Calendar.MINUTE, getMinutes());
        cal.set(Calendar.MILLISECOND, getMilliSeconds());
        return cal.getTimeInMillis();
    }

    /**
     * System Time not locked to external time reference indicator
     */
    public boolean isSystemTimeNotLockedToExternalReference()
    {
        return getMessage().get(IST_INVALID_SYSTEM_TIME_NOT_LOCKED_TO_EXTERNAL_REFERENCE_FLAG + getOffset());
    }

    /**
     * Micro-slot-to-Minute rollover boundary lock indicator.
     *
     * Note: we invert the message value so that true indicates that the micro
     * slots and minute rollovers are locked, whereas the ICD uses false to
     * indicate a lock.
     *
     * @return false if the microslots and minutes rollover are not locked.  This
     * indicates that the microslots minute rollover is free rolling.
     */
    public boolean isMicroslotsLockedToMinuteRollover()
    {
        return !getMessage().get(MM_MICRO_SLOTS_TO_MINUTE_ROLLOVER_UNLOCKED_FLAG + getOffset());
    }

    /**
     * Leap second correction value that should be applied to the system time.
     *
     * The leap-second correction field indicates the number of 2.5 millisecond
     * units that must be added to the system time to account for the insertion
     * of leap seconds when the system time is represented as UTC universal time.
     *
     * @return leap-second correction value in milliseconds 0.0-10.0
     */
    public double getLeapSecondCorrection()
    {
        return (double)getInt(MC_LEAP_SECOND_CORRECTION) * 2.5d;
    }


    /**
     * Indicates the local time offset fields contain valid information.
     */
    public boolean isValidLocalTimeOffset()
    {
        return !getMessage().get(VL_LOCAL_TIME_OFFSET_VALID_FLAG + getOffset());
    }

    /**
     * Local time zone offset from UTC universal time (zulu) when the VALID
     * LOCAL TIME OFFSET flag indicates a valid offset.  Otherwise, this method
     * returns a static +00:00 indicating no local time offset.
     */
    public TimeZone getTimeZone()
    {
        if(isValidLocalTimeOffset())
        {
            int offset = 0;

            offset += getInt(LOCAL_TIME_OFFSET_HOURS) * 3600000;
            offset += getMessage().get(LOCAL_TIME_OFFSET_HALF_HOUR + getOffset()) ? 1800000 : 0;
            offset = getMessage().get(LOCAL_TIME_OFFSET_SIGN + getOffset()) ? -offset : offset;
            return new SimpleTimeZone(offset, "LOCAL");
        }
        else
        {
            return NO_TIME_ZONE;
        }
    }

    /**
     * System time - year.
     *
     * @return year in range 2000-2127
     */
    public int getYear()
    {
        return 2000 + getInt(YEAR);
    }

    /**
     * System time - month
     *
     * @return month 1-12 where 1=January and 12=December
     */
    public int getMonth()
    {
        return getInt(MONTH);
    }

    /**
     * System time - day of month
     *
     * @return day of month in range 1-31
     */
    public int getDay()
    {
        return getInt(DAY);
    }

    /**
     * System time - hours
     *
     * @return hours in range 0 - 23
     */
    public int getHours()
    {
        return getInt(HOURS);
    }

    /**
     * System time - minutes
     *
     * @return minutes in range 0 - 59
     */
    public int getMinutes()
    {
        return getInt(MINUTES);
    }

    /**
     * System time - milli-seconds
     *
     * Note: sub-millisecond values are rounded to nearest millisecond unit to
     * conform to java Date internal milli-second precision level.
     *
     * @return milli-seconds in range 0 - 59999;
     */
    public int getMilliSeconds()
    {
        return (int)((double)getMicroSlots() * 7.5);
    }

    /**
     * TDMA Micro Slots.
     *
     * Number of 7.5 millisecond micro slots since the last minute rollover, or
     * since the last micro-slot counter rollover when the the micro-slot to
     * minute rollover is unlocked.
     *
     * @return number of 7.5 mS micro-slots since last minute or micro-slot
     * rollover
     *
     * This value supports mobile subscriber timing alignment with a TDMA traffic
     * channel when a channel is granted from the control channels, so that when
     * the mobile changes from the control channel to the traffic channel, it is
     * already aligned with the traffic channel TDMA timing.
     *
     * Super frames occur every 360 ms (48 micro-slots) on FDMA and Ultra frames
     * occur every 4 super frames on TDMA.
     *
     * Valid micro-slot values range 0-7999 and represent 60,000
     * milliseconds ( 8000 x 7.5 ms).
     */
    public int getMicroSlots()
    {
        return getInt(MICRO_SLOTS);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
