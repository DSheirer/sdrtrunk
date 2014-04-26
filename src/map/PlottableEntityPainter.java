/*
 * WaypointMapOverlay.java
 *
 * Created on April 1, 2006, 4:59 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package map;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.painter.AbstractPainter;

import settings.SettingsManager;

public class PlottableEntityPainter extends AbstractPainter<JXMapViewer>
{
	private PlottableEntityRenderer mRenderer;
	private Set<PlottableEntity> mEntities = new HashSet<PlottableEntity>();

	public PlottableEntityPainter( SettingsManager settingsManager )
	{
		mRenderer = new PlottableEntityRenderer( settingsManager );		
		setAntialiasing( true );
		setCacheable( false );
	}

	public void addEntity( PlottableEntity entity )
	{
		mEntities.add( entity );
	}
	
	public void removeEntity( PlottableEntity entity )
	{
		mEntities.remove( entity );
	}
	
	public void clearEntities()
	{
		mEntities.clear();
	}
	
	private Set<PlottableEntity> getEntities()
	{
		return Collections.unmodifiableSet( mEntities );
	}
	
	@Override
	protected void doPaint(Graphics2D g, JXMapViewer map, int width, int height)
	{
		Rectangle viewportBounds = map.getViewportBounds();

		g.translate( -viewportBounds.getX(), -viewportBounds.getY() );

		Set<PlottableEntity> entities = getEntities();
		
		for (PlottableEntity entity: entities )
		{
			mRenderer.paintPlottableEntity( g, map, entity, true );
		}

		g.translate( viewportBounds.getX(), viewportBounds.getY() );
	}
}
