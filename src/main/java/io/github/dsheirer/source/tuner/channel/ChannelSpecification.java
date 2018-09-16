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
package io.github.dsheirer.source.tuner.channel;

/**
 * Channel source specification
 */
public class ChannelSpecification
{
    private double mMinimumSampleRate;
    private int mBandwidth;
    private double mPassFrequency;
    private double mStopFrequency;

    /**
     * Constructs a channel specification instance.
     * @param minimumSampleRate requested for the channel
     * @param bandwidth
     * @param passFrequency for the pass band of the channel
     * @param stopFrequency for the stop band of teh channel
     */
    public ChannelSpecification(double minimumSampleRate, int bandwidth, double passFrequency, double stopFrequency)
    {
        mMinimumSampleRate = minimumSampleRate;
        mBandwidth = bandwidth;
        mPassFrequency = passFrequency;
        mStopFrequency = stopFrequency;
    }

    /**
     * Minimum requested sample rate for the channel
     */
    public double getMinimumSampleRate()
    {
        return mMinimumSampleRate;
    }

    /**
     * Bandwidth of the channel in Hertz
     */
    public int getBandwidth()
    {
        return mBandwidth;
    }

    /**
     * Pass band frequency for filtering the channel
     */
    public double getPassFrequency()
    {
        return mPassFrequency;
    }

    /**
     * Stop band frequency for filtering the channel
     */
    public double getStopFrequency()
    {
        return mStopFrequency;
    }
}
