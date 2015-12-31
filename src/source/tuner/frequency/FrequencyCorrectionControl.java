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

import source.tuner.frequency.FrequencyChangeEvent.Event;

/**
 * Frequency control for providing frequency error correction to a tuned channel
 */
public class FrequencyCorrectionControl implements FrequencyChangeListener
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( FrequencyCorrectionControl.class );
	
	protected FrequencyChangeListener mListener;
	protected FrequencyCorrectionResetListener mResetListener;
	
	protected int mChannelFrequencyCorrection = 0;
	private int mMaximumCorrection = 3000;

	/**
	 * Frequency correction controller.
	 * 
	 * @param maximum - defines the maximum +/- allowable frequency correction
	 * value
	 */
	public FrequencyCorrectionControl( int maximum )
	{
		mMaximumCorrection = maximum;
	}

	/**
	 * Cleanup
	 */
	public void dispose()
	{
		mListener = null;
		mResetListener = null;
	}

	/**
	 * Registers a listener to receive frequency correction events
	 */
	public void setFrequencyChangeListener( FrequencyChangeListener listener )
	{
		mListener = listener;
	}
	
	/**
	 * Registers a listener to receive frequency correction resets
	 */
	public void setListener( FrequencyCorrectionResetListener listener )
	{
		mResetListener = listener;
	}

	/**
	 * Listener for source frequency or frequency offset value changes to
	 * reset this frequency controller value to zero.
	 */
	@Override
	public void frequencyChanged( FrequencyChangeEvent event )
	{
		Event attribute = event.getEvent();
		
		switch( attribute )
		{
			//Direct the listener to reset when frequency correction (PPM) or 
			//sample rate changes
			case FREQUENCY_CORRECTION_CHANGE_NOTIFICATION:
			case SAMPLE_RATE_CHANGE_NOTIFICATION:
				reset();
				break;
			default:
				break;
		}
	}
	
	/**
	 * Resets frequency correction value to zero and notifies any reset 
	 * listeners
	 */
	public void reset()
	{
		if( mResetListener != null )
		{
			mResetListener.resetFrequencyCorrection();
		}
	}

	/**
	 * Sets frequency correction to the specified value and broadcasts a change
	 * request
	 */
	public void setFrequencyCorrection( int correction )
	{
		mChannelFrequencyCorrection = correction;

		/* Limit frequency correction to +/- max correction value */
		if( mChannelFrequencyCorrection > mMaximumCorrection )
		{
			mChannelFrequencyCorrection = mMaximumCorrection;
		}
		else if( mChannelFrequencyCorrection < -mMaximumCorrection )
		{
			mChannelFrequencyCorrection = -mMaximumCorrection;
		}
		
		/* Broadcast a change request */
		if( mListener != null )
		{
			mListener.frequencyChanged( new FrequencyChangeEvent( 
				Event.CHANNEL_FREQUENCY_CORRECTION_CHANGE_REQUEST, 
					mChannelFrequencyCorrection ) );
		}
	}
	
	/**
	 * Increments the frequency error correction value by the specified adjustment
	 */
	public void adjust( int adjustment )
	{
		setFrequencyCorrection( getErrorCorrection() + adjustment ); 
	}

	/**
	 * Current frequency error correction value
	 * @return
	 */
	public int getErrorCorrection()
	{
		return mChannelFrequencyCorrection;
	}

	/**
	 * Listener interface to be notified when this control is reset to a zero
	 * frequency correction value
	 */
	public interface FrequencyCorrectionResetListener
	{
		public void resetFrequencyCorrection();
	}
}
