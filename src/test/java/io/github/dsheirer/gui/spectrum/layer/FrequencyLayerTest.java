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

package io.github.dsheirer.gui.spectrum.layer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the frequency label spacing.
 */
public class FrequencyLayerTest
{
    @Test
    void niceStepRoundsUpToOneTwoOrFive()
    {
        assertEquals(1, FrequencyLayer.niceStep(0.3));
        assertEquals(1, FrequencyLayer.niceStep(1));
        assertEquals(2, FrequencyLayer.niceStep(1.5));
        assertEquals(5, FrequencyLayer.niceStep(3));
        assertEquals(10, FrequencyLayer.niceStep(7));
        assertEquals(100_000, FrequencyLayer.niceStep(100_000));
        assertEquals(200_000, FrequencyLayer.niceStep(120_000));
        assertEquals(500_000, FrequencyLayer.niceStep(250_001));
        assertEquals(1_000_000, FrequencyLayer.niceStep(600_000));
    }
}
