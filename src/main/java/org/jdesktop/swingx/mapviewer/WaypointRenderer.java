/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

/*
 * WaypointRenderer.java
 *
 * Created on March 30, 2006, 5:56 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

import org.jdesktop.swingx.JXMapViewer;

import java.awt.Graphics2D;

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
