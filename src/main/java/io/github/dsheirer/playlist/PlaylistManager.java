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

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasListConfiguration;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.database.DatabaseEvent;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.playlist.PlaylistRecord;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.IAliasListRefreshListener;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.playlist.PlaylistStore.PlaylistReplacedException;
import io.github.dsheirer.playlist.PlaylistStore.PlaylistStoreException;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.service.radioreference.AliasListSynchronizer;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.collections.ListChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages all aspects of playlists and related models.  Playlists are stored in the database (see PlaylistStore).
 *
 * When the database isn't available at startup, no playlist is loaded and the playlist is never saved, so that empty
 * models can't replace the stored playlist.  The playlist loads when the database becomes available.
 */
public class PlaylistManager implements Listener<ChannelEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaylistManager.class);

    public static final int PLAYLIST_CURRENT_VERSION = 4;
    private static final long SAVE_RETRY_SECONDS = 30;

    private AliasModel mAliasModel;
    private ChannelMapModel mChannelMapModel = new ChannelMapModel();
    private IconModel mIconModel;

    private BroadcastModel mBroadcastModel;
    private ChannelModel mChannelModel;
    private ChannelProcessingManager mChannelProcessingManager;
    private TunerManager mTunerManager;
    private Settings mSettings;
    private RadioReference mRadioReference;
    private AliasListSynchronizer mAliasListSynchronizer;
    private final Object mLoadLock = new Object();
    private final DatabaseManager mDatabaseManager;
    private final PlaylistStore mPlaylistStore;
    private final ExecutorService mDatabaseEventExecutor =
        Executors.newSingleThreadExecutor(new NamingThreadFactory("sdrtrunk playlist database events"));
    private final Listener<DatabaseEvent> mDatabaseEventListener = this::receiveDatabaseEvent;
    private final List<Runnable> mDelayedLoadListeners = new CopyOnWriteArrayList<>();
    private AtomicBoolean mPlaylistSavePending = new AtomicBoolean();
    private ScheduledFuture<?> mPlaylistSaveFuture;
    private volatile boolean mPlaylistLoading = false;
    private volatile boolean mPlaylistLoaded = false;
    private volatile String mLoadError;
    private List<IAliasListRefreshListener> mAliasListRefreshListeners = new ArrayList<>();

    /**
     * Playlist manager - manages all channel configurations, channel maps, and alias lists and handles loading or
     * persisting the current playlist to the database
     *
     * Monitors playlist changes to automatically save configuration changes after they occur.
     *
     * @param settings for user settings
     * @param tunerManager for access to tuner model
     * @param aliasModel for aliases
     * @param eventLogManager for event logging
     * @param iconModel for icons
     * @param databaseManager that stores the playlists
     */
    public PlaylistManager(Settings settings, TunerManager tunerManager, AliasModel aliasModel,
                           EventLogManager eventLogManager, IconModel iconModel, DatabaseManager databaseManager)
    {
        mSettings = settings;
        mDatabaseManager = databaseManager;
        mPlaylistStore = new PlaylistStore(databaseManager, settings.getPlaylistSettings());
        mTunerManager = tunerManager;
        mAliasModel = aliasModel;
        mIconModel = iconModel;

        mBroadcastModel = new BroadcastModel(mAliasModel, mIconModel, settings);
        mRadioReference = new RadioReference(mSettings);
        mAliasListSynchronizer = new AliasListSynchronizer(mAliasModel, mRadioReference, mSettings);

        mChannelModel = new ChannelModel(mAliasModel);
        mChannelProcessingManager = new ChannelProcessingManager(mChannelMapModel, eventLogManager, mTunerManager,
            mAliasModel, mSettings);

        //Register the channel processing manager to receive global channel stop processing requests so that it can
        //respond to tuner shutdown (ie error) events
        MyEventBus.getGlobalEventBus().register(mChannelProcessingManager);

        mChannelModel.addListener(mChannelProcessingManager);
        mChannelProcessingManager.addChannelEventListener(mChannelModel);

        //Register for alias, channel and channel map events so that we can
        //save the playlist when there are any changes
        mChannelModel.addListener(this);

        mAliasModel.aliasList().addListener((ListChangeListener<Alias>)c -> schedulePlaylistSave());
        mAliasModel.aliasListConfigurations()
            .addListener((ListChangeListener<AliasListConfiguration>)c -> schedulePlaylistSave());

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
     * Synchronizer for alias lists that track a Radio Reference system.
     */
    public AliasListSynchronizer getAliasListSynchronizer()
    {
        return mAliasListSynchronizer;
    }

    /**
     * Channel Map model managed by this playlist manager
     */
    public ChannelMapModel getChannelMapModel()
    {
        return mChannelMapModel;
    }


    /**
     * Loads the active playlist from the database and starts the alias list synchronizer.  When the database isn't
     * available, no playlist is loaded (see {@link #getLoadError()}) and the playlist loads when the database becomes
     * available, followed by a notification to the delayed load listeners.
     */
    public void init()
    {
        mDatabaseManager.addListener(mDatabaseEventListener);
        loadActive();
        mAliasListSynchronizer.start();
    }

    /**
     * Saves any pending playlist changes and stops listening for database events.  Invoke before stopping the
     * database manager.
     */
    public void stop()
    {
        mDatabaseManager.removeListener(mDatabaseEventListener);
        mDatabaseEventExecutor.shutdownNow();

        synchronized(mLoadLock)
        {
            saveNow();
        }
    }

    /**
     * Indicates if a playlist is loaded.  False when the database wasn't available to load the playlist.
     */
    public boolean isLoaded()
    {
        return mPlaylistLoaded;
    }

    /**
     * Reason that the playlist couldn't be loaded, or null when the playlist is loaded
     */
    public String getLoadError()
    {
        return mLoadError;
    }

    /**
     * Name of the loaded playlist, or null when no playlist is loaded
     */
    public String getActivePlaylistName()
    {
        return mPlaylistStore.getActiveName();
    }

    /**
     * Adds a listener to be notified (on a background thread) when a playlist loads after startup: when the database
     * becomes available after it wasn't available at startup, or after a backup restore replaced the playlists.
     */
    public void addDelayedLoadListener(Runnable listener)
    {
        mDelayedLoadListeners.add(listener);
    }

    /**
     * Loads the active playlist into the models
     * @return true if loaded
     */
    private boolean loadActive()
    {
        synchronized(mLoadLock)
        {
            try
            {
                PlaylistV2 playlist = mPlaylistStore.loadActive();
                transferPlaylistToModels(playlist);
                mLoadError = null;
                mPlaylistLoaded = true;
                return true;
            }
            catch(PlaylistStoreException pse)
            {
                mLoadError = pse.getMessage();
                mLog.error("Unable to load the playlist from the database - no channels or aliases are loaded until " +
                    "the database is available: " + pse.getMessage());
                return false;
            }
        }
    }

    /**
     * Saves the current playlist and then loads the named playlist and makes it the active playlist.  When the named
     * playlist can't be loaded, the current playlist stays loaded.
     * @param name of the playlist to load
     * @throws PlaylistStoreException if the current playlist can't be saved or the named playlist can't be loaded
     */
    public void selectPlaylist(String name) throws PlaylistStoreException
    {
        synchronized(mLoadLock)
        {
            if(!saveNow())
            {
                throw new PlaylistStoreException("Unable to save the changes of the current playlist");
            }

            PlaylistV2 playlist = mPlaylistStore.load(name);
            transferPlaylistToModels(playlist);
            mLoadError = null;
            mPlaylistLoaded = true;
        }
    }

    /**
     * Playlists in the database, ordered by name
     */
    public List<PlaylistRecord> getPlaylists() throws PlaylistStoreException
    {
        return mPlaylistStore.list();
    }

    /**
     * Creates an empty playlist
     */
    public PlaylistRecord createPlaylist(String name) throws PlaylistStoreException
    {
        return mPlaylistStore.create(name);
    }

    /**
     * Creates a copy of the playlist, including the latest changes when it is the active playlist
     */
    public PlaylistRecord copyPlaylist(String source, String name) throws PlaylistStoreException
    {
        saveIfActive(source);
        return mPlaylistStore.copy(source, name);
    }

    /**
     * Renames the playlist
     */
    public void renamePlaylist(String name, String newName) throws PlaylistStoreException
    {
        synchronized(mLoadLock)
        {
            mPlaylistStore.rename(name, newName);
        }
    }

    /**
     * Deletes the playlist.  The active playlist can't be deleted.
     */
    public void deletePlaylist(String name) throws PlaylistStoreException
    {
        mPlaylistStore.delete(name);
    }

    /**
     * Imports a playlist file as a new playlist.  The file isn't changed.
     */
    public PlaylistRecord importPlaylist(Path file, String name) throws PlaylistStoreException
    {
        return mPlaylistStore.importFile(file, name);
    }

    /**
     * Exports the playlist to a playlist file, including the latest changes when it is the active playlist
     */
    public void exportPlaylist(String name, Path file) throws PlaylistStoreException
    {
        saveIfActive(name);
        mPlaylistStore.exportFile(name, file);
    }

    private void saveIfActive(String name) throws PlaylistStoreException
    {
        synchronized(mLoadLock)
        {
            if(name.equals(mPlaylistStore.getActiveName()) && !saveNow())
            {
                throw new PlaylistStoreException("Unable to save the changes of the current playlist");
            }
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

    /**
     * Cancels a scheduled save and saves now when there are unsaved changes.
     * @return true if there was nothing to save or the save succeeded
     */
    private boolean saveNow()
    {
        cancelScheduledSave();

        if(mPlaylistSavePending.getAndSet(false))
        {
            if(!save())
            {
                scheduleRetry();
                return false;
            }
        }

        return true;
    }

    private void cancelScheduledSave()
    {
        ScheduledFuture<?> future = mPlaylistSaveFuture;

        if(future != null)
        {
            future.cancel(false);
            mPlaylistSaveFuture = null;
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
            mAliasModel.addAliasListConfigurations(playlist.getAliasListConfigurations());


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
     * Playlist model objects in stored order: aliases, alias lists, broadcast configurations, channel maps, channels.
     */
    private List<Object> getModelObjects()
    {
        PlaylistV2 playlist = new PlaylistV2();
        playlist.setAliases(new ArrayList<>(mAliasModel.getAliases()));
        playlist.setAliasListConfigurations(new ArrayList<>(mAliasModel.aliasListConfigurations()));
        playlist.setBroadcastConfigurations(new ArrayList<>(mBroadcastModel.getBroadcastConfigurations()));
        playlist.setChannelMaps(new ArrayList<>(mChannelMapModel.getChannelMaps()));
        playlist.setChannels(new ArrayList<>(mChannelModel.getChannels()));
        return PlaylistCodec.getModelObjects(playlist);
    }

    /**
     * Saves the current playlist to the database
     * @return true if saved (or nothing to save), false on error
     */
    private boolean save()
    {
        if(!mPlaylistLoaded)
        {
            //Never write the empty models of a playlist that wasn't loaded
            return true;
        }

        try
        {
            mPlaylistStore.save(getModelObjects(), PLAYLIST_CURRENT_VERSION);
            return true;
        }
        catch(PlaylistReplacedException pre)
        {
            mLog.info("Playlist changes were not saved because a backup restore replaced the playlists");
            return true;
        }
        catch(PlaylistStoreException pse)
        {
            mLog.error("Unable to save the playlist to the database - will retry: " + pse.getMessage());
            return false;
        }
    }

    /**
     * Schedules a playlist save task.  Subsequent calls to this method will be ignored until the save event occurs,
     * thus limiting repetitive playlist saving to a minimum.
     */
    public void schedulePlaylistSave()
    {
        if(!mPlaylistLoading && mPlaylistLoaded)
        {
            if(mPlaylistSavePending.compareAndSet(false, true))
            {
                mPlaylistSaveFuture = ThreadPool.SCHEDULED.schedule(new PlaylistSaveTask(), 2, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Schedules another save attempt after a failed save
     */
    private void scheduleRetry()
    {
        if(mPlaylistSavePending.compareAndSet(false, true))
        {
            mPlaylistSaveFuture = ThreadPool.SCHEDULED.schedule(new PlaylistSaveTask(), SAVE_RETRY_SECONDS,
                TimeUnit.SECONDS);
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
            synchronized(mLoadLock)
            {
                mPlaylistSaveFuture = null;

                //Clear the flag before saving, so that a change during the save schedules another save
                if(mPlaylistSavePending.getAndSet(false) && !save())
                {
                    scheduleRetry();
                }
            }
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    // Database events
    //----------------------------------------------------------------------------------------------------------------

    /**
     * Receives database events on the database thread and handles them on the playlist database event thread, since
     * the playlist store waits for work on the database thread.
     */
    private void receiveDatabaseEvent(DatabaseEvent event)
    {
        try
        {
            switch(event.type())
            {
                case CONNECTED -> mDatabaseEventExecutor.execute(this::loadIfNotLoaded);
                case SWITCHED -> mDatabaseEventExecutor.execute(this::databaseSwitched);
                case PLAYLISTS_RESTORED -> mDatabaseEventExecutor.execute(this::playlistsRestored);
                default -> {}
            }
        }
        catch(Exception e)
        {
            //Executor shut down while the application stops
        }
    }

    /**
     * Loads the playlist when the database becomes available after it wasn't available at startup
     */
    private void loadIfNotLoaded()
    {
        boolean loaded;

        synchronized(mLoadLock)
        {
            loaded = !mPlaylistLoaded && loadActive();
        }

        if(loaded)
        {
            mLog.info("The database is available - loaded playlist [" + getActivePlaylistName() + "]");
            notifyDelayedLoad();
        }
    }

    /**
     * Writes the running playlist to the database that was switched to, so that the new database has the playlist as
     * it is now.  The save detects the new database and writes all of the playlist items.
     */
    private void databaseSwitched()
    {
        if(!mPlaylistLoaded)
        {
            loadIfNotLoaded();
            return;
        }

        synchronized(mLoadLock)
        {
            cancelScheduledSave();
            mPlaylistSavePending.set(false);

            if(!save())
            {
                scheduleRetry();
            }
        }
    }

    /**
     * Reloads the active playlist after a backup restore replaced the playlists.
     */
    private void playlistsRestored()
    {
        boolean loaded;

        synchronized(mLoadLock)
        {
            cancelScheduledSave();
            mPlaylistSavePending.set(false);
            mPlaylistLoaded = false;
            mPlaylistStore.unload();
            loaded = loadActive();
        }

        if(loaded)
        {
            mLog.info("Reloaded playlist [" + getActivePlaylistName() + "] after the backup restore");
            notifyDelayedLoad();
        }
        else
        {
            //Nothing is loaded, so clear the models of the replaced playlist
            clearModels();
        }
    }

    private void notifyDelayedLoad()
    {
        for(Runnable listener: mDelayedLoadListeners)
        {
            try
            {
                listener.run();
            }
            catch(Exception e)
            {
                mLog.error("Error notifying playlist load listener", e);
            }
        }
    }
}
