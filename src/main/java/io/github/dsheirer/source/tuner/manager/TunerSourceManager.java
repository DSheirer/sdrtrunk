/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.manager;

import io.github.dsheirer.dsp.filter.channelizer.PolyphaseChannelManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;

import java.util.List;

public class TunerSourceManager extends AbstractSourceManager
{
    private PolyphaseChannelManager mPolyphaseChannelManager;
    private TunerController mTunerController;

    public TunerSourceManager(TunerController tunerController)
    {
        mTunerController = tunerController;
    }

    //TODO: broadcast a tuner event each time we add a new channel to the polyphase manager

    @Override
    public boolean canTune(TunerChannel tunerChannel)
    {
        return false;
    }

    @Override
    public List<TunerChannel> getChannels()
    {
        return null;
    }

    @Override
    public TunerChannelSource getSource(TunerChannel tunerChannel)
    {
        return null;
    }

    @Override
    public void releaseSource(TunerChannelSource source)
    {

    }

    @Override
    public void addSourceEventListener(Listener<SourceEvent> sourceEventListener)
    {

    }

    @Override
    public void removeSourceEventListener(Listener<SourceEvent> sourceEventListener)
    {

    }

    @Override
    public void receive(SourceEvent sourceEvent)
    {

    }
}
