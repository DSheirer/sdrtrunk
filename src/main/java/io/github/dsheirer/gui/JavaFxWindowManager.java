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
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.map.ChannelMap;
import io.github.dsheirer.controller.channel.map.ChannelRange;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.icon.IconManager;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.playlist.PlaylistEditor;
import io.github.dsheirer.gui.playlist.PlaylistEditorRequest;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.gui.playlist.channelMap.ChannelMapEditor;
import io.github.dsheirer.gui.playlist.channelMap.ViewChannelMapEditorRequest;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.UserPreferencesEditor;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.gui.preference.calibration.CalibrationDialog;
import io.github.dsheirer.gui.preference.colortheme.ColorThemeManager;
import io.github.dsheirer.gui.viewer.MessageRecordingViewer;
import io.github.dsheirer.gui.viewer.ViewRecordingViewerRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.jmbe.JmbeEditor;
import io.github.dsheirer.jmbe.JmbeEditorRequest;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.monitor.ResourceMonitor;
import io.github.dsheirer.monitor.StatusBox;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.stage.Stage;
import jiconfont.javafx.IconFontFX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Java FX window manager.  Handles all secondary Java FX windows that are used within this primarily
 * Swing application.
 */
public class JavaFxWindowManager extends Application
{
    private final static Logger mLog = LoggerFactory.getLogger(JavaFxWindowManager.class);

    public static final String CHANNEL_MAP_EDITOR = "channelmap";
    public static final String ICON_MANAGER = "iconmanager";
    public static final String PLAYLIST_EDITOR = "playlist";
    public static final String USER_PREFERENCES_EDITOR = "preferences";
    public static final String STAGE_MONITOR_KEY_CALIBRATION_DIALOG = "calibration.dialog";
    public static final String STAGE_MONITOR_KEY_CHANNEL_MAP_EDITOR = "channel.map";
    public static final String STAGE_MONITOR_KEY_RECORDING_VIEWER = "recording.viewer";
    public static final String STAGE_MONITOR_KEY_ICON_MANAGER_EDITOR = "icon.manager";
    public static final String STAGE_MONITOR_KEY_JMBE_EDITOR = "jmbe.editor";
    public static final String STAGE_MONITOR_KEY_PLAYLIST_EDITOR = "playlist";
    public static final String STAGE_MONITOR_KEY_USER_PREFERENCES_EDITOR = "user.preferences";

    private JFXPanel mJFXPanel;
    private ChannelMapEditor mChannelMapEditor;
    private IconManager mIconManager;
    private JmbeEditor mJmbeEditor;
    private PlaylistEditor mPlaylistEditor;
    private PlaylistManager mPlaylistManager;
    private TunerManager mTunerManager;
    private UserPreferences mUserPreferences;
    private UserPreferencesEditor mUserPreferencesEditor;
    private MessageRecordingViewer mMessageRecordingViewer;

    private Stage mChannelMapStage;
    private Stage mIconManagerStage;
    private Stage mJmbeEditorStage;
    private Stage mPlaylistStage;
    private Stage mUserPreferencesStage;
    private Stage mRecordingViewerStage;
    private JFXPanel mStatusPanel;

    /**
     * Constructs an instance.  Note: this constructor is used for Swing applications.
     */
    public JavaFxWindowManager(UserPreferences userPreferences, TunerManager tunerManager, PlaylistManager playlistManager)
    {
        mUserPreferences = userPreferences;
        mTunerManager = tunerManager;
        mPlaylistManager = playlistManager;

        setup();
    }

    /**
     * Constructs an instance.  Note: this constructor is used for standalone JavaFX application testing
     */
    public JavaFxWindowManager()
    {
        mUserPreferences = new UserPreferences();
        AliasModel aliasModel = new AliasModel();
        EventLogManager eventLogManager = new EventLogManager(aliasModel, mUserPreferences);
        mTunerManager = new TunerManager(mUserPreferences);
        mTunerManager.start();
        mPlaylistManager = new PlaylistManager(mUserPreferences, mTunerManager, aliasModel, eventLogManager, new IconModel());
        mPlaylistManager.init();
        setup();
    }

    /**
     * Creates or accesses the JavaFX status panel, used by the main application GUI.
     * @param resourceMonitor for statistics
     * @return JFXPanel accessible on Swing thread that delegates JavaFX scene creation to the FX event thread.
     */
    public JFXPanel getStatusPanel(ResourceMonitor resourceMonitor)
    {
        if(mStatusPanel == null)
        {
            mStatusPanel = new JFXPanel();

            //JFXPanel has to be populated on the FX event thread
            Platform.runLater(() -> {
                Scene scene = new Scene(new StatusBox(resourceMonitor));
                ColorThemeManager.applyThemeToScene(scene, mUserPreferences);
                mStatusPanel.setScene(scene);
            });
        }

        return mStatusPanel;
    }

