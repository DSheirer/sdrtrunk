/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.DuplicateCallDetector;
import io.github.dsheirer.audio.broadcast.AudioStreamingManager;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelAutoStartWindow;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.controller.channel.ChannelSelectionManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.preference.CalibrateRequest;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.gui.preference.calibration.CalibrationDialog;
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.log.ApplicationLog;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.monitor.DiagnosticMonitor;
import io.github.dsheirer.monitor.ResourceMonitor;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.record.AudioRecordingManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerSpectralDisplayManager;
import io.github.dsheirer.util.DuplicateLibraryDetector;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SDRTrunk implements Listener<TunerEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(SDRTrunk.class);

    private AudioRecordingManager mAudioRecordingManager;
    private AudioStreamingManager mAudioStreamingManager;
    private DiagnosticMonitor mDiagnosticMonitor;
    private IconModel mIconModel = new IconModel();
    private PlaylistManager mPlaylistManager;
    private MainWindow mMainWindow;
    private WindowManager mWindowManager;
    private UserPreferences mUserPreferences = new UserPreferences();
    private TunerManager mTunerManager;
    private ApplicationLog mApplicationLog;
    private ResourceMonitor mResourceMonitor;

    public SDRTrunk()
    {
        try
        {
            initialize();
        }
        catch(Throwable t)
        {
            startupFailed(t);
        }
    }

    /**
     * Creates and starts the application components and shows the main window.
     */
    private void initialize() throws Exception
    {
        if(!GraphicsEnvironment.isHeadless())
        {
            //Initialize the theme manager before any windows are created so that the chosen theme applies to the
            //first window.
            ThemeManager.getInstance().initialize(mUserPreferences);
        }

        mApplicationLog = new ApplicationLog(mUserPreferences);
        mApplicationLog.start();

        Map<String,List<String>> duplicateLibraries = DuplicateLibraryDetector.findDuplicates();

        if(!duplicateLibraries.isEmpty())
        {
            mLog.warn("More than one version of these libraries is in the library folder [" +
                    DuplicateLibraryDetector.getFolder(duplicateLibraries) + "].  This happens when a new version of " +
                    "sdrtrunk is unzipped over an older one, and it can load the older library code.  Delete the " +
                    "sdrtrunk folder and unzip the release into an empty folder.\n" +
                    DuplicateLibraryDetector.describe(duplicateLibraries));
        }

        mResourceMonitor = new ResourceMonitor(mUserPreferences);

        ThreadPool.logSettings();

        //Load properties file
        loadProperties();

        //Log current properties setting
        SystemProperties.getInstance().logCurrentSettings();


        mTunerManager = new TunerManager(mUserPreferences);
        mTunerManager.start();

        AliasModel aliasModel = new AliasModel();
        EventLogManager eventLogManager = new EventLogManager(aliasModel, mUserPreferences);
        mPlaylistManager = new PlaylistManager(mUserPreferences, mTunerManager, aliasModel, eventLogManager, mIconModel);

        boolean headless = GraphicsEnvironment.isHeadless();

        mDiagnosticMonitor = new DiagnosticMonitor(mUserPreferences, mPlaylistManager.getChannelProcessingManager(),
                mTunerManager, headless);
        mDiagnosticMonitor.start();

        if(!headless)
        {
            mWindowManager = new WindowManager(mUserPreferences, mTunerManager, mPlaylistManager);
        }

        CalibrationManager calibrationManager = CalibrationManager.getInstance(mUserPreferences);
        final boolean calibrating = !calibrationManager.isCalibrated() &&
            !mUserPreferences.getVectorCalibrationPreference().isHideCalibrationDialog();

        new ChannelSelectionManager(mPlaylistManager.getChannelModel());

        AudioPlaybackManager audioPlaybackManager = new AudioPlaybackManager(mUserPreferences);

        mAudioRecordingManager = new AudioRecordingManager(mUserPreferences);
        mAudioRecordingManager.start();

        mAudioStreamingManager = new AudioStreamingManager(mPlaylistManager.getBroadcastModel(), BroadcastFormat.MP3,
            mUserPreferences);
        mAudioStreamingManager.start();

        DuplicateCallDetector duplicateCallDetector = new DuplicateCallDetector(mUserPreferences);

        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(duplicateCallDetector);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(audioPlaybackManager);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mAudioRecordingManager);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mAudioStreamingManager);

        MapService mapService = new MapService(aliasModel, mIconModel);
        mPlaylistManager.getChannelProcessingManager().addDecodeEventListener(mapService);

        if(!headless)
        {
            mResourceMonitor.start();
            mMainWindow = FxToolkit.callAndWait(() -> new MainWindow(mUserPreferences, mPlaylistManager,
                mTunerManager, audioPlaybackManager, aliasModel, mIconModel, mapService, mResourceMonitor,
                mDiagnosticMonitor, this::processShutdown));
        }

        TunerSpectralDisplayManager tunerSpectralDisplayManager = new TunerSpectralDisplayManager(
            mMainWindow != null ? mMainWindow.getSpectralDisplay() : null, mPlaylistManager,
            mTunerManager.getDiscoveredTunerModel());
        mTunerManager.getDiscoveredTunerModel().addListener(tunerSpectralDisplayManager);
        mTunerManager.getDiscoveredTunerModel().addListener(this);

        mPlaylistManager.init();

        if(headless)
        {
            mLog.info("starting main application headless");
            autoStartChannels();
            return;
        }

        mLog.info("starting main application gui");

        Platform.runLater(() -> {
            try
            {
                mMainWindow.show();

                //The JVM only closes the splash screen when the first AWT window shows, so close it here.
                Splash.close();
                Tuner tuner = tunerSpectralDisplayManager.showFirstTuner();

                if(tuner != null)
                {
                    mMainWindow.updateTitle(tuner.getPreferredName());
                }

                if(!duplicateLibraries.isEmpty())
                {
                    mMainWindow.showDuplicateLibraries(DuplicateLibraryDetector.describe(duplicateLibraries),
                        String.valueOf(DuplicateLibraryDetector.getFolder(duplicateLibraries)));
                }
            }
            catch(Exception e)
            {
                if(!mMainWindow.getStage().isShowing())
                {
                    //The main window didn't open, so the user has no way to close the application.
                    ThreadPool.CACHED.submit(() -> startupFailed(e));
                    return;
                }

                mLog.error("Error while starting the application", e);
            }

            if(calibrating)
            {
                //Show the calibration dialog after the main window is showing
                Platform.runLater(() -> {
                    CalibrationDialog calibrationDialog = mWindowManager.getCalibrationDialog(mUserPreferences);
                    Optional<ButtonType> calibrate = calibrationDialog.showAndWait();

                    if(calibrate.isPresent() && calibrate.get().getText().equals("Calibrate"))
                    {
                        MyEventBus.getGlobalEventBus().post(new ViewUserPreferenceEditorRequest(
                            PreferenceEditorType.VECTOR_CALIBRATION));
                        MyEventBus.getGlobalEventBus().post(new CalibrateRequest());
                    }
                    else
                    {
                        autoStartChannels();
                    }
                });
            }
            else
            {
                autoStartChannels();
            }
        });
    }

    /**
     * Shows a dialog that lists the channels that have been designated for auto-start, sorted by auto-start order and
     * allows the user to start now, cancel, or allow the timer to expire and then start the channels.  The dialog will
     * only show if there are one ore more channels designated for auto-start.
     */
    private void autoStartChannels()
    {
        List<Channel> channels = mPlaylistManager.getChannelModel().getAutoStartChannels();

        if(channels.size() > 0)
        {
            if(GraphicsEnvironment.isHeadless())
            {
                for(Channel channel: channels)
                {
                    try
                    {
                        mLog.info("Auto-starting channel " + channel.getName());
                        mPlaylistManager.getChannelProcessingManager().start(channel);
                    }
                    catch(ChannelException ce)
                    {
                        mLog.error("Channel: " + channel.getName() + " auto-start failed: " + ce.getMessage());
                    }
                }
            }
            else
            {
                Platform.runLater(() -> new ChannelAutoStartWindow(mPlaylistManager.getChannelProcessingManager(),
                    channels, mUserPreferences, mMainWindow != null ? mMainWindow.getStage() : null).show());
            }
        }
    }

    /**
     * Performs shutdown operations
     */
    private void processShutdown()
    {
        mLog.info("Application shutdown started ...");
        mDiagnosticMonitor.stop();
        mLog.info("Stopping spectral display ...");

        if(mMainWindow != null)
        {
            mMainWindow.dispose();
        }

        if(mWindowManager != null)
        {
            mWindowManager.shutdown();
        }

        mLog.info("Stopping channels ...");
        mPlaylistManager.getChannelProcessingManager().shutdown();
        mAudioRecordingManager.stop();
        mResourceMonitor.stop();

        mLog.info("Stopping tuners ...");
        mTunerManager.stop();
        mLog.info("Shutdown complete.");
        mApplicationLog.stop();
    }

    /**
     * Loads the application properties file from the user's home directory,
     * creating the properties file for the first-time, if necessary
     */
    private void loadProperties()
    {
        Path propertiesPath = mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot().resolve("SDRTrunk.properties");

        if(!Files.exists(propertiesPath))
        {
            try
            {
                mLog.info("SDRTrunk - creating application properties file [" + propertiesPath.toAbsolutePath() + "]");
                Files.createFile(propertiesPath);
            }
            catch(IOException e)
            {
                mLog.error("SDRTrunk - couldn't create application properties file [" + propertiesPath.toAbsolutePath(), e);
            }
        }

        if(Files.exists(propertiesPath))
        {
            SystemProperties.getInstance().load(propertiesPath);
        }
        else
        {
            mLog.error("SDRTrunk - couldn't find or recreate the SDRTrunk application properties file");
        }
    }

    /**
     * Gets (or creates) the SDRTRunk application home directory.
     *
     * Note: the user can change this setting to allow log files and other
     * files to reside elsewhere on the file system.
     */
    private Path getHomePath()
    {
        Path homePath = FileSystems.getDefault()
            .getPath(System.getProperty("user.home"), "SDRTrunk");

        if(!Files.exists(homePath))
        {
            try
            {
                Files.createDirectory(homePath);

                mLog.info("SDRTrunk - created application home directory [" +
                    homePath.toString() + "]");
            }
            catch(Exception e)
            {
                homePath = null;

                mLog.error("SDRTrunk: exception while creating SDRTrunk home " +
                    "directory in the user's home directory", e);
            }
        }

        return homePath;
    }

    @Override
    public void receive(TunerEvent event)
    {
        if(mMainWindow != null)
        {
            mMainWindow.receive(event);
        }
    }

    /**
     * Handles an error that stops the application from starting.  Stops the tuners that already started, so that they
     * are released for the next start, then reports the error and exits.  Without this, the threads that the
     * application started before the error keep the JVM running with no main window and the splash screen showing.
     * @param t error
     */
    private void startupFailed(Throwable t)
    {
        mLog.error("sdrtrunk failed to start", t);

        if(mTunerManager != null)
        {
            try
            {
                mTunerManager.stop();
            }
            catch(Throwable t2)
            {
                mLog.error("Error stopping tuners after the startup error", t2);
            }
        }

        String logsDirectory = null;

        try
        {
            logsDirectory = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog().toString();
        }
        catch(Throwable t2)
        {
            //Not available - the dialog leaves out the log location
        }

        if(mApplicationLog != null)
        {
            mApplicationLog.stop();
        }

        exitAfterStartupError(t, logsDirectory);
    }

    /**
     * Closes the splash screen, shows the startup error to the user and exits the JVM with a non-zero status.
     * @param t error
     * @param logsDirectory where the application log is, or null if not known.
     */
    private static void exitAfterStartupError(Throwable t, String logsDirectory)
    {
        //The application log may not be running yet, so also write the error to the console.
        System.err.println("sdrtrunk failed to start");
        t.printStackTrace();

        Splash.close();

        if(FxToolkit.isStarted())
        {
            String message = "sdrtrunk couldn't start because of this error:\n\n" + t +
                (logsDirectory != null ? "\n\nThe application log has the details:\n" + logsDirectory : "") +
                "\n\nSee Help / Support on the sdrtrunk wiki for help.";

            CountDownLatch latch = new CountDownLatch(1);

            try
            {
                Platform.runLater(() -> {
                    try
                    {
                        Alert alert = new Alert(Alert.AlertType.ERROR, message);
                        alert.setTitle("sdrtrunk Startup Error");
                        alert.setHeaderText("sdrtrunk couldn't start");
                        ApplicationIcons.apply((javafx.stage.Stage)alert.getDialogPane().getScene().getWindow());
                        alert.showAndWait();
                    }
                    finally
                    {
                        latch.countDown();
                    }
                });
                latch.await();
            }
            catch(Throwable t2)
            {
                t2.printStackTrace();
            }
        }

        System.exit(1);
    }

    /**
     * Launch the application.
     */
    public static void main(String[] args)
    {
        String version = SDRTrunk.class.getPackage().getImplementationVersion();
        Splash.showVersion(version != null ? "v" + version : null);

        if(!GraphicsEnvironment.isHeadless())
        {
            try
            {
                FxToolkit.ensureStarted();
            }
            catch(Throwable t)
            {
                exitAfterStartupError(t, null);
            }
        }

        try
        {
            new SDRTrunk();
        }
        catch(Throwable t)
        {
            //An error creating the SDRTrunk fields, before the constructor's own startup error handling runs.
            exitAfterStartupError(t, null);
        }
    }
}
