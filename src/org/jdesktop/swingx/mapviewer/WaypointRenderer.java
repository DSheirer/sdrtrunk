/*
 * WaypointRenderer.java
 *
 * Created on March 30, 2006, 5:56 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

import java.awt.Graphics2D;

import org.jdesktop.swingx.JXMapViewer;

/**
 * A interface that draws waypoints. Implementations of WaypointRenderer can
 * be set on a WayPointPainter to draw waypoints on a JXMapViewer
 * @param <W> the waypoint type
 * @author joshua.marinacci@sun.com
 */
public interface WaypointRenderer<W>
{
    /**
     * paint the specified waypoint on the specified map and graphics context
     * @param g the graphics2D object 
     * @param map the map
     * @param waypoint the waypoint
     */
    public void paintWaypoint(Graphics2D g, JXMapViewer map, W waypoint);
    
}
