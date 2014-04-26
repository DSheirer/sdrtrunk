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

import sample.complex.ComplexSample;

/**
 * Circular sample buffer - allocates a buffer and stores samples in a circular
 * fashion, overwriting older samples with newly arrived samples.  Initially
 * fills buffer with 0-valued samples
 * 
 * Can be used as a delay-type buffer, to delay samples by the 'size' amount
 */
public class ComplexCircularBuffer
{
	ComplexSample[] mBuffer;
	int mBufferPointer = 0;
	
	public ComplexCircularBuffer( int size )
	{
		mBuffer = new ComplexSample[ size ];
		
		Arrays.fill( mBuffer, new ComplexSample( 1.0f, 1.0f ) );
	}

	/**
	 * Returns the sample currently pointed to in the buffer and stores the 
	 * new sample in its place
	 * @param sample
	 * @return
	 */
	public ComplexSample get( ComplexSample sample )
	{
		ComplexSample retVal = mBuffer[ mBufferPointer ];

		put( sample );

		return retVal;
	}
	
	public void put( ComplexSample sample )
	{
		mBuffer[ mBufferPointer ] = sample;

		mBufferPointer++;
		
		if( mBufferPointer >= mBuffer.length )
		{
			mBufferPointer = 0;
		}
	}
	
	public ComplexSample[] get( int count )
	{
		ComplexSample[] samples = new ComplexSample[ count ];
		
		int tempCounter = mBufferPointer;

		for( int x = 0; x < count; x++ )
		{
			samples[ x ] = mBuffer[ tempCounter ];
			
			tempCounter++;
			
			if( tempCounter > mBuffer.length - 1 )
			{
				tempCounter = 0;
			}
		}
		
		return samples;
	}
}
