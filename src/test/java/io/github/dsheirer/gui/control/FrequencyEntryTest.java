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

package io.github.dsheirer.gui.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the frequency control's digit arithmetic, parsing and typed entry.
 */
public class FrequencyEntryTest
{
    @Test
    public void digits()
    {
        long frequency = 851_012_500L;
        assertEquals(0, FrequencyEntry.getDigit(frequency, 0));
        assertEquals(5, FrequencyEntry.getDigit(frequency, 2));
        assertEquals(1, FrequencyEntry.getDigit(frequency, 4));
        assertEquals(1, FrequencyEntry.getDigit(frequency, 6));
        assertEquals(8, FrequencyEntry.getDigit(frequency, 8));
        assertEquals(0, FrequencyEntry.getDigit(frequency, 9));
        assertTrue(FrequencyEntry.isLeadingZero(frequency, 9));
        assertFalse(FrequencyEntry.isLeadingZero(frequency, 8));
        assertFalse(FrequencyEntry.isLeadingZero(0, 6), "1 MHz digit is never a leading zero");
    }

    @Test
    public void stepCarriesAndClamps()
    {
        assertEquals(852_000_000L, FrequencyEntry.step(851_900_000L, 5, 1, 1, 2_000_000_000L), "carry into MHz");
        assertEquals(850_900_000L, FrequencyEntry.step(851_000_000L, 5, -1, 1, 2_000_000_000L), "borrow from MHz");
        assertEquals(1_766_000_000L, FrequencyEntry.step(1_765_500_000L, 6, 3, 24_000_000L, 1_766_000_000L),
                "clamp at maximum");
        assertEquals(24_000_000L, FrequencyEntry.step(25_000_000L, 7, -1, 24_000_000L, 1_766_000_000L),
                "clamp at minimum");
    }

    @Test
    public void setDigit()
    {
        assertEquals(861_012_500L, FrequencyEntry.setDigit(851_012_500L, 7, 6));
        assertEquals(851_012_509L, FrequencyEntry.setDigit(851_012_500L, 0, 9));
    }

    @Test
    public void parse()
    {
        assertEquals(851_012_500L, FrequencyEntry.parse("851.0125"));
        assertEquals(851_012_500L, FrequencyEntry.parse(" 851.012500 MHz "));
        assertEquals(851_012_500L, FrequencyEntry.parse("851012500"), "large whole number is Hertz");
        assertEquals(851_012_500L, FrequencyEntry.parse("851,012,500 Hz"));
        assertEquals(162_550_000L, FrequencyEntry.parse("162550 kHz"));
        assertEquals(1_296_000_000L, FrequencyEntry.parse("1.296GHz"));
        assertEquals(9_999_000_000L, FrequencyEntry.parse("9999"), "4 digit whole number is MHz");
        assertNull(FrequencyEntry.parse("851.0125005"), "finer than 1 Hz");
        assertNull(FrequencyEntry.parse("abc"));
        assertNull(FrequencyEntry.parse("-5"));
        assertNull(FrequencyEntry.parse(""));
        assertNull(FrequencyEntry.parse(null));
    }

    @Test
    public void format()
    {
        assertEquals("851.012500", FrequencyEntry.format(851_012_500L));
        assertEquals("0.000001", FrequencyEntry.format(1));
        assertEquals("1296.000000", FrequencyEntry.format(1_296_000_000L));
    }

    @Test
    public void typedEntry()
    {
        FrequencyEntry.TypedEntry entry = new FrequencyEntry.TypedEntry();
        assertTrue(entry.isEmpty());

        for(char c: "0851.0125".toCharArray())
        {
            assertTrue(entry.type(c));
        }

        assertEquals(851_012_500L, entry.getFrequency());
        assertTrue(entry.isTyped(8), "100 MHz digit typed");
        assertFalse(entry.isTyped(9), "leading zero isn't counted");
        assertTrue(entry.isTyped(2), "100 Hz digit typed");
        assertFalse(entry.isTyped(1), "10 Hz digit implied");
        assertFalse(entry.type('.'), "second decimal point");

        assertTrue(entry.backspace());
        assertEquals(851_012_000L, entry.getFrequency());

        entry.clear();

        for(char c: "12345".toCharArray())
        {
            entry.type(c);
        }

        assertEquals(1234_000_000L, entry.getFrequency(), "fifth MHz digit is rejected");

        entry.clear();

        for(char c: ".1234567".toCharArray())
        {
            entry.type(c);
        }

        assertEquals(123_456L, entry.getFrequency(), "seventh fractional digit is rejected");
    }
}
