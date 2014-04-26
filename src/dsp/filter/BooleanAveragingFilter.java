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
package dsp.filter;

import java.util.Arrays;

import sample.Listener;

/**
 * Averages (low pass filter) boolean values over the defined length
 */
public class BooleanAveragingFilter implements Listener<Boolean>
{
	private boolean[] mBuffer;
	private int mBufferPointer;
	private int mThreshold;
	private Listener<Boolean> mListener;
	
	public BooleanAveragingFilter( int length )
	{
		mBuffer = new boolean[ length ];

		if( length % 2 == 0 )
		{
			mThreshold = (int)( length /2 );
		}
		else
		{
			mThreshold = (int)( ( length + 1 ) / 2 );
		}
		
		//Preload the array with false values
		Arrays.fill( mBuffer, false );
	}
	
	/**
	 * Loads the newValue into this buffer and adjusts the buffer pointer
	 * to prepare for the next get/put cycle
	 */
	public void put( boolean newValue )
	{
		//Store the new value to the buffer
		mBuffer[ mBufferPointer ] = newValue;
		
		//Increment the buffer pointer
		mBufferPointer++;

		//Wrap the buffer pointer around to 0 when necessary
		if( mBufferPointer >= mBuffer.length )
		{
			mBufferPointer = 0;
		}
	}

	/**
	 * Loads the newValue into the buffer, calculates the average
	 * and returns that average from this method
	 * 
	 * This effectively performs low-pass filtering
	 */
	public void receive( Boolean newValue )
	{
		//Load the new value into the buffer
		put( newValue );
		
		int trueCount = 0;
		
		for( int x = 0; x < mBuffer.length; x++ )
		{
			if( mBuffer[ x ] )
			{
				trueCount++;
			}
		}

		/**
		 * If the number of true values in the buffer is 
		 * more than half, send true, otherwise, false
		 */
		if( mListener != null )
		{
			mListener.receive( trueCount >= mThreshold );
		}
	}
	
    public void setListener( Listener<Boolean> listener )
    {
		mListener = listener;
    }

    public void clearListener()
    {
		mListener = null;
    }
}
