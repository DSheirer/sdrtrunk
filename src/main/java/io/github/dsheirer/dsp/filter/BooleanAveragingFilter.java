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
package io.github.dsheirer.dsp.filter;

import io.github.dsheirer.sample.Listener;

import java.util.BitSet;

/**
 * Averages (low pass filter) boolean values over the defined size
 */
public class BooleanAveragingFilter implements Listener<Boolean>
{
	private BitSet mBuffer = new BitSet();
	private int mBufferPointer;
	private int mBufferLength;
	private int mThreshold;
	private Listener<Boolean> mListener;
	
	public BooleanAveragingFilter( int length )
	{
		mBufferLength = length;
		
		/* Round up */
		mThreshold = (int)( length / 2) + ( length % 2 );
	}
	
	public void receive( Boolean value )
	{
		if( value )
		{
			mBuffer.set( mBufferPointer );
		}
		else
		{
			mBuffer.clear( mBufferPointer );
		}

		mBufferPointer++;
		
		if( mBufferPointer >= mBufferLength )
		{
			mBufferPointer = 0;
		}
		
		if( mListener != null )
		{
			mListener.receive( mBuffer.cardinality() >= mThreshold );
		}
	}
	
    public void setListener( Listener<Boolean> listener )
    {
		mListener = listener;
    }

    public void removeListener( Listener<Boolean> listener )
    {
		mListener = null;
    }
}
