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
 * WaypointMapOverlay.java
 *
 * Created on April 1, 2006, 4:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.painter.AbstractPainter;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Paints waypoints on the JXMapViewer. This is an 
 * instance of Painter that only can draw on to JXMapViewers.
 * @param <W> the waypoint type
 * @author rbair
 */
public class WaypointPainter<W extends Waypoint> extends AbstractPainter<JXMapViewer>
{
	private WaypointRenderer<? super W> renderer = new DefaultWaypointRenderer();
	private Set<W> waypoints = new HashSet<W>();

	/**
	 * Creates a new instance of WaypointPainter
	 */
	public WaypointPainter()
	{
		setAntialiasing(true);
		setCacheable(false);
	}

	/**
	 * Sets the waypoint renderer to use when painting waypoints
	 * @param r the new WaypointRenderer to use
	 */
	public void setRenderer(WaypointRenderer<W> r)
	{
		this.renderer = r;
	}

	/**
	 * Gets the current set of waypoints to paint
	 * @return a typed Set of Waypoints
	 */
	public Set<W> getWaypoints()
	{
		return Collections.unmodifiableSet(waypoints);
	}

	/**
	 * Sets the current set of waypoints to paint
	 * @param waypoints the new Set of Waypoints to use
	 */
	public void setWaypoints(Set<? extends W> waypoints)
	{
		this.waypoints.clear();
		this.waypoints.addAll(waypoints);
	}

	@Override
	protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height)
	{
		if (renderer == null)
		{
			return;
		}

		Rectangle viewportBounds = map.getViewportBounds();

		g.translate(-viewportBounds.getX(), -viewportBounds.getY());

		for (W w : getWaypoints())
		{
			renderer.paintWaypoint(g, map, w);
		}

		g.translate(viewportBounds.getX(), viewportBounds.getY());

	}

}
