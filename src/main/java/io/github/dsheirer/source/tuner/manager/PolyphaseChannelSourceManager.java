/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.dsp.filter.channelizer.PolyphaseChannelManager;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;

public class PolyphaseChannelSourceManager extends ChannelSourceManager
{
    private final static Logger mLog = LoggerFactory.getLogger(PolyphaseChannelSourceManager.class);
    private PolyphaseChannelManager mPolyphaseChannelManager;
    private TunerController mTunerController;

    /**
     * PolyphaseChannelSourceManager is responsible for managing the tuner's center tuned frequency and providing access to
     * polyphase tuner channel sources (ie DDCs).  This class is responsible for determining IF a requested channel can
     * be provided and then adjusting the center frequency and provisioning a DDC Polyphase Tuner channel source.
     *
     * @param tunerController with a center tuned frequency that will be managed by this instance
     */
    public PolyphaseChannelSourceManager(TunerController tunerController)
    {
        mTunerController = tunerController;

        mPolyphaseChannelManager = new PolyphaseChannelManager(tunerController);
        //Register to receive channel count change notifications for rebroadcasting
        mPolyphaseChannelManager.addSourceEventListener(this::process);
        mTunerController.addListener(mPolyphaseChannelManager);
    }

    /**
     * Indicates if the channel min/max frequencies are within the tunable frequency range of the tuner controller
     *
     * @param tunerChannel to check
     * @return true if the channel is within the frequency range of the tuner controller
     */
    private boolean isTunable(TunerChannel tunerChannel)
    {
        return mTunerController.canTune(tunerChannel.getMinFrequency()) &&
                mTunerController.canTune(tunerChannel.getMaxFrequency());
    }

    /**
     * Indicates if the list of tuner channels can be tuned by the tuner controller within the current bandwidth and
     * tuner center-blocked frequency range limitations.
     *
     * @param tunerChannels that are being evaluated
     * @return true if the set of channels fit within the bandwidth and center blocked frequency band of the tuner
     */
    private boolean canTune(SortedSet<TunerChannel> tunerChannels)
    {
        //If there is only one channel, check to ensure it fits the current bandwidth or half-bandwidth
        if(tunerChannels.size() == 1)
        {
            if(mTunerController.hasMiddleUnusableBandwidth())
            {
                return tunerChannels.first().getBandwidth() < mTunerController.getUsableHalfBandwidth();
            }
            else
            {
                return tunerChannels.first().getBandwidth() < mTunerController.getUsableBandwidth();
            }
        }
        else
        {
            //Check that the total bandwidth of the channel set is within the usable bandwidth of the tuner
            int tunerUsableBandwidth = mTunerController.getUsableBandwidth();

            long channelSetBandwidth = tunerChannels.last().getMaxFrequency() - tunerChannels.first().getMinFrequency();

            if(channelSetBandwidth <= tunerUsableBandwidth)
            {
                //If the tuner doesn't have a center DC spike blocked region, we're good
                if(!mTunerController.hasMiddleUnusableBandwidth())
                {
                    return true;
                }

                //Check to see if the channel set fits within the usable half bandwidth of the tuner
                int tunerUsableHalfBandwidth = mTunerController.getUsableHalfBandwidth();

                if(channelSetBandwidth < tunerUsableHalfBandwidth)
                {
                    return true;
                }

                //At this point we're within the tuner's total usable bandwidth, but there is a central DC spike blocked
                //region.  See if we can notionally partition the channels into left/right usable half bandwidth regions
                TunerChannel firstRightChannel = null;

                for(TunerChannel tunerChannel: tunerChannels)
                {
                    if(firstRightChannel == null)
                    {
                        long leftSideBandwidth = tunerChannel.getMaxFrequency() - tunerChannels.first().getMinFrequency();

                        if(leftSideBandwidth > tunerUsableHalfBandwidth)
                        {
                            //Ensure this first right-partition channel fits within the half bandwidth
                            if(tunerChannel.getBandwidth() > tunerUsableHalfBandwidth)
                            {
                                return false;
                            }

                            firstRightChannel = tunerChannel;
                        }
                    }
                    else
                    {
                        long rightSideBandwidth = tunerChannel.getMaxFrequency() - firstRightChannel.getMinFrequency();

                        //If the right side partition's bandwidth exceeds the half bandwidth, we're done
                        if(rightSideBandwidth > tunerUsableHalfBandwidth)
                        {
                            return false;
                        }
                    }
                }

                //All channels can be partitioned into notional left/right partition sets - we're good
                return true;
            }
        }

        return false;
    }

