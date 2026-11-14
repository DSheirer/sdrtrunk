/*
 * *********************************************************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 * *********************************************************************************************************************
 */

package io.github.dsheirer.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jdesktop.swingx.mapviewer.GeoPosition;

/**
 * Map view stored in the settings file by older versions of the application.  Only read so that settings files from
 * older versions still load and their default view can be moved to the map preferences (MapPreference).  The settings
 * manager drops map views after loading, so they aren't saved again.
 */
@JsonSubTypes.Type(value = MapViewSetting.class, name = "mapViewSetting")
@Deprecated
public class MapViewSetting extends Setting
{
    private double mLatitude;
    private double mLongitude;
    private int mZoom;

    public MapViewSetting()
    {
        /* Empty constructor for JAXB */
    }

    public MapViewSetting(String name, double latitude, double longitude, int zoom)
    {
        super(name);
        mLatitude = latitude;
        mLongitude = longitude;
        mZoom = zoom;
    }

    public MapViewSetting(String name, GeoPosition position, int zoom)
    {
        super(name);
        mLatitude = position.getLatitude();
        mLongitude = position.getLongitude();
        mZoom = zoom;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public SettingType getType()
    {
        return SettingType.MAP_VIEW_SETTING;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "latitude")
    public double getLatitude()
    {
        return mLatitude;
    }

    public void setLatitude(double latitude)
    {
        mLatitude = latitude;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "longitude")
    public double getLongitude()
    {
        return mLongitude;
    }

    public void setLongitude(double longitude)
    {
        mLongitude = longitude;
    }

    @JacksonXmlProperty(isAttribute = true, localName = "zoom")
    public int getZoom()
    {
        return mZoom;
    }

    public void setZoom(int zoom)
    {
        mZoom = zoom;
    }

    @JsonIgnore
    public GeoPosition getGeoPosition()
    {
        return new GeoPosition(mLatitude, mLongitude);
    }

    public void setGeoPosition(GeoPosition position)
    {
        mLatitude = position.getLatitude();
        mLongitude = position.getLongitude();
    }
}
