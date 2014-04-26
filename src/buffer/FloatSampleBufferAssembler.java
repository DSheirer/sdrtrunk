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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FloatSampleBufferAssembler
{
	private ByteBuffer mByteBuffer;
	private int mSizeInBytes;

	public FloatSampleBufferAssembler( int size )
	{
		mSizeInBytes = size * 2;

		//Allocate a byte buffer with 2 bytes per sample (ie 1 x 16-bit sample)
		mByteBuffer = ByteBuffer.allocate( mSizeInBytes );
		mByteBuffer.order( ByteOrder.LITTLE_ENDIAN );
	}

	/**
	 * Load the sample's bytes, left then right, little endian, into the 
	 * backing byte array
	 */
	public void put( float sample )
	{
		mByteBuffer.putShort( Float.valueOf( sample ).shortValue() );
	}

	/**
	 * Indicates if there is room for more samples in the buffer, or if it is full
	 */
	public boolean hasRemaining()
	{
		return mByteBuffer.hasRemaining();
	}

	/**
	 * Returns a copy of the byte array backing this buffer
	 */
	public ByteBuffer get()
	{
		return mByteBuffer;
//		return ByteBuffer.wrap( Arrays.copyOf( mByteBuffer.array(), mSizeInBytes ) );
	}

	/**
	 * Clear the bytes from the underlying buffer and reset the pointer to 0.
	 */
	public void reset()
	{
//		mByteBuffer.clear();
		mByteBuffer = ByteBuffer.allocate( mSizeInBytes );
		mByteBuffer.order( ByteOrder.LITTLE_ENDIAN );
	}
}
