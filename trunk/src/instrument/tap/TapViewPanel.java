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
package instrument.tap;

import instrument.gui.SampleModel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observer;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JPanel;

public abstract class TapViewPanel extends JPanel implements Observer
{
	protected SampleModel mModel;
	protected String mLabel;
	
	protected float mVerticalZoom = 1.0f;
	
	public TapViewPanel( SampleModel model, String label )
	{
		mModel = model;
		mModel.addObserver( this );
		
		mLabel = label;
		
		initGui();
	}
	
	private void initGui()
	{
		setBackground( Color.WHITE );
		setForeground( Color.BLUE );
		
		setSize( new Dimension( 400, 200 ) );
	}
	
	public void reset()
	{
		mModel.clearSamples();
	}
	
	public JMenu getContextMenu()
	{
		JMenu menu = new JMenu( mLabel );
		
		JMenu verticalMenu = new JMenu( "Vertical Zoom" );
		menu.add( verticalMenu );
		
		verticalMenu.add( new VerticalZoomItem( this, "8", 8.0f ) );
		verticalMenu.add( new VerticalZoomItem( this, "4", 4.0f ) );
		verticalMenu.add( new VerticalZoomItem( this, "2", 2.0f ) );
		verticalMenu.add( new VerticalZoomItem( this, "1", 1.0f ) );
		verticalMenu.add( new VerticalZoomItem( this, ".5", 0.5f ) );
		verticalMenu.add( new VerticalZoomItem( this, ".25", 0.25f ) );

		return menu;
	}

	public SampleModel getModel()
	{
		return mModel;
	}

	public int getSampleCount()
	{
		return getModel().getSampleCount();
	}
	
	public void setSampleCount( int sampleCount )
	{
		getModel().setSampleCount( sampleCount );
	}

	public float getVerticalZoom()
	{
		return mVerticalZoom;
	}
	
	public void setVerticalZoom( float zoom )
	{
		mVerticalZoom = zoom;
		repaint();
	}
	
	public class VerticalZoomItem extends JCheckBoxMenuItem
	{
		private TapViewPanel mPanel;
		private float mZoom;
		
		public VerticalZoomItem( TapViewPanel panel, String label, float zoom )
		{
			super( label );
			mPanel = panel;
			mZoom = zoom;
			
			if( mPanel.getVerticalZoom() == zoom )
			{
				setSelected( true );
			}
			
			addActionListener( new ActionListener() 
			{
				@Override
                public void actionPerformed( ActionEvent arg0 )
                {
					mPanel.setVerticalZoom( mZoom );
                }
			} );
		}
	}
}
