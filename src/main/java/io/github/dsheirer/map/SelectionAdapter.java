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
package io.github.dsheirer.map;

import org.apache.commons.math3.util.FastMath;
import org.jdesktop.swingx.JXMapViewer;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

/**
 * Creates a selection rectangle based on mouse input
 * Also triggers repaint events in the viewer
 * @author Martin Steiger
 */
public class SelectionAdapter extends MouseAdapter 
{
	private boolean dragging;
	private JXMapViewer viewer;

	private Point2D startPos = new Point2D.Double();
	private Point2D endPos = new Point2D.Double();

	/**
	 * @param viewer the jxmapviewer
	 */
	public SelectionAdapter(JXMapViewer viewer)
	{
		this.viewer = viewer;
	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (e.getButton() != MouseEvent.BUTTON3)
			return;
		
		startPos.setLocation(e.getX(), e.getY());
		endPos.setLocation(e.getX(), e.getY());
		
		dragging = true;
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		if (!dragging)
			return;
		
		endPos.setLocation(e.getX(), e.getY());
		
		viewer.repaint();
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		if (!dragging)
			return;
		
		if (e.getButton() != MouseEvent.BUTTON3)
			return;
		
		viewer.repaint();
		
		dragging = false;
	}

	/**
	 * @return the selection rectangle
	 */
	public Rectangle getRectangle()
	{
		if (dragging)
		{
			int x1 = (int) FastMath.min(startPos.getX(), endPos.getX());
			int y1 = (int) FastMath.min(startPos.getY(), endPos.getY());
			int x2 = (int) FastMath.max(startPos.getX(), endPos.getX());
			int y2 = (int) FastMath.max(startPos.getY(), endPos.getY());
			
			return new Rectangle(x1, y1, x2-x1, y2-y1);
		}
		
		return null;
	}

}
