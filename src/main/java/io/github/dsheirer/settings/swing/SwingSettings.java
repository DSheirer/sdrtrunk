/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.settings.swing;

import io.github.dsheirer.settings.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;

/**
 * Settings for Swing windows and components.
 */
public class SwingSettings
{
    private static final Logger mLog = LoggerFactory.getLogger(SwingSettings.class);



    public SwingSettings()
    {
    }

    /**
     * Returns the stored swing table column width
     * @param key identifying the table
     * @param columnIndex of the column in the swing table
     * @param defaultValue to return if the value has not yet been persisted
     * @return value or default value
     */
    public int getColumnWidth(String key, int columnIndex, int defaultValue)
    {
        return Setting.SWING_COLUMN_WIDTH.with(key, columnIndex).getInt(defaultValue);
    }

    /**
     * Returns the persisted window location for the specified setting key.
     * @param key identifying the window
     * @return location or null
     */
    public Point getLocation(String key)
    {
        int x = Setting.SWING_X.with(key).getInt(Integer.MAX_VALUE);
        int y = Setting.SWING_Y.with(key).getInt(Integer.MAX_VALUE);

        if(x != Integer.MAX_VALUE && y != Integer.MAX_VALUE)
        {
            Point location = new Point(x,y);
            return location;
        }

        return null;
    }

    /**
     * Stores the current window location
     * @param key to identify the window
     * @param location to persist
     */
    public void setLocation(String key, Point location)
    {
        Setting.SWING_X.with(key).putInt(location.x);
        Setting.SWING_Y.with(key).putInt(location.y);
    }

    /**
     * Stores the window maximized state for the window key
     * @param key identifying the window
     * @param maximized state
     */
    public void setMaximized(String key, boolean maximized)
    {
        Setting.SWING_MAXIMIZED.with(key).putBoolean(maximized);
    }

    /**
     * Retrieves the window maximized setting for the specified window key
     * @param key to identify the window
     * @param defaultMaximized to be the default value when no setting exists
     * @return maximized state
     */
    public boolean getMaximized(String key, boolean defaultMaximized)
    {
        return Setting.SWING_MAXIMIZED.with(key).getBoolean(defaultMaximized);
    }

    /**
     * Returns the stored size/dimension of the window identified by the key argument.
     * @param key to identify the window
     * @return dimension or null
     */
    public Dimension getDimension(String key)
    {
        int height = Setting.SWING_HEIGHT.with(key).getInt(Integer.MAX_VALUE);
        int width = Setting.SWING_WIDTH.with(key).getInt(Integer.MAX_VALUE);

        if(height != Integer.MAX_VALUE && width != Integer.MAX_VALUE)
        {
            return new Dimension(width, height);
        }

        return null;
    }

    public void setDimension(String key, Dimension dimension)
    {
        Setting.SWING_HEIGHT.with(key).putInt(dimension.height);
        Setting.SWING_WIDTH.with(key).putInt(dimension.width);
    }

    /**
     * Tests a stored window location to determine if it can be displayed on the currently available
     * graphics display devices so that we don't position a window at a previously available location
     * that is not currently available.
     *
     * @param location to test
     * @return true if the location is displayable.
     */
    private boolean isValidWindowLocation(Point location)
    {
        for(GraphicsDevice gd:GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
        {
            if(gd.getDefaultConfiguration().getBounds().contains(location))
            {
                return true;
            }
        }

        return false;
    }
}
