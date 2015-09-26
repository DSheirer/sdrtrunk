/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2015 Dennis Sheirer
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
package sample.real;

import sample.Listener;

/**
 * Assembles single float samples into a RealBuffer containing an array of floats
 */
public class RealToRealBufferAssembler implements RealSampleListener
{
	private int mBufferSize;
	private float[] mBuffer;
	private int mBufferPointer;
	
	private Listener<RealBuffer> mListener;
	
	public RealToRealBufferAssembler( int bufferSize )
	{
		mBufferSize = bufferSize;
		mBuffer = new float[ mBufferSize ];
	}
	
	public void dispose()
	{
		mListener = null;
		mBuffer = null;
	}
	
	public void reset()
	{
		mBuffer = new float[ mBufferSize ];
		mBufferPointer = 0;
	}
	
	@Override
	public void receive( float sample )
	{
		if( mBufferPointer >= mBufferSize )
		{
			if( mListener != null )
			{
				mListener.receive( new RealBuffer( mBuffer ) );
			}

			mBuffer = new float[ mBufferSize ];
			mBufferPointer = 0;
		}

		mBuffer[ mBufferPointer ] = sample;

		mBufferPointer++;
	}

	public void setListener( Listener<RealBuffer> listener )
	{
		mListener = listener;
	}
}