    /**
     * Calculates the optimal center tune frequency for the set of currently sourced tuner channels.  Note: the returned
     * frequency will only be different from the current center frequency argument if a change is required in order
     * to fit all of the sourced tuner channels, or to better optimize the channels for alignment with the current
     * polyphase channelizer channel partitioning.
     *
     * @param channels that are currently sourced by this source manager
     * @param currentCenterFrequency of the tuner.
     * @return optimal center tuned frequency for the set of currently sourced tuner channels.
     * @throws IllegalArgumentException if a center frequency cannot be determined for the set of tuner channels
     */
    private long getCenterFrequency(SortedSet<TunerChannel> channels, long currentCenterFrequency)
        throws IllegalArgumentException
    {
        if(channels.isEmpty())
        {
            return currentCenterFrequency;
        }

        long channelSetBandwidth = getChannelSetBandwidth(channels);

        if(channelSetBandwidth > mTunerController.getUsableBandwidth())
        {
            throw new IllegalArgumentException("Channel set bandwidth is greater than tuner's available bandwidth");
        }

        long bestIntegralFrequency = getIntegralFrequency(channels);

        //Strategy 1: reuse the current frequency if it's a good integral and the channels fit
        if(isIntegralSpacing(currentCenterFrequency, bestIntegralFrequency) &&
            isValidCenterFrequency(channels, currentCenterFrequency))
        {
            return currentCenterFrequency;
        }

        double usableHalfBandwidth = mTunerController.getUsableHalfBandwidth();

        //Strategy 2: start by placing the center frequency exactly one channel width below the first channel frequency
        //and iteratively increase it one channel width at a time.  This optimally places the channels nearest to the
        //center of the bandwidth
        long start = channels.first().getFrequency() - (int)mPolyphaseChannelManager.getChannelBandwidth();

        if(isValidCenterFrequency(channels, start))
        {
            return start;
        }

        while(start - channels.first().getMinFrequency() < usableHalfBandwidth)
        {
            start += (int)mPolyphaseChannelManager.getChannelBandwidth();

            if(isValidCenterFrequency(channels, start))
            {
                return start;
            }
        }

        //Strategy 3: start by placing the first channel at the minimum location within the tuner bandwidth
        double startFrequency = channels.first().getMinFrequency() + usableHalfBandwidth;

        //Align the test frequency to the next greater integral frequency placing the first channel at the
        // minimum extreme of the available bandwidth as a starting location
        double delta = Math.abs(startFrequency - bestIntegralFrequency);
        double adjustment = delta % mPolyphaseChannelManager.getChannelBandwidth();

        startFrequency += adjustment;

        long availableTestBandwidth = mTunerController.getUsableBandwidth() - channelSetBandwidth;
        int availableTestChannels = (int)(availableTestBandwidth / mPolyphaseChannelManager.getChannelBandwidth()) + 1;

        if(isValidCenterFrequency(channels,(long)startFrequency))
        {
            return (long)startFrequency;
        }

        int currentChannel = 1;

        while(currentChannel <= availableTestChannels)
        {
            long testFrequency = (long)(startFrequency - (currentChannel * mPolyphaseChannelManager.getChannelBandwidth()));

            if(isValidCenterFrequency(channels, testFrequency))
            {
                return testFrequency;
            }

            currentChannel++;
        }

        //Strategy 4: brute force walk across the spectrum 1 hertz at a time looking for a center frequency that will
        //fit all channels
        long testFrequency = channels.first().getMinFrequency() + mTunerController.getUsableHalfBandwidth();
        long minimumFrequency = testFrequency - availableTestBandwidth;

        if(isValidCenterFrequency(channels,testFrequency))
        {
            return testFrequency;
        }

        while(testFrequency >= minimumFrequency)
        {
            testFrequency--;

            if(isValidCenterFrequency(channels, testFrequency))
            {
                return testFrequency;
            }
        }

        throw new IllegalArgumentException("Can't calculate valid center frequency for the channel set");
    }

    private long getChannelSetBandwidth(SortedSet<TunerChannel> channels)
    {
        return channels.last().getMaxFrequency() - channels.first().getMinFrequency();
    }

