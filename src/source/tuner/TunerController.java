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
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.RejectedExecutionException;

import log.Log;
import source.SourceException;
import source.tuner.FrequencyController.Tunable;
import controller.ThreadPoolManager;

public abstract class TunerController implements Tunable
{
	/* List of currently tuned channels being served to demod channels */
	protected ArrayList<TunerChannel> mTunedChannels = 
					new ArrayList<TunerChannel>();
	
	protected FrequencyController mFrequencyController;
	
	/**
	 * Abstract tuner controller class.  The tuner controller manages frequency
	 * bandwidth and currently tuned channels that are being fed samples from
	 * the tuner.
	 * 
	 * @param minimumFrequency - minimum uncorrected tunable frequency
	 * @param maximumFrequency - maximum uncorrected tunable frequency
	 * @throws SourceException - for any issues related to constructing the 
	 * class, tuning a frequency, or setting the bandwidth
	 */
	public TunerController( long minimumFrequency, long maximumFrequency ) 
			throws SourceException
	{
		mFrequencyController = new FrequencyController( this, 
														minimumFrequency, 
														maximumFrequency, 
														0.0d );
	}
	
	/**
	 * Applies the settings in the tuner configuration
	 */
	public abstract void apply( TunerConfiguration config ) 
								throws SourceException;
	
    public int getBandwidth()
    {
    	return mFrequencyController.getBandwidth();
    }
    
    /**
     * Sets the center frequency of the local oscillator.
     * 
     * @param frequency in hertz
     * @throws SourceException - if the tuner has any issues
     */
	public void setFrequency( long frequency ) throws SourceException
	{
		mFrequencyController.setFrequency( frequency );
	}

	/**
	 * Gets the center frequency of the local oscillator
	 * 
	 * @return frequency in hertz
	 */
	public long getFrequency()	
	{
		return mFrequencyController.getFrequency();
	}

	public double getFrequencyCorrection()
	{
		return mFrequencyController.getFrequencyCorrection();
	}
	
	public void setFrequencyCorrection( double correction ) throws SourceException
	{
		mFrequencyController.setFrequencyCorrection( correction );
	}
	
	public long getMinFrequency()
	{
		return mFrequencyController.getMinimumFrequency();
	}

	public long getMaxFrequency()
	{
		return mFrequencyController.getMaximumFrequency();
	}

	/**
	 * Indicates if the tuner can accomodate this new channel frequency and
	 * bandwidth, along with all of the existing tuned channels currently in 
	 * place.
	 */
	public boolean canTuneChannel( TunerChannel channel )
	{
		//Make sure we're within the tunable frequency range of this tuner
		if( getMinFrequency() < channel.getMinFrequency() &&
			getMaxFrequency() > channel.getMaxFrequency() )
		{
			//If this is the first lock, then we're good
			if( mTunedChannels.isEmpty() )
			{
				return true;
			}
			else
			{
				//Sort the existing locks and get the min/max locked frequencies
				Collections.sort( mTunedChannels );

				long minLockedFrequency = mTunedChannels.get( 0 ).getMinFrequency();
				long maxLockedFrequency = mTunedChannels
						.get( mTunedChannels.size() - 1 ).getMaxFrequency();

				//Requested channel is higher than min locked frequency
				if( minLockedFrequency <= channel.getMinFrequency() &&
					( channel.getMaxFrequency() - minLockedFrequency ) <= getBandwidth()  )
				{
					return true;
				}
				//Requested channel is lower than the max locked frequency
				else if( channel.getMaxFrequency() <= maxLockedFrequency && 
					( maxLockedFrequency - channel.getMinFrequency() ) <= getBandwidth() )
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
							Tuner tuner, TunerChannel tunerChannel )
									throws RejectedExecutionException,
										   SourceException
	{
		TunerChannelSource source = null;
		
		if( canTuneChannel( tunerChannel ) )
		{
			mTunedChannels.add( tunerChannel );
			
			updateLOFrequency();
			
			source = new TunerChannelSource( threadPoolManager, tuner, tunerChannel );
		}

		return source;
	}
	
	public void releaseChannel( TunerChannelSource tunerChannelSource )
	{
		if( tunerChannelSource != null )
		{
			mTunedChannels.remove( tunerChannelSource.getTunerChannel() );
		}
		else
		{
			Log.error( "Tuner Controller - couldn't find the tuned channel "
					+ "to release it" );
		}
	}
	
	/**
	 * Sets the Local Oscillator frequency to the middle of the currently
	 * locked frequency range, adjusting the left/right of a channel, if the
	 * middle falls within the locked range of any of the channels.  Note: this
	 * will fail to set the correct frequency if multiple overlapping channel
	 * bandwidths are locked in the exact middle of the total locked channel 
	 * frequency range.
	 *  
	 * @throws SourceException
	 */
	public void updateLOFrequency() throws SourceException
	{
		Collections.sort( mTunedChannels );

		long minLockedFrequency = mTunedChannels.get( 0 ).getMinFrequency();
		long maxLockedFrequency = mTunedChannels
				.get( mTunedChannels.size() - 1 ).getMaxFrequency();

		long middle = minLockedFrequency + 
				( ( maxLockedFrequency - minLockedFrequency ) / 2 );
		long middleMin = middle - 10000;
		long middleMax = middle + 10000;
		
		Iterator<TunerChannel> it = mTunedChannels.iterator();
		
		while( it.hasNext() )
		{
			TunerChannel lock = it.next();

			//If a locked channel overlaps our middle frequency lockout, adjust to
			//the left or the right of that channel, whichever is closer
			if( lock.getMinFrequency() < middleMax && middleMin < lock.getMaxFrequency() )
			{
				if( middleMax - lock.getMinFrequency() < lock.getMaxFrequency() - middleMin )
				{
					middle = lock.getMinFrequency() - 10000;
				}
				else
				{
					middle = lock.getMaxFrequency() + 10000;
				}
			}
		}
		
		mFrequencyController.setFrequency( middle );
	}

	/**
	 * Sets the listener to be notified any time that the tuner changes frequency
	 * or bandwidth/sample rate.
	 * 
	 * Note: this is normally used by the Tuner.  Any additional listeners can
	 * be registered on the tuner.
	 */
    public void addListener( FrequencyChangeListener listener )
    {
    	mFrequencyController.addListener( listener );
    }

    /**
     * Removes the frequency change listener
     */
    public void removeListener( FrequencyChangeListener listener )
    {
    	mFrequencyController.removeListener( listener );
    }
}
