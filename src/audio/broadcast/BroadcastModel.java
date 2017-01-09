/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
package audio.broadcast;

import alias.id.broadcast.BroadcastChannel;
import audio.AudioPacket;
import audio.metadata.AudioMetadata;
import controller.ThreadPoolManager;
import icon.IconManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import properties.SystemProperties;
import sample.Broadcaster;
import sample.Listener;

import javax.swing.*;
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

public class BroadcastModel extends AbstractTableModel implements Listener<AudioPacket>
{
    private final static Logger mLog = LoggerFactory.getLogger( BroadcastModel.class );

    public static final String TEMPORARY_STREAM_DIRECTORY = "streaming";
    public static final String TEMPORARY_STREAM_FILE_SUFFIX = "temporary_streaming_file_";

    private static final String UNIQUE_NAME_REGEX = "(.*)\\((\\d*)\\)";

    public static final int COLUMN_SERVER_ICON = 0;
    public static final int COLUMN_STREAM_NAME = 1;
    public static final int COLUMN_BROADCASTER_STATUS = 2;
    public static final int COLUMN_BROADCASTER_QUEUE_SIZE = 3;
    public static final int COLUMN_BROADCASTER_STREAMED_COUNT = 4;

    private List<BroadcastConfiguration> mBroadcastConfigurations = new CopyOnWriteArrayList<>();
    private List<AudioRecording> mRecordingQueue = new CopyOnWriteArrayList<>();

    private Map<String,BroadcastConfiguration> mBroadcastConfigurationMap = new HashMap<>();
    private Map<String,AudioBroadcaster> mBroadcasterMap = new HashMap<>();
    private ThreadPoolManager mThreadPoolManager;
    private IconManager mIconManager;
    private StreamManager mStreamManager;
    private Broadcaster<BroadcastEvent> mBroadcastEventBroadcaster = new Broadcaster<>();

    /**
     * Model for managing Broadcast configurations and any associated broadcaster instances.
     */
    public BroadcastModel(ThreadPoolManager threadPoolManager, IconManager iconManager)
    {
        mThreadPoolManager = threadPoolManager;
        mIconManager = iconManager;
        mStreamManager = new StreamManager(threadPoolManager, new CompletedRecordingListener(), BroadcastFormat.MP3,
            SystemProperties.getInstance().getApplicationFolder(TEMPORARY_STREAM_DIRECTORY));
        mStreamManager.start();

        //Monitor to remove temporary recording files that have been streamed by all audio broadcasters
        mThreadPoolManager.scheduleFixedRate(ThreadPoolManager.ThreadType.AUDIO_RECORDING,
            new RecordingDeletionMonitor(), 15l, TimeUnit.SECONDS );

        removeOrphanedTemporaryRecordings();
    }

    /**
     * List of broadcastAudio configuration names
     */
    public List<String> getBroadcastConfigurationNames()
    {
        List<String> names = new ArrayList<>();

        for(BroadcastConfiguration configuration: mBroadcastConfigurations)
        {
            names.add(configuration.getName());
        }

        return names;
    }

    /**
     * Current list of broadcastAudio configurations
     */
    public List<BroadcastConfiguration> getBroadcastConfigurations()
    {
        return mBroadcastConfigurations;
    }

    /**
     * Adds the list of broadcastAudio configurations to this model
     */
    public void addBroadcastConfigurations(List<BroadcastConfiguration> configurations)
    {
        for(BroadcastConfiguration configuration: configurations)
        {
            addBroadcastConfiguration(configuration);
        }
    }

