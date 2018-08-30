/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Sync Broadcast - used to broadcast FDMA-TDMA timing synchronization
 * information so that a subscriber unit that is time synchronized on an FDMA
 * control channel is also time synchronized with all TDMA traffic channels at
 * the site.
 */
public class SyncBroadcast extends TSBKMessage
{
    public static final int[] RESERVED = {80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92};
    public static final int SYSTEM_TIME_NOT_LOCKED_TO_EXTERNAL_REFERENCE = 93;
    public static final int MICRO_SLOTS_TO_MINUTE_ROLLOVER_UNLOCKED = 94;
    public static final int[] LEAP_SECOND_CORRECTION = {95, 96};
    public static final int LOCAL_TIME_OFFSET_VALID = 97;
    public static final int LOCAL_TIME_OFFSET_SIGN = 98;
    public static final int[] LOCAL_TIME_OFFSET_HOURS = {99, 100, 101, 102};
    public static final int LOCAL_TIME_OFFSET_HALF_HOUR = 103;

    public static final int[] YEAR = {104, 105, 106, 107, 108, 109, 110};
    public static final int[] MONTH = {111, 112, 113, 114};
    public static final int[] DAY = {115, 116, 117, 118, 119};
    public static final int[] HOURS = {120, 121, 122, 123, 124};
    public static final int[] MINUTES = {125, 126, 127, 128, 129, 130};
    public static final int[] MICRO_SLOTS = {131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};
    public static final int[] TSBK_CRC = {144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159};

    private static final DateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
    private static final TimeZone NO_TIME_ZONE = new SimpleTimeZone(0, "NONE");

    public SyncBroadcast(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
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
        sb.append(" " + TIME_FORMATTER.format(new Date(getSystemTime())));

        sb.append(" LEAP-SECOND CORRECTION:" + getLeapSecondCorrection() + "mS");

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
        return mMessage.get(SYSTEM_TIME_NOT_LOCKED_TO_EXTERNAL_REFERENCE);
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
        return !mMessage.get(MICRO_SLOTS_TO_MINUTE_ROLLOVER_UNLOCKED);
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
        return (double)mMessage.getInt(LEAP_SECOND_CORRECTION) * 2.5d;
    }


    /**
     * Indicates the local time offset fields contain valid information.
     */
    public boolean isValidLocalTimeOffset()
    {
        return !mMessage.get(LOCAL_TIME_OFFSET_VALID);
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

            offset += mMessage.getInt(LOCAL_TIME_OFFSET_HOURS) * 3600000;
            offset += mMessage.get(LOCAL_TIME_OFFSET_HALF_HOUR) ? 1800000 : 0;
            offset = mMessage.get(LOCAL_TIME_OFFSET_SIGN) ? -offset : offset;

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
        return 2000 + mMessage.getInt(YEAR);
    }

    /**
     * System time - month
     *
     * @return month 1-12 where 1=January and 12=December
     */
    public int getMonth()
    {
        return mMessage.getInt(MONTH);
    }

    /**
     * System time - day of month
     *
     * @return day of month in range 1-31
     */
    public int getDay()
    {
        return mMessage.getInt(DAY);
    }

    /**
     * System time - hours
     *
     * @return hours in range 0 - 23
     */
    public int getHours()
    {
        return mMessage.getInt(HOURS);
    }

    /**
     * System time - minutes
     *
     * @return minutes in range 0 - 59
     */
    public int getMinutes()
    {
        return mMessage.getInt(MINUTES);
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
     *
     * @return number of 7.5 mS micro-slots since last minute or micro-slot
     * rollover
     */
    public int getMicroSlots()
    {
        return mMessage.getInt(MICRO_SLOTS);
    }
}