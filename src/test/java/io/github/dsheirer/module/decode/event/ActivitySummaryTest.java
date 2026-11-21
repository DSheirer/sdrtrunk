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
package io.github.dsheirer.module.decode.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ActivitySummaryTest
{
    @Test
    void fieldsAlignAcrossSection()
    {
        ActivitySummary summary = new ActivitySummary("P25 Phase 1");
        summary.section("Network").field("WACN", "BEE00").field("System", "3A1").field("NAC", null);

        String expected = """
            P25 Phase 1
            ===========

            Network
            -------
              WACN    BEE00
              System  3A1
              NAC     Unknown

            """;
        assertEquals(expected, summary.toString());
    }

    @Test
    void tableColumnsSizeToWidestValue()
    {
        ActivitySummary summary = new ActivitySummary("DMR");
        ActivitySummary.Table table = summary.section("Talker Aliases").table("Radio", "Talker Alias");
        table.row(12, "ENGINE 1");
        table.row(1234567, "BC");
        table.row(345, "A VERY LONG TALKER ALIAS");

        String expected = """
            DMR
            ===

            Talker Aliases
            --------------
              Radio    Talker Alias
              -------  ------------------------
              12       ENGINE 1
              1234567  BC
              345      A VERY LONG TALKER ALIAS

            """;
        assertEquals(expected, summary.toString());
    }

    @Test
    void emptySectionAndTableShowNone()
    {
        ActivitySummary summary = new ActivitySummary("NXDN");
        summary.section("Neighbor Sites");
        summary.section("Talker Aliases").table("Radio", "Talker Alias");

        String expected = """
            NXDN
            ====

            Neighbor Sites
            --------------
              None

            Talker Aliases
            --------------
              None

            """;
        assertEquals(expected, summary.toString());
    }

    @Test
    void tableSeparatedFromFields()
    {
        ActivitySummary summary = new ActivitySummary(null);
        summary.section("Current Site").field("Site", "01");
        summary.table("Channel Type", "Downlink").row("Primary Control", ActivitySummary.mhz(851012500));

        String expected = """

            Current Site
            ------------
              Site  01

              Channel Type     Downlink
              ---------------  ---------
              Primary Control  851.01250

            """;
        assertEquals(expected, summary.toString());
    }

    @Test
    void frequencyFormats()
    {
        assertEquals("851.01250", ActivitySummary.mhz(851012500));
        assertEquals("Unknown", ActivitySummary.mhz(0));
        assertEquals("12.50", ActivitySummary.khz(12500));
        assertEquals("25", ActivitySummary.khz(25000));
        assertEquals("-45.0000", ActivitySummary.offsetMhz(-45000000));
        assertEquals("+5.0000", ActivitySummary.offsetMhz(5000000));
    }
}
