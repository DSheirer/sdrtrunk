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
package dsp;

import sample.Listener;

public class BooleanZeroCrossingDetector implements Listener<Boolean>
{
	private int mBaudLength;
	private int mCounter = 0;
	private int mThreshold;
	private Listener<Boolean> mListener;
	private Cycle mCycle = Cycle.POSITIVE;
	
	/**
	 * Implements a zero crossing detector for use with squared, filtered
	 * binary values.  Makes a baud decision at 100% of the baud
	 * length, or at 80% of the baud length during a zero crossing.  Counter
	 * is reset at each zero crossing.
	 * 
	 * @param baudLength - estimated number of boolean samples per baud
	 */
	public BooleanZeroCrossingDetector( int baudLength )
	{
		mBaudLength = baudLength;
		mThreshold = (int)( baudLength * .80 );
	}

	@Override
    public void receive( Boolean sample )
    {
		mCounter++;

		switch( mCycle )
		{
			case POSITIVE:
				if( sample )
				{
					if( mCounter >= mBaudLength )
					{
						send( true );
						mCounter -= mBaudLength;
					}
				}
				else
				{
					if( mCounter >= mThreshold )
					{
						send( true );
						mCounter -= mBaudLength;
					}
					
					mCycle = Cycle.NEGATIVE;
				}
				break;
				
			case NEGATIVE:
				if( sample )
				{
					if( mCounter >= mThreshold )
					{
						send( false );

						mCounter -= mBaudLength;
					}

					mCycle = Cycle.POSITIVE;
				}
				else
				{
					if( mCounter >= mBaudLength )
					{
						send( false );
						
						mCounter -= mBaudLength;
					}
				}
				
				break;
		}
    }
	
	private void send( boolean bit )
	{
		if( mListener != null )
		{
			mListener.receive( bit );
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
	
	public enum Cycle { POSITIVE, NEGATIVE };
}
