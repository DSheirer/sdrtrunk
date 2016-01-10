/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
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

import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import source.SourceException;
import source.tuner.frequency.FrequencyController;
import source.tuner.frequency.FrequencyController.Tunable;
import source.tuner.frequency.IFrequencyChangeProcessor;
import source.tuner.frequency.IFrequencyChangeListener;
import controller.ThreadPoolManager;

public abstract class TunerController implements Tunable
{
	private final static Logger mLog = 
			LoggerFactory.getLogger( TunerController.class );

	/* List of currently tuned channels being served to demod channels */
<<<<<<< Upstream, based on origin/master
	private SortedSet<TunerChannel> mTunedChannels = new ConcurrentSkipListSet<>();
=======
	protected ArrayList<TunerChannel> mTunedChannels = new ArrayList<>();
>>>>>>> 20e3bee Work in progress. -Started work on new channel editor channel viewer/controller -Removed a number of unused buffer package classes -Moved automatic frequency control to be a module and removed special handling -Added IUnfilteredRealBufferListener & Provider interfaces -Completely removed the ResourceManager class
	protected FrequencyController mFrequencyController;
	private int mMiddleUnusable;
	private double mUsableBandwidthPercentage;
	
	/**
	 * Abstract tuner controller class.  The tuner controller manages frequency
	 * bandwidth and currently tuned channels that are being fed samples from
	 * the tuner.
	 * 
	 * @param minimumFrequency minimum uncorrected tunable frequency
	 * @param maximumFrequency maximum uncorrected tunable frequency
	 * @param middleUnusable is the +/- value center DC spike to avoid for channels
	 * @param extantUnusable is the unusable space at the extreme ends of the spectrum
	 * 
	 * @throws SourceException - for any issues related to constructing the 
	 * class, tuning a frequency, or setting the bandwidth
	 */
	public TunerController( long minimumFrequency, 
							long maximumFrequency,
							int middleUnusable,
							double usableBandwidthPercentage ) 
	{
		mFrequencyController = new FrequencyController( this, 
														minimumFrequency, 
														maximumFrequency, 
														0.0d );
		mMiddleUnusable = middleUnusable;
		mUsableBandwidthPercentage = usableBandwidthPercentage;
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
	
	private long getMinTunedFrequency() throws SourceException
	{
		return mFrequencyController.getFrequency() - ( getUsableBandwidth() / 2 );
	}

	private long getMaxTunedFrequency() throws SourceException
	{
		return mFrequencyController.getFrequency() + ( getUsableBandwidth() / 2 );
	}

	/**
	 * Indicates if channel along with all of the other currently sourced 
	 * channels can fit within the tunable bandwidth.
	 */
	private boolean canTune( TunerChannel channel )
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
				int usableBandwidth = getUsableBandwidth();
				long minLockedFrequency = mTunedChannels.first().getMinFrequency();
				long maxLockedFrequency = mTunedChannels.last().getMaxFrequency();

				//Requested channel is within current locked channel frequency range
				if( minLockedFrequency <= channel.getMinFrequency() &&
					channel.getMaxFrequency() <= maxLockedFrequency )
				{
					return true;
				}
				
				//Requested channel is higher than min locked frequency
				if( channel.getMaxFrequency() > minLockedFrequency &&
					channel.getMaxFrequency() - minLockedFrequency <= usableBandwidth )
				{
					return true;
				}
				//Requested channel is lower than the max locked frequency
				if( channel.getMinFrequency() <= maxLockedFrequency && 
					maxLockedFrequency - channel.getMinFrequency() <= usableBandwidth )
				{
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * Constructs a digital drop channel (DDC) as a tuner channel from from 
	 * the tuner, or returns null if the channel cannot be sourced from the 
	 * tuner.  
	 * 
	 * This controller can't provide a channel if the channel, along with the
	 * current set of sourced channels, can't be accomodated within the current
	 * tuner bandwidth, or if a center frequency cannot be calculated that will
	 * ensure all of the channels can be accomodated and any defined central DC 
	 * spike avoided.
	 * 
	 * @param threadPoolManager for the channel to use for runnables
	 * @param tuner to source the channel from
	 * @param channel with defined center frequency and bandwidth
	 * 
	 * @return fully constructed tuner channel or null
	 * 
	 * @throws RejectedExecutionException if the decimation processor has an error
	 */
	public TunerChannelSource getChannel( ThreadPoolManager threadPoolManager,
		Tuner tuner, TunerChannel channel ) throws RejectedExecutionException
	{
		TunerChannelSource source = null;
		
		if( canTune( channel ) )
		{
			try
			{
				mTunedChannels.add( channel );

				if( requiresLOUpdate( channel ) )
				{
					updateLOFrequency();
				}
				
				source = new TunerChannelSource( threadPoolManager, 
						tuner, channel );
			}
			catch( SourceException se )
			{
				mTunedChannels.remove( channel );
				source = null;
			}
		}

		return source;
	}

	/**
	 * Indicates if the tuner's LO frequency must be updated in order to 
	 * accomodate the tuner channel
	 */
	private boolean requiresLOUpdate( TunerChannel channel ) throws SourceException
	{
		return !( getMinTunedFrequency() <= channel.getMinFrequency() &&
				  channel.getMaxFrequency() <= getMaxTunedFrequency() &&
				  ( ( mMiddleUnusable == 0 ) ||
				    ( !channel.overlaps( getTunedFrequency() - mMiddleUnusable, 
				    					 getTunedFrequency() + mMiddleUnusable ) ) ) );
	}

	/**
	 * Releases the currently sourced tuner channel from this tuner and shuts
	 * down the tuner if no other sources exist.
	 */
	public void releaseChannel( TunerChannelSource tunerChannelSource )
	{
		if( tunerChannelSource != null )
		{
			mTunedChannels.remove( tunerChannelSource.getTunerChannel() );
		}
	}
	
	/**
	 * Sets the Local Oscillator frequency accomodate the current set of tuned
	 * channels.  
	 * 
	 * If there is only a single tuned channel, it is placed immediately to the
	 * right of the usable bandwidth right of the central DC spike.
	 * 
	 * Otherwise, it places the highest channel frequency at the upper end of
	 * the tuner bandwidth, and then iteratively moves the center frequency 
	 * higher until all channels fit within the bandwidth and none of the
	 * channels overlap any defined central DC spike unusable region.  If a 
	 * center tune frequency cannot be calculated, throw an exception so that 
	 * the most recently added channel can be removed.
	 * 
	 * Note: the tuned frequency is not changed until a legitimate new frequency
	 * can be calculated.  If an exception is thrown, the current frequency is
	 * retained, so that the recently added channel can be removed and all other
	 * channels can continue as previously arranged.
	 *  
	 * @throws SourceException if the set of tuner channels, including a recently
	 * added channel, cannot be tuned within the current tuner bandwidth and any
	 * central DC spike unusable region.
	 */
	private void updateLOFrequency() throws SourceException
	{
		long frequency = getFrequency();
		
		boolean frequencyValid = true;

		//If there is only 1 channel, position it to the right of center
		if( mTunedChannels.size() == 1 )
		{
			frequency = mTunedChannels.first().getMinFrequency() - mMiddleUnusable;
		}
		else
		{
			long minLockedFrequency = mTunedChannels.first().getMinFrequency();
			long maxLockedFrequency = mTunedChannels.last().getMaxFrequency();

			//Start by placing the highest frequency channel at the high end of
			//the spectrum
			frequency = maxLockedFrequency - ( getUsableBandwidth() / 2 );

			//Iterate the channels and make sure that none of them overlap the
			//center DC spike buffer, if one exists
			if( mMiddleUnusable > 0 )
			{
				boolean processingRequired = true;
				
				while( frequencyValid && processingRequired )
				{
					processingRequired = false;
					
					long minAvoid = frequency - mMiddleUnusable;
					long maxAvoid = frequency + mMiddleUnusable;

					for( TunerChannel channel: mTunedChannels )
					{
						if( channel.overlaps( minAvoid, maxAvoid ) )
						{
							//Can we move the frequency lower so that the
							//channel sits to the right of center?
							long adjustment = channel.getMaxFrequency() - minAvoid;

							if( frequency + adjustment - 
								( getUsableBandwidth() / 2 ) <= minLockedFrequency )
							{
								frequency += adjustment;
								processingRequired = true;
							}
							else
							{
								frequencyValid = false;
							}
							
							//break out of the for/each loop, so that we can 
							//start over again with all of the channels
							break;
						}
					}
				}
			}
		}

		if( frequencyValid )
		{
			mFrequencyController.setFrequency( frequency );
		}
		else
		{
			throw new SourceException( "Couldn't calculate viable center "
					+ "frequency from set of tuner channels" );
		}
	}

	/**
	 * Usable bandwidth - total bandwidth minus the unusable space at either end
	 * of the spectrum.
	 */
	private int getUsableBandwidth()
	{
		return (int)( getBandwidth() * mUsableBandwidthPercentage );
	}

	/**
	 * Sets the listener to be notified any time that the tuner changes frequency
	 * or bandwidth/sample rate.
	 * 
	 * Note: this is normally used by the Tuner.  Any additional listeners can
	 * be registered on the tuner.
	 */
    public void addListener( IFrequencyChangeProcessor processor )
    {
    	mFrequencyController.addListener( processor );
    }

    /**
     * Removes the frequency change listener
     */
    public void removeListener( IFrequencyChangeProcessor processor )
    {
    	mFrequencyController.removeFrequencyChangeProcessor( processor );
    }
}
