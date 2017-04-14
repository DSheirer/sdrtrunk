package dsp.gain;

import sample.complex.Complex;
import sample.complex.ComplexSampleListener;
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
public class ComplexFeedForwardGainControl implements ComplexSampleListener
{
	public static final float OBJECTIVE_ENVELOPE = 1.0f;
	public static final float MINIMUM_ENVELOPE = 0.0001f;

	private ComplexSampleListener mListener;

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
	
	public void dispose()
	{
		mListener = null;
	}

	@Override
	public void receive( float inphase, float quadrature )
	{
		float envelope = Complex.envelope( inphase, quadrature );

		if( envelope > mMaxEnvelope )
		{
			mMaxEnvelope = envelope;
			
			adjustGain();
		}

		/* Replace oldest envelope value with current envelope value */
		float oldestEnvelope = mEnvelopeHistory.putAndGet( envelope );

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
			mListener.receive( inphase *= mGain, quadrature *= mGain );
		}
	}
	
	private void adjustGain()
	{
		mGain = OBJECTIVE_ENVELOPE / mMaxEnvelope;
	}

	public void setListener( ComplexSampleListener listener )
	{
		mListener = listener;
	}
}
