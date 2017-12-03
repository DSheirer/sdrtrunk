/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2017 Dennis Sheirer
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.IComplexBufferProvider;
import source.ISourceEventProcessor;
import source.SourceEvent;
import source.SourceEvent.Event;
import source.SourceException;
import source.tuner.channel.TunerChannel;
import source.tuner.channel.TunerChannelSource;
import source.tuner.configuration.TunerConfiguration;
import source.tuner.frequency.FrequencyController;
import source.tuner.frequency.FrequencyController.Tunable;

import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RejectedExecutionException;

public abstract class TunerController implements Tunable, ISourceEventProcessor,
    IComplexBufferProvider, Listener<ComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(TunerController.class);
    protected Broadcaster<ComplexBuffer> mSampleBroadcaster = new Broadcaster<>();

    /* List of currently tuned channels being served to demod channels */
    private SortedSet<TunerChannel> mTunedChannels = new ConcurrentSkipListSet<>();
    protected FrequencyController mFrequencyController;
    private int mMiddleUnusable;
    private double mUsableBandwidthPercentage;

    /**
     * Abstract tuner controller class.  The tuner controller manages frequency bandwidth and currently tuned channels
     * that are being fed samples from the tuner.
     *
     * @param minimumFrequency minimum uncorrected tunable frequency
     * @param maximumFrequency maximum uncorrected tunable frequency
     * @param middleUnusable is the +/- value center DC spike to avoid for channels
     * @param usableBandwidth percentage of usable bandwidth relative to space at the extreme ends of the spectrum
     * @throws SourceException - for any issues related to constructing the
     *                         class, tuning a frequency, or setting the bandwidth
     */
    public TunerController(long minimumFrequency, long maximumFrequency, int middleUnusable, double usableBandwidth)
    {
        mFrequencyController = new FrequencyController(this, minimumFrequency, maximumFrequency, 0.0d);
        mMiddleUnusable = middleUnusable;
        mUsableBandwidthPercentage = usableBandwidth;
    }

    /**
     * Applies the settings in the tuner configuration
     */
    public abstract void apply(TunerConfiguration config) throws SourceException;

    /**
     * Responds to requests to set the frequency
     */
    @Override
	public void process(SourceEvent event ) throws SourceException
	{
    	if( event.getEvent() == Event.REQUEST_FREQUENCY_CHANGE )
    	{
			setFrequency( event.getValue().longValue() );
    	}
	}

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
    public void setFrequency(long frequency) throws SourceException
    {
        mFrequencyController.setFrequency(frequency);
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

    @Override
    public boolean canTune(long frequency)
    {
        return mFrequencyController.canTune(frequency);
    }

    public int getSampleRate()
    {
        return mFrequencyController.getSampleRate();
    }

    public double getFrequencyCorrection()
    {
        return mFrequencyController.getFrequencyCorrection();
    }

    public void setFrequencyCorrection(double correction) throws SourceException
    {
        mFrequencyController.setFrequencyCorrection(correction);
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
        return mFrequencyController.getFrequency() - (getUsableBandwidth() / 2);
    }

    private long getMaxTunedFrequency() throws SourceException
    {
        return mFrequencyController.getFrequency() + (getUsableBandwidth() / 2);
    }

    /**
     * Indicates if channel along with all of the other currently sourced
     * channels can fit within the tunable bandwidth.
     */
    private boolean canTune(TunerChannel channel)
    {
        //Make sure we're within the tunable frequency range of this tuner
        if(getMinFrequency() < channel.getMinFrequency() && getMaxFrequency() > channel.getMaxFrequency())
        {
            //If this is the first lock, then we're good
            if(mTunedChannels.isEmpty())
            {
                return true;
            }
            else
            {
                int usableBandwidth = getUsableBandwidth();
                long minLockedFrequency = mTunedChannels.first().getMinFrequency();
                long maxLockedFrequency = mTunedChannels.last().getMaxFrequency();

                //Requested channel is within current locked channel frequency range
                if(minLockedFrequency <= channel.getMinFrequency() && channel.getMaxFrequency() <= maxLockedFrequency)
                {
                    return true;
                }

                //Requested channel is higher than min locked frequency
                if(channel.getMaxFrequency() > minLockedFrequency &&
                    channel.getMaxFrequency() - minLockedFrequency <= usableBandwidth)
                {
                    return true;
                }

                //Requested channel is lower than the max locked frequency
                if(channel.getMinFrequency() <= maxLockedFrequency &&
                    maxLockedFrequency - channel.getMinFrequency() <= usableBandwidth)
                {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Constructs a digital drop channel (DDC) as a tuner channel from from the tuner, or returns null if the channel
     * cannot be sourced from the tuner.
     *
     * This controller can't provide a channel if the channel, along with the current set of sourced channels, can't be
     * accomodated within the current tuner bandwidth, or if a center frequency cannot be calculated that will ensure
     * all of the channels can be accomodated and any defined central DC spike avoided.
     *
     * @param tuner to source the channel from
     * @param channel with defined center frequency and bandwidth
     * @return fully constructed tuner channel or null
     * @throws RejectedExecutionException if the decimation processor has an error
     */
    public TunerChannelSource getChannel(Tuner tuner, TunerChannel channel) throws RejectedExecutionException
    {
        TunerChannelSource source = null;

        if(canTune(channel))
        {
            try
            {
                mTunedChannels.add(channel);

                if(requiresLOUpdate(channel))
                {
                    updateLOFrequency();
                }

                source = new TunerChannelSource(tuner, channel);
            }
            catch(SourceException se)
            {
                mTunedChannels.remove(channel);
                source = null;
            }
        }

        return source;
    }

    public int getChannelCount()
    {
        return mTunedChannels.size();
    }

    /**
     * Indicates if the tuner's LO frequency must be updated in order to accommodate the tuner channel
     */
    private boolean requiresLOUpdate(TunerChannel channel) throws SourceException
    {
        boolean isBelow = channel.getMinFrequency() < getMinTunedFrequency();
        boolean isAbove = channel.getMaxFrequency() > getMaxTunedFrequency();
        boolean overlapsCenter = mMiddleUnusable > 0 &&
            channel.overlaps(getFrequency() - mMiddleUnusable, getFrequency() + mMiddleUnusable);

        return isAbove || isBelow || overlapsCenter;
    }

    /**
     * Releases the currently sourced tuner channel from this tuner and shuts down the tuner if no other sources exist.
     */
    public void releaseChannel(TunerChannelSource tunerChannelSource)
    {
        if(tunerChannelSource != null)
        {
            mTunedChannels.remove(tunerChannelSource.getTunerChannel());
        }
    }

    /**
     * Sets the Local Oscillator frequency to accomodate the current set of tuned channels.
     *
     * If there is only a single tuned channel, it is placed immediately to the right of the usable bandwidth right
     * of the central DC spike.
     *
     * Otherwise, it places the highest channel frequency at the upper end of the tuner bandwidth, and then iteratively
     * moves the center frequency higher until all channels fit within the bandwidth and none of the channels overlap
     * any defined central DC spike unusable region.  If a center tune frequency cannot be calculated, throw an
     * exception so that the most recently added channel can be removed.
     *
     * Note: the tuned frequency is not changed until a legitimate new frequency can be calculated.  If an exception
     * is thrown, the current frequency is retained, so that the recently added channel can be removed and all other
     * channels can continue as previously arranged.
     *
     * @throws SourceException if the set of tuner channels, including a recently added channel, cannot be tuned
     *                         within the current tuner bandwidth and any central DC spike unusable region.
     */
    private void updateLOFrequency() throws SourceException
    {
        long candidateFrequency;

        boolean isValidCandidateFrequency = true;

        //If there is only 1 channel set the center frequency so that the channel is positioned to the right of center
        if(mTunedChannels.size() == 1)
        {
            candidateFrequency = mTunedChannels.first().getMinFrequency() - mMiddleUnusable + 1;
        }
        else
        {
            long minLockedFrequency = mTunedChannels.first().getMinFrequency();
            long maxLockedFrequency = mTunedChannels.last().getMaxFrequency();

            //Start by placing the highest frequency channel at the high end of the spectrum
            candidateFrequency = maxLockedFrequency - (getUsableBandwidth() / 2);

            //Iterate the channels and make sure that none of them overlap the center DC spike buffer, if one exists
            if(mMiddleUnusable > 0)
            {
                boolean processingRequired = true;

                while(isValidCandidateFrequency && processingRequired)
                {
                    processingRequired = false;

                    long minAvoid = candidateFrequency - mMiddleUnusable;
                    long maxAvoid = candidateFrequency + mMiddleUnusable;

                    //If any of the center channel(s) overlap the central DC spike avoid area, we'll iteratively
                    //increase the tuned frequency causing the set of channels to move left in the tuned bandwidth until
                    //we either find a good center tune frequency, or we walk the lowest frequency channel out of the
                    //minimum tuned range, in which case we'll throw an exception indicating we don't have a solution.
                    for(TunerChannel channel : mTunedChannels)
                    {
                        if(channel.overlaps(minAvoid, maxAvoid))
                        {
                            //Calculate a tuned frequency adjustment that places this overlapping channel just to the
                            //left of the central DC spike avoid zone
                            long adjustment = channel.getMaxFrequency() - minAvoid + 1;

                            //If the candidate frequency doesn't push the lowest channel out of bounds, make adjustment
                            if(candidateFrequency + adjustment - (getUsableBandwidth() / 2) <= minLockedFrequency)
                            {
                                candidateFrequency += adjustment;
                                processingRequired = true;
                            }
                            //Otherwise, punt and indicate that we can't find a center frequency or add the channel
                            else
                            {
                                isValidCandidateFrequency = false;
                            }

                            //break out of the for/each loop, so that we can start over again with all of the channels
                            break;
                        }
                    }
                }
            }
        }

        if(isValidCandidateFrequency)
        {
            mFrequencyController.setFrequency(candidateFrequency);
        }
        else
        {
            throw new SourceException("Couldn't calculate viable center frequency from set of tuner channels");
        }
    }

    /**
     * Usable bandwidth - total bandwidth minus the unusable space at either end of the spectrum.
     */
    private int getUsableBandwidth()
    {
        return (int)(getBandwidth() * mUsableBandwidthPercentage);
    }

	/**
	 * Sets the listener to be notified any time that the tuner changes frequency
	 * or bandwidth/sample rate.
	 * 
	 * Note: this is normally used by the Tuner.  Any additional listeners can
	 * be registered on the tuner.
	 */
    public void addListener( ISourceEventProcessor processor )
    {
        mFrequencyController.addListener(processor);
    }

    /**
     * Removes the frequency change listener
     */
    public void removeListener( ISourceEventProcessor processor )
    {
        mFrequencyController.removeFrequencyChangeProcessor(processor);
    }

    /**
     * Adds the listener to receive complex buffer samples
     */
    @Override
    public void addComplexBufferListener(Listener<ComplexBuffer> listener)
    {
        mSampleBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving complex buffer samples
     */
    @Override
    public void removeComplexBufferListener(Listener<ComplexBuffer> listener)
    {
        mSampleBroadcaster.removeListener(listener);
    }

    /**
     * Indicates if there are any complex buffer listeners registered on this controller
     */
    @Override
    public boolean hasComplexBufferListeners()
    {
        return mSampleBroadcaster.hasListeners();
    }

    /**
     * Broadcasts the buffer to any registered listeners
     */
    protected void broadcast(ComplexBuffer complexBuffer)
    {
        mSampleBroadcaster.broadcast(complexBuffer);
    }

    /**
     * Implements the Listener<T> interface to receive and distribute complex buffers from subclass implementations
     */
    @Override
    public void receive(ComplexBuffer complexBuffer)
    {
        broadcast(complexBuffer);
    }
}
