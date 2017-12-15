/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2017 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package record;

import audio.AudioPacket;
import channel.metadata.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import record.wave.ComplexBufferWaveRecorder;
import record.wave.RealBufferWaveRecorder;
import sample.Listener;
import sample.OverflowableTransferQueue;
import sample.real.IOverflowListener;
import util.ThreadPool;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RecorderManager implements Listener<AudioPacket>
{
    private static final Logger mLog = LoggerFactory.getLogger(RecorderManager.class);

    public static final int AUDIO_SAMPLE_RATE = 8000;
    public static final long IDLE_RECORDER_REMOVAL_THRESHOLD = 6000; //6 seconds

    private Map<String,RealBufferWaveRecorder> mRecorders = new HashMap<>();
    private OverflowableTransferQueue<AudioPacket> mAudioPacketQueue = new OverflowableTransferQueue<>(1000, 100);
    private ScheduledFuture<?> mBufferProcessorFuture;

    private boolean mCanStartNewRecorders = true;

    /**
     * Audio recording manager.  Monitors stream of audio packets produced by decoding channels and automatically starts
     * audio recorders when the channel's metadata designates a call as recordable.  Routes call audio to each recorder
     * based on audio packet metadata.  Recorders are shutdown when the channel sends an end-call audio packet
     * indicating that the call is complete.  A separate recording monitor periodically checks for idled recorders to
     * be stopped for cases when the channel fails to send an end-call audio packet.
     */
    public RecorderManager()
    {
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
     * @param audioPacket to process
     */
    @Override
    public void receive(AudioPacket audioPacket)
    {
        if(audioPacket.hasMetadata() && audioPacket.getMetadata().isRecordable())
        {
            mAudioPacketQueue.offer(audioPacket);
        }
    }

    /**
     * Process any queued audio buffers and dispatch them to the audio recorders
     */
    private void processBuffers()
    {
        List<AudioPacket> audioPackets = new ArrayList<>();

        mAudioPacketQueue.drainTo(audioPackets, 50);

        while(!audioPackets.isEmpty())
        {
            for(AudioPacket audioPacket: audioPackets)
            {
                String identifier = audioPacket.getMetadata().getUniqueIdentifier();

                if(mRecorders.containsKey(identifier))
                {
                    RealBufferWaveRecorder recorder = mRecorders.get(identifier);

                    if(audioPacket.getType() == AudioPacket.Type.AUDIO)
                    {
                        recorder.receive(audioPacket.getAudioBuffer());
                    }
                    else if(audioPacket.getType() == AudioPacket.Type.END)
                    {
                        RealBufferWaveRecorder finished = mRecorders.remove(identifier);
                        finished.stop();
                    }
                }
                else if(audioPacket.getType() == AudioPacket.Type.AUDIO)
                {
                    if(mCanStartNewRecorders)
                    {
                        String filePrefix = getFilePrefix(audioPacket);

                        RealBufferWaveRecorder recorder = null;

                        try
                        {
                            recorder = new RealBufferWaveRecorder(AUDIO_SAMPLE_RATE, filePrefix);

                            recorder.start(ThreadPool.SCHEDULED);

                            recorder.receive(audioPacket.getAudioBuffer());
                            mRecorders.put(identifier, recorder);
                        }
                        catch(Exception ioe)
                        {
                            mCanStartNewRecorders = false;

                            mLog.error("Error attempting to start new audio wave recorder. All (future) audio recording " +
                                "is disabled", ioe);

                            if(recorder != null)
                            {
                                recorder.stop();
                            }
                        }
                    }
                }
            }

            audioPackets.clear();
            mAudioPacketQueue.drainTo(audioPackets, 50);
        }
    }

    /**
     * Removes recorders that have not received any new audio buffers in the last 6 seconds.
     */
    private void removeIdleRecorders()
    {
        Iterator<Map.Entry<String,RealBufferWaveRecorder>> it = mRecorders.entrySet().iterator();

        while(it.hasNext())
        {
            Map.Entry<String,RealBufferWaveRecorder> entry = it.next();

            if(entry.getValue().getLastBufferReceived() + IDLE_RECORDER_REMOVAL_THRESHOLD < System.currentTimeMillis())
            {
                mLog.info("Removing idle recorder [" + entry.getKey() + "]");
                it.remove();
                entry.getValue().stop();
            }
        }
    }

    /**
     * Constructs a file name and path for an audio recording
     */
    private String getFilePrefix(AudioPacket packet)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(SystemProperties.getInstance().getApplicationFolder("recordings"));

        sb.append(File.separator);

        Metadata metadata = packet.getMetadata();

        sb.append(metadata.hasChannelConfigurationSystem() ? metadata.getChannelConfigurationSystem() + "_" : "");
        sb.append(metadata.hasChannelConfigurationSite() ? metadata.getChannelConfigurationSite() + "_" : "");
        sb.append(metadata.hasChannelConfigurationName() ? metadata.getChannelConfigurationName() + "_" : "");

        if(metadata.getPrimaryAddressTo().hasIdentifier())
        {
            sb.append("_TO_").append(metadata.getPrimaryAddressTo().getIdentifier());

            if(metadata.getPrimaryAddressFrom().hasIdentifier())
            {
                sb.append("_FROM_").append(metadata.getPrimaryAddressFrom().getIdentifier());
            }
        }

        return sb.toString();
    }

    /**
     * Constructs a baseband recorder for use in a processing chain.
     */
    public ComplexBufferWaveRecorder getBasebandRecorder(String channelName)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(SystemProperties.getInstance().getApplicationFolder("recordings"));
        sb.append(File.separator).append(channelName).append("_baseband");

        return new ComplexBufferWaveRecorder(AUDIO_SAMPLE_RATE, sb.toString());
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
