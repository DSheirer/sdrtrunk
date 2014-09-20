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
package sample.complex;

import gui.SDRTrunk;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.Listener;
import source.tuner.FrequencyChangeListener;
import buffer.FloatAveragingBuffer;

public class ComplexSampleRateCounter implements Listener<ComplexSample>,
												 ComplexSampleListListener,
												 FrequencyChangeListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( ComplexSampleRateCounter.class );

	private int mCounter;
	private int mAverageCounter;
	private int mExpectedSampleRate;
	private long mLastRun = 0;
	private int mBufferSize = 100;
	private FloatAveragingBuffer mAveragingBuffer;
	
	public ComplexSampleRateCounter( int expectedSampleRate, int bufferSize )
	{
		mExpectedSampleRate = expectedSampleRate;
		mBufferSize = bufferSize;
		
		mAveragingBuffer = new FloatAveragingBuffer( mBufferSize );		

		mLastRun = System.currentTimeMillis();

		mLog.info( "Complex Sample Rate Counter started ...." );
	}
	
	@Override
    public void receive( ComplexSample sample )
    {
		mCounter++;
		
		if( mCounter >= mExpectedSampleRate )
		{
			mCounter = 0;
			
			long now = System.currentTimeMillis();
			long elapsed = now - mLastRun;
			mLastRun = now;

			float average = (float)mExpectedSampleRate * 1000.0f / (float)elapsed;
			float runningAverage = mAveragingBuffer.get( average );
			
			mAverageCounter++;
			
			mLog.info( "Complex Sample Rate Avg " + mAverageCounter + 
					  " current [" + elapsed + " ms/" + average + 
					  " hz] average [" + runningAverage + " hz] expected [" +
					  mExpectedSampleRate + " hz] over [" +
						mBufferSize + "] buffer length" );
		}
    }

	@Override
    public void receive( List<ComplexSample> samples )
    {
		mCounter += samples.size();
		
		if( mCounter >= mExpectedSampleRate )
		{
			mCounter -= mExpectedSampleRate;
			
			long now = System.currentTimeMillis();
			long elapsed = now - mLastRun;
			mLastRun = now;

			float average = (float)mExpectedSampleRate * 1000.0f / (float)elapsed;
			float runningAverage = mAveragingBuffer.get( average );
			
			mAverageCounter++;
			
			mLog.info( "Complex Sample Rate Avg " + mAverageCounter + 
					  " current [" + elapsed + " ms/" + average + 
					  " hz] average [" + runningAverage + " hz] expected [" +
					  mExpectedSampleRate + " hz] over [" +
						mBufferSize + "] buffer length" );
		}
    }

	@Override
    public void frequencyChanged( long frequency, int bandwidth )
    {
		mExpectedSampleRate = bandwidth;
    }
}
