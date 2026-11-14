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

package io.github.dsheirer.preference.map;

import io.github.dsheirer.preference.Preference;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.sample.Listener;
import java.util.Objects;
import java.util.prefs.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map preferences: the last center point and zoom level of the map, restored when the application starts, and the
 * map layers to show.  The user's own location is a radio reference preference, shown on the map as a pin.
 */
public class MapPreference extends Preference
{
    private static final Logger mLog = LoggerFactory.getLogger(MapPreference.class);

    /**
     * Zoom level when none is stored
     */
    public static final int DEFAULT_ZOOM = 7;

    private static final String CENTER = "center";
    private static final String ZOOM = "zoom";
    private static final String SHOW_RADIO_REFERENCE_SITES = "show.radioreference.sites";
    private final Preferences mPreferences = Preferences.userNodeForPackage(MapPreference.class);

    /**
     * Map center point
     * @param latitude in decimal degrees, -90 to 90
     * @param longitude in decimal degrees, -180 to 180
     */
    public record Center(double latitude, double longitude)
    {
        /**
         * Constructs an instance
         * @throws IllegalArgumentException if the coordinates are out of range
         */
        public Center
        {
            if(!Double.isFinite(latitude) || !Double.isFinite(longitude) || Math.abs(latitude) > 90.0 ||
                Math.abs(longitude) > 180.0)
            {
                throw new IllegalArgumentException("Invalid map center: " + latitude + ", " + longitude);
            }
        }
    }

    /**
     * Constructs an instance
     * @param updateListener to receive notifications that a preference has been updated
     */
    public MapPreference(Listener<PreferenceType> updateListener)
    {
        super(updateListener);
    }

    @Override
    public PreferenceType getPreferenceType()
    {
        return PreferenceType.MAP;
    }

    /**
     * Last center point of the map.  Can be called from any thread.
     * @return center, or null if none is stored
     */
    public Center getCenter()
    {
        String value = mPreferences.get(CENTER, null);

        if(value != null)
        {
            String[] parts = value.split(",");

            if(parts.length == 2)
            {
                try
                {
                    return new Center(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
                }
                catch(IllegalArgumentException e)
                {
                    mLog.warn("Ignoring invalid stored map center [" + value + "]");
                }
            }
        }

        return null;
    }

    /**
     * Stores the center point of the map
     * @param center to store
     */
    public void setCenter(Center center)
    {
        if(center != null && !Objects.equals(center, getCenter()))
        {
            //Stored as one value so that a reader never sees the latitude of one point with the longitude of another
            mPreferences.put(CENTER, center.latitude() + "," + center.longitude());
            notifyPreferenceUpdated();
        }
    }

    /**
     * Last zoom level of the map.  Can be called from any thread.
     * @return zoom level, or {@link #DEFAULT_ZOOM} if none is stored
     */
    public int getZoom()
    {
        return mPreferences.getInt(ZOOM, DEFAULT_ZOOM);
    }

    /**
     * Stores the zoom level of the map
     * @param zoom level
     */
    public void setZoom(int zoom)
    {
        if(zoom != getZoom())
        {
            mPreferences.putInt(ZOOM, zoom);
            notifyPreferenceUpdated();
        }
    }

    /**
     * Indicates if the map shows the radio reference trunked system sites near the user
     */
    public boolean isShowRadioReferenceSites()
    {
        return mPreferences.getBoolean(SHOW_RADIO_REFERENCE_SITES, true);
    }

    /**
     * Shows or hides the radio reference trunked system sites on the map
     */
    public void setShowRadioReferenceSites(boolean show)
    {
        if(show != isShowRadioReferenceSites())
        {
            mPreferences.putBoolean(SHOW_RADIO_REFERENCE_SITES, show);
            notifyPreferenceUpdated();
        }
    }

    /**
     * Moves the map view stored in the settings file by older versions of the application into this preference.
     * Does nothing once a center point is stored here.
     * @param latitude of the stored map view
     * @param longitude of the stored map view
     * @param zoom of the stored map view
     */
    public void migrateLegacyView(double latitude, double longitude, int zoom)
    {
        if(getCenter() == null)
        {
            try
            {
                setCenter(new Center(latitude, longitude));
                setZoom(zoom);
                mLog.info("Moved the map view from the settings file to the map preferences");
            }
            catch(IllegalArgumentException e)
            {
                mLog.warn("Ignoring invalid map view from the settings file: " + latitude + ", " + longitude);
            }
        }
    }
}
