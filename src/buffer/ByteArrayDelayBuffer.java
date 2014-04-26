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

public class ByteArrayDelayBuffer
{
	private int mBufferSize;
	private byte[][] mBuffer = null;
	private int mBufferPointer = 0;

	/**
	 * Implements a delay buffer for byte arrays.  As a new byte[] is placed 
	 * into the buffer, the oldest byte[] is returned
	 * @param bufferSize - number of byte arrays to buffer
	 */
	public ByteArrayDelayBuffer( int bufferSize )
	{
		mBufferSize = bufferSize;
	}

	/**
	 * Inserts new bytes into the buffer
	 * 
	 * @return - oldest byte[] from the buffer, or null if the buffer is not
	 * yet full
	 */
	public byte[] get( byte[] bytes )
	{
		if( mBuffer == null )
		{
			mBuffer = new byte[ mBufferSize ][ bytes.length ];
		}
		
		byte[] returnBytes = mBuffer[ mBufferPointer ];
		
		mBuffer[ mBufferPointer++ ] = bytes;
		
		if( mBufferPointer >= mBufferSize )
		{
			mBufferPointer = 0;
		}
		
		return returnBytes;
	}
}
