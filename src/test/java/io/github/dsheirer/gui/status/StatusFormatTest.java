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

package io.github.dsheirer.gui.status;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusFormatTest
{
    @Test
    void bytesUseOneDecimalBelowOneHundred()
    {
        assertEquals("0 B", ResourceStatusItem.bytes(0));
        assertEquals("512 B", ResourceStatusItem.bytes(512));
        assertEquals("1.5 KB", ResourceStatusItem.bytes(1536));
        assertEquals("850 MB", ResourceStatusItem.bytes(850L * 1024 * 1024));
        assertEquals("1.2 GB", ResourceStatusItem.bytes(1288490189L));
        assertEquals("2.0 TB", ResourceStatusItem.bytes(2L * 1024 * 1024 * 1024 * 1024));
        assertEquals("0 B", ResourceStatusItem.bytes(-5));
    }

    @Test
    void percentRounds()
    {
        assertEquals("0%", ResourceStatusItem.percent(0));
        assertEquals("23%", ResourceStatusItem.percent(0.234));
        assertEquals("100%", ResourceStatusItem.percent(0.999));
    }

    @Test
    void levelThresholdsAreInclusive()
    {
        assertEquals(StatusSeverity.OK, StatusSeverity.forLevel(0.79, 0.8, 0.95));
        assertEquals(StatusSeverity.WARNING, StatusSeverity.forLevel(0.8, 0.8, 0.95));
        assertEquals(StatusSeverity.ERROR, StatusSeverity.forLevel(0.95, 0.8, 0.95));
        assertEquals(StatusSeverity.ERROR, StatusSeverity.forLevel(1.4, 0.8, 0.95));
    }

    @Test
    void maxKeepsTheMoreSevere()
    {
        assertEquals(StatusSeverity.ERROR, StatusSeverity.OK.max(StatusSeverity.ERROR));
        assertEquals(StatusSeverity.WARNING, StatusSeverity.WARNING.max(StatusSeverity.OK));
        assertEquals(StatusSeverity.OK, StatusSeverity.NEUTRAL.max(StatusSeverity.OK));
    }
}
