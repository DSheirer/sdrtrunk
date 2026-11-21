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

package io.github.dsheirer.gui;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.icon.IconManager;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.playlist.PlaylistEditor;
import io.github.dsheirer.gui.playlist.PlaylistEditorRequest;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.gui.playlist.channelMap.ChannelMapEditor;
import io.github.dsheirer.gui.playlist.channelMap.ViewChannelMapEditorRequest;
import io.github.dsheirer.gui.preference.UserPreferencesEditor;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.gui.preference.calibration.CalibrationDialog;
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.gui.viewer.MessageRecordingViewer;
import io.github.dsheirer.gui.viewer.ViewRecordingViewerRequest;
import io.github.dsheirer.jmbe.JmbeEditor;
import io.github.dsheirer.jmbe.JmbeEditorRequest;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jiconfont.javafx.IconFontFX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens the application's tool windows (playlist editor, user preferences, icon manager, channel map editor, JMBE
 * library editor and message recording viewer) when it receives their request events on the event bus.  Each window
 * is created once, then restored and brought to the front on later requests, and its size and location are kept
 * across sessions.
 */
public class WindowManager
{
    private final static Logger mLog = LoggerFactory.getLogger(WindowManager.class);

    public static final String STAGE_MONITOR_KEY_CALIBRATION_DIALOG = "calibration.dialog";
    public static final String STAGE_MONITOR_KEY_CHANNEL_MAP_EDITOR = "channel.map";
    public static final String STAGE_MONITOR_KEY_RECORDING_VIEWER = "recording.viewer";
    public static final String STAGE_MONITOR_KEY_ICON_MANAGER_EDITOR = "icon.manager";
    public static final String STAGE_MONITOR_KEY_JMBE_EDITOR = "jmbe.editor";
    public static final String STAGE_MONITOR_KEY_PLAYLIST_EDITOR = "playlist";
    public static final String STAGE_MONITOR_KEY_USER_PREFERENCES_EDITOR = "user.preferences";

    private ChannelMapEditor mChannelMapEditor;
    private IconManager mIconManager;
    private JmbeEditor mJmbeEditor;
    private PlaylistEditor mPlaylistEditor;
    private PlaylistManager mPlaylistManager;
    private TunerManager mTunerManager;
    private UserPreferences mUserPreferences;
    private final DatabaseManager mDatabaseManager;
    private UserPreferencesEditor mUserPreferencesEditor;
    private MessageRecordingViewer mMessageRecordingViewer;

    private Stage mChannelMapStage;
    private Stage mIconManagerStage;
    private Stage mJmbeEditorStage;
    private Stage mPlaylistStage;
    private Stage mUserPreferencesStage;
    private Stage mRecordingViewerStage;

    /**
     * Constructs an instance and registers it to receive window requests on the event bus.
     * @param userPreferences for the windows and their stored locations
     * @param tunerManager for the playlist editor
     * @param playlistManager for the playlist and channel map editors
     * @param databaseManager for the call database preference editor
     */
    public WindowManager(UserPreferences userPreferences, TunerManager tunerManager, PlaylistManager playlistManager,
                         DatabaseManager databaseManager)
    {
        mUserPreferences = userPreferences;
        mDatabaseManager = databaseManager;
        mTunerManager = tunerManager;
        mPlaylistManager = playlistManager;

        setup();
    }

    private void setup()
    {
        //Register this class to receive events via each method annotated with @Subscribe
        MyEventBus.getGlobalEventBus().register(this);

        //Register JavaFX icon fonts
        IconFontFX.register(jiconfont.icons.font_awesome.FontAwesome.getIconFont());

        startToolkit();
    }

    /**
     * Executes the runnable on the JavaFX application thread
     */
    private void execute(Runnable runnable)
    {
        startToolkit();

        if(Platform.isFxApplicationThread())
        {
            runnable.run();
        }
        else
        {
            Platform.runLater(runnable);
        }
    }

    /**
     * Starts the JavaFX toolkit if it is not running
     */
    private void startToolkit()
    {
        FxToolkit.ensureStarted();
    }

