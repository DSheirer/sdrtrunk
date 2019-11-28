/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.audio.broadcast;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.icon.IconManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BroadcastModel extends AbstractTableModel implements Listener<AudioRecording>
{
    private final static Logger mLog = LoggerFactory.getLogger(BroadcastModel.class);

    public static final String TEMPORARY_STREAM_DIRECTORY = "streaming";
    public static final String TEMPORARY_STREAM_FILE_SUFFIX = "temporary_streaming_file_";

    private static final String UNIQUE_NAME_REGEX = "(.*)\\((\\d*)\\)";

    public static final int COLUMN_SERVER_ICON = 0;
    public static final int COLUMN_STREAM_NAME = 1;
    public static final int COLUMN_BROADCASTER_STATUS = 2;
    public static final int COLUMN_BROADCASTER_QUEUE_SIZE = 3;
    public static final int COLUMN_BROADCASTER_STREAMED_COUNT = 4;
    public static final int COLUMN_BROADCASTER_AGED_OFF_COUNT = 5;

    public static final String[] COLUMN_NAMES = new String[]
        {"Streaming", "Name", "Status", "Queued", "Streamed", "Aged Off"};

    private ObservableList<ConfiguredBroadcast> mConfiguredBroadcasts =
                FXCollections.observableArrayList(ConfiguredBroadcast.extractor());
    private ObservableList<BroadcastConfiguration> mBroadcastConfigurations =
                FXCollections.observableArrayList(BroadcastConfiguration.extractor());
    private List<AudioRecording> mRecordingQueue = new CopyOnWriteArrayList<>();
    private Map<Integer,AudioBroadcaster> mBroadcasterMap = new HashMap<>();
    private IconManager mIconManager;
    private AliasModel mAliasModel;
    private Broadcaster<BroadcastEvent> mBroadcastEventBroadcaster = new Broadcaster<>();

    /**
     * Model for managing Broadcast configurations and any associated broadcaster instances.
     */
    public BroadcastModel(AliasModel aliasModel, IconManager iconManager, UserPreferences userPreferences)
    {
        mAliasModel = aliasModel;
        mIconManager = iconManager;

        //Monitor to remove temporary recording files that have been streamed by all audio broadcasters
        ThreadPool.SCHEDULED.scheduleAtFixedRate(new RecordingDeletionMonitor(), 15l, 15l, TimeUnit.SECONDS);

        removeOrphanedTemporaryRecordings();
        mBroadcastConfigurations.addListener(new ConfigurationChangeListener());

        //TODO: remove broadcast of change events and simply have the playlist manager register as a listener on the configs
        //TODO: list to detect changes and execute playlist saves.
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
        List<BroadcastConfiguration> configsToRemove = new ArrayList<>(mBroadcastConfigurations);

        for(BroadcastConfiguration configToRemove: configsToRemove)
        {
            removeBroadcastConfiguration(configToRemove);
        }
    }

    /**
     * List of broadcastAudio configuration names
     */
    public List<String> getBroadcastConfigurationNames()
    {
        List<String> names = new ArrayList<>();

        for(BroadcastConfiguration configuration : mBroadcastConfigurations)
        {
            names.add(configuration.getName());
        }

        return names;
    }

    /**
     * Current list of broadcastAudio configurations
     */
    public ObservableList<BroadcastConfiguration> getBroadcastConfigurations()
    {
        return mBroadcastConfigurations;
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

            if(!mBroadcastConfigurations.contains(configuration))
            {
                mBroadcastConfigurations.add(configuration);
                ConfiguredBroadcast configuredBroadcast = new ConfiguredBroadcast(configuration);
                mConfiguredBroadcasts.add(configuredBroadcast);

                int index = mBroadcastConfigurations.size() - 1;

                fireTableRowsInserted(index, index);

                process(new BroadcastEvent(configuration, BroadcastEvent.Event.CONFIGURATION_ADD));

                return configuredBroadcast;
            }
        }

        return null;
    }

    /**
     * Clones the configuration and adds it this model with a unique configuration name
     */
    public BroadcastConfiguration cloneBroadcastConfiguration(BroadcastConfiguration configuration)
    {
        if(configuration != null)
        {
            BroadcastConfiguration clone = configuration.copyOf();
            addBroadcastConfiguration(clone);
            return clone;
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

        for(BroadcastConfiguration configurationToCompare : mBroadcastConfigurations)
        {
            if(configurationToCompare != configuration &&
                configurationToCompare.getName() != null &&
                configurationToCompare.getName().equals(name))
            {
                return false;
            }
        }

        return true;
    }

    public void removeBroadcastConfiguration(BroadcastConfiguration broadcastConfiguration)
    {
        if(broadcastConfiguration != null && mBroadcastConfigurations.contains(broadcastConfiguration))
        {
            int index = mBroadcastConfigurations.indexOf(broadcastConfiguration);

            mBroadcastConfigurations.remove(broadcastConfiguration);

            Iterator<ConfiguredBroadcast> it = mConfiguredBroadcasts.iterator();
            while(it.hasNext())
            {
                if(it.next().getBroadcastConfiguration() == broadcastConfiguration)
                {
                    it.remove();
                    break;
                }
            }

            process(new BroadcastEvent(broadcastConfiguration, BroadcastEvent.Event.CONFIGURATION_DELETE));

            fireTableRowsDeleted(index, index);
        }
    }

    /**
     * Returns the broadcaster associated with the stream name or null if there is no broadcaster setup for the name.
     */
    public AudioBroadcaster getBroadcaster(String streamName)
    {
        return mBroadcasterMap.get(streamName);
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
                    AudioBroadcaster audioBroadcaster = getBroadcaster(channelName);

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
        if(broadcastConfiguration != null && broadcastConfiguration.isEnabled() && broadcastConfiguration.isValid())
        {
            //Remove current broadcaster if one exists
            deleteBroadcaster(broadcastConfiguration.getId());

            AudioBroadcaster audioBroadcaster = BroadcastFactory.getBroadcaster(broadcastConfiguration, mAliasModel);

            if(audioBroadcaster != null)
            {
                audioBroadcaster.setListener(broadcastEvent -> process(broadcastEvent));
                audioBroadcaster.start();

                Iterator<ConfiguredBroadcast> it = mConfiguredBroadcasts.iterator();
                while(it.hasNext())
                {
                    ConfiguredBroadcast configuredBroadcast = it.next();

                    if(configuredBroadcast.getBroadcastConfiguration() == broadcastConfiguration)
                    {
                        configuredBroadcast.setAudioBroadcaster(audioBroadcaster);
                        break;
                    }
                }

                mBroadcasterMap.put(audioBroadcaster.getBroadcastConfiguration().getId(), audioBroadcaster);

                int index = mBroadcastConfigurations.indexOf(audioBroadcaster.getBroadcastConfiguration());

                if(index >= 0)
                {
                    fireTableRowsUpdated(index, index);
                }

                broadcast(new BroadcastEvent(audioBroadcaster, BroadcastEvent.Event.BROADCASTER_ADD));
            }
        }
    }

    /**
     * Shut down a broadcaster created from the configuration and remove it from this model
     */
    private void deleteBroadcaster(int id)
    {
        if(mBroadcasterMap.containsKey(id))
        {
            AudioBroadcaster audioBroadcaster = mBroadcasterMap.remove(id);

            if(audioBroadcaster != null)
            {
                Iterator<ConfiguredBroadcast> it = mConfiguredBroadcasts.iterator();
                while(it.hasNext())
                {
                    ConfiguredBroadcast configuredBroadcast = it.next();

                    if(configuredBroadcast.getBroadcastConfiguration().getId() == id)
                    {
                        configuredBroadcast.setAudioBroadcaster(null);
                        break;
                    }
                }

                audioBroadcaster.stop();
                audioBroadcaster.removeListener();
                audioBroadcaster.dispose();

                int index = mBroadcastConfigurations.indexOf(audioBroadcaster.getBroadcastConfiguration());

                if(index >= 0)
                {
                    fireTableRowsUpdated(index, index);
                }

                broadcast(new BroadcastEvent(audioBroadcaster, BroadcastEvent.Event.BROADCASTER_DELETE));
            }
        }
    }

    /**
     * Returns the broadcast configuration identified by the stream name
     */
    public BroadcastConfiguration getBroadcastConfiguration(String streamName)
    {
        for(BroadcastConfiguration config: mBroadcastConfigurations)
        {
            if(config.getName() != null && config.getName().contentEquals(streamName))
            {
                return config;
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

                    //Delete the broadcaster if it exists
                    deleteBroadcaster(broadcastConfiguration.getId());

                    //If the configuration is enabled, create a new broadcaster after a brief delay
                    if(broadcastConfiguration.isEnabled())
                    {
                        //Delay restarting the broadcaster to allow remote server time to cleanup
                        ThreadPool.SCHEDULED.schedule(new DelayedBroadcasterStartup(broadcastConfiguration),
                            1, TimeUnit.SECONDS);
                    }

                    int index = mBroadcastConfigurations.indexOf(broadcastConfiguration);
                    fireTableRowsUpdated(index, index);
                    break;
                case CONFIGURATION_DELETE:
                    deleteBroadcaster(broadcastEvent.getBroadcastConfiguration().getId());
                    break;
            }
        }
        else if(broadcastEvent.isAudioBroadcasterEvent())
        {
            int row = mBroadcastConfigurations.indexOf(broadcastEvent.getAudioBroadcaster().getBroadcastConfiguration());

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
                    break;
            }
        }

        //Rebroadcast the event to any listeners of this model
        broadcast(broadcastEvent);
    }

    @Override
    public int getRowCount()
    {
        return mBroadcastConfigurations.size();
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
            if(rowIndex <= mBroadcastConfigurations.size())
            {
                BroadcastConfiguration configuration = mBroadcastConfigurations.get(rowIndex);

                if(configuration != null)
                {
                    switch(columnIndex)
                    {
                        case COLUMN_SERVER_ICON:
                            String iconPath = configuration.getBroadcastServerType().getIconPath();

                            if(iconPath != null && mIconManager != null)
                            {
                                return mIconManager.getScaledIcon(iconPath, 14);
                            }
                            break;
                        case COLUMN_STREAM_NAME:
                            return configuration.getName();
                        case COLUMN_BROADCASTER_STATUS:
                            AudioBroadcaster audioBroadcasterA = mBroadcasterMap.get(configuration.getName());

                            if(audioBroadcasterA != null)
                            {
                                return audioBroadcasterA.getBroadcastState();
                            }
                            else if(!configuration.isEnabled())
                            {
                                return BroadcastState.DISABLED;
                            }
                            else if(!configuration.isValid())
                            {
                                return BroadcastState.INVALID_SETTINGS;
                            }
                        case COLUMN_BROADCASTER_QUEUE_SIZE:
                            AudioBroadcaster audioBroadcasterB = mBroadcasterMap.get(configuration.getName());

                            if(audioBroadcasterB != null)
                            {
                                return audioBroadcasterB.getQueueSize();
                            }
                            break;
                        case COLUMN_BROADCASTER_STREAMED_COUNT:
                            AudioBroadcaster audioBroadcasterC = mBroadcasterMap.get(configuration.getName());

                            if(audioBroadcasterC != null)
                            {
                                return audioBroadcasterC.getStreamedAudioCount();
                            }
                            break;
                        case COLUMN_BROADCASTER_AGED_OFF_COUNT:
                            AudioBroadcaster audioBroadcasterD = mBroadcasterMap.get(configuration.getName());

                            if(audioBroadcasterD != null)
                            {
                                return audioBroadcasterD.getAgedOffAudioCount();
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
                return Integer.class;
            case COLUMN_SERVER_ICON:
                return ImageIcon.class;
            case COLUMN_STREAM_NAME:
            default:
                return String.class;
        }
    }

    /**
     * Broadcast configuration at the specified model row
     */
    public BroadcastConfiguration getConfigurationAt(int rowIndex)
    {
        return mBroadcastConfigurations.get(rowIndex);
    }

    /**
     * Model row number for the specified configuration
     */
    public int getRowForConfiguration(BroadcastConfiguration configuration)
    {
        return mBroadcastConfigurations.indexOf(configuration);
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
     * List change listener to detect broadcast configuration changes and broadcast a change event
     */
    public class ConfigurationChangeListener implements ListChangeListener<BroadcastConfiguration>
    {
        @Override
        public void onChanged(Change<? extends BroadcastConfiguration> change)
        {
            while(change.next())
            {
                if(change.wasUpdated())
                {
                    for(int x = change.getFrom(); x < change.getTo(); x++)
                    {
                        BroadcastConfiguration config = change.getList().get(x);
                        BroadcastEvent event = new BroadcastEvent(config, BroadcastEvent.Event.CONFIGURATION_CHANGE);
                        process(event);
                        broadcast(event);
                    }
                }
            }
        }
    }

}
