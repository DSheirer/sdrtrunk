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
package io.github.dsheirer.gui.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.map.GeoPosition;
import io.github.dsheirer.service.radioreference.SiteRecord;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests the radio reference site markers and card on the map, without loading map tiles
 */
public class SiteLayerTest
{
    private static SiteRecord site(String system, String description, double latitude, double longitude, int channels)
    {
        List<SiteRecord.SiteChannel> list = new ArrayList<>();

        for(int index = 0; index < channels; index++)
        {
            list.add(new SiteRecord.SiteChannel(851_012_500L + (index * 250_000L), index + 1,
                index == 0 ? "Control" : index == 1 ? "Alternate control" : null));
        }

        return new SiteRecord(1, system, "Project 25 Phase II", "APCO-25 Common Air Interface Exclusive", 10, 3,
            description, "Onondaga County", latitude, longitude, 10, "1A3", 1, 2, list);
    }

    @Test
    void hitTestFindsTowersUnderTheMouse()
    {
        MapViewport viewport = new MapViewport();
        viewport.setSize(800, 600);
        viewport.setZoom(13);
        viewport.setCenter(new GeoPosition(43.05, -76.15));

        SiteRecord syracuse = site("System A", "Syracuse", 43.0481, -76.1474, 4);
        SiteRecord sameTower = site("System B", "Syracuse", 43.0481, -76.1474, 4);
        SiteRecord fulton = site("System C", "Fulton", 43.3226, -76.4171, 4);
        SiteLayer layer = new SiteLayer(() -> List.of(syracuse, sameTower, fulton));

        //Middle of the tower, above its base at the site location
        double x = viewport.getScreenX(-76.1474);
        double y = viewport.getScreenY(43.0481) - 8;

        assertEquals(List.of("System A", "System B"),
            layer.getSitesAt(viewport, x, y).stream().map(SiteRecord::systemName).toList(), "both towers at the spot");
        assertTrue(layer.getSitesAt(viewport, x + 60, y).isEmpty());

        layer.setShowSites(false);
        assertTrue(layer.getSitesAt(viewport, x, y).isEmpty(), "hidden sites aren't found");
    }

    @Test
    void cardShowsSystemSiteAndChannels()
    {
        SiteCard.Content content = SiteCard.content(List.of(site("Fire & Rescue <Interop>", "Syracuse", 43.0481,
            -76.1474, 30)), null);
        assertEquals(1, content.entries().size());
        SiteCard.Entry entry = content.entries().getFirst();
        assertEquals("Fire & Rescue <Interop>", entry.title());
        assertEquals(List.of("Project 25 Phase II · APCO-25 Common Air Interface Exclusive",
            "Site 3 · Syracuse · Onondaga County", "NAC 1A3 · RFSS 1 · Zone 2"), entry.lines(),
            "no distance without a location");
        assertEquals(new SiteCard.Channel("851.01250", "C"), entry.channels().get(0), "control channel marked");
        assertEquals(new SiteCard.Channel("851.26250", "A"), entry.channels().get(1), "alternate control marked");
        assertEquals(new SiteCard.Channel("851.51250", null), entry.channels().get(2));
        assertEquals(24, entry.channels().size(), "24 channels shown");
        assertEquals(List.of("+ 6 more channels · C control, A alternate control"), entry.notes());
        assertEquals(0, content.moreSites());
    }

    @Test
    void cardLimitsSites()
    {
        List<SiteRecord> sites = new ArrayList<>();

        for(int index = 0; index < 5; index++)
        {
            sites.add(site("System " + index, "Syracuse", 43.0481, -76.1474, 2));
        }

        SiteCard.Content content = SiteCard.content(sites, null);
        assertEquals(List.of("System 0", "System 1", "System 2"),
            content.entries().stream().map(SiteCard.Entry::title).toList());
        assertEquals(2, content.moreSites());
        assertFalse(content.entries().getFirst().notes().getFirst().contains("more channels"));
    }
}
