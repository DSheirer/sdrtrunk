/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.swing.JideSplitPane;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.audio.DuplicateCallDetector;
import io.github.dsheirer.audio.broadcast.AudioStreamingManager;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastStatusPanel;
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.controller.ControllerPanel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelAutoStartFrame;
import io.github.dsheirer.controller.channel.ChannelException;
import io.github.dsheirer.controller.channel.ChannelSelectionManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.gui.preference.CalibrateRequest;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.gui.preference.calibration.CalibrationDialog;
import io.github.dsheirer.gui.preference.colortheme.ColorThemeManager;
import io.github.dsheirer.gui.viewer.ViewRecordingViewerRequest;
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
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayLibraryHelper;
import io.github.dsheirer.source.tuner.ui.TunerSpectralDisplayManager;
import io.github.dsheirer.spectrum.DisableSpectrumWaterfallMenuItem;
import io.github.dsheirer.spectrum.ShowTunerMenuItem;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.control.ButtonType;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class SDRTrunk implements Listener<TunerEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(SDRTrunk.class);
    private Preferences mPreferences = Preferences.userNodeForPackage(SDRTrunk.class);

    private static final String PREFERENCE_BROADCAST_STATUS_VISIBLE = "sdrtrunk.broadcast.status.visible";
    private static final String PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE = "sdrtrunk.now.playing.details.visible";
    private static final String PREFERENCE_RESOURCE_STATUS_VISIBLE = "sdrtrunk.resource.status.visible";
    private static final String BASE_WINDOW_NAME = "sdrtrunk.main.window";
    private static final String CONTROLLER_PANEL_IDENTIFIER = BASE_WINDOW_NAME + ".control.panel";
    private static final String SPECTRAL_PANEL_IDENTIFIER = BASE_WINDOW_NAME + ".spectral.panel";
    private static final String WINDOW_FRAME_IDENTIFIER = BASE_WINDOW_NAME + ".frame";

    private boolean mBroadcastStatusVisible;
    private boolean mResourceStatusVisible;
    private boolean mNowPlayingDetailsVisible;
    private AudioRecordingManager mAudioRecordingManager;
    private AudioStreamingManager mAudioStreamingManager;
    private BroadcastStatusPanel mBroadcastStatusPanel;
    private ControllerPanel mControllerPanel;
    private DiagnosticMonitor mDiagnosticMonitor;
    private IconModel mIconModel = new IconModel();
    private PlaylistManager mPlaylistManager;
    private SettingsManager mSettingsManager;
    private SpectralDisplayPanel mSpectralPanel;
    private JFrame mMainGui;
    private JideSplitPane mSplitPane;
    private JavaFxWindowManager mJavaFxWindowManager;
    private UserPreferences mUserPreferences = new UserPreferences();
    private TunerManager mTunerManager;
    private ApplicationLog mApplicationLog;
    private ResourceMonitor mResourceMonitor;
    private JFXPanel mResourceStatusPanel;

    private String mTitle;

    public SDRTrunk()
    {
        if(!GraphicsEnvironment.isHeadless())
        {
            mMainGui = new JFrame();
        }

        mApplicationLog = new ApplicationLog(mUserPreferences);
        mApplicationLog.start();

        //Note: invoke this early in the application lifecycle, before the TunerManager causes the sdrplay classes
        //to be loaded since the jextract auto-generated code attempts to load the library by name and that can fail
        //when the library was not installed into a normal/default location, particularly on windows OS systems.
        if(SDRPlayLibraryHelper.LOADED)
        {
            mLog.info("SDRPlay API native library preemptively loaded");
        }

        mResourceMonitor = new ResourceMonitor(mUserPreferences);

        String operatingSystem = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);

        if(operatingSystem.contains("mac") || operatingSystem.contains("nux"))
        {
            try
            {
                UIManager.setLookAndFeel(MetalLookAndFeel.class.getName());
                LookAndFeelFactory.installJideExtension();
            }
            catch(Exception e)
            {
                mLog.error("Error trying to set Metal look and feel for OS [" + operatingSystem + "]");
            }
        }

        ThreadPool.logSettings();

        //Load properties file
        loadProperties();

        //Log current properties setting
        SystemProperties.getInstance().logCurrentSettings();

        //Register FontAwesome so we can use the fonts in Swing windows
        IconFontSwing.register(FontAwesome.getIconFont());

        //Apply color theme (dark mode if enabled) BEFORE creating GUI components
        ColorThemeManager.applySwingTheme(mUserPreferences);

        mTunerManager = new TunerManager(mUserPreferences);
        mTunerManager.start();

        mSettingsManager = new SettingsManager();

        AliasModel aliasModel = new AliasModel();
        EventLogManager eventLogManager = new EventLogManager(aliasModel, mUserPreferences);
        mPlaylistManager = new PlaylistManager(mUserPreferences, mTunerManager, aliasModel, eventLogManager, mIconModel);

        boolean headless = GraphicsEnvironment.isHeadless();

        mDiagnosticMonitor = new DiagnosticMonitor(mUserPreferences, mPlaylistManager.getChannelProcessingManager(),
                mTunerManager, headless);
        mDiagnosticMonitor.start();

        if(!headless)
        {
            mJavaFxWindowManager = new JavaFxWindowManager(mUserPreferences, mTunerManager, mPlaylistManager);
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

        mNowPlayingDetailsVisible = mPreferences.getBoolean(PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE, true);

        if(!GraphicsEnvironment.isHeadless())
        {
            mControllerPanel = new ControllerPanel(mPlaylistManager, audioPlaybackManager, mIconModel, mapService,
                    mSettingsManager, mTunerManager, mUserPreferences, mNowPlayingDetailsVisible);
        }

        mSpectralPanel = new SpectralDisplayPanel(mPlaylistManager, mSettingsManager, mTunerManager.getDiscoveredTunerModel());

        TunerSpectralDisplayManager tunerSpectralDisplayManager = new TunerSpectralDisplayManager(mSpectralPanel,
            mPlaylistManager, mSettingsManager, mTunerManager.getDiscoveredTunerModel());
        mTunerManager.getDiscoveredTunerModel().addListener(tunerSpectralDisplayManager);
        mTunerManager.getDiscoveredTunerModel().addListener(this);

        mPlaylistManager.init();

        if(GraphicsEnvironment.isHeadless())
        {
            mLog.info("starting main application headless");
        }
        else
        {
            mLog.info("starting main application gui");

            //Initialize the GUI
            initGUI();
        }

        //Start the gui
        EventQueue.invokeLater(() -> {
            try
            {
                if(!GraphicsEnvironment.isHeadless())
                {
                    mMainGui.setVisible(true);
                    Tuner tuner = tunerSpectralDisplayManager.showFirstTuner();

                    if(tuner != null)
                    {
                        updateTitle(tuner.getPreferredName());
                    }
                }

                if(calibrating && !GraphicsEnvironment.isHeadless())
                {
                    Platform.runLater(() ->
                    {
                        CalibrationDialog calibrationDialog = mJavaFxWindowManager.getCalibrationDialog(mUserPreferences);
                        Optional<ButtonType> calibrate = calibrationDialog.showAndWait();
                        if(calibrate.isPresent() && calibrate.get().getText().equals("Calibrate"))
                        {
                            //Request focus and execute calibration
                            MyEventBus.getGlobalEventBus().post(new ViewUserPreferenceEditorRequest(PreferenceEditorType.VECTOR_CALIBRATION));
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
            }
            catch(Exception e)
            {
                e.printStackTrace();
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
                new ChannelAutoStartFrame(mPlaylistManager.getChannelProcessingManager(), channels, mUserPreferences);
            }
        }
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initGUI()
    {
        mMainGui.setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]0[shrink 0]"));

        /**
         * Setup main JFrame window
         */
        mTitle = SystemProperties.getInstance().getApplicationName();
        mMainGui.setTitle(mTitle);

        Point location = mUserPreferences.getSwingPreference().getLocation(WINDOW_FRAME_IDENTIFIER);
        if(location != null)
        {
            mMainGui.setLocation(location);
        }
        else
        {
            mMainGui.setLocationRelativeTo(null);
        }
        mMainGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mMainGui.addWindowListener(new ShutdownMonitor());

        Dimension dimension = mUserPreferences.getSwingPreference().getDimension(WINDOW_FRAME_IDENTIFIER);

        mSpectralPanel.setPreferredSize(new Dimension(1280, 300));
        mControllerPanel.setPreferredSize(new Dimension(1280, 500));

        if(dimension != null)
        {
            Dimension spectral = mUserPreferences.getSwingPreference().getDimension(SPECTRAL_PANEL_IDENTIFIER);
            if(spectral != null)
            {
                Dimension pref = mSpectralPanel.getPreferredSize();
                mSpectralPanel.setPreferredSize(new Dimension(pref.width, spectral.height));
                // mSpectralPanel.setSize(spectral);
            }

            Dimension controller = mUserPreferences.getSwingPreference().getDimension(CONTROLLER_PANEL_IDENTIFIER);
            if(controller != null)
            {
                Dimension pref = mControllerPanel.getPreferredSize();
                mControllerPanel.setPreferredSize(new Dimension(pref.width, controller.height));
                // mControllerPanel.setSize(controller);
            }

            mMainGui.setSize(dimension);

            if(mUserPreferences.getSwingPreference().getMaximized(WINDOW_FRAME_IDENTIFIER, false))
            {
                mMainGui.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        }
        else
        {
            mMainGui.setSize(new Dimension(1280, 800));
        }
        mSplitPane = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        mSplitPane.setDividerSize(5);
        mSplitPane.add(mSpectralPanel);
        mSplitPane.add(mControllerPanel);

        mBroadcastStatusVisible = mPreferences.getBoolean(PREFERENCE_BROADCAST_STATUS_VISIBLE, false);

        //Show broadcast status panel when user requests - disabled by default
        if(mBroadcastStatusVisible)
        {
            mSplitPane.add(getBroadcastStatusPanel());
        }

        mMainGui.add(mSplitPane, "cell 0 0,span,grow");

        mResourceMonitor.start();
        mResourceStatusVisible = mPreferences.getBoolean(PREFERENCE_RESOURCE_STATUS_VISIBLE, true);
        if(mResourceStatusVisible)
        {
            mMainGui.add(getResourceStatusPanel(), "span,growx");
        }

        //Update GUI components to apply theme after all components are created
        if(mUserPreferences.getColorThemePreference().isDarkModeEnabled())
        {
            ColorThemeManager.updateComponentTreeUI(mMainGui);
        }

        /**
         * Menu items
         */
        JMenuBar menuBar = new JMenuBar();
        menuBar.setOpaque(true);
        ColorThemeManager.applyDarkThemeToComponent(menuBar, mUserPreferences);
        mMainGui.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        ColorThemeManager.applyDarkThemeToComponent(fileMenu, mUserPreferences);
        menuBar.add(fileMenu);

        JMenuItem processingStatusReportMenuItem = new JMenuItem("Processing Diagnostic Report");
        processingStatusReportMenuItem.addActionListener(e -> {
            try
            {
                Path path = mDiagnosticMonitor.generateProcessingDiagnosticReport("User initiated diagnostic report");

                JOptionPane.showMessageDialog(mMainGui, "Report created: " +
                        path.toString(), "Processing Status Report Created", JOptionPane.INFORMATION_MESSAGE);
            }
            catch(IOException ioe)
            {
                mLog.error("Error creating processing status report file", ioe);
                JOptionPane.showMessageDialog(mMainGui, "Unable to create report file.  Please " +
                        "see application log for details.", "Processing Status Report Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        JMenuItem threadDumpReportMenuItem = new JMenuItem("Thread Dump Report");
        threadDumpReportMenuItem.addActionListener(e -> {
            try
            {
                Path path = mDiagnosticMonitor.generateThreadDumpReport();

                JOptionPane.showMessageDialog(mMainGui, "Report created: " +
                        path.toString(), "Thread Dump Report Created", JOptionPane.INFORMATION_MESSAGE);
            }
            catch(IOException ioe)
            {
                mLog.error("Error creating thread dump report file", ioe);
                JOptionPane.showMessageDialog(mMainGui, "Unable to create report file.  Please " +
                        "see application log for details.", "Thread Dump Report Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        JMenu diagnosticMenu = new JMenu(("Reports"));
        diagnosticMenu.add(processingStatusReportMenuItem);
        diagnosticMenu.add(threadDumpReportMenuItem);
        fileMenu.add(diagnosticMenu);
        fileMenu.add(new JSeparator(JSeparator.HORIZONTAL));

        JMenuItem exitMenu = new JMenuItem("Exit");
        exitMenu.addActionListener(event -> {
                processShutdown();
                System.exit(0);
            }
        );

        fileMenu.add(exitMenu);

        JMenu viewMenu = new JMenu("View");
        ColorThemeManager.applyDarkThemeToComponent(viewMenu, mUserPreferences);

        JMenuItem viewPlaylistItem = new JMenuItem("Playlist Editor");
        viewPlaylistItem.setIcon(IconFontSwing.buildIcon(FontAwesome.PLAY_CIRCLE_O, 12));
        viewPlaylistItem.addActionListener(e -> MyEventBus.getGlobalEventBus().post(new ViewPlaylistRequest()));
        viewMenu.add(viewPlaylistItem);

        viewMenu.add(new JSeparator());

        JMenuItem viewApplicationLogsMenu = new JMenuItem("Application Log Files");
        viewApplicationLogsMenu.setIcon(IconFontSwing.buildIcon(FontAwesome.FOLDER_OPEN_O, 12));
        viewApplicationLogsMenu.addActionListener(arg0 -> {
            File logsDirectory = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog().toFile();
            try
            {
                Desktop.getDesktop().open(logsDirectory);
            }
            catch(Exception e)
            {
                mLog.error("Couldn't open file explorer");

                JOptionPane.showMessageDialog(mMainGui,
                        "Can't launch file explorer - files are located at: " + logsDirectory,
                        "Can't launch file explorer",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        viewMenu.add(viewApplicationLogsMenu);

        JMenuItem viewRecordingsMenuItem = new JMenuItem("Audio Recordings");
        viewRecordingsMenuItem.setIcon(IconFontSwing.buildIcon(FontAwesome.FOLDER_OPEN_O, 12));
        viewRecordingsMenuItem.addActionListener(arg0 -> {
            File recordingsDirectory = mUserPreferences.getDirectoryPreference().getDirectoryRecording().toFile();

            try
            {
                Desktop.getDesktop().open(recordingsDirectory);
            }
            catch(Exception e)
            {
                mLog.error("Couldn't open file explorer");

                JOptionPane.showMessageDialog(mMainGui,
                        "Can't launch file explorer - files are located at: " +
                                recordingsDirectory,
                        "Can't launch file explorer",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        viewMenu.add(viewRecordingsMenuItem);

        JMenuItem viewEventLogsMenu = new JMenuItem("Channel Event Log Files");
        viewEventLogsMenu.setIcon(IconFontSwing.buildIcon(FontAwesome.FOLDER_OPEN_O, 12));
        viewEventLogsMenu.addActionListener(arg0 -> {
            File eventLogsDirectory = mUserPreferences.getDirectoryPreference().getDirectoryEventLog().toFile();
            try
            {
                Desktop.getDesktop().open(eventLogsDirectory);
            }
            catch(Exception e)
            {
                mLog.error("Couldn't open file explorer");

                JOptionPane.showMessageDialog(mMainGui,
                        "Can't launch file explorer - files are located at: " + eventLogsDirectory,
                        "Can't launch file explorer",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        viewMenu.add(viewEventLogsMenu);

        JMenuItem iconManagerMenu = new JMenuItem("Icon Manager");
        iconManagerMenu.setIcon(IconFontSwing.buildIcon(FontAwesome.PICTURE_O, 12));
        iconManagerMenu.addActionListener(arg0 -> MyEventBus.getGlobalEventBus().post(new ViewIconManagerRequest()));
        viewMenu.add(iconManagerMenu);

        JMenuItem recordingViewerMenu = new JMenuItem("Message Recording Viewer (.bits)");
        recordingViewerMenu.setIcon(IconFontSwing.buildIcon(FontAwesome.BRAILLE, 12));
        recordingViewerMenu.addActionListener(e -> MyEventBus.getGlobalEventBus().post(new ViewRecordingViewerRequest()));
        viewMenu.add(recordingViewerMenu);

        JMenuItem viewScreenCapturesMenu = new JMenuItem("Screen Captures");
        viewScreenCapturesMenu.setIcon(IconFontSwing.buildIcon(FontAwesome.FOLDER_OPEN_O, 12));
        viewScreenCapturesMenu.addActionListener(arg0 -> {
            File screenCapturesDirectory = mUserPreferences.getDirectoryPreference().getDirectoryScreenCapture().toFile();
            try
            {
                Desktop.getDesktop().open(screenCapturesDirectory);
            }
            catch(Exception e)
            {
                mLog.error("Couldn't open file explorer");

                JOptionPane.showMessageDialog(mMainGui,
                        "Can't launch file explorer - files are located at: " + screenCapturesDirectory,
                        "Can't launch file explorer",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
        viewMenu.add(viewScreenCapturesMenu);

        JMenuItem preferencesItem = new JMenuItem("User Preferences");
        preferencesItem.setIcon(IconFontSwing.buildIcon(FontAwesome.COG, 12));
        preferencesItem.addActionListener(e -> MyEventBus.getGlobalEventBus().post(new ViewUserPreferenceEditorRequest()));
        viewMenu.add(preferencesItem);

        viewMenu.add(new JSeparator());
        viewMenu.add(new TunersMenu());
        viewMenu.add(new JSeparator());
        viewMenu.add(new DisableSpectrumWaterfallMenuItem(mSpectralPanel));
        viewMenu.add(new NowPlayingChannelDetailsVisibleMenuItem());
        viewMenu.add(new BroadcastStatusVisibleMenuItem());
        viewMenu.add(new ResourceStatusVisibleMenuItem());

        menuBar.add(viewMenu);

        JMenuItem screenCaptureItem = new JMenuItem("Screen Capture");
        screenCaptureItem.setOpaque(true);
        screenCaptureItem.setIcon(IconFontSwing.buildIcon(FontAwesome.CAMERA, 12));
        screenCaptureItem.setMnemonic(KeyEvent.VK_C);
        screenCaptureItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
        screenCaptureItem.setMaximumSize(screenCaptureItem.getPreferredSize());
        screenCaptureItem.addActionListener(arg0 -> {
            try
            {
                Robot robot = new Robot();

                final BufferedImage image = robot.createScreenCapture(mMainGui.getBounds());

                String filename = TimeStamp.getTimeStamp("_") + "_screen_capture.png";

                final Path captureFile = mUserPreferences.getDirectoryPreference().getDirectoryScreenCapture().resolve(filename);

                ThreadPool.CACHED.submit(() -> {
                    try
                    {
                        ImageIO.write(image, "png", captureFile.toFile());
                    }
                    catch(IOException e)
                    {
                        mLog.error("Couldn't write screen capture to file [" + captureFile + "]", e);
                    }
                });
            }
            catch(AWTException e)
            {
                mLog.error("Exception while taking screen capture", e);
            }
        });

        menuBar.add(screenCaptureItem);
    }

    /**
     * Performs shutdown operations
     */
    private void processShutdown()
    {
        mLog.info("Application shutdown started ...");
        mDiagnosticMonitor.stop();
        mUserPreferences.getSwingPreference().setLocation(WINDOW_FRAME_IDENTIFIER, mMainGui.getLocation());
        mUserPreferences.getSwingPreference().setDimension(WINDOW_FRAME_IDENTIFIER, mMainGui.getSize());
        mUserPreferences.getSwingPreference().setMaximized(WINDOW_FRAME_IDENTIFIER,
            (mMainGui.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH);
        mUserPreferences.getSwingPreference().setDimension(SPECTRAL_PANEL_IDENTIFIER, mSpectralPanel.getSize());
        mUserPreferences.getSwingPreference().setDimension(CONTROLLER_PANEL_IDENTIFIER, mControllerPanel.getSize());
        mJavaFxWindowManager.shutdown();
        mLog.info("Stopping channels ...");
        mPlaylistManager.getChannelProcessingManager().shutdown();
        mAudioRecordingManager.stop();
        mResourceMonitor.stop();

        mLog.info("Stopping spectral display ...");
        mSpectralPanel.clearTuner();
        mLog.info("Stopping tuners ...");
        mTunerManager.stop();
        mLog.info("Shutdown complete.");
        mApplicationLog.stop();
    }

    /**
     * Lazy constructor for broadcast status panel
     */
    private BroadcastStatusPanel getBroadcastStatusPanel()
    {
        if(mBroadcastStatusPanel == null)
        {
            mBroadcastStatusPanel = new BroadcastStatusPanel(mPlaylistManager.getBroadcastModel(), mUserPreferences,
                "application.broadcast.status.panel");
            mBroadcastStatusPanel.setPreferredSize(new Dimension(880, 70));
            mBroadcastStatusPanel.getTable().setEnabled(false);
        }

        return mBroadcastStatusPanel;
    }

    /**
     * Toggles visibility of the broadcast channels status panel at the bottom of the controller panel
     */
    private void toggleBroadcastStatusPanelVisibility()
    {
        mBroadcastStatusVisible = !mBroadcastStatusVisible;

        EventQueue.invokeLater(() -> {
            if(mBroadcastStatusVisible)
            {
                mSplitPane.add(getBroadcastStatusPanel());
            }
            else
            {
                mSplitPane.remove(getBroadcastStatusPanel());
            }

            mMainGui.revalidate();
        });

        mPreferences.putBoolean(PREFERENCE_BROADCAST_STATUS_VISIBLE, mBroadcastStatusVisible);
    }

    /**
     * Lazy constructor for resource status panel
     */
    private JFXPanel getResourceStatusPanel()
    {

        if(mResourceStatusPanel == null)
        {
            mResourceStatusPanel = mJavaFxWindowManager.getStatusPanel(mResourceMonitor);
        }

        return mResourceStatusPanel;
    }

    /**
     * Toggles visibility of the resource status panel at the bottom of the main UI window
     */
    private void toggleResourceStatusPanelVisibility()
    {
        mResourceStatusVisible = !mResourceStatusVisible;

        EventQueue.invokeLater(() -> {
            if(mResourceStatusVisible)
            {
                mMainGui.add(getResourceStatusPanel(), "span,growx");
            }
            else
            {
                mMainGui.remove(getResourceStatusPanel());
            }

            mMainGui.revalidate();
        });

        mPreferences.putBoolean(PREFERENCE_RESOURCE_STATUS_VISIBLE, mResourceStatusVisible);
    }

    /**
     * Toggles visibility of the Now Playing channel details panel
     */
    private void toggleNowPlayingDetailsPanelVisibility()
    {
        mNowPlayingDetailsVisible = !mNowPlayingDetailsVisible;
        mControllerPanel.getNowPlayingPanel().setDetailTabsVisible(mNowPlayingDetailsVisible);
        mPreferences.putBoolean(PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE, mNowPlayingDetailsVisible);
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
        switch(event.getEvent())
        {
            case REQUEST_MAIN_SPECTRAL_DISPLAY:
                updateTitle(event.getTuner().getPreferredName());
                break;
            case REQUEST_CLEAR_MAIN_SPECTRAL_DISPLAY:
                updateTitle(null);
                break;
            case NOTIFICATION_SHUTTING_DOWN:
                Tuner currentTuner = mSpectralPanel.getTuner();

                if(event.hasTuner() && event.getTuner().equals(currentTuner) || currentTuner == null)
                {
                    updateTitle(null);
                }
                break;
        }
    }

    /**
     * Updates the title bar with the tuner name
     * @param tunerName optional
     */
    private void updateTitle(String tunerName)
    {
        if(tunerName != null)
        {
            mMainGui.setTitle(mTitle + " - " + tunerName);
        }
        else
        {
            mMainGui.setTitle(mTitle);
        }
    }

    public class ShutdownMonitor extends WindowAdapter
    {
        @Override
        public void windowClosing(WindowEvent e)
        {
            processShutdown();
        }
    }

    /**
     * Broadcast status panel visible toggle menu item
     */
    public class BroadcastStatusVisibleMenuItem extends JCheckBoxMenuItem
    {
        public BroadcastStatusVisibleMenuItem()
        {
            super("Show Streaming Status");
            setSelected(mBroadcastStatusVisible);
            addActionListener(e -> {
                toggleBroadcastStatusPanelVisibility();
                setSelected(mBroadcastStatusVisible);
            });
        }
    }

    /**
     * Resource status panel visible toggle menu item
     */
    public class ResourceStatusVisibleMenuItem extends JCheckBoxMenuItem
    {
        public ResourceStatusVisibleMenuItem()
        {
            super("Show Resource Status");
            setSelected(mResourceStatusVisible);
            addActionListener(e -> {
                toggleResourceStatusPanelVisibility();
                setSelected(mResourceStatusVisible);
            });
        }
    }

    /**
     * Now Playing channel details visible toggle menu item
     */
    public class NowPlayingChannelDetailsVisibleMenuItem extends JCheckBoxMenuItem
    {
        public NowPlayingChannelDetailsVisibleMenuItem()
        {
            super("Show Now Playing Channel Details");
            setSelected(mNowPlayingDetailsVisible);
            addActionListener(e -> {
                toggleNowPlayingDetailsPanelVisibility();
                setSelected(mNowPlayingDetailsVisible);
            });
        }
    }

    public class TunersMenu extends JMenu
    {
        public TunersMenu()
        {
            super("Tuners");

            addMenuListener(new MenuListener()
            {
                @Override
                public void menuSelected(MenuEvent e)
                {
                    removeAll();

                    for(DiscoveredTuner discoveredTuner: mTunerManager.getAvailableTuners())
                    {
                        add(new ShowTunerMenuItem(mTunerManager.getDiscoveredTunerModel(), discoveredTuner.getTuner()));
                    }
                }

                @Override
                public void menuDeselected(MenuEvent e) { }
                @Override
                public void menuCanceled(MenuEvent e) { }
            });
        }

    }

    /**
     * Launch the application.
     */
    public static void main(String[] args)
    {
        new SDRTrunk();
    }
}
