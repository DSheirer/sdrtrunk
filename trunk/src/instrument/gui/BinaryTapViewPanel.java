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
package instrument.gui;

import instrument.tap.Tap;
import instrument.tap.stream.BinaryTap;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Observable;

public class BinaryTapViewPanel extends TapViewPanel
{
	private Tap mTap;
	private List<Boolean> mSamples;
	private int mSampleCount;
	private int mOffset = 0;

	public BinaryTapViewPanel( BinaryTap tap )
	{
		super( new SampleModel<Boolean>(), tap.getName() );
		
		mTap = tap;
		mTap.addListener( getModel() );
		
		getModel().setSampleCount( 
			(int)( getModel().getSampleCount() * tap.getSampleRateRatio() ) );
		
		getModel().setDelay( tap.getDelay() );
	}
	
	@Override
    public void update( Observable arg0, Object arg1 )
    {
		mSamples = (List<Boolean>)getModel().getSamples();
		mSampleCount = getModel().getSampleCount();
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
				paintBit( g, x, mSamples.get( x ) );
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
	private void paintBit( Graphics graphics, int index, boolean value )
	{
		Graphics2D g2 = (Graphics2D)graphics;

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

		float y = ( value ? ( middleY - ( middleY * 0.5f) ) : 
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
		String bit = value ? "1" : "0";

        FontMetrics fontMetrics = graphics.getFontMetrics( this.getFont() );
        
        Rectangle2D label = fontMetrics.getStringBounds( bit, graphics );
        
        float offsetX = (float)label.getWidth() / 2.0f;
        float offsetY = (float)label.getWidth() / 2.0f;

        float middleX = startX + ( indexWidth / 2.0f );
        
        graphics.drawString( bit, 
        					 (int)( middleX - offsetX ), 
        					 (int)( middleY - offsetY ) );
	}
}
