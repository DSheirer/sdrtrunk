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

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.dsp.filter.channelizer.ContinuousReusableBufferProcessor;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import io.github.dsheirer.sample.buffer.ReusableBufferBroadcaster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Processes audio packets from all decoder channels, assigns alias attributes and distributes packets
 * for playback, streaming and recording.
 */
public class AudioPacketManager implements Listener<ReusableAudioPacket>
{
    private final static Logger mLog = LoggerFactory.getLogger(AudioPacketManager.class);
    private AudioMetadataProcessor mAudioMetadataProcessor;
    private ReusableBufferBroadcaster<ReusableAudioPacket> mBroadcaster = new ReusableBufferBroadcaster<>();
    private ContinuousReusableBufferProcessor<ReusableAudioPacket> mBufferProcessor;

    public AudioPacketManager(AliasModel aliasModel)
    {
        mAudioMetadataProcessor = new AudioMetadataProcessor(aliasModel);
        mBufferProcessor = new ContinuousReusableBufferProcessor<>(10000, 500);
        mBufferProcessor.setListener(new Listener<List<ReusableAudioPacket>>()
        {
            @Override
            public void receive(List<ReusableAudioPacket> reusableAudioPackets)
            {
                for(ReusableAudioPacket audioPacket: reusableAudioPackets)
                {
                    mAudioMetadataProcessor.process(audioPacket);
                    mBroadcaster.broadcast(audioPacket);
                }
            }
        });
    }

    public void start()
    {
        mLog.info("Audio packet manager started");
        mBufferProcessor.start();
    }

    public void stop()
    {
        mLog.info("Audio packet manager stopped");
        mBufferProcessor.stop();
    }

    /**
     * Primary processing method for receiving audio packets from all processing channels.
     */
    @Override
    public void receive(ReusableAudioPacket audioPacket)
    {
        mBufferProcessor.receive(audioPacket);
    }

    /**
     * Adds the listener to receive enriched audio packets from this manager
     */
    public void addListener(Listener<ReusableAudioPacket> listener)
    {
        mBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving enriched audio packets from this manager
     */
    public void removeListener(Listener<ReusableAudioPacket> listener)
    {
        mBroadcaster.removeListener(listener);
    }
}
