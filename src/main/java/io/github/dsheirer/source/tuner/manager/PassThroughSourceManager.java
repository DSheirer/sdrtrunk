/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.ChannelSpecification;
import io.github.dsheirer.source.tuner.channel.PassThroughChannelSource;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Pass through source manager for relatively small bandwidth complex streams that don't warrant the overhead of
 * a polyphase or heterodyne channelizer.
 */
public class PassThroughSourceManager extends ChannelSourceManager
{
    private final static Logger mLog = LoggerFactory.getLogger(PassThroughSourceManager.class);
    private TunerController mTunerController;
    private SortedSet<TunerChannel> mTunerChannels = new TreeSet<>();
    private List<TunerChannelSource> mTunerChannelSources = new CopyOnWriteArrayList<>();

    public PassThroughSourceManager(TunerController tunerController)
    {
        mTunerController = tunerController;
    }

    @Override
    public SortedSet<TunerChannel> getTunerChannels()
    {
        return mTunerChannels;
    }

    @Override
    public int getTunerChannelCount()
    {
        return mTunerChannels.size();
    }

    @Override
    public TunerChannelSource getSource(TunerChannel tunerChannel, ChannelSpecification channelSpecification)
    {
        PassThroughChannelSource channelSource = new PassThroughChannelSource(new SourceEventProxy(),
                mTunerController, tunerChannel);

        mTunerChannels.add(tunerChannel);
        mTunerChannelSources.add(channelSource);

        return channelSource;
    }

    @Override
    public void process(SourceEvent event) throws SourceException
    {
        switch(event.getEvent())
        {
            case REQUEST_START_SAMPLE_STREAM:
                if(event.hasSource() && event.getSource() instanceof PassThroughChannelSource)
                {
                    mTunerController.addBufferListener((PassThroughChannelSource)event.getSource());
                    broadcast(SourceEvent.channelCountChange(mTunerChannels.size()));
                }
                break;
            case REQUEST_STOP_SAMPLE_STREAM:
                if(event.hasSource() && event.getSource() instanceof PassThroughChannelSource)
                {
                    PassThroughChannelSource source = (PassThroughChannelSource)event.getSource();
                    mTunerController.removeBufferListener(source);
                    mTunerChannels.remove(source.getTunerChannel());
                    mTunerChannelSources.remove(source);
                    broadcast(SourceEvent.channelCountChange(mTunerChannels.size()));
                }
                break;
        }
    }

    @Override
    public void setErrorMessage(String errorMessage)
    {
        for(TunerChannelSource tunerChannelSource: mTunerChannelSources)
        {
            tunerChannelSource.setError(errorMessage);
        }
    }

    public class SourceEventProxy implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            try
            {
                process(sourceEvent);
            }
            catch(SourceException se)
            {
                mLog.error("Error", se);
            }
        }
    }
}
