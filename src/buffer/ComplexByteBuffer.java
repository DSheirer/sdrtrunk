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
import java.util.Enumeration;

import sample.complex.ComplexSample;

/**
 * Provides a complex sample enumeration wrapper around a byte array containing
 * interleaved byte-length I & Q sample values in the range of 0 - 255, converting
 * those samples into complex samples with value range of -128 to 0 to 127.  
 */
public class ComplexByteBuffer implements Enumeration<ComplexSample>
{
	private static float[] mValues;

	/**
	 * Creates a lookup table that converts the 8-bit valued range from 0 - 255
	 * into scaled float values of -128 to 0 to 127
	 */
	static
	{
		mValues = new float[ 256 ];
		
		float scale = (float)( 1.0f / 127.0f );
		
		for( int x = 0; x < 256; x++ )
		{
			mValues[ x ] = (float)( x - 128 ) * scale;
		}
	}

	private ByteBuffer mBytes;
//	private byte[] mBytes;
//	private int mPointer;
	
	public ComplexByteBuffer( byte[] bytes )
	{
		mBytes = ByteBuffer.wrap( bytes );
//		mBytes = bytes;
//		mPointer = 0;
	}
	
	public byte[] toArray()
	{
//		return mBytes;
		return mBytes.array();
	}

	/**
	 * Indicates if there are more complex samples remaining.
	 */
	@Override
	public boolean hasMoreElements()
	{
//		return mPointer < mBytes.length;
		return mBytes.hasRemaining();
	}

	/**
	 * Returns the next complex sample.
	 * 
	 * Note: does not check for buffer overrun.  Use the hasMoreElements()
	 * method to ensure you do not read past the end of the buffer.
	 */
	@Override
    public ComplexSample nextElement()
    {
//		return new ComplexSample( mValues[ ( mBytes[ mPointer++ ] & 0xFF ) ], 
//				  mValues[ ( mBytes[ mPointer++ ] & 0xFF ) ] );
		return new ComplexSample( mValues[ ( mBytes.get() & 0xFF ) ], 
				  mValues[ ( mBytes.get() & 0xFF ) ] );
    }
}
