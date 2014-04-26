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
 * Circular sample buffer - stores samples in a circular buffer
 * overwriting older samples with newly arrived samples.  Initially
 * fills buffer with 0-valued samples
 * 
 * Continuously calculates the average of the samples in the buffer, with the
 * average available from the average() method.
 */
public class FloatCircularAveragingBuffer2
{
	private float[] mBuffer;
	private int mBufferPointer = 0;
	private float mAverage = 0.0f;
	
	public FloatCircularAveragingBuffer2( int size )
	{
		mBuffer = new float[ size ];
		Arrays.fill( mBuffer, 0.0f );
	}
	
	/**
	 * Loads the newest sample into the buffer and returns the oldest sample
	 */
	public float get( float newestSample )
	{
		float oldestSample = mBuffer[ mBufferPointer ];
		
		mBuffer[ mBufferPointer ] = newestSample;

		mBufferPointer++;
		
		if( mBufferPointer >= mBuffer.length )
		{
			mBufferPointer = 0;
		}
		
		mAverage = mAverage - ( oldestSample / mBuffer.length ) + 
							  ( newestSample / mBuffer.length );

		return oldestSample;
	}

	public float average()
	{
		return mAverage;
	}
}
