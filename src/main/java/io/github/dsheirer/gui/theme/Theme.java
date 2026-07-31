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
 * Curated preset themes for the application.
 *
 * <p>Each theme pairs a FlatLaf Swing look-and-feel class with a flag indicating whether the
 * palette is dark.  The dark flag drives whether the dark JavaFX stylesheet is applied to
 * registered Scenes.  Other per-theme values (colours used to seed JIDE-specific UIManager
 * keys) are read at runtime from {@link javax.swing.UIManager} after the LAF installs, so each
 * theme automatically contributes its own palette to JIDE components.
 */
public enum Theme
{
    LIGHT("Light", "com.formdev.flatlaf.FlatLightLaf", false),
    DARK("Dark", "com.formdev.flatlaf.FlatDarkLaf", true),
    NORD("Nord", "com.formdev.flatlaf.intellijthemes.FlatNordIJTheme", true),
    DRACULA("Dracula", "com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme", true),
    SOLARIZED_LIGHT("Solarized Light", "com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme", false),
    SOLARIZED_DARK("Solarized Dark", "com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme", true),
    MATERIAL_DEEP_OCEAN("Material Deep Ocean", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDeepOceanIJTheme", true),
    ARC_DARK("Arc Dark", "com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme", true);

    private final String mDisplayName;
    private final String mLafClassName;
    private final boolean mDark;

    Theme(String displayName, String lafClassName, boolean dark)
    {
        mDisplayName = displayName;
        mLafClassName = lafClassName;
        mDark = dark;
    }

    /**
     * @return human-readable name shown in the preferences UI.
     */
    public String getDisplayName()
    {
        return mDisplayName;
    }

    /**
     * @return fully qualified FlatLaf class name to instantiate via reflection.
     */
    public String getLafClassName()
    {
        return mLafClassName;
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
