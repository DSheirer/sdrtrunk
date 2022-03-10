/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.icecast;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.AudioStreamingBroadcaster;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.IBroadcastMetadataUpdater;
import io.github.dsheirer.audio.convert.AudioSampleRate;
import io.github.dsheirer.audio.convert.MP3Setting;

public abstract class IcecastAudioBroadcaster extends AudioStreamingBroadcaster
{
    private IBroadcastMetadataUpdater mMetadataUpdater;
    private AliasModel mAliasModel;

    public IcecastAudioBroadcaster(BroadcastConfiguration broadcastConfiguration, AudioSampleRate audioSampleRate,
                                   MP3Setting mp3Setting, AliasModel aliasModel)
    {
        super(broadcastConfiguration, audioSampleRate, mp3Setting);
        mAliasModel = aliasModel;
    }

    /**
     * Icecast broadcast configuration
     */
    protected IcecastConfiguration getConfiguration()
    {
        return (IcecastConfiguration) getBroadcastConfiguration();
    }

    @Override
    protected IBroadcastMetadataUpdater getMetadataUpdater()
    {
        if(mMetadataUpdater == null)
        {
            mMetadataUpdater = new IcecastBroadcastMetadataUpdater(getConfiguration(), mAliasModel);
        }

        return mMetadataUpdater;
    }
}
