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
package instrument.tap.stream;

import gui.SDRTrunk;
import instrument.gui.SampleModel;
import instrument.tap.Tap;
import instrument.tap.TapViewPanel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.complex.ComplexSample;

public class ComplexTapViewPanel extends TapViewPanel
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( SDRTrunk.class );

	private Tap mTap;
	private List<ComplexSample> mSamples;
	private int mSampleCount;

	public ComplexTapViewPanel( ComplexTap tap )
	{
		super( new SampleModel<ComplexTap>(), tap.getName() );
		
		setSize( new Dimension( 400, 400 ) );
		
		mTap = tap;
		mTap.addListener( getModel() );

		mSampleCount = (int)( 2000 * tap.getSampleRateRatio() );

		getModel().setSampleCount( mSampleCount );
		getModel().setDelay( tap.getDelay() );
		
		mLog.info( "Complex Tap Panel [" + tap.getName() + "] count: " + 
			getModel().getSampleCount() + " delay:" + getModel().getDelay() );
	}
	
	@Override
    public void update( Observable arg0, Object arg1 )
    {
		mSamples = (List<ComplexSample>)getModel().getSamples();
		repaint();
    }
	
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
		
		Graphics2D g2 = (Graphics2D)g;

		int height = getHeight();
		int width = getWidth();
		
		g2.setColor( Color.DARK_GRAY );
		
		float middle = (float)height / 4.0f;

		g2.drawLine( 0, (int)middle, width, (int)middle );
		g2.drawLine( 0, (int)( 3 * middle ), width, (int)( 3 * middle ) );

		if( mSamples != null && 
			mSamples.size() > 0  )
		{
			g2.setColor( getForeground() );
			
			g2.drawString( mLabel, 5 , 15 );

			Path2D.Float polylineReal = 
		        new Path2D.Float( Path2D.Float.WIND_EVEN_ODD, mSamples.size() );
			Path2D.Float polylineImag = 
			        new Path2D.Float( Path2D.Float.WIND_EVEN_ODD, mSamples.size() );

			/* Move to the first point */
			Point2D.Float firstReal = resolve( 0, mSamples.get( 0 ), Type.REAL );
			polylineReal.moveTo( firstReal.x, firstReal.y );

			Point2D.Float firstImag = resolve( 0, mSamples.get( 0 ), Type.IMAGINERY );
			polylineImag.moveTo( firstImag.x, firstImag.y );
			
			for( int x = 1; x < mSamples.size(); x++ )
			{
				Point2D.Float pointReal = resolve( x, mSamples.get( x ), Type.REAL );
				polylineReal.lineTo( pointReal.x, pointReal.y );

				Point2D.Float pointImag = resolve( x, mSamples.get( x ), Type.IMAGINERY );
				polylineImag.lineTo( pointImag.x, pointImag.y );
				
//				mLog.debug( "Imag y:" + pointImag.getY() + " height:" + getHeight() );
			}

			g2.draw( polylineReal );
			g2.draw( polylineImag );
		}
		
		g2.dispose();
	}
	
	
	private Point2D.Float resolve( int index, ComplexSample sample, Type type )
	{
		int middle = (int)( getHeight() / 4 );

		float y = middle - ( middle * mVerticalZoom * ( type == Type.REAL ? 
				sample.real() : sample.imaginary()) );

		if( type == Type.IMAGINERY )
		{
			y += ( middle * 2 );
		}
		
		/* Clip y to zero as necessary */
		y = ( y < 0 ) ? 0 : y;

		/* Clip y to max height as necessary */
		y = ( y > getHeight() ) ? getHeight() : y;
		
		float indexWidth = (float)getWidth() / (float)mSampleCount;
		
		float x = (float)index * indexWidth;

		Point2D.Float point = new Point2D.Float( x, y );
		
		return new Point2D.Float( x , y );
	}
	
	public enum Type{ REAL,IMAGINERY };
}
