/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.gui.JavaFxWindowManager;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import jakarta.annotation.PostConstruct;
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
    private TunerManager mTunerManager;
    private UserPreferences mUserPreferences = new UserPreferences();
    private PlaylistManager mPlaylistManager;
    private JavaFxWindowManager mJavaFxWindowManager;

    public PlaylistEditorApplication()
    {
    }

    @PostConstruct
    public void postConstruct()
    {
        EventLogManager eventLogManager = new EventLogManager();
        mTunerManager = new TunerManager();
        mTunerManager.start();
        mPlaylistManager = new PlaylistManager();
        mJavaFxWindowManager = new JavaFxWindowManager();
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
            mPlaylistEditor = new PlaylistEditor();
        }

        return mPlaylistEditor;
    }

    public static void main(String[] args)
    {
        launch(args);
    }
}
