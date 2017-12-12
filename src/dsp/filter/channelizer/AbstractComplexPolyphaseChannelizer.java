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
import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;
import source.ISourceEventListener;
import source.SourceEvent;
import source.tuner.channel.polyphase.PolyphaseChannelSource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractComplexPolyphaseChannelizer implements Listener<ComplexBuffer>, ISourceEventListener
{
    private final static Logger mLog = LoggerFactory.getLogger(AbstractComplexPolyphaseChannelizer.class);

    private Broadcaster<SourceEvent> mSourceChangeBroadcaster = new Broadcaster();
    private List<PolyphaseChannelSource> mChannels = new CopyOnWriteArrayList<>();
    private double mSampleRate;
    private int mChannelCount;
    private double mChannelSampleRate;

    /**
     * Complex sample polyphase channelizer
     *
     * @param channelCount
     */
    public AbstractComplexPolyphaseChannelizer(double sampleRate, int channelCount)
    {
        mChannelCount = channelCount;
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
    public void setSampleRate(double sampleRate)
    {
        mSampleRate = sampleRate;
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
     * @param channels containing an array of I/Q samples per channel
     */
    protected void dispatch(float[] channels)
    {
        for(PolyphaseChannelSource channel : mChannels)
        {
            channel.receiveChannelResults(channels);
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
