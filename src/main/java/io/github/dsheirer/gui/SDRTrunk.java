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
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.controller.channel.ChannelSelectionManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.log.ApplicationLog;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.module.log.EventLogManager;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.record.AudioRecordingManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.SourceManager;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.TunerModel;
import io.github.dsheirer.source.tuner.TunerSpectralDisplayManager;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.spectrum.ClearTunerMenuItem;
import io.github.dsheirer.spectrum.ShowTunerMenuItem;
import io.github.dsheirer.spectrum.SpectralDisplayPanel;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class SDRTrunk implements Listener<TunerEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(SDRTrunk.class);

    private static final String PROPERTY_BROADCAST_STATUS_VISIBLE = "main.broadcast.status.visible";
    private static final String BASE_WINDOW_NAME = "sdrtrunk.main.window";
    private static final String CONTROLLER_PANEL_IDENTIFIER = BASE_WINDOW_NAME + ".control.panel";
    private static final String SPECTRAL_PANEL_IDENTIFIER = BASE_WINDOW_NAME + ".spectral.panel";
    private static final String WINDOW_FRAME_IDENTIFIER = BASE_WINDOW_NAME + ".frame";

    private boolean mBroadcastStatusVisible;
    private AudioRecordingManager mAudioRecordingManager;
    private AudioStreamingManager mAudioStreamingManager;
    private BroadcastStatusPanel mBroadcastStatusPanel;
    private ControllerPanel mControllerPanel;
    private IconModel mIconModel = new IconModel();
    private PlaylistManager mPlaylistManager;
    private SourceManager mSourceManager;
    private SettingsManager mSettingsManager;
    private SpectralDisplayPanel mSpectralPanel;
    private JFrame mMainGui;
    private JideSplitPane mSplitPane;
    private JavaFxWindowManager mJavaFxWindowManager;
    private UserPreferences mUserPreferences = new UserPreferences();
    private ApplicationLog mApplicationLog;
    public static boolean mHeadlessMode;
    public static boolean mSilentMode;

    private String mTitle;

    public SDRTrunk(String[] args)
    {
        mApplicationLog = new ApplicationLog(mUserPreferences);
        mApplicationLog.start();

        //Handle command-line options
        Options options = new Options();
        options.addOption(null, "headless", false, "Disables the application GUI");
        options.addOption(null, "silent", false, "Disables audio output");
        options.addOption("h", "help", false, "Displays usage information");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cli = parser.parse(options,  args);
			if (cli.hasOption("help")) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("SDRTrunk", options);
				System.exit(0);
			}
			mHeadlessMode = cli.hasOption("headless");
			mSilentMode = cli.hasOption("silent");
		} catch (ParseException e) {
			mLog.error("Error trying to parse command line options");
			e.printStackTrace();
		}

        String operatingSystem = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);

        if (!mHeadlessMode) {
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
        }

        //Setup the application home directory
        Path home = getHomePath();

        ThreadPool.logSettings();

        mLog.info("Home path: " + home.toString());

        //Load properties file
        if(home != null)
        {
            loadProperties(home);
        }

        //Log current properties setting
        SystemProperties.getInstance().logCurrentSettings();

        //Register FontAwesome so we can use the fonts in Swing windows
        IconFontSwing.register(FontAwesome.getIconFont());

        TunerConfigurationModel tunerConfigurationModel = new TunerConfigurationModel();
        TunerModel tunerModel = new TunerModel(tunerConfigurationModel);

        mSettingsManager = new SettingsManager(tunerConfigurationModel);
        mSourceManager = new SourceManager(tunerModel, mSettingsManager, mUserPreferences);

        AliasModel aliasModel = new AliasModel();
        EventLogManager eventLogManager = new EventLogManager(aliasModel, mUserPreferences);
        mPlaylistManager = new PlaylistManager(mUserPreferences, mSourceManager, aliasModel, eventLogManager, mIconModel);
        if (!mHeadlessMode) {
            mJavaFxWindowManager = new JavaFxWindowManager(mUserPreferences, mPlaylistManager);
        }
        new ChannelSelectionManager(mPlaylistManager.getChannelModel());

        AudioPlaybackManager audioPlaybackManager = null;
        if (!mSilentMode)
        {
            audioPlaybackManager = new AudioPlaybackManager(mUserPreferences);
        }

        mAudioRecordingManager = new AudioRecordingManager(mUserPreferences);
        mAudioRecordingManager.start();

        mAudioStreamingManager = new AudioStreamingManager(mPlaylistManager.getBroadcastModel(), BroadcastFormat.MP3,
            mUserPreferences);
        mAudioStreamingManager.start();

        DuplicateCallDetector duplicateCallDetector = new DuplicateCallDetector(mUserPreferences);

        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(duplicateCallDetector);
        if (!mSilentMode)
        {
            mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(audioPlaybackManager);
        }
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mAudioRecordingManager);
        mPlaylistManager.getChannelProcessingManager().addAudioSegmentListener(mAudioStreamingManager);

        if (!mHeadlessMode) {
            mMainGui = new JFrame();
            MapService mapService = new MapService(mIconModel);
            mPlaylistManager.getChannelProcessingManager().addDecodeEventListener(mapService);

            mControllerPanel = new ControllerPanel(mPlaylistManager, audioPlaybackManager, mIconModel, mapService,
                mSettingsManager, mSourceManager, mUserPreferences);

            mSpectralPanel = new SpectralDisplayPanel(mPlaylistManager, mSettingsManager, tunerModel);

            TunerSpectralDisplayManager tunerSpectralDisplayManager = new TunerSpectralDisplayManager(mSpectralPanel,
                mPlaylistManager, mSettingsManager, tunerModel);
            tunerModel.addListener(tunerSpectralDisplayManager);
            tunerModel.addListener(this);
        }

        mPlaylistManager.init();

        if (mHeadlessMode) {
            autoStartChannels();
        }
        else
        {
            mLog.info("starting main application gui");
            //Initialize the GUI
            initGUI();

	        tunerModel.requestFirstTunerDisplay();

	        //Start the gui
	        EventQueue.invokeLater(() -> {
	            try
	            {
	                mMainGui.setVisible(true);
	                autoStartChannels();
	            }
	            catch(Exception e)
	            {
	                e.printStackTrace();
	            }
	        });
        }
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
            if (mHeadlessMode)
            {
                for (Channel channel : channels)
                {
                    mPlaylistManager.getChannelProcessingManager().receive(new ChannelEvent(channel, ChannelEvent.Event.REQUEST_ENABLE));
                }
            }
            else
            {
                ChannelAutoStartFrame autoStartFrame = new ChannelAutoStartFrame(mPlaylistManager.getChannelProcessingManager(),
                    channels);
            }
        }
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initGUI()
    {
        mMainGui.setLayout(new MigLayout("insets 0 0 0 0 ", "[grow,fill]", "[grow,fill]"));

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
                mSpectralPanel.setSize(spectral);
            }

            Dimension controller = mUserPreferences.getSwingPreference().getDimension(CONTROLLER_PANEL_IDENTIFIER);
            if(controller != null)
            {
                mControllerPanel.setSize(controller);
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

        mBroadcastStatusVisible = SystemProperties.getInstance().get(PROPERTY_BROADCAST_STATUS_VISIBLE, false);

        //Show broadcast status panel when user requests - disabled by default
        if(mBroadcastStatusVisible)
        {
            mSplitPane.add(getBroadcastStatusPanel());
        }

        mMainGui.add(mSplitPane, "cell 0 0,span,grow");

        /**
         * Menu items
         */
        JMenuBar menuBar = new JMenuBar();
        mMainGui.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);

        JMenuItem exitMenu = new JMenuItem("Exit");
        exitMenu.addActionListener(
            new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    processShutdown();
                    System.exit(0);
                }
            }
        );

        fileMenu.add(exitMenu);

        JMenu viewMenu = new JMenu("View");

        JMenuItem viewPlaylistItem = new JMenuItem("Playlist Editor");
        viewPlaylistItem.addActionListener(e -> MyEventBus.getEventBus().post(new ViewPlaylistRequest()));
        viewMenu.add(viewPlaylistItem);

        JMenuItem preferencesItem = new JMenuItem("User Preferences");
        preferencesItem.addActionListener(e -> MyEventBus.getEventBus().post(new ViewUserPreferenceEditorRequest()));
        viewMenu.add(preferencesItem);

        JMenuItem settingsMenu = new JMenuItem("Icon Manager");
        settingsMenu.addActionListener(arg0 -> MyEventBus.getEventBus().post(new ViewIconManagerRequest()));
        viewMenu.add(settingsMenu);

        JMenuItem logFilesMenu = new JMenuItem("Logs & Recordings");
        logFilesMenu.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                try
                {
                    Desktop.getDesktop().open(getHomePath().toFile());
                }
                catch(Exception e)
                {
                    mLog.error("Couldn't open file explorer");

                    JOptionPane.showMessageDialog(mMainGui,
                        "Can't launch file explorer - files are located at: " +
                            getHomePath().toString(),
                        "Can't launch file explorer",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        viewMenu.add(logFilesMenu);

        viewMenu.add(new JSeparator());
        viewMenu.add(new TunersMenu());
        viewMenu.add(new JSeparator());
        viewMenu.add(new ClearTunerMenuItem(mSpectralPanel));
        viewMenu.add(new BroadcastStatusVisibleMenuItem(mControllerPanel));

        menuBar.add(viewMenu);

        JMenuItem screenCaptureItem = new JMenuItem("Screen Capture");
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
     * Performs shutdown operations
     */
    private void processShutdown()
    {
        mLog.info("Application shutdown started ...");
        if (mMainGui != null) 
        {
            mUserPreferences.getSwingPreference().setLocation(WINDOW_FRAME_IDENTIFIER, mMainGui.getLocation());
            mUserPreferences.getSwingPreference().setDimension(WINDOW_FRAME_IDENTIFIER, mMainGui.getSize());
            mUserPreferences.getSwingPreference().setMaximized(WINDOW_FRAME_IDENTIFIER,
                (mMainGui.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH);
            mUserPreferences.getSwingPreference().setDimension(SPECTRAL_PANEL_IDENTIFIER, mSpectralPanel.getSize());
            mUserPreferences.getSwingPreference().setDimension(CONTROLLER_PANEL_IDENTIFIER, mControllerPanel.getSize());
            mJavaFxWindowManager.shutdown();
        }
        mLog.info("Stopping channels ...");
        mPlaylistManager.getChannelProcessingManager().shutdown();
        mAudioRecordingManager.stop();

        if (mSpectralPanel != null) 
        {
            mLog.info("Stopping spectral display ...");
            mSpectralPanel.clearTuner();
        }
        mSourceManager.shutdown();
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

        SystemProperties.getInstance().set(PROPERTY_BROADCAST_STATUS_VISIBLE, mBroadcastStatusVisible);
    }


    /**
     * Loads the application properties file from the user's home directory,
     * creating the properties file for the first-time, if necessary
     */
    private void loadProperties(Path homePath)
    {
        Path propsPath = homePath.resolve("SDRTrunk.properties");

        if(!Files.exists(propsPath))
        {
            try
            {
                mLog.info("SDRTrunk - creating application properties file [" +
                    propsPath.toAbsolutePath() + "]");

                Files.createFile(propsPath);
            }
            catch(IOException e)
            {
                mLog.error("SDRTrunk - couldn't create application properties "
                    + "file [" + propsPath.toAbsolutePath(), e);
            }
        }

        if(Files.exists(propsPath))
        {
            SystemProperties.getInstance().load(propsPath);
        }
        else
        {
            mLog.error("SDRTrunk - couldn't find or recreate the SDRTrunk " +
                "application properties file");
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
        if(event.getEvent() == TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY)
        {
            mMainGui.setTitle(mTitle + " - " + event.getTuner().getName());
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

    public class BroadcastStatusVisibleMenuItem extends JCheckBoxMenuItem
    {
        private ControllerPanel mControllerPanel;

        public BroadcastStatusVisibleMenuItem(ControllerPanel controllerPanel)
        {
            super("Show Streaming Status");

            mControllerPanel = controllerPanel;

            setSelected(mBroadcastStatusPanel != null);

            addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    toggleBroadcastStatusPanelVisibility();
                    setSelected(mBroadcastStatusVisible);
                }
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

                    for(Tuner tuner : mSourceManager.getTunerModel().getTuners())
                    {
                        add(new ShowTunerMenuItem(mSourceManager.getTunerModel(), tuner));
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
        new SDRTrunk(args);
    }
}
