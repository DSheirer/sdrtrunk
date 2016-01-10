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
package source.tuner.frequency;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceException;
import source.tuner.frequency.FrequencyChangeEvent.Event;

public class FrequencyController
{
	private final static Logger mLog = LoggerFactory.getLogger( FrequencyController.class );

	private Tunable mTunable;
	private long mFrequency = 101100000;
	private long mTunedFrequency = 101100000;
	private long mMinimumFrequency;
	private long mMaximumFrequency;
	private double mFrequencyCorrection = 0d;
	private int mSampleRate = 0;
	
	private ArrayList<IFrequencyChangeProcessor> mProcessors =	new ArrayList<>();
	
	public FrequencyController( Tunable tunable,
								long minFrequency,
								long maxFrequency,
								double frequencyCorrection )
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
		return mSampleRate;
	}

	/**
	 * Set sample rate in hertz
	 */
	public void setSampleRate( int sampleRate )
	{
		mSampleRate = sampleRate;
		
		broadcastSampleRateChange();
	}

	/**
	 * Get frequency in hertz
	 */
	public long getFrequency()
	{
		return mFrequency;
	}

	/**
	 * Set frequency
	 * @param frequency in hertz
	 * @throws SourceException if tunable doesn't support tuning a corrected
	 * version of the requested frequency
	 */
	public void setFrequency( long frequency ) throws SourceException
	{
		setFrequency( frequency, true );
	}

	/**
	 * Set frequency with optional broadcast of frequency change event.  This
	 * method supports changing the frequency correction value without
	 * broadcasting a frequency change event.
	 */
	private void setFrequency( long frequency, boolean broadcastChange ) 
													throws SourceException
	{
		long tunedFrequency = getTunedFrequency( frequency );
		
		if( tunedFrequency < mMinimumFrequency || 
			tunedFrequency > mMaximumFrequency )
		{
//TODO: this should throw a frequency limit exception so that the frequency
//controller can update its display to the last tuned frequency
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
	
	public long getTunedFrequency()
	{
		return mTunedFrequency;
	}
	
	public long getMinimumFrequency()
	{
		return mMinimumFrequency;
	}
	
	public long getMaximumFrequency()
	{
		return mMaximumFrequency;
	}
	
    /**
     * Calculate the tuned frequency by adding frequency correction to the
     * corrected frequency.
     * @param correctedFrequency
     */
    private long getTunedFrequency( long correctedFrequency )
    {
		return (long)( (double)correctedFrequency / 
				( 1.0 + ( mFrequencyCorrection / 1000000.0 ) ) );
    }

    /**
     * Calculate the corrected frequency by subtracting frequency correction 
     * from the tuned frequency.
     * @param tunedFrequency
     */
    @SuppressWarnings("unused")
	private long getCorrectedFrequency( long tunedFrequency )
    {
		return (long)( (double)tunedFrequency / 
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
        	setFrequency( mFrequency, true );
    	}

    	broadcastFrequencyErrorChange();
    }
	
	/**
	 * Adds listener to receive frequency change events
	 */
//TODO: rename this to addFrequencyChangeProcessor()
	public void addListener( IFrequencyChangeProcessor processor )
	{
		if( !mProcessors.contains( processor ) )
		{
			mProcessors.add( processor );
		}
	}

	/**
	 * Removes listener from receiving frequency change events
	 */
	public void removeFrequencyChangeProcessor( IFrequencyChangeProcessor processor )
	{
		mProcessors.remove( processor );
	}
	
	
    /**
     * Broadcasts a change/update to the current (uncorrected) frequency or the 
     * bandwidth/sample rate value.
     */
    protected void broadcastFrequencyChange()
    {
    	broadcastFrequencyChangeEvent( 
    			new FrequencyChangeEvent( Event.NOTIFICATION_FREQUENCY_CHANGE, mFrequency ) );
    }
	
    /**
     * Broadcast a frequency error/correction value change
     */
    protected void broadcastFrequencyErrorChange()
    {
    	broadcastFrequencyChangeEvent( 
			new FrequencyChangeEvent( Event.NOTIFICATION_FREQUENCY_CORRECTION_CHANGE, mFrequencyCorrection ) );
    }
    
    /**
     * Broadcasts a sample rate change
     */
    protected void broadcastSampleRateChange()
    {
    	broadcastFrequencyChangeEvent( 
			new FrequencyChangeEvent( Event.NOTIFICATION_SAMPLE_RATE_CHANGE, mSampleRate ) );
    }
    
    public void broadcastFrequencyChangeEvent( FrequencyChangeEvent event )
    {
    	for( IFrequencyChangeProcessor processor: mProcessors )
    	{
    		processor.frequencyChanged( event );
    	}
    }
    
	
	public interface Tunable
	{
		/**
		 * Gets the tuned frequency of the device
		 */
		public long getTunedFrequency() throws SourceException;

		/**
		 * Sets the tuned frequency of the device
		 */
		public void setTunedFrequency( long frequency ) throws SourceException;

		/**
		 * Gets the current bandwidth setting of the device
		 */
		public int getCurrentSampleRate() throws SourceException;
	}
}
