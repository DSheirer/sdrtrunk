/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;

public class BroadcastModel extends AbstractTableModel implements Listener<AudioRecording>
{
    private final static Logger mLog = LoggerFactory.getLogger(BroadcastModel.class);

    public static final String TEMPORARY_STREAM_DIRECTORY = "streaming";
    public static final String TEMPORARY_STREAM_FILE_SUFFIX = "temporary_streaming_file_";

    private static final String UNIQUE_NAME_REGEX = "(.*)\\((\\d*)\\)";

    public static final int COLUMN_BROADCAST_SERVER_TYPE = 0;
    public static final int COLUMN_STREAM_NAME = 1;
    public static final int COLUMN_BROADCASTER_STATUS = 2;
    public static final int COLUMN_BROADCASTER_QUEUE_SIZE = 3;
    public static final int COLUMN_BROADCASTER_STREAMED_COUNT = 4;
    public static final int COLUMN_BROADCASTER_AGED_OFF_COUNT = 5;
    public static final int COLUMN_BROADCASTER_ERROR_COUNT = 6;

    public static final String[] COLUMN_NAMES = new String[]
        {"Stream Type", "Name", "Status", "Queued", "Streamed/Uploaded", "Aged Off", "Upload Error"};

    private ObservableList<ConfiguredBroadcast> mConfiguredBroadcasts =
        FXCollections.observableArrayList(ConfiguredBroadcast.extractor());
    private List<AudioRecording> mRecordingQueue = new CopyOnWriteArrayList<>();
    private Map<Integer,AbstractAudioBroadcaster> mBroadcasterMap = new HashMap<>();
    private IconModel mIconModel;
    private AliasModel mAliasModel;
    private Broadcaster<BroadcastEvent> mBroadcastEventBroadcaster = new Broadcaster<>();
    private BroadcastEventListener mBroadcastEventListener = new BroadcastEventListener();
    private UserPreferences mUserPreferences;

    /**
     * Model for managing Broadcast configurations and any associated broadcaster instances.
     */
    public BroadcastModel(AliasModel aliasModel, IconModel iconModel, UserPreferences userPreferences)
    {
        mAliasModel = aliasModel;
        mIconModel = iconModel;
        mUserPreferences = userPreferences;

        //Monitor to remove temporary recording files that have been streamed by all audio broadcasters
        ThreadPool.SCHEDULED.scheduleAtFixedRate(new RecordingDeletionMonitor(), 15l, 15l, TimeUnit.SECONDS);

        removeOrphanedTemporaryRecordings();
    }

    /**
     * List of broadcast configurations with (optional) audio broadcasters created from each configuration
     */
    public ObservableList<ConfiguredBroadcast> getConfiguredBroadcasts()
    {
        return mConfiguredBroadcasts;
    }

    /**
     * Removes all broadcast configurations and shuts down any running broadcasters.
     */
    public void clear()
    {
        List<ConfiguredBroadcast> configuredBroadcasts = new ArrayList<>(mConfiguredBroadcasts);

        for(ConfiguredBroadcast configuredBroadcast: configuredBroadcasts)
        {
            removeBroadcastConfiguration(configuredBroadcast.getBroadcastConfiguration());
        }
    }

    /**
     * List of broadcastAudio configuration names
     */
    public List<String> getBroadcastConfigurationNames()
    {
        List<String> names = new ArrayList<>();

        for(ConfiguredBroadcast configuredBroadcast: mConfiguredBroadcasts)
        {
            names.add(configuredBroadcast.getBroadcastConfiguration().getName());
        }

        return names;
    }

    /**
     * Current list of broadcastAudio configurations
     */
    public List<BroadcastConfiguration> getBroadcastConfigurations()
    {
        List<BroadcastConfiguration> configs = new ArrayList<>();

        for(ConfiguredBroadcast configuredBroadcast: mConfiguredBroadcasts)
        {
            configs.add(configuredBroadcast.getBroadcastConfiguration());
        }

        return Collections.unmodifiableList(configs);
    }

