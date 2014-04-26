/*
 * Waypoint.java
 *
 * Created on March 30, 2006, 5:22 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;


/**
 * A Waypoint is a GeoPosition that can be 
 * drawn on a may using a WaypointPainter.
 * @author joshy
 */
public interface Waypoint
{
	/**
	 * Get the current GeoPosition of this Waypoint
	 * @return the current position
	 */
	public GeoPosition getPosition();
}
