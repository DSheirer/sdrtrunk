package ua.in.smartjava.source.tuner.frequency;
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
import java.util.concurrent.ScheduledExecutorService;

import ua.in.smartjava.module.Module;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.sample.real.RealBuffer;
import ua.in.smartjava.source.tuner.frequency.FrequencyChangeEvent.Event;

public class DCTrackingFrequencyControl extends Module 
				implements Listener<RealBuffer>, IFrequencyChangeProvider
{
	private static float COARSE_THRESHOLD = 0.1f;
	private static float LOOP_GAIN = 0.1f;
	private static float COARSE_GAIN = 1000.0f;
	private static float FINE_GAIN = 100.0f;
	private int mErrorCorrection = 0;
	private int mMaximumFrequencyCorrection = 0;
	private Listener<FrequencyChangeEvent> mListener;
	
	/**
	 * DC offset Automatic Frequency Control.  Monitors DC bias present in an FM 
	 * demodulated ua.in.smartjava.audio stream and issues automatic frequency corrections to
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
	public void start( ScheduledExecutorService executor )
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
