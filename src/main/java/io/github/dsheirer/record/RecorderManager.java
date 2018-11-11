/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
package io.github.dsheirer.record;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.string.StringIdentifier;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.record.wave.AudioPacketWaveRecorder;
import io.github.dsheirer.record.wave.ComplexBufferWaveRecorder;
import io.github.dsheirer.record.wave.WaveMetadata;
import io.github.dsheirer.sample.IOverflowListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.OverflowableTransferQueue;
import io.github.dsheirer.sample.buffer.ReusableAudioPacket;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RecorderManager implements Listener<ReusableAudioPacket>
{
    private static final Logger mLog = LoggerFactory.getLogger(RecorderManager.class);

    public static final float BASEBAND_SAMPLE_RATE = 25000.0f; //Default sample rate - source can override
    public static final long IDLE_RECORDER_REMOVAL_THRESHOLD = 6000; //6 seconds

    private Map<Integer,AudioPacketWaveRecorder> mRecorders = new HashMap<>();
    private OverflowableTransferQueue<ReusableAudioPacket> mAudioPacketQueue = new OverflowableTransferQueue<>(1000, 100);
    private List<ReusableAudioPacket> mAudioPackets = new ArrayList<>();
    private ScheduledFuture<?> mBufferProcessorFuture;
    private AliasModel mAliasModel;

    private boolean mCanStartNewRecorders = true;

    /**
     * Audio recording manager.  Monitors stream of audio packets produced by decoding channels and automatically starts
     * audio recorders when the channel's metadata designates a call as recordable.  Routes call audio to each recorder
     * based on audio packet metadata.  Recorders are shutdown when the channel sends an end-call audio packet
     * indicating that the call is complete.  A separate recording monitor periodically checks for idled recorders to
     * be stopped for cases when the channel fails to send an end-call audio packet.
     */
    public RecorderManager(AliasModel aliasModel)
    {
        mAliasModel = aliasModel;
        mAudioPacketQueue.setOverflowListener(new IOverflowListener()
        {
            @Override
            public void sourceOverflow(boolean overflow)
            {
                if(overflow)
                {
                    mLog.warn("overflow - audio packets will be dropped until recording catches up");
                }
                else
                {
                    mLog.info("audio recorder packet processing has returned to normal");
                }
            }
        });

        mBufferProcessorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new BufferProcessor(), 0,
            1, TimeUnit.SECONDS);
    }

    /**
     * Prepares this class for shutdown
     */
    public void dispose()
    {
        if(mBufferProcessorFuture != null)
        {
            mBufferProcessorFuture.cancel(true);
        }
    }

    /**
     * Primary ingest point for audio packets from all decoding channels
     *
     * @param audioPacket to process
     */
    @Override
    public void receive(ReusableAudioPacket audioPacket)
    {
        if(audioPacket.hasIdentifierCollection() && audioPacket.isRecordable())
        {
            mAudioPacketQueue.offer(audioPacket);
        }
        else
        {
            audioPacket.decrementUserCount();
        }
    }

    /**
     * Process any queued audio buffers and dispatch them to the audio recorders
     */
    private void processBuffers()
    {
        mAudioPacketQueue.drainTo(mAudioPackets, 50);

        while(!mAudioPackets.isEmpty())
        {
            for(ReusableAudioPacket audioPacket : mAudioPackets)
            {
                int audioChannelId = audioPacket.getAudioChannelId();

                if(mRecorders.containsKey(audioChannelId))
                {
                    AudioPacketWaveRecorder recorder = mRecorders.get(audioChannelId);

                    if(audioPacket.getType() == ReusableAudioPacket.Type.AUDIO)
                    {
                        audioPacket.incrementUserCount();
                        recorder.receive(audioPacket);
                    }
                    else if(audioPacket.getType() == ReusableAudioPacket.Type.END)
                    {
                        AudioPacketWaveRecorder finished = mRecorders.remove(audioChannelId);
                        stopRecorder(finished);
                    }
                }
                else if(audioPacket.getType() == ReusableAudioPacket.Type.AUDIO)
                {
                    if(mCanStartNewRecorders)
                    {
                        AudioPacketWaveRecorder recorder = null;

                        try
                        {
                            recorder = new AudioPacketWaveRecorder(getTemporaryFilePath(audioPacket));

                            recorder.start();

                            audioPacket.incrementUserCount();
                            recorder.receive(audioPacket);
                            mRecorders.put(audioPacket.getAudioChannelId(), recorder);
                        }
                        catch(Exception ioe)
                        {
                            mCanStartNewRecorders = false;

                            mLog.error("Error attempting to start new audio wave recorder. All (future) audio recording " +
                                "is disabled", ioe);

                            if(recorder != null)
                            {
                                stopRecorder(recorder);
                            }
                        }
                    }
                }

                audioPacket.decrementUserCount();
            }

            mAudioPackets.clear();
            mAudioPacketQueue.drainTo(mAudioPackets, 50);
        }
    }

    private void stopRecorder(AudioPacketWaveRecorder recorder)
    {
        Path rename = getFinalFileName(recorder.getIdentifierCollection());
        AliasList aliasList = mAliasModel.getAliasList(recorder.getIdentifierCollection().getAliasListConfiguration());
        WaveMetadata waveMetadata = WaveMetadata.createFrom(recorder.getIdentifierCollection(), aliasList);
        recorder.stop(rename, waveMetadata);
    }

    /**
     * Removes recorders that have not received any new audio buffers in the last 6 seconds.
     */
    private void removeIdleRecorders()
    {
        Iterator<Map.Entry<Integer,AudioPacketWaveRecorder>> it = mRecorders.entrySet().iterator();

        while(it.hasNext())
        {
            Map.Entry<Integer,AudioPacketWaveRecorder> entry = it.next();

            if(entry.getValue().getLastBufferReceived() + IDLE_RECORDER_REMOVAL_THRESHOLD < System.currentTimeMillis())
            {
                mLog.info("Removing idle recorder [" + entry.getKey() + "]");
                it.remove();
                stopRecorder(entry.getValue());
            }
        }
    }

    private Path getTemporaryFilePath(ReusableAudioPacket packet)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(TimeStamp.getTimeStamp("-"));
        sb.append("_audio_channel_");
        sb.append(packet.getAudioChannelId());
        sb.append(".tmp");
        return getRecordingBasePath().resolve(sb.toString());
    }

    /**
     * Provides a formatted audio recording filename to use as the final audio filename.
     * @return
     */
    private Path getFinalFileName(IdentifierCollection identifierCollection)
    {
        StringBuilder sb = new StringBuilder();

        if(identifierCollection != null)
        {
            Identifier system = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SYSTEM, Role.ANY);

            if(system != null)
            {
                sb.append(((StringIdentifier)system).getValue()).append("_");
            }

            Identifier site = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.SITE, Role.ANY);

            if(site != null)
            {
                sb.append(((StringIdentifier)site).getValue()).append("_");
            }

            Identifier channel = identifierCollection.getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL, Role.ANY);

            if(channel != null)
            {
                sb.append(((StringIdentifier)channel).getValue()).append("_");
            }

            Identifier to = identifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.TO);

            if(to != null)
            {
                sb.append("_TO_").append(((IntegerIdentifier)to).getValue());
            }

            Identifier from = identifierCollection.getIdentifier(IdentifierClass.USER, Form.TALKGROUP, Role.FROM);

            if(from != null)
            {
                sb.append("_FROM_").append(((IntegerIdentifier)from).getValue());
            }
        }
        else
        {
            sb.append("audio_recording");
        }

        StringBuilder sbFinal = new StringBuilder();
        sbFinal.append(TimeStamp.getTimeStamp("_"));

        //Remove any illegal filename characters
        sbFinal.append(sb.toString().replaceAll("[^\\w.-]", "_"));
        sbFinal.append(".wav");

        return getRecordingBasePath().resolve(sbFinal.toString());
    }


    public Path getRecordingBasePath()
    {
        return SystemProperties.getInstance().getApplicationFolder("recordings");
    }

    /**
     * Constructs a baseband recorder for use in a processing chain.
     */
    public ComplexBufferWaveRecorder getBasebandRecorder(String channelName)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getRecordingBasePath());
        sb.append(File.separator).append(channelName).append("_baseband");

        return new ComplexBufferWaveRecorder(BASEBAND_SAMPLE_RATE, sb.toString());
    }

    /**
     * Processes queued audio packets and distributes to each of the audio recorders.  Removes any idle recorders
     * that have not been updated according to an idle threshold period
     */
    public class BufferProcessor implements Runnable
    {
        @Override
        public void run()
        {
            processBuffers();
            removeIdleRecorders();
        }
    }
}
