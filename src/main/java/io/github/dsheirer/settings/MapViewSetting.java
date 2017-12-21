package io.github.dsheirer.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jdesktop.swingx.mapviewer.GeoPosition;

@JsonSubTypes.Type(value = MapViewSetting.class, name = "mapViewSetting")
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
