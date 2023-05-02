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
package io.github.dsheirer.dsp.filter.channelizer.output;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.source.heartbeat.HeartbeatManager;
import io.github.dsheirer.util.Dispatcher;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ChannelOutputProcessor implements IPolyphaseChannelOutputProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelOutputProcessor.class);

    private Dispatcher<List<float[]>> mChannelResultsDispatcher;
    private HeartbeatManager mHeartbeatManager;
    protected Listener<ComplexSamples> mComplexSamplesListener;
    private int mInputChannelCount;
    private long mCurrentSampleTimestamp = System.currentTimeMillis();

    /**
     * Base class for polyphase channelizer output channel processing.  Provides built-in frequency translation
     * oscillator support to apply frequency correction to the channel sample stream as requested by sample consumer.
     *
     * @param inputChannelCount is the number of input channels for this output processor
     * @param sampleRate of the output channel.  This is used to match the oscillator's sample rate to the output
     * channel sample rate for frequency translation/correction.
     * @param heartbeatManager to receive pings on the dispatcher thread
     */
    public ChannelOutputProcessor(int inputChannelCount, double sampleRate, HeartbeatManager heartbeatManager)
    {
        mInputChannelCount = inputChannelCount;
        //Process 1/10th of the sample rate per second at a rate of 20 times a second (200% of anticipated rate)
        mHeartbeatManager = heartbeatManager;
        mChannelResultsDispatcher = new Dispatcher("sdrtrunk polyphase channel", (int)(sampleRate / 10),
                50, mHeartbeatManager);
        mChannelResultsDispatcher.setListener(floats -> {
            try
            {
                process(floats);
            }
            catch(Throwable t)
            {
                mLog.error("Error processing channel results", t);
            }
        });
    }

    /**
     * Timestamp for the current series of samples.
     * @return time in milliseconds to use with assembled complex sample buffers.
     */
    protected long getCurrentSampleTimestamp()
    {
        return mCurrentSampleTimestamp;
    }

    @Override
    public void start()
    {
        mChannelResultsDispatcher.start();
    }

    @Override
    public void stop()
    {
        mChannelResultsDispatcher.stop();
    }

    /**
     * Registers the listener to receive the assembled complex sample buffers from this processor.
     */
    @Override
    public void setListener(Listener<ComplexSamples> listener)
    {
        mComplexSamplesListener = listener;
    }

    @Override
    public int getPolyphaseChannelIndexCount()
    {
        return mInputChannelCount;
    }

    public void dispose()
    {
    }

    @Override
    public void receiveChannelResults(List<float[]> channelResultsList, long timestamp)
    {
        mChannelResultsDispatcher.receive(channelResultsList);
        mCurrentSampleTimestamp = timestamp;
    }

    /**
     * Sub-class implementation to process one polyphase channelizer result array.
     * @param channelResults to process
     */
    public abstract void process(List<float[]> channelResults);

    @Override
    public int getInputChannelCount()
    {
        return mInputChannelCount;
    }
}