    /**
     * Adds the list of broadcastAudio configurations to this model
     */
    public void addBroadcastConfigurations(List<BroadcastConfiguration> configurations)
    {
        for(BroadcastConfiguration configuration : configurations)
        {
            addBroadcastConfiguration(configuration);
        }
    }

    /**
     * Adds the broadcastAudio configuration to this model
     */
    public ConfiguredBroadcast addBroadcastConfiguration(BroadcastConfiguration configuration)
    {
        if(configuration != null)
        {
            ensureUniqueName(configuration);

            ConfiguredBroadcast configuredBroadcast = getConfiguredBroadcast(configuration);

            if(configuredBroadcast == null)
            {
                configuredBroadcast = new ConfiguredBroadcast(configuration);
                mConfiguredBroadcasts.add(configuredBroadcast);
                int index = mConfiguredBroadcasts.indexOf(configuredBroadcast);
                fireTableRowsInserted(index, index);
                process(new BroadcastEvent(configuration, BroadcastEvent.Event.CONFIGURATION_ADD));
                return configuredBroadcast;
            }
        }

        return null;
    }

    /**
     * Updates the configuration's name so that it is unique among all other broadcast configurations
     *
     * @param configuration
     */
    private void ensureUniqueName(BroadcastConfiguration configuration)
    {
        if(configuration.getName() == null || configuration.getName().isEmpty())
        {
            configuration.setName("New Configuration");
        }

        while(!isUniqueName(configuration.getName(), configuration))
        {
            String currentName = configuration.getName();

            if(currentName.matches(UNIQUE_NAME_REGEX))
            {
                int currentVersion = 1;

                StringBuilder sb = new StringBuilder();
                Matcher m = Pattern.compile(UNIQUE_NAME_REGEX).matcher(currentName);

                if(m.find())
                {
                    String version = m.group(2);

                    try
                    {
                        currentVersion = Integer.parseInt(version);
                    }
                    catch(Exception e)
                    {
                        //Couldn't parse the version number -- keep incrementing until we find a winner
                    }

                    currentVersion++;

                    sb.append(m.group(1)).append("(").append(currentVersion).append(")");
                }
                else
                {
                    sb.append(configuration.getName()).append("(").append(currentVersion).append(")");
                }

                configuration.setName(sb.toString());
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                sb.append(configuration.getName()).append("(2)");
                configuration.setName(sb.toString());
            }
        }

    }

    /**
     * Indicates if the name is unique among all of the broadcast configurations.  Checks each of the configurations
     * in this model for a name collision.  Assumes that the name argument will be assigned to the configuration
     * argument if the name is unique, therefore, the name will not be checked against the configuration argument for
     * a collision.
     *
     * @param name to check for uniqueness
     * @param configuration to ignore when checking the name for uniqueness
     * @return true if the name is not null and unique among all configurations managed by this model
     */
    public boolean isUniqueName(String name, BroadcastConfiguration configuration)
    {
        if(name == null || name.isEmpty())
        {
            return false;
        }

        for(ConfiguredBroadcast configuredBroadcast: mConfiguredBroadcasts)
        {
            BroadcastConfiguration toCompare = configuredBroadcast.getBroadcastConfiguration();

            if(toCompare != configuration && toCompare.getName() != null && toCompare.getName().equals(name))
            {
                return false;
            }
        }

        return true;
    }

    public void removeBroadcastConfiguration(BroadcastConfiguration broadcastConfiguration)
    {
        ConfiguredBroadcast configuredBroadcast = getConfiguredBroadcast(broadcastConfiguration);

        if(configuredBroadcast != null)
        {
            int index = mConfiguredBroadcasts.indexOf(configuredBroadcast);

            mConfiguredBroadcasts.remove(configuredBroadcast);

            if(configuredBroadcast.hasAudioBroadcaster())
            {
                mBroadcasterMap.remove(broadcastConfiguration.getId());
                configuredBroadcast.getAudioBroadcaster().stop();;
                configuredBroadcast.setAudioBroadcaster(null);
            }

            process(new BroadcastEvent(broadcastConfiguration, BroadcastEvent.Event.CONFIGURATION_DELETE));

            fireTableRowsDeleted(index, index);
        }
    }

