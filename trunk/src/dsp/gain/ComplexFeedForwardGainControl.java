package dsp.gain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.Provider;
import sample.complex.ComplexSample;
import buffer.FloatCircularBuffer;

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
 *     
 *     This class is modeled using concepts detailed in the GNURadio 
 *     implementation of feed forward AGC class located at:
 *     https://github.com/gnuradio/gnuradio/blob/master/gr-analog/lib/
 *     feedforward_agc_cc_impl.cc
 *     
 ******************************************************************************/
public class ComplexFeedForwardGainControl 
		implements Listener<ComplexSample>, Provider<ComplexSample>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ComplexFeedForwardGainControl.class );

	public static final float OBJECTIVE_ENVELOPE = 1.0f;
	public static final float MINIMUM_ENVELOPE = 0.0001f;

	private Listener<ComplexSample> mListener;

	private FloatCircularBuffer mEnvelopeHistory;
	
	private float mMaxEnvelope = 0.0f;
	private float mGain = 1.0f;
	
	/**
	 * Dynamic gain control for incoming sample stream to amplify or attenuate
	 * all samples toward an objective unity)gain, using the maximum envelope 
	 * value detected in the stream history window.  
	 * 
	 * Uses the specified damping factor to limit gain swings.  Damping factor
	 * is applied against the delta between current gain value and a recalculated
	 * gain value to limit how quickly the gain value will increase or decrease.
	 * 
	 * @param window - history size to use in detecting maximum envelope value
	 * @param damping - a value between 0 < damping <= 1.0;
	 */
	public ComplexFeedForwardGainControl( int window )
	{
		mEnvelopeHistory = new FloatCircularBuffer( window );
	}

	@Override
	public void receive( ComplexSample sample )
	{
		float envelope = sample.envelope();

		if( envelope > mMaxEnvelope )
		{
			mMaxEnvelope = envelope;
			
			adjustGain();
		}

		/* Replace oldest envelope value with current envelope value */
		float oldestEnvelope = mEnvelopeHistory.get( envelope );

		/* If the oldest envelope value was the max envelope value, then we 
		 * have to rediscover the max value from the envelope history */
		if( mMaxEnvelope == oldestEnvelope && mMaxEnvelope != envelope )
		{
			mMaxEnvelope = MINIMUM_ENVELOPE;
			
			for( float value: mEnvelopeHistory.getBuffer() )
			{
				if( value > mMaxEnvelope )
				{
					mMaxEnvelope = value;
				}
			}
			
			adjustGain();
		}
		
		/* Apply current gain value to the sample and send to the listener */
		if( mListener != null )
		{
			sample.multiply( mGain );
			
			mListener.receive( sample );
		}
	}
	
	private void adjustGain()
	{
		mGain = OBJECTIVE_ENVELOPE / mMaxEnvelope;
	}

	@Override
	public void setListener( Listener<ComplexSample> listener )
	{
		mListener = listener;
	}

	@Override
	public void removeListener( Listener<ComplexSample> listener )
	{
		mListener = null;
	}
}
