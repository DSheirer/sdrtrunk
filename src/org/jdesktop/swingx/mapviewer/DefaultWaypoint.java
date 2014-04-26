/*
 * Waypoint.java
 *
 * Created on March 30, 2006, 5:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

import org.jdesktop.beans.AbstractBean;

/**
 * @author joshy
 */
public class DefaultWaypoint extends AbstractBean implements Waypoint 
{
	private GeoPosition position;

	/** 
	 * Creates a new instance of Waypoint 
	 */
	public DefaultWaypoint()
	{
		this(new GeoPosition(0, 0));
	}

	/**
	 * @param latitude the latitude
	 * @param longitude the longitude
	 */
	public DefaultWaypoint(double latitude, double longitude)
	{
		this(new GeoPosition(latitude, longitude));
	}

	/**
	 * @param coord the geo coordinate
	 */
	public DefaultWaypoint(GeoPosition coord)
	{
		this.position = coord;
	}

	@Override
	public GeoPosition getPosition()
	{
		return position;
	}

	/**
	 * Set a new GeoPosition for this Waypoint
	 * @param coordinate a new position
	 */
	public void setPosition(GeoPosition coordinate)
	{
		GeoPosition old = getPosition();
		this.position = coordinate;
		firePropertyChange("position", old, getPosition());
	}

}
