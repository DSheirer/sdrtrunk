/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package source.tuner.channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import source.Source;
import source.SourceEvent;
import source.SourceException;
import source.tuner.Tuner;
import source.tuner.channel.cic.CICTunerChannelSource;

import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RejectedExecutionException;

public abstract class ChannelManager implements Listener<SourceEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelManager.class);

    private FrequencyController mFrequencyController;
    private SortedSet<TunerChannel> mTunedChannels = new ConcurrentSkipListSet<>();

    /**
     * Channel manager provides access to Digital Drop Channel (DDC) tuner channel sources and automatic updating of
     * a frequency controller to ensure that the center tuned frequency is correct for the provided DDC channels.
     *
     * @param frequencyController with a center tuned frequency that can be adjusted
     */
    public ChannelManager(FrequencyController frequencyController)
    {
        mFrequencyController = frequencyController;
    }

    protected FrequencyController getFrequencyController()
    {
        return mFrequencyController;
    }

    /**
     * Creates a tuner channel source for the tuner channel if possible.
     *
     * @param tunerChannel to source
     * @return tuner channel source or null if the tuner channel cannot be sourced by this channel manager
     */
    public TunerChannelSource getSource(TunerChannel tunerChannel)
    {
        TunerChannelSource tunerChannelSource = null;

        if(canTune(tunerChannel))
        {
            //Add the channel so that we can calculate the new center frequency
            mTunedChannels.add(tunerChannel);

            long centerFrequency = calculateCenterFrequency();

            if(centerFrequency != getFrequencyController().getFrequency())
            {
                try
                {
                    getFrequencyController().setLockedFrequency(centerFrequency);
                    tunerChannelSource = getTunerChannelSource(tunerChannel);
                }
                catch(SourceException se)
                {
                    mLog.error("Error setting tuner frequency while creating new tuner channel source for channel: " +
                        tunerChannel, se);
                }
            }
        }

        //If we're not successful, remove the tuner channel from the sorted set
        if(tunerChannelSource == null && mTunedChannels.contains(tunerChannel))
        {
            mTunedChannels.remove(tunerChannel);
        }

        return tunerChannelSource;
    }

    /**
     * Releases the source and performs any cleanup.
     * @param source to release that was previously provided by this channel manager
     */
    public void releaseSource(TunerChannelSource source)
    {
        mTunedChannels.remove(source.getTunerChannel());
    }

    /**
     * Determine the center frequency to use for the current set of tuner channels.
     */
    protected abstract long calculateCenterFrequency();

    /**
     * Constructs the actual tuner channel source for the tuner channel.  This method assumes that the tuner channel
     * can be tuned and that the tuner is correctly center tuned to source the channel.
     * @param tunerChannel to source
     * @return tuner channel source or null if there are any errors in creating the tuner channel source
     */
    protected abstract TunerChannelSource getTunerChannelSource(TunerChannel tunerChannel);

    /**
     * Indicates if channel along with all of the other currently sourced channels can fit within the tunable bandwidth.
     */
    private boolean canTune(TunerChannel channel)
    {
        //Make sure we're within the tunable frequency range of this tuner
        if(getFrequencyController().getMinimumFrequency() < channel.getMinFrequency() &&
            getFrequencyController().getMaximumFrequency() > channel.getMaxFrequency())
        {
            //If this is the first lock, then we're good
            if(mTunedChannels.isEmpty())
            {
                return true;
            }
            else
            {
                int usableBandwidth = getFrequencyController().getUsableBandwidth();
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

                source = new CICTunerChannelSource(tuner, channel);
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
        int usableBandwidth = getFrequencyController().getUsableBandwidth();

        long minimumTunedFrequency = getFrequencyController().getFrequency() - (usableBandwidth / 2);
        boolean isBelow = channel.getMinFrequency() < minimumTunedFrequency;

        long maximumTunedFrequency = getFrequencyController().getFrequency() + (usableBandwidth / 2);
        boolean isAbove = channel.getMaxFrequency() > maximumTunedFrequency;

        long centerBlackoutBandwidth = getFrequencyController().getCenterBlackoutBandwidth();
        boolean overlapsCenter = centerBlackoutBandwidth > 0 &&
            channel.overlaps(getFrequencyController().getFrequency() - centerBlackoutBandwidth,
                getFrequencyController().getFrequency() + centerBlackoutBandwidth);

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
            candidateFrequency = mTunedChannels.first().getMinFrequency() -
                getFrequencyController().getCenterBlackoutBandwidth() + 1;
        }
        else
        {
            long minLockedFrequency = mTunedChannels.first().getMinFrequency();
            long maxLockedFrequency = mTunedChannels.last().getMaxFrequency();

            //Start by placing the highest frequency channel at the high end of the spectrum
            candidateFrequency = maxLockedFrequency - (getFrequencyController().getUsableBandwidth() / 2);

            //Iterate the channels and make sure that none of them overlap the center DC spike buffer, if one exists
            if(getFrequencyController().getCenterBlackoutBandwidth() > 0)
            {
                boolean processingRequired = true;

                while(isValidCandidateFrequency && processingRequired)
                {
                    processingRequired = false;

                    long centerBlackout = getFrequencyController().getCenterBlackoutBandwidth();

                    long minAvoid = candidateFrequency - centerBlackout;
                    long maxAvoid = candidateFrequency + centerBlackout;

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
                            if(candidateFrequency + adjustment -
                                (getFrequencyController().getUsableBandwidth() / 2) <= minLockedFrequency)
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
            mFrequencyController.setLockedFrequency(candidateFrequency);
        }
        else
        {
            throw new SourceException("Couldn't calculate viable center frequency from set of tuner channels");
        }
    }

}
