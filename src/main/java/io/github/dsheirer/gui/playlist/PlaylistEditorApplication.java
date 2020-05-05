/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.controller.channel.map.ChannelMapModel;
import io.github.dsheirer.gui.JavaFxWindowManager;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.SourceManager;
import io.github.dsheirer.source.tuner.TunerModel;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Simple application wrapper for testing the Playlist Editor individually.
 */
public class PlaylistEditorApplication extends Application
{
    private Stage mStage;
    private Parent mPlaylistEditor;
    private UserPreferences mUserPreferences = new UserPreferences();
    private PlaylistManager mPlaylistManager;
    private JavaFxWindowManager mJavaFxWindowManager;

    public PlaylistEditorApplication()
    {
        AliasModel aliasModel = new AliasModel();
        BroadcastModel broadcastModel = new BroadcastModel(aliasModel, new IconModel(), mUserPreferences);
        ChannelMapModel channelMapModel = new ChannelMapModel();
        TunerConfigurationModel tunerConfigurationModel = new TunerConfigurationModel();
        TunerModel tunerModel = new TunerModel(tunerConfigurationModel);
        SourceManager sourceManager = new SourceManager(tunerModel, new SettingsManager(tunerConfigurationModel),
            mUserPreferences);
        EventLogManager eventLogManager = new EventLogManager(aliasModel, mUserPreferences);
        mPlaylistManager = new PlaylistManager(mUserPreferences, sourceManager, aliasModel, eventLogManager, new IconModel());

        mPlaylistManager.init();
        mJavaFxWindowManager = new JavaFxWindowManager(mUserPreferences, mPlaylistManager);
    }

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        mStage = primaryStage;
        mStage.setTitle("Playlist Editor");
        Scene scene = new Scene(getPlaylistEditor(), 1000, 750);
        mStage.setScene(scene);
        mStage.show();
    }

    private Parent getPlaylistEditor()
    {
        if(mPlaylistEditor == null)
        {
            mPlaylistEditor = new PlaylistEditor(mPlaylistManager, mUserPreferences);
        }

        return mPlaylistEditor;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
