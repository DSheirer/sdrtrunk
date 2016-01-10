package source.tuner.frequency;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2016 Dennis Sheirer
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

import module.Module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.real.RealBuffer;
import source.tuner.frequency.FrequencyChangeEvent.Event;

public class DCTrackingFrequencyControl extends Module 
				implements Listener<RealBuffer>, IFrequencyChangeProvider
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( DCTrackingFrequencyControl.class );

	private static float COARSE_THRESHOLD = 0.1f;
	private static float LOOP_GAIN = 0.1f;
	private static float COARSE_GAIN = 1000.0f;
	private static float FINE_GAIN = 100.0f;
	private int mErrorCorrection = 0;
	private int mMaximumFrequencyCorrection = 0;
	private Listener<FrequencyChangeEvent> mListener;
	
	/**
	 * DC offset Automatic Frequency Control.  Monitors DC bias present in an FM 
	 * demodulated audio stream and issues automatic frequency corrections to 
	 * maintain the DC bias close to zero.
	 * 
	 * @param maximum - maximum allowable (+/-) frequency correction value.
	 */
	public DCTrackingFrequencyControl( int maximum )
	{
		mMaximumFrequencyCorrection = maximum;
	}
	
	public void dispose()
	{
		mListener = null;
	}

	@Override
	public void receive( RealBuffer buffer )
	{
		double sum = 0.0d;
		
		for( float sample: buffer.getSamples() )
		{
			sum += sample;
		}
		
		float mean = (float)( sum / (double)buffer.getSamples().length );
		
		float adjustment = 0.0f;
		
		if( mean > COARSE_THRESHOLD || mean < - COARSE_THRESHOLD )
		{
			adjustment = mean * COARSE_GAIN;
		}
		else
		{
			adjustment = mean * FINE_GAIN;
		}

		setErrorCorrection( mErrorCorrection + (int)( (float)adjustment * LOOP_GAIN ) );
	}
	
	private void setErrorCorrection( int correction )
	{
		if( mListener != null )
		{
			if( correction > mMaximumFrequencyCorrection )
			{
				correction = mMaximumFrequencyCorrection;
			}
			else if( correction < -mMaximumFrequencyCorrection )
			{
				correction = -mMaximumFrequencyCorrection;
			}
			
			if( correction != mErrorCorrection )
			{
				mListener.receive( new FrequencyChangeEvent( 
					Event.REQUEST_CHANNEL_FREQUENCY_CORRECTION_CHANGE, correction ) );
			}
		}
	}
	
	@Override
	public void setFrequencyChangeListener(	Listener<FrequencyChangeEvent> listener )
	{
		mListener = listener;
	}

	@Override
	public void removeFrequencyChangeListener()
	{
		mListener = null;
	}

	@Override
	public void start()
	{
	}

	@Override
	public void stop()
	{
	}

	@Override
	public void reset()
	{
		setErrorCorrection( 0 );
	}
}
