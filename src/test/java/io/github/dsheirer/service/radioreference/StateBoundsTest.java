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

package io.github.dsheirer.service.radioreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests finding the states around a location
 */
public class StateBoundsTest
{
    private static List<String> near(double latitude, double longitude)
    {
        return StateBounds.near(latitude, longitude, RadioReferenceFrequencyIndex.RADIUS_KILOMETERS).stream()
            .map(StateBounds::key).toList();
    }

    @Test
    void locationStateIsFirst()
    {
        //Fulton, NY: across Lake Ontario from Ontario, more than 80 km from Pennsylvania
        List<String> fulton = near(43.331042, -76.37563);
        assertEquals("US-NY", fulton.get(0));
        assertFalse(fulton.contains("US-PA"));
        assertFalse(fulton.contains("US-AZ"));

        //Phoenix, AZ
        assertEquals(List.of("US-AZ"), near(33.4484, -112.0740));
    }

    @Test
    void includesNeighborsNearABorder()
    {
        //Kansas City, MO is on the Kansas border
        List<String> kansasCity = near(39.0997, -94.5786);
        assertTrue(kansasCity.contains("US-MO"));
        assertTrue(kansasCity.contains("US-KS"));

        //Windsor, ON is across the river from Detroit, MI
        List<String> windsor = near(42.3149, -83.0364);
        assertTrue(windsor.contains("CA-ON"));
        assertTrue(windsor.contains("US-MI"));
    }

    @Test
    void outsideTheUnitedStatesAndCanada()
    {
        //London, UK
        assertTrue(near(51.5074, -0.1278).isEmpty());
    }
}
