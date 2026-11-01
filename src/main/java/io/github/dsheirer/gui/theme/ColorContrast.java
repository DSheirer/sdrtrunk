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

package io.github.dsheirer.gui.theme;

import java.awt.Color;

/**
 * Color contrast utilities based on the WCAG relative luminance and contrast ratio definitions.
 */
public final class ColorContrast
{
    /**
     * Minimum contrast ratio to treat a color as readable.  This is the WCAG threshold for graphics and large text.
     * The stricter 4.5 threshold for body text would also replace saturated colors like pure red that read fine on a
     * dark background.
     */
    public static final double MINIMUM_CONTRAST_RATIO = 3.0;

    private static final double BLEND_STEP = 0.05;

    private ColorContrast()
    {
    }

    /**
     * Provides a color that is readable against the background.
     * @param color that is preferred.
     * @param background that the color is rendered against.
     * @return the color when it has sufficient contrast, otherwise its RGB complement, adjusted toward white or black
     * when the complement also lacks sufficient contrast (e.g. mid-gray is its own complement).
     */
    public static Color getReadable(Color color, Color background)
    {
        if(getContrastRatio(color, background) >= MINIMUM_CONTRAST_RATIO)
        {
            return color;
        }

        Color complement = new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
        Color target = getContrastRatio(Color.WHITE, background) >= getContrastRatio(Color.BLACK, background) ?
            Color.WHITE : Color.BLACK;

        Color readable = complement;

        for(double fraction = BLEND_STEP; getContrastRatio(readable, background) < MINIMUM_CONTRAST_RATIO &&
            fraction < 1.0; fraction += BLEND_STEP)
        {
            readable = blend(complement, target, fraction);
        }

        return getContrastRatio(readable, background) < MINIMUM_CONTRAST_RATIO ? target : readable;
    }

    /**
     * Contrast ratio between two colors.
     * @return ratio in the range 1.0 (identical luminance) to 21.0 (black and white).
     */
    public static double getContrastRatio(Color a, Color b)
    {
        double luminanceA = getRelativeLuminance(a);
        double luminanceB = getRelativeLuminance(b);
        return (Math.max(luminanceA, luminanceB) + 0.05) / (Math.min(luminanceA, luminanceB) + 0.05);
    }

    private static double getRelativeLuminance(Color color)
    {
        return 0.2126 * linearize(color.getRed()) + 0.7152 * linearize(color.getGreen()) +
            0.0722 * linearize(color.getBlue());
    }

    private static double linearize(int value)
    {
        double normalized = value / 255.0;
        return normalized <= 0.04045 ? normalized / 12.92 : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }

    private static Color blend(Color from, Color to, double fraction)
    {
        return new Color((int)Math.round(from.getRed() + (to.getRed() - from.getRed()) * fraction),
            (int)Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * fraction),
            (int)Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * fraction));
    }
}
