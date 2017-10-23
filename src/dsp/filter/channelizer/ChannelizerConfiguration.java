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
package dsp.filter.channelizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import source.tuner.TunerChannel;

import java.util.ArrayList;
import java.util.List;

public class ChannelizerConfiguration
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelizerConfiguration.class);

    private long mCenterFrequency;
    private int mChannelCount;
    private int mChannelBandwidth;
    private double mOversampling = 1.0;

    /**
     * Configuration details for a polyphase channelizer that can be used to identify which polyphase
     * channelizer output channel(s) should be processed to produce an output channel of a specified
     * center frequency and channel sample rate/bandwidth.
     *
     * @param centerFrequency
     * @param channelCount
     * @param channelBandwidth
     * @param oversampling set to 1.0 for no oversample, or greater than 1.0 for oversampling
     */
    public ChannelizerConfiguration(long centerFrequency, int channelCount, int channelBandwidth, double oversampling)
    {
        mCenterFrequency = centerFrequency;
        mChannelCount = channelCount;
        mChannelBandwidth = channelBandwidth;
        mOversampling = oversampling;
    }

    /**
     * Center frequency for the channelizer
     */
    public long getCenterFrequency()
    {
        return mCenterFrequency;
    }

    /**
     * Sets or changes the center frequency for the channelizer
     */
    public void setCenterFrequency(long frequency)
    {
        mCenterFrequency = frequency;
    }

    /**
     * Number of channels being channelized by the polyphase channelizer
     */
    public int getChannelCount()
    {
        return mChannelCount;
    }

    /**
     * Sets the number of channels being channelized by the polyphase channelizer
     */
    public void setChannelCount(int channelCount)
    {
        mChannelCount = channelCount;
    }

    /**
     * Channel bandwidth.  Note: this value may be smaller than the channel sample rate when oversampling
     * is being used.
     */
    public int getChannelBandwidth()
    {
        return mChannelBandwidth;
    }

    /**
     * Half of the channel bandwidth.
     */
    public int getHalfChannelBandwidth()
    {
        return mChannelBandwidth / 2;
    }

    /**
     * Sets the channel bandwidth in hertz.
     */
    public void setChannelBandwidth(int channelBandwidth)
    {
        mChannelBandwidth = channelBandwidth;
    }

    /**
     * Oversampling rate produced by the channelizer.
     *
     * @return 1.0 for no oversampling, or a value greater than 1.0 when oversampling is applied.
     */
    public double getOversampling()
    {
        return mOversampling;
    }

    /**
     * Sets the oversampling rate being applied by the channelizer
     * @param oversampling rate where 1.0 indicates no oversampling, and a value greater than 1.0 indicates
     * that the polyphase channelizer is oversampling each channel.
     */
    public void setOversampling(double oversampling)
    {
        mOversampling = oversampling;
    }

    /**
     * Channel sample rate.  This value is the product of the filtered channel bandwidth times the
     * oversampling rate.
     */
    public double getChannelSampleRate()
    {
        return (double)getChannelBandwidth() * getOversampling();
    }

    /**
     * PolyphaseChannelManager input sample rate
     * @return input sample rate in hertz
     */
    public int getSampleRate()
    {
        return getChannelBandwidth() * getChannelCount();
    }

    /**
     * PolyphaseChannelManager input minimum frequency
     * @return minimum frequency in hertz
     */
    public long getMinimumFrequency()
    {
        return getCenterFrequency() - (getSampleRate() / 2);
    }

    /**
     * PolyphaseChannelManager input maximum frequency
     * @return maximum frequency in hertz
     */
    public long getMaximumFrequency()
    {
        return getCenterFrequency() + (getSampleRate() / 2);
    }

    /**
     * Identifies the channelizer output channel(s) required to produce a tuner channel with the specified center
     * frequency and channel bandwidth.
     *
     * @param tunerChannel specifying the desired channel frequency and bandwidth
     * @return an array of one or more polyphase channelizer channels to target in order to produce an output channel
     * for the desired frequency and bandwidth.
     * @throws IllegalArgumentException if the tuner channel's minimum or maximum frequency is not contained
     * within the channelizer's minimum or maximum frequency range
     */
    public List<Integer> getPolyphaseChannelIndexes(TunerChannel tunerChannel) throws IllegalArgumentException
    {
        if(tunerChannel.getMinFrequency() < getMinimumFrequency() ||
            tunerChannel.getMaxFrequency() > getMaximumFrequency())
        {
            throw new IllegalArgumentException("Requested channel cannot be provided by this " +
                "channelizer.  Requested channel [" + tunerChannel.getMinFrequency() + " - " +
                tunerChannel.getMaxFrequency() + "] exceeds current channelizer frequency range [" +
                getMinimumFrequency() + " - " + getMaximumFrequency() + "]");
        }

        List<Integer> indices = new ArrayList<>();

        long startFrequencyOffset = (tunerChannel.getMinFrequency() - getMinimumFrequency());
        int startIndex = (int)(startFrequencyOffset / getChannelBandwidth());

        long endFrequencyOffset = (tunerChannel.getMaxFrequency() - getMinimumFrequency());
        int endIndex = (int)(endFrequencyOffset / getChannelBandwidth());

        int currentIndex = startIndex;

        while(currentIndex <= endIndex)
        {
            long currentIndexMin = getMinimumFrequencyForChannelIndex(currentIndex);
            long currentIndexMax = getMaximumFrequencyForChannelIndex(currentIndex);

            //Case 1: tuner channel completely aligns with indexed channel
            if(currentIndexMin <= tunerChannel.getMinFrequency() && tunerChannel.getMaxFrequency() <= currentIndexMax)
            {
                indices.add(currentIndex);
            }
            //Case 2: tuner channel min frequency aligns and max frequency exceeds
            else if(currentIndexMin <= tunerChannel.getMinFrequency() &&
                    tunerChannel.getMinFrequency() < currentIndexMax &&
                    tunerChannel.getMaxFrequency() > currentIndexMax)
            {
                indices.add(currentIndex);
            }
            //Case 3: tuner channel min frequency exceeds and max frequency aligns
            else if(tunerChannel.getMinFrequency() < currentIndexMin &&
                    tunerChannel.getMaxFrequency() > currentIndexMin &&
                    tunerChannel.getMaxFrequency() <= currentIndexMax)
            {
                indices.add(currentIndex);
            }
            //Case 4: tuner channel encapsulates the indexed channel
            else if(tunerChannel.getMinFrequency() < currentIndexMin && tunerChannel.getMaxFrequency() > currentIndexMax)
            {
                indices.add(currentIndex);
            }

            currentIndex++;
        }

        return indices;
    }

    /**
     * Calculates the center frequency for the channel identified by the index in the channel results array.
     *
     * @param index to determine the channel center frequency
     * @return center frequency for the indexed channel
     */
    public long getCenterFrequencyForChannelIndex(int index)
    {
        if(index < 0 || index >= getChannelCount())
        {
            throw new IllegalArgumentException("Illegal channel index");
        }

        return getMinimumFrequency() + getHalfChannelBandwidth() + (index * getChannelBandwidth());
    }

    /**
     * Calculates the minimum frequency band edge for the channel identified by the index in the channel results
     * array.
     * @param index to calculate the minimum band edge frequency for the channel.
     * @return frequency of the minimum band edge
     */
    public long getMinimumFrequencyForChannelIndex(int index)
    {
        if(index < 0 || index > getChannelCount())
        {
            throw new IllegalArgumentException("Can't determine minimum frequency - illegal channel index [" +
                index + "]");
        }

        return getMinimumFrequency() + (index * getChannelBandwidth());
    }

    /**
     * Calculates the maximum frequency band edge for the channel identified by the index in the channel results
     * array.
     * @param index to calculate the maximum band edge frequency for the channel.
     * @return frequency of the maximum band edge
     */
    public long getMaximumFrequencyForChannelIndex(int index)
    {
        if(index < 0 || index > getChannelCount())
        {
            throw new IllegalArgumentException("Can't determine maximum frequency - illegal channel index [" +
                index + "]");
        }

        //Maximum frequency is the same as the minimum frequency of the next higher indexed channel
        return getMinimumFrequency() + ((index + 1) * getChannelBandwidth());
    }

    public static void main(String[] args)
    {
        mLog.debug("Starting ....");

        int channelCount = 10;

        ChannelizerConfiguration channelizerConfiguration =
            new ChannelizerConfiguration(100062500, channelCount, 12500, 2.0);

        for(int x = 0; x < channelCount; x++)
        {
            mLog.debug("Channel " + x + ": " + channelizerConfiguration.getMinimumFrequencyForChannelIndex(x) + " - " +
                channelizerConfiguration.getMaximumFrequencyForChannelIndex(x) +
                " Center: " + channelizerConfiguration.getCenterFrequencyForChannelIndex(x));
        }

        TunerChannel tunerChannel = new TunerChannel(100006250, 12500);

        List<Integer> indices = channelizerConfiguration.getPolyphaseChannelIndexes(tunerChannel);

        mLog.debug("Indices: " + indices);

        mLog.debug("Finished!");
    }
}
