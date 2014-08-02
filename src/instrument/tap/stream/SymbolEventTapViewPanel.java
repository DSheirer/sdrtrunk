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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Observable;

import log.Log;
import dsp.fsk.SymbolEvent;

public class SymbolEventTapViewPanel extends TapViewPanel
{
    private static final long serialVersionUID = 1L;

    private SymbolEventTap mTap;
	private List<SymbolEvent> mSamples;
	private int mSampleCount;

	public SymbolEventTapViewPanel( SymbolEventTap tap )
	{
		super( new SampleModel<SymbolEvent>(), tap.getName() );
		
		mTap = tap;
		mTap.addListener( getModel() );
		
		mSampleCount = (int)( getModel().getSampleCount() * tap.getSampleRateRatio() );

		getModel().setSampleCount( mSampleCount );

		getModel().setDelay( tap.getDelay() );
	}
	
	@Override
    public void update( Observable arg0, Object arg1 )
    {
		mSamples = (List<SymbolEvent>)getModel().getSamples();

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

		if( mSamples != null && mSamples.size() > 0 )
		{
			g2.setColor( getForeground() );
			
			g2.drawString( mLabel, 5 , 15 );

			for( int x = 1; x < mSamples.size(); x++ )
			{
				paint( g, x, mSamples.get( x ) );
			}
		}
		
		g2.dispose();
	}
	
	/**
	 * Paints a polyline representing a boolean value with a 0 or 1 label in 
	 * the middle of it
	 * 
	 * @param graphics
	 * @param index - index of the sample int the sample buffer
	 * @param value - bit value, 0 or 1
	 */
	private void paint( Graphics graphics, int index, SymbolEvent symbol )
	{
		Graphics2D g2 = (Graphics2D)graphics;

		g2.setColor( getForeground() );
		
		Path2D.Float polyline = 
		        new Path2D.Float( Path2D.Float.WIND_EVEN_ODD, mSamples.size() );
		
		float middleY = ( (float)getHeight() / 2.0f );

		float indexWidth = (float)( getWidth() ) / (float)mSampleCount;
		
		float startX = (float)index * indexWidth;
		float stopX = (float)(index + 1 ) * indexWidth;
		float fivePercent = indexWidth * .05f;
		float leftX = startX + fivePercent;
		float rightX = stopX - fivePercent;

		/* Start */
		polyline.moveTo( startX, middleY );

		/* Left horizonal segment */
		polyline.lineTo( leftX, middleY );

		float y = ( symbol.getDecision() ? ( middleY - ( middleY * 0.5f) ) : 
			( middleY + ( middleY * 0.5f) ) );

		/* Left vertical segment */
		polyline.lineTo( leftX, y );
		
		/* Middle horizontal segment */
		polyline.lineTo( rightX, y );

		/* Right vertical segment */
		polyline.lineTo( rightX, middleY );

		/* Right horizontal segment */
		polyline.lineTo( stopX, middleY );

		g2.draw( polyline );
		
		/* Draw the bit value label */
		String bit = symbol.getDecision() ? "1" : "0";

        FontMetrics fontMetrics = graphics.getFontMetrics( this.getFont() );
        
        Rectangle2D label = fontMetrics.getStringBounds( bit, graphics );
        
        float offsetX = (float)label.getWidth() / 2.0f;
        float offsetY = (float)label.getHeight() / 2.0f;

        float middleX = startX + ( indexWidth / 2.0f );
        
        graphics.drawString( bit, 
        					 (int)( middleX - offsetX ), 
        					 (int)( middleY - ( ( middleY < y ) ? offsetY : -( 2.0f * offsetY ) ) ) );
        
        /* Draw samples */
        float sampleWidth = ( rightX - leftX ) / (float)( symbol.getSamplesPerSymbol() + 1 );

        g2.setColor( Color.GREEN );
        
        for( int x = 0; x < symbol.getSamplesPerSymbol(); x++ )
        {
    		Path2D.Float line = 
		        new Path2D.Float( Path2D.Float.WIND_EVEN_ODD, 2 );

    		float sampleX = leftX + ( ( x + 1 ) * sampleWidth );
    		
//    		line.moveTo( sampleX, middleY );
    		line.moveTo( sampleX, ( symbol.getBitSet().get( x ) ?
    								middleY - ( middleY * .05f ) :
    								middleY + ( middleY * .05f ) ) );
    		
    		float sampleY = ( symbol.getBitSet().get( x ) ? 
    						( middleY - ( middleY * 0.45f ) ) : 
    						( middleY + ( middleY * 0.45f ) ) );
    		
    		line.lineTo( sampleX, sampleY );
    		
    		g2.draw( line );

            graphics.drawString( symbol.getShift().getLabel(), 
					 (int)( middleX - offsetX ), 
					 (int)( y - ( ( y < middleY ) ? offsetY : -( 2.0f * offsetY ) ) ) );
    		
        }
	}
}
