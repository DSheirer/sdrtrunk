/*
 * WaypointRenderer.java
 *
 * Created on March 30, 2006, 5:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.jdesktop.swingx.mapviewer;

import org.jdesktop.swingx.JXMapViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * This is a standard waypoint renderer.
 * @author joshy
 */
public class DefaultWaypointRenderer implements WaypointRenderer<Waypoint>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( DefaultWaypointRenderer.class );

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
			mLog.error("couldn't read standard_waypoint.png", ex );
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
