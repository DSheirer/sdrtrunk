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
package io.github.dsheirer.sample.buffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class ChannelResultsBuffer extends AbstractBuffer
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelResultsBuffer.class);
    private Object mReusableComplexBufferQueue =
            new Object();
    private LinkedList<float[]> mEmptyBuffers = new LinkedList<>();
    private LinkedList<float[]> mFilledBuffers = new LinkedList<>();
    private Integer mLength;

    /**
     * Reusable buffer for storing polyphase channelizer results arrays.  This buffer is NOT thread safe and is
     * designed to be used by only one consumer.  The consumer must invoke the prepareForRecycle() method to indicate that this
     * buffer can be reused.
     *
     */
    public ChannelResultsBuffer()
    {
        super();
    }

    /**
     * Recycles this buffer for reuse.
     */
    protected void prepareForRecycle()
    {
        mEmptyBuffers.addAll(mFilledBuffers);
        mFilledBuffers.clear();
        mLength = null;
    }

    /**
     * Gets an empty channel results array correctly sized to the requested length.  This method should be used to
     * obtain all empty channel results buffers to fill with information and add back to this buffer so that the results
     * arrays can be reused.
     *
     * @param length of the requested array
     * @return a float array of the requested length that may contain stale data from previous use.
     */
    public float[] getEmptyBuffer(int length)
    {
        float[] emptyBuffer = mEmptyBuffers.poll();

        if(emptyBuffer == null || emptyBuffer.length != length)
        {
            emptyBuffer = new float[length];
        }

        return emptyBuffer;
    }

    /**
     * Adds the channel results to this buffer.  Note: you should only use channel results arrays obtained via the
     * getEmptyBuffer() method and filled with data and then added back to this buffer via this method, so that array
     * reuse can be accurately controlled.
     *
     * @param channelResults to add back to this buffer
     */
    public void addChannelResults(float[] channelResults)
    {
        if(mLength == null)
        {
            mLength = channelResults.length;
        }
        else if(mLength != channelResults.length)
        {
            throw new IllegalArgumentException("Channel results length must be the same for all added results");
        }

        mFilledBuffers.offer(channelResults);
    }

    /**
     * Channel results array contained in this buffer
     */
    public List<float[]> getChannelResults()
    {
        return mFilledBuffers;
    }

    /**
     * Extracts a single I/Q interleaved sample buffer from this buffer of channel results arrays
     *
     * @param iChannelIndex to extract for the inphase sample.  The q index is assumed to be one greater than i.
     * @return float[] containing interleaved I and Q samples
     * @throws IllegalArgumentException if this buffer doesn't contain any samples or if the requested channel is
     * not within the channel range of the contained channel results.
     */
    public ComplexBuffer getChannel(int iChannelIndex)
    {
        if(mFilledBuffers.isEmpty())
        {
            throw new IllegalArgumentException("Buffer is empty - cannot extract channel [" + iChannelIndex + "]");
        }

        if(!isValidChannelIndex(iChannelIndex))
        {
            throw new IllegalArgumentException("Channel [" + iChannelIndex + "] is not valid for the contained channel " +
                "results -- max channel is " + mLength);
        }

        ComplexBuffer buffer = new ComplexBuffer(new float[mFilledBuffers.size() * 2]);
        ComplexBuffer channelBuffer = buffer;

        float[] samples = channelBuffer.getSamples();

        int pointer = 0;

        int qChannelIndex = iChannelIndex + 1;

        for(float[] channelResults: mFilledBuffers)
        {
            try
            {
                samples[pointer] = channelResults[iChannelIndex];
                pointer++;
                samples[pointer] = channelResults[qChannelIndex];
                pointer++;
            }
            catch(Exception e)
            {
                mLog.error("Error accessing channel results - iIndex:" + iChannelIndex +
                " Results Size: " + mFilledBuffers.size() +
                " Samples Length:" + samples.length +
                " Pointer:" + pointer, e);
            }
        }

        return channelBuffer;
    }

    /**
     * Indicates if the channel argument is valid for the contained channel results.
     *
     * @param channelIndex number (0 to N) to check
     * @return false if the buffer is empty or if the channel is greater than the contained channel results
     */
    private boolean isValidChannelIndex(int channelIndex)
    {
        return (mLength != null) && (channelIndex <= mLength);
    }
}
