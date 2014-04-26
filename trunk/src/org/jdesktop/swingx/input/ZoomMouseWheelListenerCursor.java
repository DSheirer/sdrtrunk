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
package org.jdesktop.swingx.input;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

import org.jdesktop.swingx.JXMapViewer;

/**
 * zooms to the current mouse cursor 
 * using the mouse wheel
 * @author Martin Steiger
 */
public class ZoomMouseWheelListenerCursor implements MouseWheelListener
{
	private JXMapViewer viewer;
	
	/**
	 * @param viewer the jxmapviewer
	 */
	public ZoomMouseWheelListenerCursor(JXMapViewer viewer)
	{
		this.viewer = viewer;
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent evt)
	{
		Point current = evt.getPoint();
		Rectangle bound = viewer.getViewportBounds();
		
		double dx = current.x - bound.width / 2;
		double dy = current.y - bound.height / 2;
		
		Dimension oldMapSize = viewer.getTileFactory().getMapSize(viewer.getZoom());

		viewer.setZoom(viewer.getZoom() + evt.getWheelRotation());
		
		Dimension mapSize = viewer.getTileFactory().getMapSize(viewer.getZoom());

		Point2D center = viewer.getCenter();

		double dzw = (mapSize.getWidth() / oldMapSize.getWidth());
		double dzh = (mapSize.getHeight() / oldMapSize.getHeight());

		double x = center.getX() + dx * (dzw - 1);
		double y = center.getY() + dy * (dzh - 1);

		viewer.setCenter(new Point2D.Double(x, y));
	}
}
