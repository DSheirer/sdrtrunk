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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.SiteFrequency;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Radio Reference import builds the LCN frequency list from the LCN of each site frequency.
 */
public class RadioReferenceEDACSTest
{
    private static SiteFrequency frequency(double mhz, int lcn, String use)
    {
        SiteFrequency siteFrequency = new SiteFrequency();
        siteFrequency.setFrequency(mhz);
        siteFrequency.setLogicalChannelNumber(lcn);
        siteFrequency.setUse(use);
        return siteFrequency;
    }

    @Test
    void testLcnOrder()
    {
        Site site = new Site();
        List<SiteFrequency> frequencies = new ArrayList<>();
        //Listed control channels first, as Radio Reference does - the LCN decides the position
        frequencies.add(frequency(853.725, 10, "d"));
        frequencies.add(frequency(851.175, 1, null));
        frequencies.add(frequency(851.225, 2, null));
        frequencies.add(frequency(853.925, 15, "a"));
        site.setSiteFrequencies(frequencies);

        List<Long> lcns = RadioReferenceDecoder.getLcnFrequencies(site);
        assertEquals(15, lcns.size());
        assertEquals(851_175_000L, lcns.get(0));
        assertEquals(851_225_000L, lcns.get(1));
        assertEquals(0L, lcns.get(2), "LCN 3 not listed");
        assertEquals(853_725_000L, lcns.get(9));
        assertEquals(853_925_000L, lcns.get(14));

        //Without LCNs: the order listed
        Site noLcns = new Site();
        noLcns.setSiteFrequencies(List.of(frequency(851.175, 0, null), frequency(851.225, 0, null)));
        assertEquals(List.of(851_175_000L, 851_225_000L), RadioReferenceDecoder.getLcnFrequencies(noLcns));
    }
}
