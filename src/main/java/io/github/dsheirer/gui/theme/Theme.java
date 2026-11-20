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

/**
 * Application color themes.  Each theme has a palette that the theme manager applies to the JavaFX scenes, and a
 * dark flag that adds the dark stylesheet.  The palettes are the colors of the FlatLaf themes that sdrtrunk used for
 * its Swing windows, so the themes look the same after the move to JavaFX.
 */
public enum Theme
{
    LIGHT("Light", false,
        new Palette(0xf2f2f2, 0xffffff, 0xfafafa, 0x2675bf, 0x2675bf, 0xffffff, 0x000000, 0xc2c2c2)),
    DARK("Dark", true,
        new Palette(0x3c3f41, 0x46494b, 0x353739, 0x4b6eaf, 0x4b6eaf, 0xeeeeee, 0xdddddd, 0x616365)),
    NORD("Nord", true,
        new Palette(0x2e3440, 0x3b4252, 0x353739, 0x88c0d0, 0x88c0d0, 0x2e3440, 0xeceff4, 0x3b4252)),
    DRACULA("Dracula", true,
        new Palette(0x414450, 0x3a3d4c, 0x353739, 0xff79c6, 0x6272a4, 0xf8f8f2, 0xdddddd, 0x6272a4)),
    SOLARIZED_LIGHT("Solarized Light", false,
        new Palette(0xeee8d5, 0xfdf6e3, 0xfafafa, 0x3d7ec0, 0x3570cd, 0xffffeb, 0x2e4e58, 0xbac0b8)),
    SOLARIZED_DARK("Solarized Dark", true,
        new Palette(0x0e3c4a, 0x1b4854, 0x353739, 0x3985c7, 0x1c62c9, 0xc2c5bc, 0xa9b3ae, 0x49636b)),
    MATERIAL_DEEP_OCEAN("Material Deep Ocean", true,
        new Palette(0x0f111a, 0x090b10, 0x353739, 0x84ffff, 0x3d4669, 0xffffff, 0x8f93a2, 0x1f2233)),
    ARC_DARK("Arc Dark", true,
        new Palette(0x383c4a, 0x474c5b, 0x353739, 0x2679db, 0x2679db, 0xffffff, 0xd3dae3, 0x2b2e39));

    /**
     * Theme colors as 0xRRGGBB values.
     * @param background of windows and panels
     * @param inner background of text fields, tables and lists
     * @param innerAlternate background of alternate table rows
     * @param accent for focus, default buttons and highlights
     * @param selectionBackground of selected rows
     * @param selectionText of selected rows
     * @param text color
     * @param border of controls and scroll bar thumbs
     */
    public record Palette(int background, int inner, int innerAlternate, int accent, int selectionBackground,
                          int selectionText, int text, int border)
    {
    }

    private final String mDisplayName;
    private final boolean mDark;
    private final Palette mPalette;

    Theme(String displayName, boolean dark, Palette palette)
    {
        mDisplayName = displayName;
        mDark = dark;
        mPalette = palette;
    }

    /**
     * @return human-readable name shown in the preferences UI.
     */
    public String getDisplayName()
    {
        return mDisplayName;
    }

    /**
     * @return true if this theme is a dark palette - the JavaFX dark stylesheet is applied for
     *         dark themes and removed for light themes.
     */
    public boolean isDark()
    {
        return mDark;
    }

    /**
     * Theme colors
     */
    public Palette getPalette()
    {
        return mPalette;
    }

    /**
     * Resolve a theme by its enum name, returning {@link #LIGHT} as a safe default if {@code name}
     * is null or unrecognised.
     */
    public static Theme fromName(String name)
    {
        if(name != null)
        {
            for(Theme t: values())
            {
                if(t.name().equals(name))
                {
                    return t;
                }
            }
        }
        return LIGHT;
    }
}
