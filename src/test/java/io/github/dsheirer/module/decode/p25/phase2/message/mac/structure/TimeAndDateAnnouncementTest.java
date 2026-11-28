/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessageFactory;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JUnit tests for the time and date announcement (MAC opcode 0x75).  The messages are built from TIA-102.BBAD-A
 * 3.23 Figure 66, with the fields of the Local Time Offset, Date and Time as TIA-102.AABC-D 6.2.27 lays them out:
 *
 * octet 1: B1 B2 MCO, octet 2: VD VT VL res and the local time offset's sign (bit 3) and 3 MS bits of its 11-bit
 * minutes, octet 3: the rest of the minutes, octets 4-6: month (4), day (5), years after 2000 (13), reserved (2),
 * octets 7-9: hours (5), minutes (6), seconds (6), reserved (7).
 */
public class TimeAndDateAnnouncementTest
{
    private static final int OFFSET = MacMessageFactory.DEFAULT_MAC_STRUCTURE_INDEX;

    /**
     * Sets a field given by the figure's octet (1-9) and bit (7-0) of its MS bit, MS bit first.
     */
    private static void set(CorrectedBinaryMessage message, int octet, int bit, int width, int value)
    {
        int start = OFFSET + (octet - 1) * 8 + (7 - bit);

        for(int i = 0; i < width; i++)
        {
            if(((value >> (width - 1 - i)) & 1) == 1)
            {
                message.set(start + i);
            }
            else
            {
                message.clear(start + i);
            }
        }
    }

