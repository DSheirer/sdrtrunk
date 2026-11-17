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

import io.github.dsheirer.map.GeoPosition;
import org.junit.jupiter.api.Test;

/**
 * Tests the map projection and viewport conversions
 */
public class MapViewportTest
{
    @Test
    void projectionRoundTrips()
    {
        assertEquals(0.5, Mercator.x(0), 1e-12);
        assertEquals(0.5, Mercator.y(0), 1e-12);
        assertEquals(0.0, Mercator.y(Mercator.MAX_LATITUDE), 1e-9, "top of the map");
        assertEquals(43.048, Mercator.latitude(Mercator.y(43.048)), 1e-9);
        assertEquals(-76.147, Mercator.longitude(Mercator.x(-76.147)), 1e-9);
        assertEquals(-170.0, Mercator.normalizeLongitude(190.0), 1e-9);
    }

    @Test
    void openStreetMapTileOfAPosition()
    {
        //Syracuse, NY is in tile 12/1181/1504 of the OpenStreetMap tiles
        int zoom = 12;
        assertEquals(1181, (int)Math.floor(Mercator.x(-76.147) * Mercator.tileCount(zoom)));
        assertEquals(1504, (int)Math.floor(Mercator.y(43.048) * Mercator.tileCount(zoom)));
    }

    @Test
    void centerIsInTheMiddleOfTheView()
    {
        MapViewport viewport = new MapViewport();
        viewport.setSize(800, 600);
        viewport.setZoom(12);
        viewport.setCenter(new GeoPosition(43.048, -76.147));

        assertEquals(400, viewport.getScreenX(-76.147), 0.5);
        assertEquals(300, viewport.getScreenY(43.048), 0.5);
        GeoPosition center = viewport.getPosition(400, 300);
        assertEquals(43.048, center.getLatitude(), 1e-3);
        assertEquals(-76.147, center.getLongitude(), 1e-3);
    }

    @Test
    void zoomKeepsThePointUnderTheMouse()
    {
        MapViewport viewport = new MapViewport();
        viewport.setSize(800, 600);
        viewport.setZoom(10);
        viewport.setCenter(new GeoPosition(43.048, -76.147));
        GeoPosition before = viewport.getPosition(650, 120);

        viewport.zoomAt(13, 650, 120);

        GeoPosition after = viewport.getPosition(650, 120);
        assertEquals(13, viewport.getZoom());
        //Within a pixel at zoom 13, since the view edges are whole pixels
        assertEquals(before.getLatitude(), after.getLatitude(), 2e-4);
        assertEquals(before.getLongitude(), after.getLongitude(), 2e-4);
    }

    @Test
    void panWrapsAroundTheWorldAndStopsAtThePoles()
    {
        MapViewport viewport = new MapViewport();
        viewport.setSize(800, 600);
        viewport.setZoom(3);
        viewport.setCenter(new GeoPosition(0, 179));

        viewport.pan(Mercator.worldSize(3) * 0.01, 0);
        assertEquals(-177.4, viewport.getCenter().getLongitude(), 0.01, "wrapped past 180 degrees");

        viewport.pan(0, -1_000_000);
        assertEquals(0, viewport.getTop(), "stops at the top of the map");
        viewport.pan(0, 1_000_000);
        assertEquals(Mercator.worldSize(3) - 600, viewport.getTop(), "stops at the bottom of the map");
    }

    @Test
    void positionsAcrossTheDateLineAreDrawnNearTheView()
    {
        MapViewport viewport = new MapViewport();
        viewport.setSize(800, 600);
        viewport.setZoom(4);
        viewport.setCenter(new GeoPosition(0, 179.5));

        //Just east of the date line is on the right of the view, not a world away
        double x = viewport.getScreenX(-179.5);
        assertEquals(400 + Mercator.worldSize(4) / 360.0, x, 1.0);
    }
}
