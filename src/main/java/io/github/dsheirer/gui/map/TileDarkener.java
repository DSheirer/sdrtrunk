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

import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * Recolors light map tiles for a dark theme: inverts each pixel's lightness while keeping its hue, so a light
 * background turns dark, water stays blue and roads keep their colors.  The lightness is kept within a range so
 * the darkest areas aren't black and the lightest aren't white, and the colors are muted a little.
 */
public final class TileDarkener
{
    //Lightness of a white pixel after recoloring, and of a black pixel
    static final double DARKEST = 0.10;
    static final double LIGHTEST = 0.85;
    private static final double SATURATION = 0.80;

    private TileDarkener()
    {
    }

    /**
     * Recolored copy of the image.  Can be called on any thread.
     */
    public static Image darken(Image image)
    {
        int width = (int)image.getWidth();
        int height = (int)image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);

        for(int index = 0; index < pixels.length; index++)
        {
            pixels[index] = darken(pixels[index]);
        }

        WritableImage darkened = new WritableImage(width, height);
        darkened.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), pixels, 0, width);
        return darkened;
    }

    /**
     * Recolors one ARGB pixel: the HSL lightness L becomes DARKEST + (LIGHTEST - DARKEST) * (1 - L), and the
     * saturation is scaled by SATURATION, keeping the hue and alpha
     */
    static int darken(int argb)
    {
        double r = ((argb >> 16) & 0xFF) / 255.0;
        double g = ((argb >> 8) & 0xFF) / 255.0;
        double b = (argb & 0xFF) / 255.0;
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double lightness = (max + min) / 2.0;
        double chroma = max - min;
        double target = DARKEST + (LIGHTEST - DARKEST) * (1.0 - lightness);

        //Chroma that keeps the saturation at the new lightness: saturation = chroma / (1 - |2L - 1|)
        double room = 1.0 - Math.abs(2.0 * lightness - 1.0);
        double scale = chroma > 0 && room > 0 ? SATURATION * (1.0 - Math.abs(2.0 * target - 1.0)) / room : 0;

        return (argb & 0xFF000000) | (channel(target + (r - lightness) * scale) << 16) |
            (channel(target + (g - lightness) * scale) << 8) | channel(target + (b - lightness) * scale);
    }

    private static int channel(double value)
    {
        return (int)Math.round(Math.clamp(value, 0.0, 1.0) * 255.0);
    }
}