    /**
     * Removes monitoring for all JavaFX stages and shuts down the FX thread, killing all FX windows.
     */
    public void shutdown()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
        mUserPreferences.getJavaFxPreferences().clearStageMonitors();
        Platform.exit();
    }

    public CalibrationDialog getCalibrationDialog(UserPreferences userPreferences)
    {
        startToolkit();
        return new CalibrationDialog(userPreferences);
    }

    /**
     * Stage for the recording viewer
     */
    public Stage getRecordingViewerStage()
    {
        if(mRecordingViewerStage == null)
        {
            startToolkit();
            Scene scene = new Scene(getRecordingViewer(), 1100, 800);
            ThemeManager.getInstance().register(scene);
            mRecordingViewerStage = new Stage();
            mRecordingViewerStage.setTitle("sdrtrunk - Message Recording Viewer (.bits)");
            mRecordingViewerStage.setScene(scene);
            mUserPreferences.getJavaFxPreferences().monitor(mRecordingViewerStage, STAGE_MONITOR_KEY_RECORDING_VIEWER);
        }

        return mRecordingViewerStage;
    }

    public MessageRecordingViewer getRecordingViewer()
    {
        if(mMessageRecordingViewer == null)
        {
            mMessageRecordingViewer = new MessageRecordingViewer();
        }

        return mMessageRecordingViewer;
    }

    public Stage getIconManagerStage()
    {
        if(mIconManagerStage == null)
        {
            startToolkit();
            Scene scene = new Scene(getIconManager(), 500, 500);
            ThemeManager.getInstance().register(scene);
            mIconManagerStage = new Stage();
            mIconManagerStage.setTitle("sdrtrunk - Icon Manager");
            mIconManagerStage.setScene(scene);
            mUserPreferences.getJavaFxPreferences().monitor(mIconManagerStage, STAGE_MONITOR_KEY_ICON_MANAGER_EDITOR);
        }

        return mIconManagerStage;
    }

    public IconManager getIconManager()
    {
        if(mIconManager == null)
        {
            mIconManager = new IconManager(mPlaylistManager.getIconModel());
        }

        return mIconManager;
    }

    /**
     * Processes a JMBE s editor request
     */
    @Subscribe
    public void process(final JmbeEditorRequest request)
    {
        if(request.isCloseEditorRequest())
        {
            execute(() -> {
                getJmbeEditorStage().hide();
                mJmbeEditorStage = null;
                mJmbeEditor = null;
            });
        }
        else
        {
            execute(() -> {
                restoreStage(getJmbeEditorStage());
                getJmbeEditor().process(request);
            });
        }
    }

    public Stage getJmbeEditorStage()
    {
        if(mJmbeEditorStage == null)
        {
            startToolkit();
            Scene scene = new Scene(getJmbeEditor(), 650, 650);
            ThemeManager.getInstance().register(scene);
            mJmbeEditorStage = new Stage();
            mJmbeEditorStage.setTitle("sdrtrunk - JMBE Library Updater");
            mJmbeEditorStage.setScene(scene);
            mUserPreferences.getJavaFxPreferences().monitor(mJmbeEditorStage, STAGE_MONITOR_KEY_JMBE_EDITOR);
        }

        return mJmbeEditorStage;
    }

    public JmbeEditor getJmbeEditor()
    {
        if(mJmbeEditor == null)
        {
            mJmbeEditor = new JmbeEditor(mUserPreferences);
        }

        return mJmbeEditor;
    }

    /**
     * Lazy construct and access the playlist editor
     */
    public PlaylistEditor getPlaylistEditor()
    {
        if(mPlaylistEditor == null)
        {
            mPlaylistEditor = new PlaylistEditor(mPlaylistManager, mTunerManager, mUserPreferences);
        }

        return mPlaylistEditor;
    }

    /**
     * Access the playlist stage.
     */
    private Stage getPlaylistStage()
    {
        if(mPlaylistStage == null)
        {
            startToolkit();
            Scene scene = new Scene(getPlaylistEditor(), 1000, 750);
            ThemeManager.getInstance().register(scene);
            mPlaylistStage = new Stage();
            mPlaylistStage.setTitle("sdrtrunk - Playlist Editor");
            mPlaylistStage.setScene(scene);
            mUserPreferences.getJavaFxPreferences().monitor(mPlaylistStage, STAGE_MONITOR_KEY_PLAYLIST_EDITOR);
        }

        return mPlaylistStage;
    }

    /**
     * Processes a playlist editor request and brings the playlist editor into focus
     */
    @Subscribe
    public void process(PlaylistEditorRequest request)
    {
        execute(() -> {
            try
            {
                restoreStage(getPlaylistStage());
                getPlaylistEditor().process(request);
            }
            catch(Throwable t)
            {
                mLog.error("Error processing show playlist editor request", t);
            }
        });
    }

    /**
     * User Preferences Editor
     */
    private UserPreferencesEditor getUserPreferencesEditor()
    {
        if(mUserPreferencesEditor == null)
        {
            mUserPreferencesEditor = new UserPreferencesEditor(mUserPreferences, mDatabaseManager);
        }

        return mUserPreferencesEditor;
    }

    /**
     * User Preferences Stage
     */
    private Stage getUserPreferencesStage()
    {
        if(mUserPreferencesStage == null)
        {
            startToolkit();
            Scene scene = new Scene(getUserPreferencesEditor(), 900, 500);
            ThemeManager.getInstance().register(scene);
            mUserPreferencesStage = new Stage();
            mUserPreferencesStage.setTitle("sdrtrunk - User Preferences");
            mUserPreferencesStage.setScene(scene);
            mUserPreferences.getJavaFxPreferences().monitor(mUserPreferencesStage, STAGE_MONITOR_KEY_USER_PREFERENCES_EDITOR);
        }

        return mUserPreferencesStage;
    }

    /**
     * Processes a user preferences editor request
     */
    @Subscribe
    public void process(final ViewUserPreferenceEditorRequest request)
    {
        execute(() -> {
            restoreStage(getUserPreferencesStage());
            getUserPreferencesEditor().process(request);
        });
    }

    /**
     * Channel Map Editor
     */
    private ChannelMapEditor getChannelMapEditor()
    {
        if(mChannelMapEditor == null)
        {
            mChannelMapEditor = new ChannelMapEditor(mPlaylistManager.getChannelMapModel());
        }

        return mChannelMapEditor;
    }

    /**
     * Channel Map Stage
     */
    private Stage getChannelMapStage()
    {
        if(mChannelMapStage == null)
        {
            startToolkit();
            Scene scene = new Scene(getChannelMapEditor(), 500, 500);
            ThemeManager.getInstance().register(scene);
            mChannelMapStage = new Stage();
            mChannelMapStage.setTitle("sdrtrunk - Channel Map Editor");
            mChannelMapStage.setScene(scene);
            mUserPreferences.getJavaFxPreferences().monitor(mChannelMapStage, STAGE_MONITOR_KEY_CHANNEL_MAP_EDITOR);
        }

        return mChannelMapStage;
    }

    @Subscribe
    public void process(final ViewIconManagerRequest request)
    {
        execute(() -> restoreStage(getIconManagerStage()));
    }

    /**
     * Process a channel map editor request
     */
    @Subscribe
    public void process(final ViewChannelMapEditorRequest request)
    {
        execute(() -> {
            restoreStage(getChannelMapStage());
            getChannelMapEditor().process(request);
        });
    }

    /**
     * Process a channel map editor request
     */
    @Subscribe
    public void process(final ViewRecordingViewerRequest request)
    {
        execute(() -> restoreStage(getRecordingViewerStage()));
    }

    /**
     * Restores the stage to previous size and location.
     * @param stage to restore.
     */
    private void restoreStage(Stage stage)
    {
        stage.setIconified(false);
        stage.show();
        stage.requestFocus();
        stage.toFront();
    }
}
