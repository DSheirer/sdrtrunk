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
package io.github.dsheirer.buffer;

import java.util.Arrays;

/**
 * Circular sample buffer - allocates a buffer and stores samples in a circular
 * fashion, overwriting older samples with newly arrived samples.  Initially
 * fills buffer with 0-valued samples
 * 
 * Can be used as a delay-type buffer, to delay samples by the 'size' amount
 */
public class DoubleCircularBuffer
{
	double[] mBuffer;
	int mBufferPointer = 0;
	
	public DoubleCircularBuffer( int size )
	{
		mBuffer = new double[ size ];
		
		Arrays.fill( mBuffer, 0.0 );
	}

	/**
	 * Puts the new value into the buffer and returns the oldest buffer value
	 * that it replaced
	 * @param newValue
	 * @return
	 */
	public double get( double newValue )
	{
		double oldestSample = mBuffer[ mBufferPointer ];

		mBuffer[ mBufferPointer ] = newValue;
		
		mBufferPointer++;
		
		if( mBufferPointer >= mBuffer.length )
		{
			mBufferPointer = 0;
		}
		
		return oldestSample;
	}

	/**
	 * Returns the maximum from the values currently in the buffer
	 */
	public double max()
	{
		double max = 0.0d;
		
		for( int x = 0; x < mBuffer.length; x++ )
		{
			if( mBuffer[ x ] > max )
			{
				max = mBuffer[ x ];
			}
		}
		
		return max;
	}
}
