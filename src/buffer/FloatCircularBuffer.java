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
package buffer;

import java.util.Arrays;

/**
 * Circular sample buffer - allocates a buffer and stores samples in a circular
 * fashion, overwriting older samples with newly arrived samples.  Initially
 * fills buffer with 0-valued samples
 * 
 * Can be used as a delay-type buffer, to delay samples by the 'size' amount
 * @author denny
 *
 */
public class FloatCircularBuffer
{
	float[] mBuffer;
	int mBufferPointer = 0;
	
	public FloatCircularBuffer( int size )
	{
		mBuffer = new float[ size ];
		
		Arrays.fill( mBuffer, (float)0.0 );
	}

	/**
	 * Puts the new value into the buffer and returns the oldest buffer value
	 * that it replaced
	 * @param newValue
	 * @return
	 */
	public float get( float newValue )
	{
		float oldestSample = mBuffer[ mBufferPointer ];

		mBuffer[ mBufferPointer ] = newValue;
		
		mBufferPointer++;
		
		if( mBufferPointer >= mBuffer.length )
		{
			mBufferPointer = 0;
		}
		
		return oldestSample;
	}

	/**
	 * Convenience method to put a new value in the buffer, while discarding
	 * the old value
	 */
	public void put( float newValue )
	{
		get( newValue );
	}

	/**
	 * Returns an array of arraySize, loaded with sampleCount of the 
	 * oldest samples, loaded into the array starting at index 0.
	 * 
	 * This is useful for fetching an array filled half way with samples to
	 * use in FFT processing.
	 * @param arraySize - size of the array (must be larger than sampleCount)
	 * @param sampleCount - number of the oldest samples in the buffer to load
	 * @return
	 */
	public float[] get( int arraySize, int sampleCount )
	{
		float[] samples = new float[ arraySize ];

		if( arraySize > sampleCount )
		{
			int tempCounter = mBufferPointer;

			for( int x = 0; x < sampleCount; x++ )
			{
				samples[ x ] = mBuffer[ tempCounter ];
				
				tempCounter++;
				
				if( tempCounter > mBuffer.length )
				{
					tempCounter = 0;
				}
			}
		}
		
		return samples;
	}
	
}
