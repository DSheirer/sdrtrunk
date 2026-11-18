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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests choosing tiles for the theme and the user's key, and recoloring light tiles for dark themes
 */
public class TileServerTest
{
    @Test
    void serverWithoutDarkStyleIsRecoloredInDarkThemes()
    {
        TileSource light = TileServer.OPEN_STREET_MAP.getSource(false, null);
        TileSource dark = TileServer.OPEN_STREET_MAP.getSource(true, null);

        assertFalse(light.darken());
        assertFalse(light.dark());
        assertTrue(dark.darken());
        assertTrue(dark.dark());
        assertEquals("osm/light", dark.cacheName(), "recolored from the cached light tiles");
        assertEquals("https://tile.openstreetmap.org/12/1181/1504.png", dark.getUrl(new TileKey(12, 1181, 1504)));
    }

    @Test
    void serverWithDarkStyleFollowsTheTheme()
    {
        TileSource light = TileServer.ESRI_GRAY.getSource(false, null);
        TileSource dark = TileServer.ESRI_GRAY.getSource(true, null);

        assertFalse(dark.darken(), "dark style isn't recolored");
        assertTrue(dark.dark());
        assertEquals("esri.gray/dark", dark.cacheName());
        assertNotEquals(light.cacheName(), dark.cacheName());
        assertEquals("https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/" +
            "12/1504/1181", dark.getUrl(new TileKey(12, 1181, 1504)), "row before column");
    }

    @Test
    void keyedServerNeedsAKey()
    {
        TileSource missing = TileServer.CARTO.getSource(true, "  ");
        assertFalse(missing.isAvailable());
        assertTrue(missing.unavailableReason().contains("API key"));

        TileSource keyed = TileServer.CARTO.getSource(true, " abc+def ");
        assertTrue(keyed.isAvailable());
        assertNull(keyed.unavailableReason());
        assertEquals("https://basemaps.cartocdn.com/rastertiles/dark_all/5/9/12.png?key=abc%2Bdef",
            keyed.getUrl(new TileKey(5, 9, 12)), "key trimmed and encoded");
        assertEquals("carto/dark", keyed.cacheName(), "cache doesn't depend on the key");
    }

    @Test
    void subdomainsSpreadTheTiles()
    {
        TileSource source = TileServer.OPEN_TOPO_MAP.getSource(false, null);
        assertEquals("https://a.tile.opentopomap.org/1/0/0.png", source.getUrl(new TileKey(1, 0, 0)));
        assertEquals("https://b.tile.opentopomap.org/1/1/0.png", source.getUrl(new TileKey(1, 1, 0)));
        assertEquals("https://c.tile.opentopomap.org/1/1/1.png", source.getUrl(new TileKey(1, 1, 1)));
    }

    @Test
    void unknownServerIsOpenStreetMap()
    {
        assertEquals(TileServer.OPEN_STREET_MAP, TileServer.get(null));
        assertEquals(TileServer.OPEN_STREET_MAP, TileServer.get("gone"));
        assertEquals(TileServer.CARTO, TileServer.get("carto"));
    }

    @Test
    void choiceFollowsTheThemeOrKeepsItsStyle()
    {
        TileChoice theme = TileChoice.get("carto", null);
        assertEquals(TileChoice.Style.THEME, theme.style(), "stored server without a style follows the theme");
        assertEquals("carto/dark", theme.getSource(true, "k").cacheName());
        assertEquals("carto/light", theme.getSource(false, "k").cacheName());

        TileChoice light = TileChoice.get("carto", "LIGHT");
        assertEquals("carto/light", light.getSource(true, "k").cacheName(), "light in a dark theme");
        assertFalse(light.getSource(true, "k").dark());

        TileSource dark = TileChoice.get("osm", "DARK").getSource(false, null);
        assertTrue(dark.dark(), "dark in a light theme");
        assertTrue(dark.darken(), "recolored, no dark style");

        assertEquals(TileChoice.Style.THEME, TileChoice.get("osm", "gone").style());
        assertEquals(TileServer.SERVERS.size() * 3, TileChoice.CHOICES.size());
        assertEquals("CARTO - Auto", theme.toString());
        assertEquals("CARTO - Dark", TileChoice.get("carto", "DARK").toString());
    }

    @Test
    void darkenerInvertsLightnessAndKeepsHue()
    {
        assertEquals(gray(TileDarkener.DARKEST), TileDarkener.darken(0xFFFFFFFF) & 0xFFFFFF, "white turns dark");
        assertEquals(gray(TileDarkener.LIGHTEST), TileDarkener.darken(0xFF000000) & 0xFFFFFF, "black turns light");
        assertEquals(0x80000000, TileDarkener.darken(0x80FFFFFF) & 0xFF000000, "alpha kept");

        //Light blue water turns dark blue: blue stays the strongest channel and it gets darker
        int water = TileDarkener.darken(0xFFAAD3DF);
        int r = (water >> 16) & 0xFF;
        int g = (water >> 8) & 0xFF;
        int b = water & 0xFF;
        assertTrue(b > g && g > r, "hue kept");
        assertTrue(b < 0x80, "darker");
    }

    private static int gray(double lightness)
    {
        int value = (int)Math.round(lightness * 255.0);
        return (value << 16) | (value << 8) | value;
    }
}
