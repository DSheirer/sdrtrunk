/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.alias;

import javafx.scene.paint.Color;

/**
 * Utilities for JavaFX colors
 *
 * Note: the 32-bit integer representation used by this class is compatible with Swing color representations where the
 * value represents: ARGB (alpha, red, green, blue).  This is different from the way that JavaFX represents colors
 * which is RGBA.  This utility uses the Swing representation for backward compatiblity with existing user playlists.
 */
public class ColorUtil
{
    /**
     * Converts the color to a 32 bit integer value
     * @param color to convert
     * @return 32 bit integer representation
     */
    public static int toInteger(Color color)
    {
        return to32BitInteger((int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255),
            (int)(color.getOpacity() * 255));
    }

    /**
     * Converts the RGBA values to a 32-bit integer representation
     * @param red 0-255
     * @param green 0-255
     * @param blue 0-255
     * @param alpha 0-255
     * @return integer representation
     */
    private static int to32BitInteger(int red, int green, int blue, int alpha)
    {
        int i = alpha;
        i = i << 8;
        i = i | red;
        i = i << 8;
        i = i | green;
        i = i << 8;
        i = i | blue;
        return i;
    }

    /**
     * Creates a color object from the 32-bit integer representation
     * @param value that represents an RGBA color
     * @return color instance
     */
    public static Color fromInteger(int value)
    {
        int blue = value & 0xFF;
        value = value >> 8;
        int green = value & 0xFF;
        value = value >> 8;
        int red = value & 0xFF;
        value = value >> 8;
        double alpha = (double)(value & 0xFF) / 255.0;

        Color color = Color.rgb(red, green, blue, alpha);
        return color;
    }

    public static void main(String[] args)
    {
        int rgb = -1;

        Color color = ColorUtil.fromInteger(rgb);

        int converted = ColorUtil.toInteger(color);

    }
}
