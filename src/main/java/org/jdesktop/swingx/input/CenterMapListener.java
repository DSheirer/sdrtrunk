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

import org.jdesktop.swingx.JXMapViewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * Centers the map on the mouse cursor
 * if left is double-clicked or middle mouse
 * button is pressed.
 * @author Martin Steiger
 * @author joshy
 */
public class CenterMapListener extends MouseAdapter
{
	private JXMapViewer viewer;
	
	/**
	 * @param viewer the jxmapviewer
	 */
	public CenterMapListener(JXMapViewer viewer)
	{
		this.viewer = viewer;
	}

	@Override
	public void mousePressed(MouseEvent evt)
	{
		boolean left = SwingUtilities.isLeftMouseButton(evt);
		boolean middle = SwingUtilities.isMiddleMouseButton(evt);
		boolean doubleClick = (evt.getClickCount() == 2);

		if (middle || (left && doubleClick))
		{
			recenterMap(evt);
		}
	}
	
	private void recenterMap(MouseEvent evt)
	{
		Rectangle bounds = viewer.getViewportBounds();
		double x = bounds.getX() + evt.getX();
		double y = bounds.getY() + evt.getY();
		viewer.setCenter(new Point2D.Double(x, y));
                viewer.setZoom(viewer.getZoom() - 1);
		viewer.repaint();
	}
}