    private static TimeAndDateAnnouncement build(int vd, int vt, int vl, int reserved, int subtract, int offsetMinutes,
                                                 int month, int day, int year, int hours, int minutes, int seconds)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(OFFSET + 72);
        set(message, 1, 7, 1, 0);           //B1
        set(message, 1, 6, 1, 1);           //B2
        set(message, 1, 5, 6, 0b110101);    //MCO
        set(message, 2, 7, 1, vd);
        set(message, 2, 6, 1, vt);
        set(message, 2, 5, 1, vl);
        set(message, 2, 4, 1, reserved);
        set(message, 2, 3, 1, subtract);
        set(message, 2, 2, 11, offsetMinutes);
        set(message, 4, 7, 4, month);
        set(message, 4, 3, 5, day);
        set(message, 5, 6, 13, year);
        set(message, 7, 7, 5, hours);
        set(message, 7, 2, 6, minutes);
        set(message, 8, 4, 6, seconds);
        return new TimeAndDateAnnouncement(message, OFFSET);
    }

    private static void assertFields(TimeAndDateAnnouncement tda, boolean vd, boolean vt, boolean vl, int year,
                                     int month, int day, int hours, int minutes, int seconds, int offsetMinutes)
    {
        assertEquals(MacOpcode.PHASE1_75_TIME_AND_DATE_ANNOUNCEMENT, tda.getOpcode());
        assertEquals(vd, tda.isDateValid());
        assertEquals(vt, tda.isTimeValid());
        assertEquals(vl, tda.isLocalTimeOffsetValid());
        assertEquals(year, tda.getYear());
        assertEquals(month, tda.getMonth());
        assertEquals(day, tda.getDay());
        assertEquals(hours, tda.getHours());
        assertEquals(minutes, tda.getMinutes());
        assertEquals(seconds, tda.getSeconds());
        assertEquals(offsetMinutes, tda.getLocalTimeOffsetMinutes());
    }

    @Test
    public void minusEightHours()
    {
        //2026-11-26 15:04:05 UTC, subtract 480 minutes
        TimeAndDateAnnouncement tda = build(1, 1, 1, 0, 1, 480, 11, 26, 26, 15, 4, 5);
        assertFields(tda, true, true, true, 2026, 11, 26, 15, 4, 5, -480);
        assertEquals(ZoneOffset.ofHours(-8), tda.getLocalTimeOffset());
        assertEquals(OffsetDateTime.of(2026, 11, 26, 7, 4, 5, 0, ZoneOffset.ofHours(-8)), tda.getDateAndTime());
        assertEquals(Instant.parse("2026-11-26T15:04:05Z"), tda.getDateAndTime().toInstant());
        assertEquals("TIME AND DATE ANNOUNCEMENT DATE:2026-11-26 TIME:15:04:05 UTC LOCAL TIME OFFSET:-08:00",
                tda.toString());
    }

    @Test
    public void plusFiveThirty()
    {
        //2024-02-29 23:59:59 UTC, add 330 minutes: local time is the next day.  The reserved bit is set and ignored.
        TimeAndDateAnnouncement tda = build(1, 1, 1, 1, 0, 330, 2, 29, 24, 23, 59, 59);
        assertFields(tda, true, true, true, 2024, 2, 29, 23, 59, 59, 330);
        assertEquals(ZoneOffset.ofHoursMinutes(5, 30), tda.getLocalTimeOffset());
        assertEquals(OffsetDateTime.of(2024, 3, 1, 5, 29, 59, 0, ZoneOffset.ofHoursMinutes(5, 30)),
                tda.getDateAndTime());
        assertEquals("TIME AND DATE ANNOUNCEMENT DATE:2024-02-29 TIME:23:59:59 UTC LOCAL TIME OFFSET:+05:30",
                tda.toString());
    }

    @Test
    public void dateNotValid()
    {
        //VD clear: month and day are zero
        TimeAndDateAnnouncement tda = build(0, 1, 1, 0, 1, 480, 0, 0, 0, 15, 4, 5);
        assertFields(tda, false, true, true, 2000, 0, 0, 15, 4, 5, -480);
        assertNull(tda.getDateAndTime());
        assertEquals("TIME AND DATE ANNOUNCEMENT TIME:15:04:05 UTC LOCAL TIME OFFSET:-08:00", tda.toString());
    }

    @Test
    public void timeNotValid()
    {
        //VT clear: the time field holds out of range values
        TimeAndDateAnnouncement tda = build(1, 0, 1, 0, 1, 480, 11, 26, 26, 31, 63, 63);
        assertFields(tda, true, false, true, 2026, 11, 26, 31, 63, 63, -480);
        assertNull(tda.getDateAndTime());
        assertEquals("TIME AND DATE ANNOUNCEMENT DATE:2026-11-26 LOCAL TIME OFFSET:-08:00", tda.toString());
    }

    @Test
    public void localTimeOffsetNotValid()
    {
        //VL clear: the offset field (subtract 2047 minutes) is ignored and the date and time are given in UTC
        TimeAndDateAnnouncement tda = build(1, 1, 0, 0, 1, 2047, 11, 26, 26, 15, 4, 5);
        assertFields(tda, true, true, false, 2026, 11, 26, 15, 4, 5, -2047);
        assertEquals(ZoneOffset.UTC, tda.getLocalTimeOffset());
        assertEquals(OffsetDateTime.of(2026, 11, 26, 15, 4, 5, 0, ZoneOffset.UTC), tda.getDateAndTime());
        assertEquals("TIME AND DATE ANNOUNCEMENT DATE:2026-11-26 TIME:15:04:05 UTC", tda.toString());
    }

    @Test
    public void yearCountsFrom2000()
    {
        TimeAndDateAnnouncement tda = build(1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0);
        assertFields(tda, true, true, false, 2000, 1, 1, 0, 0, 0, 0);
        assertEquals(OffsetDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), tda.getDateAndTime());

        TimeAndDateAnnouncement max = build(1, 1, 0, 0, 0, 0, 12, 31, 8191, 23, 59, 59);
        assertFields(max, true, true, false, 10191, 12, 31, 23, 59, 59, 0);
        assertEquals(OffsetDateTime.of(10191, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC), max.getDateAndTime());
    }

    @Test
    public void offsetBeyondEighteenHoursIsIgnored()
    {
        //ZoneOffset only goes to +/-18:00.  The 11-bit field reaches 2047 minutes.
        TimeAndDateAnnouncement tda = build(1, 1, 1, 0, 0, 1081, 11, 26, 26, 15, 4, 5);
        assertFields(tda, true, true, true, 2026, 11, 26, 15, 4, 5, 1081);
        assertEquals(ZoneOffset.UTC, tda.getLocalTimeOffset());
        assertEquals(OffsetDateTime.of(2026, 11, 26, 15, 4, 5, 0, ZoneOffset.UTC), tda.getDateAndTime());
        assertEquals("TIME AND DATE ANNOUNCEMENT DATE:2026-11-26 TIME:15:04:05 UTC LOCAL TIME OFFSET:+18:01",
                tda.toString());
    }

    @Test
    public void invalidDateIsNull()
    {
        //VD and VT set, but February 30th
        TimeAndDateAnnouncement tda = build(1, 1, 1, 0, 1, 480, 2, 30, 26, 15, 4, 5);
        assertTrue(tda.isDateValid());
        assertNull(tda.getDateAndTime());
    }

    @Test
    public void neverThrows()
    {
        CorrectedBinaryMessage ones = new CorrectedBinaryMessage(OFFSET + 72);
        ones.set(0, OFFSET + 72);
        TimeAndDateAnnouncement allOnes = new TimeAndDateAnnouncement(ones, OFFSET);
        assertDoesNotThrow(allOnes::toString);
        assertNull(allOnes.getDateAndTime());

        Random random = new Random(75);

        for(int x = 0; x < 10000; x++)
        {
            CorrectedBinaryMessage message = new CorrectedBinaryMessage(OFFSET + 72);

            for(int bit = 0; bit < OFFSET + 72; bit++)
            {
                if(random.nextBoolean())
                {
                    message.set(bit);
                }
            }

            TimeAndDateAnnouncement tda = new TimeAndDateAnnouncement(message, OFFSET);
            assertDoesNotThrow(tda::toString);
            assertDoesNotThrow(tda::getDateAndTime);
        }
    }

    @Test
    public void nothingValid()
    {
        TimeAndDateAnnouncement tda = build(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        assertFields(tda, false, false, false, 2000, 0, 0, 0, 0, 0, 0);
        assertEquals(ZoneOffset.UTC, tda.getLocalTimeOffset());
        assertNull(tda.getDateAndTime());
        assertEquals("TIME AND DATE ANNOUNCEMENT", tda.toString());
    }
}
