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
import util.ThreadPool;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RecorderManager implements Listener<AudioPacket>
{
    private static final Logger mLog = LoggerFactory.getLogger(RecorderManager.class);

    public static final int AUDIO_SAMPLE_RATE = 8000;
    public static final long IDLE_RECORDER_REMOVAL_THRESHOLD = 6000; //6 seconds

    private Map<String,RealBufferWaveRecorder> mRecorders = new HashMap<>();
    private ScheduledFuture<?> mRecorderMonitorFuture;

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
        mRecorderMonitorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(new RecorderMonitor(), 2,
            2, TimeUnit.SECONDS);
    }

    /**
     * Prepares this class for shutdown
     */
    public void dispose()
    {
        if(mRecorderMonitorFuture != null)
        {
            mRecorderMonitorFuture.cancel(true);
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
            String identifier = audioPacket.getMetadata().getUniqueIdentifier();

            synchronized(mRecorders)
            {
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
     * Monitors currently running recorders and removes/closes any recorders that are idle more than 6 seconds
     */
    public class RecorderMonitor implements Runnable
    {
        @Override
        public void run()
        {
            synchronized(mRecorders)
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
        }
    }
}
