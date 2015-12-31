package source.tuner.frequency;

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
 ******************************************************************************/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import sample.real.RealBuffer;
import source.tuner.frequency.FrequencyChangeEvent.Event;
import buffer.FloatAveragingBuffer;

public class AutomaticFrequencyControl_RB extends FrequencyCorrectionControl 
									   implements Listener<RealBuffer>
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( AutomaticFrequencyControl_RB.class );
	
	private static float LARGE_ERROR = 0.300f;
	private static float MEDIUM_ERROR = 0.150f;
	private static float SMALL_ERROR = 0.030f;
	private static float FINE_ERROR = 0.015f;

	private static int LARGE_FREQUENCY_CORRECTION = 1500; //Hertz
	private static int MEDIUM_FREQUENCY_CORRECTION = 500;
	private static int SMALL_FREQUENCY_CORRECTION = 100;
	private static int FINE_FREQUENCY_CORRECTION = 50;
	
	private static int SAMPLE_THRESHOLD = 15000;
	private Mode mMode = Mode.FAST;
	
	private FloatAveragingBuffer mBuffer = 
			new FloatAveragingBuffer( SAMPLE_THRESHOLD );

	private int mSampleCounter = 0;
	private int mSkipCounter = 0;
	private int mSkipThreshold = 4;
	private int mErrorCorrection = 0;
	
	public enum Mode
	{
		NORMAL,FAST;
	}
	
	/**
	 * Automatic Frequency Control.  Monitors DC bias present in an FM 
	 * demodulated audio stream and issues automatic frequency corrections to 
	 * maintain the DC bias close to zero.
	 * 
	 * @param maximum - maximum allowable (+/-) frequency correction value.
	 */
	public AutomaticFrequencyControl_RB( int maximum )
	{
		super( maximum );
	}
	
	public void dispose()
	{
		super.dispose();
		
		mBuffer = null;
	}

	/**
	 * Resets the error correction value to zero and broadcasts a change event
	 */
	public void reset()
	{
		super.reset();
		
		mMode = Mode.FAST;
		dispatch();
	}

	/**
	 * Broadcasts a frequency correction change event to the registered listener
	 */
	private void dispatch()
	{
		if( mListener != null )
		{
			mListener.frequencyChanged( 
				new FrequencyChangeEvent( Event.CHANNEL_FREQUENCY_CORRECTION_CHANGE_NOTIFICATION, 
										  mErrorCorrection ) );
		}
	}

	@Override
	public void receive( RealBuffer t )
	{
		float average = 0.0f;
		
		for( float sample: t.getSamples() )
		{
			average = mBuffer.get( sample );
		}
		
		//TODO: fix this
		
//		update( average );
	}
	
    public void receive( float sample )
    {
		if( mMode == Mode.FAST )
		{
			float average = mBuffer.get( sample );
			
			mSampleCounter++;
			
			if( mSampleCounter >= SAMPLE_THRESHOLD )
			{
				mSampleCounter = 0;
				
				update( average );
			}
		}
		else
		{
			mSkipCounter++;
			
			if( mSkipCounter >= mSkipThreshold )
			{
				mSkipCounter = 0;

				float average = mBuffer.get( sample );
				
				mSampleCounter++;
				
				if( mSampleCounter >= SAMPLE_THRESHOLD )
				{
					mSampleCounter = 0;
					
					update( average );
				}
			}
		}
    }
	
	private void update( float average )
	{
		mMode = Mode.NORMAL;

		if( average > LARGE_ERROR )
		{
			adjust( -LARGE_FREQUENCY_CORRECTION );
			mMode = Mode.FAST;
		}
		else if( average > MEDIUM_ERROR )
		{
			adjust( -MEDIUM_FREQUENCY_CORRECTION );
			mMode = Mode.FAST;
		}
		else if( average > SMALL_ERROR )
		{
			adjust( -SMALL_FREQUENCY_CORRECTION );
		}
		else if( average > FINE_ERROR )
		{
			adjust( -FINE_FREQUENCY_CORRECTION );
		}
		else if( average < -LARGE_ERROR )
		{
			adjust( LARGE_FREQUENCY_CORRECTION );
			mMode = Mode.FAST;
		}
		else if( average < -MEDIUM_ERROR )
		{
			adjust( MEDIUM_FREQUENCY_CORRECTION );
			mMode = Mode.FAST;
		}
		else if( average < -SMALL_ERROR )
		{
			adjust( SMALL_FREQUENCY_CORRECTION );
		}
		else if( average < -FINE_ERROR )
		{
			adjust( FINE_FREQUENCY_CORRECTION );
		}
	}
}
