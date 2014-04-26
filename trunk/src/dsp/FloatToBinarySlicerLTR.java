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
import buffer.FloatCircularAveragingBuffer2;

/**
 * Binary slicer tuned to operate on LTR type short burst signals in a heavy 
 * DC bias environment coupled with transmitter automatic frequency correction
 * being applied as the transmission progresses.
 * 
 * Uses a pair of float buffers that calculate the average of the samples
 * contained in the buffer.
 * 
 * The leading and trailing buffers are the same length as the sync pattern.
 * 
 * The slicer uses the Leading buffer's average as the slicing reference for 
 * normal operation.  
 * 
 * Once a sync pattern is detected, the trailing buffer should have the sync 
 * pattern bauds and the leading buffer should have the first of the message 
 * bits.  At this point, ie sync detect, we switch to using the average of both 
 * buffers as the slicing reference point.
 * 
 * Once the reference counter counts down to indicate that there are sync length
 * bauds remaining of the message, and those remaining bauds should be in the
 * leading buffer, we swich to using the trailing buffer as the slicing 
 * reference point for the remainder of the message.
 * 
 * As the reference counter decrements to 0, meaning the message should be 
 * clear of both buffers, we switch back to normal operation of using the 
 * leading buffer as the slicing reference.
 * 
 * An external sync detector indicates a sync pattern match by using the 
 * syncDetected() method, which immediately switches the slicing average 
 * reference to both, and sets the counter to the expected message length.
 */
public class FloatToBinarySlicerLTR implements Listener<Float>,
												SyncDetectListener
											
{
	private enum SlicingReference
	{
		LEADING, TRAILING, BOTH;
	}

	private SlicingReference mSlicingReference = SlicingReference.LEADING;
	private FloatCircularAveragingBuffer2 mLeadingBuffer;
	private FloatCircularAveragingBuffer2 mTrailingBuffer;
	private int mReferenceCounter = 0;
	private int mSyncLength;
	private int mMessageLength;
	private Listener<Boolean> mListener;

	public FloatToBinarySlicerLTR( int syncLength, 
								   int messageLength, 
								   int samplesPerBaud )
	{
		mSyncLength = syncLength * samplesPerBaud;
		mMessageLength = messageLength * samplesPerBaud;
		
		mLeadingBuffer = new FloatCircularAveragingBuffer2( mSyncLength );
		mTrailingBuffer = new FloatCircularAveragingBuffer2( mSyncLength );
	}

	@Override
    public void receive( Float newestSample )
    {
		float middleSample = mLeadingBuffer.get( newestSample );
		mTrailingBuffer.get( middleSample );
		
		//Dispatch the decision
		if( mListener != null )
		{
			switch( mSlicingReference )
			{
				case LEADING:
					mListener.receive( middleSample > mLeadingBuffer.average() );
					break;
				case BOTH:
					mListener.receive( middleSample > 
						( ( mLeadingBuffer.average() + 
							mTrailingBuffer.average() ) / 2.0f ) );
					break;
				case TRAILING:
					mListener.receive( middleSample > mTrailingBuffer.average() );
					break;
			}
			
			mReferenceCounter--;
			
			if( mReferenceCounter <= mSyncLength && mReferenceCounter > 0 )
			{
				mSlicingReference = SlicingReference.TRAILING;
			}
			else if( mReferenceCounter <= 0 )
			{
				mSlicingReference = SlicingReference.LEADING;
				
				mReferenceCounter = 0;
			}
		}
    }

	@Override
    public void syncDetected()
    {
		mSlicingReference = SlicingReference.BOTH;
		mReferenceCounter = mMessageLength;
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
