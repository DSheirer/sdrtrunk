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

import com.formdev.flatlaf.FlatLaf;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import jiconfont.IconCode;
import jiconfont.swing.IconFontSwing;

/**
 * Swing font-glyph icon whose colour follows the active theme.
 *
 * <p>{@link IconFontSwing#buildIcon(IconCode, float)} bakes a fixed colour (black by default) into
 * the rendered image, so those icons never change when the user switches themes and disappear on
 * dark palettes.  This icon resolves its colour from {@link UIManager} each time it is painted and
 * only re-renders the glyph when that colour changes, so it automatically tracks theme changes
 * applied by {@link ThemeManager}.
 *
 * <p>Colour resolution:
 * <ul>
 *   <li>Disabled component: {@code Label.disabledForeground}</li>
 *   <li>Armed/selected menu item: {@code MenuItem.selectionForeground}</li>
 *   <li>Otherwise: the configured foreground key (default {@code Label.foreground})</li>
 * </ul>
 */
public class ThemedFontIcon implements Icon, FlatLaf.DisabledIconProvider
{
    private static final String DEFAULT_FOREGROUND_KEY = "Label.foreground";
    private static final String DISABLED_FOREGROUND_KEY = "Label.disabledForeground";
    private static final String SELECTION_FOREGROUND_KEY = "MenuItem.selectionForeground";

    private final IconCode mIconCode;
    private final float mSize;
    private final String mForegroundKey;
    private final boolean mForceDisabled;
    private Color mCachedColor;
    private Icon mCachedIcon;
    private ThemedFontIcon mDisabledIcon;

    /**
     * Constructs a theme-aware glyph icon using the standard label foreground colour.
     * @param iconCode glyph to render
     * @param size in points
     */
    public ThemedFontIcon(IconCode iconCode, float size)
    {
        this(iconCode, size, DEFAULT_FOREGROUND_KEY);
    }

    /**
     * Constructs a theme-aware glyph icon using the specified UIManager colour key.
     * @param iconCode glyph to render
     * @param size in points
     * @param foregroundKey UIManager colour key used for the normal (enabled) state
     */
    public ThemedFontIcon(IconCode iconCode, float size, String foregroundKey)
    {
        this(iconCode, size, foregroundKey, false);
    }

    private ThemedFontIcon(IconCode iconCode, float size, String foregroundKey, boolean forceDisabled)
    {
        mIconCode = iconCode;
        mSize = size;
        mForegroundKey = foregroundKey;
        mForceDisabled = forceDisabled;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        getIcon(resolveColor(c)).paintIcon(c, g, x, y);
    }

    //Glyph dimensions do not depend on colour, so size queries reuse whatever is cached rather than
    //forcing a re-render in a different colour between paints.
    @Override
    public int getIconWidth()
    {
        return getSizingIcon().getIconWidth();
    }

    @Override
    public int getIconHeight()
    {
        return getSizingIcon().getIconHeight();
    }

    private synchronized Icon getSizingIcon()
    {
        return mCachedIcon != null ? mCachedIcon : getIcon(resolveColor(null));
    }

    /**
     * Disabled variant used by FlatLaf when the owning component is disabled.
     */
    @Override
    public Icon getDisabledIcon()
    {
        if(mDisabledIcon == null)
        {
            mDisabledIcon = new ThemedFontIcon(mIconCode, mSize, mForegroundKey, true);
        }

        return mDisabledIcon;
    }

    /**
     * Determines the colour to paint with from the current theme and component state.
     */
    private Color resolveColor(Component c)
    {
        if(mForceDisabled || (c != null && !c.isEnabled()))
        {
            return color(DISABLED_FOREGROUND_KEY, Color.GRAY);
        }

        //Menu items render their text in the selection foreground while armed (hovered) - match it
        //so the glyph stays legible against the selection background.  Top-level menus on the menu
        //bar report as 'selected' while their popup is open.
        if(c instanceof JMenuItem menuItem)
        {
            ButtonModel model = menuItem.getModel();
            boolean highlighted = model.isArmed() || (menuItem instanceof JMenu && model.isSelected());

            if(highlighted)
            {
                return color(SELECTION_FOREGROUND_KEY, color(mForegroundKey, Color.BLACK));
            }
        }

        return color(mForegroundKey, Color.BLACK);
    }

    private static Color color(String key, Color fallback)
    {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }

    /**
     * Returns the glyph rendered in the requested colour, re-rendering only when the colour changes.
     */
    private synchronized Icon getIcon(Color color)
    {
        if(mCachedIcon == null || !color.equals(mCachedColor))
        {
            mCachedIcon = IconFontSwing.buildIcon(mIconCode, mSize, color);
            mCachedColor = color;
        }

        return mCachedIcon;
    }
}