    /**
     * Indicates if the candidate center frequency is valid to ensure that all of the channels fit within the tuner's
     * bandwidth and avoid any center DC spike blocked frequency region
     *
     * @param tunerChannels to test for the candidate center frequency
     * @param centerFrequency to test
     * @return true if the candidate center frequency is valid for the set of channels
     */
    private boolean isValidCenterFrequency(SortedSet<TunerChannel> tunerChannels, long centerFrequency)
    {
        for(TunerChannel tunerChannel: tunerChannels)
        {
            if(!isValidCenterFrequency(tunerChannel, centerFrequency))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Indicates if the channel will fit within the tuner's bandwidth if the tuner were using the candidate center
     * frequency and that the channel does not overlap an unusable center region that would be centered about the
     * candidate center frequency
     *
     * @param tunerChannel to test for fit
     * @param centerFrequency to test against
     * @return true if the channel will fit when the tuner uses the specified center frequency
     */
    private boolean isValidCenterFrequency(TunerChannel tunerChannel, long centerFrequency)
    {
        boolean fits = (tunerChannel.getMinFrequency() >= centerFrequency - mTunerController.getUsableHalfBandwidth()) &&
            (tunerChannel.getMaxFrequency() <= centerFrequency + mTunerController.getUsableHalfBandwidth());

        if(fits && mTunerController.hasMiddleUnusableBandwidth())
        {
            fits = !tunerChannel.overlaps(centerFrequency - mTunerController.getMiddleUnusableHalfBandwidth(),
                centerFrequency + mTunerController.getMiddleUnusableHalfBandwidth());
        }

        return fits;
    }

    /**
     * Indicates if the two frequencies have a spacing that is an integral multiple of the channel bandwidth.
     */
    private boolean isIntegralSpacing(long frequencyA, long frequencyB)
    {
        double delta = Math.abs(frequencyA - frequencyB);
        return delta % mPolyphaseChannelManager.getChannelBandwidth() <= 1.0;
    }

    /**
     * Identifies the tuner channel frequency that has the best fit as an integral multiple of all of the other tuner
     * channels.  Using a polyphase channelizer to split each of the channels at a spacing of the channel sample rate,
     * best fit is defined as the lowest absolute miss distance for each channel from the integral centers
     * for each of the channels in the polyphase channelizer.
     *
     * @param channels containing a subset of channels that are spaced at integral multiples of the channel sample rate
     * @return best channel containing the highest subset of matching, integrally spaced channels in the set, or null
     * if there are no channels that are integrally spaced.
     */
    private long getIntegralFrequency(SortedSet<TunerChannel> channels)
    {
        if(channels.isEmpty())
        {
            throw new IllegalArgumentException("Channels cannot be empty");
        }

        long bestIntegralFrequency = channels.first().getFrequency();

        if(channels.size() > 1)
        {
            double bestScore = Double.MAX_VALUE;

            for(TunerChannel firstChannel: channels)
            {
                double score = 0.0;

                for(TunerChannel secondChannel: channels)
                {
                    if(firstChannel != secondChannel)
                    {
                        double delta = Math.abs(firstChannel.getFrequency() - secondChannel.getFrequency()) %
                            mPolyphaseChannelManager.getChannelBandwidth();

                        score += delta;
                    }
                }

                if(score < bestScore)
                {
                    bestScore = score;
                    bestIntegralFrequency = firstChannel.getFrequency();
                }
            }
        }

        return bestIntegralFrequency;
    }

    /**
     * List of tuner channels currently being sourced by this source manager
     */
    @Override
    public SortedSet<TunerChannel> getTunerChannels()
    {
        return mPolyphaseChannelManager.getTunerChannels();
    }

    /**
     * Count of tuner channels currently being sourced by this source manager
     */
    @Override
    public int getTunerChannelCount()
    {
        return mPolyphaseChannelManager.getTunerChannelCount();
    }

    /**
     * Allocates a tuner channel source for the tuner channel.
     *
     * @param tunerChannel for requested source
     * @return allocated DDC tuner channel source, or null if the channel cannot be provided by this source manager
     */
    @Override
    public TunerChannelSource getSource(TunerChannel tunerChannel)
    {
        if(isTunable(tunerChannel))
        {
            //Get a new set of currently tuned channels
            SortedSet<TunerChannel> tunerChannels = getTunerChannels();

            //Add the requested channel to the list
            tunerChannels.add(tunerChannel);

            if(canTune(tunerChannels))
            {
                long currentCenterFrequency = mTunerController.getFrequency();
                long updatedCenterFrequency = 0;

                //Attempt to adjust the center frequency before we allocate the channel
                try
                {
                    updatedCenterFrequency = getCenterFrequency(tunerChannels, currentCenterFrequency);

                    if(updatedCenterFrequency != currentCenterFrequency && updatedCenterFrequency != 0)
                    {
                        mTunerController.setFrequency(updatedCenterFrequency);
                    }

                    //If we're successful to here, allocate the channel
                    return mPolyphaseChannelManager.getChannel(tunerChannel);
                }
                catch(SourceException se)
                {
                    //Tuner controller threw an error trying to tune to the updated frequency
                    mLog.error("Error while updating tuner controller with new center frequency [" +
                        updatedCenterFrequency + "] - unable to allocate new tuner channel", se);
                }
                catch(IllegalArgumentException iae)
                {
                    //Center frequency calculation failed
//                    mLog.debug("Couldn't calculate new tuner center frequency for new tuner channel [" +
//                        tunerChannel + "] unable to allocate new tuner channel");
                }
            }
        }

        return null;
    }

    /**
     * Processes source events received from the tuner by simply passing them on to the embedded polyphase channel
     * manager
     */
    @Override
    public void process(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case NOTIFICATION_CHANNEL_COUNT_CHANGE:
                //Rebroadcast this event to any registered listeners (ie tuner and tuner controller)
                broadcast(sourceEvent);
                //Lock the frequency and sample rate controls on the tuner controller so users can't change them
                //when the polyphase manager has channels allocated
                mTunerController.setLocked(getTunerChannelCount() > 0);
                break;
            case NOTIFICATION_MEASURED_FREQUENCY_ERROR_SYNC_LOCKED:
                //Rebroadcast these frequency measurement errors to the tuner and tuner controller
                //for display and/or correction
                broadcast(sourceEvent);
                break;
            default:
                mLog.info("Unrecognized source event: " + sourceEvent);
                break;
        }
    }
}
