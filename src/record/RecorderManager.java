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
import audio.metadata.Metadata;
import audio.metadata.MetadataType;
import controller.ThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import record.wave.ComplexBufferWaveRecorder;
import record.wave.RealBufferWaveRecorder;
import sample.Listener;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class RecorderManager implements Listener<AudioPacket>
{
    private static final Logger mLog = LoggerFactory.getLogger(RecorderManager.class);

    public static final int AUDIO_SAMPLE_RATE = 8000;

    private Map<String, RealBufferWaveRecorder> mRecorders = new HashMap<>();

    private ThreadPoolManager mThreadPoolManager;

    private boolean mCanStartNewRecorders = true;

    /**
     * Manages all audio recording for all processing channels. Reconstructs audio streams and distributes channel
     * audio to each of the recorders, starting and stopping the recorders as needed.
     */
    public RecorderManager(ThreadPoolManager threadPoolManager)
    {
        mThreadPoolManager = threadPoolManager;
    }

    public void dispose()
    {
        mThreadPoolManager = null;
    }

    @Override
    public void receive(AudioPacket audioPacket)
    {
        if(audioPacket.hasAudioMetadata() &&
            audioPacket.getAudioMetadata().isRecordable())
        {
            String identifier = audioPacket.getAudioMetadata().getIdentifier();

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

                        recorder.start(mThreadPoolManager.getScheduledExecutorService());

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

    /**
     * Constructs a file name and path for an audio recording
     */
    private String getFilePrefix(AudioPacket packet)
    {
        StringBuilder sb = new StringBuilder();

        sb.append(SystemProperties.getInstance().getApplicationFolder("recordings"));

        sb.append(File.separator);

        Metadata systemMetadata = packet.getAudioMetadata().getMetadata(MetadataType.SYSTEM);

        sb.append(systemMetadata != null ? systemMetadata.getValue() : "UNKNOWN_SYSTEM");

        Metadata toMetadata = packet.getAudioMetadata().getMetadata(MetadataType.TO);

        if(toMetadata != null)
        {
            sb.append("_TO_").append(toMetadata.getValue());

            Metadata fromMetadata = packet.getAudioMetadata().getMetadata(MetadataType.FROM);

            if(fromMetadata != null)
            {
                sb.append("_FROM_").append(fromMetadata.getValue());
            }
        }
        else
        {
            Metadata siteMetadata = packet.getAudioMetadata().getMetadata(MetadataType.SITE_ID);

            if(siteMetadata != null)
            {
                sb.append("_").append(siteMetadata.getValue());
            }

            Metadata channelMetadata = packet.getAudioMetadata().getMetadata(MetadataType.CHANNEL_NAME);

            if(channelMetadata != null)
            {
                sb.append("_").append(channelMetadata.getValue());
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
}
