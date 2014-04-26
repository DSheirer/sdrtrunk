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
 * Produces no output.  Obtain the average buffer value on-demand via the 
 * getAverage() method
 */
public class FloatCircularAveragingBuffer
{
	private float[] mBuffer;
	private int mBufferPointer = 0;
	private int mSampleCountForAveraging;
	private int mCurrentSampleCount = 0;
	private float mAverage = 0.0f;
	
	public FloatCircularAveragingBuffer( int size, int sampleCountForAveraging )
	{
		mBuffer = new float[ size ];
		mSampleCountForAveraging = sampleCountForAveraging;
		Arrays.fill( mBuffer, 0.0f );
	}

	/**
	 * Convenience wrapper to throw away to put the new sample and throwaway
	 * the oldest sample
	 */
	public void put( float newestSample )
	{
		get( newestSample );
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
		
		mCurrentSampleCount++;
		
		if( mCurrentSampleCount >= mSampleCountForAveraging )
		{
			calculateAverage();
			
			mCurrentSampleCount = 0;
		}
		
		return oldestSample;
	}

	/**
	 * Calculates the average of the maximum and minimum values present in 
	 * the buffer
	 */
	private void calculateAverage()
	{
		float min = mBuffer[ 0 ];
		float max = mBuffer[ 0 ];
		
		for( int x = 0; x < mBuffer.length; x++ )
		{
			if( mBuffer[ x ] < min )
			{
				min = mBuffer[ x ];
			}
			if( mBuffer[ x ] > max )
			{
				max = mBuffer[ x ];
			}
		}
		
		mAverage = (float)( ( min + max ) / 2.0f );
	}
	
	public float getAverage()
	{
		return mAverage;
	}
}
