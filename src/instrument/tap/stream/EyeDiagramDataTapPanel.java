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

import sample.complex.Complex;

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
		
		float third = getWidth() / 3.0f;
		float two_thirds = 2.0f * third;
		float halfHeight = getHeight() / 2.0f;
		
		float xAxisIncrement = third / 20.0f;

		if( mSamples != null && mSamples.size() > 0  )
		{
			/* Draw each of the sample sets */
			for( int z = 0; z < mSamples.size(); z++ )
			{
				if( z == mSamples.size() - 1 )
				{
					g2.setColor( Color.BLUE );
				}
				else if( z > ( mSamples.size() * .75 ) )
				{
					g2.setColor( Color.DARK_GRAY );
				}
				else
				{
					g2.setColor( Color.LIGHT_GRAY );
				}
				
				EyeDiagramData data = mSamples.get( z );

				float[] inphaseSamples = data.getInphaseSamples();
				float[] quadratureSamples = data.getQuadratureSamples();
				
				/* Draw the sample set containing two symbol periods of data */
				if( inphaseSamples.length >= 1 )
				{
					Path2D.Float iLine = new Path2D.Float( GeneralPath.WIND_EVEN_ODD, 20 );
					Path2D.Float qLine = new Path2D.Float( GeneralPath.WIND_EVEN_ODD, 20 );
					Path2D.Float mLine = new Path2D.Float( GeneralPath.WIND_EVEN_ODD, 20 );
					
					float xAxis = 0;
					
					iLine.moveTo( xAxis, getSampleHeight( halfHeight, inphaseSamples[ 0 ] ) );
					qLine.moveTo( xAxis + third, getSampleHeight( halfHeight, quadratureSamples[ 0 ] ) );
					mLine.moveTo( xAxis + ( two_thirds ), getMagnitudeHeight( Complex.magnitude( inphaseSamples[ 0 ], quadratureSamples[ 0 ] ) ) );

					for( int x = 1; x < inphaseSamples.length; x++ )
					{
						xAxis += xAxisIncrement;
						
						iLine.lineTo( xAxis, getSampleHeight( halfHeight, inphaseSamples[ x ] ) );
						qLine.lineTo( xAxis + third, getSampleHeight( halfHeight, quadratureSamples[ x ] ) );
						mLine.lineTo( xAxis + two_thirds, getMagnitudeHeight( Complex.magnitude( inphaseSamples[ x ], quadratureSamples[ x ] ) ) );
					}
					
					g2.draw( iLine );
					g2.draw( qLine );
					g2.draw( mLine );
					
					/* Left Sample line */
					if( z == mSamples.size() - 1 )
					{
						g2.setColor( Color.GREEN );
					}
					else
					{
						g2.setColor( Color.LIGHT_GRAY );
					}
					xAxis = xAxisIncrement * data.getLeftPoint();
					g2.drawLine( (int)xAxis, 5, (int)xAxis, getHeight() - 5 );
					g2.drawLine( (int)( xAxis + third ), 5, (int)( xAxis + third ), getHeight() - 5 );
					g2.drawLine( (int)( xAxis + two_thirds ), 5, (int)( xAxis + two_thirds ), getHeight() - 5 );
					
					/* Right Sample line */
					if( z == mSamples.size() - 1 )
					{
						g2.setColor( Color.GREEN );
					}
					else
					{
						g2.setColor( Color.LIGHT_GRAY );
					}
					xAxis = xAxisIncrement * data.getRightPoint();
					g2.drawLine( (int)xAxis, 5, (int)xAxis, getHeight() - 5 );
					g2.drawLine( (int)( xAxis + third ), 5, (int)( xAxis + third ), getHeight() - 5 );
					g2.drawLine( (int)( xAxis + two_thirds ), 5, (int)( xAxis + two_thirds ), getHeight() - 5 );

					/* Error indicator */
					float error = data.getError();
					
					if( z == mSamples.size() - 1 )
					{
						g2.setColor( Color.BLACK );

						float xFrom;
						float xTo;
						
						if( error < 0.0f )
						{
							xFrom = (int)( xAxisIncrement * data.getLeftPoint() );
							xTo = xFrom - 25.0f;
						}
						else
						{
							xFrom = (int)( xAxisIncrement * data.getRightPoint() );
							xTo = xFrom + 25.0f;
						}
						
						g2.drawLine( (int)xFrom, (int)halfHeight - 15, (int)xTo, (int)halfHeight );
						g2.drawLine( (int)xFrom, (int)halfHeight + 15, (int)xTo, (int)halfHeight );
						g2.drawLine( (int)( xFrom + third ), (int)halfHeight - 15, (int)( xTo + third ), (int)halfHeight );
						g2.drawLine( (int)( xFrom + third ), (int)halfHeight + 15, (int)( xTo + third ), (int)halfHeight );
						g2.drawLine( (int)( xFrom + two_thirds ), (int)halfHeight - 15, (int)( xTo + two_thirds ), (int)halfHeight );
						g2.drawLine( (int)( xFrom + two_thirds ), (int)halfHeight + 15, (int)( xTo + two_thirds ), (int)halfHeight );
					}

					
					
					
				}
			}
		}

		g2.setColor( Color.BLUE );
		g2.drawString( "Eye Diagram - I Leg", 5, 15 );
		g2.drawString( "Eye Diagram - Q Leg", third, 15 );
		g2.drawString( "Eye Diagram - Magnitude", two_thirds, 15 );


		/* Horizontal middle line */
		g2.setColor( Color.CYAN );
		g2.drawLine( 0, (int)halfHeight, getWidth(), (int)halfHeight );
		
		g2.dispose();
	}
	
	private float getSampleHeight( float halfHeight, float value )
	{
		if( value > 1.0 )
		{
			value = 1.0f;
		}
		if( value < -1.0 )
		{
			value = -1.0f;
		}
		
		return halfHeight + ( .95f * ( halfHeight * value ) );
	}

	private float getMagnitudeHeight( float magnitude )
	{
		if( magnitude > 1.0 )
		{
			magnitude = 1.0f;
		}
		
		return getHeight() - ( getHeight() * magnitude );
	}
}