    /**
     * Returns the broadcaster associated with the stream name or null if there is no broadcaster setup for the name.
     */
    public AbstractAudioBroadcaster getBroadcaster(String streamName)
    {
        BroadcastConfiguration broadcastConfiguration = getBroadcastConfiguration(streamName);

        if(broadcastConfiguration != null)
        {
            return mBroadcasterMap.get(broadcastConfiguration.getId());
        }

        return null;
    }

    @Override
    public void receive(AudioRecording audioRecording)
    {
        if(audioRecording != null && !audioRecording.getBroadcastChannels().isEmpty())
        {
            for(BroadcastChannel broadcastChannel : audioRecording.getBroadcastChannels())
            {
                String channelName = broadcastChannel.getChannelName();

                if(channelName != null)
                {
                    AbstractAudioBroadcaster audioBroadcaster = getBroadcaster(channelName);

                    if(audioBroadcaster != null)
                    {
                        audioRecording.addPendingReplay();
                        audioBroadcaster.receive(audioRecording);
                    }
                }
            }
        }

        mRecordingQueue.add(audioRecording);
    }

    /**
     * Creates a new broadcaster for the broadcast configuration and adds it to the model
     */
    private void createBroadcaster(BroadcastConfiguration broadcastConfiguration)
    {
        ConfiguredBroadcast configuredBroadcast = getConfiguredBroadcast(broadcastConfiguration);

        if(configuredBroadcast != null && broadcastConfiguration.isEnabled())
        {
            if(configuredBroadcast.hasAudioBroadcaster())
            {
                deleteBroadcaster(configuredBroadcast);
            }

            final AbstractAudioBroadcaster audioBroadcaster = BroadcastFactory.getBroadcaster(broadcastConfiguration,
                    mAliasModel, mUserPreferences);

            if(audioBroadcaster != null)
            {
                configuredBroadcast.setAudioBroadcaster(audioBroadcaster);
                audioBroadcaster.setListener(mBroadcastEventListener);
                mBroadcasterMap.put(audioBroadcaster.getBroadcastConfiguration().getId(), audioBroadcaster);

                int index = mConfiguredBroadcasts.indexOf(configuredBroadcast);

                if(index >= 0)
                {
                    fireTableRowsUpdated(index, index);
                }

                broadcast(new BroadcastEvent(audioBroadcaster, BroadcastEvent.Event.BROADCASTER_ADD));
                ThreadPool.CACHED.submit(audioBroadcaster::start);
            }
        }
    }

    /**
     * Shut down a broadcaster created from the configuration and remove it from this model
     */
    private void deleteBroadcaster(ConfiguredBroadcast configuredBroadcast)
    {
        if(configuredBroadcast != null && configuredBroadcast.hasAudioBroadcaster())
        {
            mBroadcasterMap.remove(configuredBroadcast.getBroadcastConfiguration().getId());

            AbstractAudioBroadcaster broadcaster = configuredBroadcast.getAudioBroadcaster();
            configuredBroadcast.setAudioBroadcaster(null);

            broadcaster.stop();
            broadcaster.removeListener();
            broadcaster.dispose();

            int index = mConfiguredBroadcasts.indexOf(configuredBroadcast);

            if(index >= 0)
            {
                fireTableRowsUpdated(index, index);
            }

            broadcast(new BroadcastEvent(broadcaster, BroadcastEvent.Event.BROADCASTER_DELETE));
        }
    }

