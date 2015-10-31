/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package spectrum.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexDecibelConverter extends DFTResultsConverter
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ComplexDecibelConverter.class );
	
	private float mDynamicRangeReference = (float)Math.pow( 2.0, 16.0 );

	/**
	 * Converts the output of the JTransforms FloatFFT_1D.complexForward()
	 * calculation into the power spectrum in decibels, normalized to the 
	 * sample bit depth.
	 */
	public ComplexDecibelConverter()
	{
	}
	
	/**
	 * Specifies the bit depth to establish the maximum dynamic range.  All
	 * FFT bin values will be scaled according to this value.
	 */
	@Override
	public void setSampleSize( double size )
	{
		assert( 2.0 <= size && size <= 32.0 );
		
		mDynamicRangeReference = (float)Math.pow( 2.0d, size );
	}

	@Override
    public void receive( float[] results )
    {
		float[] processed = new float[ results.length / 2 ];

		int half = processed.length / 2;

		for( int x = 0; x < results.length; x += 2 )
		{
			//Calculate the magnitude squared value from each bin's real and
			//imaginary value, scale it to the available dynamic range and 
			//convert to dB.
			float normalizedMagnitude = 10.0f * (float)Math.log10( 
				( ( results[ x ] * results[ x ] ) + 
				  ( results[ x + 1 ] * results[ x + 1 ] ) ) / 
				  		mDynamicRangeReference );

			// We have to swap the upper and lower halves of the JTransforms
			// DFT results for correct display
			if( x / 2 >= half )
			{
				processed[ x / 2 - half ] = normalizedMagnitude;
			}
			else
			{
				processed[ x / 2 + half ] = normalizedMagnitude;
			}
		}
		
		dispatch( processed );
    }
}
