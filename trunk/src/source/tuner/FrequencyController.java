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
package source.tuner;

import java.util.ArrayList;

import source.SourceException;

public class FrequencyController
{
	private Tunable mTunable;
	private int mFrequency = 101100000;
	private int mTunedFrequency = 101100000;
	private int mMinimumFrequency;
	private int mMaximumFrequency;
	private double mFrequencyCorrection = 0d;
	private int mBandwidth = 0;
	
	private ArrayList<FrequencyChangeListener> mListeners =
								new ArrayList<FrequencyChangeListener>();
	
	public FrequencyController( Tunable tunable,
								int minFrequency,
								int maxFrequency,
								double frequencyCorrection ) throws SourceException
	{
		mTunable = tunable;
		mMinimumFrequency = minFrequency;
		mMaximumFrequency = maxFrequency;
		mFrequencyCorrection = frequencyCorrection;
	}

	/**
	 * Get bandwidth/sample rate in hertz
	 */
	public int getBandwidth()
	{
		return mBandwidth;
	}

	/**
	 * Set bandwidth/sample rate in hertz
	 */
	public void setBandwidth( int bandwidth )
	{
		mBandwidth = bandwidth;
		
		broadcastFrequencyChange();
	}

	/**
	 * Get frequency in hertz
	 */
	public int getFrequency()
	{
		return mFrequency;
	}

	/**
	 * Set frequency
	 * @param frequency in hertz
	 * @throws SourceException if tunable doesn't support tuning a corrected
	 * version of the requested frequency
	 */
	public void setFrequency( int frequency ) throws SourceException
	{
		setFrequency( frequency, true );
	}

	/**
	 * Set frequency with optional broadcast of frequency change event.  This
	 * method supports changing the frequency correction value without
	 * broadcasting a frequency change event.
	 */
	private void setFrequency( int frequency, boolean broadcastChange ) 
													throws SourceException
	{
		int tunedFrequency = getTunedFrequency( frequency );
		
		if( tunedFrequency < mMinimumFrequency || 
			tunedFrequency > mMaximumFrequency )
		{
			throw new SourceException( "Cannot tune frequency ( requested: " + 
					frequency + " actual:" + tunedFrequency + "]" ); 
		}
		
		mFrequency = frequency;
		mTunedFrequency = tunedFrequency;

		if( mTunable != null )
		{
			mTunable.setTunedFrequency( mTunedFrequency );
		}

		/* Broadcast to all listeners that the frequency has changed */
		if( broadcastChange )
		{
			broadcastFrequencyChange();
		}
	}
	
	public int getTunedFrequency()
	{
		return mTunedFrequency;
	}
	
	public int getMinimumFrequency()
	{
		return mMinimumFrequency;
	}
	
	public int getMaximumFrequency()
	{
		return mMaximumFrequency;
	}
	
    /**
     * Calculate the tuned frequency by adding frequency correction to the
     * corrected frequency.
     * @param correctedFrequency
     */
    private int getTunedFrequency( int correctedFrequency )
    {
		return (int)( (double)correctedFrequency / 
				( 1.0 + ( mFrequencyCorrection / 1000000.0 ) ) );
    }

    /**
     * Calculate the corrected frequency by subtracting frequency correction 
     * from the tuned frequency.
     * @param tunedFrequency
     */
    private int getCorrectedFrequency( int tunedFrequency )
    {
		return (int)( (double)tunedFrequency / 
				( 1.0 - ( mFrequencyCorrection / 1000000.0 ) ) );
    }
    
    public double getFrequencyCorrection()
    {
    	return mFrequencyCorrection;
    }
    
    public void setFrequencyCorrection( double correction ) 
    												throws SourceException
    {
    	mFrequencyCorrection = correction;

    	if( mFrequency > 0 )
    	{
        	/* Reset the frequency, but don't broadcast a change */
        	setFrequency( mFrequency, false );
    	}
    }
	
	/**
	 * Adds listener to receive frequency change events
	 */
	public void addListener( FrequencyChangeListener listener )
	{
		if( !mListeners.contains( listener ) )
		{
			mListeners.add( listener );
		}
	}

	/**
	 * Removes listener from receiving frequency change events
	 */
	public void removeListener( FrequencyChangeListener listener )
	{
		mListeners.remove( listener );
	}
	
	
    /**
     * Broadcasts a change/update to the current (uncorrected) frequency or the 
     * bandwidth/sample rate value.
     */
    protected void broadcastFrequencyChange()
    {
    	for( FrequencyChangeListener listener: mListeners )
    	{
    		listener.frequencyChanged( mFrequency, mBandwidth );
    	}
    }
	
	public interface Tunable
	{
		/**
		 * Gets the tuned frequency of the device
		 */
		public int getTunedFrequency() throws SourceException;

		/**
		 * Sets the tuned frequency of the device
		 */
		public void setTunedFrequency( int frequency ) throws SourceException;

		/**
		 * Gets the current bandwidth setting of the device
		 */
		public int getCurrentSampleRate() throws SourceException;
	}
}
