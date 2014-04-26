/*
 * WaypointRenderer.java
 *
 * Created on March 30, 2006, 5:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import log.Log;

import org.jdesktop.swingx.JXMapViewer;

/**
 * This is a standard waypoint renderer.
 * @author joshy
 */
public class DefaultWaypointRenderer implements WaypointRenderer<Waypoint>
{
	private BufferedImage img = null;

	/**
	 * Uses a default waypoint image
	 */
	public DefaultWaypointRenderer()
	{
		try
		{
			img = ImageIO.read(getClass().getResource("resources/standard_waypoint.png"));
		}
		catch (Exception ex)
		{
			Log.warning("couldn't read standard_waypoint.png - " + ex.getLocalizedMessage() );
		}
	}

	@Override
	public void paintWaypoint(Graphics2D g, JXMapViewer map, Waypoint w)
	{
		if (img == null)
			return;

		Point2D point = map.getTileFactory().geoToPixel(w.getPosition(), map.getZoom());
		
		int x = (int)point.getX() -img.getWidth() / 2;
		int y = (int)point.getY() -img.getHeight();
		
		g.drawImage(img, x, y, null);
	}
}
