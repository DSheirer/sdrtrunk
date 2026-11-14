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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.dsheirer.preference.radioreference.RadioReferencePreference.Location;
import org.junit.jupiter.api.Test;

/**
 * Tests coordinate parsing and formatting for the location dialog, and location validation
 */
public class LocationDialogTest
{
    private static final double DELTA = 1e-9;

    @Test
    void parsesPastedPairs()
    {
        assertArrayEquals(new double[]{43.0481, -76.1474}, LocationDialog.parsePair("43.0481, -76.1474"), DELTA);
        assertArrayEquals(new double[]{43.0481, -76.1474}, LocationDialog.parsePair(" 43.0481 -76.1474 "), DELTA);
        assertArrayEquals(new double[]{43.0481, -76.1474}, LocationDialog.parsePair("43.0481; -76.1474"), DELTA);
        assertArrayEquals(new double[]{43.0481, -76.1474}, LocationDialog.parsePair("43.0481° N, 76.1474° W"), DELTA);
        assertArrayEquals(new double[]{-33.8688, 151.2093}, LocationDialog.parsePair("33.8688 S 151.2093 E"), DELTA);
        assertArrayEquals(new double[]{43.0481, -76.1474}, LocationDialog.parsePair("43,0481 -76,1474"), DELTA,
            "decimal commas with a space separator");
    }

    @Test
    void rejectsInvalidPairs()
    {
        assertNull(LocationDialog.parsePair("43,048"), "a decimal comma isn't a separator");
        assertNull(LocationDialog.parsePair("43.0481"));
        assertNull(LocationDialog.parsePair("95, 10"), "latitude out of range");
        assertNull(LocationDialog.parsePair("43, 190"), "longitude out of range");
        assertNull(LocationDialog.parsePair("43.0481 E, 76.1474 W"), "longitude letter on the latitude");
        assertNull(LocationDialog.parsePair("syracuse"));
        assertNull(LocationDialog.parsePair(null));
    }

    @Test
    void parsesSingleCoordinates()
    {
        assertEquals(-76.1474, LocationDialog.parseCoordinate("-76.1474", false), DELTA);
        assertEquals(-76.1474, LocationDialog.parseCoordinate("76.1474° W", false), DELTA);
        assertEquals(43.5, LocationDialog.parseCoordinate("43,5", true), DELTA, "decimal comma");
        assertEquals(90.0, LocationDialog.parseCoordinate("90", true), DELTA);
        assertTrue(Double.isNaN(LocationDialog.parseCoordinate("91", true)));
        assertTrue(Double.isNaN(LocationDialog.parseCoordinate("181", false)));
        assertTrue(Double.isNaN(LocationDialog.parseCoordinate("-5 S", true)), "sign and hemisphere letter");
        assertTrue(Double.isNaN(LocationDialog.parseCoordinate("43 E", true)), "longitude letter on a latitude");
        assertTrue(Double.isNaN(LocationDialog.parseCoordinate("abc", true)));
        assertTrue(Double.isNaN(LocationDialog.parseCoordinate("", true)));
    }

    @Test
    void formatsCoordinates()
    {
        assertEquals("43.048123", LocationDialog.format(43.04812345));
        assertEquals("-76", LocationDialog.format(-76.0));
        assertEquals("Syracuse, New York 13202 (43.0481, -76.1474)",
            new Location(43.04812, -76.14742, "Syracuse, New York 13202").describe());
        assertEquals("43.0481, -76.1474", new Location(43.04812, -76.14742, null).describe());
    }

    @Test
    void locationValidation()
    {
        assertThrows(IllegalArgumentException.class, () -> new Location(91, 0, null));
        assertThrows(IllegalArgumentException.class, () -> new Location(0, -181, null));
        assertThrows(IllegalArgumentException.class, () -> new Location(Double.NaN, 0, null));
        assertNull(new Location(43, -76, "  ").label(), "blank label");
        assertEquals("Syracuse", new Location(43, -76, " Syracuse ").label());
    }
}
