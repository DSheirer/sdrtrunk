/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.broadcast.icecast;

import audio.broadcast.AudioBroadcaster;
import audio.broadcast.BroadcastConfiguration;
import audio.broadcast.IBroadcastMetadataUpdater;
import controller.ThreadPoolManager;

public abstract class IcecastAudioBroadcaster extends AudioBroadcaster
{
    private IBroadcastMetadataUpdater mMetadataUpdater;

    public IcecastAudioBroadcaster(ThreadPoolManager threadPoolManager, BroadcastConfiguration broadcastConfiguration)
    {
        super(threadPoolManager, broadcastConfiguration);
    }

    /**
     * Icecast broadcast configuration
     */
    protected IcecastConfiguration getConfiguration()
    {
        return (IcecastConfiguration)getBroadcastConfiguration();
    }

    @Override
    protected IBroadcastMetadataUpdater getMetadataUpdater()
    {
        if(mMetadataUpdater == null)
        {
            mMetadataUpdater = new IcecastBroadcastMetadataUpdater(getThreadPoolManager(), getConfiguration());
        }

        return mMetadataUpdater;
    }
}
