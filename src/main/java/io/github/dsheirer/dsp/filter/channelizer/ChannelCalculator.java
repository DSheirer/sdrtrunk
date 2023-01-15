/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
 * ****************************************************************************
 */
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelCalculator
{
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private final static Logger mLog = LoggerFactory.getLogger(ChannelCalculator.class);

    /**
     * Policy to determine desired corrective action for methods that calculate an index for a frequency or a frequency
     * from an index where the frequency may exist either at the overlapping boundary of two indexes, or in the middle
     * of the wrap-around (N/2) index.
     */
    public enum IndexBoundaryPolicy
    {
        ADJUST_POSITIVE,
        ADJUST_NEGATIVE
    }

    private double mSampleRate;
    private int mChannelCount;
    private double mCenterFrequency;
    private double mChannelBandwidth;
    private double mOversampling = 2.0;

    /**
     * Calculates channel index(es) from a polyphase channelizer output to source a DDC polyphase channel source
     * with a specified channel center frequency and bandwidth.
     *
     * Polyphase channel indexes are not linear/consecutive owing to the use of the FFT to rotate channels to their
     * center frequencies following filtering in the polyphase filter bank.  As such, this calculator provides methods
     * to determine the min/center/max frequency of each channel that is adjusted for the current center frequency
     * value and to determine the channel index(es) that need to be targeted to produce a requested output channel
     * with a specific frequency and bandwdith.
     *
     * @param sampleRate in hertz for the input sample rate from the sample source (ie tuner)
     * @param channelCount number of channels in the polyphase filter bank
     * @param centerFrequency center frequency for the input sample stream
     * @param oversampling set to 1.0 for no oversample, or greater than 1.0 for oversampling
     */
    public ChannelCalculator(double sampleRate, int channelCount, double centerFrequency, double oversampling)
    {
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
        mCenterFrequency = centerFrequency;
        mOversampling = oversampling;

        updateChannelBandwidth();;
    }

    /**
     * Calculates the channel bandwidth from the sample rate and channel count.
     */
    private void updateChannelBandwidth()
    {
        mChannelBandwidth = (double)mSampleRate / (double)mChannelCount;
    }

    /**
     * Center frequency for the channelizer
     */
    public double getCenterFrequency()
    {
        return mCenterFrequency;
    }

    /**
     * Updates the center frequency for the channelizer
     */
    public void setCenterFrequency(double frequency)
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
     * Updates the number of channels being channelized by the polyphase channelizer
     */
    public void setChannelCount(int channelCount)
    {
        mChannelCount = channelCount;
        updateChannelBandwidth();
    }

    /**
     * Identifies the index of the wrap-around channel meaning that the lower half of the channel exists at the upper
     * end of the spectrum and the upper half of the channel exists at the lower end of the spectrum
     *
     * @return wrap-around channel index
     */
    public int getWrapAroundIndex()
    {
        return mChannelCount / 2;
    }

    /**
     * PolyphaseChannelManager input sample rate
     * @return input sample rate in hertz
     */
    public double getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Updates the channel count using the sample rate argument to calculate the number of channels from the
     * already established channel bandwidth.
     *
     * @param sampleRate to use for channel count
     */
    public void setRates(double sampleRate, int channelCount)
    {
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
        updateChannelBandwidth();
    }

    /**
     * Channel bandwidth.  Note: this value may be smaller than the channel sample rate when oversampling
     * is being used.
     */
    public double getChannelBandwidth()
    {
        return mChannelBandwidth;
    }

    /**
     * Half of the channel bandwidth.
     */
    public double getHalfChannelBandwidth()
    {
        return mChannelBandwidth / 2.0;
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
     * Sets the oversampling rate being applied by the polyphase channelizer
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
        return getChannelBandwidth() * getOversampling();
    }

    /**
     * PolyphaseChannelManager input minimum frequency
     * @return minimum frequency in hertz
     */
    public long getMinimumFrequency()
    {
        return (long)(getCenterFrequency() - (getSampleRate() / 2.0));
    }

    /**
     * PolyphaseChannelManager input maximum frequency
     * @return maximum frequency in hertz
     */
    public long getMaximumFrequency()
    {
        return (long)(getCenterFrequency() + (getSampleRate() / 2.0));
    }

    /**
     * Identifies the channelizer channel index(es) required to produce a tuner channel with the specified center
     * frequency and channel bandwidth.
     *
     * @param tunerChannel specifying the desired channel frequency and bandwidth
     *
     * @return an array of one or more polyphase channelizer index(es) to target in order to produce an output channel
     * for the desired frequency and bandwidth.
     *
     * @throws IllegalArgumentException if the tuner channel's minimum or maximum frequency is not contained
     * within the channelizer's minimum or maximum frequency range
     */
    public List<Integer> getChannelIndexes(TunerChannel tunerChannel) throws IllegalArgumentException
    {
        if(tunerChannel.getMinFrequency() < getMinimumFrequency() ||
           tunerChannel.getMaxFrequency() > getMaximumFrequency())
        {
            throw new IllegalArgumentException("Requested channel cannot be provided by this " +
                "channelizer.  Requested channel [" + tunerChannel.getMinFrequency() + " - " +
                tunerChannel.getMaxFrequency() + "] exceeds current channelizer frequency range [" +
                getMinimumFrequency() + " - " + getMaximumFrequency() + "]");
        }

        int minIndex = getIndexForFrequency(tunerChannel.getMinFrequency(), IndexBoundaryPolicy.ADJUST_POSITIVE);
        int maxIndex = getIndexForFrequency(tunerChannel.getMaxFrequency(), IndexBoundaryPolicy.ADJUST_NEGATIVE);

        //Detect the unsourceable situation where the wrap around index would be included in both the minimum index
        //and the maximum index, meaning that the tuner channel bandwidth is nearly the same as the input sample rate.
        if(minIndex == getWrapAroundIndex() && maxIndex == getWrapAroundIndex())
        {
            throw new IllegalArgumentException("Requested tuner channel cannot be provided.  Requested bandwidth is " +
                "within two channel bandwidths of the sample rate.");
        }

        List<Integer> indexes = new ArrayList<>();

        indexes.add(minIndex);

        if(minIndex != maxIndex)
        {
            //Ensure our min/max indexes are within the correct bounds 0 <> N-1 to safeguard the while loop below
            if(minIndex < 0 || minIndex >= getChannelCount() || maxIndex < 0 || maxIndex >= getChannelCount())
            {
                throw new IllegalArgumentException("Something went wrong while calculating the min and max polyphase " +
                    "channel indexes for tuner channel: " + tunerChannel);
            }

            int pointer = minIndex + 1;

            if(pointer >= getChannelCount())
            {
                pointer -= getChannelCount();
            }

            while(pointer != maxIndex)
            {
                indexes.add(pointer);

                pointer++;

                if(pointer >= getChannelCount())
                {
                    pointer -= getChannelCount();
                }
            }

            indexes.add(maxIndex);
        }

        return indexes;
    }

    /**
     * Calculates the index for the specified frequency.  Certain frequencies can exist as both the maximum frequency
     * of one channel and the minimum frequency of an adjacent channel.  The index boundary policy identifies your
     * choice to select the higher frequency channel index (ADJUST_POSITIVE) or the lower frequency channel
     * index (ADJUST_NEGATIVE).
     *
     * @param frequency to determine channel index for
     * @param indexBoundaryPolicy to determine the behavior when the frequency overlaps two channel boundaries
     * @return index for the frequency
     */
    public int getIndexForFrequency(long frequency, IndexBoundaryPolicy indexBoundaryPolicy)
    {
        double offset = frequency - getCenterFrequency();

        if(FastMath.abs(offset) < getHalfChannelBandwidth())
        {
            return 0; //Center index
        }

        if(offset > 0)
        {
            offset += getHalfChannelBandwidth();
        }
        else
        {
            offset -= getHalfChannelBandwidth();
        }

        int indexOffset = (int)(offset / getChannelBandwidth());

        if(indexOffset < 0)
        {
            indexOffset += getChannelCount();
        }

        if(indexBoundaryPolicy == IndexBoundaryPolicy.ADJUST_POSITIVE &&
            isOverlapFrequency(frequency, indexOffset, indexOffset + 1))
        {
            indexOffset = normalize(indexOffset + 1);
        }
        else if(indexBoundaryPolicy == IndexBoundaryPolicy.ADJUST_NEGATIVE &&
            isOverlapFrequency(frequency, indexOffset - 1, indexOffset))
        {
            indexOffset = normalize(indexOffset - 1);
        }

        return indexOffset;
    }

    /**
     * Indicates if the frequency is an overlapping frequency between channel index 1 and channel index 2 where the
     * maximum frequency of index 1 is the same as the minimum frequency of index 2 and both of these frequencies are
     * the same as the frequency argument.
     *
     * Note: both index 1 and index 2 will be normalized to the range of 0 <> N-1 prior to determining overlap
     *
     * @param frequency to test for overlap between index 1 and 2
     * @param index1 which is the lower channel index located next to index 2.
     * @param index2 which is the higher channel index located next to index 1.
     * @return true if frequency = index1 maximum frequency = index2 minimum frequency
     */
    public boolean isOverlapFrequency(long frequency, int index1, int index2)
    {
        index1 = normalize(index1);
        index2 = normalize(index2);

        //Check that these two indexes are side by side
        int delta = normalize(index2 - index1);

        if(delta != 1)
        {
            return false;
        }

        long index1MaximumFrequency = (long)getIndexMaximumFrequency(index1, IndexBoundaryPolicy.ADJUST_POSITIVE);
        long index2MinimumFrequency = (long)getIndexMinimumFrequency(index2, IndexBoundaryPolicy.ADJUST_NEGATIVE);

        if(index1 == getWrapAroundIndex())
        {
            index1MaximumFrequency = (long)getIndexMaximumFrequency(index1, IndexBoundaryPolicy.ADJUST_NEGATIVE);
        }

        if(index2 == getWrapAroundIndex())
        {
            index2MinimumFrequency = (long)getIndexMinimumFrequency(index2, IndexBoundaryPolicy.ADJUST_POSITIVE);
        }

        return frequency == index1MaximumFrequency && frequency == index2MinimumFrequency;
    }

    /**
     * Normalizes the index to the range of 0 <> N-1 channel indexes</>
     */
    private int normalize(int index)
    {
        while(index < 0)
        {
            index += getChannelCount();
        }

        while(index >= getChannelCount())
        {
            index -= getChannelCount();
        }

        return index;
    }

    /**
     * Calculates the center frequency for the channel identified by the index in the channel results array.
     *
     * @param index to determine the channel center frequency
     * @return center frequency for the indexed channel
     */
    public double getIndexCenterFrequency(int index, IndexBoundaryPolicy indexBoundaryPolicy)
    {
        if(index < 0 || index >= getChannelCount())
        {
            throw new IllegalArgumentException("Illegal channel index [" + index + "] current max channels[" +
                getChannelCount() + "]");
        }

        int wrapAroundIndex = getWrapAroundIndex();

        if(index == wrapAroundIndex)
        {
            if(indexBoundaryPolicy == IndexBoundaryPolicy.ADJUST_POSITIVE)
            {
                return getCenterFrequency() + (index * getChannelBandwidth());
            }
            else
            {
                return getCenterFrequency() - (index * getChannelBandwidth());
            }
        }
        else if(index < wrapAroundIndex)
        {
            return getCenterFrequency() + (index * getChannelBandwidth());
        }
        else
        {
            return getCenterFrequency() - ((getChannelCount() - index) * getChannelBandwidth());
        }
    }

    /**
     * Calculates the minimum frequency band edge for the channel identified by the index in the channel results
     * array.
     * @param index to calculate the minimum band edge frequency for the channel.
     * @param indexBoundaryPolicy for the wrap around index to indicate if this method should return the minimum
     * frequency of the upper spectrum half of the wrap around channel (ADJUST_POSITIVE) or the minimum frequency of the
     * lower spectrum half of the wrap around channel (ADJUST_NEGATIVE)
     * @return frequency of the minimum band edge
     */
    public double getIndexMinimumFrequency(int index, IndexBoundaryPolicy indexBoundaryPolicy)
    {
        if(index < 0 || index >= getChannelCount())
        {
            throw new IllegalArgumentException("Can't determine minimum frequency - illegal channel index [" +
                index + "]");
        }

        int wrapAroundIndex = getWrapAroundIndex();

        if(index == wrapAroundIndex)
        {
            if(indexBoundaryPolicy == IndexBoundaryPolicy.ADJUST_POSITIVE)
            {
                return getCenterFrequency() + ((double)index * getChannelBandwidth()) - getHalfChannelBandwidth();
            }
            else
            {
                return getIndexCenterFrequency(index, indexBoundaryPolicy);
            }
        }
        else if(index <= wrapAroundIndex)
        {
            return getCenterFrequency() + ((double)index * getChannelBandwidth()) - getHalfChannelBandwidth();
        }
        else
        {
            return getCenterFrequency() - ((double)(getChannelCount() - index) * getChannelBandwidth()) -
                getHalfChannelBandwidth();
        }
    }

    /**
     * Calculates the maximum frequency band edge for the channel identified by the index in the channel results
     * array.
     * @param index to calculate the maximum band edge frequency for the channel.
     * @param indexBoundaryPolicy for the wrap around index to indicate if this method should return the maximum
     * frequency of the upper spectrum half of the wrap around channel (ADJUST_POSITIVE) or the maximum frequency of the
     * lower spectrum half of the wrap around channel (ADJUST_NEGATIVE)
     * @return frequency of the maximum band edge
     */
    public double getIndexMaximumFrequency(int index, IndexBoundaryPolicy indexBoundaryPolicy)
    {
        if(index < 0 || index >= getChannelCount())
        {
            throw new IllegalArgumentException("Can't determine maximum frequency - illegal channel index [" +
                index + "]");
        }

        int wrapAroundIndex = getWrapAroundIndex();

        if(index == wrapAroundIndex)
        {
            if(indexBoundaryPolicy == IndexBoundaryPolicy.ADJUST_POSITIVE)
            {
                return getIndexCenterFrequency(index, indexBoundaryPolicy);
            }
            else
            {
                return getCenterFrequency() - ((double)index * getChannelBandwidth() - getHalfChannelBandwidth());
            }
        }
        else if(index <= wrapAroundIndex)
        {
            return getCenterFrequency() + ((double)index * getChannelBandwidth()) + getHalfChannelBandwidth();
        }
        else
        {
            return getCenterFrequency() - ((double)(getChannelCount() - index) * getChannelBandwidth()) +
                getHalfChannelBandwidth();
        }
    }

    /**
     * Calculates the center frequency for a channel that will be synthesized from the channel indexes.
     * @param indexes that are contiguous in the spectrum
     * @return center frequency in hertz
     */
    public long getCenterFrequencyForIndexes(List<Integer> indexes)
    {
        if(indexes.isEmpty())
        {
            throw new IllegalArgumentException("Indexes cannot be empty");
        }

        int centerIndex = (indexes.size() - 1) / 2;

        int index = indexes.get(centerIndex);

        if(indexes.size() % 2 == 0)
        {
            return (long)getIndexMaximumFrequency(index, IndexBoundaryPolicy.ADJUST_NEGATIVE);
        }
        else
        {
            if(index == getWrapAroundIndex())
            {
                return (long)getIndexCenterFrequency(index, IndexBoundaryPolicy.ADJUST_NEGATIVE);
            }
            else
            {
                return (long)getIndexCenterFrequency(index, IndexBoundaryPolicy.ADJUST_POSITIVE);
            }
        }
    }

    /**
     * String representation of the channel calculator settings
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Channel Calculator | Tuner SR:").append(FREQUENCY_FORMAT.format(getSampleRate() / 1E6D));
        sb.append(" CF:").append(FREQUENCY_FORMAT.format(getCenterFrequency() / 1E6D));
        sb.append(" MIN:").append(FREQUENCY_FORMAT.format(getMinimumFrequency() / 1E6D));
        sb.append(" MAX:").append(FREQUENCY_FORMAT.format(getMaximumFrequency() / 1E6D));
        sb.append(" | Channel COUNT:").append(getChannelCount());
        sb.append(" BW:").append(FREQUENCY_FORMAT.format(getChannelBandwidth() / 1E6D));
        sb.append(" SR:").append(FREQUENCY_FORMAT.format(getChannelSampleRate() / 1E6D));
        return sb.toString();
    }
}
