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

import sample.Listener;

/**
 * Delay Buffer - implements a circular delay buffer backed by an object array
 */
public class DelayBuffer<T> implements Listener<T>
{
	private Object[] mBuffer;
	private int mBufferPointer = 0;
	private Listener<T> mListener;
	
	public DelayBuffer( int length )
	{
		mBuffer = new Object[ length ];
	}
	
    @Override
	@SuppressWarnings( "unchecked" )
    public void receive( T value )
    {
		Object delayed = mBuffer[ mBufferPointer ];
		
		mBuffer[ mBufferPointer ] = value;
		
		mBufferPointer++;

		/* Wrap the buffer pointer around to 0 when necessary */
		if( mBufferPointer >= mBuffer.length )
		{
			mBufferPointer = 0;
		}

		if( mListener != null && delayed != null )
		{
			mListener.receive( (T)delayed );
		}
    }
	
	public void setListener( Listener<T> listener )
	{
		mListener = listener;
	}
	
	public void removeListener( Listener<T> listener )
	{
		mListener = null;
	}
}
