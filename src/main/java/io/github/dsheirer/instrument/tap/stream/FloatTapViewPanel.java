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
package io.github.dsheirer.instrument.tap.stream;

import io.github.dsheirer.gui.SDRTrunk;
import io.github.dsheirer.instrument.gui.SampleModel;
import io.github.dsheirer.instrument.tap.Tap;
import io.github.dsheirer.instrument.tap.TapViewPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Observable;

public class FloatTapViewPanel extends TapViewPanel
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( SDRTrunk.class );

	private Tap mTap;
	private List<Float> mSamples;
	private int mSampleCount;

	public FloatTapViewPanel( FloatTap tap )
	{
		super( new SampleModel<Float>(), tap.getName() );
		
		mTap = tap;
		mTap.addListener( getModel() );

		mSampleCount = (int)( 2000 * tap.getSampleRateRatio() );

		getModel().setSampleCount( mSampleCount );
		getModel().setDelay( tap.getDelay() );
		
		mLog.info( "Float Tap Panel [" + tap.getName() + "] count: " + 
			getModel().getSampleCount() + " delay:" + getModel().getDelay() );
	}
	
	@Override
    public void update( Observable arg0, Object arg1 )
    {
		mSamples = (List<Float>)getModel().getSamples();
		repaint();
    }
	
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
		
		Graphics2D g2 = (Graphics2D)g;

		int height = getHeight();
		int width = getWidth();
		
		g2.setColor( Color.DARK_GRAY );
		
		float middle = (float)height / 2.0f;

		g2.drawLine( 0, (int)middle, width, (int)middle );

		if( mSamples != null && 
			mSamples.size() > 0  )
		{
			g2.setColor( getForeground() );
			
			g2.drawString( mLabel, 5 , 15 );

			Path2D.Float polyline = 
		        new Path2D.Float( Path2D.Float.WIND_EVEN_ODD, mSamples.size() );

			/* Move to the first point */
			Point2D.Float first = resolve( 0, mSamples.get( 0 ) );
			polyline.moveTo( first.x, first.y );
			
			for( int x = 1; x < mSamples.size(); x++ )
			{
				Point2D.Float point = resolve( x, mSamples.get( x ) );
				
				polyline.lineTo( point.x, point.y );
			}

			g2.draw(polyline);
		}
		
		g2.dispose();
	}
	
	private Point2D.Float resolve( int index, float value )
	{
		int middle = (int)( getHeight() / 2 );
		
		float y = middle - ( middle * value * mVerticalZoom );

		/* Clip y to zero as necessary */
		y = ( y < 0 ) ? 0 : y;

		/* Clip y to max height as necessary */
		y = ( y > getHeight() ) ? getHeight() : y;
		
		float indexWidth = (float)getWidth() / (float)mSampleCount;
		
		float x = (float)index * indexWidth;

		Point2D.Float point = new Point2D.Float( x, y );
		
		return new Point2D.Float( x , y );
	}
}
