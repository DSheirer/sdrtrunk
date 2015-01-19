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
package dsp.gain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.real.RealSampleListener;

public class DirectGainControl implements GainController, RealSampleListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( DirectGainControl.class );

	private float mGain;
	private float mMinimum;
	private float mMaximum;
	private float mIncrement;
	
	private RealSampleListener mListener;

	/**
	 * Gain Control.
	 * 
	 * Note: does not perform null checking on output listener in order to 
	 * streamline sample processing.
	 * 
	 * @param gain - initial value
	 * @param minimum - minimum value greater than 0
	 * @param maximum - maximum permitted value
	 * @param increment - amount gain is adjusted when increase() or decrease()
	 * is invoked
	 */
	public DirectGainControl( float gain, float minimum, float maximum, float increment )
	{
		assert( minimum > 0 );
		
		mGain = gain;
		mMinimum = minimum;
		mMaximum = maximum;
		mIncrement = increment;
	}

	@Override
    public void receive( float sample )
    {
		mListener.receive( (float)( sample * mGain ) );
    }

	@Override
	public void increase()
	{
		mGain += mIncrement;
		
		if( mGain > mMaximum )
		{
			mGain = mMaximum;
		}
		
		mLog.debug( "Gain Increased: " + mGain );
	}
	
	@Override
	public void decrease()
	{
		mGain -= mIncrement;
		
		if( mGain < mMinimum )
		{
			mGain = mMinimum;
		}
		
		mLog.debug( "Gain Decreased: " + mGain );
	}
	
    public void setListener( RealSampleListener listener )
    {
		mListener = listener;
    }
}
