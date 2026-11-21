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
    void classicPaletteUsesTheOriginalWaterfallColors()
    {
        int[] palette = WaterfallPalette.classic();
        assertEquals(0xFF0000BF, palette[63]);
        assertEquals(0xFFFFFF00, palette[100]);
        assertEquals(0xFFFF0000, palette[255]);
    }

    @Test
    void classicPaletteHasDistinctColorsNearTheNoiseFloor()
    {
        int[] palette = WaterfallPalette.classic();

        for(int index = 1; index < 64; index++)
        {
            assertTrue((palette[index] & 0xFF) > (palette[index - 1] & 0xFF), "index " + index);
        }
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

    @Test
    void channelMenuColorsAreReadableAndDistinct()
    {
        for(SpectralDisplayTheme theme: SpectralDisplayTheme.values())
        {
            javafx.scene.paint.Color background = theme.getBackground();
            javafx.scene.paint.Color active = theme.getChannelActive();
            javafx.scene.paint.Color inactive = theme.getChannelInactive();
            javafx.scene.paint.Color text = theme.getText();

            //Readable without the menu having to adjust them
            assertTrue(ChannelMenus.contrast(active, background) >= 3.0, theme + " active contrast");
            assertTrue(ChannelMenus.contrast(inactive, background) >= 3.0, theme + " inactive contrast");
            assertTrue(ChannelMenus.contrast(text, background) >= 4.5, theme + " text contrast");
            assertTrue(!active.equals(inactive) && !active.equals(text) && !inactive.equals(text),
                theme + " colors differ");
        }
    }
}
