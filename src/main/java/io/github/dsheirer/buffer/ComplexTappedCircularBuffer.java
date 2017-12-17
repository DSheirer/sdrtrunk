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

import io.github.dsheirer.sample.complex.Complex;

/**
 * Implements a FIFO circular buffer for complex samples that provides access
 * to each complex sample by index number, where index 0 is the newest/latest
 * complex sample and index (length - 1) is the oldest sample in the buffer.
 */
public class ComplexTappedCircularBuffer
{
	private Complex[] mBuffer;
	private int mBufferPointer = 0;
	
	public ComplexTappedCircularBuffer( int length )
	{
		mBuffer = new Complex[ length ];

		//Pre-load the buffer with 0-valued samples
		for( int x = 0; x < length; x++ )
		{
			mBuffer[ x ] = new Complex( 0, 0 );
		}
	}

	/**
	 * Adds the sample to the buffer and removes the oldest sample
	 * @param sample
	 */
    public void add( Complex sample )
    {
		mBuffer[ mBufferPointer++ ] = sample;

		//Reset the buffer pointer once it points past the end of the buffer
		if( mBufferPointer >= mBuffer.length )
		{
			mBufferPointer = 0;
		}
    }

	/**
	 * Retrieves a sample currently in the buffer by index position.
	 * 
	 * Index 0 is the newest/latest sample in the buffer and index (length - 1)
	 * is the oldest.
	 * 
	 * Note: the buffer is preloaded with complex samples of 0,0 value.
	 * 
	 * @param index where 0 is the newest/latest sample and length-1 is the oldest
	 * @return complex sample from the buffer.
	 * @throws ArrayIndexOutOfBoundsException if you attempt to access a sample
	 * outside of the buffer size
	 */
	public Complex get( int index )
	{
		/* Since the samples are stored in a forward fashion, we have to access
		 * the samples in reverse to comply with the access contract, where
		 * index 0 is the newest and index (length -1) is the oldest, all 
		 * relative to the current buffer pointer */
		
		int pointer = mBuffer.length - 1 + mBufferPointer - index;

		if( pointer >= mBuffer.length )
		{
			pointer -= mBuffer.length;
		}

		return mBuffer[ pointer ];
	}
}
