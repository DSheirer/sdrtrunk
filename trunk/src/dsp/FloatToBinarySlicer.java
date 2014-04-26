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
import buffer.FloatCircularAveragingBuffer;

public class FloatToBinarySlicer implements Listener<Float>
											
{
	private FloatCircularAveragingBuffer mAveragingBuffer;
	private Listener<Boolean> mListener;

	/**
	 * Provides binary slicing of the stream of floats, using a circular buffer
	 * to calculate a running average, then basing the slice decision based
	 * on the oldest sample leaving the circular buffer, compared to the running
	 * average.
	 * 
	 * Designed to operate with a DC bias inherent in the sample stream.
	 * 
	 * Tracks a long average and a short average to compensate for drifting 
	 * transmitters.  
	 * 
	 * The long buffer compensates for long strings of ones or 
	 * zeros.  Set the long buffer size to a few baud periods longer than the
	 * longest anticipated string of like symbols.
	 * 
	 */
	public FloatToBinarySlicer( int bufferSize, int sampleCountForAveraging )
	{
		mAveragingBuffer = new FloatCircularAveragingBuffer( bufferSize, 
													sampleCountForAveraging );
	}

	@Override
    public void receive( Float newestSample )
    {
		float oldestSample = mAveragingBuffer.get( newestSample );
		
		//Dispatch the decision
		mListener.receive( oldestSample > mAveragingBuffer.getAverage()  );
    }

    public void setListener( Listener<Boolean> listener )
    {
		mListener = listener;
    }

    public void clearListener()
    {
		mListener = null;
    }
}
