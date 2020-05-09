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

package io.github.dsheirer.preference.swing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.util.prefs.Preferences;

/**
 * User preference settings for Swing windows and components.
 */
public class SwingPreference
{
    private static final Logger mLog = LoggerFactory.getLogger(SwingPreference.class);

    private static final String LOCATION_X = ".x";
    private static final String LOCATION_Y = ".y";
    private static final String SIZE_HEIGHT = ".height";
    private static final String SIZE_WIDTH = ".width";
    private static final String MAXIMIZED = ".maximized";

    private Preferences mPreferences = Preferences.userNodeForPackage(SwingPreference.class);

    public SwingPreference()
    {
    }

    /**
     * Returns the stored swing related integer value
     * @param key for accessing the value
     * @param defaultValue to return if the value has not yet been persisted
     * @return value or default value
     */
    public int getInt(String key, int defaultValue)
    {
        return mPreferences.getInt(key, defaultValue);
    }

    /**
     * Stores the swing related integer value
     * @param key to reference the value
     */
    public void setInt(String key, int value)
    {
        mPreferences.putInt(key, value);
    }

    /**
     * Returns the persisted window location for the specified preference key.
     * @param key identifying the window
     * @return location or null
     */
    public Point getLocation(String key)
    {
        int x = mPreferences.getInt(key + LOCATION_X, Integer.MAX_VALUE);
        int y = mPreferences.getInt(key + LOCATION_Y, Integer.MAX_VALUE);

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
        mPreferences.putInt(key + LOCATION_X, location.x);
        mPreferences.putInt(key + LOCATION_Y, location.y);
    }

    /**
     * Stores the window maximized state for the window key
     * @param key identifying the window
     * @param maximized state
     */
    public void setMaximized(String key, boolean maximized)
    {
        mPreferences.putBoolean(key + MAXIMIZED, maximized);
    }

    /**
     * Retrieves the window maximized preference for the specified window key
     * @param key to identify the window
     * @param defaultMaximized to be the default value when no preference exists
     * @return maximized state
     */
    public boolean getMaximized(String key, boolean defaultMaximized)
    {
        return mPreferences.getBoolean(key + MAXIMIZED, defaultMaximized);
    }

    /**
     * Returns the stored size/dimension of the window identified by the key argument.
     * @param key to identify the window
     * @return dimension or null
     */
    public Dimension getDimension(String key)
    {
        int height = mPreferences.getInt(key + SIZE_HEIGHT, Integer.MAX_VALUE);
        int width = mPreferences.getInt(key + SIZE_WIDTH, Integer.MAX_VALUE);

        if(height != Integer.MAX_VALUE && width != Integer.MAX_VALUE)
        {
            return new Dimension(width, height);
        }

        return null;
    }

    public void setDimension(String key, Dimension dimension)
    {
        mPreferences.putInt(key + SIZE_HEIGHT, dimension.height);
        mPreferences.putInt(key + SIZE_WIDTH, dimension.width);
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
