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

import java.util.ArrayList;
import java.util.Arrays;

public class FloatArrayCircularAveragingBuffer
{
	private ArrayList<float[]> mBuffer;
	private int mBufferPointer;
	private int mBufferSize;
	private float[] mAverage;
	
	public FloatArrayCircularAveragingBuffer( int bufferSize )
	{
		mBufferSize = bufferSize;
		mBuffer = new ArrayList<float[]>();
	}
	
	/**
	 * Replaces the oldest float array element in the buffer with this new
	 * float array element, and then returns the average of the buffer contents
	 * with the newElement values included in the average array
	 */
	public float[] get( float[] newElement )
	{
		/**
		 * If we've never setup the average array, or the newElements length is
		 * different than what we've been processing, reset the buffer & averages
		 */
		if( mAverage == null || mAverage.length != newElement.length )
		{
			reset( newElement.length );
		}

		float[] oldElement = mBuffer.get( mBufferPointer );
		
		for( int x = 0; x < newElement.length; x++ )
		{
			/**
			 * Change the current average value in each array element by the
			 * difference of the new element minus the old element, divided
			 * by the buffer size ... average it
			 */
			mAverage[ x ] += 
					( ( newElement[ x ] - oldElement[ x ] ) / mBufferSize );
		}
		
		mBuffer.set( mBufferPointer, newElement );

		mBufferPointer++;
		
		if( mBufferPointer >= mBufferSize )
		{
			mBufferPointer = 0;
		}
		
		return mAverage;
	}

	/**
	 * Clears buffer and average and fills buffer with zero-valued float arrays
	 */
	private void reset( int arrayLength )
	{
		mAverage = new float[ arrayLength ];
		Arrays.fill( mAverage, 0.0f );
		
		mBuffer.clear();
		mBufferPointer = 0;

		float[] emptyArray = new float[ arrayLength ];
		Arrays.fill( emptyArray, 0.0f );
		
		for( int x = 0; x < mBufferSize; x++ )
		{
			mBuffer.add( emptyArray );
		}
	}

}
