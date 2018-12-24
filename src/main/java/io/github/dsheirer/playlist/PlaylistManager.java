/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.playlist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.github.dsheirer.alias.AliasEvent;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.controller.channel.Channel.ChannelType;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.map.ChannelMapEvent;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.playlist.FilePreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlaylistManager implements Listener<ChannelEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(PlaylistManager.class);

    public static final int PLAYLIST_CURRENT_VERSION = 2;

    private AliasModel mAliasModel;
    private BroadcastModel mBroadcastModel;
    private ChannelModel mChannelModel;
    private ChannelMapModel mChannelMapModel;
    private UserPreferences mUserPreferences;
    private AtomicBoolean mPlaylistSavePending = new AtomicBoolean();
    private boolean mPlaylistLoading = false;

    /**
     * Playlist manager - manages all channel configurations, channel maps, and
     * alias lists and handles loading or persisting to a playlist.xml file
     *
     * Monitors playlist changes to automatically save configuration changes
     * after they occur.
     *
     * @param channelModel
     */
    public PlaylistManager(AliasModel aliasModel, BroadcastModel broadcastModel, ChannelModel channelModel,
                           ChannelMapModel channelMapModel, UserPreferences userPreferences)
    {
        mAliasModel = aliasModel;
        mBroadcastModel = broadcastModel;
        mChannelModel = channelModel;
        mChannelMapModel = channelMapModel;
        mUserPreferences = userPreferences;

        //Register for alias, channel and channel map events so that we can
        //save the playlist when there are any changes
        mChannelModel.addListener(this);

        mAliasModel.addListener(new Listener<AliasEvent>()
        {
            @Override
            public void receive(AliasEvent t)
            {
                //Save the playlist for all alias events
                schedulePlaylistSave();
            }
        });

        mChannelMapModel.addListener(new Listener<ChannelMapEvent>()
        {
            @Override
            public void receive(ChannelMapEvent t)
            {
                //Save the playlist for all channel map events
                schedulePlaylistSave();
            }
        });

        mBroadcastModel.addListener(new Listener<BroadcastEvent>()
        {
            @Override
            public void receive(BroadcastEvent broadcastEvent)
            {
                switch(broadcastEvent.getEvent())
                {
                    case CONFIGURATION_ADD:
                    case CONFIGURATION_CHANGE:
                    case CONFIGURATION_DELETE:
                        schedulePlaylistSave();
                        break;
                    case BROADCASTER_ADD:
                    case BROADCASTER_QUEUE_CHANGE:
                    case BROADCASTER_STATE_CHANGE:
                    case BROADCASTER_STREAMED_COUNT_CHANGE:
                    case BROADCASTER_AGED_OFF_COUNT_CHANGE:
                    case BROADCASTER_DELETE:
                    default:
                        //Do nothing
                        break;
                }
            }
        });
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
     * Transfers data from persisted playlist into system models
     */
    private void transferPlaylistToModels(PlaylistV2 playlist)
    {
        if(playlist != null)
        {
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
        FilePreferences files = mUserPreferences.getFilePreferences();

        PlaylistV2 playlist = new PlaylistV2();

        playlist.setAliases(mAliasModel.getAliases());
        playlist.setBroadcastConfigurations(mBroadcastModel.getBroadcastConfigurations());
        playlist.setChannels(mChannelModel.getChannels());
        playlist.setChannelMaps(mChannelMapModel.getChannelMaps());
        playlist.setVersion(PLAYLIST_CURRENT_VERSION);

        //Create a backup copy of the current playlist
        if(Files.exists(files.getPlaylist()))
        {
            try
            {
                Files.copy(files.getPlaylist(), files.getPlaylistBackup(), StandardCopyOption.REPLACE_EXISTING);
            }
            catch(Exception e)
            {
                mLog.error("Error creating backup copy of current playlist prior to saving updates [" +
                    files.getPlaylist().toString() + "]", e);
            }
        }

        //Create a temporary lock file to signify that we're in the process of updating the playlist
        if(!Files.exists(files.getPlaylistLock()))
        {
            try
            {
                Files.createFile(files.getPlaylistLock());
            }
            catch(IOException e)
            {
                mLog.error("Error creating temporary lock file prior to saving playlist [" +
                    files.getPlaylistLock().toString() + "]", e);
            }
        }

        try(OutputStream out = Files.newOutputStream(files.getPlaylist()))
        {
            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(out, playlist);
            out.flush();

            //Remove the playlist lock file to indicate that we successfully saved the file
            if(Files.exists(files.getPlaylistLock()))
            {
                Files.delete(files.getPlaylistLock());
            }

            mUserPreferences.getFilePreferences().setPlaylistLastAccessedPath(files.getPlaylist());
        }
        catch(IOException ioe)
        {
            mLog.error("IO error while writing the playlist to a file [" + files.getPlaylist().toString() + "]", ioe);
        }
        catch(Exception e)
        {
            mLog.error("Error while saving playlist [" + files.getPlaylist().toString() + "]", e);
        }
    }

    /**
     * Loads a version 2 playlist
     */
    public PlaylistV2 load()
    {
        FilePreferences files = mUserPreferences.getFilePreferences();

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
            mLog.info("Loading playlist file [" + files.getPlaylist().toString() + "]");

            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule);

            try(InputStream in = Files.newInputStream(files.getPlaylist()))
            {
                playlist = objectMapper.readValue(in, PlaylistV2.class);
            }
            catch(IOException ioe)
            {
                mLog.error("IO error while reading playlist file", ioe);
            }
        }
        else if(Files.exists(files.getLegacyPlaylist()))
        {
            mLog.info("Loading legacy playlist file [" + files.getLegacyPlaylist().toString() + "]");

            JacksonXmlModule xmlModule = new JacksonXmlModule();
            xmlModule.setDefaultUseWrapper(false);
            ObjectMapper objectMapper = new XmlMapper(xmlModule);

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
    private void schedulePlaylistSave()
    {
        if(!mPlaylistLoading)
        {
            if(mPlaylistSavePending.compareAndSet(false, true))
            {
                ThreadPool.SCHEDULED.schedule(new PlaylistSaveTask(), 2, TimeUnit.SECONDS);
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

            mPlaylistSavePending.set(false);
        }
    }
}
