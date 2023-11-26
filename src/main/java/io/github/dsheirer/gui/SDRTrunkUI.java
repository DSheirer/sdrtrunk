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

package io.github.dsheirer.gui;

import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.swing.JideSplitPane;
import io.github.dsheirer.audio.broadcast.BroadcastStatusPanel;
import io.github.dsheirer.controller.ControllerPanel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelAutoStartFrame;
import io.github.dsheirer.controller.channel.ChannelModel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.controller.channel.ChannelSelectionManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.gui.preference.CalibrateRequest;
import io.github.dsheirer.gui.preference.PreferenceEditorType;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.gui.preference.calibration.CalibrationDialog;
import io.github.dsheirer.gui.viewer.ViewRecordingViewerRequest;
import io.github.dsheirer.monitor.ResourceMonitor;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import io.github.dsheirer.source.tuner.ui.TunerSpectralDisplayManager;
import io.github.dsheirer.spectrum.DisableSpectrumWaterfallMenuItem;
import io.github.dsheirer.spectrum.ShowTunerMenuItem;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.util.TimeStamp;
import io.github.dsheirer.vector.calibrate.CalibrationManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
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

/**
 * SDRTrunk primary user interface frame/window
 */
public class SDRTrunkUI extends JFrame implements Listener<TunerEvent>
{
    private static final Logger mLog = LoggerFactory.getLogger(SDRTrunkUI.class);
    private final Preferences mPreferences = Preferences.userNodeForPackage(SDRTrunkUI.class);

    private static final String PREFERENCE_BROADCAST_STATUS_VISIBLE = "sdrtrunk.broadcast.status.visible";
    private static final String PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE = "sdrtrunk.now.playing.details.visible";
    private static final String PREFERENCE_RESOURCE_STATUS_VISIBLE = "sdrtrunk.resource.status.visible";
    private static final String BASE_WINDOW_NAME = "sdrtrunk.main.window";
    private static final String CONTROLLER_PANEL_IDENTIFIER = BASE_WINDOW_NAME + ".control.panel";
    private static final String SPECTRAL_PANEL_IDENTIFIER = BASE_WINDOW_NAME + ".spectral.panel";
    private static final String WINDOW_FRAME_IDENTIFIER = BASE_WINDOW_NAME + ".frame";

    @Resource
    private BroadcastStatusPanel mBroadcastStatusPanel;
    @Resource
    private ChannelModel mChannelModel;
    @Resource
    private ChannelProcessingManager mChannelProcessingManager;
    @Resource
    private ControllerPanel mControllerPanel;
    @Resource
    private DiscoveredTunerModel mDiscoveredTunerModel;
    @Resource
    private JavaFxWindowManager mJavaFxWindowManager;
    @Resource
    private ResourceMonitor mResourceMonitor;
    @Resource
    private SpectralDisplayPanel mSpectralPanel;
    @Resource
    private TunerSpectralDisplayManager mTunerSpectralDisplayManager;
    @Resource
    private TunerManager mTunerManager;

    private JFXPanel mResourceStatusPanel;
    private JideSplitPane mSplitPane;
    private boolean mBroadcastStatusVisible;
    private boolean mResourceStatusVisible;
    private boolean mNowPlayingDetailsVisible;

    @Resource
    private UserPreferences mUserPreferences;

    private String mTitle;

    /**
     * Constructs an instance of the SDRTrunk user interface
     */
    public SDRTrunkUI()
    {
        //Register FontAwesome so we can use the fonts in Swing windows
        IconFontSwing.register(FontAwesome.getIconFont());
    }

