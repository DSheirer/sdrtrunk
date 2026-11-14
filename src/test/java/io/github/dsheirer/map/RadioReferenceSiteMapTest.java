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

package io.github.dsheirer.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.service.radioreference.SiteRecord;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;
import org.junit.jupiter.api.Test;

/**
 * Tests the radio reference site markers and card on the map, without loading map tiles
 */
public class RadioReferenceSiteMapTest
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
        //Offline tile source with the OSM geometry - nothing is loaded without painting
        JXMapViewer map = new JXMapViewer();
        map.setTileFactory(new DefaultTileFactory(new TileFactoryInfo(1, 17, 19, 256, true, true,
            "file:///nonexistent", "x", "y", "z")));
        map.setSize(800, 600);
        map.setZoom(6);
        map.setCenterPosition(new GeoPosition(43.05, -76.15));

        SiteRecord syracuse = site("System A", "Syracuse", 43.0481, -76.1474, 4);
        SiteRecord sameTower = site("System B", "Syracuse", 43.0481, -76.1474, 4);
        SiteRecord fulton = site("System C", "Fulton", 43.3226, -76.4171, 4);
        RadioReferenceSitePainter painter = new RadioReferenceSitePainter(() -> List.of(syracuse, sameTower, fulton));

        Point2D pixel = map.getTileFactory().geoToPixel(new GeoPosition(43.0481, -76.1474), map.getZoom());
        Point tower = new Point((int)Math.round(pixel.getX() - map.getViewportBounds().getX()),
            (int)Math.round(pixel.getY() - map.getViewportBounds().getY()) - 8);

        assertEquals(List.of("System A", "System B"),
            painter.getSitesAt(map, tower).stream().map(SiteRecord::systemName).toList(), "both towers at the spot");
        assertTrue(painter.getSitesAt(map, new Point(tower.x + 60, tower.y)).isEmpty());

        painter.setShowSites(false);
        assertTrue(painter.getSitesAt(map, tower).isEmpty(), "hidden sites aren't found");
    }

    @Test
    void cardShowsSystemSiteAndChannels()
    {
        String html = RadioReferenceSiteCard.html(List.of(site("Fire & Rescue <Interop>", "Syracuse", 43.0481,
            -76.1474, 30)), null);

        assertTrue(html.contains("<b>Fire &amp; Rescue &lt;Interop&gt;</b>"), "escaped system name");
        assertTrue(html.contains("Project 25 Phase II"));
        assertTrue(html.contains("Site 3 &middot; Syracuse &middot; Onondaga County"));
        assertTrue(html.contains("NAC 1A3 &middot; RFSS 1 &middot; Zone 2"));
        assertTrue(html.contains("851.01250 <b>C</b>"), "control channel marked");
        assertTrue(html.contains("851.26250 <b>A</b>"), "alternate control channel marked");
        assertTrue(html.contains("+ 6 more channels"), "24 channels shown");
        assertFalse(html.contains(" mi") || html.contains(" km"), "no distance without a location");
    }

    @Test
    void cardLimitsSites()
    {
        List<SiteRecord> sites = new ArrayList<>();

        for(int index = 0; index < 5; index++)
        {
            sites.add(site("System " + index, "Syracuse", 43.0481, -76.1474, 2));
        }

        String html = RadioReferenceSiteCard.html(sites, null);
        assertTrue(html.contains("System 2"));
        assertFalse(html.contains("System 3"));
        assertTrue(html.contains("+ 2 more sites here"));
    }
}
