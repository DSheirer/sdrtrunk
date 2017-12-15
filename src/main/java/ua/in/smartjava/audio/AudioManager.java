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
package ua.in.smartjava.audio;

import ua.in.smartjava.audio.AudioEvent.Type;
import ua.in.smartjava.audio.output.AudioOutput;
import ua.in.smartjava.audio.output.MonoAudioOutput;
import ua.in.smartjava.audio.output.StereoAudioOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.in.smartjava.properties.SystemProperties;
import ua.in.smartjava.sample.Broadcaster;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.source.mixer.MixerChannel;
import ua.in.smartjava.source.mixer.MixerChannelConfiguration;
import ua.in.smartjava.source.mixer.MixerManager;
import ua.in.smartjava.util.ThreadPool;

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

    public static final String AUDIO_CHANNELS_PROPERTY = "ua.in.smartjava.audio.manager.channels";
    public static final String AUDIO_MIXER_PROPERTY = "ua.in.smartjava.audio.manager.mixer";

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
     * Processes all ua.in.smartjava.audio produced by the decoding channels and routes ua.in.smartjava.audio
     * packets to any combination of outputs based on any ua.in.smartjava.alias ua.in.smartjava.audio routing
     * options specified by the user.
     */
    public AudioManager(MixerManager mixerManager)
    {
        mMixerManager = mixerManager;

        loadSettings();
    }

    /**
     * Loads the saved mixer configuration or a default configuration for ua.in.smartjava.audio playback.
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
            mLog.error("Couldn't set stored ua.in.smartjava.audio mixer/ua.in.smartjava.channel configuration - using default", e);

            try
            {
                setMixerChannelConfiguration(getDefaultConfiguration());
            }
            catch(Exception e2)
            {
                mLog.error("Couldn't set default ua.in.smartjava.audio mixer/ua.in.smartjava.channel configuration - no ua.in.smartjava.audio will be available", e2);
            }
        }
    }

    /**
     * Creates a default ua.in.smartjava.audio playback configuration with a mono ua.in.smartjava.audio playback ua.in.smartjava.channel.
     */
    private MixerChannelConfiguration getDefaultConfiguration()
    {
        /* Use the system default mixer and mono ua.in.smartjava.channel as default startup */
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
     * Primary ingest point for ua.in.smartjava.audio produced by all decoding channels, for distribution to ua.in.smartjava.audio playback devices.
     */
    @Override
    public synchronized void receive(AudioPacket packet)
    {
        mAudioPacketQueue.add(packet);
    }

    /**
     * Checks each ua.in.smartjava.audio ua.in.smartjava.channel assignment and disconnects any inactive connections
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
     * Identifies the lowest priority ua.in.smartjava.channel connection where the a higher value indicates a lower priority.
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
     * Configures ua.in.smartjava.audio playback to use the configuration specified in the entry argument.
     *
     * @param entry to use in configuring the ua.in.smartjava.audio playback setup.
     * @throws AudioException if there is an error
     */
    @Override
    public void setMixerChannelConfiguration(MixerChannelConfiguration entry) throws AudioException
    {
        setMixerChannelConfiguration(entry, true);
    }

    /**
     * Configures ua.in.smartjava.audio playback to use the configuration specified in the entry argument.
     *
     * @param entry to use in configuring the ua.in.smartjava.audio playback setup.
     * @param saveSettings to save the ua.in.smartjava.audio playback configuration settings in the ua.in.smartjava.properties file.
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
                    throw new AudioException("Unsupported mixer ua.in.smartjava.channel "
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
     * Clears all ua.in.smartjava.channel assignments and terminates all ua.in.smartjava.audio outputs in preparation for complete shutdown or change
     * to another mixer/ua.in.smartjava.channel configuration
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
     * Current ua.in.smartjava.audio playback mixer ua.in.smartjava.channel configuration setting.
     */
    @Override
    public MixerChannelConfiguration getMixerChannelConfiguration() throws AudioException
    {
        return mMixerChannelConfiguration;
    }

    /**
     * List of ua.in.smartjava.audio outputs available for the current mixer ua.in.smartjava.channel configuration
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
     * Adds an ua.in.smartjava.audio event listener to receive ua.in.smartjava.audio event notifications.
     */
    @Override
    public void addControllerListener(Listener<AudioEvent> listener)
    {
        mControllerBroadcaster.addListener(listener);
    }

    /**
     * Removes an ua.in.smartjava.audio event listener from receiving ua.in.smartjava.audio event notifications.
     */
    @Override
    public void removeControllerListener(Listener<AudioEvent> listener)
    {
        mControllerBroadcaster.removeListener(listener);
    }

    /**
     * Returns an ua.in.smartjava.audio output connection for the packet if one is available, or overrides an existing lower priority
     * connection. Returns null if no connection is available for the ua.in.smartjava.audio packet.
     *
     * @param audioPacket from a decoding ua.in.smartjava.channel ua.in.smartjava.source
     * @return an ua.in.smartjava.audio output connection or null
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
                mLog.error("Encountered error while processing ua.in.smartjava.audio packets", e);
            }
        }
    }

    /**
     * Audio output connection manages a connection between a ua.in.smartjava.source and an ua.in.smartjava.audio
     * output and maintains current state information about the ua.in.smartjava.audio activity
     * received form the ua.in.smartjava.source.
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
                    mLog.error("Received ua.in.smartjava.audio packet from ua.in.smartjava.channel metadata [" + packet.getMetadata().getMetadataID() +
                        "] however this assignment is currently connected to metadata [" + mChannelMetadataID + "]");
                }
                else
                {
                    mLog.error("Received ua.in.smartjava.audio packet with no metadata - cannot route ua.in.smartjava.audio packet");
                }
            }
        }

        /**
         * Terminates the ua.in.smartjava.audio output and prepares this connection for disposal
         */
        public void dispose()
        {
            mAudioOutput.dispose();
            mAudioOutput = null;
        }

        /**
         * Indicates if this assignment is currently disconnected from a ua.in.smartjava.channel ua.in.smartjava.source
         */
        public boolean isDisconnected()
        {
            return mChannelMetadataID == DISCONNECTED;
        }

        /**
         * Indicates if this assignment is currently connected to a ua.in.smartjava.channel ua.in.smartjava.source
         */
        public boolean isConnected()
        {
            return !isDisconnected();
        }

        /**
         * Connects this assignment to the indicated ua.in.smartjava.source so that ua.in.smartjava.audio
         * packets from this ua.in.smartjava.source can be sent to the ua.in.smartjava.audio output
         */
        public void connect(int source, int priority)
        {
            mChannelMetadataID = source;
            mPriority = priority;
            updateLowestPriorityAssignment();

            mAudioOutput.updateTimestamp();
        }

        /**
         * Currently connected ua.in.smartjava.source or -1 if disconnected
         */
        public int getChannelMetadataID()
        {
            return mChannelMetadataID;
        }

        /**
         * Disconnects this assignment from the ua.in.smartjava.source and prevents any ua.in.smartjava.audio
         * from being routed to the ua.in.smartjava.audio output until another ua.in.smartjava.source is assigned
         */
        public void disconnect()
        {
            mChannelMetadataID = DISCONNECTED;
            mPriority = 0;
        }

        /**
         * Indicates if ua.in.smartjava.audio output is current inactive, meaning that the
         * ua.in.smartjava.audio output hasn't recently processed any ua.in.smartjava.audio packets.
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