    /**
     * Hook into this visible method so that we can show the auto-start channels dialog on window show.
     */
    @Override
    public void setVisible(boolean b)
    {
        super.setVisible(b);

        CalibrationManager calibrationManager = CalibrationManager.getInstance(mUserPreferences);
        final boolean calibrating = !calibrationManager.isCalibrated() &&
                !mUserPreferences.getVectorCalibrationPreference().isHideCalibrationDialog();

        if(calibrating)
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

    /**
     * Shows the auto-start channels dialog
     */
    private void autoStartChannels()
    {
        List<Channel> channels = mChannelModel.getAutoStartChannels();

        if(channels.size() > 0)
        {
            new ChannelAutoStartFrame(mChannelProcessingManager, channels, mUserPreferences);
        }
    }

    @PostConstruct
    public void postConstruct()
    {
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

        new ChannelSelectionManager();

        mNowPlayingDetailsVisible = mPreferences.getBoolean(PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE, true);
        mTunerSpectralDisplayManager.setSpectralDisplayPanel(mSpectralPanel);
        mDiscoveredTunerModel.addListener(mTunerSpectralDisplayManager);
        mDiscoveredTunerModel.addListener(this);

        initGUI();

        Tuner tuner = mTunerSpectralDisplayManager.showFirstTuner();

        if(tuner != null)
        {
            updateTitle(tuner.getPreferredName());
        }
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initGUI()
    {
        setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]0[shrink 0]"));

        /**
         * Setup main JFrame window
         */
        mTitle = SystemProperties.getInstance().getApplicationName();
        setTitle(mTitle);

        Point location = mUserPreferences.getSwingPreference().getLocation(WINDOW_FRAME_IDENTIFIER);
        if(location != null)
        {
            setLocation(location);
        }
        else
        {
            setLocationRelativeTo(null);
        }
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new ShutdownMonitor());

        Dimension dimension = mUserPreferences.getSwingPreference().getDimension(WINDOW_FRAME_IDENTIFIER);

        mSpectralPanel.setPreferredSize(new Dimension(1280, 300));
        mControllerPanel.setPreferredSize(new Dimension(1280, 500));

        if(dimension != null)
        {
            Dimension spectral = mUserPreferences.getSwingPreference().getDimension(SPECTRAL_PANEL_IDENTIFIER);
            if(spectral != null)
            {
                mSpectralPanel.setSize(spectral);
            }

            Dimension controller = mUserPreferences.getSwingPreference().getDimension(CONTROLLER_PANEL_IDENTIFIER);
            if(controller != null)
            {
                mControllerPanel.setSize(controller);
            }

            setSize(dimension);

            if(mUserPreferences.getSwingPreference().getMaximized(WINDOW_FRAME_IDENTIFIER, false))
            {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }
        }
        else
        {
            setSize(new Dimension(1280, 800));
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

        add(mSplitPane, "cell 0 0,span,grow");

        mResourceMonitor.start();
        mResourceStatusVisible = mPreferences.getBoolean(PREFERENCE_RESOURCE_STATUS_VISIBLE, true);
        if(mResourceStatusVisible)
        {
            add(getResourceStatusPanel(), "span,growx");
        }

        /**
         * Menu items
         */
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem exitMenu = new JMenuItem("Exit");
        exitMenu.addActionListener(event -> {
                    shutdown();
                    System.exit(0);
                }
        );

        fileMenu.add(exitMenu);

        JMenu viewMenu = new JMenu("View");

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

                JOptionPane.showMessageDialog(this,
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

                JOptionPane.showMessageDialog(this,
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

                JOptionPane.showMessageDialog(this, "Can't launch file explorer - files are located at: " + eventLogsDirectory,
                        "Can't launch file explorer", JOptionPane.ERROR_MESSAGE);
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

                JOptionPane.showMessageDialog(this, "Can't launch file explorer - files are located at: " + screenCapturesDirectory,
                        "Can't launch file explorer", JOptionPane.ERROR_MESSAGE);
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
        viewMenu.add(new DisableSpectrumWaterfallMenuItem(mSpectralPanel, mUserPreferences));
        viewMenu.add(new NowPlayingChannelDetailsVisibleMenuItem());
        viewMenu.add(new BroadcastStatusVisibleMenuItem());
        viewMenu.add(new ResourceStatusVisibleMenuItem());

        menuBar.add(viewMenu);

        JMenuItem screenCaptureItem = new JMenuItem("Screen Capture");
        screenCaptureItem.setIcon(IconFontSwing.buildIcon(FontAwesome.CAMERA, 12));
        screenCaptureItem.setMnemonic(KeyEvent.VK_C);
        screenCaptureItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
        screenCaptureItem.setMaximumSize(screenCaptureItem.getPreferredSize());
        screenCaptureItem.addActionListener(arg0 -> {
            try
            {
                Robot robot = new Robot();

                final BufferedImage image = robot.createScreenCapture(getBounds());

                String filename = TimeStamp.getTimeStamp("_") + "_screen_capture.png";

                final Path captureFile = mUserPreferences.getDirectoryPreference().getDirectoryScreenCapture().resolve(filename);

                EventQueue.invokeLater(() -> {
                    try
                    {
                        ImageIO.write(image, "png", captureFile.toFile());
                    }
                    catch(IOException e)
                    {
                        mLog.error("Couldn't write screen capture to file [" + captureFile.toString() + "]", e);
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
     * Updates the title bar with the tuner name
     * @param tunerName optional
     */
    private void updateTitle(String tunerName)
    {
        if(tunerName != null)
        {
            setTitle(mTitle + " - " + tunerName);
        }
        else
        {
            setTitle(mTitle);
        }
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
     * Lazy constructor for broadcast status panel
     */
    private BroadcastStatusPanel getBroadcastStatusPanel()
    {
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

            revalidate();
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
                add(getResourceStatusPanel(), "span,growx");
            }
            else
            {
                remove(getResourceStatusPanel());
            }

            revalidate();
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
     * Performs shutdown operations.  This is triggered by the shutdown monitor's windowClosing() method
     */
    private void shutdown()
    {
        mLog.info("SDRTrunk UI shutdown started ...");
        mUserPreferences.getSwingPreference().setLocation(WINDOW_FRAME_IDENTIFIER, getLocation());
        mUserPreferences.getSwingPreference().setDimension(WINDOW_FRAME_IDENTIFIER, getSize());
        mUserPreferences.getSwingPreference().setMaximized(WINDOW_FRAME_IDENTIFIER,
                (getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH);
        mUserPreferences.getSwingPreference().setDimension(SPECTRAL_PANEL_IDENTIFIER, mSpectralPanel.getSize());
        mUserPreferences.getSwingPreference().setDimension(CONTROLLER_PANEL_IDENTIFIER, mControllerPanel.getSize());
        mLog.info("Stopping JavaFX window manager ...");
        mJavaFxWindowManager.shutdown();
        mLog.info("Stopping resource monitor ...");
        mResourceMonitor.stop();
        mLog.info("Stopping spectral display ...");
        mSpectralPanel.clearTuner();
    }

    /**
     * Window adapter to receive window closing event to initiate shutdown.
     */
    public class ShutdownMonitor extends WindowAdapter
    {
        @Override
        public void windowClosing(WindowEvent e)
        {
            shutdown();
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

    /**
     * Menu for selecting tuners for display.
     */
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
                        add(new ShowTunerMenuItem(discoveredTuner.getTuner(), mDiscoveredTunerModel, mUserPreferences));
                    }
                }

                @Override
                public void menuDeselected(MenuEvent e) { }
                @Override
                public void menuCanceled(MenuEvent e) { }
            });
        }

    }
}
