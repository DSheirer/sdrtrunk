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
import io.github.dsheirer.sample.complex.Complex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;
import java.util.Observable;

public class QPSKTapPanel extends TapViewPanel
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = LoggerFactory.getLogger( SDRTrunk.class );

	private Tap mTap;
	private List<Complex> mSamples;
	private int mSampleCount;

	public QPSKTapPanel( ComplexTap tap )
	{
		super( new SampleModel<ComplexTap>(), tap.getName() );
		
		mTap = tap;
		mTap.addListener( getModel() );

		mSampleCount = (int)( 60 * tap.getSampleRateRatio() );

		getModel().setSampleCount( mSampleCount );
		getModel().setDelay( tap.getDelay() );
		
		mLog.info( "Complex Tap Panel [" + tap.getName() + "] count: " + 
			getModel().getSampleCount() + " delay:" + getModel().getDelay() );
	}
	
	@Override
    public void update( Observable arg0, Object arg1 )
    {
		mSamples = (List<Complex>)getModel().getSamples();
		repaint();
    }
	
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
		
		Graphics2D g2 = (Graphics2D)g;

		int min = ( getHeight() < getWidth() ? getHeight() : getWidth() );
		
		float middle = (float)min / 2.0f;

		g2.setColor( Color.DARK_GRAY );
		
		/* Vertical Line */
		g2.drawLine( getWidth() / 2, 5, getWidth() / 2, getHeight() - 5 );
		
		/* Horizontal Line */
		g2.drawLine( getWidth() / 2 - (int)( middle ), getHeight() / 2, 
					 getWidth() / 2 + (int)( middle ), getHeight() / 2 );
		
		g2.setColor( Color.CYAN );
		
		/* Crossed Lines */
		g2.drawLine( getWidth() / 2 - (int)( middle ), 5, 
				     getWidth() / 2 + (int)( middle ), getHeight() - 5 );
		g2.drawLine( getWidth() / 2 + (int)( middle ), 5, 
				     getWidth() / 2 - (int)( middle ), getHeight() - 5 );
		


		if( mSamples != null && 
			mSamples.size() > 0  )
		{
			g2.drawString( mLabel, 5 , 15 );

			/* Set color to light gray for oldest 90% of constellations */
			g2.setColor( Color.LIGHT_GRAY );

			int counter = 0;

			int threshold = (int)( mSamples.size() * 0.9f );

			for( Complex sample: mSamples )
			{
				/* Set color to foreground for newest 10% of constellations */
				if( counter == threshold )
				{
					g2.setColor( getForeground() );
				}
				
//				float scale;
//				
//				if( sample.real() != 0 && sample.imaginary() != 0 )
//				{
//					scale = 1.0f / sample.magnitude();
//				}
//				else if( sample.real() == 0 )
//				{
//					scale = 1.0f / sample.imaginary();
//				}
//				else
//				{
//					scale = 1.0f / sample.real();
//				}
				
//				float x = ( getWidth() / 2 ) + 
//						  ( scale *  sample.real() * middle * 0.75f * mVerticalZoom );
				float x = ( getWidth() / 2 ) + 
						  ( sample.real() * middle * 0.75f * mVerticalZoom );
				
				if( Float.isNaN( x ) )
				{
					x = getWidth() / 2;
				}
				
//				float y = ( getHeight() / 2 ) + 
//						  ( scale * sample.imaginary() * middle * 0.75f * mVerticalZoom );
				float y = ( getHeight() / 2 ) + 
						  ( sample.imaginary() * middle * 0.75f * mVerticalZoom );
				
				if( Float.isNaN( y ) )
				{
					y = getHeight() / 2;
				}
				
				g2.draw( new Ellipse2D.Float( x, y, 4.0f, 4.0f ) );
				
				counter++;
			}
		}
		
		g2.dispose();
	}
}
