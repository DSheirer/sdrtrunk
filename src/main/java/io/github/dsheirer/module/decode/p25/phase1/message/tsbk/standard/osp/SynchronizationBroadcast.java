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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;
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
public class SynchronizationBroadcast extends OSPMessage
{
    public static final int[] RESERVED = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28};
    public static final int SYSTEM_TIME_NOT_LOCKED_TO_EXTERNAL_REFERENCE_FLAG = 29;
    public static final int MICRO_SLOTS_TO_MINUTE_ROLLOVER_UNLOCKED_FLAG = 30;
    public static final int[] LEAP_SECOND_CORRECTION = {31, 32};
    public static final int LOCAL_TIME_OFFSET_VALID_FLAG = 33;
    public static final int LOCAL_TIME_OFFSET_SIGN = 34;
    public static final int[] LOCAL_TIME_OFFSET_HOURS = {35, 36, 37, 38};
    public static final int LOCAL_TIME_OFFSET_HALF_HOUR = 39;
    public static final int[] YEAR = {40, 41, 42, 43, 44, 45, 46};
    public static final int[] MONTH = {47, 48, 49, 50};
    public static final int[] DAY = {51, 52, 53, 54, 55};
    public static final int[] HOURS = {56, 57, 58, 59, 60};
    public static final int[] MINUTES = {61, 62, 63, 65, 65, 66};
    public static final int[] MICRO_SLOTS = {67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private static final DateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
    private static final TimeZone NO_TIME_ZONE = new SimpleTimeZone(0, "NONE");

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public SynchronizationBroadcast(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
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
        return getMessage().get(SYSTEM_TIME_NOT_LOCKED_TO_EXTERNAL_REFERENCE_FLAG);
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
        return !getMessage().get(MICRO_SLOTS_TO_MINUTE_ROLLOVER_UNLOCKED_FLAG);
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
        return (double)getMessage().getInt(LEAP_SECOND_CORRECTION) * 2.5d;
    }


    /**
     * Indicates the local time offset fields contain valid information.
     */
    public boolean isValidLocalTimeOffset()
    {
        return !getMessage().get(LOCAL_TIME_OFFSET_VALID_FLAG);
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

            offset += getMessage().getInt(LOCAL_TIME_OFFSET_HOURS) * 3600000;
            offset += getMessage().get(LOCAL_TIME_OFFSET_HALF_HOUR) ? 1800000 : 0;
            offset = getMessage().get(LOCAL_TIME_OFFSET_SIGN) ? -offset : offset;

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
        return 2000 + getMessage().getInt(YEAR);
    }

    /**
     * System time - month
     *
     * @return month 1-12 where 1=January and 12=December
     */
    public int getMonth()
    {
        return getMessage().getInt(MONTH);
    }

    /**
     * System time - day of month
     *
     * @return day of month in range 1-31
     */
    public int getDay()
    {
        return getMessage().getInt(DAY);
    }

    /**
     * System time - hours
     *
     * @return hours in range 0 - 23
     */
    public int getHours()
    {
        return getMessage().getInt(HOURS);
    }

    /**
     * System time - minutes
     *
     * @return minutes in range 0 - 59
     */
    public int getMinutes()
    {
        return getMessage().getInt(MINUTES);
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
     * @see isMicroslotsLockedToMinuteRollover() method.
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
        return getMessage().getInt(MICRO_SLOTS);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
