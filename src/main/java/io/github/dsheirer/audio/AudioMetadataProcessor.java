/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.audio;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Processes audio packets to assign attributes to each packet based on alias list settings.  Updates the
 * monitoring, recording and streaming attributes.
 */
public class AudioMetadataProcessor
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioMetadataProcessor.class);
    private AliasModel mAliasModel;
    private Map<Integer, AliasList> mAliasListMap = new HashMap<>();
    private Set<Integer> mAliasListRetrievedSet = new TreeSet<>();

    public AudioMetadataProcessor(AliasModel aliasModel)
    {
        mAliasModel = aliasModel;
    }

    public void process(ReusableAudioPacket audioPacket)
    {
        if(audioPacket.hasIdentifierCollection() && audioPacket.getIdentifierCollection().hasAliasListConfiguration())
        {
            AliasList aliasList = null;

            if(mAliasListRetrievedSet.contains(audioPacket.getAudioChannelId()))
            {
                aliasList = mAliasListMap.get(audioPacket.getAudioChannelId());
            }
            else
            {
                aliasList = mAliasModel.getAliasList(audioPacket.getIdentifierCollection().getAliasListConfiguration());

                if(aliasList != null)
                {
                    mAliasListMap.put(audioPacket.getAudioChannelId(), aliasList);
                    mAliasListRetrievedSet.add(audioPacket.getAudioChannelId());
                }
            }

            if(aliasList != null)
            {
                audioPacket.setRecordable(aliasList.isRecordable(audioPacket.getIdentifierCollection()));
                audioPacket.addBroadcastChannels(aliasList.getBroadcastChannels(audioPacket.getIdentifierCollection()));
                audioPacket.setMonitoringPriority(aliasList.getAudioPlaybackPriority(audioPacket.getIdentifierCollection()));
            }
        }
    }
}
