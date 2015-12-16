package dsp.gain;

import java.util.concurrent.atomic.AtomicBoolean;

import sample.Listener;
import sample.Provider;
import sample.complex.Complex;
import buffer.ComplexCircularBuffer;
import buffer.DoubleCircularBuffer;

/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *     Copyright (C) 2011 Alex Csete
 *     Copyright (C) 2010 Moe Wheatley
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
 *     ---------------------------------------------------
 *     Ported from Alex Csete's gqrx source code at:
 *     https://github.com/csete/gqrx/blob/74ba2742c89b0dfb66854fc9f72d19e5c6e355b6/dsp/agc_impl.cpp
 *     
 * 	   This Software is released under the "Simplified BSD License"  + + +
 *     Copyright 2010 Moe Wheatley. All rights reserved.
 *
 *     Redistribution and use in source and binary forms, with or without 
 *     modification, are permitted provided that the following conditions are 
 *     met:
 *
 *     1. Redistributions of source code must retain the above copyright notice, 
 *     this list of conditions and the following disclaimer.
 *
 *     2. Redistributions in binary form must reproduce the above copyright 
 *     notice, this list of conditions and the following disclaimer in the 
 *     documentation and/or other materials provided with the distribution.
 *
 *     THIS SOFTWARE IS PROVIDED BY Moe Wheatley ``AS IS'' AND ANY EXPRESS OR 
 *     IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 *     OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 *     IN NO EVENT SHALL Moe Wheatley OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, 
 *     INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *     (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *     SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 *     HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 *     STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 *     ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 *     POSSIBILITY OF SUCH DAMAGE.
 *
 *     The views and conclusions contained in the software and documentation are 
 *     those of the authors and should not be interpreted as representing 
 *     official policies, either expressed or implied, of Moe Wheatley.
 ******************************************************************************/
public class ComplexAutomaticGainControl implements Listener<Complex>,
												    Provider<Complex>
{
	private static final double SAMPLE_RATE = 48000;
	
	/* Signal delay line - time delay in seconds */
	private static final double DELAY_TIME_CONSTANT = 0.015;

	/* Peak detector window - time delay in seconds */
	private static final double WINDOW_TIME_CONSTANT = 0.018;

	/* Attack time constants in seconds */
	private static final double ATTACK_RISE_TIME_CONSTANT = 0.002;
	private static final double ATTACK_FALL_TIME_CONSTANT = 0.005;

	private static final double ATTACK_RISE_ALPHA = 1.0 - 
		Math.exp( -1.0 / SAMPLE_RATE * ATTACK_RISE_TIME_CONSTANT );

	private static final double ATTACK_FALL_ALPHA = 1.0 - 
		Math.exp( -1.0 / SAMPLE_RATE * ATTACK_FALL_TIME_CONSTANT );

	/* AGC decay value in milliseconds (20 to 5000) */
	private static final double DECAY = 200;
	
	/* Ratio between rise and fall times of decay time constants - adjust for
	 * best action with SSB */
	private static final double DECAY_RISEFALL_RATIO = 0.3;

	private static final double DECAY_RISE_ALPHA = 1.0 - 
		Math.exp( -1.0 / ( SAMPLE_RATE * DECAY * .001 * DECAY_RISEFALL_RATIO ) );

	private static final double DECAY_FALL_ALPHA = 1.0 - 
		Math.exp( -1.0 / ( SAMPLE_RATE * DECAY * .001 ) );

	/* Hang timer release decay time constant in seconds */
	private static final double RELEASE_TIME_CONSTANT = 0.05;

	/* Specifies the AGC Knee in dB if AGC is active (nominal range -160 to 0 dB) */
	private static final double THRESHOLD = -100;

	/* Limit output to about 3db of maximum */
	private static final double AGC_OUT_SCALE = 0.7;

	/* Keep max input and output the same */
	private static final double MAX_AMPLITUDE = 1.0;
	private static final double MAX_MANUAL_AMPLITUDE = 1.0;
	
	/* Specifies AGC manual gain in dB if AGC is not active ( 0 to 100 dB) */
	private static final double MANUAL_GAIN = 0.0;

	private static final double MANUAL_AGC_GAIN = MAX_MANUAL_AMPLITUDE * 
			Math.pow( 10.0, MANUAL_GAIN / 20.0 );

	/* Specifies dB reduction in output at knee from max output level (0 - 10dB) */
	private static final double SLOPE_FACTOR = 2.0;

	private static final double KNEE = THRESHOLD / 20.0;

	private static final double GAIN_SLOPE = SLOPE_FACTOR / 100.0;

	private static final double FIXED_GAIN = AGC_OUT_SCALE * 
			Math.pow( 10.0, KNEE * ( GAIN_SLOPE - 1.0 ) );
	
	/* Constant for calc log() so that a value of 0 magnitude = -8 */
	private static final double MIN_CONSTANT = 3.2767E-4;
	
	private AtomicBoolean mAGCEnabled = new AtomicBoolean( true );
	
	private double mPeakMagnitude = 0.0;
	private double mAttackAverage = 0.0;
	private double mDecayAverage = 0.0;
	
	private ComplexCircularBuffer mDelayBuffer = 
		new ComplexCircularBuffer( (int)( SAMPLE_RATE * DELAY_TIME_CONSTANT ) );
	private DoubleCircularBuffer mMagnitudeBuffer = 
		new DoubleCircularBuffer( (int)( SAMPLE_RATE * WINDOW_TIME_CONSTANT ) );
	
	private Listener<Complex> mListener;
	
	public ComplexAutomaticGainControl()
	{
	}

	@Override
    public void receive( Complex currentSample )
    {
		Complex delayedSample = mDelayBuffer.get( currentSample );

		double gain = MANUAL_AGC_GAIN;
		
		if( mAGCEnabled.get() )
		{
			float max = currentSample.maximumAbsolute();

			double currentMagnitude = Math.log10( max + MIN_CONSTANT ) - 
							   Math.log10( MAX_AMPLITUDE );

			double delayedMagnitude = mMagnitudeBuffer.get( currentMagnitude );
			
			if( currentMagnitude > mPeakMagnitude )
			{
				/* Use current magnitude as peak if it's larger */
				mPeakMagnitude = currentMagnitude;
			}
			else if( delayedMagnitude == mPeakMagnitude )
			{
				/* If delayed magnitude is the current peak, then find a new peak */
				mPeakMagnitude = mMagnitudeBuffer.max();
			}

			/* Exponential decay mode */
			if( mPeakMagnitude > mAttackAverage )
			{
				mAttackAverage = ( ( 1.0 - ATTACK_RISE_ALPHA ) * mAttackAverage ) +
						         ( ATTACK_RISE_ALPHA * mPeakMagnitude );
			}
			else
			{
				mAttackAverage = ( ( 1.0 - ATTACK_FALL_ALPHA ) * mAttackAverage ) +
				         ( ATTACK_FALL_ALPHA * mPeakMagnitude );
			}
			
			if( mPeakMagnitude > mDecayAverage )
			{
				mDecayAverage = ( ( 1.0 - DECAY_RISE_ALPHA ) * mDecayAverage ) + 
						        ( DECAY_RISE_ALPHA * mPeakMagnitude );
			}
			else
			{
				mDecayAverage = ( ( 1.0 - DECAY_FALL_ALPHA ) * mDecayAverage ) + 
				        ( DECAY_RISE_ALPHA * mPeakMagnitude );
			}

			double magnitude = ( mAttackAverage > mDecayAverage ) ? 
								 mAttackAverage : mDecayAverage;

			if( magnitude < KNEE )
			{
				gain = FIXED_GAIN;
			}
			else
			{
				gain = AGC_OUT_SCALE * Math.pow( 10.0, 
						magnitude * ( GAIN_SLOPE - 1.0 ) );
			}
			
		}

		delayedSample.multiply( (float)gain );
		
		if( mListener != null )
		{
			mListener.receive( delayedSample );
		}
    }
	
	/**
	 * Enables or disables Automatic Gain Control (AGC).
	 */
	public void setAGCEnabled( boolean enabled )
	{
		mAGCEnabled.set( enabled );
	}

	/**
	 * Indicates if AGC is enabled
	 * @return true=AGC, false=MANUAL GAIN
	 */
	public boolean isAGCEnabled()
	{
		return mAGCEnabled.get();
	}

	@Override
    public void setListener( Listener<Complex> listener )
    {
		mListener = listener;
    }

	@Override
    public void removeListener( Listener<Complex> listener )
    {
		mListener = null;
    }
}
