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

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;

/**
 * used to pan using the arrow keys
 * @author joshy
 */
public class PanKeyListener extends KeyAdapter
{
	private static final int OFFSET = 10;

	private JXMapViewer viewer;
	
	/**
	 * @param viewer the jxmapviewer
	 */
	public PanKeyListener(JXMapViewer viewer)
	{
		this.viewer = viewer;
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		int delta_x = 0;
		int delta_y = 0;
		int requestedZoom = 0;

		switch ( e.getKeyCode() )
		{
			
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_NUMPAD4:
				delta_x = -OFFSET;
				break;
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_NUMPAD6:
				delta_x = OFFSET;
				break;
			case KeyEvent.VK_UP:
			case KeyEvent.VK_NUMPAD8:
				delta_y = -OFFSET;
				break;
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_NUMPAD2:
				delta_y = OFFSET;
				break;
			case KeyEvent.VK_MINUS:
			case KeyEvent.VK_SUBTRACT:
				requestedZoom = 1;
				break;
			case KeyEvent.VK_ADD:
			case KeyEvent.VK_EQUALS:
				requestedZoom = -1;
				break;
		}

		if (delta_x != 0 || delta_y != 0)
		{
			Rectangle bounds = viewer.getViewportBounds();
			double x = bounds.getCenterX() + delta_x;
			double y = bounds.getCenterY() + delta_y;
			viewer.setCenter(new Point2D.Double(x, y));
			viewer.repaint();
		}
		
		if( requestedZoom != 0 )
		{
			int zoomLevel = viewer.getZoom() + requestedZoom;
			
			viewer.setZoom( zoomLevel );
		}
	}
}
