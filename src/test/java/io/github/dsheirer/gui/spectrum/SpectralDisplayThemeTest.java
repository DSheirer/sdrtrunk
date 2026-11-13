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

package io.github.dsheirer.gui.spectrum;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the spectral display themes and waterfall palettes.
 */
public class SpectralDisplayThemeTest
{
    @Test
    void gradientEndsAtTheFirstAndLastColors()
    {
        int[] palette = WaterfallPalette.gradient("#000000", "#FF8000", "#FFFFFF");
        assertEquals(WaterfallPalette.SIZE, palette.length);
        assertEquals(0xFF000000, palette[0]);
        assertEquals(0xFFFFFFFF, palette[WaterfallPalette.SIZE - 1]);
    }

    @Test
    void classicPaletteMatchesTheOriginalWaterfall()
    {
        int[] palette = WaterfallPalette.classic();
        assertEquals(0xFF00007F, palette[0]);
        assertEquals(0xFF0000BF, palette[16]);
        assertEquals(0xFFFFFF00, palette[60]);
        assertEquals(0xFFFF0000, palette[255]);
    }

    @Test
    void everyThemeHasAnOpaqueWaterfallAndALabel()
    {
        for(SpectralDisplayTheme theme: SpectralDisplayTheme.values())
        {
            assertTrue(theme.getLabel() != null && !theme.getLabel().isEmpty(), theme.name());
            assertEquals(1.0, theme.getBackground().getOpacity(), 1E-9, theme.name());

            for(int index = 0; index < WaterfallPalette.SIZE; index++)
            {
                assertEquals(0xFF, theme.getWaterfallColor(index) >>> 24, theme.name() + " index " + index);
            }
        }
    }

    @Test
    void negativeByteIndexesMapToTheUpperPalette()
    {
        SpectralDisplayTheme theme = SpectralDisplayTheme.CLASSIC;
        assertEquals(theme.getWaterfallColor(255), theme.getWaterfallColor((byte)255));
    }
}
