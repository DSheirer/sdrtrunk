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
import sample.Listener;
import source.ISourceEventProcessor;
import source.Source;
import source.SourceEvent;
import source.tuner.Tuner;
import source.tuner.TunerChannel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Channelizer implements ISourceEventProcessor, Listener<SourceEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(Channelizer.class);

    private Tuner mTuner;
    private List<PolyphaseChannelSource> mChannelSources = new CopyOnWriteArrayList<>();

    /**
     * Provides access to Digital Drop Channel (DDC) sources from the tuner.  Incorporates a polyphase channelizer
     * with downstream channel and upstream tuner management.
     *
     * @param tuner providing broadband sample buffers
     */
    public Channelizer(Tuner tuner)
    {
        mTuner = tuner;
        mTuner.addSourceEventListener(this);
    }

    public Source getChannel(TunerChannel tunerChannel)
    {
        //TODO: return a polyphase channel here
        return null;
    }

    /**
     * Starts/adds the channel source to receive channelized sample buffers, registering with the tuner to receive
     * sample buffers when this is the first channel.
     *
     * @param channelSource to start
     */
    private void startChannelSource(PolyphaseChannelSource channelSource)
    {
        if(mChannelSources.isEmpty())
        {
            //TODO: start the tuner
        }

        mChannelSources.add(channelSource);
    }

    /**
     * Stops/removes the channel source from receiving channelized sample buffers and deregisters from the tuner
     * if this is the last channel being sourced.
     *
     * @param channelSource to stop
     */
    private void stopChannelSource(PolyphaseChannelSource channelSource)
    {
        mChannelSources.remove(channelSource);

        if(mChannelSources.isEmpty())
        {
            //TODO: stop the tuner
        }

        if(mTuner != null)
        {
            //TODO: change the tuner to accept a remove(TunerChannel) method
//            mTuner.remove(channel.getTunerChannel());
        }
    }

    @Override
    public void process(SourceEvent sourceEvent)
    {
        switch(sourceEvent.getEvent())
        {
            case REQUEST_START_SAMPLE_STREAM:
                if(sourceEvent.hasSource() && sourceEvent.getSource() instanceof PolyphaseChannelSource)
                {
                    startChannelSource((PolyphaseChannelSource)sourceEvent.getSource());
                }
                else
                {
                    mLog.error("Request to stop sample stream for unrecognized source: " +
                        (sourceEvent.hasSource() ? sourceEvent.getSource().getClass() : "null source"));
                }
                //TODO: add the channel and register for samples from the tuner
                break;
            case REQUEST_STOP_SAMPLE_STREAM:
                if(sourceEvent.hasSource() && sourceEvent.getSource() instanceof PolyphaseChannelSource)
                {
                    stopChannelSource((PolyphaseChannelSource)sourceEvent.getSource());
                }
                else
                {
                    mLog.error("Request to stop sample stream for unrecognized source: " +
                        (sourceEvent.hasSource() ? sourceEvent.getSource().getClass() : "null source"));
                }
                break;
        }



    }

    @Override
    public void receive(SourceEvent sourceEvent)
    {

    }
}
