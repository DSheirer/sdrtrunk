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

import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Calculates tuner center frequency for a set of tuner channels.
 */
public class CenterFrequencyCalculator
{
    public static final long INVALID_FREQUENCY = -1;

    /**
     * Determines the optimal center frequency for a given tuner and set of tuner channels.
     *
     * @param tunerController that is providing tuner channels
     * @param channels to calculate
     * @return optimal center frequency or INVALID_FREQUENCY if a center frequency cannot be calculated.
     */
    public static long getCenterFrequency(TunerController tunerController, SortedSet<TunerChannel> channels)
    {
        long candidateFrequency = INVALID_FREQUENCY;

        boolean isValidCandidateFrequency = true;

        //If there is only 1 channel set the center frequency so that the channel is positioned to the right of center
        if(channels.size() == 1)
        {
            candidateFrequency = channels.first().getMinFrequency() - tunerController.getMiddleUnusableHalfBandwidth() + 1;
        }
        else
        {
            long minChannelFrequency = channels.first().getMinFrequency();
            long maxChannelFrequency = channels.last().getMaxFrequency();

            //Start by placing the highest frequency channel at the high end of the spectrum
            candidateFrequency = maxChannelFrequency - (tunerController.getUsableBandwidth() / 2);

            if(maxChannelFrequency - minChannelFrequency <= tunerController.getUsableHalfBandwidth())
            {
                candidateFrequency = minChannelFrequency - tunerController.getMiddleUnusableHalfBandwidth();
                isValidCandidateFrequency = true;
            }
            else
            {
                //Iterate the channels and make sure that none of them overlap the center DC spike buffer, if one exists
                if(tunerController.getMiddleUnusableHalfBandwidth() > 0)
                {
                    boolean processingRequired = true;

                    while(isValidCandidateFrequency && processingRequired)
                    {
                        processingRequired = false;

                        long minAvoid = candidateFrequency - tunerController.getMiddleUnusableHalfBandwidth();
                        long maxAvoid = candidateFrequency + tunerController.getMiddleUnusableHalfBandwidth();

                        //If any of the center channel(s) overlap the central DC spike avoid area, we'll iteratively
                        //increase the tuned frequency causing the set of channels to move left in the tuned bandwidth until
                        //we either find a good center tune frequency, or we walk the lowest frequency channel out of the
                        //minimum tuned range, in which case we'll throw an exception indicating we don't have a solution.
                        for(TunerChannel channel : channels)
                        {
                            if(channel.overlaps(minAvoid, maxAvoid))
                            {
                                //Calculate a tuned frequency adjustment that places this overlapping channel just to the
                                //left of the central DC spike avoid zone
                                long adjustment = channel.getMaxFrequency() - minAvoid + 1;

                                //If the candidate frequency doesn't push the lowest channel out of bounds, make adjustment
                                if(candidateFrequency + adjustment - (tunerController.getUsableBandwidth() / 2) <= minChannelFrequency)
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

        }

        if(isValidCandidateFrequency)
        {
            return candidateFrequency;
        }

        return INVALID_FREQUENCY;
    }

    /**
     * Indicates if channel along with all of the other currently sourced
     * channels can fit within the tunable bandwidth.
     *
     */
    /**
     *
     * Indicates if channel along with all of the other currently sourced
     * channels can fit within the tunable bandwidth.

     * @param channel to test
     * @param tunerController that will provide the channel
     * @param channels is a current set of channels being sourced by the tuner controller
     * @return
     */
    public static boolean canTune(TunerChannel channel, TunerController tunerController, SortedSet<TunerChannel> channels)
    {
        //Make sure we're within the tunable frequency range of this tuner
        if(tunerController.getMinFrequency() < channel.getMinFrequency() && tunerController.getMaxFrequency() > channel.getMaxFrequency())
        {
            //If this is the first lock, then we're good
            if(channels.isEmpty())
            {
                return true;
            }
            else
            {
                SortedSet<TunerChannel> allChannels = new TreeSet<>(channels);
                allChannels.add(channel);

                //If the bandwidth of the channel set is less than or equal to the tuner's usable bandwidth, then
                //check to see if we can find a valid center frequency
                if(allChannels.last().getMaxFrequency() - allChannels.first().getMinFrequency() <= tunerController.getUsableBandwidth())
                {
                    return getCenterFrequency(tunerController, allChannels) != INVALID_FREQUENCY;
                }
            }
        }

        return false;
    }
}
