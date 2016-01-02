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

import java.awt.EventQueue;

import javax.swing.JPanel;

import message.Message;
import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.OSMTileFactoryInfo;
import org.jdesktop.swingx.input.PanKeyListener;
import org.jdesktop.swingx.input.ZoomMouseWheelListenerCursor;
import org.jdesktop.swingx.mapviewer.DefaultTileFactory;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.jdesktop.swingx.mapviewer.TileFactoryInfo;

import sample.Listener;
import settings.MapViewSetting;
import settings.SettingsManager;
import controller.channel.ChannelProcessingManager;

public class MapPanel extends JPanel implements PlottableUpdateListener
{
    private static final long serialVersionUID = 1L;

    private SettingsManager mSettingsManager;
    private MapService mMapService;
    private JXMapViewer mMapViewer = new JXMapViewer();
    private PlottableEntityPainter mMapPainter;
    
    public MapPanel( MapService mapService, 
    				 SettingsManager settingsManager,
    				 ChannelProcessingManager channelProcessingManager )
    {
    	mSettingsManager = settingsManager;
    	mMapService = mapService;
    	mMapPainter = new PlottableEntityPainter( mSettingsManager );    	
    	
    	/* Add Map Service as message listener to receive all messages */
    	channelProcessingManager.addListener( (Listener<Message>)mMapService );
    	
    	init();
    }
    
    private void init()
    {
		setLayout( new MigLayout( "insets 0 0 0 0", 
				"[grow,fill]", "[grow,fill]") );

		/**
		 * Set the entity painter as the overlay painter and register this panel
		 * to receive new messages (plots)
		 */
		mMapViewer.setOverlayPainter( mMapPainter );
		mMapService.addListener( this );
		
    	/**
    	 * Map image source
    	 */
    	TileFactoryInfo info = new OSMTileFactoryInfo();
		DefaultTileFactory tileFactory = new DefaultTileFactory( info );
		mMapViewer.setTileFactory( tileFactory );

		/**
		 * Defines how many threads will be used to fetch the background map
		 * tiles (graphics)
		 */
		tileFactory.setThreadPoolSize( 8 );

		/**
		 * Set initial location and zoom for the map upon display
		 */
		GeoPosition syracuse = new GeoPosition( 43.048, -76.147 );
		int zoom = 7;
		
		MapViewSetting view = mSettingsManager
				.getMapViewSetting( "Default", syracuse, zoom );
		
		mMapViewer.setAddressLocation( view.getGeoPosition() );
		mMapViewer.setZoom( view.getZoom() );

		/**
		 * Add a mouse adapter for panning and scrolling
		 */
		MapMouseListener listener = 
				new MapMouseListener( mMapViewer, mSettingsManager );
		mMapViewer.addMouseListener( listener );
		mMapViewer.addMouseMotionListener( listener );
		
		/* Map zoom listener */
		mMapViewer.addMouseWheelListener(
				new ZoomMouseWheelListenerCursor( mMapViewer ) );

		/* Keyboard panning listener */
		mMapViewer.addKeyListener( new PanKeyListener( mMapViewer ) );

		/**
		 * Add a selection listener
		 */
		SelectionAdapter sa = new SelectionAdapter( mMapViewer ); 
		mMapViewer.addMouseListener( sa ); 
		mMapViewer.addMouseMotionListener( sa ); 

		/**
    	 * Map component
    	 */
    	add(  mMapViewer, "span" );
    }

	@Override
    public void entitiesUpdated()
    {
		EventQueue.invokeLater( new Runnable() 
		{
			@Override
            public void run()
            {
				mMapViewer.repaint();
            }
		} );
    }

	@Override
    public void addPlottableEntity( PlottableEntity entity )
    {
		mMapPainter.addEntity( entity );
		
		entitiesUpdated();
    }

	@Override
    public void removePlottableEntity( PlottableEntity entity )
    {
		mMapPainter.removeEntity( entity );
		
		entitiesUpdated();
    }
}
