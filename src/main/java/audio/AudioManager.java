/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
package audio;

import audio.AudioEvent.Type;
import audio.output.AudioOutput;
import audio.output.MonoAudioOutput;
import audio.output.StereoAudioOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import sample.Broadcaster;
import sample.Listener;
import source.mixer.MixerChannel;
import source.mixer.MixerChannelConfiguration;
import source.mixer.MixerManager;
import util.ThreadPool;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AudioManager implements Listener<AudioPacket>, IAudioController
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioManager.class);

    public static final int AUDIO_TIMEOUT = 1000; //1 second

    public static final String AUDIO_CHANNELS_PROPERTY = "audio.manager.channels";
    public static final String AUDIO_MIXER_PROPERTY = "audio.manager.mixer";

    public static final AudioEvent CONFIGURATION_CHANGE_STARTED =
        new AudioEvent(Type.AUDIO_CONFIGURATION_CHANGE_STARTED, null);

    public static final AudioEvent CONFIGURATION_CHANGE_COMPLETE =
        new AudioEvent(Type.AUDIO_CONFIGURATION_CHANGE_COMPLETE, null);

    private LinkedTransferQueue<AudioPacket> mAudioPacketQueue = new LinkedTransferQueue<>();
    private Map<Integer,AudioOutputConnection> mChannelConnectionMap = new HashMap<>();
    private List<AudioOutputConnection> mAudioOutputConnections = new ArrayList<>();
    private AudioOutputConnection mLowestPriorityConnection;
    private int mAvailableConnectionCount;

    private Map<String,AudioOutput> mAudioOutputMap = new HashMap<>();

    private Broadcaster<AudioEvent> mControllerBroadcaster = new Broadcaster<>();

    private ScheduledFuture<?> mProcessingTask;
    private MixerManager mMixerManager;
    private MixerChannelConfiguration mMixerChannelConfiguration;


    /**
     * Processes all audio produced by the decoding channels and routes audio
     * packets to any combination of outputs based on any alias audio routing
     * options specified by the user.
     */
    public AudioManager(MixerManager mixerManager)
    {
        mMixerManager = mixerManager;

        loadSettings();
    }

    /**
     * Loads the saved mixer configuration or a default configuration for audio playback.
     */
    private void loadSettings()
    {
        MixerChannelConfiguration configuration = null;

        SystemProperties properties = SystemProperties.getInstance();

        Mixer defaultMixer = AudioSystem.getMixer(null);

        String mixer = properties.get(AUDIO_MIXER_PROPERTY, defaultMixer.getMixerInfo().getName());

        String channels = properties.get(AUDIO_CHANNELS_PROPERTY, MixerChannel.MONO.name());

        MixerChannelConfiguration[] mixerConfigurations = mMixerManager.getOutputMixers();

        for(MixerChannelConfiguration mixerConfig : mixerConfigurations)
        {
            if(mixerConfig.matches(mixer, channels))
            {
                configuration = mixerConfig;
            }
        }

        if(configuration == null)
        {
            configuration = getDefaultConfiguration();
        }

        try
        {
            setMixerChannelConfiguration(configuration, false);
        }
        catch(Exception e)
        {
            mLog.error("Couldn't set stored audio mixer/channel configuration - using default", e);

            try
            {
                setMixerChannelConfiguration(getDefaultConfiguration());
            }
            catch(Exception e2)
            {
                mLog.error("Couldn't set default audio mixer/channel configuration - no audio will be available", e2);
            }
        }
    }

    /**
     * Creates a default audio playback configuration with a mono audio playback channel.
     */
    private MixerChannelConfiguration getDefaultConfiguration()
    {
        /* Use the system default mixer and mono channel as default startup */
        Mixer defaultMixer = AudioSystem.getMixer(null);

        return new MixerChannelConfiguration(defaultMixer, MixerChannel.MONO);
    }

    public void dispose()
    {
        if(mProcessingTask != null)
        {
            mProcessingTask.cancel(true);
        }

        mAudioPacketQueue.clear();

        mProcessingTask = null;

        mChannelConnectionMap.clear();

        for(AudioOutputConnection connection : mAudioOutputConnections)
        {
            connection.dispose();
        }

        mAudioOutputConnections.clear();
    }

    /**
     * Primary ingest point for audio produced by all decoding channels, for distribution to audio playback devices.
     */
    @Override
    public synchronized void receive(AudioPacket packet)
    {
        mAudioPacketQueue.add(packet);
    }

    /**
     * Checks each audio channel assignment and disconnects any inactive connections
     */
    private void disconnectInactiveChannelAssignments()
    {
        boolean changed = false;

        for(AudioOutputConnection connection : mAudioOutputConnections)
        {
            if(connection.isInactive() && mChannelConnectionMap.containsKey(connection.getChannelMetadataID()))
            {
                mChannelConnectionMap.remove(connection.getChannelMetadataID());
                connection.disconnect();
                mAvailableConnectionCount++;
                changed = true;
            }
        }

        if(changed)
        {
            updateLowestPriorityAssignment();
        }
    }

    /**
     * Identifies the lowest priority channel connection where the a higher value indicates a lower priority.
     */
    private void updateLowestPriorityAssignment()
    {
        mLowestPriorityConnection = null;

        for(AudioOutputConnection connection : mAudioOutputConnections)
        {
            if(connection.isConnected() &&
                (mLowestPriorityConnection == null || mLowestPriorityConnection.getPriority() < connection.getPriority()))
            {
                mLowestPriorityConnection = connection;
            }
        }
    }

    /**
     * Configures audio playback to use the configuration specified in the entry argument.
     *
     * @param entry to use in configuring the audio playback setup.
     * @throws AudioException if there is an error
     */
    @Override
    public void setMixerChannelConfiguration(MixerChannelConfiguration entry) throws AudioException
    {
        setMixerChannelConfiguration(entry, true);
    }

    /**
     * Configures audio playback to use the configuration specified in the entry argument.
     *
     * @param entry to use in configuring the audio playback setup.
     * @param saveSettings to save the audio playback configuration settings in the properties file.
     * @throws AudioException if there is an error
     */
    public void setMixerChannelConfiguration(MixerChannelConfiguration entry, boolean saveSettings) throws AudioException
    {
        if(entry != null && (entry.getMixerChannel() == MixerChannel.MONO || entry.getMixerChannel() == MixerChannel.STEREO))
        {
            mControllerBroadcaster.broadcast(CONFIGURATION_CHANGE_STARTED);

            if(mProcessingTask != null)
            {
                mProcessingTask.cancel(true);
            }

            disposeCurrentConfiguration();

            switch(entry.getMixerChannel())
            {
                case MONO:
                    AudioOutput mono = new MonoAudioOutput(entry.getMixer());
                    mAudioOutputConnections.add(new AudioOutputConnection(mono));
                    mAvailableConnectionCount++;
                    mAudioOutputMap.put(mono.getChannelName(), mono);
                    break;
                case STEREO:
                    AudioOutput left = new StereoAudioOutput(entry.getMixer(), MixerChannel.LEFT);
                    mAudioOutputConnections.add(new AudioOutputConnection(left));
                    mAvailableConnectionCount++;
                    mAudioOutputMap.put(left.getChannelName(), left);

                    AudioOutput right = new StereoAudioOutput(entry.getMixer(), MixerChannel.RIGHT);
                    mAudioOutputConnections.add(new AudioOutputConnection(right));
                    mAvailableConnectionCount++;
                    mAudioOutputMap.put(right.getChannelName(), right);
                    break;
                default:
                    throw new AudioException("Unsupported mixer channel "
                        + "configuration: " + entry.getMixerChannel());
            }

            mProcessingTask = ThreadPool.SCHEDULED.scheduleAtFixedRate(new AudioPacketProcessor(),
                0, 15, TimeUnit.MILLISECONDS);

            mControllerBroadcaster.broadcast(CONFIGURATION_CHANGE_COMPLETE);

            if(saveSettings)
            {
                SystemProperties properties = SystemProperties.getInstance();
                properties.set(AUDIO_MIXER_PROPERTY, entry.getMixer().getMixerInfo().getName());
                properties.set(AUDIO_CHANNELS_PROPERTY, entry.getMixerChannel().name());
            }
        }
    }

    /**
     * Clears all channel assignments and terminates all audio outputs in preparation for complete shutdown or change
     * to another mixer/channel configuration
     */
    private void disposeCurrentConfiguration()
    {
        mChannelConnectionMap.clear();

        for(AudioOutputConnection connection : mAudioOutputConnections)
        {
            connection.dispose();
        }

        mAvailableConnectionCount = 0;

        mAudioOutputConnections.clear();

        mAudioOutputMap.clear();

        mLowestPriorityConnection = null;
    }

    /**
     * Current audio playback mixer channel configuration setting.
     */
    @Override
    public MixerChannelConfiguration getMixerChannelConfiguration() throws AudioException
    {
        return mMixerChannelConfiguration;
    }

    /**
     * List of audio outputs available for the current mixer channel configuration
     */
    @Override
    public List<AudioOutput> getAudioOutputs()
    {
        List<AudioOutput> outputs = new ArrayList<>(mAudioOutputMap.values());

        Collections.sort(outputs, new Comparator<AudioOutput>()
        {
            @Override
            public int compare(AudioOutput first, AudioOutput second)
            {
                return first.getChannelName().compareTo(second.getChannelName());
            }
        });

        return outputs;
    }

    /**
     * Adds an audio event listener to receive audio event notifications.
     */
    @Override
    public void addControllerListener(Listener<AudioEvent> listener)
    {
        mControllerBroadcaster.addListener(listener);
    }

    /**
     * Removes an audio event listener from receiving audio event notifications.
     */
    @Override
    public void removeControllerListener(Listener<AudioEvent> listener)
    {
        mControllerBroadcaster.removeListener(listener);
    }

    /**
     * Returns an audio output connection for the packet if one is available, or overrides an existing lower priority
     * connection. Returns null if no connection is available for the audio packet.
     *
     * @param audioPacket from a decoding channel source
     * @return an audio output connection or null
     */
    private AudioOutputConnection getConnection(AudioPacket audioPacket)
    {
        int channelMetadataID = audioPacket.getMetadata().getMetadataID();

        //Use an existing connection
        if(mChannelConnectionMap.containsKey(channelMetadataID))
        {
            return mChannelConnectionMap.get(channelMetadataID);
        }

        //Connect to an unused, available connection
        if(mAvailableConnectionCount > 0)
        {
            for(AudioOutputConnection connection : mAudioOutputConnections)
            {
                if(connection.isDisconnected())
                {
                    connection.connect(channelMetadataID, audioPacket.getMetadata().getAudioPriority());
                    mChannelConnectionMap.put(channelMetadataID, connection);
                    mAvailableConnectionCount--;
                    return connection;
                }
            }
        }
        //Preempt an existing lower priority connection and connect when this is a higher priority packet
        else
        {
            int priority = audioPacket.getMetadata().getAudioPriority();

            AudioOutputConnection connection = mLowestPriorityConnection;

            if(connection != null && priority < connection.getPriority())
            {
                mChannelConnectionMap.remove(connection.getChannelMetadataID());

                connection.connect(channelMetadataID, priority);

                mChannelConnectionMap.put(channelMetadataID, connection);

                return connection;
            }
        }

        return null;
    }

    public class AudioPacketProcessor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                disconnectInactiveChannelAssignments();

                if(mAudioPacketQueue != null)
                {
                    List<AudioPacket> packets = new ArrayList<AudioPacket>();

                    mAudioPacketQueue.drainTo(packets);

                    for(AudioPacket packet : packets)
                    {
                        /* Don't process any packet's marked as do not monitor */
                        if(!packet.getMetadata().isDoNotMonitor() && packet.getType() == AudioPacket.Type.AUDIO)
                        {
                            AudioOutputConnection connection = getConnection(packet);

                            if(connection != null)
                            {
                                connection.receive(packet);
                            }
                        }
                    }
                }
            }
            catch(Exception e)
            {
                mLog.error("Encountered error while processing audio packets", e);
            }
        }
    }

    /**
     * Audio output connection manages a connection between a source and an audio
     * output and maintains current state information about the audio activity
     * received form the source.
     */
    public class AudioOutputConnection
    {
        private static final int DISCONNECTED = -1;

        private AudioOutput mAudioOutput;
        private int mPriority = 0;
        private int mChannelMetadataID = DISCONNECTED;

        public AudioOutputConnection(AudioOutput audioOutput)
        {
            mAudioOutput = audioOutput;
        }

        public void receive(AudioPacket packet)
        {
            if(packet.hasMetadata() && packet.getMetadata().getMetadataID() == mChannelMetadataID)
            {
                int priority = packet.getMetadata().getAudioPriority();

                if(mPriority != priority)
                {
                    mPriority = priority;
                    updateLowestPriorityAssignment();
                }

                if(mAudioOutput != null)
                {
                    mAudioOutput.receive(packet);
                }
            }
            else
            {
                if(packet.hasMetadata())
                {
                    mLog.error("Received audio packet from channel metadata [" + packet.getMetadata().getMetadataID() +
                        "] however this assignment is currently connected to metadata [" + mChannelMetadataID + "]");
                }
                else
                {
                    mLog.error("Received audio packet with no metadata - cannot route audio packet");
                }
            }
        }

        /**
         * Terminates the audio output and prepares this connection for disposal
         */
        public void dispose()
        {
            mAudioOutput.dispose();
            mAudioOutput = null;
        }

        /**
         * Indicates if this assignment is currently disconnected from a channel source
         */
        public boolean isDisconnected()
        {
            return mChannelMetadataID == DISCONNECTED;
        }

        /**
         * Indicates if this assignment is currently connected to a channel source
         */
        public boolean isConnected()
        {
            return !isDisconnected();
        }

        /**
         * Connects this assignment to the indicated source so that audio
         * packets from this source can be sent to the audio output
         */
        public void connect(int source, int priority)
        {
            mChannelMetadataID = source;
            mPriority = priority;
            updateLowestPriorityAssignment();

            mAudioOutput.updateTimestamp();
        }

        /**
         * Currently connected source or -1 if disconnected
         */
        public int getChannelMetadataID()
        {
            return mChannelMetadataID;
        }

        /**
         * Disconnects this assignment from the source and prevents any audio
         * from being routed to the audio output until another source is assigned
         */
        public void disconnect()
        {
            mChannelMetadataID = DISCONNECTED;
            mPriority = 0;
        }

        /**
         * Indicates if audio output is current inactive, meaning that the
         * audio output hasn't recently processed any audio packets.
         */
        public boolean isInactive()
        {
            return (mAudioOutput.getLastActivityTimestamp() + AUDIO_TIMEOUT) < System.currentTimeMillis();
        }

        public int getPriority()
        {
            return mPriority;
        }
    }
}
