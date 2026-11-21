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

import javafx.scene.paint.Color;

/**
 * Builds waterfall color palettes that map an intensity index (0 - 255) to an opaque ARGB color.
 */
public class WaterfallPalette
{
    public static final int SIZE = 256;

    private WaterfallPalette()
    {
    }

    /**
     * Palette based on the original Swing waterfall colors: a dark to bright blue ramp, a blue to yellow ramp, yellow
     * fading to orange, then red.  The blue ramp covers the bottom quarter of the palette, where the waterfall places
     * the noise floor, so the noise stays blue and weak signals stand out from it.
     */
    public static int[] classic()
    {
        int[] palette = new int[SIZE];

        for(int x = 0; x < 64; x++)
        {
            palette[x] = argb(0, 0, blend(64, 191, x / 63.0));
        }

        for(int x = 64; x < 100; x++)
        {
            double t = (x - 64) / 36.0;
            palette[x] = argb(blend(0, 255, t), blend(0, 255, t), blend(191, 0, t));
        }

        for(int x = 100; x < 220; x++)
        {
            palette[x] = argb(255, blend(255, 0, (x - 100) / 120.0), 0);
        }

        for(int x = 220; x < SIZE; x++)
        {
            palette[x] = argb(255, 0, 0);
        }

        return palette;
    }

    private static int blend(int from, int to, double fraction)
    {
        return (int)Math.round(from + (to - from) * fraction);
    }

    /**
     * Palette that blends linearly between evenly spaced colors.
     * @param colors web color strings (for example #440154), lowest intensity first
     */
    public static int[] gradient(String... colors)
    {
        if(colors.length < 2)
        {
            throw new IllegalArgumentException("A gradient palette needs at least two colors");
        }

        Color[] stops = new Color[colors.length];

        for(int x = 0; x < colors.length; x++)
        {
            stops[x] = Color.web(colors[x]);
        }

        int[] palette = new int[SIZE];
        int segments = stops.length - 1;

        for(int x = 0; x < SIZE; x++)
        {
            double position = (double)x / (SIZE - 1) * segments;
            int segment = Math.min(segments - 1, (int)Math.floor(position));
            Color color = stops[segment].interpolate(stops[segment + 1], position - segment);
            palette[x] = argb((int)Math.round(color.getRed() * 255), (int)Math.round(color.getGreen() * 255),
                    (int)Math.round(color.getBlue() * 255));
        }

        return palette;
    }

    /**
     * JavaFX color for an ARGB palette entry.
     */
    public static Color toColor(int argb)
    {
        return Color.rgb((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
    }

    private static int argb(int r, int g, int b)
    {
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }
}
