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

public class FloatAveragingBuffer
{
	private float[] mBuffer;
	private float mAverage = 0.0f;
	private int mBufferSize;
	private int mBufferPointer;
	
	public FloatAveragingBuffer( int size )
	{
		mBufferSize = size;
		mBuffer = new float[ size ];
	}
	
	public float get( float newValue )
	{
		float oldValue = mBuffer[ mBufferPointer ];

		if( Float.isInfinite( newValue ) || Float.isNaN( newValue ) )
		{
			mAverage = mAverage - ( oldValue / mBufferSize );

			mBuffer[ mBufferPointer++ ] = 0.0f;
		}
		else
		{
			mAverage = mAverage - ( oldValue / mBufferSize ) + ( newValue / mBufferSize );

			mBuffer[ mBufferPointer++ ] = newValue;
		}

		if( mBufferPointer >= mBufferSize )
		{
			mBufferPointer = 0;
		}

		return mAverage;
	}
}
