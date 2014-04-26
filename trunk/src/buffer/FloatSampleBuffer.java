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


public class FloatSampleBuffer
{
    private static final long serialVersionUID = 1L;

    private int mSize;
    private float[] mBuffer;
    private int mBufferPointer = 0;
	
	public FloatSampleBuffer( int size )
	{
		super();
		mBuffer = new float[ size ];
	}

	public void put( float sample )
	{
		if( !isFull() )
		{
			mBuffer[ mBufferPointer++ ] = sample;
		}
	}
	
	public void reset()
	{
		mBufferPointer = 0;
	}
	
	public boolean isFull()
	{
		return mBufferPointer >= mBuffer.length;
	}
	
	public byte[] getBytes()
	{
		ByteBuffer bb = ByteBuffer.allocate( mBuffer.length * 2 );
		
		for( int x = 0; x < mBuffer.length; x++ )
		{
			bb.putShort( ((Float)mBuffer[ x ]).shortValue() );
		}

		return bb.array();
	}
}
