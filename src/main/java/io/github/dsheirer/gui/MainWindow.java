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
import io.github.dsheirer.audio.playback.AudioPlaybackManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.audio.AudioView;
import io.github.dsheirer.gui.broadcast.BroadcastStatusView;
import io.github.dsheirer.gui.icon.ViewIconManagerRequest;
import io.github.dsheirer.gui.map.MapView;
import io.github.dsheirer.gui.nowplaying.NowPlayingView;
import io.github.dsheirer.gui.playlist.ViewPlaylistRequest;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferencePlaylistActions;
import io.github.dsheirer.gui.preference.ViewUserPreferenceEditorRequest;
import io.github.dsheirer.gui.spectrum.MainSpectralDisplay;
import io.github.dsheirer.gui.spectrum.TunerSpectralDisplay;
import io.github.dsheirer.gui.theme.ThemeManager;
import io.github.dsheirer.gui.viewer.ViewRecordingViewerRequest;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.map.MapService;
import io.github.dsheirer.monitor.DiagnosticMonitor;
import io.github.dsheirer.monitor.ResourceMonitor;
import io.github.dsheirer.monitor.StatusBox;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.TunerEvent;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconFontFX;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application window: menu bar, audio playback display, the spectrum and waterfall, the Now Playing, Map and
 * Tuners tabs, the optional streaming status and the optional resource status bar.
 *
 * The spectrum and waterfall can be hidden from the View menu, the hide button in the display settings panel or a
 * keyboard shortcut.  A hidden spectrum is disposed so that it uses no processing or memory.
 */
public class MainWindow
{
    private static final Logger mLog = LoggerFactory.getLogger(MainWindow.class);
    public static final String STAGE_MONITOR_KEY = "main.window";
    private static final String PREFERENCE_BROADCAST_STATUS_VISIBLE = "sdrtrunk.broadcast.status.visible";
    private static final String PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE = "sdrtrunk.now.playing.details.visible";
    private static final String PREFERENCE_RESOURCE_STATUS_VISIBLE = "sdrtrunk.resource.status.visible";
    private static final String PREFERENCE_SPECTRAL_DISPLAY_VISIBLE = "sdrtrunk.spectral.display.visible";
    private static final String PREFERENCE_SPECTRAL_DIVIDER = "sdrtrunk.main.window.spectral.fraction";
    private static final String PREFERENCE_BROADCAST_DIVIDER = "sdrtrunk.main.window.streaming.fraction";
    private static final double DEFAULT_SPECTRAL_DIVIDER = 0.38;
    private static final double DEFAULT_BROADCAST_FRACTION = 0.18;
    static final KeyCombination SPECTRAL_TOGGLE_SHORTCUT = new KeyCodeCombination(KeyCode.S,
        KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN);

    private final Preferences mPreferences = Preferences.userNodeForPackage(SDRTrunk.class);
    private final UserPreferences mUserPreferences;
    private final PlaylistManager mPlaylistManager;
    private final TunerManager mTunerManager;
    private final DiagnosticMonitor mDiagnosticMonitor;
    private final ResourceMonitor mResourceMonitor;
    private final Runnable mExitHandler;
    private final String mTitle;

    private final Stage mStage;
    private final BorderPane mRoot = new BorderPane();
    private final SplitPane mSplitPane = new SplitPane();
    private final VBox mCenter = new VBox();
    private final AudioView mAudioView;
    private final MainSpectralDisplay mSpectralDisplay;
    private final HBox mSpectralHiddenBar;
    private final NowPlayingView mNowPlayingView;
    private final TabPane mTabPane = new TabPane();
    private BroadcastStatusView mBroadcastStatusView;
    private StatusBox mStatusBox;
    private final CheckMenuItem mSpectralMenuItem = new CheckMenuItem("Show Spectrum & Waterfall");
    private final CheckMenuItem mBroadcastMenuItem = new CheckMenuItem("Show Streaming Status");
    private final CheckMenuItem mResourceMenuItem = new CheckMenuItem("Show Resource Status");
    private final CheckMenuItem mNowPlayingDetailsMenuItem = new CheckMenuItem("Show Now Playing Channel Details");
    private double mSpectralDivider;
    private double mBroadcastFraction;
    private boolean mUpdatingDividers;

    static
    {
        IconFontFX.register(FontAwesome.getIconFont());
    }