    /**
     * Returns the broadcast configuration identified by the stream name
     */
    public BroadcastConfiguration getBroadcastConfiguration(String streamName)
    {
        for(ConfiguredBroadcast configuredBroadcast: mConfiguredBroadcasts)
        {
            if(configuredBroadcast.getBroadcastConfiguration().getName() != null &&
                configuredBroadcast.getBroadcastConfiguration().getName().equals(streamName))
            {
                return configuredBroadcast.getBroadcastConfiguration();
            }

        }
        return null;
    }

    /**
     * Registers the listener to receive broadcastAudio configuration events
     */
    public void addListener(Listener<BroadcastEvent> listener)
    {
        mBroadcastEventBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving broadcastAudio configuration events
     */
    public void removeListener(Listener<BroadcastEvent> listener)
    {
        mBroadcastEventBroadcaster.removeListener(listener);
    }

    /**
     * Retrieves the configured broadcast that contains the specified configuration
     * @param configuration to search for
     * @return configured broadcast that matches, or null
     */
    private ConfiguredBroadcast getConfiguredBroadcast(BroadcastConfiguration configuration)
    {
        for(ConfiguredBroadcast configuredBroadcast: mConfiguredBroadcasts)
        {
            if(configuredBroadcast.getBroadcastConfiguration().equals(configuration))
            {
                return configuredBroadcast;
            }
        }

        return null;
    }

    /**
     * Retrieves the configured broadcast that contains the specified broadcaster
     * @param broadcaster to search for
     * @return configured broadcast that matches, or null
     */
    private ConfiguredBroadcast getConfiguredBroadcast(AbstractAudioBroadcaster broadcaster)
    {
        for(ConfiguredBroadcast configuredBroadcast: mConfiguredBroadcasts)
        {
            if(configuredBroadcast.getAudioBroadcaster() != null &&
                configuredBroadcast.getAudioBroadcaster().equals(broadcaster))
            {
                return configuredBroadcast;
            }
        }

        return null;
    }

    /**
     * Broadcasts the broadcastAudio configuration change event
     */
    private void broadcast(BroadcastEvent event)
    {
        mBroadcastEventBroadcaster.broadcast(event);
    }

    /**
     * Process a broadcast event from one of the broadcasters managed by this model
     */
    public void process(BroadcastEvent broadcastEvent)
    {
        if(broadcastEvent.isBroadcastConfigurationEvent())
        {
            switch(broadcastEvent.getEvent())
            {
                case CONFIGURATION_ADD:
                    createBroadcaster(broadcastEvent.getBroadcastConfiguration());
                    break;
                case CONFIGURATION_CHANGE:
                    BroadcastConfiguration broadcastConfiguration = broadcastEvent.getBroadcastConfiguration();
                    ConfiguredBroadcast configuredBroadcast = getConfiguredBroadcast(broadcastConfiguration);

                    //Delete the broadcaster if it exists
                    deleteBroadcaster(configuredBroadcast);

                    //If the configuration is enabled, create a new broadcaster after a brief delay
                    if(broadcastConfiguration.isEnabled())
                    {
                        //Delay restarting the broadcaster to allow remote server time to cleanup
                        ThreadPool.SCHEDULED.schedule(new DelayedBroadcasterStartup(broadcastConfiguration),
                            1, TimeUnit.SECONDS);
                    }

                    int index = mConfiguredBroadcasts.indexOf(configuredBroadcast);
                    fireTableRowsUpdated(index, index);
                    break;
                case CONFIGURATION_DELETE:
                    deleteBroadcaster(getConfiguredBroadcast(broadcastEvent.getBroadcastConfiguration()));
                    break;
            }
        }
        else if(broadcastEvent.isAudioBroadcasterEvent())
        {
            ConfiguredBroadcast configuredBroadcast = getConfiguredBroadcast(broadcastEvent.getAudioBroadcaster());
            int row = mConfiguredBroadcasts.indexOf(configuredBroadcast);

            switch(broadcastEvent.getEvent())
            {
                case BROADCASTER_QUEUE_CHANGE:
                    if(row >= 0)
                    {
                        fireTableCellUpdated(row, COLUMN_BROADCASTER_QUEUE_SIZE);
                    }
                    break;
                case BROADCASTER_STATE_CHANGE:
                    if(row >= 0)
                    {
                        fireTableCellUpdated(row, COLUMN_BROADCASTER_STATUS);
                    }
                    break;
                case BROADCASTER_STREAMED_COUNT_CHANGE:
                    if(row >= 0)
                    {
                        fireTableCellUpdated(row, COLUMN_BROADCASTER_STREAMED_COUNT);
                    }
                    break;
                case BROADCASTER_AGED_OFF_COUNT_CHANGE:
                    if(row >= 0)
                    {
                        fireTableCellUpdated(row, COLUMN_BROADCASTER_AGED_OFF_COUNT);
                    }
                case BROADCASTER_ERROR_COUNT_CHANGE:
                    if(row >= 0)
                    {
                        fireTableCellUpdated(row, COLUMN_BROADCASTER_ERROR_COUNT);
                    }
                    break;
            }
        }

        //Rebroadcast the event to any listeners of this model
        broadcast(broadcastEvent);
    }

    @Override
    public int getRowCount()
    {
        return mConfiguredBroadcasts.size();
    }

    @Override
    public int getColumnCount()
    {
        return COLUMN_NAMES.length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        try
        {
            if(rowIndex <= mConfiguredBroadcasts.size())
            {
                ConfiguredBroadcast configuredBroadcast = mConfiguredBroadcasts.get(rowIndex);

                if(configuredBroadcast != null)
                {
                    switch(columnIndex)
                    {
                        case COLUMN_BROADCAST_SERVER_TYPE:
                            return configuredBroadcast.getBroadcastServerType();
                        case COLUMN_STREAM_NAME:
                            return configuredBroadcast.getBroadcastConfiguration().getName();
                        case COLUMN_BROADCASTER_STATUS:
                            if(configuredBroadcast.hasAudioBroadcaster())
                            {
                                return configuredBroadcast.getAudioBroadcaster().getBroadcastState();
                            }
                            else if(!configuredBroadcast.getBroadcastConfiguration().isEnabled())
                            {
                                return BroadcastState.DISABLED;
                            }
                            else if(!configuredBroadcast.getBroadcastConfiguration().isValid())
                            {
                                return BroadcastState.INVALID_SETTINGS;
                            }
                            else
                            {
                                return BroadcastState.ERROR;
                            }
                        case COLUMN_BROADCASTER_QUEUE_SIZE:
                            if(configuredBroadcast.hasAudioBroadcaster())
                            {
                                return configuredBroadcast.getAudioBroadcaster().getAudioQueueSize();
                            }
                            break;
                        case COLUMN_BROADCASTER_STREAMED_COUNT:
                            if(configuredBroadcast.hasAudioBroadcaster())
                            {
                                return configuredBroadcast.getAudioBroadcaster().getStreamedAudioCount();
                            }
                            break;
                        case COLUMN_BROADCASTER_AGED_OFF_COUNT:
                            if(configuredBroadcast.hasAudioBroadcaster())
                            {
                                return configuredBroadcast.getAudioBroadcaster().getAgedOffAudioCount();
                            }
                            break;
                        case COLUMN_BROADCASTER_ERROR_COUNT:
                            if(configuredBroadcast.hasAudioBroadcaster())
                            {
                                return configuredBroadcast.getAudioBroadcaster().getAudioErrorCount();
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        catch(Exception e)
        {
            mLog.error("Error accessing data in broadcast model", e);
        }

        return null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        switch(columnIndex)
        {
            case COLUMN_BROADCASTER_STATUS:
                return BroadcastState.class;
            case COLUMN_BROADCASTER_AGED_OFF_COUNT:
            case COLUMN_BROADCASTER_QUEUE_SIZE:
            case COLUMN_BROADCASTER_STREAMED_COUNT:
            case COLUMN_BROADCASTER_ERROR_COUNT:
                return Integer.class;
            case COLUMN_BROADCAST_SERVER_TYPE:
                return BroadcastServerType.class;
            case COLUMN_STREAM_NAME:
            default:
                return String.class;
        }
    }

    @Override
    public String getColumnName(int column)
    {
        if(0 <= column && column < COLUMN_NAMES.length)
        {
            return COLUMN_NAMES[column];
        }

        return null;
    }

    /**
     * Cleanup method to remove a temporary recording file from disk.
     *
     * @param recording to remove
     */
    private void removeRecording(AudioRecording recording)
    {
        try
        {
            Files.delete(recording.getPath());
        }
        catch(IOException ioe)
        {
            mLog.error("Error deleting temporary internet recording file: " + recording.getPath().toString() + " - " +
                ioe.getMessage());
        }
    }


    /**
     * Removes any temporary stream recordings left-over from the previous application run.
     *
     * This should only be invoked on startup.
     */
    private void removeOrphanedTemporaryRecordings()
    {
        ThreadPool.SCHEDULED.submit(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Path path = SystemProperties.getInstance().getApplicationFolder(BroadcastModel.TEMPORARY_STREAM_DIRECTORY);

                    if(path != null && Files.isDirectory(path))
                    {
                        StringBuilder sb = new StringBuilder();
                        sb.append(TEMPORARY_STREAM_FILE_SUFFIX);
                        sb.append("*.*");

                        try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path, sb.toString()))
                        {
                            directoryStream.forEach(new Consumer<Path>()
                            {
                                @Override
                                public void accept(Path path)
                                {
                                    try
                                    {
                                        Files.delete(path);
                                    }
                                    catch(IOException ioe)
                                    {
                                        mLog.error("Couldn't delete orphaned temporary recording: " + path.toString(), ioe);
                                    }
                                }
                            });
                        }
                        catch(IOException ioe)
                        {
                            mLog.error("Error discovering orphaned temporary stream recording files", ioe);
                        }
                    }
                }
                catch(Throwable t)
                {
                    mLog.error("Error during cleanup of orphaned temporary streaming recording files");
                }
            }
        });
    }

    /**
     * Provides delayed startup of a broadcaster to allow for the remote server to complete a disconnection.
     */
    public class DelayedBroadcasterStartup implements Runnable
    {
        private BroadcastConfiguration mBroadcastConfiguration;

        public DelayedBroadcasterStartup(BroadcastConfiguration configuration)
        {
            mBroadcastConfiguration = configuration;
        }


        @Override
        public void run()
        {
            createBroadcaster(mBroadcastConfiguration);
        }
    }

    /**
     * Monitors the recording queue and removes any recordings that have no pending replays by audio broadcasters
     */
    public class RecordingDeletionMonitor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                List<AudioRecording> recordingsToDelete = new ArrayList<>();

                Iterator<AudioRecording> it = mRecordingQueue.iterator();

                AudioRecording recording;

                while(it.hasNext())
                {
                    recording = it.next();

                    if(!recording.hasPendingReplays())
                    {
                        recordingsToDelete.add(recording);
                    }
                }

                if(!recordingsToDelete.isEmpty())
                {
                    for(AudioRecording recordingToDelete : recordingsToDelete)
                    {
                        mRecordingQueue.remove(recordingToDelete);
                        removeRecording(recordingToDelete);
                    }
                }
            }
            catch(Exception e)
            {
                mLog.error("Error while checking audio recording queue for recordings to delete", e);
            }
        }
    }

    /**
     * Adapter to receive and process broadcast events from constructed audio broadcasters
     */
    public class BroadcastEventListener implements Listener<BroadcastEvent>
    {
        @Override
        public void receive(BroadcastEvent broadcastEvent)
        {
            process(broadcastEvent);
        }
    }
}
