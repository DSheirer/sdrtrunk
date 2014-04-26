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
package map;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.Set;

import javax.swing.ImageIcon;

import org.jdesktop.swingx.JXMapViewer;

import settings.SettingsManager;

public class PlottableEntityRenderer
{
	private SettingsManager mSettingsManager;

	public PlottableEntityRenderer( SettingsManager settingsManager )
	{
		mSettingsManager = settingsManager;
	}

	public void paintPlottableEntity( Graphics2D g, 
									  JXMapViewer viewer, 
									  PlottableEntity entity,
									  boolean antiAliasing )
	{
		Graphics2D graphics = (Graphics2D)g.create();

		if( antiAliasing )
		{
			graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, 
					RenderingHints.VALUE_ANTIALIAS_ON );
		}

		/**
		 * Use the entity's preferred color for lines and labels
		 */
		graphics.setColor( entity.getColor() );

		/**
		 * Convert the lat/long geoposition to an x/y point on the viewer
		 */
		Point2D point = viewer.getTileFactory().geoToPixel( 
				entity.getCurrentGeoPosition(), viewer.getZoom() );

		/**
		 * Paint the route first, so the icon and label overlay it
		 */
		paintRoute( graphics, viewer, entity );

		/**
		 * Paint the icon at the current location
		 */
		ImageIcon icon = getIcon( entity ).getImageIcon();
		
		paintIcon( graphics, point, icon );

		/**
		 * Paint the label offset to the right of the icon
		 */
		paintLabel( graphics, point, entity, (int)( icon.getIconWidth() / 2 ), 0 );

		/**
		 * Cleanup
		 */
		graphics.dispose();
	}
	
	private MapIcon getIcon( PlottableEntity entity )
	{
		return mSettingsManager.getMapIcon( entity.getMapIconName() );
	}
	
	private void paintIcon( Graphics2D graphics, 
							Point2D point, 
							ImageIcon icon )
	{
		graphics.drawImage( icon.getImage(), 
							(int)point.getX() - ( icon.getIconWidth() / 2 ), 
							(int)point.getY() - ( icon.getIconHeight() / 2 ), 
							null );
		
	}
	
	private void paintLabel( Graphics2D graphics, 
							 Point2D point, 
							 PlottableEntity entity,
							 int xOffset,
							 int yOffset )
	{
		graphics.drawString( entity.getLabel(), 
							 (int)point.getX() + xOffset,
							 (int)point.getY() + yOffset );
	}
	
	/**
	 * Paints a two-tone route from the entity's list of plottables (locations).
	 * using black as a wider background route, and the entity's preferred color
	 * as a narrower foreground route.
	 */
	private void paintRoute( Graphics2D graphics, 
							 JXMapViewer viewer, 
							 PlottableEntity entity )
	{
		Set<Plottable> plottables = entity.getPlottables();
		
		if( plottables.size() > 1 )
		{
			// Draw the route with a black background line
			graphics.setColor( Color.BLACK );
			graphics.setStroke( new BasicStroke( 3 ) );

			drawRoute( plottables, graphics, viewer );

			// Draw the route again, in the entity's preferred color
			graphics.setColor( entity.getColor() );
			graphics.setStroke( new BasicStroke( 1 ) );

			drawRoute( plottables, graphics, viewer );
		}
	}

	/**
	 * Draws a route from a list of plottables
	 */
	private void drawRoute( Set<Plottable> plottables, 
								  Graphics2D g, 
								  JXMapViewer viewer )
	{
		Point2D lastPoint = null;

		for (Plottable plottable: plottables )
		{
			// convert geo-coordinate to world bitmap pixel
			Point2D currentPoint = viewer.getTileFactory()
					.geoToPixel( plottable.getGeoPosition(), viewer.getZoom() );

			if( lastPoint != null )
			{
				g.drawLine( (int)lastPoint.getX(), (int)lastPoint.getY(), 
							(int)currentPoint.getX(), (int)currentPoint.getY() );
			}
			
			lastPoint = currentPoint;
		}
	}
}


