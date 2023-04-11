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

import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.InterleavedComplexSamples;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.SourceEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractComplexPolyphaseChannelizer implements Listener<InterleavedComplexSamples>, ISourceEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractComplexPolyphaseChannelizer.class);
    private Broadcaster<SourceEvent> mSourceChangeBroadcaster = new Broadcaster();
    private List<PolyphaseChannelSource> mChannels = new CopyOnWriteArrayList<>();
    private double mSampleRate;
    private int mChannelCount;
    private int mSubChannelCount;
    private double mChannelSampleRate;
    protected long mCurrentSamplesTimestamp;

    /**
     * Complex sample polyphase channelizer
     *
     * @param channelCount
     */
    public AbstractComplexPolyphaseChannelizer(double sampleRate, int channelCount)
    {
        mChannelCount = channelCount;
        mSubChannelCount = channelCount * 2; //Number of I/Q channels
        mSampleRate = sampleRate;
        mChannelSampleRate = (double)mSampleRate / (double)mChannelCount;
    }

    /**
     * Input sample rate for this channelizer
     * @return sample rate in hertz
     */
    public double getSampleRate()
    {
        return mSampleRate;
    }

    /**
     * Sets the input sample rate for for this channelizer
     * @param sampleRate in hertz
     */
    public void setRates(double sampleRate, int channelCount)
    {
        mSampleRate = sampleRate;
        mChannelCount = channelCount;
        mChannelSampleRate = mSampleRate / (double)mChannelCount;
    }

    /**
     * Number of channels being processed by this channelizer.
     */
    public int getChannelCount()
    {
        return mChannelCount;
    }

    /**
     * Number of sub channels (I/Q) being processed by this channelizer.  This is 2 * channel count.
     */
    public int getSubChannelCount()
    {
        return mSubChannelCount;
    }

    /**
     * Output channel sample rate
     * @return sample rate in hertz
     */
    public double getChannelSampleRate()
    {
        return mChannelSampleRate;
    }

    /**
     * Dispatches the processed channel samples to any registered polyphase channel outputs.
     *
     * @param channelResultsList a list of arrays of I/Q samples per channel
     */
    protected void dispatch(List<float[]> channelResultsList)
    {
        for(PolyphaseChannelSource channel : mChannels)
        {
            channel.receiveChannelResults(channelResultsList, mCurrentSamplesTimestamp);
        }
    }

    /**
     * Adds the polyphase channel source to receive processed output channel samples
     *
     * @param polyphaseChannelSource
     */
    public void addChannel(PolyphaseChannelSource polyphaseChannelSource)
    {
        if(polyphaseChannelSource != null && !mChannels.contains(polyphaseChannelSource))
        {
            mChannels.add(polyphaseChannelSource);
            mSourceChangeBroadcaster.addListener(polyphaseChannelSource.getSourceEventListener());
        }
        else
        {
            mLog.error("Error adding polyphase channel source - " + (polyphaseChannelSource == null ? "source is null" :
                    "channel source is already added to this channelizer"));
        }
    }

    /**
     * Removes the polyphase channel source from receiving output channel samples.
     *
     * @param polyphaseChannelSource
     */
    public void removeChannel(PolyphaseChannelSource polyphaseChannelSource)
    {
        if(polyphaseChannelSource != null && mChannels.contains(polyphaseChannelSource))
        {
            mChannels.remove(polyphaseChannelSource);
            mSourceChangeBroadcaster.removeListener(polyphaseChannelSource.getSourceEventListener());
        }
        else
        {
            mLog.error("Error removing polyphase channel source - " + (polyphaseChannelSource == null ? "source is null" :
                    "channel source was not previously added to this channelizer"));
        }
    }

    /**
     * Number of polyphase channels registered to receive sample streams
     */
    public int getRegisteredChannelCount()
    {
        return mChannels.size();
    }

    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceChangeBroadcaster;
    }
}