    /**
     * Adds the broadcastAudio configuration to this model
     */
    public void addBroadcastConfiguration(BroadcastConfiguration configuration)
    {
        if(configuration != null)
        {
            ensureUniqueName(configuration);

            if(!mBroadcastConfigurations.contains(configuration))
            {
                mBroadcastConfigurations.add(configuration);

                int index = mBroadcastConfigurations.size() - 1;

                fireTableRowsInserted( index, index );

                mBroadcastConfigurationMap.put(configuration.getName(), configuration);

                process( new BroadcastEvent( configuration, BroadcastEvent.Event.CONFIGURATION_ADD) );
            }
        }
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

        for(BroadcastConfiguration configurationToCompare: mBroadcastConfigurations)
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

            mBroadcastConfigurationMap.remove(broadcastConfiguration.getName());

            process(new BroadcastEvent( broadcastConfiguration, BroadcastEvent.Event.CONFIGURATION_DELETE));

            fireTableRowsDeleted( index, index );
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
    public void receive(AudioPacket audioPacket)
    {
        if (audioPacket.hasAudioMetadata() &&
            audioPacket.getAudioMetadata().isStreamable())
        {
            mStreamManager.receive(audioPacket);
        }
    }

    /**
     * Creates a new broadcaster for the broadcast configuration and adds it to the model
     */
    private void createBroadcaster(BroadcastConfiguration broadcastConfiguration)
    {
        if (broadcastConfiguration != null &&
            broadcastConfiguration.isEnabled() &&
            broadcastConfiguration.isValid() &&
            !mBroadcasterMap.containsKey(broadcastConfiguration.getName()))
        {
            AudioBroadcaster audioBroadcaster = BroadcastFactory.getBroadcaster(mThreadPoolManager,
                broadcastConfiguration);

            if (audioBroadcaster != null)
            {
                audioBroadcaster.setListener(new Listener<BroadcastEvent>()
                {
                    @Override
                    public void receive(BroadcastEvent broadcastEvent)
                    {
                        process(broadcastEvent);
                    }
                });

                audioBroadcaster.start();

                mBroadcasterMap.put(audioBroadcaster.getBroadcastConfiguration().getName(), audioBroadcaster);

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
    private void deleteBroadcaster(String name)
    {
        if(name != null && mBroadcasterMap.containsKey(name))
        {
            AudioBroadcaster audioBroadcaster = mBroadcasterMap.remove(name);

            if(audioBroadcaster != null)
            {
                audioBroadcaster.stop();
                audioBroadcaster.removeListener();

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
        return mBroadcastConfigurationMap.get(streamName);
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
        mBroadcastEventBroadcaster.broadcast( event );
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
                    int index = mBroadcastConfigurations.indexOf(broadcastConfiguration);
                    fireTableRowsUpdated( index, index );

                    //Delete and recreate the broadcaster for any broadcast configuration changes
                    String previousChannelName = cleanupMapAssociations(broadcastConfiguration);
                    deleteBroadcaster(previousChannelName);

                    //Delay restarting the broadcaster to allow remote server time to cleanup
                    mThreadPoolManager.scheduleOnce(new DelayedBroadcasterStartup(broadcastConfiguration), 1, TimeUnit.SECONDS);
                    break;
                case CONFIGURATION_DELETE:
                    deleteBroadcaster(broadcastEvent.getBroadcastConfiguration().getName());
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
            }
        }

        //Rebroadcast the event to any listeners of this model
        broadcast(broadcastEvent);
    }

    /**
     * When the name of a broadcast configuration changes, remove the old map associations so that they can be replaced
     * with the new map associations.  Remove any broadcaster that is associated with the old configuration name.
     *
     * @param broadcastConfiguration
     * @return previous or current channel name that has been replaced with the current configuration channel name so
     * that anything else associated with the previous name can be cleaned up.
     */
    private String cleanupMapAssociations(BroadcastConfiguration broadcastConfiguration)
    {
        String oldName = null;

        for(Map.Entry<String,BroadcastConfiguration> entry: mBroadcastConfigurationMap.entrySet())
        {
            if(entry.getValue() == broadcastConfiguration)
            {
                oldName = entry.getKey();
                continue;
            }
        }

        if(oldName != null)
        {
            mBroadcastConfigurationMap.remove(oldName);
        }

        mBroadcastConfigurationMap.put(broadcastConfiguration.getName(), broadcastConfiguration);

        return oldName;
    }

    @Override
    public int getRowCount()
    {
        return mBroadcastConfigurations.size();
    }

    @Override
    public int getColumnCount()
    {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        try
        {
            if( rowIndex <= mBroadcastConfigurations.size())
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
                                return mIconManager.getScaledIcon(new ImageIcon(iconPath), 14);
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
        if(columnIndex == COLUMN_SERVER_ICON)
        {
            return ImageIcon.class;
        }

        return String.class;
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
        switch(column)
        {
            case COLUMN_SERVER_ICON:
                return null;
            case COLUMN_STREAM_NAME:
                return "Name";
            case COLUMN_BROADCASTER_STATUS:
                return "Status";
            case COLUMN_BROADCASTER_QUEUE_SIZE:
                return "Queued";
            case COLUMN_BROADCASTER_STREAMED_COUNT:
                return "Streamed";
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
     * Processes completed audio recordings and distributes them to the audio broadcasters.  Adds the recording to
     * the audio recording queue to be monitored for deletion.
     */
    public class CompletedRecordingListener implements Listener<AudioRecording>
    {
        @Override
        public void receive(AudioRecording audioRecording)
        {
            AudioMetadata metadata = audioRecording.getAudioMetadata();

            if(metadata != null && metadata.isStreamable())
            {
                for (BroadcastChannel broadcastChannel : metadata.getBroadcastChannels())
                {
                    String channelName = broadcastChannel.getChannelName();

                    if (channelName != null)
                    {
                        AudioBroadcaster audioBroadcaster = getBroadcaster(channelName);

                        if (audioBroadcaster != null)
                        {
                            audioRecording.addPendingReplay();
                            audioBroadcaster.receive(audioRecording);
                        }
                    }
                }
            }

            mRecordingQueue.add(audioRecording);
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
                    for(AudioRecording recordingToDelete: recordingsToDelete)
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
}
