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

import org.jdesktop.swingx.painter.Painter;

import java.awt.*;

/**
 * Paints a selection rectangle
 * @author Martin Steiger
 */
public class SelectionPainter implements Painter<Object>
{
	private Color fillColor = new Color(128, 192, 255, 128);
	private Color frameColor = new Color(0, 0, 255, 128);

	private SelectionAdapter adapter;
	
	/**
	 * @param adapter the selection adapter
	 */
	public SelectionPainter(SelectionAdapter adapter)
	{
		this.adapter = adapter;
	}

	@Override
	public void paint(Graphics2D g, Object t, int width, int height)
	{
		Rectangle rc = adapter.getRectangle();
		
		if (rc != null)
		{
			g.setColor(frameColor);
			g.draw(rc);
			g.setColor(fillColor);
			g.fill(rc);
		}
	}
}