    private void setup()
    {
        //Register this class to receive events via each method annotated with @Subscribe
        MyEventBus.getGlobalEventBus().register(this);

        //Register JavaFX icon fonts
        IconFontFX.register(jiconfont.icons.font_awesome.FontAwesome.getIconFont());

        createJFXPanel();
    }

    /**
     * Executes the runnable on the JavaFX application thread
     */
    private void execute(Runnable runnable)
    {
        createJFXPanel();

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
     * Creates a JavaFX panel for Swing application compatibility
     */
    private void createJFXPanel()
    {
        if(mJFXPanel == null)
        {
            mJFXPanel = new JFXPanel();
            Platform.setImplicitExit(false);
        }
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
        createJFXPanel();
        return new CalibrationDialog(userPreferences);
    }

    /**
     * Stage for the recording viewer
     */
    public Stage getRecordingViewerStage()
    {
        if(mRecordingViewerStage == null)
        {
            createJFXPanel();
            Scene scene = new Scene(getRecordingViewer(), 1100, 800);
            ColorThemeManager.applyThemeToScene(scene, mUserPreferences);
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
            createJFXPanel();
            Scene scene = new Scene(getIconManager(), 500, 500);
            ColorThemeManager.applyThemeToScene(scene, mUserPreferences);
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
            createJFXPanel();
            Scene scene = new Scene(getJmbeEditor(), 650, 650);
            ColorThemeManager.applyThemeToScene(scene, mUserPreferences);
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
            createJFXPanel();
            Scene scene = new Scene(getPlaylistEditor(), 1000, 750);
            ColorThemeManager.applyThemeToScene(scene, mUserPreferences);
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
            mUserPreferencesEditor = new UserPreferencesEditor(mUserPreferences);
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
            createJFXPanel();
            Scene scene = new Scene(getUserPreferencesEditor(), 900, 500);
            ColorThemeManager.applyThemeToScene(scene, mUserPreferences);
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
            createJFXPanel();
            Scene scene = new Scene(getChannelMapEditor(), 500, 500);
            ColorThemeManager.applyThemeToScene(scene, mUserPreferences);
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

    @Override
    public void start(Stage primaryStage) throws Exception
    {
        mLog.debug("Starting ...");
        Parameters parameters = getParameters();
        mLog.debug("Parameters: " + (parameters != null));

        boolean valid = false;

        if(parameters != null && parameters.getRaw().size() == 1)
        {
            String window = parameters.getRaw().get(0);

            if(window != null)
            {
                switch(window)
                {
                    case CHANNEL_MAP_EDITOR:
                        //Generate some test data for the editor
                        ChannelMap channelMap1 = new ChannelMap("Test Map 1");
                        channelMap1.addRange(new ChannelRange(1,199,150000000, 12500));
                        channelMap1.addRange(new ChannelRange(200,299,160000000, 25000));
                        channelMap1.addRange(new ChannelRange(300,399,170000000, 12500));
                        channelMap1.addRange(new ChannelRange(400,499,180000000, 25000));
                        mPlaylistManager.getChannelMapModel().addChannelMap(channelMap1);

                        ChannelMap channelMap2 = new ChannelMap("Test Map 2");
                        channelMap2.addRange(new ChannelRange(1,199,450000000, 12500));
                        channelMap2.addRange(new ChannelRange(200,299,460000000, 25000));
                        channelMap2.addRange(new ChannelRange(300,399,470000000, 12500));
                        channelMap2.addRange(new ChannelRange(400,499,480000000, 25000));
                        mPlaylistManager.getChannelMapModel().addChannelMap(channelMap2);
                        valid = true;
                        process(new ViewChannelMapEditorRequest());
                        break;
                    case ICON_MANAGER:
                        valid = true;
                        process(new ViewIconManagerRequest());
                        break;
                    case PLAYLIST_EDITOR:
                        valid = true;
                        process(new ViewPlaylistRequest());
                        break;
                    case USER_PREFERENCES_EDITOR:
                        valid = true;
                        process(new ViewUserPreferenceEditorRequest(PreferenceEditorType.DEFAULT));
                        break;
                    default:
                        break;
                }
            }
        }

        if(!valid)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("An argument is required to launch JavaFX windows from this window manager.  " +
                "Valid options are:\n\tchannelmap\tChannel Map Editor\n\ticonmanager\tIcon Manager\n\tplaylist\tPlaylist Editor\n" +
                "\tpreferences\tUser Preferences Editor\n");
            sb.append("Supplied Argument(s): ").append(parameters.getRaw());

            mLog.error(sb.toString());
        }
    }

    public static void main(String[] args)
    {
        mLog.info("Application Start - Parameters: " + args);
        launch(args);
    }
}