    /**
     * Constructs the main window.  JavaFX thread only.
     * @param userPreferences for the views
     * @param playlistManager for the channels, aliases and streams
     * @param tunerManager for the tuners
     * @param audioPlaybackManager for the audio display
     * @param aliasModel for alias lookup
     * @param iconModel for alias icons
     * @param mapService for the map
     * @param resourceMonitor for the status bar
     * @param diagnosticMonitor for the reports
     * @param exitHandler that shuts down the application, invoked on a background thread
     */
    public MainWindow(UserPreferences userPreferences, PlaylistManager playlistManager, TunerManager tunerManager,
                      AudioPlaybackManager audioPlaybackManager, AliasModel aliasModel, IconModel iconModel,
                      MapService mapService, ResourceMonitor resourceMonitor, DiagnosticMonitor diagnosticMonitor,
                      Runnable exitHandler)
    {
        mUserPreferences = userPreferences;
        mPlaylistManager = playlistManager;
        mTunerManager = tunerManager;
        mResourceMonitor = resourceMonitor;
        mDiagnosticMonitor = diagnosticMonitor;
        mExitHandler = exitHandler;
        mTitle = SystemProperties.getInstance().getApplicationName();
        mSpectralDivider = clamp(mPreferences.getDouble(PREFERENCE_SPECTRAL_DIVIDER, DEFAULT_SPECTRAL_DIVIDER));
        mBroadcastFraction = clamp(mPreferences.getDouble(PREFERENCE_BROADCAST_DIVIDER, DEFAULT_BROADCAST_FRACTION));

        mAudioView = new AudioView(audioPlaybackManager, aliasModel, iconModel, userPreferences);

        boolean spectralVisible = mPreferences.getBoolean(PREFERENCE_SPECTRAL_DISPLAY_VISIBLE, true);
        mSpectralDisplay = new MainSpectralDisplay(playlistManager, tunerManager.getDiscoveredTunerModel(),
            spectralVisible);
        mSpectralDisplay.setHideRequestHandler(() -> setSpectralDisplayVisible(false));
        mSpectralHiddenBar = createSpectralHiddenBar();

        boolean detailsVisible = mPreferences.getBoolean(PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE, true);
        mNowPlayingView = new NowPlayingView(playlistManager, iconModel, userPreferences, detailsVisible);
        createTabs(mapService, iconModel);

        URL stylesheet = MainWindow.class.getResource("/main_window.css");

        if(stylesheet != null)
        {
            mRoot.getStylesheets().add(stylesheet.toExternalForm());
        }

        mRoot.getStyleClass().add("main-window");
        mSplitPane.setOrientation(javafx.geometry.Orientation.VERTICAL);
        mSplitPane.getStyleClass().add("main-split-pane");
        //The split pane moves its dividers while it lays out, so only store the positions after the user drags one
        mSplitPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_RELEASED, event -> storeDividers());
        mSpectralDisplay.setMinHeight(80);
        mTabPane.setMinHeight(150);
        VBox.setVgrow(mSplitPane, Priority.ALWAYS);
        mCenter.getChildren().addAll(mSpectralHiddenBar, mSplitPane);
        mRoot.setTop(new VBox(createMenuBar(), mAudioView));
        mRoot.setCenter(mCenter);

        mSpectralMenuItem.setSelected(spectralVisible);
        mNowPlayingDetailsMenuItem.setSelected(detailsVisible);
        mBroadcastMenuItem.setSelected(mPreferences.getBoolean(PREFERENCE_BROADCAST_STATUS_VISIBLE, false));
        mResourceMenuItem.setSelected(mPreferences.getBoolean(PREFERENCE_RESOURCE_STATUS_VISIBLE, true));
        updateSplitPaneItems();
        updateStatusBar();

        mStage = new Stage();
        mStage.setTitle(mTitle);
        ApplicationIcons.apply(mStage);
        Scene scene = new Scene(mRoot, 1280, 800);
        ThemeManager.getInstance().register(scene);
        scene.getAccelerators().put(SPECTRAL_TOGGLE_SHORTCUT,
            () -> setSpectralDisplayVisible(!mSpectralDisplay.displayVisibleProperty().get()));
        mStage.setScene(scene);
        mStage.setMinWidth(640);
        mStage.setMinHeight(480);
        mStage.setOnCloseRequest(event -> {
            event.consume();
            exit();
        });

