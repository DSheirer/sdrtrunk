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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;

public class MapMouseListener extends MouseInputAdapter implements MouseWheelListener
{
	private JXMapViewer mJXMapViewer;
	private Consumer<GeoPosition> mMyLocationListener;
	private Function<Point,List<JMenuItem>> mMenuItemProvider;
	private Point mPreviousPoint;
	private Point mCurrentPoint;

	/**
	 * Constructs an instance
	 * @param viewer map
	 * @param myLocationListener receives the location chosen with the 'Set My Location Here' menu item
	 * @param menuItemProvider of additional context menu items for the point that was right-clicked, listed first
	 */
	public MapMouseListener( JXMapViewer viewer, Consumer<GeoPosition> myLocationListener,
							 Function<Point,List<JMenuItem>> menuItemProvider )
	{
		mJXMapViewer = viewer;
		mMyLocationListener = myLocationListener;
		mMenuItemProvider = menuItemProvider;
	}
	
	@Override
	public void mouseDragged( MouseEvent event )
	{
		if ( !SwingUtilities.isLeftMouseButton( event ) )
		{
			return;
		}

		Point current = event.getPoint();
		
		double x = mJXMapViewer.getCenter().getX() - 
				   ( current.getX() - mPreviousPoint.getX() );

		double y = mJXMapViewer.getCenter().getY() - 
				   (current.getY() - mPreviousPoint.getY() );

		if ( !mJXMapViewer.isNegativeYAllowed() )
		{
			if ( y < 0 )
			{
				y = 0;
			}
		}

		int maxHeight = (int)( mJXMapViewer.getTileFactory()
				.getMapSize( mJXMapViewer.getZoom() ).getHeight() * 
				mJXMapViewer.getTileFactory()
				.getTileSize( mJXMapViewer.getZoom() ) );
		
		if (y > maxHeight)
		{
			y = maxHeight;
		}

		mPreviousPoint = current;
		
		mJXMapViewer.setCenter( new Point2D.Double( x, y ) );
		
		mJXMapViewer.repaint();

		/* Set cursor to dragging */
		mJXMapViewer.setCursor( Cursor.getPredefinedCursor( Cursor.MOVE_CURSOR ) );
	}

	@Override
	public void mouseReleased( MouseEvent event )
	{
		if ( !SwingUtilities.isLeftMouseButton( event ) )
		{
			return;
		}

		mPreviousPoint = null;
		
		/* Reset the curson */
		mJXMapViewer.setCursor( 
				Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
	}

	@Override
	public void mouseEntered( MouseEvent event )
	{
		SwingUtilities.invokeLater( new Runnable()
		{
			@Override
			public void run()
			{
				mJXMapViewer.requestFocusInWindow();
			}
		});
	}

	@Override
	public void mousePressed( MouseEvent event )
	{
		mCurrentPoint = event.getPoint();
		mPreviousPoint = event.getPoint();

		boolean left = SwingUtilities.isLeftMouseButton( event );
		
		boolean middle = SwingUtilities.isMiddleMouseButton( event );
		
		boolean right = SwingUtilities.isRightMouseButton( event );

		boolean doubleClick = ( event.getClickCount() == 2 );

		if (middle || ( left && doubleClick ) )
		{
			recenterMap( event );
		}
		else if( right )
		{
			JPopupMenu popup = new JPopupMenu();

			if( mMenuItemProvider != null )
			{
				List<JMenuItem> items = mMenuItemProvider.apply( event.getPoint() );

				if( items != null && !items.isEmpty() )
				{
					items.forEach( popup::add );
					popup.addSeparator();
				}
			}
			
			JMenuItem mapViewItem = new JMenuItem( "Set My Location Here" );
			mapViewItem.setToolTipText( "Sets your location, which is also used by the playlist editor's " +
				"Radio Reference tab, to this point" );
			mapViewItem.addActionListener( new ActionListener() 
			{
				@Override
                public void actionPerformed( ActionEvent arg0 )
                {
					mMyLocationListener.accept( mJXMapViewer.convertPointToGeoPosition( mCurrentPoint ) );
                }
			} );
			popup.add( mapViewItem );
			
			popup.show( mJXMapViewer, event.getX(), event.getY() );
		}
	}
	
	private void recenterMap( MouseEvent event )
	{
		Rectangle bounds = mJXMapViewer.getViewportBounds();
		
		double x = bounds.getX() + event.getX();
		
		double y = bounds.getY() + event.getY();
		
		mJXMapViewer.setCenter( new Point2D.Double( x, y ) );

		mJXMapViewer.setZoom( mJXMapViewer.getZoom() - 1 );

		mJXMapViewer.repaint();
	}
}
