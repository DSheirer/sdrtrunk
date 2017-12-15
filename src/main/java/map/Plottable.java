/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package map;

import java.util.HashMap;

import org.jdesktop.swingx.mapviewer.GeoPosition;

import alias.Alias;

public class Plottable implements Comparable<Plottable>
{
	private long mTimestamp;
	private GeoPosition mGeoPosition;
	private String mID;
	private Alias mAlias;
	
	HashMap<String,String> mAttributes = new HashMap<String,String>();

	public Plottable()
	{
	}
	
	public Plottable( long timestamp,
					   GeoPosition position,
					   String id,
					   Alias alias,
					   HashMap<String,String> attributes )
	{
		this( timestamp, position, id, alias );
		mAttributes = attributes;
	}
	
	public Plottable( long timestamp,
					   GeoPosition position,
					   String id,
					   Alias alias )
	{
		mTimestamp = timestamp;
		mGeoPosition = position;
		mID = id;
		mAlias = alias;
	}
	
	public long getTimestamp()
	{
		return mTimestamp;
	}
	
	public void setTimestamp( long timestamp )
	{
		mTimestamp = timestamp;
	}

	public GeoPosition getGeoPosition()
	{
		return mGeoPosition;
	}
	
	public void setGeoPosition( GeoPosition position )
	{
		mGeoPosition = position;
	}
	
	public String getID()
	{
		return mID;
	}
	
	public void setID( String id )
	{
		mID = id;
	}
	
	public Alias getAlias()
	{
		return mAlias;
	}
	
	public void setAlias( Alias alias )
	{
		mAlias = alias;
	}
	
	public HashMap<String,String> getAttributes()
	{
		return mAttributes;
	}
	
	public void addAttribute( String key, String value )
	{
		mAttributes.put( key, value );
	}
	
	public void removeAttribute( String key )
	{
		mAttributes.remove( key );
	}
	
	public void clearAttributes()
	{
		mAttributes.clear();
	}

	/**
	 * Comparisons for sorting of plottables is based on timestamps
	 */
	@Override
    public int compareTo( Plottable o )
    {
	    if( mTimestamp == o.mTimestamp )
	    {
	    	return 0;
	    }
	    else if( mTimestamp < o.mTimestamp )
	    {
	    	return -1;
	    }
	    else
	    {
	    	return 1;
	    }
    }
}