        migrateSwingWindowLocation();
        mUserPreferences.getJavaFxPreferences().monitor(mStage, STAGE_MONITOR_KEY);
    }

    /**
     * Main window stage.
     */
    public Stage getStage()
    {
        return mStage;
    }

    /**
     * Spectrum and waterfall display.
     */
    public MainSpectralDisplay getSpectralDisplay()
    {
        return mSpectralDisplay;
    }

    /**
     * Shows the window.  JavaFX thread only.
     */
    public void show()
    {
        mStage.show();
        //The split pane only applies divider positions once it has a size
        Platform.runLater(this::applyDividers);
    }

    /**
     * Shows the stage's title with the tuner name, or the application name when the tuner is null.  Any thread.
     */
    public void updateTitle(String tunerName)
    {
        String title = tunerName != null ? mTitle + " - " + tunerName : mTitle;
        Platform.runLater(() -> mStage.setTitle(title));
    }

    /**
     * Handles tuner events for the main spectral display.  Any thread.
     */
    public void receive(TunerEvent event)
    {
        switch(event.getEvent())
        {
            case REQUEST_MAIN_SPECTRAL_DISPLAY:
                //The user asked to see a tuner, so show the display if it's hidden
                Platform.runLater(() -> setSpectralDisplayVisible(true));
                updateTitle(event.getTuner().getPreferredName());
                break;
            case REQUEST_CLEAR_MAIN_SPECTRAL_DISPLAY:
                updateTitle(null);
                break;
            case NOTIFICATION_SHUTTING_DOWN:
                Tuner currentTuner = mSpectralDisplay.getTuner();

                if(currentTuner == null || (event.hasTuner() && event.getTuner().equals(currentTuner)))
                {
                    updateTitle(null);
                }
                break;
            default:
                break;
        }
    }

    /**
     * Saves the divider positions and stops the views.  Called during application shutdown.
     */
    public void dispose()
    {
        mAudioView.dispose();

        if(mBroadcastStatusView != null)
        {
            mBroadcastStatusView.dispose();
        }

        mSpectralDisplay.dispose();
    }

    /**
     * Shows or hides the spectrum and waterfall.  A hidden display is disposed.  JavaFX thread only.
     */
    public void setSpectralDisplayVisible(boolean visible)
    {
        mSpectralMenuItem.setSelected(visible);

        if(visible != mSpectralDisplay.displayVisibleProperty().get())
        {
            mPreferences.putBoolean(PREFERENCE_SPECTRAL_DISPLAY_VISIBLE, visible);
            mSpectralDisplay.setDisplayVisible(visible);
            updateSplitPaneItems();
            applyDividers();
        }
    }

    private HBox createSpectralHiddenBar()
    {
        IconNode icon = new IconNode(FontAwesome.EYE_SLASH);
        icon.setIconSize(13);
        icon.getStyleClass().add("main-hidden-bar-icon");
        Label label = new Label("Spectrum and waterfall are hidden and turned off, so they use no processing or memory.");
        label.getStyleClass().add("main-hidden-bar-label");
        label.setMinWidth(0);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        IconNode showIcon = new IconNode(FontAwesome.EYE);
        showIcon.setIconSize(12);
        Button show = new Button("Show Spectrum & Waterfall", showIcon);
        showIcon.fillProperty().bind(show.textFillProperty());
        show.setOnAction(event -> setSpectralDisplayVisible(true));
        HBox bar = new HBox(8, icon, label, spacer, show);
        bar.getStyleClass().add("main-hidden-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void createTabs(MapService mapService, IconModel iconModel)
    {
        MapView mapView = new MapView(mapService, iconModel, mUserPreferences,
            mPlaylistManager.getRadioReference() != null ?
                mPlaylistManager.getRadioReference().getFrequencyIndex() : null,
            new RadioReferencePlaylistActions(mPlaylistManager), true);

        Tab nowPlaying = new Tab("Now Playing", mNowPlayingView);
        Tab map = new Tab("Map", mapView);
        Tab tuners = new Tab("Tuners", new io.github.dsheirer.source.tuner.ui.TunerView(mTunerManager,
            mUserPreferences));

        //The playlist editor tab opens the playlist editor window and stays on the current tab
        IconNode playIcon = new IconNode(FontAwesome.PLAY_CIRCLE_O);
        playIcon.setIconSize(15);
        playIcon.getStyleClass().add("main-playlist-tab-icon");
        Tab playlist = new Tab("Playlist Editor");
        playlist.setGraphic(playIcon);
        playlist.setContent(new Region());

        mTabPane.getTabs().addAll(nowPlaying, map, tuners, playlist);
        mTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        mTabPane.getStyleClass().add("main-tab-pane");
        mTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if(newTab == playlist)
            {
                MyEventBus.getGlobalEventBus().post(new ViewPlaylistRequest());
                Platform.runLater(() -> mTabPane.getSelectionModel().select(oldTab != null ? oldTab : nowPlaying));
            }
        });
    }

    /**
     * Updates the split pane to hold the visible panes: spectrum (optional), tabs and streaming status (optional).
     */
    private void updateSplitPaneItems()
    {
        mUpdatingDividers = true;

        List<Node> items = new ArrayList<>();

        boolean spectralVisible = mSpectralDisplay.displayVisibleProperty().get();

        if(spectralVisible)
        {
            items.add(mSpectralDisplay);
        }

        items.add(mTabPane);

        if(mBroadcastMenuItem.isSelected())
        {
            if(mBroadcastStatusView == null)
            {
                mBroadcastStatusView = new BroadcastStatusView(mPlaylistManager.getBroadcastModel(),
                    mUserPreferences.getSwingPreference());
                mBroadcastStatusView.setMinHeight(70);
            }

            items.add(mBroadcastStatusView);
        }

        mSplitPane.getItems().setAll(items);
        SplitPane.setResizableWithParent(mTabPane, true);

        if(mBroadcastStatusView != null)
        {
            SplitPane.setResizableWithParent(mBroadcastStatusView, false);
        }

        mSpectralHiddenBar.setVisible(!spectralVisible);
        mSpectralHiddenBar.setManaged(!spectralVisible);

        mUpdatingDividers = false;
    }

    /**
     * Applies the stored divider positions to the split pane.
     */
    private void applyDividers()
    {
        mUpdatingDividers = true;
        List<SplitPane.Divider> dividers = mSplitPane.getDividers();
        int index = 0;

        if(mSpectralDisplay.displayVisibleProperty().get() && index < dividers.size())
        {
            dividers.get(index++).setPosition(mSpectralDivider);
        }

        if(mBroadcastMenuItem.isSelected() && index < dividers.size())
        {
            dividers.get(index).setPosition(1.0 - mBroadcastFraction);
        }

        mUpdatingDividers = false;
    }

    /**
     * Stores the divider positions after the user moves a divider.
     */
    private void storeDividers()
    {
        if(mUpdatingDividers || !mStage.isShowing() || mSplitPane.getDividers().isEmpty())
        {
            return;
        }

        List<SplitPane.Divider> dividers = mSplitPane.getDividers();
        int index = 0;

        if(mSpectralDisplay.displayVisibleProperty().get() && index < dividers.size())
        {
            mSpectralDivider = clamp(dividers.get(index++).getPosition());
            mPreferences.putDouble(PREFERENCE_SPECTRAL_DIVIDER, mSpectralDivider);
        }

        if(mBroadcastMenuItem.isSelected() && index < dividers.size())
        {
            mBroadcastFraction = clamp(1.0 - dividers.get(index).getPosition());
            mPreferences.putDouble(PREFERENCE_BROADCAST_DIVIDER, mBroadcastFraction);
        }
    }

    private static double clamp(double value)
    {
        return Double.isNaN(value) ? 0.3 : Math.max(0.05, Math.min(0.6, value));
    }

    private void updateStatusBar()
    {
        if(mResourceMenuItem.isSelected())
        {
            if(mStatusBox == null)
            {
                mStatusBox = new StatusBox(mResourceMonitor);
            }

            mRoot.setBottom(mStatusBox);
        }
        else
        {
            mRoot.setBottom(null);
        }
    }

    private MenuBar createMenuBar()
    {
        //File
        MenuItem processingReport = new MenuItem("Processing Diagnostic Report");
        processingReport.setOnAction(event -> createReport("Processing Status Report",
            () -> mDiagnosticMonitor.generateProcessingDiagnosticReport("User initiated diagnostic report")));
        MenuItem threadDumpReport = new MenuItem("Thread Dump Report");
        threadDumpReport.setOnAction(event -> createReport("Thread Dump Report",
            mDiagnosticMonitor::generateThreadDumpReport));
        Menu reports = new Menu("Reports", null, processingReport, threadDumpReport);

        MenuItem screenCapture = new MenuItem("Screen Capture", icon(FontAwesome.CAMERA));
        screenCapture.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.ALT_DOWN));
        screenCapture.setOnAction(event -> captureScreen());

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(event -> exit());

        Menu file = new Menu("File", null, reports, screenCapture, new SeparatorMenuItem(), exit);

        //View
        MenuItem playlistEditor = new MenuItem("Playlist Editor", icon(FontAwesome.PLAY_CIRCLE_O));
        playlistEditor.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewPlaylistRequest()));

        MenuItem logs = folderItem("Application Log Files",
            () -> mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog());
        MenuItem recordings = folderItem("Audio Recordings",
            () -> mUserPreferences.getDirectoryPreference().getDirectoryRecording());
        MenuItem eventLogs = folderItem("Channel Event Log Files",
            () -> mUserPreferences.getDirectoryPreference().getDirectoryEventLog());
        MenuItem screenCaptures = folderItem("Screen Captures",
            () -> mUserPreferences.getDirectoryPreference().getDirectoryScreenCapture());

        MenuItem iconManager = new MenuItem("Icon Manager", icon(FontAwesome.PICTURE_O));
        iconManager.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewIconManagerRequest()));
        MenuItem recordingViewer = new MenuItem("Message Recording Viewer (.bits)", icon(FontAwesome.BRAILLE));
        recordingViewer.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewRecordingViewerRequest()));
        MenuItem preferences = new MenuItem("User Preferences", icon(FontAwesome.COG));
        preferences.setOnAction(event -> MyEventBus.getGlobalEventBus().post(new ViewUserPreferenceEditorRequest()));

        Menu tuners = new Menu("Tuners");
        tuners.getItems().add(new MenuItem("(none)"));
        tuners.setOnShowing(event -> updateTunersMenu(tuners));

        mSpectralMenuItem.setAccelerator(SPECTRAL_TOGGLE_SHORTCUT);
        mSpectralMenuItem.setOnAction(event -> setSpectralDisplayVisible(mSpectralMenuItem.isSelected()));
        mNowPlayingDetailsMenuItem.setOnAction(event -> {
            mNowPlayingView.setDetailTabsVisible(mNowPlayingDetailsMenuItem.isSelected());
            mPreferences.putBoolean(PREFERENCE_NOW_PLAYING_DETAILS_VISIBLE, mNowPlayingDetailsMenuItem.isSelected());
        });
        mBroadcastMenuItem.setOnAction(event -> {
            mPreferences.putBoolean(PREFERENCE_BROADCAST_STATUS_VISIBLE, mBroadcastMenuItem.isSelected());
            updateSplitPaneItems();
            applyDividers();
        });
        mResourceMenuItem.setOnAction(event -> {
            mPreferences.putBoolean(PREFERENCE_RESOURCE_STATUS_VISIBLE, mResourceMenuItem.isSelected());
            updateStatusBar();
        });

        Menu view = new Menu("View", null, playlistEditor, new SeparatorMenuItem(), logs, recordings, eventLogs,
            iconManager, recordingViewer, screenCaptures, preferences, new SeparatorMenuItem(), tuners,
            new SeparatorMenuItem(), mSpectralMenuItem, mNowPlayingDetailsMenuItem, mBroadcastMenuItem,
            mResourceMenuItem);

        MenuBar menuBar = new MenuBar(file, view);
        menuBar.setUseSystemMenuBar(true);
        return menuBar;
    }

    /**
     * Lists the available tuners, each of which shows the tuner in the main spectral display.
     */
    private void updateTunersMenu(Menu menu)
    {
        menu.getItems().clear();

        for(DiscoveredTuner discoveredTuner: mTunerManager.getAvailableTuners())
        {
            Tuner tuner = discoveredTuner.getTuner();

            if(tuner != null)
            {
                MenuItem item = new MenuItem("Show: " + tuner.getPreferredName());
                item.setOnAction(event -> ThreadPool.CACHED.submit(() -> {
                    SystemProperties.getInstance().set(TunerSpectralDisplay.SPECTRAL_DISPLAY_ENABLED, true);
                    mTunerManager.getDiscoveredTunerModel().broadcast(new TunerEvent(tuner,
                        TunerEvent.Event.REQUEST_MAIN_SPECTRAL_DISPLAY));
                }));
                menu.getItems().add(item);
            }
        }

        if(menu.getItems().isEmpty())
        {
            MenuItem none = new MenuItem("No tuners available");
            none.setDisable(true);
            menu.getItems().add(none);
        }
    }

    private static IconNode icon(FontAwesome code)
    {
        IconNode icon = new IconNode(code);
        icon.setIconSize(12);
        icon.getStyleClass().add("main-menu-icon");
        return icon;
    }

    /**
     * Supplies a path that can throw an IO exception.
     */
    private interface PathSupplier
    {
        Path get() throws IOException;
    }

    private MenuItem folderItem(String title, PathSupplier folder)
    {
        MenuItem item = new MenuItem(title, icon(FontAwesome.FOLDER_OPEN_O));
        item.setOnAction(event -> ThreadPool.CACHED.submit(() -> {
            File directory = null;

            try
            {
                directory = folder.get().toFile();
                Desktop.getDesktop().open(directory);
            }
            catch(Exception e)
            {
                mLog.error("Couldn't open file explorer", e);
                String location = directory != null ? directory.toString() : "unknown";
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Can't launch file explorer",
                    "Can't launch the file explorer.  The files are located at:\n\n" + location));
            }
        }));
        return item;
    }

    private void createReport(String title, PathSupplier report)
    {
        ThreadPool.CACHED.submit(() -> {
            try
            {
                Path path = report.get();
                Platform.runLater(() -> showAlert(Alert.AlertType.INFORMATION, title + " Created",
                    "Report created:\n\n" + path));
            }
            catch(IOException ioe)
            {
                mLog.error("Error creating " + title, ioe);
                Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, title + " Failed",
                    "Unable to create the report file.  Please see the application log for details."));
            }
        });
    }

    /**
     * Shows a message dialog owned by the main window.  JavaFX thread only.
     */
    public void showAlert(Alert.AlertType type, String title, String message)
    {
        Alert alert = new Alert(type, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.initOwner(mStage);
        alert.show();
    }

    /**
     * Warns that the library folder holds more than one version of some libraries.  JavaFX thread only.
     * @param details listing the duplicate libraries
     * @param folder holding the libraries
     */
    public void showDuplicateLibraries(String details, String folder)
    {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Duplicate Library Versions");
        alert.setHeaderText("The sdrtrunk library folder has more than one version of some libraries");
        alert.setContentText("This happens when a new version of sdrtrunk is unzipped over an older one.  sdrtrunk " +
            "can load the older library code, which can cause crashes or errors that the newer version fixed.\n\n" +
            "Close sdrtrunk, delete the sdrtrunk folder that contains this library folder and unzip the release " +
            "into a new, empty folder:\n\n" + folder + "\n\nYour playlist and settings are stored in your user " +
            "folder and won't be affected.");
        TextArea libraries = new TextArea(details);
        libraries.setEditable(false);
        libraries.setPrefRowCount(8);
        alert.getDialogPane().setExpandableContent(libraries);
        alert.getDialogPane().setExpanded(true);
        alert.initOwner(mStage);
        alert.show();
    }

    /**
     * Saves an image of the main window to the screen captures folder.
     */
    private void captureScreen()
    {
        WritableImage image = mStage.getScene().snapshot(null);
        String filename = TimeStamp.getTimeStamp("_") + "_screen_capture.png";

        ThreadPool.CACHED.submit(() -> {
            Path file = null;

            try
            {
                file = mUserPreferences.getDirectoryPreference().getDirectoryScreenCapture().resolve(filename);
                ImageIO.write(FxToolkit.toBufferedImage(image), "png", file.toFile());
            }
            catch(Exception e)
            {
                mLog.error("Couldn't write screen capture to file [" + file + "]", e);
            }
        });
    }


    /**
     * Hides the window and shuts down the application on a background thread, since stopping the tuners can block.
     */
    private void exit()
    {
        mStage.hide();
        Thread thread = new Thread(() -> {
            try
            {
                mExitHandler.run();
            }
            catch(Throwable t)
            {
                mLog.error("Error during shutdown", t);
            }

            System.exit(0);
        }, "sdrtrunk shutdown");
        thread.start();
    }

    /**
     * Moves the main window size and location from the Swing window preferences to the JavaFX stage preferences,
     * once, so that users keep their window placement.
     */
    private void migrateSwingWindowLocation()
    {
        if(mUserPreferences.getJavaFxPreferences().hasStoredLocation(STAGE_MONITOR_KEY))
        {
            return;
        }

        String swingKey = "sdrtrunk.main.window.frame";
        java.awt.Point location = mUserPreferences.getSwingPreference().getLocation(swingKey);
        java.awt.Dimension size = mUserPreferences.getSwingPreference().getDimension(swingKey);

        if(location != null && size != null)
        {
            mUserPreferences.getJavaFxPreferences().storeLocation(STAGE_MONITOR_KEY, location.x, location.y,
                size.width, size.height, mUserPreferences.getSwingPreference().getMaximized(swingKey, false));
        }
        else
        {
            mStage.centerOnScreen();
        }
    }
}
