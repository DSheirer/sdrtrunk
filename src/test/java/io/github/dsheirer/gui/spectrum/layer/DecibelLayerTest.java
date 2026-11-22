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
 * Tests the decibel scale grid step.
 */
public class DecibelLayerTest
{
    @Test
    void stepKeepsLinesAtLeast28PixelsApart()
    {
        //100 dB over 300 pixels = 3 pixels per dB: 10 dB = 30 pixels
        assertEquals(10.0, DecibelLayer.getStep(100, 300));
        //100 dB over 1000 pixels = 10 pixels per dB: 5 dB = 50 pixels
        assertEquals(5.0, DecibelLayer.getStep(100, 1000));
        //100 dB over 100 pixels = 1 pixel per dB: 50 dB = 50 pixels
        assertEquals(50.0, DecibelLayer.getStep(100, 100));
        assertEquals(50.0, DecibelLayer.getStep(100, 0));
    }
}
