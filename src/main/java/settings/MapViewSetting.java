package settings;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import org.jdesktop.swingx.mapviewer.GeoPosition;

public class MapViewSetting extends Setting
{
	private double mLatitude;
	private double mLongitude;
	private int mZoom;
	
	public MapViewSetting()
	{
		/* Empty constructor for JAXB */
	}
	
	public MapViewSetting( String name, double latitude, double longitude, int zoom )
	{
		super( name );
		mLatitude = latitude;
		mLongitude = longitude;
		mZoom = zoom;
	}
	
	public MapViewSetting( String name, GeoPosition position, int zoom )
	{
		super( name );
		mLatitude = position.getLatitude();
		mLongitude = position.getLongitude();
		mZoom = zoom;
	}
	
	@XmlAttribute
	public double getLatitude()
	{
		return mLatitude;
	}
	
	public void setLatitude( double latitude )
	{
		mLatitude = latitude;
	}

	@XmlAttribute
	public double getLongitude()
	{
		return mLongitude;
	}
	
	public void setLongitude( double longitude )
	{
		mLongitude = longitude;
	}

	@XmlAttribute
	public int getZoom()
	{
		return mZoom;
	}
	
	public void setZoom( int zoom )
	{
		mZoom = zoom;
	}

	@XmlTransient
	public GeoPosition getGeoPosition()
	{
		return new GeoPosition( mLatitude, mLongitude );
	}
	
	public void setGeoPosition( GeoPosition position )
	{
		mLatitude = position.getLatitude();
		mLongitude = position.getLongitude();
	}
}
