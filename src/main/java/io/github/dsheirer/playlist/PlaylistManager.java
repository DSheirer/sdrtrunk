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
package io.github.dsheirer.playlist;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.IAliasListRefreshListener;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.playlist.PlaylistPreference;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.collections.ListChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all aspects of playlists and related models
 */
public class PlaylistManager implements Listener<ChannelEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaylistManager.class);

    public static final int PLAYLIST_CURRENT_VERSION = 4;

    private AliasModel mAliasModel;
    private ChannelMapModel mChannelMapModel = new ChannelMapModel();
    private IconModel mIconModel;

    private BroadcastModel mBroadcastModel;
    private ChannelModel mChannelModel;
    private ChannelProcessingManager mChannelProcessingManager;
    private TunerManager mTunerManager;
    private UserPreferences mUserPreferences;
    private RadioReference mRadioReference;
    private AtomicBoolean mPlaylistSavePending = new AtomicBoolean();
    private ScheduledFuture<?> mPlaylistSaveFuture;
    private boolean mPlaylistLoading = false;
    private List<IAliasListRefreshListener> mAliasListRefreshListeners = new ArrayList<>();

    /**
     * Playlist manager - manages all channel configurations, channel maps, and alias lists and handles loading or
     * persisting to the current playlist file
     *
     * Monitors playlist changes to automatically save configuration changes after they occur.
     *
     * @param userPreferences for user settings
     * @param tunerManager for access to tuner model
     * @param aliasModel for aliases
     * @param eventLogManager for event logging
     * @param iconModel for icons
     */
    public PlaylistManager(UserPreferences userPreferences, TunerManager tunerManager, AliasModel aliasModel,
                           EventLogManager eventLogManager, IconModel iconModel)
    {
        mUserPreferences = userPreferences;
        mTunerManager = tunerManager;
        mAliasModel = aliasModel;
        mIconModel = iconModel;

        mBroadcastModel = new BroadcastModel(mAliasModel, mIconModel, userPreferences);
        mRadioReference = new RadioReference(mUserPreferences);

        mChannelModel = new ChannelModel(mAliasModel);
        mChannelProcessingManager = new ChannelProcessingManager(mChannelMapModel, eventLogManager, mTunerManager,
            mAliasModel, mUserPreferences);

        //Register the channel processing manager to receive global channel stop processing requests so that it can
        //respond to tuner shutdown (ie error) events
        MyEventBus.getGlobalEventBus().register(mChannelProcessingManager);

        mChannelModel.addListener(mChannelProcessingManager);
        mChannelProcessingManager.addChannelEventListener(mChannelModel);

        //Register for alias, channel and channel map events so that we can
        //save the playlist when there are any changes
        mChannelModel.addListener(this);

        mAliasModel.aliasList().addListener((ListChangeListener<Alias>)c -> schedulePlaylistSave());

        mChannelMapModel.getChannelMaps().addListener((ListChangeListener<ChannelMap>)c -> schedulePlaylistSave());

        mBroadcastModel.addListener(broadcastEvent -> {
            switch(broadcastEvent.getEvent())
            {
                case CONFIGURATION_ADD:
                case CONFIGURATION_CHANGE:
                case CONFIGURATION_DELETE:
                    schedulePlaylistSave();
                    break;
                default:
                    //Do nothing
                    break;
            }
        });
    }

    /**
     * Adds the listener to be notified when an alias list refresh operation is about to take place.  The listener
     * should clear any selected or editing item to prepare for the list of alias list names to be updated so that
     * alias list combo boxes won't trigger an editor modified event when the list contents changes.
     * @param listener for alias list refresh event.
     */
    public void addAliasListRefreshListener(IAliasListRefreshListener listener)
    {
        mAliasListRefreshListeners.add(listener);
    }

    /**
     * Notifies listeners that the alias list will be refreshed
     */
    private void prepareForAliasListRefresh()
    {
        for(IAliasListRefreshListener editor : mAliasListRefreshListeners)
        {
            editor.prepareForAliasListRefresh();
        }
    }

    /**
     * Refresh the alias list names after a rename or delete operation.
     */
    private void refreshAliasListNames()
    {
        //Do a refresh from the aliases
        getAliasModel().refreshAliasListNames();
        //Add in the alias list names referred to by the channels.
        getAliasModel().addAliasListNames(mChannelModel.getAliasListNames());
    }

    /**
     * Renames alias list references across aliases and channels from the old name to the new name.
     *
     * Note: this method should be invoked on the JavaFX thread since it will touch observable alias and channel lists.
     * @param oldName that is currently used by channels and aliases
     * @param newName to apply to channels and aliases
     */
    public void renameAliasList(String oldName, String newName)
    {
        prepareForAliasListRefresh();
        getAliasModel().renameAliasList(oldName, newName);
        getChannelModel().renameAliasList(oldName, newName);
        refreshAliasListNames();
    }

    /**
     * Deletes all aliases that have the alias list name and removes the alias list name from all channels.
     *
     * Note: this method should be invoked on the JavaFX thread since it will touch observable alias and channel lists.
     * @param aliasListName to delete
     */
    public void deleteAliasList(String aliasListName)
    {
        prepareForAliasListRefresh();
        getAliasModel().deleteAliasList(aliasListName);
        getChannelModel().deleteAliasList(aliasListName);
        refreshAliasListNames();
    }

    /**
     * Channel model managed by this playlist manager
     */
    public ChannelModel getChannelModel()
    {
        return mChannelModel;
    }

    /**
     * Channel processing manager
     */
    public ChannelProcessingManager getChannelProcessingManager()
    {
        return mChannelProcessingManager;
    }

    /**
     * Alias model managed by this playlist manager
     */
    public AliasModel getAliasModel()
    {
        return mAliasModel;
    }

    /**
     * Icon manager
     */
    public IconModel getIconModel()
    {
        return mIconModel;
    }

    /**
     * Radio Reference service interface
     */
    public RadioReference getRadioReference()
    {
        return mRadioReference;
    }

    /**
     * Audio Broadcast (streaming) Model
     */
    public BroadcastModel getBroadcastModel()
    {
        return mBroadcastModel;
    }

    /**
     * Channel Map model managed by this playlist manager
     */
    public ChannelMapModel getChannelMapModel()
    {
        return mChannelMapModel;
    }

    /**
     * Loads playlist from the current playlist file, or the default playlist file,
     * as specified in the current SDRTRunk system settings
     */
    public void init()
    {
        PlaylistV2 playlist = load();
        transferPlaylistToModels(playlist);
    }

    /**
     * Closes the current playlist, saving if necessary, clears the models and sets the current playlist to use
     * the specified playlist path.
     * @param path to use for the playlist.
     */
    public void setPlaylist(Path path) throws IOException
    {
        if(path == null)
        {
            throw new IllegalArgumentException("Specified playlist path does not exist");
        }

        //Complete any pending playlist save
        saveNow();

        mUserPreferences.getPlaylistPreference().setPlaylist(path);

        if(!Files.exists(path))
        {
            createEmptyPlaylist(path);
        }

        init();
    }

    /**
     * Checks the path argument to determine if it is a valid V2 playlist file by loading and deserializer the file.
     * @param path to check
     * @return true if the path is a valid playist.
     */
    public static boolean isPlaylist(Path path)
    {
        if(path == null || !Files.exists(path))
        {
            return false;
        }

        JacksonXmlModule xmlModule = new JacksonXmlModule();
        xmlModule.setDefaultUseWrapper(false);
        ObjectMapper objectMapper = new XmlMapper(xmlModule)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try(InputStream in = Files.newInputStream(path))
        {
            PlaylistV2 playlist = objectMapper.readValue(in, PlaylistV2.class);

            //If jackson can successfully deserialize the file, then it's a good V2 playlist
            return true;
        }
        catch(IOException ioe)
        {
            mLog.error("IO error while reading playlist file", ioe);
        }

        return false;
    }

    /**
     * Creates an empty serialized playlist
     * @param path for storing the playlist
     * @throws IOException if there is an error writing to disk
     */
    public void createEmptyPlaylist(Path path) throws IOException
    {
        PlaylistV2 playlist = new PlaylistV2();

        try(OutputStream out = Files.newOutputStream(path))
        {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(out, playlist);
            out.flush();
        }
        catch(IOException ioe)
        {
            throw ioe;
        }
        catch(Exception e)
        {
            mLog.error("Error while creating empty playlist [" + path.toString() + "]", e);
        }
    }

    private void clearModels()
    {
        mPlaylistLoading = true;

        //Shutdown any running channels
        mChannelProcessingManager.shutdown();

        mChannelModel.clear();
        mChannelMapModel.clear();
        mBroadcastModel.clear();
        mAliasModel.clear();

        mPlaylistLoading = false;
    }

    private void saveNow()
    {
        //Complete any pending playlist save
        if(mPlaylistSaveFuture != null)
        {
            try
            {
                mPlaylistSaveFuture.cancel(true);
            }
            catch(Exception e)
            {
                mLog.error("Error trying to cancel pending playlist save");
            }

            mPlaylistSaveFuture = null;
        }

        if(mPlaylistSavePending.getAndSet(false))
        {
            save();
        }
    }

    /**
     * Transfers data from persisted playlist into system models
     */
    private void transferPlaylistToModels(PlaylistV2 playlist)
    {
        if(playlist != null)
        {
            clearModels();

            mPlaylistLoading = true;

            mAliasModel.addAliases(playlist.getAliases());


            mBroadcastModel.addBroadcastConfigurations(playlist.getBroadcastConfigurations());

            mChannelMapModel.addChannelMaps(playlist.getChannelMaps());

            //Channel model has to be loaded last since it will auto-start channels that are enabled
            mChannelModel.addChannels(playlist.getChannels());

            mPlaylistLoading = false;
        }
    }

    /**
     * Channel event listener method.  Monitors channel events for events that indicate that the playlist has changed
     * and queues automatic playlist saving.
     */
    @Override
    public void receive(ChannelEvent event)
    {
        //Only save playlist for changes to standard channels (not traffic)
        if(event.getChannel().getChannelType() == ChannelType.STANDARD)
        {
            switch(event.getEvent())
            {
                case NOTIFICATION_ADD:
                case NOTIFICATION_CONFIGURATION_CHANGE:
                case NOTIFICATION_DELETE:
                    schedulePlaylistSave();
                    break;
            }
        }
    }

    /**
     * Saves the current playlist
     */
    private void save()
    {
        PlaylistPreference playlistPreference = mUserPreferences.getPlaylistPreference();

        PlaylistV2 playlist = new PlaylistV2();

        playlist.setAliases(new ArrayList(mAliasModel.getAliases()));
        playlist.setBroadcastConfigurations(new ArrayList(mBroadcastModel.getBroadcastConfigurations()));
        playlist.setChannels(new ArrayList(mChannelModel.getChannels()));
        playlist.setChannelMaps(new ArrayList(mChannelMapModel.getChannelMaps()));
        playlist.setVersion(PLAYLIST_CURRENT_VERSION);

        //Create a backup copy of the current playlist
        if(Files.exists(playlistPreference.getPlaylist()))
        {
            try
            {
                Files.copy(playlistPreference.getPlaylist(), playlistPreference.getPlaylistBackup(),
                    StandardCopyOption.REPLACE_EXISTING);
            }
            catch(Exception e)
            {
                mLog.error("Error creating backup copy of current playlist prior to saving updates [" +
                    playlistPreference.getPlaylist().toString() + "]", e);
            }
        }

        //Create a temporary lock file to signify that we're in the process of updating the playlist
        if(!Files.exists(playlistPreference.getPlaylistLock()))
        {
            try
            {
                Files.createFile(playlistPreference.getPlaylistLock());
            }
            catch(IOException e)
            {
                mLog.error("Error creating temporary lock file prior to saving playlist [" +
                    playlistPreference.getPlaylistLock().toString() + "]", e);
            }
        }

        try(OutputStream out = Files.newOutputStream(playlistPreference.getPlaylist()))
        {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(out, playlist);
            out.flush();

            //Remove the playlist lock file to indicate that we successfully saved the file
            if(Files.exists(playlistPreference.getPlaylistLock()))
            {
                Files.delete(playlistPreference.getPlaylistLock());
            }
        }
        catch(IOException ioe)
        {
            mLog.error("IO error while writing the playlist to a file [" + playlistPreference.getPlaylist().toString() + "]", ioe);
        }
        catch(Exception e)
        {
            mLog.error("Error while saving playlist [" + playlistPreference.getPlaylist().toString() + "]", e);
        }
    }

    /**
     * Loads a version 2 playlist
     */
    public PlaylistV2 load()
    {
        PlaylistPreference files = mUserPreferences.getPlaylistPreference();

        PlaylistV2 playlist = null;

        //Check for a lock file that indicates the previous save attempt was incomplete or had an error
        if(Files.exists(files.getPlaylistLock()))
        {
            mLog.info("Previous playlist save was incomplete -- restoring from backup file (if possible)");

            try
            {
                //Remove the previous playlist
                Files.delete(files.getPlaylist());

                //Copy the backup file to restore the previous playlist
                if(Files.exists(files.getPlaylistBackup()))
                {
                    Files.copy(files.getPlaylistBackup(), files.getPlaylist());
                }

                //Remove the lock file
                Files.delete(files.getPlaylistLock());
            }
            catch(IOException ioe)
            {
                mLog.error("Previous playlist save attempt was incomplete and there was an error restoring the " +
                    "playlist backup file", ioe);
            }
        }

        if(Files.exists(files.getPlaylist()))
        {
            mLog.info("Loading playlist [" + files.getPlaylist().toString() + "]");

            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try(InputStream in = Files.newInputStream(files.getPlaylist()))
            {
                playlist = objectMapper.readValue(in, PlaylistV2.class);

                if(PlaylistUpdater.update(playlist))
                {
                    schedulePlaylistSave();
                }
            }
            catch(IOException ioe)
            {
                mLog.error("IO error while reading playlist file", ioe);
            }
        }
        else if(Files.exists(files.getLegacyPlaylist()))
        {
            mLog.info("Loading legacy playlist [" + files.getLegacyPlaylist().toString() + "]");

            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try(InputStream in = Files.newInputStream(files.getLegacyPlaylist()))
            {
                playlist = objectMapper.readValue(in, PlaylistV2.class);

                //Perform any updates that may be needed for the playist.
                if(PlaylistUpdater.update(playlist))
                {
                    mLog.info("Legacy playlist was updated to version [" + PLAYLIST_CURRENT_VERSION + "] - saving");
                    schedulePlaylistSave();
                }
            }
            catch(IOException ioe)
            {
                mLog.error("IO error while reading playlist file", ioe);
            }
        }
        else
        {
            mLog.info("PlaylistManager - playlist not found at [" + files.getPlaylist().toString() + "] - creating new (empty) playlist");
        }

        if(playlist == null)
        {
            playlist = new PlaylistV2();
            schedulePlaylistSave();
        }

        return playlist;
    }

    /**
     * Schedules a playlist save task.  Subsequent calls to this method will be ignored until the save event occurs,
     * thus limiting repetitive playlist saving to a minimum.
     */
    public void schedulePlaylistSave()
    {
        if(!mPlaylistLoading)
        {
            if(mPlaylistSavePending.compareAndSet(false, true))
            {
                mPlaylistSaveFuture = ThreadPool.SCHEDULED.schedule(new PlaylistSaveTask(), 2, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Resets the playlist save pending flag to false and proceeds to save the playlist.
     */
    public class PlaylistSaveTask implements Runnable
    {
        @Override
        public void run()
        {
            save();

            mPlaylistSaveFuture = null;
            mPlaylistSavePending.set(false);
        }
    }
}
