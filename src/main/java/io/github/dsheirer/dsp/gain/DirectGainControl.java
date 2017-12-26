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
package io.github.dsheirer.dsp.gain;

import io.github.dsheirer.sample.real.RealSampleListener;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectGainControl implements GainController, RealSampleListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( DirectGainControl.class );

	private float mDefaultGain;
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
		Validate.isTrue(minimum > 0);

		mDefaultGain = gain;
		mGain = gain;
		mMinimum = minimum;
		mMaximum = maximum;
		mIncrement = increment;
	}
	
	public void dispose()
	{
		mListener = null;
	}
	
	public float correct( float sample )
	{
		return sample * mGain;
	}

	@Override
    public void receive( float sample )
    {
		mListener.receive( correct( sample ) );
    }

	@Override
	public void reset()
	{
		mGain = mDefaultGain;
	}

	@Override
	public void increase()
	{
		mGain += mIncrement;
		
		if( mGain > mMaximum )
		{
			mGain = mMaximum;
		}
	}
	
	@Override
	public void decrease()
	{
		mGain -= mIncrement;
		
		if( mGain < mMinimum )
		{
			mGain = mMinimum;
		}
	}
	
    public void setListener( RealSampleListener listener )
    {
		mListener = listener;
    }
}
