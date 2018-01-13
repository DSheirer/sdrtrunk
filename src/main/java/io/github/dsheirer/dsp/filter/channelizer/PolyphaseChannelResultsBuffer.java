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
package io.github.dsheirer.dsp.filter.channelizer;

import java.util.Arrays;

public class PolyphaseChannelResultsBuffer
{
    private long mTimestamp;
    private int mPointer = 0;
    private int mMaxSize;
    private float[][] mChannelResults;

    /**
     * Buffer containing an array of polyphase channelizer complex channel results.
     * @param timestamp reference relative to the first channel results.
     * @param maxSize of channel results to store in this buffer
     */
    public PolyphaseChannelResultsBuffer(long timestamp, int maxSize)
    {
        mTimestamp = timestamp;
        mMaxSize = maxSize;
    }

    /**
     * Reference timestamp relative to the first channel results contained in this buffer.
     * @return reference timestamp in milliseconds since epoch
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Indicates if this buffer is full
     */
    public boolean isFull()
    {
        return mPointer >= mMaxSize;
    }

    /**
     * Indicates if this buffer is empty
     */
    public boolean isEmpty()
    {
        return mPointer == 0;
    }

    /**
     * Adds the channel results to this buffer
     * @param channelResults to add
     * @throws IllegalArgumentException if the buffer is full or if the size of the channel results array is a different
     * length than any of the preceeding channel results that were added to this buffer
     */
    public void add(float[] channelResults) throws IllegalArgumentException
    {
        if(mPointer >= mMaxSize)
        {
            throw new IllegalArgumentException("Buffer is full - cannot add new channel results");
        }
        if(mPointer == 0)
        {
            mChannelResults = new float[mMaxSize][channelResults.length];
            mChannelResults[mPointer++] = channelResults;
        }
        else
        {
            int currentLength = mChannelResults[0].length;

            if(channelResults.length != currentLength)
            {
                throw new IllegalArgumentException("Channel results length [" + channelResults.length + "] cannot be " +
                    "a different length than the channel results that are already contained in this buffer [" +
                    currentLength + "]");
            }

            mChannelResults[mPointer++] = channelResults;
        }
    }

    /**
     * Channel results array contained in this buffer
     */
    public float[][] getChannelResults()
    {
        if(mPointer == mMaxSize)
        {
            return mChannelResults;
        }
        else
        {
            return Arrays.copyOf(mChannelResults, mPointer);
        }
    }

    /**
     * Extracts a single I/Q interleaved sample buffer from the channel results
     * @param iChannelIndex to extract for the inphase sample.  The q index is assumed to be one greater than i.
     * @return float[] containing interleaved I and Q samples
     * @throws IllegalArgumentException if this buffer doesn't contain any samples or if the requested channel is
     * not within the channel range of the contained channel results.
     */
    public float[] getChannel(int iChannelIndex)
    {
        if(isEmpty())
        {
            throw new IllegalArgumentException("Buffer is empty - cannot extract channel [" + iChannelIndex + "]");
        }
        if(!isValidChannelIndex(iChannelIndex))
        {
            throw new IllegalArgumentException("Channel [" + iChannelIndex + "] is not valid for the contained channel " +
                "results -- max channel is " + getMaxChannelIndex());
        }

        float[] samples = new float[mChannelResults.length * 2];
        int pointer = 0;

        int qChannelIndex = iChannelIndex + 1;

        for(float[] channelResults: mChannelResults)
        {
            samples[pointer++] = channelResults[iChannelIndex];
            samples[pointer++] = channelResults[qChannelIndex];
        }

        return samples;
    }

    /**
     * Indicates if the channel argument is valid for the contained channel results.
     * @param channelIndex number (0 to N) to check
     * @return false if the buffer is empty or if the channel is greater than the contained channel results
     */
    private boolean isValidChannelIndex(int channelIndex)
    {
        return channelIndex <= getMaxChannelIndex();
    }

    /**
     * Maximum channel index represented in the channel results contained in this buffer.
     * @return max channel number, or 0 if the buffer is empty
     */
    private int getMaxChannelIndex()
    {
        if(isEmpty())
        {
            return 0;
        }

        return mChannelResults[0].length;
    }
}
