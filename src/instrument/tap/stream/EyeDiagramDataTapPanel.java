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

import instrument.gui.SampleModel;
import instrument.tap.TapViewPanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Observable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.complex.ComplexSample;
import dsp.psk.EyeDiagramData;

public class EyeDiagramDataTapPanel extends TapViewPanel
{
    private static final long serialVersionUID = 1L;
	private final static Logger mLog = 
			LoggerFactory.getLogger( EyeDiagramDataTapPanel.class );

	private EyeDiagramDataTap mTap;
	private List<EyeDiagramData> mSamples;

	public EyeDiagramDataTapPanel( EyeDiagramDataTap tap )
	{
		super( new SampleModel<EyeDiagramData>(), tap.getName() );
		
		mTap = tap;
		mTap.addListener( getModel() );

		getModel().setSampleCount( 15 );
		getModel().setDelay( tap.getDelay() );
	}
	
	@Override
    public void update( Observable arg0, Object arg1 )
    {
		mSamples = (List<EyeDiagramData>)getModel().getSamples();
		
		repaint();
    }
	
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );
		
		Graphics2D g2 = (Graphics2D)g;
		

		int min = ( getHeight() < getWidth() ? getHeight() : getWidth() );
		

		float halfWidth = getWidth() / 2.0f;
		float halfHeight = getHeight() / 2.0f;
		float xAxisIncrement = getWidth() / 19.0f / 4.0f;
		float middle = 6.0f * xAxisIncrement;

		g2.setColor( Color.BLUE );
		g2.drawString( "Eye Diagram - I Leg", 5 , 15 );
		g2.drawString( "Eye Diagram - Q Leg", halfWidth , 15 );
		

		/* Vertical Line */
		g2.drawLine( getWidth() / 8, 5, getWidth() / 8, getHeight() - 5 );
		g2.drawLine( (int)( halfWidth + ( getWidth() / 8 ) ), 5, 
				(int)( halfWidth + ( getWidth() / 8 ) ), getHeight() - 5 );

		g2.setColor( Color.LIGHT_GRAY );

		g2.drawLine( (int)( getWidth() / 8 - xAxisIncrement ), 5, (int)( getWidth() / 8 - xAxisIncrement ), getHeight() - 5 );
		g2.drawLine( (int)( halfWidth + ( getWidth() / 8 ) - xAxisIncrement ), 5, 
				(int)( halfWidth + ( getWidth() / 8 ) - xAxisIncrement ), getHeight() - 5 );

		g2.drawLine( (int)( getWidth() / 8 + xAxisIncrement ), 5, (int)( getWidth() / 8 + xAxisIncrement ), getHeight() - 5 );
		g2.drawLine( (int)( halfWidth + ( getWidth() / 8 ) + xAxisIncrement ), 5, 
				(int)( halfWidth + ( getWidth() / 8 ) + xAxisIncrement ), getHeight() - 5 );
		
		/* Horizontal Line */
		g2.drawLine( 0, getHeight() / 2, getWidth() / 4, getHeight() / 2 );
		g2.drawLine( (int)halfWidth, getHeight() / 2, (int)(getWidth() * .75), 
				getHeight() / 2 );
		
		if( mSamples != null && 
			mSamples.size() > 0  )
		{

			g2.setColor( Color.BLUE );

			for( int z = 0; z < mSamples.size(); z++ )
			{
				EyeDiagramData data = mSamples.get( z );

				if( z < mSamples.size() * .75 )
				{
					g2.setColor( Color.LIGHT_GRAY );
				}
				else if( z == mSamples.size() - 1 )
				{
					g2.setColor( Color.BLUE );
				}
				else
				{
					g2.setColor( Color.GRAY );
				}

				ComplexSample[] samples = data.getSamples();
				
				Path2D.Float iLine = new Path2D.Float( GeneralPath.WIND_EVEN_ODD, 20 );
				Path2D.Float qLine = new Path2D.Float( GeneralPath.WIND_EVEN_ODD, 20 );
				
				float xAxis = middle - ( data.getMiddlePoint() * xAxisIncrement );
				
				iLine.moveTo( xAxis, halfHeight );
				qLine.moveTo( xAxis + halfWidth, halfHeight );
				
				for( int x = 0; x < samples.length; x++ )
				{
					float i = samples[ x ].inphase();
					
					if( i > 1.0 )
					{
						i = 1.0f;
					}
					if( i < -1.0 )
					{
						i = -1.0f;
					}
					
					float iy = halfHeight + ( .85f * ( halfHeight * i ) );
					
					iLine.lineTo( xAxis, iy );
					
					float q = samples[ x ].quadrature();
					
					if( q > 1.0 )
					{
						q = 1.0f;
					}
					if( q < -1.0 )
					{
						q = -1.0f;
					}
					
					float qy = halfHeight + ( .85f * ( halfHeight * q ) );
					
					qLine.lineTo( xAxis + halfWidth, qy );
					
					xAxis += xAxisIncrement;
				}
				
				g2.draw( iLine );
				g2.draw( qLine );
			}
		}

		if( mSamples.size() > 0 )
		{
			EyeDiagramData data = mSamples.get( mSamples.size() - 1 );

			g2.setColor( Color.GREEN );
			
			float xAxis = middle - ( data.getMiddlePoint() * xAxisIncrement );

			Path2D.Float magnitude = new Path2D.Float( GeneralPath.WIND_EVEN_ODD, 20 );
			magnitude.moveTo( xAxis, halfHeight );
			
			for( ComplexSample sample: data.getSamples() )
			{
				magnitude.lineTo( xAxis, getHeight() - ( getHeight() * sample.magnitude() ) );
				
				xAxis += xAxisIncrement;
			}

			g2.setStroke( new BasicStroke( 3 ) );
			g2.draw( magnitude );
		}
		
		g2.dispose();
	}
}
